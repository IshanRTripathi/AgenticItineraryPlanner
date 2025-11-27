package com.tripplanner.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration for scheduled tasks.
 * 
 * Creates a primary TaskScheduler bean for @Scheduled annotations.
 * This is separate from:
 * - messageBrokerTaskScheduler (WebSocket operations)
 * - pubsubPublisherThreadPool (GCP Pub/Sub publishing)
 * - globalPubSubSubscriberThreadPoolScheduler (GCP Pub/Sub subscribing)
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(SchedulingConfig.class);
    
    /**
     * Primary TaskScheduler for application scheduled tasks.
     * 
     * Used by:
     * - CacheCleanupScheduler (daily cache cleanup)
     * - LockManager (lock cleanup)
     * - IdempotencyManager (idempotency record cleanup)
     * - RetryPolicy (retry attempt cleanup)
     */
    @Bean(name = "taskScheduler")
    @Primary
    public TaskScheduler taskScheduler() {
        logger.info("Creating primary TaskScheduler for @Scheduled tasks");
        
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5); // Enough for our 4 scheduled tasks
        scheduler.setThreadNamePrefix("Scheduled-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        
        logger.info("TaskScheduler configured:");
        logger.info("  Pool Size: {}", scheduler.getPoolSize());
        logger.info("  Thread Name Prefix: {}", scheduler.getThreadNamePrefix());
        
        return scheduler;
    }
}
