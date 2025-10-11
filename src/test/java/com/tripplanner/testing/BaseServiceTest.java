package com.tripplanner.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.service.DatabaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for service layer tests with common mock setup.
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseServiceTest {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Mock
    protected DatabaseService databaseService;
    
    @Mock
    protected ObjectMapper objectMapper;
    
    protected TestDataFactory testDataFactory;
    
    @BeforeEach
    protected void setUp() {
        testDataFactory = new TestDataFactory(new ObjectMapper());
        setupCommonMocks();
        setupSpecificMocks();
    }
    
    /**
     * Setup common mock behaviors that apply to all service tests.
     */
    protected void setupCommonMocks() {
        // Common mock setups can be added here
        logger.debug("Setting up common mocks for {}", getClass().getSimpleName());
    }
    
    /**
     * Override this method to setup test-specific mocks.
     */
    protected abstract void setupSpecificMocks();
    
    /**
     * Verify no unexpected mock interactions occurred.
     * Call this at the end of tests to ensure clean mock usage.
     */
    protected void verifyNoUnexpectedInteractions() {
        // This can be implemented to verify mock interactions
        logger.debug("Verifying no unexpected interactions for {}", getClass().getSimpleName());
    }
    
    /**
     * Helper method to create a mock exception for testing error scenarios.
     */
    protected RuntimeException createMockException(String message) {
        return new RuntimeException("Mock exception: " + message);
    }
}