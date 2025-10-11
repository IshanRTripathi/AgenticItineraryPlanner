package com.tripplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages system alerts and proactive notifications.
 * Monitors metrics and triggers alerts based on configurable thresholds.
 */
@Service
public class AlertManager {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertManager.class);
    
    private final SystemMetrics systemMetrics;
    private final TraceManager traceManager;
    
    // Active alerts
    private final Map<String, Alert> activeAlerts = new ConcurrentHashMap<>();
    
    // Alert history for analysis
    private final List<Alert> alertHistory = new ArrayList<>();
    
    // Alert configuration
    private final AlertConfiguration config = new AlertConfiguration();
    
    // Alert handlers
    private final List<AlertHandler> alertHandlers = new ArrayList<>();
    
    public AlertManager(SystemMetrics systemMetrics, TraceManager traceManager) {
        this.systemMetrics = systemMetrics;
        this.traceManager = traceManager;
        
        // Initialize default alert handlers
        alertHandlers.add(new LoggingAlertHandler());
        // Additional handlers can be added (email, Slack, etc.)
    }
    
    /**
     * Check all metrics and trigger alerts if thresholds are exceeded.
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void checkMetricsAndTriggerAlerts() {
        try {
            List<SystemMetrics.MetricAlert> metricAlerts = systemMetrics.checkAlerts();
            
            for (SystemMetrics.MetricAlert metricAlert : metricAlerts) {
                String alertKey = generateAlertKey(metricAlert);
                
                if (!activeAlerts.containsKey(alertKey)) {
                    Alert alert = new Alert(
                        alertKey,
                        metricAlert.getType().toString(),
                        metricAlert.getComponent(),
                        metricAlert.getMessage(),
                        mapSeverity(metricAlert.getSeverity()),
                        System.currentTimeMillis()
                    );
                    
                    triggerAlert(alert);
                }
            }
            
            // Check trace-based alerts
            checkTraceAlerts();
            
            // Check for resolved alerts
            checkResolvedAlerts();
            
        } catch (Exception e) {
            logger.error("Error checking metrics for alerts", e);
        }
    }
    
    /**
     * Manually trigger an alert.
     * 
     * @param alertType The type of alert
     * @param component The component that triggered the alert
     * @param message The alert message
     * @param severity The alert severity
     */
    public void triggerAlert(String alertType, String component, String message, Alert.Severity severity) {
        String alertKey = alertType + ":" + component;
        
        Alert alert = new Alert(
            alertKey,
            alertType,
            component,
            message,
            severity,
            System.currentTimeMillis()
        );
        
        triggerAlert(alert);
    }
    
    /**
     * Resolve an active alert.
     * 
     * @param alertKey The alert key to resolve
     * @param resolutionMessage Optional resolution message
     */
    public void resolveAlert(String alertKey, String resolutionMessage) {
        Alert alert = activeAlerts.remove(alertKey);
        if (alert != null) {
            alert.resolve(resolutionMessage);
            alertHistory.add(alert);
            
            logger.info("Alert resolved: {} - {}", alertKey, resolutionMessage);
            
            // Notify handlers
            alertHandlers.forEach(handler -> {
                try {
                    handler.handleAlertResolved(alert);
                } catch (Exception e) {
                    logger.error("Error in alert handler for resolved alert", e);
                }
            });
        }
    }
    
    /**
     * Get all active alerts.
     * 
     * @return List of active alerts
     */
    public List<Alert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts.values());
    }
    
    /**
     * Get alert history.
     * 
     * @param limit Maximum number of alerts to return
     * @return List of historical alerts
     */
    public List<Alert> getAlertHistory(int limit) {
        return alertHistory.stream()
                .sorted((a1, a2) -> Long.compare(a2.getTimestamp(), a1.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Get alert statistics.
     * 
     * @return AlertStatistics containing summary information
     */
    public AlertStatistics getAlertStatistics() {
        int activeCount = activeAlerts.size();
        int totalCount = alertHistory.size() + activeCount;
        
        Map<String, Integer> alertsByType = new HashMap<>();
        Map<Alert.Severity, Integer> alertsBySeverity = new HashMap<>();
        
        // Count active alerts
        activeAlerts.values().forEach(alert -> {
            alertsByType.merge(alert.getType(), 1, Integer::sum);
            alertsBySeverity.merge(alert.getSeverity(), 1, Integer::sum);
        });
        
        // Count historical alerts
        alertHistory.forEach(alert -> {
            alertsByType.merge(alert.getType(), 1, Integer::sum);
            alertsBySeverity.merge(alert.getSeverity(), 1, Integer::sum);
        });
        
        return new AlertStatistics(activeCount, totalCount, alertsByType, alertsBySeverity);
    }
    
    /**
     * Update alert configuration.
     * 
     * @param newConfig The new alert configuration
     */
    public void updateConfiguration(AlertConfiguration newConfig) {
        this.config.updateFrom(newConfig);
        logger.info("Alert configuration updated");
    }
    
    /**
     * Add a custom alert handler.
     * 
     * @param handler The alert handler to add
     */
    public void addAlertHandler(AlertHandler handler) {
        alertHandlers.add(handler);
        logger.info("Added alert handler: {}", handler.getClass().getSimpleName());
    }
    
    /**
     * Trigger an alert and notify handlers.
     */
    private void triggerAlert(Alert alert) {
        activeAlerts.put(alert.getKey(), alert);
        
        logger.warn("Alert triggered: {} - {}", alert.getKey(), alert.getMessage());
        
        // Notify handlers
        alertHandlers.forEach(handler -> {
            try {
                handler.handleAlert(alert);
            } catch (Exception e) {
                logger.error("Error in alert handler", e);
            }
        });
    }
    
    /**
     * Check for trace-based alerts.
     */
    private void checkTraceAlerts() {
        TraceManager.TraceStatistics traceStats = traceManager.getTraceStatistics();
        
        // Check for too many active traces
        if (traceStats.getActiveTraceCount() > config.getMaxActiveTraces()) {
            triggerAlert(
                "HIGH_ACTIVE_TRACES",
                "trace_manager",
                "High number of active traces: " + traceStats.getActiveTraceCount(),
                Alert.Severity.WARNING
            );
        }
        
        // Check for stale traces
        if (traceStats.getOldestTraceAgeMs() > config.getMaxTraceAgeMs()) {
            triggerAlert(
                "STALE_TRACES",
                "trace_manager",
                "Stale traces detected, oldest: " + (traceStats.getOldestTraceAgeMs() / 1000) + "s",
                Alert.Severity.WARNING
            );
        }
    }
    
    /**
     * Check for resolved alerts by re-evaluating conditions.
     */
    private void checkResolvedAlerts() {
        List<String> resolvedAlerts = new ArrayList<>();
        
        activeAlerts.forEach((key, alert) -> {
            if (isAlertConditionResolved(alert)) {
                resolvedAlerts.add(key);
            }
        });
        
        resolvedAlerts.forEach(key -> resolveAlert(key, "Condition resolved automatically"));
    }
    
    /**
     * Check if an alert condition has been resolved.
     */
    private boolean isAlertConditionResolved(Alert alert) {
        // This is a simplified check - in practice, you'd re-evaluate the specific condition
        switch (alert.getType()) {
            case "HIGH_MEMORY_USAGE":
                return systemMetrics.getSystemHealthMetrics().getCurrentMemoryUsageMB() < 1024;
            case "HIGH_CPU_USAGE":
                return systemMetrics.getSystemHealthMetrics().getCurrentCpuUsagePercent() < 80.0;
            case "HIGH_ACTIVE_TRACES":
                return traceManager.getTraceStatistics().getActiveTraceCount() <= config.getMaxActiveTraces();
            default:
                return false; // Keep alert active if we can't determine resolution
        }
    }
    
    /**
     * Generate a unique key for an alert.
     */
    private String generateAlertKey(SystemMetrics.MetricAlert metricAlert) {
        return metricAlert.getType().toString() + ":" + metricAlert.getComponent();
    }
    
    /**
     * Map SystemMetrics severity to Alert severity.
     */
    private Alert.Severity mapSeverity(SystemMetrics.MetricAlert.Severity severity) {
        switch (severity) {
            case INFO: return Alert.Severity.INFO;
            case WARNING: return Alert.Severity.WARNING;
            case CRITICAL: return Alert.Severity.CRITICAL;
            default: return Alert.Severity.WARNING;
        }
    }
    
    /**
     * Represents an alert in the system.
     */
    public static class Alert {
        public enum Severity {
            INFO, WARNING, CRITICAL
        }
        
        private final String key;
        private final String type;
        private final String component;
        private final String message;
        private final Severity severity;
        private final long timestamp;
        private long resolvedTimestamp;
        private String resolutionMessage;
        private boolean resolved;
        
        public Alert(String key, String type, String component, String message, 
                    Severity severity, long timestamp) {
            this.key = key;
            this.type = type;
            this.component = component;
            this.message = message;
            this.severity = severity;
            this.timestamp = timestamp;
            this.resolved = false;
        }
        
        public void resolve(String resolutionMessage) {
            this.resolved = true;
            this.resolvedTimestamp = System.currentTimeMillis();
            this.resolutionMessage = resolutionMessage;
        }
        
        // Getters
        public String getKey() { return key; }
        public String getType() { return type; }
        public String getComponent() { return component; }
        public String getMessage() { return message; }
        public Severity getSeverity() { return severity; }
        public long getTimestamp() { return timestamp; }
        public long getResolvedTimestamp() { return resolvedTimestamp; }
        public String getResolutionMessage() { return resolutionMessage; }
        public boolean isResolved() { return resolved; }
        public long getDuration() { 
            return resolved ? resolvedTimestamp - timestamp : System.currentTimeMillis() - timestamp;
        }
    }
    
    /**
     * Alert configuration with thresholds and settings.
     */
    public static class AlertConfiguration {
        private int maxActiveTraces = 100;
        private long maxTraceAgeMs = 300000; // 5 minutes
        private double maxCpuUsagePercent = 80.0;
        private long maxMemoryUsageMB = 1024;
        private double minSuccessRate = 0.95;
        private long maxLatencyMs = 5000;
        
        public void updateFrom(AlertConfiguration other) {
            this.maxActiveTraces = other.maxActiveTraces;
            this.maxTraceAgeMs = other.maxTraceAgeMs;
            this.maxCpuUsagePercent = other.maxCpuUsagePercent;
            this.maxMemoryUsageMB = other.maxMemoryUsageMB;
            this.minSuccessRate = other.minSuccessRate;
            this.maxLatencyMs = other.maxLatencyMs;
        }
        
        // Getters and setters
        public int getMaxActiveTraces() { return maxActiveTraces; }
        public void setMaxActiveTraces(int maxActiveTraces) { this.maxActiveTraces = maxActiveTraces; }
        
        public long getMaxTraceAgeMs() { return maxTraceAgeMs; }
        public void setMaxTraceAgeMs(long maxTraceAgeMs) { this.maxTraceAgeMs = maxTraceAgeMs; }
        
        public double getMaxCpuUsagePercent() { return maxCpuUsagePercent; }
        public void setMaxCpuUsagePercent(double maxCpuUsagePercent) { this.maxCpuUsagePercent = maxCpuUsagePercent; }
        
        public long getMaxMemoryUsageMB() { return maxMemoryUsageMB; }
        public void setMaxMemoryUsageMB(long maxMemoryUsageMB) { this.maxMemoryUsageMB = maxMemoryUsageMB; }
        
        public double getMinSuccessRate() { return minSuccessRate; }
        public void setMinSuccessRate(double minSuccessRate) { this.minSuccessRate = minSuccessRate; }
        
        public long getMaxLatencyMs() { return maxLatencyMs; }
        public void setMaxLatencyMs(long maxLatencyMs) { this.maxLatencyMs = maxLatencyMs; }
    }
    
    /**
     * Alert statistics for monitoring.
     */
    public static class AlertStatistics {
        private final int activeAlertCount;
        private final int totalAlertCount;
        private final Map<String, Integer> alertsByType;
        private final Map<Alert.Severity, Integer> alertsBySeverity;
        
        public AlertStatistics(int activeAlertCount, int totalAlertCount,
                             Map<String, Integer> alertsByType,
                             Map<Alert.Severity, Integer> alertsBySeverity) {
            this.activeAlertCount = activeAlertCount;
            this.totalAlertCount = totalAlertCount;
            this.alertsByType = alertsByType;
            this.alertsBySeverity = alertsBySeverity;
        }
        
        // Getters
        public int getActiveAlertCount() { return activeAlertCount; }
        public int getTotalAlertCount() { return totalAlertCount; }
        public Map<String, Integer> getAlertsByType() { return alertsByType; }
        public Map<Alert.Severity, Integer> getAlertsBySeverity() { return alertsBySeverity; }
    }
    
    /**
     * Interface for handling alerts.
     */
    public interface AlertHandler {
        void handleAlert(Alert alert);
        void handleAlertResolved(Alert alert);
    }
    
    /**
     * Default logging alert handler.
     */
    public static class LoggingAlertHandler implements AlertHandler {
        private static final Logger logger = LoggerFactory.getLogger(LoggingAlertHandler.class);
        
        @Override
        public void handleAlert(Alert alert) {
            switch (alert.getSeverity()) {
                case CRITICAL:
                    logger.error("🚨 CRITICAL ALERT: {} - {} ({})", 
                               alert.getType(), alert.getMessage(), alert.getComponent());
                    break;
                case WARNING:
                    logger.warn("⚠️ WARNING ALERT: {} - {} ({})", 
                              alert.getType(), alert.getMessage(), alert.getComponent());
                    break;
                case INFO:
                    logger.info("ℹ️ INFO ALERT: {} - {} ({})", 
                              alert.getType(), alert.getMessage(), alert.getComponent());
                    break;
            }
        }
        
        @Override
        public void handleAlertResolved(Alert alert) {
            logger.info("✅ ALERT RESOLVED: {} - {} (duration: {}ms)", 
                       alert.getType(), alert.getResolutionMessage(), alert.getDuration());
        }
    }
}