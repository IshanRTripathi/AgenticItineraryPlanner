package com.tripplanner.testing;

import java.time.Instant;

/**
 * Represents a test failure with detailed information.
 */
public class TestFailure {
    
    private final String testClass;
    private final String testMethod;
    private final String errorMessage;
    private final String stackTrace;
    private final Instant timestamp;
    private final TestLayer layer;
    
    public TestFailure(String testClass, String testMethod, String errorMessage, 
                      String stackTrace, TestLayer layer) {
        this.testClass = testClass;
        this.testMethod = testMethod;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.layer = layer;
        this.timestamp = Instant.now();
    }
    
    public String getTestClass() {
        return testClass;
    }
    
    public String getTestMethod() {
        return testMethod;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public String getStackTrace() {
        return stackTrace;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public TestLayer getLayer() {
        return layer;
    }
    
    public String getFullTestName() {
        return testClass + "." + testMethod;
    }
    
    @Override
    public String toString() {
        return String.format("TestFailure{test=%s.%s, layer=%s, error=%s}", 
                testClass, testMethod, layer, errorMessage);
    }
}