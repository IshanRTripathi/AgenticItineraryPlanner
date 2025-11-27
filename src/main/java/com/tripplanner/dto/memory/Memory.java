package com.tripplanner.dto.memory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tripplanner.enums.MemoryCategory;
import com.tripplanner.enums.MemoryType;

import java.util.List;
import java.util.Map;

/**
 * Memory DTO for storing user preferences and learned patterns
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Memory {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("category")
    private MemoryCategory category;
    
    @JsonProperty("type")
    private MemoryType type;
    
    @JsonProperty("data")
    private Map<String, Object> data;
    
    @JsonProperty("confidence")
    private Double confidence;
    
    @JsonProperty("createdAt")
    private Long createdAt;
    
    @JsonProperty("lastUpdated")
    private Long lastUpdated;
    
    @JsonProperty("lastUsed")
    private Long lastUsed;
    
    @JsonProperty("source")
    private String source; // USER_STATED, LEARNED, TRIP_CREATION
    
    @JsonProperty("isPersonal")
    private Boolean isPersonal;
    
    @JsonProperty("learnedFrom")
    private List<String> learnedFrom;
    
    @JsonProperty("sticky")
    private Boolean sticky;
    
    @JsonProperty("neverExpires")
    private Boolean neverExpires;
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public MemoryCategory getCategory() {
        return category;
    }
    
    public void setCategory(MemoryCategory category) {
        this.category = category;
    }
    
    public MemoryType getType() {
        return type;
    }
    
    public void setType(MemoryType type) {
        this.type = type;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public Long getLastUsed() {
        return lastUsed;
    }
    
    public void setLastUsed(Long lastUsed) {
        this.lastUsed = lastUsed;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public Boolean getIsPersonal() {
        return isPersonal;
    }
    
    public void setIsPersonal(Boolean isPersonal) {
        this.isPersonal = isPersonal;
    }
    
    public List<String> getLearnedFrom() {
        return learnedFrom;
    }
    
    public void setLearnedFrom(List<String> learnedFrom) {
        this.learnedFrom = learnedFrom;
    }
    
    public Boolean getSticky() {
        return sticky;
    }
    
    public void setSticky(Boolean sticky) {
        this.sticky = sticky;
    }
    
    public Boolean getNeverExpires() {
        return neverExpires;
    }
    
    public void setNeverExpires(Boolean neverExpires) {
        this.neverExpires = neverExpires;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Memory memory = new Memory();
        
        public Builder id(String id) {
            memory.id = id;
            return this;
        }
        
        public Builder itineraryId(String itineraryId) {
            memory.itineraryId = itineraryId;
            return this;
        }
        
        public Builder userId(String userId) {
            memory.userId = userId;
            return this;
        }
        
        public Builder category(MemoryCategory category) {
            memory.category = category;
            return this;
        }
        
        public Builder type(MemoryType type) {
            memory.type = type;
            return this;
        }
        
        public Builder data(Map<String, Object> data) {
            memory.data = data;
            return this;
        }
        
        public Builder confidence(Double confidence) {
            memory.confidence = confidence;
            return this;
        }
        
        public Builder source(String source) {
            memory.source = source;
            return this;
        }
        
        public Builder isPersonal(Boolean isPersonal) {
            memory.isPersonal = isPersonal;
            return this;
        }
        
        public Builder sticky(Boolean sticky) {
            memory.sticky = sticky;
            return this;
        }
        
        public Builder neverExpires(Boolean neverExpires) {
            memory.neverExpires = neverExpires;
            return this;
        }
        
        public Builder learnedFrom(List<String> learnedFrom) {
            memory.learnedFrom = learnedFrom;
            return this;
        }
        
        public Memory build() {
            return memory;
        }
    }
}
