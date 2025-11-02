package com.tripplanner.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller to handle SockJS info endpoint
 * SockJS clients make a GET request to /info before establishing WebSocket connection
 */
@RestController
public class SockJsInfoController {

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("websocket", true);
        info.put("origins", new String[]{"*:*"});
        info.put("cookie_needed", false);
        info.put("entropy", System.currentTimeMillis());
        return ResponseEntity.ok(info);
    }
}
