package com.tripplanner.testing;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of field validation with detailed error information.
 */
public class FieldValidationResult {
    
    private final List<FieldError> errors = new ArrayList<>();
    private final List<FieldSuccess> successes = new ArrayList<>();
    
    /**
     * Validate a field based on annotations and type checking.
     */
    public void validateField(String fieldName, Object value, Class<?> expectedType, boolean hasNotNullAnnotation, boolean hasNotBlankAnnotation) {
        // Check @NotNull annotation
        if (hasNotNullAnnotation && value == null) {
            errors.add(new FieldError(fieldName, "Field marked with @NotNull is null", null, "not null"));
            return;
        }
        
        // Check @NotBlank annotation (only applies to String fields)
        if (hasNotBlankAnnotation && expectedType == String.class) {
            if (value == null) {
                errors.add(new FieldError(fieldName, "Field marked with @NotBlank is null", null, "not blank string"));
                return;
            }
            if (!(value instanceof String)) {
                errors.add(new FieldError(fieldName, "Field marked with @NotBlank is not a String", value.getClass().getSimpleName(), "String"));
                return;
            }
            String stringValue = (String) value;
            if (stringValue.trim().isEmpty()) {
                errors.add(new FieldError(fieldName, "Field marked with @NotBlank is blank", "blank string", "not blank string"));
                return;
            }
        }
        
        // Type validation (only if value is not null)
        if (value != null && !isTypeCompatible(value, expectedType)) {
            errors.add(new FieldError(fieldName, 
                String.format("Type mismatch: expected %s but got %s", expectedType.getSimpleName(), value.getClass().getSimpleName()),
                value.getClass().getSimpleName(), expectedType.getSimpleName()));
            return;
        }
        
        // If we reach here, validation passed
        successes.add(new FieldSuccess(fieldName, expectedType, value));
    }
    
    /**
     * Check if value is compatible with expected type.
     */
    private boolean isTypeCompatible(Object value, Class<?> expectedType) {
        if (expectedType.isInstance(value)) {
            return true;
        }
        
        // Special handling for primitive types and their wrappers
        if (expectedType == Integer.class && value instanceof Integer) return true;
        if (expectedType == Long.class && value instanceof Long) return true;
        if (expectedType == Double.class && value instanceof Double) return true;
        if (expectedType == Boolean.class && value instanceof Boolean) return true;
        if (expectedType == String.class && value instanceof String) return true;
        
        // Special handling for collections
        if (expectedType == java.util.List.class && value instanceof java.util.List) return true;
        if (expectedType == java.util.Map.class && value instanceof java.util.Map) return true;
        
        return false;
    }
    
    public boolean isValid() {
        return errors.isEmpty();
    }
    
    public List<FieldError> getErrors() {
        return new ArrayList<>(errors);
    }
    
    public List<FieldSuccess> getSuccesses() {
        return new ArrayList<>(successes);
    }
    
    public int getValidatedFieldCount() {
        return successes.size() + errors.size();
    }
    
    public int getErrorCount() {
        return errors.size();
    }
    
    public int getSuccessCount() {
        return successes.size();
    }
    
    @Override
    public String toString() {
        return String.format("FieldValidationResult{valid=%s, totalFields=%d, errors=%d, successes=%d}", 
                isValid(), getValidatedFieldCount(), getErrorCount(), getSuccessCount());
    }
    
    /**
     * Represents a field validation error.
     */
    public static class FieldError {
        private final String fieldName;
        private final String message;
        private final String actualValue;
        private final String expectedValue;
        
        public FieldError(String fieldName, String message, String actualValue, String expectedValue) {
            this.fieldName = fieldName;
            this.message = message;
            this.actualValue = actualValue;
            this.expectedValue = expectedValue;
        }
        
        public String getFieldName() {
            return fieldName;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getActualValue() {
            return actualValue;
        }
        
        public String getExpectedValue() {
            return expectedValue;
        }
        
        @Override
        public String toString() {
            return String.format("FieldError{field='%s', message='%s', actual='%s', expected='%s'}", 
                    fieldName, message, actualValue, expectedValue);
        }
    }
    
    /**
     * Represents a successful field validation.
     */
    public static class FieldSuccess {
        private final String fieldName;
        private final Class<?> expectedType;
        private final Object value;
        
        public FieldSuccess(String fieldName, Class<?> expectedType, Object value) {
            this.fieldName = fieldName;
            this.expectedType = expectedType;
            this.value = value;
        }
        
        public String getFieldName() {
            return fieldName;
        }
        
        public Class<?> getExpectedType() {
            return expectedType;
        }
        
        public Object getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return String.format("FieldSuccess{field='%s', type='%s'}", fieldName, expectedType.getSimpleName());
        }
    }
}