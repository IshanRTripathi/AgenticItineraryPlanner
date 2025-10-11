package com.tripplanner.testing;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents the complete result of executing the atomic test suite.
 */
public class TestSuiteResult {
    
    private final boolean success;
    private final TestLayer failedLayer;
    private final List<TestResult> layerResults;
    private final Instant startTime;
    private final Instant endTime;
    private final Duration totalExecutionTime;
    
    public TestSuiteResult(boolean success, TestLayer failedLayer, List<TestResult> layerResults, 
                          Instant startTime, Instant endTime) {
        this.success = success;
        this.failedLayer = failedLayer;
        this.layerResults = new ArrayList<>(layerResults);
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalExecutionTime = Duration.between(startTime, endTime);
    }
    
    public static TestSuiteResult success(List<TestResult> layerResults, Instant startTime, Instant endTime) {
        return new TestSuiteResult(true, null, layerResults, startTime, endTime);
    }
    
    public static TestSuiteResult failure(TestLayer failedLayer, List<TestResult> layerResults, 
                                        Instant startTime, Instant endTime) {
        return new TestSuiteResult(false, failedLayer, layerResults, startTime, endTime);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public TestLayer getFailedLayer() {
        return failedLayer;
    }
    
    public List<TestResult> getLayerResults() {
        return new ArrayList<>(layerResults);
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public Duration getTotalExecutionTime() {
        return totalExecutionTime;
    }
    
    public int getTotalTests() {
        return layerResults.stream().mapToInt(TestResult::getTotalTests).sum();
    }
    
    public int getTotalPassedTests() {
        return layerResults.stream().mapToInt(TestResult::getPassedTests).sum();
    }
    
    public int getTotalFailedTests() {
        return layerResults.stream().mapToInt(TestResult::getFailedTests).sum();
    }
    
    public List<TestFailure> getAllFailures() {
        return layerResults.stream()
                .flatMap(result -> result.getFailures().stream())
                .collect(Collectors.toList());
    }
    
    public double getOverallSuccessRate() {
        int totalTests = getTotalTests();
        if (totalTests == 0) return 0.0;
        return (double) getTotalPassedTests() / totalTests * 100.0;
    }
    
    public Map<TestLayer, TestResult> getResultsByLayer() {
        return layerResults.stream()
                .collect(Collectors.toMap(TestResult::getLayer, result -> result));
    }
    
    public TestResult getResultForLayer(TestLayer layer) {
        return layerResults.stream()
                .filter(result -> result.getLayer() == layer)
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public String toString() {
        return String.format("TestSuiteResult{success=%s, failedLayer=%s, totalTests=%d, passedTests=%d, failedTests=%d, totalTime=%s}",
                success, failedLayer, getTotalTests(), getTotalPassedTests(), getTotalFailedTests(), totalExecutionTime);
    }
}