package com.tripplanner.api;

import com.tripplanner.api.dto.CreateItineraryReq;
import com.tripplanner.api.dto.NormalizedItinerary;
import com.tripplanner.service.agents.PlannerAgent;
import com.tripplanner.service.agents.BaseAgent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/test")
public class TestPlannerAgentController {
    
    private final PlannerAgent plannerAgent;
    
    public TestPlannerAgentController(PlannerAgent plannerAgent) {
        this.plannerAgent = plannerAgent;
    }
    
    @PostMapping("/planner-agent")
    public ResponseEntity<String> testPlannerAgent(@RequestBody CreateItineraryReq request) {
        try {
            String itineraryId = "it_test_" + System.currentTimeMillis();
            
            // Create agent request
            BaseAgent.AgentRequest<com.tripplanner.api.dto.ItineraryDto> agentRequest = 
                new BaseAgent.AgentRequest<>(request, com.tripplanner.api.dto.ItineraryDto.class);
            
            // Call PlannerAgent directly
            com.tripplanner.api.dto.ItineraryDto result = plannerAgent.execute(itineraryId, agentRequest);
            
            return ResponseEntity.ok("PlannerAgent executed successfully. Result: " + result.toString());
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("PlannerAgent failed: " + e.getMessage());
        }
    }
}
