package com.tripplanner.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Centralized utility for formatting time-of-day for LLM consumption and human readability.
 * 
 * Design Philosophy - TIMEZONE-INDEPENDENT:
 * - Itinerary planning uses RELATIVE time-of-day (e.g., "9:00 AM" = 9 AM local time at destination)
 * - Storage uses Long (milliseconds from midnight: 0-86400000) for efficiency
 * - LLMs receive formatted strings for readability
 * - NO timezone conversions needed - travelers think in local time at destination
 * 
 * Why Timezone-Independent?
 * - User thinks: "I want to start my day at 9 AM" (regardless of timezone)
 * - Destination time is what matters, not absolute UTC time
 * - Simplifies logic and avoids timezone conversion bugs
 * - Works consistently across all destinations
 * 
 * Format Standards:
 * - LLM Display: "h:mm a" (12-hour with AM/PM) - e.g., "9:00 AM", "2:30 PM"
 * - LLM Generation: "HH:mm" (24-hour) - e.g., "09:00", "14:30"
 * - Storage: Long (milliseconds from midnight) - e.g., 32400000 = 9:00 AM
 * 
 * Examples:
 * - 0 ms = 12:00 AM (midnight)
 * - 32400000 ms = 9:00 AM
 * - 43200000 ms = 12:00 PM (noon)
 * - 64800000 ms = 6:00 PM
 * - 86400000 ms = 12:00 AM (next day)
 */
public class TimeFormatter {
    
    private static final Logger logger = LoggerFactory.getLogger(TimeFormatter.class);
    
    // Standard formatters
    private static final DateTimeFormatter TWELVE_HOUR_FORMAT = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter TWENTY_FOUR_HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    
    /**
     * Format time-of-day to 12-hour format with AM/PM for LLM display.
     * This is the most natural format for English-speaking LLMs.
     * 
     * TIMEZONE-INDEPENDENT: Works with relative time-of-day (0-86400000 ms from midnight).
     * 
     * @param timeOfDayMs Milliseconds from midnight (0-86400000)
     * @return Formatted time string (e.g., "9:00 AM", "2:30 PM") or "N/A" if null/invalid
     */
    public static String formatFor12HourDisplay(Long timeOfDayMs) {
        if (timeOfDayMs == null) {
            return "N/A";
        }
        
        try {
            // Convert milliseconds to LocalTime (timezone-independent)
            long seconds = timeOfDayMs / 1000;
            LocalTime time = LocalTime.ofSecondOfDay(seconds);
            return time.format(TWELVE_HOUR_FORMAT);
        } catch (Exception e) {
            logger.warn("Failed to format time-of-day {} ms: {}", timeOfDayMs, e.getMessage());
            return "N/A";
        }
    }
    
    /**
     * @deprecated Use formatFor12HourDisplay(Long) instead. Timezones are not needed for itinerary planning.
     */
    @Deprecated
    public static String formatFor12HourDisplay(Long timestamp, ZoneId zoneId) {
        return formatFor12HourDisplay(timestamp);
    }
    
    /**
     * Format time-of-day to 24-hour format for LLM generation.
     * This format is used when LLMs generate time values.
     * 
     * TIMEZONE-INDEPENDENT: Works with relative time-of-day (0-86400000 ms from midnight).
     * 
     * @param timeOfDayMs Milliseconds from midnight (0-86400000)
     * @return Formatted time string (e.g., "09:00", "14:30") or "N/A" if null/invalid
     */
    public static String formatFor24HourDisplay(Long timeOfDayMs) {
        if (timeOfDayMs == null) {
            return "N/A";
        }
        
        try {
            // Convert milliseconds to LocalTime (timezone-independent)
            long seconds = timeOfDayMs / 1000;
            LocalTime time = LocalTime.ofSecondOfDay(seconds);
            return time.format(TWENTY_FOUR_HOUR_FORMAT);
        } catch (Exception e) {
            logger.warn("Failed to format time-of-day {} ms: {}", timeOfDayMs, e.getMessage());
            return "N/A";
        }
    }
    
    /**
     * @deprecated Use formatFor24HourDisplay(Long) instead. Timezones are not needed for itinerary planning.
     */
    @Deprecated
    public static String formatFor24HourDisplay(Long timestamp, ZoneId zoneId) {
        return formatFor24HourDisplay(timestamp);
    }
    
    /**
     * Format time-of-day for debugging.
     * 
     * TIMEZONE-INDEPENDENT: Works with relative time-of-day (0-86400000 ms from midnight).
     * 
     * @param timeOfDayMs Milliseconds from midnight (0-86400000)
     * @return Formatted time string (e.g., "09:00 AM (32400000 ms)") or "N/A" if null/invalid
     */
    public static String formatForDebug(Long timeOfDayMs) {
        if (timeOfDayMs == null) {
            return "N/A";
        }
        
        try {
            long seconds = timeOfDayMs / 1000;
            LocalTime time = LocalTime.ofSecondOfDay(seconds);
            return String.format("%s (%d ms)", 
                               time.format(TWELVE_HOUR_FORMAT), timeOfDayMs);
        } catch (Exception e) {
            logger.warn("Failed to format time-of-day {} ms for debug: {}", timeOfDayMs, e.getMessage());
            return String.valueOf(timeOfDayMs) + " ms";
        }
    }
    
    /**
     * Format time range for display.
     * 
     * @param startTime Start timestamp in milliseconds
     * @param endTime End timestamp in milliseconds
     * @return Formatted range (e.g., "9:00 AM - 11:30 AM") or "N/A" if invalid
     */
    public static String formatTimeRange(Long startTime, Long endTime) {
        if (startTime == null && endTime == null) {
            return "N/A";
        }
        
        if (startTime == null) {
            return "ends at " + formatFor12HourDisplay(endTime);
        }
        
        if (endTime == null) {
            return "starts at " + formatFor12HourDisplay(startTime);
        }
        
        return formatFor12HourDisplay(startTime) + " - " + formatFor12HourDisplay(endTime);
    }
    
    /**
     * Convert 24-hour time string to milliseconds (time-of-day only, no date).
     * Used for parsing LLM-generated time strings.
     * 
     * @param timeStr Time string in "HH:mm" format (e.g., "09:00", "14:30")
     * @return Milliseconds representing time-of-day (0-86400000) or null if invalid
     */
    public static Long parseTimeOfDay(String timeStr) {
        if (timeStr == null || !timeStr.matches("^([0-1][0-9]|2[0-3]):[0-5][0-9]$")) {
            logger.warn("Invalid time format: {}", timeStr);
            return null;
        }
        
        try {
            LocalTime time = LocalTime.parse(timeStr, TWENTY_FOUR_HOUR_FORMAT);
            return time.toSecondOfDay() * 1000L;
        } catch (Exception e) {
            logger.warn("Failed to parse time string {}: {}", timeStr, e.getMessage());
            return null;
        }
    }
    
    /**
     * @deprecated Not needed for itinerary planning. Use parseTimeOfDay() instead.
     * Itineraries use relative time-of-day, not absolute timestamps with dates.
     */
    @Deprecated
    public static Long parseTimeWithDate(String timeStr, String date, ZoneId zoneId) {
        // For backward compatibility, just parse as time-of-day
        return parseTimeOfDay(timeStr);
    }
    
    /**
     * Check if a timestamp represents a valid time (not just time-of-day).
     * Time-of-day values are typically < 86400000 (24 hours in milliseconds).
     * 
     * @param timestamp Milliseconds value
     * @return true if this appears to be a full timestamp, false if it's just time-of-day
     */
    public static boolean isFullTimestamp(Long timestamp) {
        if (timestamp == null) {
            return false;
        }
        
        // If less than 24 hours in milliseconds, it's likely just time-of-day
        return timestamp >= 86400000L;
    }
    
    /**
     * Get a human-readable description of what a timestamp represents.
     * Useful for debugging timestamp issues.
     * 
     * @param timestamp Milliseconds value
     * @return Description string
     */
    public static String describeTimestamp(Long timestamp) {
        if (timestamp == null) {
            return "null timestamp";
        }
        
        if (!isFullTimestamp(timestamp)) {
            long hours = timestamp / (60 * 60 * 1000);
            long minutes = (timestamp % (60 * 60 * 1000)) / (60 * 1000);
            return String.format("time-of-day only: %d hours %d minutes (%d ms)", 
                               hours, minutes, timestamp);
        }
        
        return String.format("full timestamp: %s (%d ms)", 
                           formatForDebug(timestamp), timestamp);
    }
}
