package com.tripplanner.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.data.entity.FirestoreItinerary;
import com.tripplanner.dto.NormalizedItinerary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for managing normalized JSON itineraries using DatabaseService abstraction.
 * This version can work with both H2 and Firestore databases.
 */
@Service
public class ItineraryJsonService {
    
    private static final Logger logger = LoggerFactory.getLogger(ItineraryJsonService.class);
    
    @Autowired
    private DatabaseService databaseService;
    
    private final ObjectMapper objectMapper;
    
    public ItineraryJsonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Create a new normalized itinerary.
     */
    public FirestoreItinerary createItinerary(NormalizedItinerary itinerary) {
        try {
            String json = objectMapper.writeValueAsString(itinerary);
            FirestoreItinerary entity = new FirestoreItinerary(itinerary.getItineraryId(), itinerary.getVersion(), json);
            return databaseService.save(entity);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize itinerary to JSON", e);
            throw new RuntimeException("Failed to create itinerary", e);
        }
    }
    
    /**
     * Update an existing itinerary.
     */
    public FirestoreItinerary updateItinerary(NormalizedItinerary itinerary) {
        try {
            String json = objectMapper.writeValueAsString(itinerary);
            FirestoreItinerary entity = new FirestoreItinerary(itinerary.getItineraryId(), itinerary.getVersion(), json);
            entity.updateTimestamp();
            return databaseService.save(entity);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize itinerary to JSON", e);
            throw new RuntimeException("Failed to update itinerary", e);
        }
    }
    
    /**
     * Get itinerary by ID.
     */
    public Optional<NormalizedItinerary> getItinerary(String id) {
        return databaseService.findById(id)
                .flatMap(this::deserializeItinerary);
    }
    
    /**
     * Get all itineraries ordered by updated timestamp.
     */
    public List<NormalizedItinerary> getAllItineraries() {
        return databaseService.findAllOrderByUpdatedAtDesc()
                .stream()
                .map(this::deserializeItinerary)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
    
    /**
     * Delete itinerary by ID.
     */
    public void deleteItinerary(String id) {
        databaseService.deleteById(id);
    }
    
    /**
     * Save a revision of the itinerary.
     */
    public void saveRevision(String itineraryId, NormalizedItinerary itinerary) {
        try {
            String json = objectMapper.writeValueAsString(itinerary);
            FirestoreItinerary entity = new FirestoreItinerary(itinerary.getItineraryId(), itinerary.getVersion(), json);
            databaseService.saveRevision(itineraryId, entity);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize itinerary revision to JSON", e);
            throw new RuntimeException("Failed to save revision", e);
        }
    }
    
    /**
     * Get revision by itinerary ID and version.
     */
    public Optional<NormalizedItinerary> getRevision(String itineraryId, Integer version) {
        return databaseService.findRevisionByItineraryIdAndVersion(itineraryId, version)
                .map(this::deserializeItinerary)
                .orElse(Optional.empty());
    }
    
    /**
     * Deserialize JSON string to NormalizedItinerary.
     */
    private Optional<NormalizedItinerary> deserializeItinerary(FirestoreItinerary entity) {
        try {
            return Optional.of(objectMapper.readValue(entity.getJson(), NormalizedItinerary.class));
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize itinerary JSON", e);
            return Optional.empty();
        }
    }
}
