package com.tripease.service;

import com.tripease.dto.Dtos.ChatMessage;
import com.tripease.dto.Dtos.ChatRequest;
import com.tripease.dto.Dtos.ChatResponse;
import com.tripease.dto.Dtos.Suggestion;
import com.tripease.model.City;
import com.tripease.model.Hotel;
import com.tripease.model.TransportMode;
import com.tripease.model.Trip;
import com.tripease.repository.CityRepository;
import com.tripease.repository.HotelRepository;
import com.tripease.repository.TripRepository;
import com.tripease.util.AppClock;
import com.tripease.util.Fmt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Travel chatbot = "retrieval + generation".
 *  1. Understand the message (cities, mode, date, budget) with light NLP.
 *  2. Retrieve LIVE options from our own database (same pricing engine as the booking flow).
 *  3. Ask GPT-3.5 to phrase a friendly answer using ONLY that data (no invented fares).
 * Without an OpenAI key, step 3 is replaced by a template so the bot still works.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            You are TripEase Assistant, a friendly travel concierge inside an Indian travel booking app that sells \
            flights, trains, buses, hotels and resorts.
            Rules:
            - Keep answers under 110 words, warm and practical. Plain text; short lines starting with "-" are fine.
            - For prices, timings and availability use ONLY the LIVE DATA block. Never invent fares, schedules or hotels.
            - If LIVE DATA is empty, ask ONE clarifying question (origin, destination, date or budget) or give brief general travel advice.
            - You cannot book for the user; tell them to tap the suggested search button and press Book.
            - All prices are in Indian rupees.
            """;

    private static final Map<String, String> ALIASES = Map.of(
            "bangalore", "Bengaluru", "bombay", "Mumbai", "vizag", "Visakhapatnam", "calcutta", "Kolkata",
            "cochin", "Kochi", "new delhi", "Delhi", "madras", "Chennai");

    private static final Pattern BUDGET = Pattern.compile(
            "(?:under|below|within|budget(?: of)?|less than|upto|up to|max(?:imum)?)\\s*(?:rs\\.?|inr|₹)?\\s*(\\d[\\d,]*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ISO_DATE = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern HOTEL_WORDS = Pattern.compile("\\b(hotels?|resorts?|stays?|rooms?|accommodation|lodging)\\b");
    private static final Pattern FLIGHT_WORDS = Pattern.compile("\\b(flights?|fly|flying|plane|air)\\b");
    private static final Pattern TRAIN_WORDS = Pattern.compile("\\b(trains?|rail|railway)\\b");
    private static final Pattern BUS_WORDS = Pattern.compile("\\b(buses|bus|coach|volvo)\\b");
    private static final DateTimeFormatter NICE_DATE = DateTimeFormatter.ofPattern("EEE, d MMM");

    private final CityRepository cityRepo;
    private final TripRepository tripRepo;
    private final HotelRepository hotelRepo;
    private final PricingService pricing;
    private final OpenAiClient openAi;

    private record Intent(String origin, String dest, String mode, boolean hotel, LocalDate date,
                          boolean dateAssumed, Integer budget) {}

    private record Priced(Trip trip, BigDecimal price) {}

    @Transactional(readOnly = true)
    public ChatResponse chat(ChatRequest req) {
        List<City> cities = cityRepo.findAll();
        Intent intent = parse(req.message(), cities);

        List<String> data = new ArrayList<>();
        List<Suggestion> suggestions = new ArrayList<>();
        retrieve(intent, data, suggestions);

        Optional<String> ai = openAi.isConfigured()
                ? openAi.complete(buildMessages(req, intent, data, cities), 0.5, 300)
                : Optional.empty();

        String reply = ai.orElseGet(() -> templateReply(intent, data));
        return new ChatResponse(reply, suggestions, ai.isPresent() ? openAi.model() : "rule-based");
    }

    // ------------------------------------------------------------------ understanding

    private Intent parse(String message, List<City> cities) {
        String m = message.toLowerCase(Locale.ROOT);
        LocalDate today = AppClock.today();

        // cities (with a few common aliases), ordered by position in the sentence
        TreeMap<Integer, String> found = new TreeMap<>();
        for (City c : cities) {
            int i = indexOfWord(m, c.getName().toLowerCase(Locale.ROOT));
            if (i >= 0) found.put(i, c.getName());
        }
        for (Map.Entry<String, String> a : ALIASES.entrySet()) {
            int i = indexOfWord(m, a.getKey());
            if (i >= 0) found.putIfAbsent(i, a.getValue());
        }
        String origin = null;
        String dest = null;
        for (Map.Entry<Integer, String> e : found.entrySet()) {
            String before = m.substring(Math.max(0, e.getKey() - 8), e.getKey());
            if (before.contains("from")) {
                origin = e.getValue();
            } else if (before.contains("to ") || before.contains(" in") || before.contains("at ")
                    || before.contains("for ") || before.contains("near")) {
                dest = e.getValue();
            }
        }
        List<String> ordered = found.values().stream().distinct().toList();
        if (origin == null && dest == null && ordered.size() >= 2) {
            origin = ordered.get(0);
            dest = ordered.get(1);
        } else if (origin == null && dest != null && ordered.size() >= 2) {
            final String d = dest;
            origin = ordered.stream().filter(c -> !c.equals(d)).findFirst().orElse(null);
        } else if (dest == null && origin != null && ordered.size() >= 2) {
            final String o = origin;
            dest = ordered.stream().filter(c -> !c.equals(o)).findFirst().orElse(null);
        } else if (origin == null && dest == null && ordered.size() == 1) {
            dest = ordered.get(0);
        }
        if (origin != null && origin.equals(dest)) dest = null;

        // mode
        String mode = null;
        if (FLIGHT_WORDS.matcher(m).find()) mode = "FLIGHT";
        else if (TRAIN_WORDS.matcher(m).find()) mode = "TRAIN";
        else if (BUS_WORDS.matcher(m).find()) mode = "BUS";
        boolean hotel = HOTEL_WORDS.matcher(m).find();

        // date
        LocalDate date;
        boolean assumed = false;
        Matcher iso = ISO_DATE.matcher(m);
        if (iso.find()) {
            try {
                date = LocalDate.parse(iso.group(1));
            } catch (DateTimeParseException e) {
                date = today.plusDays(7);
                assumed = true;
            }
        } else if (m.contains("day after tomorrow")) {
            date = today.plusDays(2);
        } else if (m.contains("tomorrow")) {
            date = today.plusDays(1);
        } else if (m.contains("today") || m.contains("tonight")) {
            date = today;
        } else if (m.contains("weekend") || m.contains("saturday")) {
            date = today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        } else if (m.contains("next week")) {
            date = today.plusDays(7);
        } else if (m.contains("next month")) {
            date = today.plusDays(30);
        } else {
            date = today.plusDays(7);
            assumed = true;
        }
        if (date.isBefore(today)) date = today;

        // budget
        Integer budget = null;
        Matcher b = BUDGET.matcher(message);
        if (b.find()) {
            try {
                budget = Integer.parseInt(b.group(1).replace(",", ""));
            } catch (NumberFormatException ignored) {
                // ignore absurdly large numbers
            }
        }
        return new Intent(origin, dest, mode, hotel, date, assumed, budget);
    }

    private static int indexOfWord(String haystack, String word) {
        Matcher mt = Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(haystack);
        return mt.find() ? mt.start() : -1;
    }

    // ------------------------------------------------------------------ retrieval

    private void retrieve(Intent in, List<String> data, List<Suggestion> suggestions) {
        String date = in.date().toString();
        boolean wantsRoute = in.origin() != null && in.dest() != null && (in.mode() != null || !in.hotel());
        String hotelCity = in.dest() != null ? in.dest() : in.origin();

        if (wantsRoute) {
            List<TransportMode> modes = in.mode() != null
                    ? List.of(TransportMode.valueOf(in.mode()))
                    : List.of(TransportMode.FLIGHT, TransportMode.TRAIN, TransportMode.BUS);
            for (TransportMode mode : modes) {
                String label = title(mode.name());
                List<Trip> trips = tripRepo.findByModeAndOriginIgnoreCaseAndDestinationIgnoreCase(mode, in.origin(), in.dest());
                if (trips.isEmpty()) {
                    data.add(label + ": no direct service between " + in.origin() + " and " + in.dest());
                    continue;
                }
                List<Priced> all = trips.stream().map(t -> new Priced(t, pricing.tripPrice(t, in.date()))).toList();
                List<Priced> options = all.stream()
                        .filter(p -> in.budget() == null || p.price().intValue() <= in.budget()).toList();
                if (options.isEmpty()) {
                    BigDecimal min = all.stream().map(Priced::price).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
                    data.add(label + ": nothing within " + Fmt.inr(BigDecimal.valueOf(in.budget())) + "; cheapest overall is " + Fmt.inr(min));
                } else {
                    Priced cheapest = options.stream().min(Comparator.comparing(Priced::price)).orElseThrow();
                    Priced fastest = options.stream().min(Comparator.comparingInt(p -> p.trip().getDurationMinutes())).orElseThrow();
                    String line = label + " - cheapest: " + describe(cheapest);
                    if (fastest != cheapest) line += "; fastest: " + describe(fastest);
                    data.add(line);
                }
                suggestions.add(new Suggestion(label + "s " + in.origin() + " to " + in.dest(), mode.name(), in.origin(), in.dest(), date));
            }
        }

        if (in.hotel() && hotelCity != null) {
            List<Hotel> stays = hotelRepo.findByCityIgnoreCase(hotelCity);
            List<String> lines = stays.stream()
                    .map(h -> Map.entry(h, pricing.hotelNightPrice(h, in.date())))
                    .filter(e -> in.budget() == null || e.getValue().intValue() <= in.budget())
                    .sorted((x, y) -> {
                        int byRating = Double.compare(y.getKey().getRating(), x.getKey().getRating());
                        return byRating != 0 ? byRating : x.getValue().compareTo(y.getValue());
                    })
                    .limit(3)
                    .map(e -> e.getKey().getName() + " (" + title(e.getKey().getType().name()) + ", " + e.getKey().getStars()
                            + "-star, rating " + e.getKey().getRating() + "): " + Fmt.inr(e.getValue()) + " per night")
                    .toList();
            if (lines.isEmpty()) {
                data.add("Stays in " + hotelCity + ": nothing matches that budget for " + in.date());
            } else {
                data.add("Top stays in " + hotelCity + " for check-in " + in.date() + ": " + String.join("; ", lines));
            }
            suggestions.add(new Suggestion("Stays in " + hotelCity, "HOTEL", null, hotelCity, date));
        } else if (!wantsRoute && in.origin() != null && in.dest() == null) {
            getawayIdeas(in, data, suggestions);
        } else if (!wantsRoute && in.dest() != null && in.origin() == null) {
            data.add("User is interested in " + in.dest() + " but has not said where they are travelling from");
            suggestions.add(new Suggestion("Stays in " + in.dest(), "HOTEL", null, in.dest(), date));
        }
    }

    private void getawayIdeas(Intent in, List<String> data, List<Suggestion> suggestions) {
        Map<String, Priced> cheapestByDest = new HashMap<>();
        for (Trip t : tripRepo.findByOriginIgnoreCase(in.origin())) {
            Priced p = new Priced(t, pricing.tripPrice(t, in.date()));
            if (in.budget() != null && p.price().intValue() > in.budget()) continue;
            cheapestByDest.merge(t.getDestination(), p, (a, b) -> a.price().compareTo(b.price()) <= 0 ? a : b);
        }
        List<Priced> top = cheapestByDest.values().stream()
                .sorted(Comparator.comparing(Priced::price)).limit(4).toList();
        if (top.isEmpty()) {
            data.add("No getaways from " + in.origin() + " found for that budget");
            return;
        }
        data.add("Cheapest getaways from " + in.origin() + " on " + in.date() + ": " + top.stream()
                .map(p -> p.trip().getDestination() + " from " + Fmt.inr(p.price()) + " by " + p.trip().getMode().name().toLowerCase())
                .collect(Collectors.joining("; ")));
        top.stream().limit(3).forEach(p -> suggestions.add(new Suggestion(
                p.trip().getDestination() + " by " + p.trip().getMode().name().toLowerCase(),
                p.trip().getMode().name(), in.origin(), p.trip().getDestination(), in.date().toString())));
    }

    private static String describe(Priced p) {
        Trip t = p.trip();
        return t.getOperator() + " " + t.getCode() + " (" + t.getTravelClass() + ") departs " + t.getDepartureTime()
                + ", " + Fmt.duration(t.getDurationMinutes()) + ", " + Fmt.inr(p.price());
    }

    private static String title(String upper) {
        return upper.charAt(0) + upper.substring(1).toLowerCase();
    }

    // ------------------------------------------------------------------ generation

    private List<Map<String, String>> buildMessages(ChatRequest req, Intent in, List<String> data, List<City> cities) {
        String cityNames = cities.stream().map(City::getName).sorted().collect(Collectors.joining(", "));
        String live = data.isEmpty() ? "(none)" : String.join("\n", data);
        String system = SYSTEM_PROMPT
                + "\nSupported cities: " + cityNames + "."
                + "\nToday: " + AppClock.today() + "."
                + "\nLIVE DATA (travel date " + in.date() + (in.dateAssumed() ? ", assumed because the user gave none" : "") + "):\n" + live;

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(OpenAiClient.message("system", system));
        List<ChatMessage> history = req.history() == null ? List.of() : req.history();
        int from = Math.max(0, history.size() - 8);
        for (ChatMessage h : history.subList(from, history.size())) {
            String role = h.role().toLowerCase(Locale.ROOT);
            if (role.equals("user") || role.equals("assistant")) {   // never accept a client-supplied "system" role
                String content = h.content().length() > 800 ? h.content().substring(0, 800) : h.content();
                messages.add(OpenAiClient.message(role, content));
            }
        }
        messages.add(OpenAiClient.message("user", req.message()));
        return messages;
    }

    private String templateReply(Intent in, List<String> data) {
        if (data.isEmpty()) {
            return "Hi! I'm your TripEase assistant. Tell me where you're travelling from and to, when, and your budget. "
                    + "For example: \"Cheapest way from Hyderabad to Goa this weekend under ₹5000\" or \"Resorts in Goa under ₹9000\". "
                    + "I can compare flights, trains, buses and stays.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Here's what I found for ").append(NICE_DATE.format(in.date())).append(":\n");
        data.forEach(line -> sb.append("- ").append(line).append('\n'));
        if (in.dateAssumed()) {
            sb.append("\nI assumed travel on ").append(NICE_DATE.format(in.date())).append(". Tell me your date for exact fares.");
        } else {
            sb.append("\nTip: fares climb as departure gets closer, so booking early usually saves money.");
        }
        return sb.toString().trim();
    }
}
