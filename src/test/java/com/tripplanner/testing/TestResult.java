package com.tripplanner.testing;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of executing tests for a specific layer.
 */
public class TestResult {
    
    private final TestLayer layer;
    private final boolean success;
    private final List<TestFailure> failures;
    private final int totalTests;
    private final int passedTests;
    private final Instant startTime;
    private final Instant endTime;
    private final Duration executionTime;
    
    public TestResult(TestLayer layer, boolean success, List<TestFailure> failures, 
                     int totalTests, int passedTests, Instant startTime, Instant endTime) {
        this.layer = layer;
        this.success = success;
        this.failures = new ArrayList<>(failures);
        this.totalTests = totalTests;
        this.passedTests = passedTests;
        this.startTime = startTime;
        this.endTime = endTime;
        this.executionTime = Duration.between(startTime, endTime);
    }
    
    public static TestResult success(TestLayer layer, int totalTests, Instant startTime, Instant endTime) {
        return new TestResult(layer, true, new ArrayList<>(), totalTests, totalTests, startTime, endTime);
    }
    
    public static TestResult failure(TestLayer layer, List<TestFailure> failures, 
                                   int totalTests, int passedTests, Instant startTime, Instant endTime) {
        return new TestResult(layer, false, failures, totalTests, passedTests, startTime, endTime);
    }
    
    public TestLayer getLayer() {
        return layer;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public List<TestFailure> getFailures() {
        return new ArrayList<>(failures);
    }
    
    public int getTotalTests() {
        return totalTests;
    }
    
    public int getPassedTests() {
        return passedTests;
    }
    
    public int getFailedTests() {
        return failures.size();
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public Duration getExecutionTime() {
        return executionTime;
    }
    
    public double getSuccessRate() {
        if (totalTests == 0) return 0.0;
        return (double) passedTests / totalTests * 100.0;
    }
    
    @Override
    public String toString() {
        return String.format("TestResult{layer=%s, success=%s, totalTests=%d, passedTests=%d, failedTests=%d, executionTime=%s}",
                layer, success, totalTests, passedTests, getFailedTests(), executionTime);
    }
}