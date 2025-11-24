package com.tripplanner.service.agents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Coordinates agent execution to prevent concurrent modifications
 * Provides locking mechanism for itinerary-level operations
 */
@Service
public class AgentCoordinator {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentCoordinator.class);
    
    private final Map<String, ReentrantLock> itineraryLocks = new ConcurrentHashMap<>();
    private final Map<String, AgentExecutionInfo> activeExecutions = new ConcurrentHashMap<>();
    
    /**
     * Execute operation with itinerary-level lock
     */
    public <T> T executeWithLock(String itineraryId, String agentName, Supplier<T> operation) {
        ReentrantLock lock = itineraryLocks.computeIfAbsent(itineraryId, k -> new ReentrantLock());
        
        logger.info("Agent {} acquiring lock for itinerary {}", agentName, itineraryId);
        lock.lock();
        
        try {
            // Record execution start
            AgentExecutionInfo info = new AgentExecutionInfo(agentName, System.currentTimeMillis());
            activeExecutions.put(itineraryId, info);
            
            logger.info("Agent {} executing for itinerary {}", agentName, itineraryId);
            T result = operation.get();
            
            logger.info("Agent {} completed for itinerary {}", agentName, itineraryId);
            return result;
            
        } finally {
            // Record execution end
            activeExecutions.remove(itineraryId);
            
            lock.unlock();
            logger.info("Agent {} released lock for itinerary {}", agentName, itineraryId);
        }
    }
    
    /**
     * Execute operation with itinerary-level lock (void return)
     */
    public void executeWithLock(String itineraryId, String agentName, Runnable operation) {
        executeWithLock(itineraryId, agentName, () -> {
            operation.run();
            return null;
        });
    }
    
    /**
     * Check if an agent is currently executing for an itinerary
     */
    public boolean isExecuting(String itineraryId) {
        return activeExecutions.containsKey(itineraryId);
    }
    
    /**
     * Get currently executing agent for an itinerary
     */
    public String getExecutingAgent(String itineraryId) {
        AgentExecutionInfo info = activeExecutions.get(itineraryId);
        return info != null ? info.getAgentName() : null;
    }
    
    /**
     * Get execution duration for currently executing agent
     */
    public Long getExecutionDuration(String itineraryId) {
        AgentExecutionInfo info = activeExecutions.get(itineraryId);
        if (info != null) {
            return System.currentTimeMillis() - info.getStartTime();
        }
        return null;
    }
    
    /**
     * Clean up locks for completed itineraries
     */
    public void cleanupLocks(String itineraryId) {
        itineraryLocks.remove(itineraryId);
        activeExecutions.remove(itineraryId);
        logger.info("Cleaned up locks for itinerary {}", itineraryId);
    }
    
    /**
     * Information about agent execution
     */
    private static class AgentExecutionInfo {
        private final String agentName;
        private final long startTime;
        
        public AgentExecutionInfo(String agentName, long startTime) {
            this.agentName = agentName;
            this.startTime = startTime;
        }
        
        public String getAgentName() {
            return agentName;
        }
        
        public long getStartTime() {
            return startTime;
        }
    }
}
