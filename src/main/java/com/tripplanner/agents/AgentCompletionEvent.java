package com.tripplanner.agents;

import com.tripplanner.dto.ItineraryDto;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when agent orchestration completes successfully.
 */
public class AgentCompletionEvent extends ApplicationEvent {
    
    private final String itineraryId;
    private final ItineraryDto itinerary;
    
    public AgentCompletionEvent(Object source, String itineraryId, ItineraryDto itinerary) {
        super(source);
        this.itineraryId = itineraryId;
        this.itinerary = itinerary;
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public ItineraryDto getItinerary() {
        return itinerary;
    }
}
