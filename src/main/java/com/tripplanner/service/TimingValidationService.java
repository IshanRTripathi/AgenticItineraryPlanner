package com.tripplanner.service;

import com.tripplanner.dto.NormalizedDay;
import com.tripplanner.dto.NormalizedItinerary;
import com.tripplanner.dto.NormalizedNode;
import com.tripplanner.dto.NodeTiming;
import com.tripplanner.dto.tools.TimingValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * TimingValidationService - Validates schedule feasibility.
 * Checks if there's enough time for travel, activities, and transitions.
 */
@Service
public class TimingValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TimingValidationService.class);
    
    // Minimum buffer time between activities (minutes)
    private static final int MIN_BUFFER_MINUTES = 15;
    
    // Maximum reasonable day length (hours)
    private static final int MAX_DAY_HOURS = 16;
    
    /**
     * Validate timing for a specific day.
     */
    public TimingValidationResult validateDay(NormalizedItinerary itinerary, Integer dayNumber) {
        TimingValidationResult result = new TimingValidationResult();
        
        if (itinerary == null || itinerary.getDays() == null) {
            return TimingValidationResult.error("Invalid itinerary");
        }
        
        // Find the day
        NormalizedDay day = itinerary.getDays().stream()
            .filter(d -> d.getDayNumber().equals(dayNumber))
            .findFirst()
            .orElse(null);
        
        if (day == null) {
            return TimingValidationResult.error("Day " + dayNumber + " not found");
        }
        
        return validateDaySchedule(day);
    }
    
    /**
     * Validate a proposed schedule.
     */
    public TimingValidationResult validateSchedule(List<NormalizedNode> nodes) {
        TimingValidationResult result = new TimingValidationResult();
        
        if (nodes == null || nodes.isEmpty()) {
            return result; // Empty schedule is valid
        }
        
        // Check each consecutive pair of nodes
        for (int i = 0; i < nodes.size() - 1; i++) {
            NormalizedNode current = nodes.get(i);
            NormalizedNode next = nodes.get(i + 1);
            
            validateNodePair(current, next, result);
        }
        
        // Check total day length
        validateDayLength(nodes, result);
        
        return result;
    }
    
    /**
     * Validate schedule for a day.
     */
    private TimingValidationResult validateDaySchedule(NormalizedDay day) {
        if (day.getNodes() == null || day.getNodes().isEmpty()) {
            return new TimingValidationResult(); // Empty day is valid
        }
        
        return validateSchedule(day.getNodes());
    }
    
    /**
     * Validate timing between two consecutive nodes.
     */
    private void validateNodePair(NormalizedNode current, NormalizedNode next, TimingValidationResult result) {
        // Check if both nodes have timing
        if (current.getTiming() == null || next.getTiming() == null) {
            result.addWarning("Missing timing information for nodes");
            return;
        }
        
        NodeTiming currentTiming = current.getTiming();
        NodeTiming nextTiming = next.getTiming();
        
        // Check if times are set
        if (currentTiming.getEndTime() == null || nextTiming.getStartTime() == null) {
            result.addWarning("Missing start/end times");
            return;
        }
        
        // Calculate gap between activities
        long gapMinutes = nextTiming.getStartTime() - currentTiming.getEndTime();
        
        // Check for overlap
        if (gapMinutes < 0) {
            result.addIssue(new TimingValidationResult.TimingIssue(
                "TIME_OVERLAP",
                current.getId() + " -> " + next.getId(),
                0,
                (int) gapMinutes,
                "Activities overlap by " + Math.abs(gapMinutes) + " minutes. Adjust timing."
            ));
            return;
        }
        
        // Check if gap is too small (no buffer time)
        if (gapMinutes < MIN_BUFFER_MINUTES) {
            result.addWarning(
                String.format("Very tight schedule between %s and %s (%d min gap). Consider %d min buffer.",
                    current.getTitle(), next.getTitle(), gapMinutes, MIN_BUFFER_MINUTES)
            );
        }
        
        // Note: Transport metadata would be checked here if available
        // For now, we just check for basic timing conflicts
    }
    
    /**
     * Validate total day length is reasonable.
     */
    private void validateDayLength(List<NormalizedNode> nodes, TimingValidationResult result) {
        if (nodes.isEmpty()) return;
        
        // Find first and last nodes with timing
        NormalizedNode first = nodes.stream()
            .filter(n -> n.getTiming() != null && n.getTiming().getStartTime() != null)
            .findFirst()
            .orElse(null);
        
        NormalizedNode last = nodes.stream()
            .filter(n -> n.getTiming() != null && n.getTiming().getEndTime() != null)
            .reduce((a, b) -> b) // Get last
            .orElse(null);
        
        if (first == null || last == null) {
            return; // Can't validate without timing
        }
        
        long dayLengthMinutes = last.getTiming().getEndTime() - first.getTiming().getStartTime();
        long dayLengthHours = dayLengthMinutes / 60;
        
        if (dayLengthHours > MAX_DAY_HOURS) {
            result.addIssue(new TimingValidationResult.TimingIssue(
                "DAY_TOO_LONG",
                "Full day",
                MAX_DAY_HOURS * 60,
                (int) dayLengthMinutes,
                String.format("Day is %d hours long (max recommended: %d hours). " +
                            "Consider splitting activities or reducing durations.",
                            dayLengthHours, MAX_DAY_HOURS)
            ));
        } else if (dayLengthHours > 12) {
            result.addWarning(
                String.format("Long day: %d hours. Ensure adequate rest breaks.", dayLengthHours)
            );
        }
    }
}
