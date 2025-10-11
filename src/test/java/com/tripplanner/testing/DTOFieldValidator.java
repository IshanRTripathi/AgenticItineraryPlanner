package com.tripplanner.testing;

import com.tripplanner.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Validates DTO fields with exact type checking and annotation compliance.
 */
@Component
public class DTOFieldValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(DTOFieldValidator.class);
    
    /**
     * Validate NormalizedItinerary with all 20+ fields.
     */
    public FieldValidationResult validateNormalizedItinerary(NormalizedItinerary itinerary) {
        logger.debug("Validating NormalizedItinerary fields");
        FieldValidationResult result = new FieldValidationResult();
        
        if (itinerary == null) {
            result.validateField("itinerary", null, NormalizedItinerary.class, true, false);
            return result;
        }
        
        // Validate all fields based on annotations in the DTO class
        result.validateField("itineraryId", itinerary.getItineraryId(), String.class, true, true); // @NotBlank
        result.validateField("version", itinerary.getVersion(), Integer.class, false, false); // @Positive
        result.validateField("userId", itinerary.getUserId(), String.class, false, false);
        result.validateField("createdAt", itinerary.getCreatedAt(), Long.class, false, false);
        result.validateField("updatedAt", itinerary.getUpdatedAt(), Long.class, false, false);
        result.validateField("summary", itinerary.getSummary(), String.class, false, false);
        result.validateField("currency", itinerary.getCurrency(), String.class, false, false);
        result.validateField("themes", itinerary.getThemes(), List.class, false, false);
        result.validateField("origin", itinerary.getOrigin(), String.class, false, false);
        result.validateField("destination", itinerary.getDestination(), String.class, false, false);
        result.validateField("startDate", itinerary.getStartDate(), String.class, false, false);
        result.validateField("endDate", itinerary.getEndDate(), String.class, false, false);
        result.validateField("days", itinerary.getDays(), List.class, true, false); // @Valid @NotNull
        result.validateField("settings", itinerary.getSettings(), ItinerarySettings.class, false, false); // @Valid
        result.validateField("agents", itinerary.getAgents(), Map.class, false, false); // @Valid
        result.validateField("mapBounds", itinerary.getMapBounds(), MapBounds.class, false, false); // @Valid
        result.validateField("countryCentroid", itinerary.getCountryCentroid(), Coordinates.class, false, false); // @Valid
        result.validateField("agentData", itinerary.getAgentData(), Map.class, false, false); // @Valid
        result.validateField("workflow", itinerary.getWorkflow(), WorkflowData.class, false, false); // @Valid
        result.validateField("revisions", itinerary.getRevisions(), List.class, false, false); // @Valid
        result.validateField("chat", itinerary.getChat(), List.class, false, false); // @Valid
        
        logger.debug("NormalizedItinerary validation completed: {} fields validated, {} errors", 
                    result.getValidatedFieldCount(), result.getErrorCount());
        return result;
    }
    
    /**
     * Validate NormalizedDay with all 13 fields.
     */
    public FieldValidationResult validateNormalizedDay(NormalizedDay day) {
        logger.debug("Validating NormalizedDay fields");
        FieldValidationResult result = new FieldValidationResult();
        
        if (day == null) {
            result.validateField("day", null, NormalizedDay.class, true, false);
            return result;
        }
        
        // Validate all fields based on annotations
        result.validateField("dayNumber", day.getDayNumber(), Integer.class, false, false); // @Positive
        result.validateField("date", day.getDate(), String.class, false, false);
        result.validateField("location", day.getLocation(), String.class, false, false);
        result.validateField("warnings", day.getWarnings(), List.class, false, false);
        result.validateField("notes", day.getNotes(), String.class, false, false);
        result.validateField("pace", day.getPace(), String.class, false, false);
        result.validateField("totalDistance", day.getTotalDistance(), Double.class, false, false);
        result.validateField("totalCost", day.getTotalCost(), Double.class, false, false);
        result.validateField("totalDuration", day.getTotalDuration(), Double.class, false, false);
        result.validateField("timeWindowStart", day.getTimeWindowStart(), String.class, false, false);
        result.validateField("timeWindowEnd", day.getTimeWindowEnd(), String.class, false, false);
        result.validateField("timeZone", day.getTimeZone(), String.class, false, false);
        result.validateField("nodes", day.getNodes(), List.class, true, false); // @Valid @NotNull
        result.validateField("edges", day.getEdges(), List.class, false, false); // @Valid
        
        logger.debug("NormalizedDay validation completed: {} fields validated, {} errors", 
                    result.getValidatedFieldCount(), result.getErrorCount());
        return result;
    }
    
    /**
     * Validate NormalizedNode with all 16 fields.
     */
    public FieldValidationResult validateNormalizedNode(NormalizedNode node) {
        logger.debug("Validating NormalizedNode fields");
        FieldValidationResult result = new FieldValidationResult();
        
        if (node == null) {
            result.validateField("node", null, NormalizedNode.class, true, false);
            return result;
        }
        
        // Validate all fields based on annotations
        result.validateField("id", node.getId(), String.class, true, true); // @NotBlank
        result.validateField("type", node.getType(), String.class, true, true); // @NotBlank
        result.validateField("title", node.getTitle(), String.class, true, true); // @NotBlank
        result.validateField("location", node.getLocation(), NodeLocation.class, false, false); // @Valid
        result.validateField("timing", node.getTiming(), NodeTiming.class, false, false); // @Valid
        result.validateField("cost", node.getCost(), NodeCost.class, false, false); // @Valid
        result.validateField("details", node.getDetails(), NodeDetails.class, false, false); // @Valid
        result.validateField("labels", node.getLabels(), List.class, false, false);
        result.validateField("tips", node.getTips(), NodeTips.class, false, false); // @Valid
        result.validateField("links", node.getLinks(), NodeLinks.class, false, false); // @Valid
        result.validateField("transit", node.getTransit(), TransitInfo.class, false, false); // @Valid
        result.validateField("locked", node.getLocked(), Boolean.class, false, false);
        result.validateField("bookingRef", node.getBookingRef(), String.class, false, false);
        result.validateField("status", node.getStatus(), String.class, false, false);
        result.validateField("updatedBy", node.getUpdatedBy(), String.class, false, false);
        result.validateField("updatedAt", node.getUpdatedAt(), Long.class, false, false);
        result.validateField("agentData", node.getAgentData(), Map.class, false, false); // @Valid
        
        logger.debug("NormalizedNode validation completed: {} fields validated, {} errors", 
                    result.getValidatedFieldCount(), result.getErrorCount());
        return result;
    }
    
    /**
     * Validate NodeLocation with all 8 fields.
     */
    public FieldValidationResult validateNodeLocation(NodeLocation location) {
        logger.debug("Validating NodeLocation fields");
        FieldValidationResult result = new FieldValidationResult();
        
        if (location == null) {
            result.validateField("location", null, NodeLocation.class, false, false);
            return result;
        }
        
        // Validate all fields (no annotations in NodeLocation)
        result.validateField("name", location.getName(), String.class, false, false);
        result.validateField("address", location.getAddress(), String.class, false, false);
        result.validateField("coordinates", location.getCoordinates(), Coordinates.class, false, false);
        result.validateField("placeId", location.getPlaceId(), String.class, false, false);
        result.validateField("googleMapsUri", location.getGoogleMapsUri(), String.class, false, false);
        result.validateField("rating", location.getRating(), Double.class, false, false);
        result.validateField("openingHours", location.getOpeningHours(), String.class, false, false);
        result.validateField("closingHours", location.getClosingHours(), String.class, false, false);
        
        logger.debug("NodeLocation validation completed: {} fields validated, {} errors", 
                    result.getValidatedFieldCount(), result.getErrorCount());
        return result;
    }
    
    /**
     * Validate NodeCost with both fields.
     */
    public FieldValidationResult validateNodeCost(NodeCost cost) {
        logger.debug("Validating NodeCost fields");
        FieldValidationResult result = new FieldValidationResult();
        
        if (cost == null) {
            result.validateField("cost", null, NodeCost.class, false, false);
            return result;
        }
        
        // Validate both fields (no annotations in NodeCost)
        result.validateField("amountPerPerson", cost.getAmountPerPerson(), Double.class, false, false);
        result.validateField("currency", cost.getCurrency(), String.class, false, false);
        
        logger.debug("NodeCost validation completed: {} fields validated, {} errors", 
                    result.getValidatedFieldCount(), result.getErrorCount());
        return result;
    }
    
    /**
     * Validate NodeLinks with nested BookingInfo.
     */
    public FieldValidationResult validateNodeLinks(NodeLinks links) {
        logger.debug("Validating NodeLinks fields");
        FieldValidationResult result = new FieldValidationResult();
        
        if (links == null) {
            result.validateField("links", null, NodeLinks.class, false, false);
            return result;
        }
        
        // Validate main field
        result.validateField("booking", links.getBooking(), NodeLinks.BookingInfo.class, false, false);
        
        // Validate nested BookingInfo if present
        if (links.getBooking() != null) {
            NodeLinks.BookingInfo booking = links.getBooking();
            result.validateField("booking.refNumber", booking.getRefNumber(), String.class, false, false);
            result.validateField("booking.status", booking.getStatus(), String.class, false, false);
            result.validateField("booking.details", booking.getDetails(), String.class, false, false);
        }
        
        logger.debug("NodeLinks validation completed: {} fields validated, {} errors", 
                    result.getValidatedFieldCount(), result.getErrorCount());
        return result;
    }
    
    /**
     * Validate AgentDataSection with flexible Map<String, Object> operations.
     */
    public FieldValidationResult validateAgentDataSection(AgentDataSection agentData) {
        logger.debug("Validating AgentDataSection fields");
        FieldValidationResult result = new FieldValidationResult();
        
        if (agentData == null) {
            result.validateField("agentData", null, AgentDataSection.class, false, false);
            return result;
        }
        
        // Validate the data field (Map<String, Object>)
        result.validateField("data", agentData.getData(), Map.class, false, false);
        
        logger.debug("AgentDataSection validation completed: {} fields validated, {} errors", 
                    result.getValidatedFieldCount(), result.getErrorCount());
        return result;
    }
}