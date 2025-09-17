package com.tripplanner.data.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Cost estimate entity for travel itineraries.
 * Stored in Firestore collection: costEstimates/{id}
 */
public class CostEstimate {
    
    @DocumentId
    private String id;
    
    @NotBlank
    @PropertyName("itineraryId")
    private String itineraryId;
    
    @NotBlank
    @PropertyName("userId")
    private String userId;
    
    @PropertyName("destination")
    private String destination;
    
    @PropertyName("budgetTier")
    private String budgetTier;
    
    @PropertyName("duration")
    private int duration; // days
    
    @PropertyName("partySize")
    private int partySize;
    
    @PropertyName("currency")
    private String currency;
    
    @PropertyName("totals")
    private CostBreakdown totals;
    
    @PropertyName("perDay")
    private CostBreakdown perDay;
    
    @PropertyName("perPerson")
    private CostBreakdown perPerson;
    
    @PropertyName("confidence")
    private String confidence = "medium"; // low, medium, high
    
    @PropertyName("notes")
    private String notes;
    
    @PropertyName("assumptions")
    private Map<String, String> assumptions;
    
    @PropertyName("createdAt")
    private Instant createdAt;
    
    @PropertyName("updatedAt")
    private Instant updatedAt;
    
    @PropertyName("expiresAt")
    private Instant expiresAt; // TTL for cleanup
    
    public CostEstimate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        // Set expiry to 7 days from creation
        this.expiresAt = Instant.now().plusSeconds(7 * 24 * 60 * 60);
    }
    
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
    
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
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public String getBudgetTier() {
        return budgetTier;
    }
    
    public void setBudgetTier(String budgetTier) {
        this.budgetTier = budgetTier;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public int getPartySize() {
        return partySize;
    }
    
    public void setPartySize(int partySize) {
        this.partySize = partySize;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public CostBreakdown getTotals() {
        return totals;
    }
    
    public void setTotals(CostBreakdown totals) {
        this.totals = totals;
    }
    
    public CostBreakdown getPerDay() {
        return perDay;
    }
    
    public void setPerDay(CostBreakdown perDay) {
        this.perDay = perDay;
    }
    
    public CostBreakdown getPerPerson() {
        return perPerson;
    }
    
    public void setPerPerson(CostBreakdown perPerson) {
        this.perPerson = perPerson;
    }
    
    public String getConfidence() {
        return confidence;
    }
    
    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public Map<String, String> getAssumptions() {
        return assumptions;
    }
    
    public void setAssumptions(Map<String, String> assumptions) {
        this.assumptions = assumptions;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CostEstimate that = (CostEstimate) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "CostEstimate{" +
                "id='" + id + '\'' +
                ", itineraryId='" + itineraryId + '\'' +
                ", destination='" + destination + '\'' +
                ", budgetTier='" + budgetTier + '\'' +
                ", currency='" + currency + '\'' +
                '}';
    }
    
    // Nested class
    public static class CostBreakdown {
        @PropertyName("transport")
        private double transport = 0.0;
        
        @PropertyName("lodging")
        private double lodging = 0.0;
        
        @PropertyName("food")
        private double food = 0.0;
        
        @PropertyName("activities")
        private double activities = 0.0;
        
        @PropertyName("shopping")
        private double shopping = 0.0;
        
        @PropertyName("misc")
        private double misc = 0.0;
        
        @PropertyName("total")
        private double total = 0.0;
        
        public CostBreakdown() {}
        
        public void calculateTotal() {
            this.total = transport + lodging + food + activities + shopping + misc;
        }
        
        // Getters and Setters
        public double getTransport() { return transport; }
        public void setTransport(double transport) { this.transport = transport; }
        public double getLodging() { return lodging; }
        public void setLodging(double lodging) { this.lodging = lodging; }
        public double getFood() { return food; }
        public void setFood(double food) { this.food = food; }
        public double getActivities() { return activities; }
        public void setActivities(double activities) { this.activities = activities; }
        public double getShopping() { return shopping; }
        public void setShopping(double shopping) { this.shopping = shopping; }
        public double getMisc() { return misc; }
        public void setMisc(double misc) { this.misc = misc; }
        public double getTotal() { return total; }
        public void setTotal(double total) { this.total = total; }
    }
}
