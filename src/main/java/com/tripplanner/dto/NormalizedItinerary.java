package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalized itinerary JSON structure as per MVP contract.
 * This is the single source of truth for itinerary data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NormalizedItinerary {
    
    @NotBlank
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @Positive
    @JsonProperty("version")
    private Integer version;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("createdAt")
    private Long createdAt;
    
    @JsonProperty("updatedAt")
    private Long updatedAt;
    
    @JsonProperty("summary")
    private String summary;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("themes")
    private List<String> themes;

    // Explicit trip meta to avoid parsing from summary
    @JsonProperty("origin")
    private String origin;
    
    @JsonProperty("destination")
    private String destination;

    @JsonProperty("startDate")
    private String startDate; // ISO yyyy-MM-dd

    @JsonProperty("endDate")
    private String endDate; // ISO yyyy-MM-dd
    
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
    
    @Valid
    @JsonProperty("mapBounds")
    private MapBounds mapBounds;
    
    @Valid
    @JsonProperty("countryCentroid")
    private Coordinates countryCentroid;
    
    // Unified structure extensions for agent-friendly architecture
    @Valid
    @JsonProperty("agentData")
    private Map<String, AgentDataSection> agentData;
    
    @Valid
    @JsonProperty("workflow")
    private WorkflowData workflow;
    
    @Valid
    @JsonProperty("revisions")
    private List<RevisionRecord> revisions;
    
    @Valid
    @JsonProperty("chat")
    private List<ChatRecord> chat;
    
    public NormalizedItinerary() {
        // Initialize collections to prevent null pointer exceptions
        this.agentData = new java.util.HashMap<>();
        this.revisions = new java.util.ArrayList<>();
        this.chat = new java.util.ArrayList<>();
    }
    
    public NormalizedItinerary(String itineraryId, Integer version) {
        this();
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
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
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

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
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
    
    public MapBounds getMapBounds() {
        return mapBounds;
    }
    
    public void setMapBounds(MapBounds mapBounds) {
        this.mapBounds = mapBounds;
    }
    
    public Coordinates getCountryCentroid() {
        return countryCentroid;
    }
    
    public void setCountryCentroid(Coordinates countryCentroid) {
        this.countryCentroid = countryCentroid;
    }
    
    public Map<String, AgentDataSection> getAgentData() {
        return agentData;
    }
    
    public void setAgentData(Map<String, AgentDataSection> agentData) {
        this.agentData = agentData;
    }
    
    public WorkflowData getWorkflow() {
        return workflow;
    }
    
    public void setWorkflow(WorkflowData workflow) {
        this.workflow = workflow;
    }
    
    public List<RevisionRecord> getRevisions() {
        return revisions;
    }
    
    public void setRevisions(List<RevisionRecord> revisions) {
        this.revisions = revisions;
    }
    
    public List<ChatRecord> getChat() {
        return chat;
    }
    
    public void setChat(List<ChatRecord> chat) {
        this.chat = chat;
    }

    /**
     * Initialize collections for unified structure
     */
    public void initializeUnifiedStructure() {
        if (this.agentData == null) {
            this.agentData = new HashMap<>();
        }
        if (this.revisions == null) {
            this.revisions = new ArrayList<>();
        }
        if (this.chat == null) {
            this.chat = new ArrayList<>();
        }
        if (this.workflow == null) {
            this.workflow = new WorkflowData();
        }
    }
    
    @Override
    public String toString() {
        return "NormalizedItinerary{" +
                "itineraryId='" + itineraryId + '\'' +
                ", version=" + version +
                ", userId='" + userId + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", summary='" + summary + '\'' +
                ", currency='" + currency + '\'' +
                ", themes=" + themes +
                ", origin='" + origin + '\'' +
                ", destination='" + destination + '\'' +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", days=" + days +
                ", settings=" + settings +
                ", agents=" + agents +
                ", mapBounds=" + mapBounds +
                ", countryCentroid=" + countryCentroid +
                ", agentData=" + agentData +
                ", workflow=" + workflow +
                ", revisions=" + revisions +
                ", chat=" + chat +
                '}';
    }
}
