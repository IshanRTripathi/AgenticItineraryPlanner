package com.tripplanner.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Circuit breaker pattern implementation for AI providers.
 * 
 * Prevents repeated calls to a consistently failing provider by "opening the circuit"
 * after a threshold of consecutive failures. The circuit stays open for a timeout period,
 * then enters a half-open state to test if the provider has recovered.
 * 
 * States:
 * - CLOSED: Normal operation, requests allowed
 * - OPEN: Provider is failing, requests blocked
 * - HALF_OPEN: Testing recovery, one request allowed
 */
public class CircuitBreaker {
    
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);
    
    /**
     * Circuit breaker states.
     */
    public enum State {
        /** Normal operation - requests are allowed */
        CLOSED,
        
        /** Provider is failing - requests are blocked */
        OPEN,
        
        /** Testing recovery - one request allowed to test */
        HALF_OPEN
    }
    
    private final String providerName;
    private State state;
    private int consecutiveFailures;
    private long circuitOpenedAt;
    
    // Configuration
    private static final int FAILURE_THRESHOLD = 5;      // Open circuit after 5 failures
    private static final long TIMEOUT_MS = 60000;        // Keep circuit open for 60 seconds
    
    /**
     * Create a new circuit breaker for a provider.
     * 
     * @param providerName Name of the provider (for logging)
     */
    public CircuitBreaker(String providerName) {
        this.providerName = providerName;
        this.state = State.CLOSED;
        this.consecutiveFailures = 0;
        this.circuitOpenedAt = 0;
    }
    
    /**
     * Check if a request should be allowed through the circuit breaker.
     * 
     * @return true if request is allowed, false if circuit is open
     */
    public synchronized boolean allowRequest() {
        if (state == State.CLOSED) {
            return true;
        }
        
        if (state == State.OPEN) {
            long elapsed = System.currentTimeMillis() - circuitOpenedAt;
            if (elapsed > TIMEOUT_MS) {
                logger.info("Circuit breaker for {} transitioning to HALF_OPEN after {}ms timeout", 
                           providerName, elapsed);
                state = State.HALF_OPEN;
                return true;
            }
            
            logger.debug("Circuit breaker for {} is OPEN, blocking request ({}ms remaining)", 
                        providerName, TIMEOUT_MS - elapsed);
            return false;
        }
        
        // HALF_OPEN: allow one request to test
        logger.debug("Circuit breaker for {} is HALF_OPEN, allowing test request", providerName);
        return true;
    }
    
    /**
     * Record a successful request.
     * Resets the failure counter and closes the circuit if it was open.
     */
    public synchronized void recordSuccess() {
        int previousFailures = consecutiveFailures;
        State previousState = state;
        
        consecutiveFailures = 0;
        
        if (state == State.HALF_OPEN) {
            logger.info("Circuit breaker for {} recovered! Transitioning from HALF_OPEN to CLOSED", 
                       providerName);
            state = State.CLOSED;
        } else if (state == State.OPEN) {
            // Shouldn't happen (request shouldn't have been allowed), but handle it
            logger.warn("Circuit breaker for {} received success while OPEN, closing circuit", 
                       providerName);
            state = State.CLOSED;
        } else if (previousFailures > 0) {
            logger.debug("Circuit breaker for {} recorded success, reset failure count from {}", 
                        providerName, previousFailures);
        }
    }
    
    /**
     * Record a failed request.
     * Increments the failure counter and opens the circuit if threshold is reached.
     */
    public synchronized void recordFailure() {
        consecutiveFailures++;
        
        if (state == State.HALF_OPEN) {
            logger.warn("Circuit breaker for {} failed in HALF_OPEN state, reopening circuit", 
                       providerName);
            state = State.OPEN;
            circuitOpenedAt = System.currentTimeMillis();
        } else if (consecutiveFailures >= FAILURE_THRESHOLD && state == State.CLOSED) {
            logger.error("Circuit breaker for {} OPENED after {} consecutive failures", 
                        providerName, consecutiveFailures);
            state = State.OPEN;
            circuitOpenedAt = System.currentTimeMillis();
        } else {
            logger.debug("Circuit breaker for {} recorded failure {}/{}", 
                        providerName, consecutiveFailures, FAILURE_THRESHOLD);
        }
    }
    
    /**
     * Get the current state of the circuit breaker.
     * 
     * @return Current state (CLOSED, OPEN, or HALF_OPEN)
     */
    public synchronized State getState() {
        // Check if we should transition from OPEN to HALF_OPEN
        if (state == State.OPEN) {
            long elapsed = System.currentTimeMillis() - circuitOpenedAt;
            if (elapsed > TIMEOUT_MS) {
                return State.HALF_OPEN;
            }
        }
        return state;
    }
    
    /**
     * Get the number of consecutive failures.
     * 
     * @return Consecutive failure count
     */
    public synchronized int getConsecutiveFailures() {
        return consecutiveFailures;
    }
    
    /**
     * Get the timestamp when the circuit was opened.
     * 
     * @return Timestamp in milliseconds, or 0 if circuit is not open
     */
    public synchronized long getCircuitOpenedAt() {
        return circuitOpenedAt;
    }
    
    /**
     * Get the provider name.
     * 
     * @return Provider name
     */
    public String getProviderName() {
        return providerName;
    }
    
    /**
     * Get the failure threshold.
     * 
     * @return Number of failures before circuit opens
     */
    public static int getFailureThreshold() {
        return FAILURE_THRESHOLD;
    }
    
    /**
     * Get the timeout duration.
     * 
     * @return Timeout in milliseconds
     */
    public static long getTimeoutMs() {
        return TIMEOUT_MS;
    }
    
    /**
     * Reset the circuit breaker to initial state (for testing).
     */
    public synchronized void reset() {
        logger.info("Circuit breaker for {} manually reset", providerName);
        state = State.CLOSED;
        consecutiveFailures = 0;
        circuitOpenedAt = 0;
    }
    
    @Override
    public synchronized String toString() {
        return String.format("CircuitBreaker[provider=%s, state=%s, failures=%d/%d, openedAt=%d]",
                providerName, state, consecutiveFailures, FAILURE_THRESHOLD, circuitOpenedAt);
    }
}
