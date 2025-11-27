package com.tripplanner.service;

import com.tripplanner.dto.PlaceDetails;
import com.tripplanner.enums.ToolType;
import com.tripplanner.service.cache.ToolCacheService;
import com.tripplanner.util.ToolCacheKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing and analyzing opening hours from Google Places API.
 * Provides detailed hour information beyond just "open now" boolean.
 * 
 * CACHING: Opening hours cached for 7 days (can change seasonally)
 */
@Service
public class OpeningHoursService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpeningHoursService.class);
    
    @Autowired(required = false)
    private ToolCacheService toolCacheService;
    
    // Pattern to match time ranges like "9:00 AM – 5:00 PM" or "9:00 AM - 5:00 PM"
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile(
        "(\\d{1,2}:\\d{2})\\s*([AP]M)?\\s*[–-]\\s*(\\d{1,2}:\\d{2})\\s*([AP]M)?",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern to match "Closed" or "Closed on [day]"
    private static final Pattern CLOSED_PATTERN = Pattern.compile(
        "closed",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Opening hours information parsed from Google Places
     */
    public static class ParsedHours {
        private boolean open;
        private String opensAt;  // HH:mm format (24-hour)
        private String closesAt; // HH:mm format (24-hour)
        private boolean closedAllDay;
        private boolean open24Hours;
        
        public ParsedHours() {}
        
        public ParsedHours(boolean open, String opensAt, String closesAt) {
            this.open = open;
            this.opensAt = opensAt;
            this.closesAt = closesAt;
        }
        
        // Getters and setters
        public boolean isOpen() { return open; }
        public void setOpen(boolean open) { this.open = open; }
        public String getOpensAt() { return opensAt; }
        public void setOpensAt(String opensAt) { this.opensAt = opensAt; }
        public String getClosesAt() { return closesAt; }
        public void setClosesAt(String closesAt) { this.closesAt = closesAt; }
        public boolean isClosedAllDay() { return closedAllDay; }
        public void setClosedAllDay(boolean closedAllDay) { this.closedAllDay = closedAllDay; }
        public boolean isOpen24Hours() { return open24Hours; }
        public void setOpen24Hours(boolean open24Hours) { this.open24Hours = open24Hours; }
    }
    
    /**
     * Parse opening hours for a specific day from weekday text
     * 
     * @param itineraryId Itinerary ID for caching (optional)
     * @param placeId Place ID for caching
     * @param weekdayText List of strings like ["Monday: 9:00 AM – 5:00 PM", ...]
     * @param dayOfWeek Day to parse (e.g., DayOfWeek.MONDAY)
     * @return Parsed hours or null if not found
     */
    public ParsedHours parseHoursForDay(String itineraryId, String placeId, List<String> weekdayText, DayOfWeek dayOfWeek) {
        if (weekdayText == null || weekdayText.isEmpty()) {
            return null;
        }
        
        // Try cache first if itineraryId and placeId provided
        if (toolCacheService != null && itineraryId != null && placeId != null) {
            String date = dayOfWeek.toString(); // Use day name as date proxy
            String cacheKey = ToolCacheKeyGenerator.forOpeningHours(placeId, date);
            
            return toolCacheService.getOrCompute(
                itineraryId,
                ToolType.OPENING_HOURS.getValue(),
                cacheKey,
                Map.of("placeId", placeId, "dayOfWeek", dayOfWeek.toString()),
                () -> parseHoursForDayInternal(weekdayText, dayOfWeek),
                ParsedHours.class
            );
        }
        
        // Fallback to direct parsing
        return parseHoursForDayInternal(weekdayText, dayOfWeek);
    }
    
    /**
     * Overload for backward compatibility (no caching)
     */
    public ParsedHours parseHoursForDay(List<String> weekdayText, DayOfWeek dayOfWeek) {
        return parseHoursForDay(null, null, weekdayText, dayOfWeek);
    }
    
    /**
     * Internal parsing implementation (called by cache or directly)
     */
    private ParsedHours parseHoursForDayInternal(List<String> weekdayText, DayOfWeek dayOfWeek) {
        String dayName = dayOfWeek.toString().substring(0, 1) + 
                        dayOfWeek.toString().substring(1).toLowerCase();
        
        for (String dayText : weekdayText) {
            if (dayText.toLowerCase().startsWith(dayName.toLowerCase())) {
                return parseHoursFromText(dayText);
            }
        }
        
        return null;
    }
    
    /**
     * Parse opening hours from a single text line
     * 
     * @param hoursText Text like "Monday: 9:00 AM – 5:00 PM" or "Tuesday: Closed"
     * @return Parsed hours
     */
    public ParsedHours parseHoursFromText(String hoursText) {
        ParsedHours hours = new ParsedHours();
        
        if (hoursText == null || hoursText.isEmpty()) {
            return hours;
        }
        
        // Check if closed
        if (CLOSED_PATTERN.matcher(hoursText).find()) {
            hours.setClosedAllDay(true);
            hours.setOpen(false);
            return hours;
        }
        
        // Check for 24 hours
        if (hoursText.toLowerCase().contains("24 hours") || 
            hoursText.toLowerCase().contains("open 24 hours")) {
            hours.setOpen24Hours(true);
            hours.setOpen(true);
            hours.setOpensAt("00:00");
            hours.setClosesAt("23:59");
            return hours;
        }
        
        // Try to extract time range
        Matcher matcher = TIME_RANGE_PATTERN.matcher(hoursText);
        if (matcher.find()) {
            try {
                String openTime = matcher.group(1);
                String openPeriod = matcher.group(2);
                String closeTime = matcher.group(3);
                String closePeriod = matcher.group(4);
                
                // Convert to 24-hour format
                String opensAt24 = convertTo24Hour(openTime, openPeriod);
                String closesAt24 = convertTo24Hour(closeTime, closePeriod);
                
                hours.setOpen(true);
                hours.setOpensAt(opensAt24);
                hours.setClosesAt(closesAt24);
                
                logger.debug("Parsed hours: {} - {}", opensAt24, closesAt24);
                
            } catch (Exception e) {
                logger.warn("Failed to parse time from: {}", hoursText, e);
            }
        }
        
        return hours;
    }
    
    /**
     * Check if place is open at a specific time
     * 
     * @param hours Parsed hours
     * @param time Time to check (HH:mm format, 24-hour)
     * @return true if open at that time
     */
    public boolean isOpenAt(ParsedHours hours, String time) {
        if (hours == null) {
            return true; // Assume open if no data
        }
        
        if (hours.isClosedAllDay()) {
            return false;
        }
        
        if (hours.isOpen24Hours()) {
            return true;
        }
        
        if (hours.getOpensAt() == null || hours.getClosesAt() == null) {
            return true; // Assume open if no specific hours
        }
        
        try {
            LocalTime checkTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime openTime = LocalTime.parse(hours.getOpensAt(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime closeTime = LocalTime.parse(hours.getClosesAt(), DateTimeFormatter.ofPattern("HH:mm"));
            
            // Handle cases where closing time is after midnight
            if (closeTime.isBefore(openTime)) {
                // Open past midnight (e.g., 10 PM - 2 AM)
                return checkTime.isAfter(openTime) || checkTime.isBefore(closeTime);
            } else {
                // Normal hours (e.g., 9 AM - 5 PM)
                return !checkTime.isBefore(openTime) && !checkTime.isAfter(closeTime);
            }
            
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse time: {}", time, e);
            return true; // Assume open on parse error
        }
    }
    
    /**
     * Get today's hours in readable format
     * 
     * @param weekdayText List of weekday text
     * @param dayOfWeek Current day
     * @return Readable hours string (e.g., "9:00 AM – 5:00 PM")
     */
    public String getTodayHoursText(List<String> weekdayText, DayOfWeek dayOfWeek) {
        if (weekdayText == null || weekdayText.isEmpty()) {
            return "Hours not available";
        }
        
        String dayName = dayOfWeek.toString().substring(0, 1) + 
                        dayOfWeek.toString().substring(1).toLowerCase();
        
        for (String dayText : weekdayText) {
            if (dayText.toLowerCase().startsWith(dayName.toLowerCase())) {
                // Extract just the hours part (after the colon)
                int colonIndex = dayText.indexOf(':');
                if (colonIndex >= 0 && colonIndex < dayText.length() - 1) {
                    return dayText.substring(colonIndex + 1).trim();
                }
                return dayText;
            }
        }
        
        return "Hours not available";
    }
    
    /**
     * Convert 12-hour time to 24-hour format
     * 
     * @param time Time string (e.g., "9:00" or "9:00 AM")
     * @param period AM/PM indicator (can be null if already in 24-hour format)
     * @return Time in HH:mm format (24-hour)
     */
    private String convertTo24Hour(String time, String period) {
        if (time == null) {
            return "00:00";
        }
        
        String[] parts = time.split(":");
        if (parts.length != 2) {
            return time; // Return as-is if can't parse
        }
        
        try {
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            
            // If period is provided, convert from 12-hour to 24-hour
            if (period != null) {
                boolean isPM = period.trim().equalsIgnoreCase("PM");
                
                if (isPM && hour != 12) {
                    hour += 12;
                } else if (!isPM && hour == 12) {
                    hour = 0;
                }
            }
            
            return String.format("%02d:%02d", hour, minute);
            
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse time components: {}", time, e);
            return time;
        }
    }
    
    /**
     * Generate recommendation based on opening hours
     * 
     * @param hours Parsed hours
     * @param requestedTime Time user wants to visit (can be null)
     * @return Recommendation string
     */
    public String generateRecommendation(ParsedHours hours, String requestedTime) {
        if (hours == null) {
            return "Opening hours not available - verify before visiting";
        }
        
        if (hours.isClosedAllDay()) {
            return "Place is closed on this day - choose a different day";
        }
        
        if (hours.isOpen24Hours()) {
            return "Place is open 24 hours";
        }
        
        if (hours.getOpensAt() != null && hours.getClosesAt() != null) {
            if (requestedTime != null && !isOpenAt(hours, requestedTime)) {
                return String.format("Place is closed at %s. Opens at %s, closes at %s", 
                                   requestedTime, hours.getOpensAt(), hours.getClosesAt());
            }
            
            return String.format("Place is open from %s to %s", 
                               hours.getOpensAt(), hours.getClosesAt());
        }
        
        return "Opening hours available - check before visiting";
    }
}
