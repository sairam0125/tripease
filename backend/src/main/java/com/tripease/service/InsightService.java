package com.tripease.service;

import com.tripease.dto.Dtos.InsightResponse;
import com.tripease.dto.Dtos.PriceSeries;
import com.tripease.exception.ApiException;
import com.tripease.model.Hotel;
import com.tripease.model.Trip;
import com.tripease.repository.HotelRepository;
import com.tripease.repository.TripRepository;
import com.tripease.util.AppClock;
import com.tripease.util.Fmt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "AI price insight" for one trip or hotel: we compute the facts ourselves (price series, cheapest
 * day, price if you wait a week) and let llama-3.1 phrase the advice. Without an API key the same
 * facts are turned into advice by simple rules. Results are cached for 30 minutes.
 */
@Service
@RequiredArgsConstructor
public class InsightService {

    private static final long TTL_MS = 30 * 60 * 1000L;
    private static final String SYSTEM_PROMPT =
            "You are TripEase's price advisor. Using ONLY the numbers provided, write 2-3 short sentences "
                    + "(max 55 words) saying whether to book now or shift dates, and how much could be saved in rupees. "
                    + "Plain text only, no markdown, no emojis.";

    private final TripRepository trips;
    private final HotelRepository hotels;
    private final PricingService pricing;
    private final OpenAiClient openAi;

    private record Cached(InsightResponse value, long expiresAt) {}

    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public InsightResponse insight(String kind, Long id, LocalDate requested) {
        LocalDate today = AppClock.today();
        final LocalDate date = requested.isBefore(today) ? today : requested;
        String key = kind.toUpperCase() + ":" + id + ":" + date + ":" + today;
        Cached hit = cache.get(key);
        if (hit != null && hit.expiresAt() > System.currentTimeMillis()) {
            return hit.value();
        }

        LocalDate bookLater = today.plusDays(7);
        String subject;
        PriceSeries series;
        BigDecimal later = null;
        boolean isHotel;

        if ("TRIP".equalsIgnoreCase(kind)) {
            Trip t = trips.findById(id).orElseThrow(() -> ApiException.notFound("Trip not found"));
            isHotel = false;
            subject = t.getMode().name().toLowerCase() + " " + t.getOperator() + " " + t.getCode()
                    + " from " + t.getOrigin() + " to " + t.getDestination() + " (per person)";
            series = pricing.tripSeries(t, date, 30);
            if (date.isAfter(bookLater)) later = pricing.tripPrice(t, date, bookLater);
        } else if ("HOTEL".equalsIgnoreCase(kind)) {
            Hotel h = hotels.findById(id).orElseThrow(() -> ApiException.notFound("Hotel not found"));
            isHotel = true;
            subject = h.getName() + " in " + h.getCity() + " (per night)";
            series = pricing.hotelSeries(h, date, 30);
            if (date.isAfter(bookLater)) later = pricing.hotelNightPrice(h, date, bookLater);
        } else {
            throw ApiException.badRequest("kind must be TRIP or HOTEL");
        }

        long daysAhead = ChronoUnit.DAYS.between(today, date);
        final String fallback = ruleBased(series, later, daysAhead, isHotel);
        InsightResponse result = openAi.isConfigured()
                ? askModel(subject, series, later, daysAhead)
                .map(text -> new InsightResponse(text, openAi.model()))
                .orElseGet(() -> new InsightResponse(fallback, "rule-based"))
                : new InsightResponse(fallback, "rule-based");

        if (cache.size() > 2000) cache.clear();
        cache.put(key, new Cached(result, System.currentTimeMillis() + TTL_MS));
        return result;
    }

    private Optional<String> askModel(String subject, PriceSeries s, BigDecimal later, long daysAhead) {
        StringBuilder facts = new StringBuilder();
        facts.append("Subject: ").append(subject).append('\n');
        facts.append("Selected date: ").append(s.selectedDate()).append(" (").append(daysAhead).append(" days from today)\n");
        facts.append("Price on selected date: ").append(Fmt.inr(s.selectedPrice())).append('\n');
        facts.append("Lowest price in the next 30-day window: ").append(Fmt.inr(s.min())).append(" on ").append(s.cheapestDate()).append('\n');
        facts.append("Window average: ").append(Fmt.inr(s.average())).append(" (selected is ").append(s.percentVsAverage()).append("% vs average)\n");
        if (later != null) {
            facts.append("Expected price for the selected date if booked 7 days from now: ").append(Fmt.inr(later)).append('\n');
        }
        facts.append("Give a short booking recommendation.");
        return openAi.complete(List.of(
                OpenAiClient.message("system", SYSTEM_PROMPT),
                OpenAiClient.message("user", facts.toString())), 0.4, 160);
    }

    /** Deterministic fallback used when there is no API key (or OpenAI is unreachable). */
    String ruleBased(PriceSeries s, BigDecimal later, long daysAhead, boolean hotel) {
        StringBuilder sb = new StringBuilder();
        double pct = s.percentVsAverage();
        BigDecimal saving = s.selectedPrice().subtract(s.min());
        String unit = hotel ? "per night" : "per person";

        if (s.selectedPrice().doubleValue() <= s.min().doubleValue() * 1.03) {
            sb.append("Good timing: ").append(Fmt.inr(s.selectedPrice())).append(" is at or near the lowest price in this window. ");
        } else if (pct > 8) {
            sb.append("This date is ").append(pct).append("% above the window average. Choosing ").append(s.cheapestDate())
                    .append(" would cost ").append(Fmt.inr(s.min())).append(", saving about ").append(Fmt.inr(saving))
                    .append(" ").append(unit).append(". ");
        } else if (pct < -8) {
            sb.append("This is ").append(Math.abs(pct)).append("% cheaper than the window average, which is a good deal. ");
        } else {
            sb.append("The price is close to the window average of ").append(Fmt.inr(s.average())).append(", so it is a fair deal. ");
        }

        if (later != null) {
            double change = (later.doubleValue() - s.selectedPrice().doubleValue()) / s.selectedPrice().doubleValue() * 100;
            if (change > 3) {
                sb.append("Prices are expected to rise about ").append(Math.round(change)).append("% if you wait a week, so booking now is safer.");
            } else if (change < -3) {
                sb.append("Prices may ease slightly over the next week, but availability can drop.");
            } else {
                sb.append("Prices look stable for the next week.");
            }
        } else if (daysAhead <= 3) {
            sb.append("Departure is close, so last-minute pricing applies. Book soon before it sells out.");
        }
        return sb.toString().trim();
    }
}
