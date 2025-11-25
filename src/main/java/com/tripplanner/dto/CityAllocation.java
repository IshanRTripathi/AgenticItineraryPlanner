package com.tripplanner.dto;

import java.util.List;

public class CityAllocation {
    private String cityName; // e.g., "Guwahati"
    private String region; // e.g., "Assam" (for context)
    private int startDay; // 1-indexed
    private int endDay; // 1-indexed (inclusive)
    private int totalDays; // endDay - startDay + 1
    private String priority; // "must-visit", "recommended", "optional"
    private String reason; // Why this city? (for user transparency)
    private List<String> highlights; // Key attractions (3-5 items)
    private String destinationType; // "gateway", "main-attraction", "cultural", "nature"

    public CityAllocation() {
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public int getStartDay() {
        return startDay;
    }

    public void setStartDay(int startDay) {
        this.startDay = startDay;
    }

    public int getEndDay() {
        return endDay;
    }

    public void setEndDay(int endDay) {
        this.endDay = endDay;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<String> getHighlights() {
        return highlights;
    }

    public void setHighlights(List<String> highlights) {
        this.highlights = highlights;
    }

    public String getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(String destinationType) {
        this.destinationType = destinationType;
    }
}
