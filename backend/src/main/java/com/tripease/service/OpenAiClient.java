package com.tripease.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thin wrapper around OpenAI's Chat Completions API (llama-3.1).
 * Returns Optional.empty() when no key is configured or the call fails, so callers can
 * transparently fall back to rule-based answers.
 */
@Slf4j
@Component
public class OpenAiClient {

    private final RestClient rest;
    private final String apiKey;
    private final String model;

    public OpenAiClient(@Value("${openai.api-key:}") String apiKey,
                        @Value("${openai.model:gpt-3.5-turbo}") String model,
                        @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(25000);
        this.rest = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    public String model() {
        return model;
    }

    public static Map<String, String> message(String role, String content) {
        return Map.of("role", role, "content", content);
    }

    public Optional<String> complete(List<Map<String, String>> messages, double temperature, int maxTokens) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", messages,
                    "temperature", temperature,
                    "max_tokens", maxTokens);
            JsonNode response = rest.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String text = response == null ? "" : response.path("choices").path(0).path("message").path("content").asText("");
            return text.isBlank() ? Optional.empty() : Optional.of(text.trim());
        } catch (Exception e) {
            log.warn("OpenAI call failed, using rule-based fallback: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
