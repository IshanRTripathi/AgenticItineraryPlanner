package com.tripplanner.data.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provider configuration entity for external service providers.
 * Stored in Firestore collection: providerConfigs/{id}
 */
public class ProviderConfig {
    
    @DocumentId
    private String id;
    
    @NotBlank
    @PropertyName("type")
    private String type; // hotels, flights, activities, ground, pt
    
    @NotBlank
    @PropertyName("provider")
    private String provider; // booking, amadeus, viator, etc.
    
    @NotNull
    @PropertyName("enabled")
    private boolean enabled = true;
    
    @PropertyName("priority")
    private int priority = 1; // Lower number = higher priority
    
    @PropertyName("authMeta")
    private Map<String, String> authMeta; // API keys, tokens, etc.
    
    @PropertyName("capabilities")
    private List<String> capabilities; // search, book, cancel, etc.
    
    @PropertyName("configuration")
    private Map<String, Object> configuration; // Provider-specific config
    
    @PropertyName("rateLimits")
    private RateLimits rateLimits;
    
    @PropertyName("healthCheck")
    private HealthCheck healthCheck;
    
    @PropertyName("createdAt")
    private Instant createdAt;
    
    @PropertyName("updatedAt")
    private Instant updatedAt;
    
    public ProviderConfig() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public Map<String, String> getAuthMeta() {
        return authMeta;
    }
    
    public void setAuthMeta(Map<String, String> authMeta) {
        this.authMeta = authMeta;
    }
    
    public List<String> getCapabilities() {
        return capabilities;
    }
    
    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }
    
    public Map<String, Object> getConfiguration() {
        return configuration;
    }
    
    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }
    
    public RateLimits getRateLimits() {
        return rateLimits;
    }
    
    public void setRateLimits(RateLimits rateLimits) {
        this.rateLimits = rateLimits;
    }
    
    public HealthCheck getHealthCheck() {
        return healthCheck;
    }
    
    public void setHealthCheck(HealthCheck healthCheck) {
        this.healthCheck = healthCheck;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderConfig that = (ProviderConfig) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "ProviderConfig{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", provider='" + provider + '\'' +
                ", enabled=" + enabled +
                ", priority=" + priority +
                '}';
    }
    
    // Nested classes
    public static class RateLimits {
        @PropertyName("requestsPerSecond")
        private int requestsPerSecond = 10;
        
        @PropertyName("requestsPerMinute")
        private int requestsPerMinute = 100;
        
        @PropertyName("requestsPerHour")
        private int requestsPerHour = 1000;
        
        @PropertyName("burstCapacity")
        private int burstCapacity = 50;
        
        public RateLimits() {}
        
        // Getters and Setters
        public int getRequestsPerSecond() { return requestsPerSecond; }
        public void setRequestsPerSecond(int requestsPerSecond) { this.requestsPerSecond = requestsPerSecond; }
        public int getRequestsPerMinute() { return requestsPerMinute; }
        public void setRequestsPerMinute(int requestsPerMinute) { this.requestsPerMinute = requestsPerMinute; }
        public int getRequestsPerHour() { return requestsPerHour; }
        public void setRequestsPerHour(int requestsPerHour) { this.requestsPerHour = requestsPerHour; }
        public int getBurstCapacity() { return burstCapacity; }
        public void setBurstCapacity(int burstCapacity) { this.burstCapacity = burstCapacity; }
    }
    
    public static class HealthCheck {
        @PropertyName("enabled")
        private boolean enabled = true;
        
        @PropertyName("url")
        private String url;
        
        @PropertyName("intervalSeconds")
        private int intervalSeconds = 300; // 5 minutes
        
        @PropertyName("timeoutSeconds")
        private int timeoutSeconds = 30;
        
        @PropertyName("lastCheckAt")
        private Instant lastCheckAt;
        
        @PropertyName("status")
        private String status = "unknown"; // healthy, unhealthy, unknown
        
        @PropertyName("consecutiveFailures")
        private int consecutiveFailures = 0;
        
        public HealthCheck() {}
        
        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public int getIntervalSeconds() { return intervalSeconds; }
        public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        public Instant getLastCheckAt() { return lastCheckAt; }
        public void setLastCheckAt(Instant lastCheckAt) { this.lastCheckAt = lastCheckAt; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getConsecutiveFailures() { return consecutiveFailures; }
        public void setConsecutiveFailures(int consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }
    }
}

