package com.tripease.service;

import com.tripease.dto.Dtos.HotelResult;
import com.tripease.dto.Dtos.TripResult;
import com.tripease.exception.ApiException;
import com.tripease.model.BookingStatus;
import com.tripease.model.Hotel;
import com.tripease.model.HotelType;
import com.tripease.model.TransportMode;
import com.tripease.model.Trip;
import com.tripease.repository.BookingRepository;
import com.tripease.repository.HotelRepository;
import com.tripease.repository.TripRepository;
import com.tripease.util.AppClock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Search + availability for transport and stays. */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final TripRepository trips;
    private final HotelRepository hotels;
    private final BookingRepository bookings;
    private final PricingService pricing;

    // ------------------------------------------------------------ transport

    @Transactional(readOnly = true)
    public List<TripResult> searchTrips(TransportMode mode, String from, String to, LocalDate date) {
        requireTodayOrLater(date);
        if (from.trim().equalsIgnoreCase(to.trim())) {
            throw ApiException.badRequest("Origin and destination must be different");
        }
        return trips.findByModeAndOriginIgnoreCaseAndDestinationIgnoreCase(mode, from.trim(), to.trim())
                .stream()
                .map(t -> toResult(t, date))
                .sorted(Comparator.comparing(TripResult::price))
                .toList();
    }

    @Transactional(readOnly = true)
    public TripResult getTrip(Long id, LocalDate date) {
        requireTodayOrLater(date);
        Trip trip = trips.findById(id).orElseThrow(() -> ApiException.notFound("Trip not found"));
        return toResult(trip, date);
    }

    public int seatsLeft(Trip trip, LocalDate date) {
        long booked = bookings.seatsBooked(trip.getId(), date, BookingStatus.CONFIRMED);
        return (int) Math.max(0, trip.getTotalSeats() - pricing.baselineSold(trip, date) - booked);
    }

    public TripResult toResult(Trip t, LocalDate date) {
        LocalTime arrival = t.getDepartureTime().plusMinutes(t.getDurationMinutes());
        int minutesFromMidnight = t.getDepartureTime().getHour() * 60 + t.getDepartureTime().getMinute();
        int dayOffset = (minutesFromMidnight + t.getDurationMinutes()) / 1440;
        return new TripResult(t.getId(), t.getMode(), t.getOperator(), t.getCode(), t.getTravelClass(),
                t.getOrigin(), t.getOriginCode(), t.getDestination(), t.getDestinationCode(),
                date, t.getDepartureTime(), arrival, dayOffset, t.getDurationMinutes(),
                pricing.tripPrice(t, date), seatsLeft(t, date));
    }

    // ------------------------------------------------------------ stays

    @Transactional(readOnly = true)
    public List<HotelResult> searchHotels(String city, LocalDate checkIn, LocalDate checkOut, HotelType type) {
        requireValidStay(checkIn, checkOut);
        return hotels.findByCityIgnoreCase(city.trim()).stream()
                .filter(h -> type == null || h.getType() == type)
                .map(h -> toHotelResult(h, checkIn, checkOut))
                .sorted(Comparator.comparing(HotelResult::pricePerNight))
                .toList();
    }

    @Transactional(readOnly = true)
    public HotelResult getHotel(Long id, LocalDate checkIn, LocalDate checkOut) {
        requireValidStay(checkIn, checkOut);
        Hotel hotel = hotels.findById(id).orElseThrow(() -> ApiException.notFound("Hotel not found"));
        return toHotelResult(hotel, checkIn, checkOut);
    }

    public int roomsLeft(Hotel hotel, LocalDate checkIn, LocalDate checkOut) {
        long booked = bookings.roomsBooked(hotel.getId(), checkIn, checkOut, BookingStatus.CONFIRMED);
        return (int) Math.max(0, hotel.getRoomsTotal() - pricing.baselineOccupied(hotel, checkIn) - booked);
    }

    public HotelResult toHotelResult(Hotel h, LocalDate checkIn, LocalDate checkOut) {
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal total = pricing.hotelStayTotal(h, checkIn, checkOut);
        BigDecimal perNight = total.divide(BigDecimal.valueOf(nights), 0, RoundingMode.HALF_UP);
        List<String> amenities = h.getAmenities() == null ? List.of()
                : Arrays.stream(h.getAmenities().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return new HotelResult(h.getId(), h.getName(), h.getCity(), h.getType(), h.getStars(), h.getRating(),
                amenities, h.getDescription(), checkIn, checkOut, nights, perNight, total, roomsLeft(h, checkIn, checkOut));
    }

    // ------------------------------------------------------------ validation

    public void requireTodayOrLater(LocalDate date) {
        if (date.isBefore(AppClock.today())) {
            throw ApiException.badRequest("Travel date cannot be in the past");
        }
    }

    public void requireValidStay(LocalDate checkIn, LocalDate checkOut) {
        requireTodayOrLater(checkIn);
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.badRequest("Check-out must be after check-in");
        }
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > 30) {
            throw ApiException.badRequest("Stays are limited to 30 nights");
        }
    }
}
