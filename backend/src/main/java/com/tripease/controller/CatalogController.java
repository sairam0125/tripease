package com.tripease.controller;

import com.tripease.dto.Dtos.CityDto;
import com.tripease.dto.Dtos.HotelResult;
import com.tripease.dto.Dtos.TripResult;
import com.tripease.model.HotelType;
import com.tripease.model.TransportMode;
import com.tripease.repository.CityRepository;
import com.tripease.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final CityRepository cities;
    private final CatalogService catalog;

    @GetMapping("/cities")
    public List<CityDto> cities() {
        return cities.findAllByOrderByNameAsc().stream().map(c -> new CityDto(c.getName(), c.getCode())).toList();
    }

    @GetMapping("/trips/search")
    public List<TripResult> searchTrips(
            @RequestParam TransportMode mode,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return catalog.searchTrips(mode, from, to, date);
    }

    @GetMapping("/trips/{id}")
    public TripResult getTrip(@PathVariable Long id,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return catalog.getTrip(id, date);
    }

    @GetMapping("/hotels/search")
    public List<HotelResult> searchHotels(
            @RequestParam String city,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) HotelType type) {
        return catalog.searchHotels(city, checkIn, checkOut, type);
    }

    @GetMapping("/hotels/{id}")
    public HotelResult getHotel(@PathVariable Long id,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return catalog.getHotel(id, checkIn, checkOut);
    }
}
