package com.tripplanner.service.agents;

import com.tripplanner.api.dto.*;
import com.tripplanner.data.entity.Itinerary;
import com.tripplanner.data.repo.ItineraryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Orchestrates the execution of multiple agents to generate complete itineraries.
 */
@Service
public class AgentOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestrator.class);
    
    private final PlannerAgent plannerAgent;
    private final ItineraryRepository itineraryRepository;
    
    public AgentOrchestrator(PlannerAgent plannerAgent, ItineraryRepository itineraryRepository) {
        this.plannerAgent = plannerAgent;
        this.itineraryRepository = itineraryRepository;
    }
    
    /**
     * Generate a complete itinerary using multiple agents.
     */
    @Async
    public CompletableFuture<ItineraryDto> generateItinerary(String itineraryId, CreateItineraryReq request) {
        logger.info("Starting itinerary generation for ID: {}", itineraryId);
        
        try {
            // Create basic itinerary structure first
            ItineraryDto basicItinerary = createBasicItineraryDto(itineraryId, request);
            
            // Update the itinerary in the database with basic info
            updateItineraryInDatabase(itineraryId, basicItinerary);
            
            // Run the planner agent to generate the actual itinerary
            logger.info("Running PlannerAgent for itinerary ID: {}", itineraryId);
            logger.info("PlannerAgent instance: {}", plannerAgent);
            logger.info("Request data: {}", request);
            
            // Create agent request
            BaseAgent.AgentRequest<ItineraryDto> agentRequest = new BaseAgent.AgentRequest<>(request, ItineraryDto.class);
            ItineraryDto completedItinerary = plannerAgent.execute(itineraryId, agentRequest);
            
            // Update the itinerary with the completed data
            updateItineraryInDatabase(itineraryId, completedItinerary);
            
            logger.info("Itinerary generation completed for ID: {}", itineraryId);
            return CompletableFuture.completedFuture(completedItinerary);
            
        } catch (Exception e) {
            logger.error("Failed to generate itinerary for ID: {}", itineraryId, e);
            // Update status to failed
            updateItineraryStatus(itineraryId, "failed");
            throw new RuntimeException("Itinerary generation failed", e);
        }
    }
    
    private void updateItineraryInDatabase(String itineraryId, ItineraryDto itinerary) {
        try {
            logger.info("=== UPDATE ITINERARY IN DATABASE ===");
            logger.info("Itinerary ID: {}", itineraryId);
            logger.info("Itinerary DTO: {}", itinerary);
            logger.info("Days in DTO: {}", itinerary.getDays() != null ? itinerary.getDays().size() : 0);
            
            Long id = Long.parseLong(itineraryId);
            var entity = itineraryRepository.findById(id);
            if (entity.isPresent()) {
                var it = entity.get();
                
                // Update basic fields
                it.setStatus(itinerary.getStatus());
                it.setSummary(itinerary.getSummary());
                
                // Convert and update days if present
                if (itinerary.getDays() != null && !itinerary.getDays().isEmpty()) {
                    logger.info("Converting {} days from DTO to entity", itinerary.getDays().size());
                    
                    // Create new list to avoid lazy loading issues
                    var newDays = new ArrayList<Itinerary.ItineraryDay>();
                    
                    // Convert each day from DTO to entity
                    for (var dayDto : itinerary.getDays()) {
                        logger.info("Converting day {}: {}", dayDto.getDay(), dayDto.getLocation());
                        var dayEntity = convertDayDtoToEntity(dayDto, it);
                        newDays.add(dayEntity);
                    }
                    
                    // Set the new days list
                    it.setDays(newDays);
                    
                    logger.info("Successfully converted {} days to entity", itinerary.getDays().size());
                } else {
                    logger.warn("No days to convert - itinerary.getDays() is null or empty");
                }
                
                itineraryRepository.save(it);
                logger.info("Updated itinerary {} in database with {} days", itineraryId, 
                    itinerary.getDays() != null ? itinerary.getDays().size() : 0);
            } else {
                logger.error("Itinerary entity not found for ID: {}", itineraryId);
            }
        } catch (Exception e) {
            logger.error("Failed to update itinerary {} in database", itineraryId, e);
        }
    }
    
    private Itinerary.ItineraryDay convertDayDtoToEntity(ItineraryDayDto dayDto, Itinerary itinerary) {
        var dayEntity = new Itinerary.ItineraryDay();
        // Itinerary relationship is managed by JPA
        dayEntity.setDay(dayDto.getDay());
        dayEntity.setDate(dayDto.getDate());
        dayEntity.setLocation(dayDto.getLocation());
        dayEntity.setNotes(dayDto.getNotes());
        
        // Convert activities
        if (dayDto.getActivities() != null) {
            dayEntity.setActivities(new ArrayList<>());
            for (var activityDto : dayDto.getActivities()) {
                var activityEntity = convertActivityDtoToEntity(activityDto, dayEntity);
                dayEntity.getActivities().add(activityEntity);
            }
        }
        
        // Convert accommodation
        if (dayDto.getAccommodation() != null) {
            var accommodationEntity = convertAccommodationDtoToEntity(dayDto.getAccommodation(), dayEntity);
            dayEntity.setAccommodation(accommodationEntity);
        }
        
        // Convert transportation
        if (dayDto.getTransportation() != null) {
            dayEntity.setTransportation(new ArrayList<>());
            for (var transportDto : dayDto.getTransportation()) {
                var transportEntity = convertTransportationDtoToEntity(transportDto, dayEntity);
                dayEntity.getTransportation().add(transportEntity);
            }
        }
        
        // Convert meals
        if (dayDto.getMeals() != null) {
            dayEntity.setMeals(new ArrayList<>());
            for (var mealDto : dayDto.getMeals()) {
                var mealEntity = convertMealDtoToEntity(mealDto, dayEntity);
                dayEntity.getMeals().add(mealEntity);
            }
        }
        
        return dayEntity;
    }
    
    private Itinerary.Activity convertActivityDtoToEntity(ActivityDto activityDto, Itinerary.ItineraryDay dayEntity) {
        var activityEntity = new Itinerary.Activity();
        // Relationship managed by JPA
        activityEntity.setName(activityDto.name());
        activityEntity.setDescription(activityDto.description());
        activityEntity.setStartTime(activityDto.startTime());
        activityEntity.setEndTime(activityDto.endTime());
        activityEntity.setDuration(parseDuration(activityDto.duration()));
        activityEntity.setCategory(activityDto.category());
        activityEntity.setBookingRequired(activityDto.bookingRequired());
        activityEntity.setBookingUrl(activityDto.bookingUrl());
        activityEntity.setTips(activityDto.tips());
        
        // Convert location
        if (activityDto.location() != null) {
            var locationEntity = convertLocationDtoToEntity(activityDto.location());
            activityEntity.setLocation(locationEntity);
        }
        
        // Convert price
        if (activityDto.price() != null) {
            var priceEntity = convertPriceDtoToEntity(activityDto.price());
            activityEntity.setPrice(priceEntity);
        }
        
        return activityEntity;
    }
    
    private Itinerary.Accommodation convertAccommodationDtoToEntity(AccommodationDto accommodationDto, Itinerary.ItineraryDay dayEntity) {
        var accommodationEntity = new Itinerary.Accommodation();
        // Relationship managed by JPA
        accommodationEntity.setName(accommodationDto.name());
        accommodationEntity.setType(accommodationDto.type());
        accommodationEntity.setCheckIn(accommodationDto.checkIn());
        accommodationEntity.setCheckOut(accommodationDto.checkOut());
        accommodationEntity.setRating(accommodationDto.rating());
        accommodationEntity.setAmenities(accommodationDto.amenities());
        accommodationEntity.setBookingUrl(accommodationDto.bookingUrl());
        
        // Convert location
        if (accommodationDto.location() != null) {
            var locationEntity = convertLocationDtoToEntity(accommodationDto.location());
            accommodationEntity.setLocation(locationEntity);
        }
        
        // Convert price
        if (accommodationDto.price() != null) {
            var priceEntity = convertPriceDtoToEntity(accommodationDto.price());
            accommodationEntity.setPrice(priceEntity);
        }
        
        return accommodationEntity;
    }
    
    private Itinerary.Transportation convertTransportationDtoToEntity(TransportationDto transportDto, Itinerary.ItineraryDay dayEntity) {
        var transportEntity = new Itinerary.Transportation();
        // Relationship managed by JPA
        transportEntity.setMode(transportDto.mode());
        transportEntity.setDepartureTime(transportDto.departureTime());
        transportEntity.setArrivalTime(transportDto.arrivalTime());
        transportEntity.setDuration(parseDuration(transportDto.duration()));
        transportEntity.setProvider(transportDto.provider());
        transportEntity.setBookingUrl(transportDto.bookingUrl());
        transportEntity.setNotes(transportDto.notes());
        
        // Convert from/to locations
        if (transportDto.from() != null) {
            var fromLocationEntity = convertLocationDtoToEntity(transportDto.from());
            transportEntity.setFrom(fromLocationEntity);
        }
        
        if (transportDto.to() != null) {
            var toLocationEntity = convertLocationDtoToEntity(transportDto.to());
            transportEntity.setTo(toLocationEntity);
        }
        
        // Convert price
        if (transportDto.price() != null) {
            var priceEntity = convertPriceDtoToEntity(transportDto.price());
            transportEntity.setPrice(priceEntity);
        }
        
        return transportEntity;
    }
    
    private Itinerary.Meal convertMealDtoToEntity(MealDto mealDto, Itinerary.ItineraryDay dayEntity) {
        var mealEntity = new Itinerary.Meal();
        // Relationship managed by JPA
        mealEntity.setType(mealDto.type());
        mealEntity.setName(mealDto.name());
        mealEntity.setRestaurant(mealDto.restaurant());
        mealEntity.setTime(mealDto.time());
        mealEntity.setCuisine(mealDto.cuisine());
        mealEntity.setNotes(mealDto.notes());
        
        // Convert location
        if (mealDto.location() != null) {
            var locationEntity = convertLocationDtoToEntity(mealDto.location());
            mealEntity.setLocation(locationEntity);
        }
        
        // Convert price
        if (mealDto.price() != null) {
            var priceEntity = convertPriceDtoToEntity(mealDto.price());
            mealEntity.setPrice(priceEntity);
        }
        
        return mealEntity;
    }
    
    private Itinerary.Location convertLocationDtoToEntity(LocationDto locationDto) {
        var locationEntity = new Itinerary.Location();
        locationEntity.setName(locationDto.name());
        locationEntity.setAddress(locationDto.address());
        locationEntity.setLat(locationDto.lat());
        locationEntity.setLng(locationDto.lng());
        locationEntity.setPlaceId(locationDto.placeId());
        return locationEntity;
    }
    
    private Itinerary.Price convertPriceDtoToEntity(PriceDto priceDto) {
        var priceEntity = new Itinerary.Price();
        priceEntity.setAmount(priceDto.amount());
        priceEntity.setCurrency(priceDto.currency());
        priceEntity.setPer(priceDto.per());
        return priceEntity;
    }
    
    private int parseDuration(String duration) {
        if (duration == null || duration.trim().isEmpty()) {
            return 0;
        }
        
        try {
            // Try to parse as integer (minutes)
            return Integer.parseInt(duration.trim());
        } catch (NumberFormatException e) {
            // If it's a string like "1 hour", try to extract the number
            String[] parts = duration.toLowerCase().split("\\s+");
            for (String part : parts) {
                try {
                    int value = Integer.parseInt(part);
                    if (duration.toLowerCase().contains("hour")) {
                        return value * 60; // Convert hours to minutes
                    }
                    return value; // Assume minutes
                } catch (NumberFormatException ignored) {
                    // Continue to next part
                }
            }
            return 0; // Default to 0 if parsing fails
        }
    }
    
    private void updateItineraryStatus(String itineraryId, String status) {
        try {
            Long id = Long.parseLong(itineraryId);
            var entity = itineraryRepository.findById(id);
            if (entity.isPresent()) {
                var it = entity.get();
                it.setStatus(status);
                itineraryRepository.save(it);
                logger.info("Updated itinerary {} status to {}", itineraryId, status);
            }
        } catch (Exception e) {
            logger.error("Failed to update itinerary {} status", itineraryId, e);
        }
    }
    
    private ItineraryDto createBasicItineraryDto(String itineraryId, CreateItineraryReq request) {
        return ItineraryDto.builder()
                .id(itineraryId)
                .destination(request.getDestination())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .party(request.getParty())
                .budgetTier(request.getBudgetTier())
                .interests(request.getInterests())
                .constraints(request.getConstraints())
                .language(request.getLanguage())
                .summary("Your personalized itinerary for " + request.getDestination())
                .map(null)
                .days(null)
                .agentResults(null)
                .status("generating")
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .isPublic(false)
                .shareToken(null)
                .build();
    }
}