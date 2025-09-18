package com.tripplanner.service;

import com.tripplanner.api.dto.ExtendReq;
import com.tripplanner.api.dto.ItineraryDto;
import com.tripplanner.security.GoogleUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for itinerary extension operations.
 */
@Service
public class ExtendService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExtendService.class);
    
    /**
     * Extend an existing itinerary.
     */
    public ItineraryDto extend(String id, ExtendReq request, GoogleUserPrincipal user) {
        logger.info("Extending itinerary: {} by {} days for user: {}", id, request.days(), user.getUserId());
        
        // TODO: Implement extension logic with LLM agents
        // This would involve calling the planner agent to extend the itinerary
        
        throw new UnsupportedOperationException("Extension functionality not yet implemented");
    }
}

