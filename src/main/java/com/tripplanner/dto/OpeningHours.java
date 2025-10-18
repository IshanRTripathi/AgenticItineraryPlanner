package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Opening hours DTO for Google Places API.
 */
public class OpeningHours {
    
    @JsonProperty("open_now")
    private Boolean openNow;
    
    @JsonProperty("periods")
    private List<Period> periods;
    
    @JsonProperty("weekday_text")
    private List<String> weekdayText;
    
    public OpeningHours() {}
    
    // Getters and Setters
    public Boolean getOpenNow() {
        return openNow;
    }
    
    public void setOpenNow(Boolean openNow) {
        this.openNow = openNow;
    }
    
    public List<Period> getPeriods() {
        return periods;
    }
    
    public void setPeriods(List<Period> periods) {
        this.periods = periods;
    }
    
    public List<String> getWeekdayText() {
        return weekdayText;
    }
    
    public void setWeekdayText(List<String> weekdayText) {
        this.weekdayText = weekdayText;
    }
    
    /**
     * Check if the place is currently open.
     */
    public boolean isCurrentlyOpen() {
        return openNow != null && openNow;
    }
    
    /**
     * Get formatted opening hours text.
     */
    public String getFormattedHours() {
        if (weekdayText != null && !weekdayText.isEmpty()) {
            return String.join(", ", weekdayText);
        }
        return "Hours not available";
    }
    
    @Override
    public String toString() {
        return "OpeningHours{" +
                "openNow=" + openNow +
                ", periods=" + (periods != null ? periods.size() + " periods" : "null") +
                ", weekdayText=" + weekdayText +
                '}';
    }
    
    /**
     * Period DTO for opening hours.
     */
    public static class Period {
        @JsonProperty("open")
        private TimeOfDay open;
        
        @JsonProperty("close")
        private TimeOfDay close;
        
        public TimeOfDay getOpen() {
            return open;
        }
        
        public void setOpen(TimeOfDay open) {
            this.open = open;
        }
        
        public TimeOfDay getClose() {
            return close;
        }
        
        public void setClose(TimeOfDay close) {
            this.close = close;
        }
        
        @Override
        public String toString() {
            return "Period{" +
                    "open=" + open +
                    ", close=" + close +
                    '}';
        }
    }
    
    /**
     * Time of day DTO.
     */
    public static class TimeOfDay {
        @JsonProperty("day")
        private Integer day; // 0 = Sunday, 1 = Monday, etc.
        
        @JsonProperty("time")
        private String time; // HHMM format
        
        public Integer getDay() {
            return day;
        }
        
        public void setDay(Integer day) {
            this.day = day;
        }
        
        public String getTime() {
            return time;
        }
        
        public void setTime(String time) {
            this.time = time;
        }
        
        /**
         * Get formatted time (HH:MM).
         */
        public String getFormattedTime() {
            if (time == null || time.length() != 4) {
                return time;
            }
            return time.substring(0, 2) + ":" + time.substring(2);
        }
        
        @Override
        public String toString() {
            return "TimeOfDay{" +
                    "day=" + day +
                    ", time='" + time + '\'' +
                    '}';
        }
    }
}