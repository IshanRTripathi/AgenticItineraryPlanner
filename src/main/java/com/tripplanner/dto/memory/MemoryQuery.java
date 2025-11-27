package com.tripplanner.dto.memory;

import com.tripplanner.enums.MemoryCategory;
import com.tripplanner.enums.MemoryType;

/**
 * Query builder for filtering memories
 */
public class MemoryQuery {
    
    private MemoryCategory category;
    private MemoryType type;
    private String source;
    private Boolean isPersonal;
    private Double minConfidence;
    
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
    
    public Double getMinConfidence() {
        return minConfidence;
    }
    
    public void setMinConfidence(Double minConfidence) {
        this.minConfidence = minConfidence;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private MemoryQuery query = new MemoryQuery();
        
        public Builder category(MemoryCategory category) {
            query.category = category;
            return this;
        }
        
        public Builder type(MemoryType type) {
            query.type = type;
            return this;
        }
        
        public Builder source(String source) {
            query.source = source;
            return this;
        }
        
        public Builder isPersonal(Boolean isPersonal) {
            query.isPersonal = isPersonal;
            return this;
        }
        
        public Builder minConfidence(Double minConfidence) {
            query.minConfidence = minConfidence;
            return this;
        }
        
        public MemoryQuery build() {
            return query;
        }
    }
}
