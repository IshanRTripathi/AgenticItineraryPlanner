package com.tripplanner.service.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import com.tripplanner.dto.cache.CacheStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ToolCacheService implementation.
 * 
 * Tests cache hit/miss scenarios, TTL expiration, and type safety.
 */
@ExtendWith(MockitoExtension.class)
public class ToolCacheServiceTest {
    
    @Mock
    private Firestore firestore;
    
    private ObjectMapper objectMapper;
    private FirestoreToolCacheService cacheService;
    
    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        cacheService = new FirestoreToolCacheService(firestore, objectMapper);
    }
    
    @Test
    public void testCacheMiss_CallsToolFunction() {
        // Arrange
        String itineraryId = "test-123";
        String toolType = "test-tool";
        String cacheKey = "test:key";
        TestRequest request = new TestRequest("test");
        AtomicInteger callCount = new AtomicInteger(0);
        
        // Act
        TestResult result = cacheService.getOrCompute(
            itineraryId,
            toolType,
            cacheKey,
            request,
            () -> {
                callCount.incrementAndGet();
                return new TestResult("result");
            },
            TestResult.class
        );
        
        // Assert
        assertNotNull(result);
        assertEquals("result", result.getValue());
        assertEquals(1, callCount.get(), "Tool should be called once on cache miss");
    }
    
    @Test
    public void testCacheHit_DoesNotCallToolFunction() {
        // Arrange
        String itineraryId = "test-123";
        String toolType = "test-tool";
        String cacheKey = "test:key";
        TestRequest request = new TestRequest("test");
        AtomicInteger callCount = new AtomicInteger(0);
        
        // First call - cache miss
        TestResult firstResult = cacheService.getOrCompute(
            itineraryId,
            toolType,
            cacheKey,
            request,
            () -> {
                callCount.incrementAndGet();
                return new TestResult("result");
            },
            TestResult.class
        );
        
        // Second call - should be cache hit
        TestResult secondResult = cacheService.getOrCompute(
            itineraryId,
            toolType,
            cacheKey,
            request,
            () -> {
                callCount.incrementAndGet();
                return new TestResult("result");
            },
            TestResult.class
        );
        
        // Assert
        assertNotNull(firstResult);
        assertNotNull(secondResult);
        assertEquals("result", firstResult.getValue());
        assertEquals("result", secondResult.getValue());
        assertEquals(1, callCount.get(), "Tool should only be called once (second call is cache hit)");
    }
    
    @Test
    public void testGetStats_ReturnsCorrectMetrics() {
        // Arrange
        String itineraryId = "test-123";
        String toolType = "test-tool";
        TestRequest request = new TestRequest("test");
        
        // Make some cache calls
        cacheService.getOrCompute(itineraryId, toolType, "key1", request,
            () -> new TestResult("result1"), TestResult.class);
        
        cacheService.getOrCompute(itineraryId, toolType, "key1", request,
            () -> new TestResult("result1"), TestResult.class); // Cache hit
        
        cacheService.getOrCompute(itineraryId, toolType, "key2", request,
            () -> new TestResult("result2"), TestResult.class);
        
        // Act
        CacheStats stats = cacheService.getStats(itineraryId);
        
        // Assert
        assertNotNull(stats);
        assertEquals(itineraryId, stats.getItineraryId());
        assertEquals(2, stats.getTotalEntries()); // 2 unique keys
        assertEquals(1, stats.getTotalHits()); // 1 cache hit
        assertEquals(2, stats.getTotalMisses()); // 2 cache misses
        assertTrue(stats.getHitRate() > 0.0 && stats.getHitRate() < 1.0);
    }
    
    @Test
    public void testInvalidate_RemovesCacheEntry() {
        // Arrange
        String itineraryId = "test-123";
        String toolType = "test-tool";
        String cacheKey = "test:key";
        TestRequest request = new TestRequest("test");
        AtomicInteger callCount = new AtomicInteger(0);
        
        // First call - cache miss
        cacheService.getOrCompute(itineraryId, toolType, cacheKey, request,
            () -> {
                callCount.incrementAndGet();
                return new TestResult("result");
            }, TestResult.class);
        
        // Invalidate
        cacheService.invalidate(itineraryId, cacheKey);
        
        // Second call - should be cache miss again
        cacheService.getOrCompute(itineraryId, toolType, cacheKey, request,
            () -> {
                callCount.incrementAndGet();
                return new TestResult("result");
            }, TestResult.class);
        
        // Assert
        assertEquals(2, callCount.get(), "Tool should be called twice (cache was invalidated)");
    }
    
    // Test DTOs
    
    static class TestRequest {
        private String value;
        
        public TestRequest() {}
        
        public TestRequest(String value) {
            this.value = value;
        }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
    
    static class TestResult {
        private String value;
        
        public TestResult() {}
        
        public TestResult(String value) {
            this.value = value;
        }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}
