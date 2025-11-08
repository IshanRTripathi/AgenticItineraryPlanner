package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Trip metadata stored under users/{userId}/itineraries/{itineraryId}
 * Contains essential trip information for listing and quick access
 */
public class TripMetadata {
    
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("destination")
    private String destination;
    
    @JsonProperty("startLocation")
    private String startLocation;
    
    @JsonProperty("startDate")
    private String startDate;
    
    @JsonProperty("endDate")
    private String endDate;
    
    @JsonProperty("summary")
    private String summary;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("party")
    private PartyInfo party;
    
    @JsonProperty("budgetTier")
    private String budgetTier;
    
    @JsonProperty("budgetMin")
    private Double budgetMin;
    
    @JsonProperty("budgetMax")
    private Double budgetMax;
    
    @JsonProperty("interests")
    private List<String> interests;
    
    @JsonProperty("constraints")
    private List<String> constraints;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("createdAt")
    private Long createdAt;
    
    @JsonProperty("updatedAt")
    private Long updatedAt;
    
    @JsonProperty("version")
    private Integer version;
    
    @JsonProperty("status")
    private String status; // "planning", "generating", "completed", "failed"
    
    // Default constructor
    public TripMetadata() {}
    
    // Constructor for creating from NormalizedItinerary
    public TripMetadata(NormalizedItinerary itinerary) {
        this.itineraryId = itinerary.getItineraryId();
        this.userId = itinerary.getUserId();
        this.destination = itinerary.getDestination();
        this.startDate = itinerary.getStartDate();
        this.endDate = itinerary.getEndDate();
        this.summary = itinerary.getSummary();
        this.currency = itinerary.getCurrency();
        this.createdAt = itinerary.getCreatedAt();
        this.updatedAt = itinerary.getUpdatedAt();
        this.version = itinerary.getVersion();
        
        // Note: NormalizedItinerary doesn't contain party, budgetTier, interests, constraints, language
        // These will need to be set separately when creating TripMetadata
    }
    
    // Constructor for creating from CreateItineraryReq and NormalizedItinerary
    public TripMetadata(CreateItineraryReq request, NormalizedItinerary itinerary) {
        this.itineraryId = itinerary.getItineraryId();
        this.userId = itinerary.getUserId();
        this.destination = request.getDestination();
        this.startLocation = request.getStartLocation();
        this.startDate = request.getStartDate().toString();
        this.endDate = request.getEndDate().toString();
        this.summary = itinerary.getSummary();
        this.currency = itinerary.getCurrency();
        this.createdAt = itinerary.getCreatedAt();
        this.updatedAt = itinerary.getUpdatedAt();
        this.version = itinerary.getVersion();
        
        // Set status from itinerary (defaults to "generating" if not set)
        this.status = itinerary.getStatus() != null ? itinerary.getStatus() : "generating";
        
        // Extract party info from request
        if (request.getParty() != null) {
            this.party = new PartyInfo();
            this.party.setAdults(request.getParty().getAdults());
            this.party.setChildren(request.getParty().getChildren());
            this.party.setInfants(request.getParty().getInfants());
            this.party.setRooms(request.getParty().getRooms());
        }
        
        // Extract other metadata from request
        this.budgetTier = request.getBudgetTier();
        this.budgetMin = request.getBudgetMin();
        this.budgetMax = request.getBudgetMax();
        this.interests = request.getInterests();
        this.constraints = request.getConstraints();
        this.language = request.getLanguage();
    }
    
    // Getters and Setters
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
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public String getStartLocation() {
        return startLocation;
    }
    
    public void setStartLocation(String startLocation) {
        this.startLocation = startLocation;
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
    
    public PartyInfo getParty() {
        return party;
    }
    
    public void setParty(PartyInfo party) {
        this.party = party;
    }
    
    public String getBudgetTier() {
        return budgetTier;
    }
    
    public void setBudgetTier(String budgetTier) {
        this.budgetTier = budgetTier;
    }
    
    public Double getBudgetMin() {
        return budgetMin;
    }
    
    public void setBudgetMin(Double budgetMin) {
        this.budgetMin = budgetMin;
    }
    
    public Double getBudgetMax() {
        return budgetMax;
    }
    
    public void setBudgetMax(Double budgetMax) {
        this.budgetMax = budgetMax;
    }
    
    public List<String> getInterests() {
        return interests;
    }
    
    public void setInterests(List<String> interests) {
        this.interests = interests;
    }
    
    public List<String> getConstraints() {
        return constraints;
    }
    
    public void setConstraints(List<String> constraints) {
        this.constraints = constraints;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
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
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Inner class for party information
     */
    public static class PartyInfo {
        @JsonProperty("adults")
        private Integer adults;
        
        @JsonProperty("children")
        private Integer children;
        
        @JsonProperty("infants")
        private Integer infants;
        
        @JsonProperty("rooms")
        private Integer rooms;
        
        public PartyInfo() {}
        
        public Integer getAdults() {
            return adults;
        }
        
        public void setAdults(Integer adults) {
            this.adults = adults;
        }
        
        public Integer getChildren() {
            return children;
        }
        
        public void setChildren(Integer children) {
            this.children = children;
        }
        
        public Integer getInfants() {
            return infants;
        }
        
        public void setInfants(Integer infants) {
            this.infants = infants;
        }
        
        public Integer getRooms() {
            return rooms;
        }
        
        public void setRooms(Integer rooms) {
            this.rooms = rooms;
        }
    }
}
