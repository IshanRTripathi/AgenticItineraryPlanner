package com.tripplanner.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time communication.
 * Provides messaging capabilities for itinerary updates and agent coordination.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        logger.info("=== WEBSOCKET CONFIG: CONFIGURING MESSAGE BROKER ===");
        
        // Enable a simple memory-based message broker to carry messages back to the client
        config.enableSimpleBroker("/topic", "/queue");
        
        // Set application destination prefix for messages bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        
        // Set user destination prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
        
        logger.info("Message broker configured successfully");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        logger.info("=== WEBSOCKET CONFIG: REGISTERING STOMP ENDPOINTS ===");
        
        // Register STOMP endpoint for WebSocket connections
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Allow all origins for development
                .withSockJS()
                .setSessionCookieNeeded(false) // Prevent session cookie issues
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"); // Use CDN for consistency
        
        logger.info("STOMP endpoints registered successfully with session management optimizations");
    }
    
    /**
     * Dedicated task executor for WebSocket operations.
     * Separate from main application thread pool to prevent blocking.
     */
    @Bean(name = "webSocketTaskExecutor")
    public ThreadPoolTaskExecutor webSocketTaskExecutor() {
        logger.info("=== WEBSOCKET CONFIG: CREATING DEDICATED TASK EXECUTOR ===");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("WebSocket-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        
        logger.info("WebSocket TaskExecutor configured:");
        logger.info("  Core Pool Size: {}", executor.getCorePoolSize());
        logger.info("  Max Pool Size: {}", executor.getMaxPoolSize());
        logger.info("  Queue Capacity: {}", executor.getQueueCapacity());
        logger.info("  Thread Name Prefix: {}", executor.getThreadNamePrefix());
        
        return executor;
    }
}