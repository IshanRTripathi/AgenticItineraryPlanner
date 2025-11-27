package com.tripplanner.dto.validation;

/**
 * Validation levels for ValidationAdvisor
 */
public enum ValidationLevel {
    BASIC,          // Fast - schema + critical only (~50ms)
    STANDARD,       // Medium - all core validations (~200ms)
    COMPREHENSIVE   // Thorough - all validations + recommendations (~500ms)
}
