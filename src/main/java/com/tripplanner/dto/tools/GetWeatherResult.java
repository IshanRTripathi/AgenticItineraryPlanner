package com.tripplanner.dto.tools;

import java.util.ArrayList;
import java.util.List;

public class GetWeatherResult {
    private boolean success;
    private String condition;
    private Double temperatureCelsius;
    private Integer precipitationChance;
    private String recommendation;
    private List<String> warnings;
    private String error;
    
    public GetWeatherResult() {
        this.warnings = new ArrayList<>();
    }
    
    public static GetWeatherResult error(String error) {
        GetWeatherResult result = new GetWeatherResult();
        result.success = false;
        result.error = error;
        return result;
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public Double getTemperatureCelsius() {
        return temperatureCelsius;
    }
    
    public void setTemperatureCelsius(Double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }
    
    public Integer getPrecipitationChance() {
        return precipitationChance;
    }
    
    public void setPrecipitationChance(Integer precipitationChance) {
        this.precipitationChance = precipitationChance;
    }
    
    public String getRecommendation() {
        return recommendation;
    }
    
    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
