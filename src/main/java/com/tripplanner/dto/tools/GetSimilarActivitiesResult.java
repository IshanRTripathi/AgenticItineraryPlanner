package com.tripplanner.dto.tools;

import java.util.ArrayList;
import java.util.List;

public class GetSimilarActivitiesResult {
    private boolean success;
    private List<SimilarActivity> activities;
    private List<String> warnings;
    private String error;
    
    public GetSimilarActivitiesResult() {
        this.activities = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }
    
    public static GetSimilarActivitiesResult error(String error) {
        GetSimilarActivitiesResult result = new GetSimilarActivitiesResult();
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
    
    public List<SimilarActivity> getActivities() {
        return activities;
    }
    
    public void setActivities(List<SimilarActivity> activities) {
        this.activities = activities;
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
    
    public static class SimilarActivity {
        private String placeId;
        private String name;
        private String type;
        private Double rating;
        private Double distanceKm;
        private String similarityReason;
        
        public SimilarActivity() {
        }
        
        public String getPlaceId() {
            return placeId;
        }
        
        public void setPlaceId(String placeId) {
            this.placeId = placeId;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public Double getRating() {
            return rating;
        }
        
        public void setRating(Double rating) {
            this.rating = rating;
        }
        
        public Double getDistanceKm() {
            return distanceKm;
        }
        
        public void setDistanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
        }
        
        public String getSimilarityReason() {
            return similarityReason;
        }
        
        public void setSimilarityReason(String similarityReason) {
            this.similarityReason = similarityReason;
        }
    }
}
