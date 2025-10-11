package com.tripplanner.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for Spring Boot application.
 * Provides a unified TaskExecutor configuration to avoid conflicts and warnings.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);
    
    /**
     * Primary TaskExecutor for all async operations.
     * This bean is marked as @Primary to resolve any conflicts.
     */
    @Bean(name = "taskExecutor")
    @Primary
    @Override
    public Executor getAsyncExecutor() {
        logger.info("=== ASYNC CONFIG: CREATING PRIMARY TASK EXECUTOR ===");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - number of threads to keep alive
        executor.setCorePoolSize(5);
        
        // Maximum pool size - maximum number of threads
        executor.setMaxPoolSize(20);
        
        // Queue capacity - number of tasks to queue when all threads are busy
        executor.setQueueCapacity(100);
        
        // Thread name prefix for easier debugging
        executor.setThreadNamePrefix("AsyncTask-");
        
        // Keep alive time for idle threads
        executor.setKeepAliveSeconds(60);
        
        // Allow core threads to timeout
        executor.setAllowCoreThreadTimeOut(true);
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // Maximum time to wait for shutdown
        executor.setAwaitTerminationSeconds(30);
        
        // Initialize the executor
        executor.initialize();
        
        logger.info("Primary TaskExecutor configured:");
        logger.info("  Core Pool Size: {}", executor.getCorePoolSize());
        logger.info("  Max Pool Size: {}", executor.getMaxPoolSize());
        logger.info("  Queue Capacity: {}", executor.getQueueCapacity());
        logger.info("  Thread Name Prefix: {}", executor.getThreadNamePrefix());
        logger.info("  Keep Alive Seconds: {}", executor.getKeepAliveSeconds());
        
        return executor;
    }
    
    /**
     * Exception handler for uncaught async exceptions.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            logger.error("=== ASYNC EXCEPTION HANDLER ===");
            logger.error("Exception in async method: {}", method.getName());
            logger.error("Method parameters: {}", java.util.Arrays.toString(params));
            logger.error("Exception details: {}", throwable.getMessage(), throwable);
            logger.error("===============================");
        };
    }
}