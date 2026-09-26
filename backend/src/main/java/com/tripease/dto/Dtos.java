package com.tripease.dto;

import com.tripease.model.BookingStatus;
import com.tripease.model.BookingType;
import com.tripease.model.HotelType;
import com.tripease.model.TransportMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** All request/response shapes of the API in one place (Java records). */
public final class Dtos {
    private Dtos() {}

    // ---------- auth ----------
    public record RegisterRequest(
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(min = 6, max = 72) String password) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record UserDto(Long id, String name, String email) {}

    public record AuthResponse(String token, UserDto user) {}

    // ---------- catalog ----------
    public record CityDto(String name, String code) {}

    public record TripResult(
            Long id, TransportMode mode, String operator, String code, String travelClass,
            String origin, String originCode, String destination, String destinationCode,
            LocalDate travelDate, LocalTime departureTime, LocalTime arrivalTime, int arrivalDayOffset,
            int durationMinutes, BigDecimal price, int seatsLeft) {}

    public record HotelResult(
            Long id, String name, String city, HotelType type, int stars, double rating,
            List<String> amenities, String description,
            LocalDate checkIn, LocalDate checkOut, int nights,
            BigDecimal pricePerNight, BigDecimal totalPrice, int roomsLeft) {}

    // ---------- prices & AI ----------
    public record PricePoint(LocalDate date, BigDecimal price) {}

    public record PriceSeries(
            String label, List<PricePoint> points, LocalDate selectedDate, BigDecimal selectedPrice,
            BigDecimal min, BigDecimal max, BigDecimal average, LocalDate cheapestDate, double percentVsAverage) {}

    public record InsightResponse(String insight, String source) {}

    public record ChatMessage(@NotBlank @Size(max = 20) String role, @NotBlank @Size(max = 1500) String content) {}

    public record ChatRequest(
            @NotBlank @Size(max = 500) String message,
            @Size(max = 12) List<@Valid ChatMessage> history) {}

    /** A one-tap search the chatbot suggests. For hotels, {@code to} holds the city. */
    public record Suggestion(String label, String mode, String from, String to, String date) {}

    public record ChatResponse(String reply, List<Suggestion> suggestions, String source) {}

    // ---------- bookings ----------
    public record BookingRequest(
            @NotNull BookingType type,
            Long tripId,
            Long hotelId,
            @NotNull LocalDate travelDate,
            LocalDate checkOutDate,
            @Min(1) @Max(9) int quantity,
            @NotBlank @Size(max = 500) String travellerNames,
            @Size(max = 30) String paymentMethod) {}

    public record BookingResponse(
            Long id, String reference, BookingType type, BookingStatus status,
            String title, String subtitle, LocalDate travelDate, LocalDate checkOutDate,
            String departureTime, String arrivalTime, int quantity, String travellerNames,
            BigDecimal totalAmount, String paymentMethod, LocalDateTime createdAt) {}
}
