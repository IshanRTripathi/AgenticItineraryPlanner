package com.tripplanner.dto.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Result for suggest-best-time tool
 * Contains recommended and avoid time slots with weather context
 */
public class SuggestBestTimeResult {
    
    /**
     * Time slot with suitability score and reasoning
     */
    public static class TimeSlot {
        private String startTime;  // HH:mm format
        private String endTime;    // HH:mm format
        private int suitabilityScore;  // 0-100
        private String reason;
        
        public TimeSlot() {}
        
        public TimeSlot(int hour, int score, String reason) {
            this.startTime = String.format("%02d:00", hour);
            this.endTime = String.format("%02d:00", hour + 1);
            this.suitabilityScore = score;
            this.reason = reason;
        }
        
        public TimeSlot(String startTime, String endTime, int score, String reason) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.suitabilityScore = score;
            this.reason = reason;
        }
        
        // Getters and setters
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public int getSuitabilityScore() { return suitabilityScore; }
        public void setSuitabilityScore(int suitabilityScore) { this.suitabilityScore = suitabilityScore; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    private boolean success;
    private List<TimeSlot> recommendedTimeSlots;
    private List<TimeSlot> avoidTimeSlots;
    private WeatherContext weatherContext;
    private List<String> alternativeActivities;
    private ExtremeWeatherWarning extremeWeatherWarning;
    private String error;
    
    // Constructors
    public SuggestBestTimeResult() {
        this.recommendedTimeSlots = new ArrayList<>();
        this.avoidTimeSlots = new ArrayList<>();
        this.alternativeActivities = new ArrayList<>();
    }
    
    public static SuggestBestTimeResult error(String message) {
        SuggestBestTimeResult result = new SuggestBestTimeResult();
        result.setSuccess(false);
        result.setError(message);
        return result;
    }
    
    // Extreme weather warning inner class
    public static class ExtremeWeatherWarning {
        private boolean isExtreme;
        private String severity; // "moderate", "severe", "extreme"
        private List<String> warnings;
        private List<String> gearRequirements;
        
        public ExtremeWeatherWarning() {
            this.warnings = new ArrayList<>();
            this.gearRequirements = new ArrayList<>();
        }
        
        // Getters and setters
        public boolean isExtreme() { return isExtreme; }
        public void setExtreme(boolean extreme) { isExtreme = extreme; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
        public List<String> getGearRequirements() { return gearRequirements; }
        public void setGearRequirements(List<String> gearRequirements) { this.gearRequirements = gearRequirements; }
    }
    
    // Weather context inner class
    public static class WeatherContext {
        private Double temperature;
        private Double temperatureMin;
        private Double temperatureMax;
        private Integer humidity;
        private String condition;
        private String sunrise;
        private String sunset;
        private String seasonalNotes;
        
        public WeatherContext() {}
        
        public WeatherContext(Double temperature, String condition, String sunrise, String sunset) {
            this.temperature = temperature;
            this.condition = condition;
            this.sunrise = sunrise;
            this.sunset = sunset;
        }
        
        // Getters and setters
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        public Double getTemperatureMin() { return temperatureMin; }
        public void setTemperatureMin(Double temperatureMin) { this.temperatureMin = temperatureMin; }
        public Double getTemperatureMax() { return temperatureMax; }
        public void setTemperatureMax(Double temperatureMax) { this.temperatureMax = temperatureMax; }
        public Integer getHumidity() { return humidity; }
        public void setHumidity(Integer humidity) { this.humidity = humidity; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        public String getSunrise() { return sunrise; }
        public void setSunrise(String sunrise) { this.sunrise = sunrise; }
        public String getSunset() { return sunset; }
        public void setSunset(String sunset) { this.sunset = sunset; }
        public String getSeasonalNotes() { return seasonalNotes; }
        public void setSeasonalNotes(String seasonalNotes) { this.seasonalNotes = seasonalNotes; }
    }
    
    // Getters and setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public List<TimeSlot> getRecommendedTimeSlots() {
        return recommendedTimeSlots;
    }
    
    public void setRecommendedTimeSlots(List<TimeSlot> recommendedTimeSlots) {
        this.recommendedTimeSlots = recommendedTimeSlots;
    }
    
    public List<TimeSlot> getAvoidTimeSlots() {
        return avoidTimeSlots;
    }
    
    public void setAvoidTimeSlots(List<TimeSlot> avoidTimeSlots) {
        this.avoidTimeSlots = avoidTimeSlots;
    }
    
    public WeatherContext getWeatherContext() {
        return weatherContext;
    }
    
    public void setWeatherContext(WeatherContext weatherContext) {
        this.weatherContext = weatherContext;
    }
    
    public List<String> getAlternativeActivities() {
        return alternativeActivities;
    }
    
    public void setAlternativeActivities(List<String> alternativeActivities) {
        this.alternativeActivities = alternativeActivities;
    }
    
    public void addAlternativeActivity(String activity) {
        this.alternativeActivities.add(activity);
    }
    
    public ExtremeWeatherWarning getExtremeWeatherWarning() {
        return extremeWeatherWarning;
    }
    
    public void setExtremeWeatherWarning(ExtremeWeatherWarning extremeWeatherWarning) {
        this.extremeWeatherWarning = extremeWeatherWarning;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
