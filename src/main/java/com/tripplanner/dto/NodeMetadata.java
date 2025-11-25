package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base metadata class for type-specific node data
 * Replaces string parsing with explicit structured data
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "metadataType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TransportMetadata.class, name = "transport"),
    @JsonSubTypes.Type(value = ActivityMetadata.class, name = "activity"),
    @JsonSubTypes.Type(value = MealMetadata.class, name = "meal")
})
public abstract class NodeMetadata {
    
    @JsonProperty("metadataType")
    private String metadataType;

    protected NodeMetadata(String metadataType) {
        this.metadataType = metadataType;
    }

    public String getMetadataType() {
        return metadataType;
    }

    public void setMetadataType(String metadataType) {
        this.metadataType = metadataType;
    }
}
