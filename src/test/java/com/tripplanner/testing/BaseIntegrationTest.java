package com.tripplanner.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for integration tests with Spring Boot test context.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "logging.level.com.tripplanner.testing=DEBUG"
})
public abstract class BaseIntegrationTest {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    protected TestDataFactory testDataFactory;
    
    @BeforeEach
    void setUp() {
        testDataFactory = new TestDataFactory(objectMapper);
        setupTestData();
        setupIntegrationMocks();
    }
    
    /**
     * Override this method to setup test-specific data.
     */
    protected abstract void setupTestData();
    
    /**
     * Override this method to setup integration-specific mocks.
     * Note: Use sparingly - integration tests should use real implementations where possible.
     */
    protected abstract void setupIntegrationMocks();
    
    /**
     * Helper method to wait for async operations to complete.
     */
    protected void waitForAsyncCompletion(long timeoutMs) {
        try {
            Thread.sleep(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for async completion", e);
        }
    }
    
    /**
     * Helper method to verify Spring context loads correctly.
     */
    protected void verifySpringContextLoads() {
        logger.info("Spring context loaded successfully for {}", getClass().getSimpleName());
        // Additional context verification can be added here
    }
}