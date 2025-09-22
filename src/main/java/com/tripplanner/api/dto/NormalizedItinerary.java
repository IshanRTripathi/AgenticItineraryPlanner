package com.tripplanner.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Map;

/**
 * Normalized itinerary JSON structure as per MVP contract.
 * This is the single source of truth for itinerary data.
 */
public class NormalizedItinerary {
    
    @NotBlank
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @Positive
    @JsonProperty("version")
    private Integer version;
    
    @JsonProperty("summary")
    private String summary;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("themes")
    private List<String> themes;
    
    @Valid
    @NotNull
    @JsonProperty("days")
    private List<NormalizedDay> days;
    
    @Valid
    @JsonProperty("settings")
    private ItinerarySettings settings;
    
    @Valid
    @JsonProperty("agents")
    private Map<String, AgentStatus> agents;
    
    public NormalizedItinerary() {}
    
    public NormalizedItinerary(String itineraryId, Integer version) {
        this.itineraryId = itineraryId;
        this.version = version;
    }
    
    // Getters and Setters
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public List<String> getThemes() {
        return themes;
    }
    
    public void setThemes(List<String> themes) {
        this.themes = themes;
    }
    
    public List<NormalizedDay> getDays() {
        return days;
    }
    
    public void setDays(List<NormalizedDay> days) {
        this.days = days;
    }
    
    public ItinerarySettings getSettings() {
        return settings;
    }
    
    public void setSettings(ItinerarySettings settings) {
        this.settings = settings;
    }
    
    public Map<String, AgentStatus> getAgents() {
        return agents;
    }
    
    public void setAgents(Map<String, AgentStatus> agents) {
        this.agents = agents;
    }
    
    @Override
    public String toString() {
        return "NormalizedItinerary{" +
                "itineraryId='" + itineraryId + '\'' +
                ", version=" + version +
                ", summary='" + summary + '\'' +
                ", currency='" + currency + '\'' +
                ", themes=" + themes +
                ", days=" + days +
                ", settings=" + settings +
                ", agents=" + agents +
                '}';
    }
}
