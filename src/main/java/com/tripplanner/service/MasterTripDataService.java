package com.tripplanner.service;

import com.tripplanner.api.dto.CreateItineraryReq;
import com.tripplanner.api.dto.EnhancedCreateItineraryReq;
import com.tripplanner.api.dto.MasterTripData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service to manage the master trip data structure.
 * This serves as the central hub for all trip information.
 */
@Service
public class MasterTripDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(MasterTripDataService.class);
    
    // In-memory storage for master data (in production, this would be in a database)
    private final Map<String, MasterTripData> masterDataStore = new HashMap<>();
    
    public MasterTripDataService() {
        // Constructor for dependency injection
    }
    
    /**
     * Create initial master trip data from the basic request.
     */
    public MasterTripData createMasterTripData(CreateItineraryReq request) {
        logger.info("Creating master trip data for destination: {}", request.getDestination());
        
        String tripId = UUID.randomUUID().toString();
        
        MasterTripData masterData = MasterTripData.builder()
                .id(tripId)
                .destination(request.getDestination())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .party(request.getParty())
                .budgetTier(request.getBudgetTier())
                .interests(request.getInterests())
                .constraints(request.getConstraints())
                .language(request.getLanguage())
                .status("generating")
                .createdAt(Instant.now().toString())
                .updatedAt(Instant.now().toString())
                .isPublic(false)
                .agentProgress(new HashMap<>())
                .build();
        
        // Store in memory
        masterDataStore.put(tripId, masterData);
        
        logger.info("Master trip data created with ID: {}", tripId);
        return masterData;
    }
    
    /**
     * Create initial master trip data from the enhanced request.
     */
    public MasterTripData createMasterTripData(EnhancedCreateItineraryReq request) {
        logger.info("Creating master trip data for destination: {}", request.getDestination());
        
        String tripId = UUID.randomUUID().toString();
        
        MasterTripData masterData = MasterTripData.builder()
                .id(tripId)
                .destination(request.getDestination())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .party(request.getParty())
                .budgetTier(request.getBudgetTier())
                .interests(request.getInterests())
                .constraints(request.getConstraints())
                .language(request.getLanguage())
                .travelers(convertTravelerProfiles(request.getTravelers()))
                .preferences(convertTravelPreferences(request.getPreferences()))
                .status("generating")
                .createdAt(Instant.now().toString())
                .updatedAt(Instant.now().toString())
                .isPublic(false)
                .agentProgress(new HashMap<>())
                .build();
        
        // Store in memory
        masterDataStore.put(tripId, masterData);
        
        logger.info("Master trip data created with ID: {}", tripId);
        return masterData;
    }
    
    /**
     * Get master trip data by ID.
     */
    public MasterTripData getMasterTripData(String tripId) {
        logger.info("Getting master trip data for ID: {}", tripId);
        return masterDataStore.get(tripId);
    }
    
    /**
     * Update master trip data.
     */
    public MasterTripData updateMasterTripData(String tripId, MasterTripData masterData) {
        logger.info("Updating master trip data for ID: {}", tripId);
        
        masterData.setUpdatedAt(Instant.now().toString());
        masterDataStore.put(tripId, masterData);
        
        return masterData;
    }
    
    /**
     * Update agent progress.
     */
    public void updateAgentProgress(String tripId, String agentName, MasterTripData.AgentProgress progress) {
        logger.info("Updating agent progress for trip: {}, agent: {}, status: {}", 
                   tripId, agentName, progress.getStatus());
        
        MasterTripData masterData = masterDataStore.get(tripId);
        if (masterData != null) {
            masterData.getAgentProgress().put(agentName, progress);
            masterData.setUpdatedAt(Instant.now().toString());
            masterDataStore.put(tripId, masterData);
        }
    }
    
    /**
     * Update destination analysis (Places Agent).
     */
    public void updateDestinationAnalysis(String tripId, MasterTripData.DestinationAnalysis analysis) {
        logger.info("Updating destination analysis for trip: {}", tripId);
        
        MasterTripData masterData = masterDataStore.get(tripId);
        if (masterData != null) {
            masterData.setDestinationAnalysis(analysis);
            masterData.setUpdatedAt(Instant.now().toString());
            masterDataStore.put(tripId, masterData);
        }
    }
    
    /**
     * Update accommodation options (Hotels Agent).
     */
    public void updateAccommodationOptions(String tripId, java.util.List<MasterTripData.AccommodationOption> options) {
        logger.info("Updating accommodation options for trip: {}, count: {}", tripId, options.size());
        
        MasterTripData masterData = masterDataStore.get(tripId);
        if (masterData != null) {
            masterData.setAccommodationOptions(options);
            masterData.setUpdatedAt(Instant.now().toString());
            masterDataStore.put(tripId, masterData);
        }
    }
    
    /**
     * Update flight options (Flights Agent).
     */
    public void updateFlightOptions(String tripId, java.util.List<MasterTripData.FlightOption> options) {
        logger.info("Updating flight options for trip: {}, count: {}", tripId, options.size());
        
        MasterTripData masterData = masterDataStore.get(tripId);
        if (masterData != null) {
            masterData.setFlightOptions(options);
            masterData.setUpdatedAt(Instant.now().toString());
            masterDataStore.put(tripId, masterData);
        }
    }
    
    /**
     * Update restaurant options (Food Agent).
     */
    public void updateRestaurantOptions(String tripId, java.util.List<MasterTripData.RestaurantOption> options) {
        logger.info("Updating restaurant options for trip: {}, count: {}", tripId, options.size());
        
        MasterTripData masterData = masterDataStore.get(tripId);
        if (masterData != null) {
            masterData.setRestaurantOptions(options);
            masterData.setUpdatedAt(Instant.now().toString());
            masterDataStore.put(tripId, masterData);
        }
    }
    
    /**
     * Update transport options (Transport Agent).
     */
    public void updateTransportOptions(String tripId, java.util.List<MasterTripData.TransportOption> options) {
        logger.info("Updating transport options for trip: {}, count: {}", tripId, options.size());
        
        MasterTripData masterData = masterDataStore.get(tripId);
        if (masterData != null) {
            masterData.setTransportOptions(options);
            masterData.setUpdatedAt(Instant.now().toString());
            masterDataStore.put(tripId, masterData);
        }
    }
    
    /**
     * Update daily itinerary (Planner Agent).
     */
    public void updateDailyItinerary(String tripId, java.util.List<MasterTripData.DayPlan> itinerary) {
        logger.info("Updating daily itinerary for trip: {}, days: {}", tripId, itinerary.size());
        
        MasterTripData masterData = masterDataStore.get(tripId);
        if (masterData != null) {
            masterData.setDailyItinerary(itinerary);
            masterData.setStatus("completed");
            masterData.setUpdatedAt(Instant.now().toString());
            masterDataStore.put(tripId, masterData);
        }
    }
    
    /**
     * Get all master trip data (for debugging).
     */
    public Map<String, MasterTripData> getAllMasterTripData() {
        return new HashMap<>(masterDataStore);
    }
    
    /**
     * Clear master trip data (for testing).
     */
    public void clearMasterTripData() {
        logger.info("Clearing all master trip data");
        masterDataStore.clear();
    }
    
    /**
     * Convert EnhancedCreateItineraryReq.TravelerProfile to MasterTripData.TravelerProfile.
     */
    private List<MasterTripData.TravelerProfile> convertTravelerProfiles(List<EnhancedCreateItineraryReq.TravelerProfile> enhancedProfiles) {
        if (enhancedProfiles == null) {
            return null;
        }
        
        return enhancedProfiles.stream()
                .map(enhanced -> MasterTripData.TravelerProfile.builder()
                        .name(enhanced.getName())
                        .age(enhanced.getAge())
                        .type(enhanced.getType())
                        .dietaryRestrictions(enhanced.getDietaryRestrictions())
                        .allergies(enhanced.getAllergies())
                        .mobilityLevel(enhanced.getMobilityLevel())
                        .interests(enhanced.getPersonalInterests())
                        .pace(enhanced.getPace())
                        .build())
                .toList();
    }
    
    /**
     * Convert EnhancedCreateItineraryReq.TravelPreferences to MasterTripData.TravelPreferences.
     */
    private MasterTripData.TravelPreferences convertTravelPreferences(EnhancedCreateItineraryReq.TravelPreferences enhancedPreferences) {
        if (enhancedPreferences == null) {
            return null;
        }
        
        return MasterTripData.TravelPreferences.builder()
                .pace(enhancedPreferences.getPace())
                .accommodationType(enhancedPreferences.getAccommodationType())
                .transportMode(enhancedPreferences.getTransportMode())
                .themes(enhancedPreferences.getThemes())
                .walkingTolerance(String.valueOf(enhancedPreferences.getWalkingTolerance()))
                .crowdTolerance(String.valueOf(enhancedPreferences.getCrowdTolerance()))
                .budgetFlexibility(enhancedPreferences.getBudgetFlexibility())
                .mustSee(enhancedPreferences.getMustSee())
                .avoid(enhancedPreferences.getAvoid())
                .timeOfDayPreference(enhancedPreferences.getTimeOfDayPreference())
                .seasonPreference(enhancedPreferences.getSeasonPreference())
                .build();
    }
}
