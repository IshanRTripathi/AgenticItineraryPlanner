package com.tripplanner.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Health check controller for testing API connectivity.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString(),
            "service", "Agentic Itinerary Planner",
            "version", "1.0.0"
        ));
    }
    
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        return ResponseEntity.ok(Map.of(
            "message", "Backend is working!",
            "timestamp", Instant.now().toString()
        ));
    }
}
