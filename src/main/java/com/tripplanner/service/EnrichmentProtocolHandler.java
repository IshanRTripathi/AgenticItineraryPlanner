package com.tripplanner.service;

import com.tripplanner.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Handles the standardized ENRICHMENT request/response protocol.
 * Manages request processing, caching, and response coordination.
 */
@Service
public class EnrichmentProtocolHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(EnrichmentProtocolHandler.class);
    
    private final PlaceEnrichmentService placeEnrichmentService;
    private final GooglePlacesService googlePlacesService;
    private final PlaceRegistry placeRegistry;
    private final IdempotencyManager idempotencyManager;
    
    // Request tracking for monitoring
    private final Map<String, EnrichmentRequest> activeRequests = new ConcurrentHashMap<>();
    
    public EnrichmentProtocolHandler(PlaceEnrichmentService placeEnrichmentService,
                                   GooglePlacesService googlePlacesService,
                                   PlaceRegistry placeRegistry,
                                   IdempotencyManager idempotencyManager) {
        this.placeEnrichmentService = placeEnrichmentService;
        this.googlePlacesService = googlePlacesService;
        this.placeRegistry = placeRegistry;
        this.idempotencyManager = idempotencyManager;
    }
    
    /**
     * Process an ENRICHMENT request asynchronously.
     * 
     * @param request The ENRICHMENT request to process
     * @return CompletableFuture containing the ENRICHMENT response
     */
    public CompletableFuture<EnrichmentResponse> processRequest(EnrichmentRequest request) {
        logger.debug("Processing ENRICHMENT request: {}", request);
        
        // Validate request
        if (!isValidRequest(request)) {
            return CompletableFuture.completedFuture(
                EnrichmentResponse.failure(request.getTraceId(), request.getIdempotencyKey(),
                    Arrays.asList(new EnrichmentResponse.EnrichmentError(
                        "INVALID_REQUEST", "Request validation failed", null))));
        }
        
        // Check cache first
        EnrichmentResponse cachedResponse = getCachedResponse(request.getIdempotencyKey());
        if (cachedResponse != null) {
            logger.debug("Returning cached response for request: {}", request.getIdempotencyKey());
            return CompletableFuture.completedFuture(cachedResponse);
        }
        
        // Track active request
        activeRequests.put(request.getTraceId(), request);
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                EnrichmentResponse response = processRequestInternal(request, startTime);
                
                // Cache successful responses
                if (response.isSuccessful()) {
                    cacheResponse(request.getIdempotencyKey(), response);
                }
                
                return response;
                
            } catch (Exception e) {
                logger.error("Failed to process ENRICHMENT request: {}", request, e);
                
                EnrichmentResponse errorResponse = EnrichmentResponse.failure(
                    request.getTraceId(), request.getIdempotencyKey(),
                    Arrays.asList(new EnrichmentResponse.EnrichmentError(
                        "PROCESSING_ERROR", "Request processing failed: " + e.getMessage(), null)));
                
                errorResponse.setProcessingTime(startTime);
                return errorResponse;
                
            } finally {
                // Remove from active requests
                activeRequests.remove(request.getTraceId());
            }
        });
    }
    
    /**
     * Process ENRICHMENT request synchronously.
     * 
     * @param request The ENRICHMENT request to process
     * @return The ENRICHMENT response
     */
    public EnrichmentResponse processRequestSync(EnrichmentRequest request) {
        try {
            return processRequest(request).get();
        } catch (Exception e) {
            logger.error("Failed to process ENRICHMENT request synchronously: {}", request, e);
            return EnrichmentResponse.failure(request.getTraceId(), request.getIdempotencyKey(),
                Arrays.asList(new EnrichmentResponse.EnrichmentError(
                    "SYNC_PROCESSING_ERROR", "Synchronous processing failed: " + e.getMessage(), null)));
        }
    }
    
    /**
     * Get cached response if available and valid.
     */
    public EnrichmentResponse getCachedResponse(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        
        Optional<IdempotencyManager.IdempotencyRecord> record = 
            idempotencyManager.getExistingOperation(idempotencyKey);
        
        if (record.isPresent()) {
            EnrichmentResponse cached = (EnrichmentResponse) record.get().getResult();
            if (cached != null) {
                // Mark as cache hit
                cached.setCacheHit(true);
                return cached;
            }
        }
        
        return null;
    }
    
    /**
     * Get statistics about the ENRICHMENT protocol.
     */
    public EnrichmentProtocolStats getStats() {
        return new EnrichmentProtocolStats(
            activeRequests.size(),
            0, // Cache size managed by IdempotencyManager
            0  // Total cache entries managed by IdempotencyManager
        );
    }
    
    /**
     * Clear expired cache entries.
     * Note: Cache cleanup is now handled by IdempotencyManager.
     */
    public void cleanupCache() {
        // Cache cleanup is now handled by IdempotencyManager
        idempotencyManager.cleanupExpiredRecords();
    }
    
    /**
     * Internal request processing logic.
     */
    private EnrichmentResponse processRequestInternal(EnrichmentRequest request, long startTime) {
        logger.debug("Processing ENRICHMENT request internally: {}", request.getRequestType());
        
        switch (request.getRequestType()) {
            case PLACE_DETAILS:
                return processPlaceDetailsRequest(request, startTime);
            case PLACE_VALIDATION:
                return processPlaceValidationRequest(request, startTime);
            case TIMING_OPTIMIZATION:
                return processTimingOptimizationRequest(request, startTime);
            case CANONICAL_MAPPING:
                return processCanonicalMappingRequest(request, startTime);
            case CONTENT_ENHANCEMENT:
                return processContentEnhancementRequest(request, startTime);
            default:
                EnrichmentResponse response = EnrichmentResponse.failure(
                    request.getTraceId(), request.getIdempotencyKey(),
                    Arrays.asList(new EnrichmentResponse.EnrichmentError(
                        "UNSUPPORTED_TYPE", "Unsupported ENRICHMENT type: " + request.getRequestType(), null)));
                response.setProcessingTime(startTime);
                return response;
        }
    }
    
    /**
     * Process place details ENRICHMENT request.
     */
    private EnrichmentResponse processPlaceDetailsRequest(EnrichmentRequest request, long startTime) {
        try {
            // Get node location to determine place ID
            String placeId = extractPlaceIdFromContext(request);
            if (placeId == null) {
                EnrichmentResponse response = EnrichmentResponse.failure(
                    request.getTraceId(), request.getIdempotencyKey(),
                    Arrays.asList(new EnrichmentResponse.EnrichmentError(
                        "NO_PLACE_ID", "No place ID found for ENRICHMENT", null)));
                response.setProcessingTime(startTime);
                return response;
            }
            
            // Get place details from Google Places
            PlaceDetails placeDetails = googlePlacesService.getPlaceDetails(placeId);
            
            Map<String, Object> enrichedData = new HashMap<>();
            List<EnrichmentResponse.PartialResult> partialResults = new ArrayList<>();
            double overallConfidence = 0.9; // High confidence for Google Places data
            
            // Process requested fields
            for (String field : request.getRequestedFields()) {
                switch (field) {
                    case "photos":
                        if (placeDetails.getPhotos() != null && !placeDetails.getPhotos().isEmpty()) {
                            enrichedData.put("photos", placeDetails.getPhotos());
                        } else {
                            partialResults.add(new EnrichmentResponse.PartialResult(
                                "photos", null, 0.0, "google_places"));
                        }
                        break;
                    case "reviews":
                        if (placeDetails.getReviews() != null && !placeDetails.getReviews().isEmpty()) {
                            enrichedData.put("reviews", placeDetails.getReviews());
                        } else {
                            partialResults.add(new EnrichmentResponse.PartialResult(
                                "reviews", null, 0.0, "google_places"));
                        }
                        break;
                    case "rating":
                        if (placeDetails.getRating() != null) {
                            enrichedData.put("rating", placeDetails.getRating());
                        }
                        break;
                    case "openingHours":
                        if (placeDetails.getOpeningHours() != null) {
                            enrichedData.put("openingHours", placeDetails.getOpeningHours());
                        }
                        break;
                }
            }
            
            EnrichmentResponse response;
            if (!enrichedData.isEmpty()) {
                if (partialResults.isEmpty()) {
                    response = EnrichmentResponse.success(request.getTraceId(), 
                        request.getIdempotencyKey(), enrichedData, overallConfidence);
                } else {
                    response = EnrichmentResponse.partialSuccess(request.getTraceId(),
                        request.getIdempotencyKey(), partialResults, overallConfidence * 0.8);
                    response.setEnrichedData(enrichedData);
                }
            } else {
                response = EnrichmentResponse.failure(request.getTraceId(), request.getIdempotencyKey(),
                    Arrays.asList(new EnrichmentResponse.EnrichmentError(
                        "NO_DATA", "No ENRICHMENT data available", null)));
            }
            
            response.setDataSource("google_places");
            response.setProcessingTime(startTime);
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to process place details request", e);
            EnrichmentResponse response = EnrichmentResponse.failure(
                request.getTraceId(), request.getIdempotencyKey(),
                Arrays.asList(new EnrichmentResponse.EnrichmentError(
                    "PROCESSING_ERROR", "Failed to get place details: " + e.getMessage(), null)));
            response.setProcessingTime(startTime);
            return response;
        }
    }
    
    /**
     * Process place validation ENRICHMENT request.
     */
    private EnrichmentResponse processPlaceValidationRequest(EnrichmentRequest request, long startTime) {
        try {
            String placeId = extractPlaceIdFromContext(request);
            if (placeId == null) {
                EnrichmentResponse response = EnrichmentResponse.failure(
                    request.getTraceId(), request.getIdempotencyKey(),
                    Arrays.asList(new EnrichmentResponse.EnrichmentError(
                        "NO_PLACE_ID", "No place ID found for validation", null)));
                response.setProcessingTime(startTime);
                return response;
            }
            
            // Validate place exists
            PlaceDetails placeDetails = googlePlacesService.getPlaceDetails(placeId);
            
            Map<String, Object> enrichedData = new HashMap<>();
            enrichedData.put("exists", placeDetails != null);
            
            if (placeDetails != null) {
                enrichedData.put("coordinates", placeDetails.getGeometry());
                enrichedData.put("address", placeDetails.getFormattedAddress());
            }
            
            EnrichmentResponse response = EnrichmentResponse.success(
                request.getTraceId(), request.getIdempotencyKey(), enrichedData, 0.95);
            response.setDataSource("google_places");
            response.setProcessingTime(startTime);
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to process place validation request", e);
            EnrichmentResponse response = EnrichmentResponse.failure(
                request.getTraceId(), request.getIdempotencyKey(),
                Arrays.asList(new EnrichmentResponse.EnrichmentError(
                    "VALIDATION_ERROR", "Failed to validate place: " + e.getMessage(), null)));
            response.setProcessingTime(startTime);
            return response;
        }
    }
    
    /**
     * Process timing optimization ENRICHMENT request.
     */
    private EnrichmentResponse processTimingOptimizationRequest(EnrichmentRequest request, long startTime) {
        // Placeholder implementation - would integrate with crowd data APIs
        Map<String, Object> enrichedData = new HashMap<>();
        enrichedData.put("optimalTiming", "10:00-11:00");
        enrichedData.put("crowdPatterns", "Low crowds in morning, high in afternoon");
        enrichedData.put("waitTimes", "5-10 minutes");
        
        EnrichmentResponse response = EnrichmentResponse.success(
            request.getTraceId(), request.getIdempotencyKey(), enrichedData, 0.7);
        response.setDataSource("timing_service");
        response.setProcessingTime(startTime);
        return response;
    }
    
    /**
     * Process canonical mapping ENRICHMENT request.
     */
    private EnrichmentResponse processCanonicalMappingRequest(EnrichmentRequest request, long startTime) {
        try {
            // Use place registry to get canonical place ID
            NodeLocation nodeLocation = extractNodeLocationFromContext(request);
            if (nodeLocation != null) {
                NodeLocation enrichedLocation = placeEnrichmentService.enrichNodeLocation(nodeLocation);
                
                Map<String, Object> enrichedData = new HashMap<>();
                enrichedData.put("canonicalPlaceId", enrichedLocation.getPlaceId());
                
                EnrichmentResponse response = EnrichmentResponse.success(
                    request.getTraceId(), request.getIdempotencyKey(), enrichedData, 0.9);
                response.setDataSource("place_registry");
                response.setProcessingTime(startTime);
                return response;
            }
            
            EnrichmentResponse response = EnrichmentResponse.failure(
                request.getTraceId(), request.getIdempotencyKey(),
                Arrays.asList(new EnrichmentResponse.EnrichmentError(
                    "NO_LOCATION", "No location data found for canonical mapping", null)));
            response.setProcessingTime(startTime);
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to process canonical mapping request", e);
            EnrichmentResponse response = EnrichmentResponse.failure(
                request.getTraceId(), request.getIdempotencyKey(),
                Arrays.asList(new EnrichmentResponse.EnrichmentError(
                    "MAPPING_ERROR", "Failed to map to canonical place: " + e.getMessage(), null)));
            response.setProcessingTime(startTime);
            return response;
        }
    }
    
    /**
     * Process content enhancement ENRICHMENT request.
     */
    private EnrichmentResponse processContentEnhancementRequest(EnrichmentRequest request, long startTime) {
        // Placeholder implementation - would integrate with content enhancement services
        Map<String, Object> enrichedData = new HashMap<>();
        enrichedData.put("enhancedDescription", "Enhanced description with more details");
        enrichedData.put("tags", Arrays.asList("popular", "historic", "must-visit"));
        
        EnrichmentResponse response = EnrichmentResponse.success(
            request.getTraceId(), request.getIdempotencyKey(), enrichedData, 0.8);
        response.setDataSource("content_service");
        response.setProcessingTime(startTime);
        return response;
    }
    
    /**
     * Validate ENRICHMENT request.
     */
    private boolean isValidRequest(EnrichmentRequest request) {
        return request != null &&
               request.getTraceId() != null &&
               request.getIdempotencyKey() != null &&
               request.getRequestType() != null &&
               request.getTargetNodeId() != null &&
               request.getItineraryId() != null &&
               !request.hasTimedOut();
    }
    
    /**
     * Extract place ID from request context.
     */
    private String extractPlaceIdFromContext(EnrichmentRequest request) {
        Object placeId = request.getContext().get("placeId");
        return placeId instanceof String ? (String) placeId : null;
    }
    
    /**
     * Extract node location from request context.
     */
    private NodeLocation extractNodeLocationFromContext(EnrichmentRequest request) {
        Object location = request.getContext().get("nodeLocation");
        return location instanceof NodeLocation ? (NodeLocation) location : null;
    }
    
    /**
     * Cache ENRICHMENT response.
     */
    private void cacheResponse(String idempotencyKey, EnrichmentResponse response) {
        if (idempotencyKey != null && response != null) {
            // Store with 5 minute TTL (converted to hours)
            idempotencyManager.storeOperationResult(
                idempotencyKey, 
                response, 
                "enrichment_request",
                5L / 60L // 5 minutes in hours
            );
        }
    }
    
    /**
     * Statistics about the ENRICHMENT protocol.
     */
    public static class EnrichmentProtocolStats {
        private final int activeRequests;
        private final int cachedResponses;
        private final int totalCacheEntries;
        
        public EnrichmentProtocolStats(int activeRequests, int cachedResponses, int totalCacheEntries) {
            this.activeRequests = activeRequests;
            this.cachedResponses = cachedResponses;
            this.totalCacheEntries = totalCacheEntries;
        }
        
        public int getActiveRequests() { return activeRequests; }
        public int getCachedResponses() { return cachedResponses; }
        public int getTotalCacheEntries() { return totalCacheEntries; }
        
        @Override
        public String toString() {
            return "EnrichmentProtocolStats{" +
                    "activeRequests=" + activeRequests +
                    ", cachedResponses=" + cachedResponses +
                    ", totalCacheEntries=" + totalCacheEntries +
                    '}';
        }
    }
}