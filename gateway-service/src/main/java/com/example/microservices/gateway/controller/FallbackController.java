package com.example.microservices.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Target of the gateway circuit-breaker fallbacks (fallbackUri: forward:/fallback).
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public ResponseEntity<Map<String, String>> fallback() {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service temporarily unavailable",
                        "message", "The downstream service did not respond in time. Please try again later."));
    }
}
