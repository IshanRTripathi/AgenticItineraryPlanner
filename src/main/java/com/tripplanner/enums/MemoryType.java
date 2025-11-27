package com.tripplanner.enums;

/**
 * Types of memory entries
 */
public enum MemoryType {
    PREFERENCE,         // User preferences
    RESTRICTION,        // Hard constraints
    DISMISSAL,          // Validation dismissals
    PATTERN,            // Learned patterns
    BEHAVIOR,           // Observed behavior
    EXPLICIT,           // Explicit user commands
    ITINERARY_SUMMARY   // Past trip summaries
}
