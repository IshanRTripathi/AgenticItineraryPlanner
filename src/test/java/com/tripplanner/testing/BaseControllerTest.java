package com.tripplanner.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Base class for controller layer tests with MockMvc configuration.
 */
@WebMvcTest
public abstract class BaseControllerTest {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Autowired
    protected MockMvc mockMvc;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    protected TestDataFactory testDataFactory;
    
    @BeforeEach
    void setUp() {
        testDataFactory = new TestDataFactory(objectMapper);
        setupMockServices();
    }
    
    /**
     * Override this method to setup mock services for the controller.
     */
    protected abstract void setupMockServices();
    
    /**
     * Helper method to perform GET request.
     */
    protected ResultActions performGet(String url, Object... urlVariables) throws Exception {
        logger.debug("Performing GET request to: {}", url);
        return mockMvc.perform(get(url, urlVariables)
                .contentType(MediaType.APPLICATION_JSON));
    }
    
    /**
     * Helper method to perform POST request with JSON body.
     */
    protected ResultActions performPost(String url, Object requestBody) throws Exception {
        logger.debug("Performing POST request to: {} with body: {}", url, requestBody);
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)));
    }
    
    /**
     * Helper method to perform PUT request with JSON body.
     */
    protected ResultActions performPut(String url, Object requestBody, Object... urlVariables) throws Exception {
        logger.debug("Performing PUT request to: {} with body: {}", url, requestBody);
        return mockMvc.perform(put(url, urlVariables)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)));
    }
    
    /**
     * Helper method to perform DELETE request.
     */
    protected ResultActions performDelete(String url, Object... urlVariables) throws Exception {
        logger.debug("Performing DELETE request to: {}", url);
        return mockMvc.perform(delete(url, urlVariables)
                .contentType(MediaType.APPLICATION_JSON));
    }
    
    /**
     * Helper method to convert object to JSON string.
     */
    protected String toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }
    
    /**
     * Helper method to convert JSON string to object.
     */
    protected <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }
}