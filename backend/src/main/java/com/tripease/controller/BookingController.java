package com.tripease.controller;

import com.tripease.dto.Dtos.BookingRequest;
import com.tripease.dto.Dtos.BookingResponse;
import com.tripease.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookings;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody BookingRequest request, Authentication auth) {
        return bookings.create(auth.getName(), request);
    }

    @GetMapping
    public List<BookingResponse> mine(Authentication auth) {
        return bookings.mine(auth.getName());
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id, Authentication auth) {
        return bookings.cancel(auth.getName(), id);
    }
}
