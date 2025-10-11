package com.tripplanner.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for atomic testing framework.
 */
@TestConfiguration
public class AtomicTestConfiguration {
    
    /**
     * Configure ObjectMapper for consistent JSON serialization/deserialization testing.
     */
    @Bean
    @Primary
    public ObjectMapper testObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Configure for consistent testing
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        return mapper;
    }
    
    /**
     * Test data factory bean for consistent test data creation.
     */
    @Bean
    public TestDataFactory testDataFactory(ObjectMapper objectMapper) {
        return new TestDataFactory(objectMapper);
    }
    
    /**
     * Atomic test execution engine bean.
     */
    @Bean
    public AtomicTestExecutionEngine atomicTestExecutionEngine() {
        return new AtomicTestExecutionEngine();
    }
}