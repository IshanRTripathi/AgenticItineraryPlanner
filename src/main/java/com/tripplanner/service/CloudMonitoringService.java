package com.tripplanner.service;

import com.google.api.Metric;
import com.google.api.MonitoredResource;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.*;
import com.google.protobuf.util.Timestamps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for exporting custom metrics to Google Cloud Monitoring.
 * Used for alerting on analytics metrics (LLM costs, failure rates, etc.)
 * 
 * Note: This service is optional and will gracefully degrade if Cloud Monitoring
 * client fails to initialize (e.g., in certain Cloud Run environments).
 */
@Service
public class CloudMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(CloudMonitoringService.class);
    
    @Value("${spring.cloud.gcp.project-id:}")
    private String projectId;
    
    @Value("${analytics.monitoring.enabled:false}")
    private boolean monitoringEnabled;
    
    private volatile boolean clientAvailable = false;
    
    public CloudMonitoringService() {
        // Test if Cloud Monitoring client can be initialized
        try {
            MetricServiceClient.create().close();
            clientAvailable = true;
            logger.info("Cloud Monitoring client initialized successfully");
        } catch (Exception e) {
            clientAvailable = false;
            logger.warn("Cloud Monitoring client not available: {}. Metrics export will be disabled.", e.getMessage());
        }
    }
    
    /**
     * Export LLM daily cost metric to Cloud Monitoring.
     * This enables alerting when daily costs exceed thresholds.
     */
    @Async
    public void exportLlmDailyCost(double costUsd) {
        if (!monitoringEnabled || !clientAvailable) {
            return;
        }
        
        try {
            writeCustomMetric(
                "analytics/llm_daily_cost",
                costUsd,
                "Daily LLM API cost in USD",
                Map.of()
            );
        } catch (Exception e) {
            logger.error("Failed to export LLM daily cost metric", e);
        }
    }
    
    /**
     * Export monthly cost projection metric.
     */
    @Async
    public void exportMonthlyProjection(double projectedCostUsd) {
        if (!monitoringEnabled || !clientAvailable) {
            return;
        }
        
        try {
            writeCustomMetric(
                "analytics/llm_monthly_projection",
                projectedCostUsd,
                "Projected monthly LLM cost in USD",
                Map.of()
            );
        } catch (Exception e) {
            logger.error("Failed to export monthly projection metric", e);
        }
    }
    
    /**
     * Export agent failure rate metric.
     */
    @Async
    public void exportAgentFailureRate(String agentType, double failureRate) {
        if (!monitoringEnabled || !clientAvailable) {
            return;
        }
        
        try {
            writeCustomMetric(
                "analytics/agent_failure_rate",
                failureRate,
                "Agent failure rate (0-1)",
                Map.of("agent_type", agentType)
            );
        } catch (Exception e) {
            logger.error("Failed to export agent failure rate metric", e);
        }
    }
    
    /**
     * Export booking failure rate metric.
     */
    @Async
    public void exportBookingFailureRate(double failureRate) {
        if (!monitoringEnabled || !clientAvailable) {
            return;
        }
        
        try {
            writeCustomMetric(
                "analytics/booking_failure_rate",
                failureRate * 100, // Convert to percentage
                "Booking failure rate percentage",
                Map.of()
            );
        } catch (Exception e) {
            logger.error("Failed to export booking failure rate metric", e);
        }
    }
    
    /**
     * Check if Cloud Monitoring is available.
     */
    public boolean isAvailable() {
        return monitoringEnabled && clientAvailable;
    }
    
    /**
     * Write a custom metric to Cloud Monitoring.
     */
    private void writeCustomMetric(String metricType, double value, String description, Map<String, String> labels) {
        if (!clientAvailable) {
            logger.debug("Skipping metric export (client not available): {}", metricType);
            return;
        }
        
        try (MetricServiceClient metricServiceClient = MetricServiceClient.create()) {
            
            String projectName = ProjectName.of(projectId).toString();
            
            // Create time series
            TimeInterval interval = TimeInterval.newBuilder()
                .setEndTime(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                .build();
            
            TypedValue typedValue = TypedValue.newBuilder()
                .setDoubleValue(value)
                .build();
            
            Point point = Point.newBuilder()
                .setInterval(interval)
                .setValue(typedValue)
                .build();
            
            // Build metric
            Metric.Builder metricBuilder = Metric.newBuilder()
                .setType("custom.googleapis.com/" + metricType);
            
            // Add labels
            labels.forEach(metricBuilder::putLabels);
            
            // Build monitored resource (generic_task)
            MonitoredResource resource = MonitoredResource.newBuilder()
                .setType("generic_task")
                .putLabels("project_id", projectId)
                .putLabels("location", "global")
                .putLabels("namespace", "analytics")
                .putLabels("job", "analytics-service")
                .putLabels("task_id", "default")
                .build();
            
            TimeSeries timeSeries = TimeSeries.newBuilder()
                .setMetric(metricBuilder.build())
                .setResource(resource)
                .addPoints(point)
                .build();
            
            // Write time series
            CreateTimeSeriesRequest request = CreateTimeSeriesRequest.newBuilder()
                .setName(projectName)
                .addTimeSeries(timeSeries)
                .build();
            
            metricServiceClient.createTimeSeries(request);
            
            logger.debug("Exported metric: {} = {}", metricType, value);
            
        } catch (ApiException e) {
            logger.error("API error writing metric {}: {}", metricType, e.getMessage());
        } catch (IOException e) {
            logger.error("IO error writing metric {}: {}", metricType, e.getMessage());
        }
    }
}
