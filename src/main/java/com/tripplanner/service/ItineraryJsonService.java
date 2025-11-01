package com.tripplanner.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.data.entity.FirestoreItinerary;
import com.tripplanner.dto.*;
import com.tripplanner.exception.ItineraryNotFoundException;
import com.tripplanner.exception.VersionConflictException;
import com.tripplanner.exception.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing normalized JSON itineraries using Firestore database.
 * Provides serialization, deserialization, and storage operations for itineraries.
 */
@Service
public class ItineraryJsonService {
    
    private static final Logger logger = LoggerFactory.getLogger(ItineraryJsonService.class);
    
    @Autowired
    private DatabaseService databaseService;
    
    @Autowired
    private MapBoundsCalculator mapBoundsCalculator;
    
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
            // CRITICAL: Validate userId is not null
            if (itinerary.getUserId() == null || itinerary.getUserId().trim().isEmpty()) {
                logger.error("Attempted to save revision for itinerary {} with null or empty userId", itineraryId);
                logger.error("Stack trace for null userId:", new Exception("UserId is null"));
                throw new IllegalStateException("Cannot save itinerary revision without userId. Itinerary: " + itineraryId);
            }
            
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
     * Deserialize JSON string to NormalizedItinerary and populate map fields.
     */
    private Optional<NormalizedItinerary> deserializeItinerary(FirestoreItinerary entity) {
        try {
            NormalizedItinerary itinerary = objectMapper.readValue(entity.getJson(), NormalizedItinerary.class);
            
            // Populate map fields if not already present
            populateMapFields(itinerary);
            
            return Optional.of(itinerary);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize itinerary JSON", e);
            return Optional.empty();
        }
    }
    
    /**
     * Save master itinerary with unified structure to masterItinerary.json.
     * Includes all agentData, workflow, revisions, and chat sections.
     */
    public void saveMasterItinerary(String itineraryId, NormalizedItinerary itinerary) {
        logger.debug("Saving master itinerary for {}", itineraryId);
        
        try {
            // Validate input parameters
            if (itineraryId == null || itineraryId.trim().isEmpty()) {
                throw new IllegalArgumentException("Itinerary ID cannot be null or empty");
            }
            if (itinerary == null) {
                throw new IllegalArgumentException("Itinerary cannot be null");
            }
            
            // CRITICAL: Validate userId is not null
            if (itinerary.getUserId() == null || itinerary.getUserId().trim().isEmpty()) {
                logger.error("Attempted to save itinerary {} with null or empty userId", itineraryId);
                logger.error("Stack trace for null userId:", new Exception("UserId is null"));
                throw new IllegalStateException("Cannot save itinerary without userId. Itinerary: " + itineraryId);
            }
            
            // Initialize unified structure if not already done
            itinerary.initializeUnifiedStructure();
            
            // Update timestamps and version before saving
            itinerary.setUpdatedAt(System.currentTimeMillis());
            
            // Populate map fields
            populateMapFields(itinerary);
            
            // Serialize unified structure to JSON with error handling
            String json = serializeItineraryWithRetry(itinerary);
            
            // Store as masterItinerary document in versions subcollection
            // Path format: itineraries/{itineraryId}/versions/master
            String path = "itineraries/" + itineraryId + "/versions/master";
            
            // Use FirestoreDatabaseService's flexible document storage with retry
            if (databaseService instanceof FirestoreDatabaseService) {
                saveDocumentWithRetry((FirestoreDatabaseService) databaseService, path, json);
            } else {
                // Fallback to regular save for other database types
                updateItinerary(itinerary);
            }
            
            logger.info("Successfully saved master itinerary for {} with version {}", itineraryId, itinerary.getVersion());
            
        } catch (SerializationException e) {
            logger.error("Serialization failed for master itinerary {}", itineraryId, e);
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid arguments for saving master itinerary {}", itineraryId, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error saving master itinerary {}", itineraryId, e);
            throw new RuntimeException("Failed to save master itinerary: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get master itinerary with unified structure from masterItinerary.json.
     * Returns Optional for null safety.
     */
    public Optional<NormalizedItinerary> getMasterItinerary(String itineraryId) {
        logger.debug("Getting master itinerary for {}", itineraryId);
        
        try {
            // Validate input
            if (itineraryId == null || itineraryId.trim().isEmpty()) {
                throw new IllegalArgumentException("Itinerary ID cannot be null or empty");
            }
            
            // Retrieve masterItinerary.json from Firebase with retry
            String path = "itineraries/" + itineraryId;
            
            if (databaseService instanceof FirestoreDatabaseService) {
                Optional<Map<String, Object>> documentOpt = 
                    getDocumentWithRetry((FirestoreDatabaseService) databaseService, path);
                
                if (documentOpt.isPresent()) {
                    Map<String, Object> document = documentOpt.get();
                    String json = (String) document.get("json");
                    
                    if (json != null && !json.trim().isEmpty()) {
                        NormalizedItinerary itinerary = deserializeItineraryWithRetry(json);
                        
                        // Ensure unified structure is initialized
                        itinerary.initializeUnifiedStructure();
                        
                        // Populate map fields if needed
                        populateMapFields(itinerary);
                        
                        logger.debug("Successfully retrieved master itinerary for {} with version {}", 
                                   itineraryId, itinerary.getVersion());
                        return Optional.of(itinerary);
                    } else {
                        logger.warn("Master itinerary document exists but JSON content is empty for {}", itineraryId);
                    }
                } else {
                    logger.debug("Master itinerary not found for {}", itineraryId);
                }
            } else {
                // Fallback to regular get for other database types
                logger.debug("Using fallback method for non-Firestore database");
                return getItinerary(itineraryId);
            }
            
            return Optional.empty();
            
        } catch (SerializationException e) {
            logger.error("Deserialization failed for master itinerary {}", itineraryId, e);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid arguments for getting master itinerary {}", itineraryId, e);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error getting master itinerary {}", itineraryId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Update master itinerary with version increment and timestamp update.
     * Uses atomic update operations in Firebase with version conflict detection.
     */
    public void updateMasterItinerary(String itineraryId, NormalizedItinerary itinerary) {
        logger.debug("Updating master itinerary for {}", itineraryId);
        
        try {
            // Validate itinerary data before saving
            validateItineraryData(itinerary);
            
            // Check for version conflicts by loading current version
            Optional<NormalizedItinerary> currentOpt = getMasterItinerary(itineraryId);
            if (currentOpt.isPresent()) {
                NormalizedItinerary current = currentOpt.get();
                if (!current.getVersion().equals(itinerary.getVersion())) {
                    throw new VersionConflictException(
                        itineraryId, 
                        itinerary.getVersion(), 
                        current.getVersion(),
                        "Concurrent modification detected. Please reload and try again."
                    );
                }
            }
            
            // Increment version number before update
            Integer newVersion = itinerary.getVersion() + 1;
            itinerary.setVersion(newVersion);
            
            // Update updatedAt timestamp
            itinerary.setUpdatedAt(System.currentTimeMillis());
            
            // Save using the master itinerary method
            saveMasterItinerary(itineraryId, itinerary);
            
            logger.info("Successfully updated master itinerary {} to version {}", itineraryId, newVersion);
            
        } catch (VersionConflictException e) {
            logger.warn("Version conflict updating master itinerary {}: {}", itineraryId, e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid data for updating master itinerary {}: {}", itineraryId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error updating master itinerary {}", itineraryId, e);
            throw new RuntimeException("Failed to update master itinerary: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate itinerary data before storage.
     * Ensures required fields are present and valid.
     */
    private void validateItineraryData(NormalizedItinerary itinerary) {
        if (itinerary == null) {
            throw new IllegalArgumentException("Itinerary cannot be null");
        }
        
        if (itinerary.getItineraryId() == null || itinerary.getItineraryId().trim().isEmpty()) {
            throw new IllegalArgumentException("Itinerary ID is required");
        }
        
        if (itinerary.getUserId() == null || itinerary.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        
        if (itinerary.getVersion() == null || itinerary.getVersion() < 0) {
            throw new IllegalArgumentException("Valid version number is required");
        }
        
        // Validate days structure if present
        if (itinerary.getDays() != null) {
            for (int i = 0; i < itinerary.getDays().size(); i++) {
                var day = itinerary.getDays().get(i);
                if (day.getDayNumber() == null) {
                    throw new IllegalArgumentException("Day number is required for day at index " + i);
                }
            }
        }
    }
    
    /**
     * Populate map bounds and country centroid for the itinerary.
     */
    private void populateMapFields(NormalizedItinerary itinerary) {
        if (itinerary == null || itinerary.getDays() == null) {
            return;
        }
        
        // Extract all nodes from all days
        List<NormalizedNode> allNodes = itinerary.getDays().stream()
                .flatMap(day -> day.getNodes() != null ? day.getNodes().stream() : java.util.stream.Stream.empty())
                .collect(java.util.stream.Collectors.toList());
        
        // Calculate and set map bounds if not already present
        if (itinerary.getMapBounds() == null) {
            MapBounds bounds = mapBoundsCalculator.calculateBounds(allNodes);
            itinerary.setMapBounds(bounds);
            logger.debug("Populated map bounds for itinerary {}: {}", itinerary.getItineraryId(), bounds);
        }
        
        // Calculate and set country centroid if not already present
        if (itinerary.getCountryCentroid() == null) {
            Coordinates centroid = mapBoundsCalculator.calculateCentroid(allNodes);
            itinerary.setCountryCentroid(centroid);
            logger.debug("Populated country centroid for itinerary {}: {}", itinerary.getItineraryId(), centroid);
        }
    }
    
    /**
     * Serialize itinerary to JSON with retry logic for transient failures.
     */
    private String serializeItineraryWithRetry(NormalizedItinerary itinerary) {
        int maxRetries = 3;
        int retryDelay = 100; // milliseconds
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return objectMapper.writeValueAsString(itinerary);
            } catch (JsonProcessingException e) {
                if (attempt == maxRetries) {
                    throw new SerializationException("serialize", "NormalizedItinerary", 
                        "Failed to serialize itinerary after " + maxRetries + " attempts", e);
                }
                
                logger.warn("Serialization attempt {} failed, retrying in {}ms", attempt, retryDelay);
                try {
                    Thread.sleep(retryDelay);
                    retryDelay *= 2; // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SerializationException("serialize", "NormalizedItinerary", 
                        "Serialization interrupted", ie);
                }
            }
        }
        
        throw new SerializationException("serialize", "NormalizedItinerary", 
            "Unexpected error in serialization retry logic");
    }
    
    /**
     * Deserialize JSON to itinerary with retry logic for transient failures.
     */
    private NormalizedItinerary deserializeItineraryWithRetry(String json) {
        int maxRetries = 3;
        int retryDelay = 100; // milliseconds
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return objectMapper.readValue(json, NormalizedItinerary.class);
            } catch (JsonProcessingException e) {
                if (attempt == maxRetries) {
                    throw new SerializationException("deserialize", "NormalizedItinerary", 
                        "Failed to deserialize itinerary after " + maxRetries + " attempts", e);
                }
                
                logger.warn("Deserialization attempt {} failed, retrying in {}ms", attempt, retryDelay);
                try {
                    Thread.sleep(retryDelay);
                    retryDelay *= 2; // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SerializationException("deserialize", "NormalizedItinerary", 
                        "Deserialization interrupted", ie);
                }
            }
        }
        
        throw new SerializationException("deserialize", "NormalizedItinerary", 
            "Unexpected error in deserialization retry logic");
    }
    
    /**
     * Save document with retry logic for transient Firebase errors.
     */
    private void saveDocumentWithRetry(FirestoreDatabaseService firestoreService, String path, String json) {
        int maxRetries = 5;
        int retryDelay = 1000; // milliseconds
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                firestoreService.saveDocument(path, json);
                return; // Success
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    throw new RuntimeException("Failed to save document after " + maxRetries + " attempts", e);
                }
                
                // Check if it's a retryable error
                if (isRetryableError(e)) {
                    logger.warn("Save attempt {} failed with retryable error, retrying in {}ms: {}", 
                              attempt, retryDelay, e.getMessage());
                    try {
                        Thread.sleep(retryDelay);
                        retryDelay = Math.min(retryDelay * 2, 10000); // Exponential backoff with max 10s
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Save operation interrupted", ie);
                    }
                } else {
                    // Non-retryable error, fail immediately
                    throw new RuntimeException("Non-retryable error saving document", e);
                }
            }
        }
    }
    
    /**
     * Get document with retry logic for transient Firebase errors.
     */
    private Optional<Map<String, Object>> getDocumentWithRetry(FirestoreDatabaseService firestoreService, String path) {
        int maxRetries = 5;
        int retryDelay = 1000; // milliseconds
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return firestoreService.getDocument(path);
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    throw new RuntimeException("Failed to get document after " + maxRetries + " attempts", e);
                }
                
                // Check if it's a retryable error
                if (isRetryableError(e)) {
                    logger.warn("Get attempt {} failed with retryable error, retrying in {}ms: {}", 
                              attempt, retryDelay, e.getMessage());
                    try {
                        Thread.sleep(retryDelay);
                        retryDelay = Math.min(retryDelay * 2, 10000); // Exponential backoff with max 10s
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Get operation interrupted", ie);
                    }
                } else {
                    // Non-retryable error, fail immediately
                    throw new RuntimeException("Non-retryable error getting document", e);
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Update agent data for a specific agent in an itinerary.
     */
    public void updateAgentData(String itineraryId, String agentName, Object agentData) {
        logger.debug("Updating agent data for itinerary: {}, agent: {}", itineraryId, agentName);
        
        try {
            Optional<NormalizedItinerary> itineraryOpt = getItinerary(itineraryId);
            if (itineraryOpt.isPresent()) {
                NormalizedItinerary itinerary = itineraryOpt.get();
                
                // Get or create agent data section for this agent
                AgentDataSection agentDataSection = itinerary.getAgentData()
                    .computeIfAbsent(agentName, k -> new AgentDataSection());
                
                // Store the data flexibly
                agentDataSection.setAgentData(agentName, agentData);
                
                // Update timestamp and save
                itinerary.setUpdatedAt(System.currentTimeMillis());
                updateMasterItinerary(itineraryId, itinerary);
                
                logger.info("Successfully updated agent data for itinerary: {}, agent: {}", 
                           itineraryId, agentName);
            } else {
                logger.warn("Itinerary not found for agent data update: {}", itineraryId);
                throw new ItineraryNotFoundException("Itinerary not found: ", itineraryId, "userId");
            }
            
        } catch (Exception e) {
            logger.error("Failed to update agent data for itinerary: {}, agent: {}", 
                        itineraryId, agentName, e);
            throw new RuntimeException("Failed to update agent data", e);
        }
    }
    
    /**
     * Retrieve specific agent data from an itinerary.
     */
    public Optional<Object> getAgentData(String itineraryId, String agentName) {
        logger.debug("Retrieving agent data for itinerary: {}, agent: {}", itineraryId, agentName);
        
        try {
            return getItinerary(itineraryId)
                .map(itinerary -> itinerary.getAgentData().get(agentName))
                .map(agentDataSection -> agentDataSection.getAgentData(agentName));
                
        } catch (Exception e) {
            logger.error("Failed to retrieve agent data for itinerary: {}, agent: {}", 
                        itineraryId, agentName, e);
            return Optional.empty();
        }
    }
    
    /**
     * Retrieve specific agent data with type conversion.
     */
    public <T> Optional<T> getAgentData(String itineraryId, String agentName, Class<T> targetType) {
        logger.debug("Retrieving typed agent data for itinerary: {}, agent: {}, type: {}", 
                    itineraryId, agentName, targetType.getSimpleName());
        
        try {
            return getItinerary(itineraryId)
                .map(itinerary -> itinerary.getAgentData().get(agentName))
                .map(agentDataSection -> agentDataSection.getAgentData(agentName, targetType));
                
        } catch (Exception e) {
            logger.error("Failed to retrieve typed agent data for itinerary: {}, agent: {}, type: {}", 
                        itineraryId, agentName, targetType.getSimpleName(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Check if agent data exists for a specific agent in an itinerary.
     */
    public boolean hasAgentData(String itineraryId, String agentName) {
        logger.debug("Checking agent data existence for itinerary: {}, agent: {}", itineraryId, agentName);
        
        try {
            return getItinerary(itineraryId)
                .map(itinerary -> itinerary.getAgentData().containsKey(agentName) && 
                                 itinerary.getAgentData().get(agentName).hasAgentData(agentName))
                .orElse(false);
                
        } catch (Exception e) {
            logger.error("Failed to check agent data existence for itinerary: {}, agent: {}", 
                        itineraryId, agentName, e);
            return false;
        }
    }
    
    /**
     * Remove agent data for a specific agent from an itinerary.
     */
    public void removeAgentData(String itineraryId, String agentName) {
        logger.debug("Removing agent data for itinerary: {}, agent: {}", itineraryId, agentName);
        
        try {
            Optional<NormalizedItinerary> itineraryOpt = getItinerary(itineraryId);
            if (itineraryOpt.isPresent()) {
                NormalizedItinerary itinerary = itineraryOpt.get();
                
                // Remove agent data section
                AgentDataSection removed = itinerary.getAgentData().remove(agentName);
                
                if (removed != null) {
                    // Update timestamp and save
                    itinerary.setUpdatedAt(System.currentTimeMillis());
                    updateMasterItinerary(itineraryId, itinerary);
                    
                    logger.info("Successfully removed agent data for itinerary: {}, agent: {}", 
                               itineraryId, agentName);
                } else {
                    logger.debug("No agent data found to remove for itinerary: {}, agent: {}", 
                                itineraryId, agentName);
                }
            } else {
                logger.warn("Itinerary not found for agent data removal: {}", itineraryId);
            }
            
        } catch (Exception e) {
            logger.error("Failed to remove agent data for itinerary: {}, agent: {}", 
                        itineraryId, agentName, e);
            throw new RuntimeException("Failed to remove agent data", e);
        }
    }
    
    /**
     * Get all agent names that have data in an itinerary.
     */
    public Set<String> getAgentNames(String itineraryId) {
        logger.debug("Getting agent names for itinerary: {}", itineraryId);
        
        try {
            return getItinerary(itineraryId)
                .map(itinerary -> itinerary.getAgentData().keySet())
                .orElse(Collections.emptySet());
                
        } catch (Exception e) {
            logger.error("Failed to get agent names for itinerary: {}", itineraryId, e);
            return Collections.emptySet();
        }
    }
    
    /**
     * Check if an error is retryable (transient Firebase errors).
     */
    private boolean isRetryableError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        
        String lowerMessage = message.toLowerCase();
        
        // Common retryable Firebase errors
        return lowerMessage.contains("timeout") ||
               lowerMessage.contains("unavailable") ||
               lowerMessage.contains("internal error") ||
               lowerMessage.contains("service unavailable") ||
               lowerMessage.contains("deadline exceeded") ||
               lowerMessage.contains("connection") ||
               e instanceof java.net.SocketTimeoutException ||
               e instanceof java.io.IOException;
    }
}
