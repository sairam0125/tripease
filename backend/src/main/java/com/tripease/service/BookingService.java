package com.tripease.service;

import com.tripease.dto.Dtos.BookingRequest;
import com.tripease.dto.Dtos.BookingResponse;
import com.tripease.exception.ApiException;
import com.tripease.model.Booking;
import com.tripease.model.BookingStatus;
import com.tripease.model.BookingType;
import com.tripease.model.Hotel;
import com.tripease.model.Trip;
import com.tripease.model.User;
import com.tripease.repository.BookingRepository;
import com.tripease.repository.HotelRepository;
import com.tripease.repository.TripRepository;
import com.tripease.repository.UserRepository;
import com.tripease.util.AppClock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookings;
    private final UserRepository users;
    private final TripRepository trips;
    private final HotelRepository hotels;
    private final PricingService pricing;
    private final CatalogService catalog;

    /**
     * Creates a booking. The amount is ALWAYS recalculated on the server (never trusted from the
     * browser) and the trip/hotel row is locked so seats cannot be oversold under concurrency.
     * Payment is simulated - no real money moves.
     */
    @Transactional
    public BookingResponse create(String email, BookingRequest req) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("Session expired. Please sign in again."));

        Booking b = new Booking();
        b.setUser(user);
        b.setType(req.type());
        b.setTravelDate(req.travelDate());
        b.setQuantity(req.quantity());
        b.setTravellerNames(req.travellerNames().trim());
        b.setPaymentMethod(req.paymentMethod() == null || req.paymentMethod().isBlank() ? "UPI" : req.paymentMethod());

        if (req.type() == BookingType.HOTEL) {
            bookHotel(b, req);
        } else {
            bookTrip(b, req);
        }

        b.setStatus(BookingStatus.CONFIRMED);
        b.setReference("TE" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        return toResponse(bookings.save(b));
    }

    private void bookTrip(Booking b, BookingRequest req) {
        if (req.tripId() == null) throw ApiException.badRequest("tripId is required");
        catalog.requireTodayOrLater(req.travelDate());
        Trip trip = trips.lockById(req.tripId()).orElseThrow(() -> ApiException.notFound("Trip not found"));
        if (!trip.getMode().name().equals(req.type().name())) {
            throw ApiException.badRequest("Booking type does not match the selected service");
        }
        int left = catalog.seatsLeft(trip, req.travelDate());
        if (left < req.quantity()) {
            throw ApiException.conflict(left == 0 ? "Sorry, this service is sold out for the selected date"
                    : "Only " + left + " seat(s) left for the selected date");
        }
        BigDecimal unit = pricing.tripPrice(trip, req.travelDate());
        b.setTrip(trip);
        b.setTotalAmount(unit.multiply(BigDecimal.valueOf(req.quantity())));
    }

    private void bookHotel(Booking b, BookingRequest req) {
        if (req.hotelId() == null) throw ApiException.badRequest("hotelId is required");
        if (req.checkOutDate() == null) throw ApiException.badRequest("checkOutDate is required");
        catalog.requireValidStay(req.travelDate(), req.checkOutDate());
        Hotel hotel = hotels.lockById(req.hotelId()).orElseThrow(() -> ApiException.notFound("Hotel not found"));
        int left = catalog.roomsLeft(hotel, req.travelDate(), req.checkOutDate());
        if (left < req.quantity()) {
            throw ApiException.conflict(left == 0 ? "Sorry, no rooms left for these dates"
                    : "Only " + left + " room(s) left for these dates");
        }
        BigDecimal perRoom = pricing.hotelStayTotal(hotel, req.travelDate(), req.checkOutDate());
        b.setHotel(hotel);
        b.setCheckOutDate(req.checkOutDate());
        b.setTotalAmount(perRoom.multiply(BigDecimal.valueOf(req.quantity())));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> mine(String email) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("Session expired. Please sign in again."));
        return bookings.findByUserOrderByCreatedAtDesc(user).stream().map(this::toResponse).toList();
    }

    @Transactional
    public BookingResponse cancel(String email, Long id) {
        Booking b = bookings.findById(id)
                .filter(x -> x.getUser().getEmail().equals(email))
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (b.getStatus() == BookingStatus.CANCELLED) {
            throw ApiException.conflict("This booking is already cancelled");
        }
        if (b.getTravelDate().isBefore(AppClock.today())) {
            throw ApiException.badRequest("Past bookings cannot be cancelled");
        }
        b.setStatus(BookingStatus.CANCELLED);
        return toResponse(b);
    }

    private BookingResponse toResponse(Booking b) {
        String title;
        String subtitle;
        String dep = null;
        String arr = null;
        if (b.getType() == BookingType.HOTEL) {
            Hotel h = b.getHotel();
            title = h.getName();
            subtitle = h.getCity() + " (" + (h.getType().name().charAt(0) + h.getType().name().substring(1).toLowerCase()) + ")";
        } else {
            Trip t = b.getTrip();
            title = t.getOperator() + " " + t.getCode();
            subtitle = t.getOrigin() + " to " + t.getDestination();
            dep = t.getDepartureTime().toString();
            arr = t.getDepartureTime().plusMinutes(t.getDurationMinutes()).toString();
        }
        return new BookingResponse(b.getId(), b.getReference(), b.getType(), b.getStatus(), title, subtitle,
                b.getTravelDate(), b.getCheckOutDate(), dep, arr, b.getQuantity(), b.getTravellerNames(),
                b.getTotalAmount(), b.getPaymentMethod(), b.getCreatedAt());
    }
}
