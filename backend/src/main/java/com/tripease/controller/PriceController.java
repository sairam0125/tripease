package com.tripease.controller;

import com.tripease.dto.Dtos.PriceSeries;
import com.tripease.exception.ApiException;
import com.tripease.repository.HotelRepository;
import com.tripease.repository.TripRepository;
import com.tripease.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Data behind the price graphs (price for every day around the selected date). */
@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PriceController {

    private final TripRepository trips;
    private final HotelRepository hotels;
    private final PricingService pricing;

    @GetMapping("/trips/{id}")
    public PriceSeries tripSeries(@PathVariable Long id,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  @RequestParam(defaultValue = "30") int days) {
        var trip = trips.findById(id).orElseThrow(() -> ApiException.notFound("Trip not found"));
        return pricing.tripSeries(trip, date, clamp(days));
    }

    @GetMapping("/hotels/{id}")
    public PriceSeries hotelSeries(@PathVariable Long id,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   @RequestParam(defaultValue = "30") int days) {
        var hotel = hotels.findById(id).orElseThrow(() -> ApiException.notFound("Hotel not found"));
        return pricing.hotelSeries(hotel, date, clamp(days));
    }

    private int clamp(int days) {
        return Math.max(14, Math.min(60, days));
    }
}
