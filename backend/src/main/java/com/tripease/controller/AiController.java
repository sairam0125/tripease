package com.tripease.controller;

import com.tripease.dto.Dtos.ChatRequest;
import com.tripease.dto.Dtos.ChatResponse;
import com.tripease.dto.Dtos.InsightResponse;
import com.tripease.exception.ApiException;
import com.tripease.service.ChatService;
import com.tripease.service.InsightService;
import com.tripease.service.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private static final long WINDOW_MS = 10 * 60 * 1000L;

    private final InsightService insights;
    private final ChatService chat;
    private final RateLimiter limiter;

    /** GET /api/ai/insight?kind=TRIP|HOTEL&id=12&date=2026-10-03 */
    @GetMapping("/insight")
    public InsightResponse insight(@RequestParam String kind,
                                   @RequestParam Long id,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   HttpServletRequest http) {
        if (!limiter.allow("insight:" + clientIp(http), 60, WINDOW_MS)) {
            throw ApiException.tooMany("Too many requests. Please wait a few minutes.");
        }
        return insights.insight(kind, id, date);
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request, HttpServletRequest http) {
        if (!limiter.allow("chat:" + clientIp(http), 30, WINDOW_MS)) {
            throw ApiException.tooMany("You're sending messages very quickly. Please wait a few minutes.");
        }
        return chat.chat(request);
    }

    private String clientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");   // set by Render / Vercel / nginx proxies
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : http.getRemoteAddr();
    }
}
