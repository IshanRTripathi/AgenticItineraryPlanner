package com.tripplanner.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled service to export analytics metrics to Cloud Monitoring.
 * Runs every hour to update metrics for alerting.
 */
@Service
public class AnalyticsMetricsExporter {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsMetricsExporter.class);
    
    private final CloudMonitoringService monitoringService;
    private final Firestore firestore;
    
    public AnalyticsMetricsExporter(CloudMonitoringService monitoringService, Firestore firestore) {
        this.monitoringService = monitoringService;
        this.firestore = firestore;
    }
    
    /**
     * Export LLM cost metrics every hour.
     * Calculates today's cost and monthly projection.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void exportLlmCostMetrics() {
        try {
            logger.info("Exporting LLM cost metrics to Cloud Monitoring...");
            
            // Calculate today's cost from raw events
            double todayCost = calculateTodayLlmCost();
            monitoringService.exportLlmDailyCost(todayCost);
            
            // Calculate monthly projection
            double monthlyProjection = calculateMonthlyProjection();
            monitoringService.exportMonthlyProjection(monthlyProjection);
            
            logger.info("LLM cost metrics exported - Today: ${}, Projected: ${}", 
                       String.format("%.2f", todayCost),
                       String.format("%.2f", monthlyProjection));
            
        } catch (Exception e) {
            logger.error("Failed to export LLM cost metrics", e);
        }
    }
    
    /**
     * Export agent performance metrics every 15 minutes.
     */
    @Scheduled(cron = "0 */15 * * * *") // Every 15 minutes
    public void exportAgentMetrics() {
        try {
            logger.info("Exporting agent performance metrics...");
            
            // Calculate failure rates for each agent type
            var agentTypes = List.of(
                "SkeletonPlannerAgent",
                "DayByDayPlannerAgent",
                "ActivityAgent",
                "TransportAgent",
                "MealAgent",
                "PlacesAgent"
            );
            
            for (String agentType : agentTypes) {
                double failureRate = calculateAgentFailureRate(agentType);
                monitoringService.exportAgentFailureRate(agentType, failureRate);
            }
            
            logger.info("Agent performance metrics exported");
            
        } catch (Exception e) {
            logger.error("Failed to export agent metrics", e);
        }
    }
    
    /**
     * Export booking metrics every hour.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void exportBookingMetrics() {
        try {
            logger.info("Exporting booking metrics...");
            
            double bookingFailureRate = calculateBookingFailureRate();
            monitoringService.exportBookingFailureRate(bookingFailureRate);
            
            logger.info("Booking metrics exported - Failure rate: {}%", 
                       String.format("%.2f", bookingFailureRate * 100));
            
        } catch (Exception e) {
            logger.error("Failed to export booking metrics", e);
        }
    }
    
    /**
     * Calculate today's LLM cost from raw events.
     */
    private double calculateTodayLlmCost() {
        try {
            String today = LocalDate.now().toString();
            
            var query = firestore.collection("analytics")
                .document("events")
                .collection("all")
                .whereEqualTo("eventName", "llm_token_usage")
                .whereGreaterThanOrEqualTo("timestamp", getTodayStartTimestamp())
                .get()
                .get();
            
            double totalCost = 0.0;
            for (QueryDocumentSnapshot doc : query.getDocuments()) {
                var properties = doc.get("properties");
                if (properties instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    var props = (java.util.Map<String, Object>) properties;
                    Object costObj = props.get("llmCostUsd");
                    if (costObj instanceof Number) {
                        totalCost += ((Number) costObj).doubleValue();
                    }
                }
            }
            
            return totalCost;
            
        } catch (Exception e) {
            logger.error("Failed to calculate today's LLM cost", e);
            return 0.0;
        }
    }
    
    /**
     * Calculate monthly cost projection.
     */
    private double calculateMonthlyProjection() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate monthStart = today.withDayOfMonth(1);
            int daysElapsed = today.getDayOfMonth();
            int daysInMonth = today.lengthOfMonth();
            
            // Get month-to-date cost
            var query = firestore.collection("analytics")
                .document("events")
                .collection("all")
                .whereEqualTo("eventName", "llm_token_usage")
                .whereGreaterThanOrEqualTo("timestamp", getMonthStartTimestamp())
                .get()
                .get();
            
            double monthToDateCost = 0.0;
            for (QueryDocumentSnapshot doc : query.getDocuments()) {
                var properties = doc.get("properties");
                if (properties instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    var props = (java.util.Map<String, Object>) properties;
                    Object costObj = props.get("llmCostUsd");
                    if (costObj instanceof Number) {
                        monthToDateCost += ((Number) costObj).doubleValue();
                    }
                }
            }
            
            // Project for full month
            double avgDailyCost = monthToDateCost / daysElapsed;
            return avgDailyCost * daysInMonth;
            
        } catch (Exception e) {
            logger.error("Failed to calculate monthly projection", e);
            return 0.0;
        }
    }
    
    /**
     * Calculate agent failure rate for last hour.
     */
    private double calculateAgentFailureRate(String agentType) {
        try {
            long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
            
            // Count started
            var startedQuery = firestore.collection("analytics")
                .document("events")
                .collection("all")
                .whereEqualTo("eventName", "agent_started")
                .whereGreaterThanOrEqualTo("timestamp", oneHourAgo)
                .get()
                .get();
            
            long started = startedQuery.getDocuments().stream()
                .filter(doc -> {
                    var props = doc.get("properties");
                    if (props instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        var p = (java.util.Map<String, Object>) props;
                        return agentType.equals(p.get("agentType"));
                    }
                    return false;
                })
                .count();
            
            // Count failed
            var failedQuery = firestore.collection("analytics")
                .document("events")
                .collection("all")
                .whereEqualTo("eventName", "agent_failed")
                .whereGreaterThanOrEqualTo("timestamp", oneHourAgo)
                .get()
                .get();
            
            long failed = failedQuery.getDocuments().stream()
                .filter(doc -> {
                    var props = doc.get("properties");
                    if (props instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        var p = (java.util.Map<String, Object>) props;
                        return agentType.equals(p.get("agentType"));
                    }
                    return false;
                })
                .count();
            
            return started > 0 ? (double) failed / started : 0.0;
            
        } catch (Exception e) {
            logger.error("Failed to calculate agent failure rate for {}", agentType, e);
            return 0.0;
        }
    }
    
    /**
     * Calculate booking failure rate for last hour.
     */
    private double calculateBookingFailureRate() {
        try {
            long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
            
            // Count initiated
            var initiatedQuery = firestore.collection("analytics")
                .document("events")
                .collection("all")
                .whereEqualTo("eventName", "booking_initiated")
                .whereGreaterThanOrEqualTo("timestamp", oneHourAgo)
                .get()
                .get();
            
            long initiated = initiatedQuery.size();
            
            // Count failed
            var failedQuery = firestore.collection("analytics")
                .document("events")
                .collection("all")
                .whereEqualTo("eventName", "booking_failed")
                .whereGreaterThanOrEqualTo("timestamp", oneHourAgo)
                .get()
                .get();
            
            long failed = failedQuery.size();
            
            return initiated > 0 ? (double) failed / initiated : 0.0;
            
        } catch (Exception e) {
            logger.error("Failed to calculate booking failure rate", e);
            return 0.0;
        }
    }
    
    private long getTodayStartTimestamp() {
        return LocalDate.now().atStartOfDay()
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
    }
    
    private long getMonthStartTimestamp() {
        return LocalDate.now().withDayOfMonth(1).atStartOfDay()
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
    }
}
