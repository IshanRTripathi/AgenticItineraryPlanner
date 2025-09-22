package com.tripplanner.api.dto;

/**
 * Represents a candidate node for disambiguation when the user's request is ambiguous.
 */
public class NodeCandidate {
    
    private String id;
    private String title;
    private Integer day;
    private String type;
    private String location;
    private double confidence; // Similarity score (0.0 to 1.0)
    
    // Constructors
    public NodeCandidate() {}
    
    public NodeCandidate(String id, String title, Integer day, String type, String location, double confidence) {
        this.id = id;
        this.title = title;
        this.day = day;
        this.type = type;
        this.location = location;
        this.confidence = confidence;
    }
    
    // Static factory method
    public static NodeCandidate of(String id, String title, Integer day, String type, String location) {
        return new NodeCandidate(id, title, day, type, location, 0.0);
    }
    
    public static NodeCandidate of(String id, String title, Integer day, String type, String location, double confidence) {
        return new NodeCandidate(id, title, day, type, location, confidence);
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public Integer getDay() {
        return day;
    }
    
    public void setDay(Integer day) {
        this.day = day;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    // Helper methods
    public String getDisplayName() {
        return title + (location != null && !location.isEmpty() ? " (" + location + ")" : "");
    }
    
    public String getDayInfo() {
        return day != null ? "Day " + day : "Unknown day";
    }
    
    public String getTypeIcon() {
        return switch (type != null ? type.toLowerCase() : "") {
            case "attraction" -> "🏛️";
            case "meal" -> "🍽️";
            case "hotel", "accommodation" -> "🏨";
            case "transit", "transport" -> "🚗";
            default -> "📍";
        };
    }
    
    @Override
    public String toString() {
        return "NodeCandidate{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", day=" + day +
                ", type='" + type + '\'' +
                ", location='" + location + '\'' +
                ", confidence=" + confidence +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeCandidate that = (NodeCandidate) o;
        return id != null ? id.equals(that.id) : that.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
