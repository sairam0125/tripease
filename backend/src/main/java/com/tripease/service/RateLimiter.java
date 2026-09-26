package com.tripease.service;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Tiny in-memory sliding-window limiter. Protects the OpenAI key from abuse on a public demo. */
@Component
public class RateLimiter {

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public boolean allow(String key, int maxRequests, long windowMs) {
        long now = System.currentTimeMillis();
        if (hits.size() > 10_000) {
            hits.clear(); // crude safety valve against unbounded growth
        }
        Deque<Long> queue = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() > windowMs) {
                queue.pollFirst();
            }
            if (queue.size() >= maxRequests) {
                return false;
            }
            queue.addLast(now);
            return true;
        }
    }
}
