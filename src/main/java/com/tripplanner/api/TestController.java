package com.tripplanner.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Simple test controller for debugging API connectivity.
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TestController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestController.class);
    
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        logger.info("=== PING REQUEST ===");
        logger.info("Request received at: {}", Instant.now());
        logger.info("Endpoint: /api/v1/ping");
        
        Map<String, Object> response = Map.of(
            "message", "pong",
            "timestamp", Instant.now().toString(),
            "status", "Backend is working!"
        );
        
        logger.info("=== PING RESPONSE ===");
        logger.info("Response: {}", response);
        logger.info("====================");
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> echo(@RequestBody Map<String, Object> payload) {
        logger.info("=== ECHO REQUEST ===");
        logger.info("Request received at: {}", Instant.now());
        logger.info("Payload: {}", payload);
        
        Map<String, Object> response = Map.of(
            "echo", payload,
            "timestamp", Instant.now().toString(),
            "message", "Echo endpoint working"
        );
        
        logger.info("=== ECHO RESPONSE ===");
        logger.info("Response: {}", response);
        logger.info("====================");
        
        return ResponseEntity.ok(response);
    }
}
