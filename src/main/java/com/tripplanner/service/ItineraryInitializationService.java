package com.tripplanner.service;

import com.tripplanner.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for creating initial itinerary structure and establishing ownership.
 * 
 * This service extracts the initialization logic that was previously in AgentOrchestrator,
 * making it reusable by any orchestrator (Pipeline or future implementations).
 * 
 * Key responsibilities:
 * - Create initial normalized itinerary structure
 * - Save to Firestore (single source of truth)
 * - Establish user ownership synchronously
 * - Handle initialization errors gracefully
 * 
 * This method MUST complete synchronously before the API response is sent to avoid
 * race conditions where the user tries to access an itinerary that doesn't exist yet.
 */
@Service
public class ItineraryInitializationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ItineraryInitializationService.class);
    
    private final ItineraryJsonService itineraryJsonService;
    private final UserDataService userDataService;
    
    public ItineraryInitializationService(ItineraryJsonService itineraryJsonService,
                                         UserDataService userDataService) {
        this.itineraryJsonService = itineraryJsonService;
        this.userDataService = userDataService;
    }
    
    /**
     * Create initial itinerary and establish ownership synchronously.
     * This method must complete before the API response is sent to avoid race conditions.
     * 
     * @param itineraryId Unique identifier for the itinerary
     * @param request User's itinerary creation request
     * @param userId Authenticated user ID
     * @return NormalizedItinerary with initial structure
     * @throws RuntimeException if creation or ownership establishment fails
     */
    public NormalizedItinerary createInitialItinerary(String itineraryId, CreateItineraryReq request, String userId) {
        logger.info("=== ITINERARY INITIALIZATION SERVICE: CREATING INITIAL ITINERARY ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("Destination: {}", request.getDestination());
        logger.info("User ID: {}", userId);
        
        try {
            // Step 1: Create initial normalized itinerary structure
            logger.info("Step 1: Creating initial itinerary structure");
            NormalizedItinerary initialItinerary = createInitialStructure(itineraryId, request, userId);
            
            // Step 2: Save initial itinerary to ItineraryJsonService (single source of truth)
            logger.info("Step 2: Saving initial itinerary to ItineraryJsonService");
            itineraryJsonService.createItinerary(initialItinerary);
            
            // Step 3: SYNCHRONOUSLY save trip metadata to establish ownership
            logger.info("Step 3: Establishing ownership by saving trip metadata");
            TripMetadata tripMetadata = new TripMetadata(request, initialItinerary);
            userDataService.saveUserTripMetadata(userId, tripMetadata);
            
            logger.info("=== INITIAL ITINERARY CREATED AND OWNERSHIP ESTABLISHED ===");
            logger.info("Itinerary ID: {}", itineraryId);
            logger.info("User can now access /itineraries/{}/json endpoint", itineraryId);
            
            return initialItinerary;
            
        } catch (Exception e) {
            logger.error("Failed to create initial itinerary for ID: {}, User ID: {}", itineraryId, userId, e);
            throw new RuntimeException("Initial itinerary creation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create the initial normalized itinerary structure.
     * Internal helper method.
     * 
     * @param itineraryId Unique identifier for the itinerary
     * @param request User's itinerary creation request
     * @param userId Authenticated user ID
     * @return NormalizedItinerary with initial structure (empty days array)
     */
    private NormalizedItinerary createInitialStructure(String itineraryId, CreateItineraryReq request, String userId) {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId(itineraryId);
        itinerary.setVersion(1);
        itinerary.setUserId(userId);
        itinerary.setCreatedAt(System.currentTimeMillis());
        itinerary.setUpdatedAt(System.currentTimeMillis());
        itinerary.setSummary("Your personalized itinerary for " + request.getDestination());
        itinerary.setCurrency("INR");
        itinerary.setThemes(request.getInterests() != null ? request.getInterests() : new ArrayList<>());
        itinerary.setDays(new ArrayList<>());

        // Set explicit trip meta
        itinerary.setOrigin(request.getStartLocation());
        itinerary.setDestination(request.getDestination());
        if (request.getStartDate() != null) {
            itinerary.setStartDate(request.getStartDate().toString());
        }
        if (request.getEndDate() != null) {
            itinerary.setEndDate(request.getEndDate().toString());
        }
        
        // Set settings
        ItinerarySettings settings = new ItinerarySettings();
        settings.setAutoApply(false);
        settings.setDefaultScope("trip");
        itinerary.setSettings(settings);
        
        // Set agent status
        Map<String, AgentStatus> agents = new HashMap<>();
        agents.put("planner", new AgentStatus());
        agents.put("enrichment", new AgentStatus());
        itinerary.setAgents(agents);
        
        return itinerary;
    }
}
