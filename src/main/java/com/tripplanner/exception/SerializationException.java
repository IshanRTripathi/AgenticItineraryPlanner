package com.tripplanner.exception;

/**
 * Exception thrown when JSON serialization/deserialization fails.
 */
public class SerializationException extends RuntimeException {
    
    private final String operation;
    private final String dataType;
    
    public SerializationException(String operation, String dataType) {
        super(String.format("Serialization failed during %s of %s", operation, dataType));
        this.operation = operation;
        this.dataType = dataType;
    }
    
    public SerializationException(String operation, String dataType, String message) {
        super(message);
        this.operation = operation;
        this.dataType = dataType;
    }
    
    public SerializationException(String operation, String dataType, String message, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.dataType = dataType;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public String getDataType() {
        return dataType;
    }
}