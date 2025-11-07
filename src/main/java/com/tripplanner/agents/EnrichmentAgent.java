package com.tripplanner.agents;

import com.tripplanner.dto.*;
import com.tripplanner.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.*;

/**
 * Enrichment Agent - Validates and enriches itineraries with warnings and pacing information.
 * This agent runs after PlannerAgent to add validation, warnings, and pacing calculations.
 */
@Component
public class EnrichmentAgent extends BaseAgent {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentAgent.class);
    
    // Node types to exclude from Google Places enrichment
    private static final Set<String> EXCLUDED_NODE_TYPES = Set.of("accommodation", "hotel", "transport", "transit");
    
    // Coordinate validation constants
    private static final double COORDINATE_ZERO_THRESHOLD = 0.0001;
    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;

    private final ItineraryJsonService itineraryJsonService;
    private final ChangeEngine changeEngine;
    private final GooglePlacesService googlePlacesService;
    private final EnrichmentProtocolHandler enrichmentProtocolHandler;

    public EnrichmentAgent(AgentEventBus eventBus,
                           ItineraryJsonService itineraryJsonService,
                           ChangeEngine changeEngine,
                           GooglePlacesService googlePlacesService,
                           EnrichmentProtocolHandler enrichmentProtocolHandler) {
        super(eventBus, AgentEvent.AgentKind.ENRICHMENT);
        this.itineraryJsonService = itineraryJsonService;
        this.changeEngine = changeEngine;
        this.googlePlacesService = googlePlacesService;
        this.enrichmentProtocolHandler = enrichmentProtocolHandler;
    }

    @Override
    public AgentCapabilities getCapabilities() {
        AgentCapabilities capabilities = new AgentCapabilities();

        // Single clear task type: enrich
        capabilities.addSupportedTask("enrich");

        // Set priority (lower = higher priority)
        capabilities.setPriority(20); // Medium priority for ENRICHMENT

        // Configuration
        capabilities.setChatEnabled(true); // Handle chat requests
        capabilities.setConfigurationValue("requiresExternalAPI", true);
        capabilities.setConfigurationValue("canModifyPlaces", true);
        capabilities.setConfigurationValue("canAddDetails", true);
        capabilities.setConfigurationValue("respectsLocks", true);

        return capabilities;
    }

    @Override
    public boolean canHandle(String taskType, Object taskContext) {
        // EnrichmentAgent handles "enrich" task type only
        return super.canHandle(taskType);
    }

    /**
     * Process ENRICHMENT using the standardized protocol.
     */
    public EnrichmentResponse processEnrichmentRequest(EnrichmentRequest request) {
        logger.info("Processing ENRICHMENT request via protocol: {}", request);

        try {
            // Map ENRICHMENT request type to agent task type
            String taskType = mapEnrichmentTypeToTaskType(request.getRequestType());
            if (!canHandle(taskType, request.getContext())) {
                return EnrichmentResponse.failure(request.getTraceId(), request.getIdempotencyKey(),
                        Arrays.asList(new EnrichmentResponse.EnrichmentError(
                                "AGENT_CANNOT_HANDLE",
                                "EnrichmentAgent cannot handle request type: " + request.getRequestType(),
                                null)));
            }

            // Process through protocol handler
            return enrichmentProtocolHandler.processRequestSync(request);

        } catch (Exception e) {
            logger.error("Failed to process ENRICHMENT request: {}", request, e);
            return EnrichmentResponse.failure(request.getTraceId(), request.getIdempotencyKey(),
                    Arrays.asList(new EnrichmentResponse.EnrichmentError(
                            "PROCESSING_ERROR",
                            "Failed to process ENRICHMENT request: " + e.getMessage(),
                            null)));
        }
    }

    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        try {
            logger.info("=== ENRICHMENT AGENT PROCESSING ===");
            logger.info("Itinerary ID: {}", itineraryId);

            // Load the current normalized itinerary
            var currentItinerary = itineraryJsonService.getItinerary(itineraryId);
            if (currentItinerary.isEmpty()) {
                throw new RuntimeException("Itinerary not found: " + itineraryId);
            }

            emitProgress(itineraryId, 10, "Loading itinerary", "loading");

            NormalizedItinerary itinerary = currentItinerary.get();

            emitProgress(itineraryId, 20, "Enriching places with Google Places data", "ENRICHMENT");

            // Enrich nodes with Google Places data
            List<ChangeOperation> enrichmentOps = enrichNodesWithPlacesData(itinerary);

            emitProgress(itineraryId, 40, "Validating opening hours", "validation");

            // Validate opening hours and add warnings
            List<ChangeOperation> warningOps = validateOpeningHours(itinerary);

            emitProgress(itineraryId, 60, "Calculating pacing", "pacing");

            // Calculate pacing and add pacing information
            List<ChangeOperation> pacingOps = calculatePacing(itinerary);

            emitProgress(itineraryId, 80, "Computing transit durations", "transit");

            // Compute transit durations between nodes
            List<ChangeOperation> transitOps = computeTransitDurations(itinerary);

            emitProgress(itineraryId, 90, "Applying enrichments", "applying");

            // Combine all ENRICHMENT operations
            List<ChangeOperation> allOps = new ArrayList<>();
            allOps.addAll(enrichmentOps);
            allOps.addAll(warningOps);
            allOps.addAll(pacingOps);
            allOps.addAll(transitOps);

            // Apply enrichments if there are any
            if (!allOps.isEmpty()) {
                ChangeSet enrichmentChangeSet = new ChangeSet();
                enrichmentChangeSet.setScope("trip");
                enrichmentChangeSet.setOps(allOps);

                // Set preferences to respect locks
                ChangePreferences preferences = new ChangePreferences();
                preferences.setRespectLocks(true);
                preferences.setUserFirst(false); // Agent changes take precedence for enrichments
                enrichmentChangeSet.setPreferences(preferences);

                // Apply the enrichments
                ChangeEngine.ApplyResult result = changeEngine.apply(itineraryId, enrichmentChangeSet);

                emitProgress(itineraryId, 100, "Enrichment complete", "complete");

                logger.info("=== ENRICHMENT COMPLETE ===");
                logger.info("Applied {} ENRICHMENT operations", allOps.size());
                logger.info("New version: {}", result.getToVersion());

                return (T) result;
            } else {
                emitProgress(itineraryId, 100, "No enrichments needed", "complete");
                logger.info("=== NO ENRICHMENTS APPLIED ===");
                return null;
            }

        } catch (Exception e) {
            logger.error("Failed to enrich itinerary: {}", itineraryId, e);
            throw new RuntimeException("Failed to enrich itinerary: " + e.getMessage(), e);
        }
    }

    @Override
    protected String getAgentName() {
        return "Enrichment Agent";
    }

    /**
     * Map EnrichmentRequest.EnrichmentType to agent task types.
     */
    private String mapEnrichmentTypeToTaskType(EnrichmentRequest.EnrichmentType enrichmentType) {
        switch (enrichmentType) {
            case PLACE_DETAILS:
            case CONTENT_ENHANCEMENT:
                return "enrich";
            case PLACE_VALIDATION:
                return "validate";
            case TIMING_OPTIMIZATION:
                return "enhance";
            case CANONICAL_MAPPING:
            default:
                return "ENRICHMENT";
        }
    }

    /**
     * Validate opening hours and add warnings for nodes that might be closed.
     */
    private List<ChangeOperation> validateOpeningHours(NormalizedItinerary itinerary) {
        List<ChangeOperation> operations = new ArrayList<>();

        if (itinerary.getDays() == null) {
            return operations;
        }

        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null) {
                continue;
            }

            for (NormalizedNode node : day.getNodes()) {
                // Skip locked nodes
                if (Boolean.TRUE.equals(node.getLocked())) {
                    continue;
                }

                // Check if node has timing information
                if (node.getTiming() != null && node.getTiming().getStartTime() != null) {
                    String startTime = node.getTiming().getStartTime().toString();

                    // Mock validation - in real implementation, this would check actual opening hours
                    if (isEarlyMorning(startTime) && isRestaurant(node)) {
                        // Add warning for early morning restaurant visit
                        ChangeOperation warningOp = createWarningOperation(node.getId(),
                                "Restaurant may not be open at this early hour");
                        operations.add(warningOp);
                    } else if (isLateEvening(startTime) && isMuseum(node)) {
                        // Add warning for late evening museum visit
                        ChangeOperation warningOp = createWarningOperation(node.getId(),
                                "Museum may be closed at this time");
                        operations.add(warningOp);
                    }
                }
            }
        }

        return operations;
    }

    /**
     * Calculate pacing and add pacing information to nodes.
     */
    private List<ChangeOperation> calculatePacing(NormalizedItinerary itinerary) {
        List<ChangeOperation> operations = new ArrayList<>();

        if (itinerary.getDays() == null) {
            return operations;
        }

        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null || day.getNodes().size() < 2) {
                continue;
            }

            // Calculate pacing between consecutive nodes
            for (int i = 0; i < day.getNodes().size() - 1; i++) {
                NormalizedNode currentNode = day.getNodes().get(i);
                NormalizedNode nextNode = day.getNodes().get(i + 1);

                // Skip if either node is locked
                if (Boolean.TRUE.equals(currentNode.getLocked()) || Boolean.TRUE.equals(nextNode.getLocked())) {
                    continue;
                }

                // Calculate time between nodes
                if (currentNode.getTiming() != null && nextNode.getTiming() != null) {
                    String currentEndTime = currentNode.getTiming().getEndTime().toString();
                    String nextStartTime = nextNode.getTiming().getStartTime().toString();

                    if (currentEndTime != null && nextStartTime != null) {
                        int timeBetween = calculateTimeBetween(currentEndTime, nextStartTime);

                        // Add pacing information
                        if (timeBetween < 30) {
                            // Very tight schedule
                            ChangeOperation pacingOp = createPacingOperation(nextNode.getId(),
                                    "Very tight schedule - only " + timeBetween + " minutes between activities");
                            operations.add(pacingOp);
                        } else if (timeBetween > 180) {
                            // Very loose schedule
                            ChangeOperation pacingOp = createPacingOperation(nextNode.getId(),
                                    "Long gap - " + timeBetween + " minutes between activities");
                            operations.add(pacingOp);
                        }
                    }
                }
            }
        }

        return operations;
    }

    /**
     * Compute transit durations between nodes and update edges.
     */
    private List<ChangeOperation> computeTransitDurations(NormalizedItinerary itinerary) {
        List<ChangeOperation> operations = new ArrayList<>();

        if (itinerary.getDays() == null) {
            return operations;
        }

        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null || day.getNodes().size() < 2) {
                continue;
            }

            // Calculate transit durations between consecutive nodes
            for (int i = 0; i < day.getNodes().size() - 1; i++) {
                NormalizedNode currentNode = day.getNodes().get(i);
                NormalizedNode nextNode = day.getNodes().get(i + 1);

                // Skip if either node is locked
                if (Boolean.TRUE.equals(currentNode.getLocked()) || Boolean.TRUE.equals(nextNode.getLocked())) {
                    continue;
                }

                // Calculate transit duration based on locations
                if (currentNode.getLocation() != null && nextNode.getLocation() != null) {
                    int transitDuration = calculateTransitDuration(
                            currentNode.getLocation(), nextNode.getLocation());

                    // Create or update edge with transit duration
                    ChangeOperation transitOp = createTransitOperation(
                            currentNode.getId(), nextNode.getId(), transitDuration);
                    operations.add(transitOp);
                }
            }
        }

        return operations;
    }

    /**
     * Create a warning operation for a node.
     */
    private ChangeOperation createWarningOperation(String nodeId, String warning) {
        ChangeOperation op = new ChangeOperation();
        op.setOp("update");
        op.setId(nodeId);

        // Create a node with warning information
        NormalizedNode nodeWithWarning = new NormalizedNode();
        nodeWithWarning.setId(nodeId);

        // Add warning to tips
        NodeTips tips = new NodeTips();
        tips.setWarnings(List.of(warning));
        nodeWithWarning.setTips(tips);

        op.setNode(nodeWithWarning);
        return op;
    }

    /**
     * Create a pacing operation for a node.
     */
    private ChangeOperation createPacingOperation(String nodeId, String pacingInfo) {
        ChangeOperation op = new ChangeOperation();
        op.setOp("update");
        op.setId(nodeId);

        // Create a node with pacing information
        NormalizedNode nodeWithPacing = new NormalizedNode();
        nodeWithPacing.setId(nodeId);

        // Add pacing to tips
        NodeTips tips = new NodeTips();
        tips.setTravel(List.of(pacingInfo));
        nodeWithPacing.setTips(tips);

        op.setNode(nodeWithPacing);
        return op;
    }

    /**
     * Create a transit operation for updating edge duration.
     */
    private ChangeOperation createTransitOperation(String fromNodeId, String toNodeId, int duration) {
        ChangeOperation op = new ChangeOperation();
        op.setOp("update_edge");
        op.setId(fromNodeId + "_to_" + toNodeId);

        // Create edge with transit duration
        Edge edge = new Edge(fromNodeId, toNodeId);

        // Note: This is a simplified approach. In a real implementation,
        // you might need a different operation type for updating edges.
        // For now, we'll just create a basic edge without duration/mode.

        return op;
    }

// Helper methods for validation

    private boolean isEarlyMorning(String time) {
        try {
            LocalTime localTime = LocalTime.parse(time.substring(11, 19)); // Extract time part
            return localTime.isBefore(LocalTime.of(9, 0));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isLateEvening(String time) {
        try {
            LocalTime localTime = LocalTime.parse(time.substring(11, 19)); // Extract time part
            return localTime.isAfter(LocalTime.of(18, 0));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isRestaurant(NormalizedNode node) {
        return "meal".equals(node.getType()) ||
                (node.getTitle() != null && node.getTitle().toLowerCase().contains("restaurant"));
    }

    private boolean isMuseum(NormalizedNode node) {
        return "activity".equals(node.getType()) &&
                (node.getTitle() != null && node.getTitle().toLowerCase().contains("museum"));
    }

    private int calculateTimeBetween(String endTime, String startTime) {
        try {
            LocalTime end = LocalTime.parse(endTime.substring(11, 19));
            LocalTime start = LocalTime.parse(startTime.substring(11, 19));

            int endMinutes = end.getHour() * 60 + end.getMinute();
            int startMinutes = start.getHour() * 60 + start.getMinute();

            return startMinutes - endMinutes;
        } catch (Exception e) {
            return 0;
        }
    }

    private int calculateTransitDuration(NodeLocation from, NodeLocation to) {
        // Mock calculation - in real implementation, this would use a mapping service
        if (from.getCoordinates() != null && to.getCoordinates() != null) {
            // Simple distance-based calculation (very rough)
            double latDiff = Math.abs(from.getCoordinates().getLat() - to.getCoordinates().getLat());
            double lngDiff = Math.abs(from.getCoordinates().getLng() - to.getCoordinates().getLng());
            double distance = Math.sqrt(latDiff * latDiff + lngDiff * lngDiff);

            // Rough conversion: 1 degree ≈ 111 km, walking speed ≈ 5 km/h
            return (int) (distance * 111 * 12); // 12 minutes per km
        }

        return 15; // Default 15 minutes
    }

    /**
     * Update node status with audit trail.
     */
    private void updateNodeStatus(NormalizedNode node, String status) {
        if (node != null && node.canTransitionTo(status)) {
            node.setStatus(status);
            setNodeAuditFields(node, "agent");
        } else if (node != null) {
            logger.warn("Invalid status transition from {} to {} for node {}",
                    node.getStatus(), status, node.getId());
        }
    }

    /**
     * Set audit trail fields for a node.
     */
    private void setNodeAuditFields(NormalizedNode node, String updatedBy) {
        if (node != null) {
            node.markAsUpdated(updatedBy);
        }
    }

    /**
     * Validate node status.
     */
    private void validateNodeStatus(NormalizedNode node) {
        if (node != null && node.getStatus() != null) {
            // Validate that the status is one of the allowed values
            List<String> validStatuses = List.of("planned", "in_progress", "skipped", "cancelled", "completed");
            if (!validStatuses.contains(node.getStatus())) {
                logger.warn("Invalid node status: {} for node {}", node.getStatus(), node.getId());
                node.setStatus("planned"); // Reset to default
                setNodeAuditFields(node, "agent");
            }
        }
    }

    /**
     * Enrich a specific node using the standardized protocol.
     */
    private EnrichmentResponse enrichNodeWithProtocol(String itineraryId, NormalizedNode node, String userId) {
        String traceId = "enrich_" + System.currentTimeMillis();

        // Create ENRICHMENT request for place details
        EnrichmentRequest enrichmentRequest = EnrichmentRequest.forPlaceDetails(
                traceId, node.getId(), itineraryId, userId);

        // Add node location context
        if (node.getLocation() != null) {
            enrichmentRequest.addContext("nodeLocation", node.getLocation());
            if (node.getLocation().getPlaceId() != null) {
                enrichmentRequest.addContext("placeId", node.getLocation().getPlaceId());
            }
        }

        // Set priority based on node importance
        if (Boolean.TRUE.equals(node.getLocked())) {
            enrichmentRequest.setPriority(EnrichmentRequest.EnrichmentPriority.HIGH);
        }

        return processEnrichmentRequest(enrichmentRequest);
    }

    /**
     * Enrich nodes with Google Places data including photos, reviews, and ratings.
     * First searches for places without coordinates, then enriches all nodes with place details.
     */
    private List<ChangeOperation> enrichNodesWithPlacesData(NormalizedItinerary itinerary) {
        List<ChangeOperation> operations = new ArrayList<>();

        if (itinerary.getDays() == null) {
            return operations;
        }

        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null) {
                continue;
            }

            for (NormalizedNode node : day.getNodes()) {
                // Skip locked nodes
                if (Boolean.TRUE.equals(node.getLocked())) {
                    continue;
                }

                // First, search for place if node doesn't have coordinates or placeId
                if (needsPlaceSearch(node)) {
                    try {
                        NormalizedNode searchedNode = searchAndSetPlaceId(node, itinerary.getDestination());
                        if (searchedNode != null) {
                            ChangeOperation searchOp = createEnrichmentOperation(searchedNode);
                            operations.add(searchOp);
                            // Update node reference for further enrichment
                            node = searchedNode;
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to search place for node {}: {}", node.getId(), e.getMessage());
                    }
                }

                // Then, check if node needs ENRICHMENT with photos/reviews
                if (needsEnrichment(node)) {
                    try {
                        logger.info("🔄 [EnrichmentAgent] Node {} needs enrichment, calling enrichNode()", node.getId());
                        // Enrich the node with Google Places data
                        NormalizedNode enrichedNode = enrichNode(node);
                        if (enrichedNode != null) {
                            logger.info("✅ [EnrichmentAgent] enrichNode() returned enriched node for {}", node.getId());
                            logger.info("   Enriched node location.photos: {}", enrichedNode.getLocation().getPhotos() != null ? enrichedNode.getLocation().getPhotos().size() : "null");
                            logger.info("   Enriched node location.rating: {}", enrichedNode.getLocation().getRating());
                            logger.info("   Enriched node location.userRatingsTotal: {}", enrichedNode.getLocation().getUserRatingsTotal());
                            logger.info("   Enriched node location.priceLevel: {}", enrichedNode.getLocation().getPriceLevel());
                            
                            ChangeOperation enrichOp = createEnrichmentOperation(enrichedNode);
                            operations.add(enrichOp);
                            logger.info("✅ [EnrichmentAgent] Created ChangeOperation for node {}", node.getId());
                        } else {
                            logger.warn("⚠️ [EnrichmentAgent] enrichNode() returned null for node {}", node.getId());
                        }
                    } catch (Exception e) {
                        logger.error("❌ [EnrichmentAgent] Failed to enrich node {}: {}", node.getId(), e.getMessage(), e);
                        // Continue with other nodes even if one fails
                    }
                }
            }
        }

        logger.info("📊 [EnrichmentAgent] Created {} ENRICHMENT operations total", operations.size());
        return operations;
    }
    
    /**
     * Check if a node needs place search (missing coordinates or placeId).
     * Only searches if coordinates are null, 0,0, or invalid.
     */
    private boolean needsPlaceSearch(NormalizedNode node) {
        if (node == null || isExcludedNodeType(node.getType())) {
            return false;
        }
        
        boolean hasValidCoordinates = hasValidCoordinates(node);
        boolean hasPlaceId = hasPlaceId(node);
        
        // Only search if coordinates are missing/invalid OR placeId is missing
        return !hasValidCoordinates || !hasPlaceId;
    }
    
    /**
     * Check if node type should be excluded from enrichment.
     */
    private boolean isExcludedNodeType(String nodeType) {
        return nodeType != null && EXCLUDED_NODE_TYPES.contains(nodeType);
    }
    
    /**
     * Build a search query combining title and location name for better specificity.
     * Strategy:
     * 1. If locationName is specific (contains place type keywords or special chars), use it
     * 2. If locationName is generic (just district/city name), use title which is more specific
     * 3. Always prefer more specific information for better Google Places search results
     */
    private String buildSearchQuery(NormalizedNode node) {
        String title = node.getTitle();
        String locationName = (node.getLocation() != null) ? node.getLocation().getName() : null;
        
        // If no information available, return null
        if ((title == null || title.trim().isEmpty()) && 
            (locationName == null || locationName.trim().isEmpty())) {
            return null;
        }
        
        // If only title available, use it
        if (locationName == null || locationName.trim().isEmpty()) {
            return title.trim();
        }
        
        // If only location name available, use it
        if (title == null || title.trim().isEmpty()) {
            return locationName.trim();
        }
        
        // Both available - check if locationName is generic (just district/city name)
        // Generic location names are typically short (1-2 words) and don't contain specific identifiers
        String locationLower = locationName.toLowerCase().trim();
        String[] locationWords = locationName.trim().split("\\s+");
        
        // Check for specific place indicators
        boolean hasSpecificIndicators = locationName.contains("(") || 
                                       locationName.contains("-") ||
                                       locationLower.contains("restaurant") ||
                                       locationLower.contains("temple") ||
                                       locationLower.contains("museum") ||
                                       locationLower.contains("park") ||
                                       locationLower.contains("market") ||
                                       locationLower.contains("street") ||
                                       locationLower.contains("tower") ||
                                       locationLower.contains("palace") ||
                                       locationLower.contains("shrine") ||
                                       locationLower.contains("garden") ||
                                       locationLower.contains("square") ||
                                       locationLower.contains("station") ||
                                       locationLower.contains("crossing") ||
                                       locationLower.contains("gate") ||
                                       locationLower.contains("bridge") ||
                                       locationLower.contains("building") ||
                                       locationLower.contains("center") ||
                                       locationLower.contains("hall");
        
        // If locationName has specific indicators or is long (3+ words), it's likely specific
        boolean isSpecificLocation = hasSpecificIndicators || locationWords.length >= 3;
        
        if (isSpecificLocation) {
            // Specific location name - use it directly
            // Example: "Sushi Zanmai (Tsukiji Honten)" or "Senso-ji Temple"
            logger.debug("Specific location detected ({}), using as-is", locationName);
            return locationName.trim();
        } else {
            // Generic location - use title which should be more specific
            // Example: locationName="Shibuya", title="Shibuya Crossing & Hachiko Statue" -> use title
            logger.debug("Generic location detected ({}), using title ({}) instead", locationName, title);
            return title.trim();
        }
    }
    
    /**
     * Check if node has valid coordinates.
     */
    private boolean hasValidCoordinates(NormalizedNode node) {
        return node.getLocation() != null && 
               node.getLocation().getCoordinates() != null &&
               node.getLocation().getCoordinates().getLat() != null &&
               node.getLocation().getCoordinates().getLng() != null &&
               !isInvalidCoordinate(node.getLocation().getCoordinates());
    }
    
    /**
     * Check if node has a valid place ID.
     */
    private boolean hasPlaceId(NormalizedNode node) {
        return node.getLocation() != null && 
               node.getLocation().getPlaceId() != null && 
               !node.getLocation().getPlaceId().trim().isEmpty();
    }
    
    /**
     * Check if coordinates are invalid (0,0, out of range, or NaN).
     */
    private boolean isInvalidCoordinate(Coordinates coords) {
        if (coords == null || coords.getLat() == null || coords.getLng() == null) {
            return true;
        }
        
        double lat = coords.getLat();
        double lng = coords.getLng();
        
        // Check for NaN
        if (Double.isNaN(lat) || Double.isNaN(lng)) {
            return true;
        }
        
        // Check for (0,0) - center of earth
        if (Math.abs(lat) < COORDINATE_ZERO_THRESHOLD && Math.abs(lng) < COORDINATE_ZERO_THRESHOLD) {
            return true;
        }
        
        // Check for out of range
        return lat < MIN_LATITUDE || lat > MAX_LATITUDE || lng < MIN_LONGITUDE || lng > MAX_LONGITUDE;
    }
    
    /**
     * Search for a place and set its placeId and coordinates.
     */
    private NormalizedNode searchAndSetPlaceId(NormalizedNode node, String destination) {
        // Build search query combining title and location name for better specificity
        String searchQuery = buildSearchQuery(node);
        
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            logger.warn("Node {} has no searchable information", node.getId());
            return null;
        }
        
        logger.info("========== ENRICHING NODE WITH PLACE SEARCH ==========");
        logger.info("Node ID: {}", node.getId());
        logger.info("Node title: {}", node.getTitle());
        logger.info("Location name: {}", node.getLocation() != null ? node.getLocation().getName() : "null");
        logger.info("Search query: {}", searchQuery);
        logger.info("Destination context: {}", destination);
        
        try {
            // Search for place using combined query
            logger.info("Calling GooglePlacesService.searchPlace()...");
            PlaceSearchResult searchResult = googlePlacesService.searchPlace(searchQuery, destination);
            logger.info("GooglePlacesService returned: {}", searchResult != null ? "result found" : "null");
            
            if (searchResult != null && searchResult.getGeometry() != null && 
                searchResult.getGeometry().getLocation() != null) {
                
                // Create enriched node
                NormalizedNode enrichedNode = createNodeCopy(node);
                
                // Initialize location if needed
                if (enrichedNode.getLocation() == null) {
                    enrichedNode.setLocation(new NodeLocation());
                }
                
                // Set coordinates
                if (enrichedNode.getLocation().getCoordinates() == null) {
                    enrichedNode.getLocation().setCoordinates(new Coordinates());
                }
                enrichedNode.getLocation().getCoordinates().setLat(
                    searchResult.getGeometry().getLocation().getLatitude());
                enrichedNode.getLocation().getCoordinates().setLng(
                    searchResult.getGeometry().getLocation().getLongitude());
                
                // Set place details
                enrichedNode.getLocation().setName(searchResult.getName());
                enrichedNode.getLocation().setAddress(searchResult.getFormattedAddress());
                enrichedNode.getLocation().setPlaceId(searchResult.getPlaceId());
                
                // Set rating if available
                if (searchResult.getRating() != null) {
                    enrichedNode.getLocation().setRating(searchResult.getRating());
                }
                
                logger.info("Found place for node {} (search: '{}'): {} at ({}, {})", 
                    node.getId(), searchQuery, searchResult.getName(),
                    searchResult.getGeometry().getLocation().getLatitude(),
                    searchResult.getGeometry().getLocation().getLongitude());
                
                return enrichedNode;
            }
        } catch (Exception e) {
            logger.error("Failed to search place for node {} (search: '{}'): {}", node.getId(), searchQuery, e.getMessage());
        }
        
        return null;
    }

    /**
     * Check if a node needs ENRICHMENT based on missing data.
     */
    private boolean needsEnrichment(NormalizedNode node) {
        if (node == null || !hasPlaceId(node) || isExcludedNodeType(node.getType())) {
            return false;
        }

        // Check if agentData.photos is empty or outdated
        boolean needsPhotos = true;
        boolean needsReviews = true;
        boolean needsRating = node.getDetails() == null || node.getDetails().getRating() == null;

        // Check if node has agent data using flexible structure
        if (node.getAgentData() != null) {
            Object locationDataObj = node.getAgentData().get("location");
            if (locationDataObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> locationData = (Map<String, Object>) locationDataObj;
                
                // Check if ENRICHMENT is recent (within 7 days)
                Object enrichmentTimeObj = locationData.get("enrichmentTimestamp");
                if (enrichmentTimeObj instanceof Long) {
                    Long enrichmentTime = (Long) enrichmentTimeObj;
                    long daysSinceEnrichment = (System.currentTimeMillis() - enrichmentTime) / (24 * 60 * 60 * 1000);
                    if (daysSinceEnrichment < 7) {
                        return false; // Recently enriched, skip
                    }
                }
            }

            Object photosDataObj = node.getAgentData().get("photos");
            if (photosDataObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> photosData = (Map<String, Object>) photosDataObj;
                Object photosObj = photosData.get("photos");
                if (photosObj instanceof java.util.List && !((java.util.List<?>) photosObj).isEmpty()) {
                    needsPhotos = false;
                }
            }
        }

        // Check if node details has reviews
        if (node.getDetails() != null && node.getDetails().getReviews() != null && !node.getDetails().getReviews().isEmpty()) {
            needsReviews = false;
        }

        return needsPhotos || needsReviews || needsRating;
    }

    /**
     * Enrich a node with Google Places data.
     */
    private NormalizedNode enrichNode(NormalizedNode node) {
        if (node == null || node.getLocation() == null || node.getLocation().getPlaceId() == null) {
            return null;
        }

        String placeId = node.getLocation().getPlaceId();
        logger.info("🔄 [EnrichmentAgent] Starting enrichment for node: {} ({})", node.getId(), node.getTitle());
        logger.info("   📍 Place ID: {}", placeId);

        try {
            // Get place details from Google Places API
            PlaceDetails placeDetails = googlePlacesService.getPlaceDetails(placeId);

            if (placeDetails != null) {
                logger.info("✅ [EnrichmentAgent] Received PlaceDetails from API:");
                logger.info("   ⭐ Rating: {}", placeDetails.getRating());
                logger.info("   👥 User Ratings Total: {}", placeDetails.getUserRatingsTotal());
                logger.info("   💰 Price Level: {}", placeDetails.getPriceLevel());
                logger.info("   📸 Photos: {}", placeDetails.getPhotos() != null ? placeDetails.getPhotos().size() : 0);
                
                // Create a copy of the node for ENRICHMENT
                NormalizedNode enrichedNode = createNodeCopy(node);

                // Update node with place data
                updateNodeWithPlaceData(enrichedNode, placeDetails);

                // Set ENRICHMENT timestamp
                setEnrichmentTimestamp(enrichedNode);

                logger.info("✅ [EnrichmentAgent] Successfully enriched node {} with {} photos and {} reviews",
                        node.getId(),
                        placeDetails.getPhotos() != null ? placeDetails.getPhotos().size() : 0,
                        placeDetails.getReviews() != null ? placeDetails.getReviews().size() : 0);

                return enrichedNode;
            } else {
                logger.warn("⚠️ [EnrichmentAgent] PlaceDetails returned null for placeId: {}", placeId);
            }

        } catch (Exception e) {
            logger.error("❌ [EnrichmentAgent] Failed to enrich node {} with place ID {}: {}", node.getId(), placeId, e.getMessage(), e);
        }

        return null;
    }

    /**
     * Update node with place data from Google Places API using flexible agent data structure.
     */
    private void updateNodeWithPlaceData(NormalizedNode node, PlaceDetails placeDetails) {
        // Initialize agent data if not present
        if (node.getAgentData() == null) {
            node.setAgentData(new java.util.HashMap<>());
        }

        // Create flexible location data structure
        Map<String, Object> locationData = new java.util.HashMap<>();
        locationData.put("placeId", placeDetails.getPlaceId());
        locationData.put("rating", placeDetails.getRating());
        locationData.put("priceLevel", placeDetails.getPriceLevel());
        locationData.put("enrichmentTimestamp", System.currentTimeMillis());
        locationData.put("source", "Google Places API");

        // Add opening hours if available
        if (placeDetails.getOpeningHours() != null && placeDetails.getOpeningHours().getWeekdayText() != null) {
            Map<String, String> openingHours = new java.util.HashMap<>();
            List<String> weekdayText = placeDetails.getOpeningHours().getWeekdayText();
            for (int i = 0; i < weekdayText.size() && i < 7; i++) {
                String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
                if (i < dayNames.length) {
                    openingHours.put(dayNames[i], weekdayText.get(i));
                }
            }
            locationData.put("openingHours", openingHours);
        }

        // Store location data flexibly
        node.getAgentData().put("location", locationData);

        // Update photos agent data with flexible structure
        if (placeDetails.getPhotos() != null && !placeDetails.getPhotos().isEmpty()) {
            Map<String, Object> photosData = new java.util.HashMap<>();
            
            // Convert Photo objects to flexible photo info maps
            List<Map<String, Object>> photoInfoList = placeDetails.getPhotos().stream()
                    .map(photo -> {
                        Map<String, Object> photoInfo = new java.util.HashMap<>();
                        photoInfo.put("url", photo.getPhotoReference()); // Use photo reference as URL for now
                        photoInfo.put("width", photo.getWidth());
                        photoInfo.put("height", photo.getHeight());
                        photoInfo.put("photoReference", photo.getPhotoReference());
                        return photoInfo;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            photosData.put("photos", photoInfoList);
            photosData.put("lastUpdated", System.currentTimeMillis());
            photosData.put("source", "Google Places API");
            
            // Store photos data flexibly
            node.getAgentData().put("photos", photosData);
        }

        // Update node details with rating and reviews
        if (node.getDetails() == null) {
            node.setDetails(new NodeDetails());
        }

        if (placeDetails.getRating() != null) {
            node.getDetails().setRating(placeDetails.getRating());
        }

        // Merge reviews into node details (preserve existing reviews)
        if (placeDetails.getReviews() != null && !placeDetails.getReviews().isEmpty()) {
            List<Review> existingReviews = node.getDetails().getReviews();
            if (existingReviews == null) {
                existingReviews = new ArrayList<>();
                node.getDetails().setReviews(existingReviews);
            }

            // Add new reviews (avoid duplicates by checking author names)
            for (Review newReview : placeDetails.getReviews()) {
                boolean isDuplicate = existingReviews.stream()
                        .anyMatch(existing -> existing.getAuthorName() != null &&
                                existing.getAuthorName().equals(newReview.getAuthorName()));

                if (!isDuplicate) {
                    existingReviews.add(newReview);
                }
            }
        }

        // Update location information if more detailed
        if (placeDetails.getFormattedAddress() != null &&
                (node.getLocation().getAddress() == null || node.getLocation().getAddress().isEmpty())) {
            node.getLocation().setAddress(placeDetails.getFormattedAddress());
        }

        // Update coordinates if available and not already set
        if (placeDetails.getGeometry() != null && placeDetails.getGeometry().getLocation() != null) {
            if (node.getLocation().getCoordinates() == null) {
                Coordinates coords = new Coordinates();
                coords.setLat(placeDetails.getGeometry().getLocation().getLat());
                coords.setLng(placeDetails.getGeometry().getLocation().getLng());
                node.getLocation().setCoordinates(coords);
            }
        }
        
        // *** NEW: Update location object with photos, ratings, and price level for frontend ***
        logger.info("📝 [EnrichmentAgent] Updating NodeLocation fields for node: {}", node.getId());
        
        // Set photos directly in location (extract photo references)
        if (placeDetails.getPhotos() != null && !placeDetails.getPhotos().isEmpty()) {
            List<String> photoReferences = placeDetails.getPhotos().stream()
                    .map(Photo::getPhotoReference)
                    .collect(java.util.stream.Collectors.toList());
            node.getLocation().setPhotos(photoReferences);
            logger.info("   ✅ Set {} photo references in location.photos", photoReferences.size());
            logger.info("      First photo ref: {}", photoReferences.get(0).substring(0, Math.min(30, photoReferences.get(0).length())) + "...");
        } else {
            logger.warn("   ⚠️ No photos to set in location.photos");
        }
        
        // Set rating in location (in addition to details)
        if (placeDetails.getRating() != null) {
            node.getLocation().setRating(placeDetails.getRating());
            logger.info("   ✅ Set location.rating = {}", placeDetails.getRating());
        } else {
            logger.warn("   ⚠️ No rating to set in location.rating");
        }
        
        // Set user ratings total
        if (placeDetails.getUserRatingsTotal() != null) {
            node.getLocation().setUserRatingsTotal(placeDetails.getUserRatingsTotal());
            logger.info("   ✅ Set location.userRatingsTotal = {}", placeDetails.getUserRatingsTotal());
        } else {
            logger.warn("   ⚠️ No userRatingsTotal to set in location.userRatingsTotal");
        }
        
        // Set price level
        if (placeDetails.getPriceLevel() != null) {
            node.getLocation().setPriceLevel(placeDetails.getPriceLevel());
            logger.info("   ✅ Set location.priceLevel = {}", placeDetails.getPriceLevel());
        } else {
            logger.warn("   ⚠️ No priceLevel to set in location.priceLevel");
        }
        
        // Verify the data was set correctly
        logger.info("🔍 [EnrichmentAgent] Verification - NodeLocation after update:");
        logger.info("   location.photos: {} items", node.getLocation().getPhotos() != null ? node.getLocation().getPhotos().size() : "null");
        logger.info("   location.rating: {}", node.getLocation().getRating());
        logger.info("   location.userRatingsTotal: {}", node.getLocation().getUserRatingsTotal());
        logger.info("   location.priceLevel: {}", node.getLocation().getPriceLevel());
    }

    /**
     * Set ENRICHMENT timestamp for tracking when data was last updated using flexible structure.
     */
    private void setEnrichmentTimestamp(NormalizedNode node) {
        if (node.getAgentData() == null) {
            node.setAgentData(new java.util.HashMap<>());
        }

        // Get or create location data as flexible map
        Object locationDataObj = node.getAgentData().get("location");
        Map<String, Object> locationData;
        
        if (locationDataObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> existingData = (Map<String, Object>) locationDataObj;
            locationData = existingData;
        } else {
            locationData = new java.util.HashMap<>();
            node.getAgentData().put("location", locationData);
        }

        locationData.put("enrichmentTimestamp", System.currentTimeMillis());
    }

    /**
     * Create a copy of a node for ENRICHMENT (to avoid modifying the original).
     */
    private NormalizedNode createNodeCopy(NormalizedNode original) {
        // For simplicity, we'll create a shallow copy and initialize required fields
        // In a production system, you might want to use a deep cloning library
        NormalizedNode copy = new NormalizedNode();
        copy.setId(original.getId());
        copy.setTitle(original.getTitle());
        copy.setType(original.getType());
        copy.setStatus(original.getStatus());
        copy.setLocked(original.getLocked());
        copy.setLocation(original.getLocation());
        copy.setTiming(original.getTiming());
        copy.setCost(original.getCost());
        copy.setDetails(original.getDetails());
        copy.setLabels(original.getLabels());
        copy.setTips(original.getTips());
        copy.setLinks(original.getLinks());

        // Initialize agent data map if original has it
        if (original.getAgentData() != null) {
            copy.setAgentData(new java.util.HashMap<>(original.getAgentData()));
        }

        return copy;
    }

    /**
     * Create an ENRICHMENT operation for updating a node.
     */
    private ChangeOperation createEnrichmentOperation(NormalizedNode enrichedNode) {
        ChangeOperation op = new ChangeOperation();
        op.setOp("update");
        op.setId(enrichedNode.getId());
        op.setNode(enrichedNode);
        return op;
    }
}

