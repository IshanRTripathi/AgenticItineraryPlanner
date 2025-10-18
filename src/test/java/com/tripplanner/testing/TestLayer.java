package com.tripplanner.testing;

/**
 * Enum representing the different layers of atomic testing.
 * Tests must be executed in this exact order.
 */
public enum TestLayer {
    DTO("DTO Layer - Data Transfer Objects"),
    SERVICE("Service Layer - Business Logic"),
    AGENT("Agent Layer - LLM Agents"),
    CONTROLLER("Controller Layer - REST Endpoints"),
    INTEGRATION("Integration Layer - Service Interactions"),
    ORCHESTRATION("Orchestration Layer - Agent Coordination"),
    END_TO_END("End-to-End Layer - Complete Workflows");
    
    private final String description;
    
    TestLayer(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the next layer in the execution order.
     * Returns null if this is the last layer.
     */
    public TestLayer getNextLayer() {
        TestLayer[] layers = TestLayer.values();
        int currentIndex = this.ordinal();
        if (currentIndex < layers.length - 1) {
            return layers[currentIndex + 1];
        }
        return null;
    }
    
    /**
     * Check if this layer can be executed after the given layer.
     */
    public boolean canExecuteAfter(TestLayer previousLayer) {
        if (previousLayer == null) {
            return this == DTO; // First layer must be DTO
        }
        return this.ordinal() == previousLayer.ordinal() + 1;
    }
}