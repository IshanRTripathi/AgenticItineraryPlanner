package com.tripplanner.testing.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Simple JSON comparison utilities using existing libraries.
 * Uses JSONAssert for straightforward JSON comparison.
 */
public class JsonTestUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Compare two objects as JSON with exact matching.
     */
    public static void assertJsonEquals(Object expected, Object actual) throws Exception {
        String expectedJson = objectMapper.writeValueAsString(expected);
        String actualJson = objectMapper.writeValueAsString(actual);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }
    
    /**
     * Compare two objects as JSON with lenient matching (ignoring field order).
     */
    public static void assertJsonEqualsLenient(Object expected, Object actual) throws Exception {
        String expectedJson = objectMapper.writeValueAsString(expected);
        String actualJson = objectMapper.writeValueAsString(actual);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.LENIENT);
    }
    
    /**
     * Convert object to JSON string.
     */
    public static String toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }
    
    /**
     * Convert JSON string to object.
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }
    
    /**
     * Test serialization round-trip (object -> JSON -> object).
     */
    public static <T> T testSerializationRoundTrip(T original, Class<T> clazz) throws Exception {
        String json = toJson(original);
        return fromJson(json, clazz);
    }
}