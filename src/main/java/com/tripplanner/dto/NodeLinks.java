package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Node links for booking and details.
 */
public class NodeLinks {
    
    @JsonProperty("book")
    private String book;
    
    @JsonProperty("details")
    private String details;
    
    public NodeLinks() {}
    
    public NodeLinks(String book, String details) {
        this.book = book;
        this.details = details;
    }
    
    // Getters and Setters
    public String getBook() {
        return book;
    }
    
    public void setBook(String book) {
        this.book = book;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    @Override
    public String toString() {
        return "NodeLinks{" +
                "book='" + book + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}
