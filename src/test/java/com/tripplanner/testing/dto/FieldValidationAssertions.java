package com.tripplanner.testing.dto;

import com.tripplanner.testing.FieldValidationResult;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple assertion helpers for field validation using existing AssertJ methods.
 * No over-engineering - just convenient static methods.
 */
public class FieldValidationAssertions {
    
    /**
     * Assert that validation result is valid with no errors.
     */
    public static void assertValidationPassed(FieldValidationResult result) {
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrorCount()).isEqualTo(0);
    }
    
    /**
     * Assert that validation result failed with specific error count.
     */
    public static void assertValidationFailed(FieldValidationResult result, int expectedErrorCount) {
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCount()).isEqualTo(expectedErrorCount);
    }
    
    /**
     * Assert that validation result has specific field count.
     */
    public static void assertFieldCount(FieldValidationResult result, int expectedFieldCount) {
        assertThat(result.getValidatedFieldCount()).isEqualTo(expectedFieldCount);
    }
    
    /**
     * Assert that validation result has error for specific field.
     */
    public static void assertHasFieldError(FieldValidationResult result, String fieldName) {
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals(fieldName))).isTrue();
    }
    
    /**
     * Assert that validation result has no error for specific field.
     */
    public static void assertNoFieldError(FieldValidationResult result, String fieldName) {
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals(fieldName))).isFalse();
    }
    
    /**
     * Assert that validation result has error with specific message content.
     */
    public static void assertHasErrorMessage(FieldValidationResult result, String messageContent) {
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getMessage().contains(messageContent))).isTrue();
    }
}