package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Node cost information.
 */
public class NodeCost {
    
    @JsonProperty("amountPerPerson")
    private Double amountPerPerson;
    
    @JsonProperty("currency")
    private String currency;
    
    public NodeCost() {}
    
    public NodeCost(Double amountPerPerson, String currency) {
        this.amountPerPerson = amountPerPerson;
        this.currency = currency;
    }
    
    // Getters and Setters
    public Double getAmountPerPerson() {
        return amountPerPerson;
    }
    
    public void setAmountPerPerson(Double amountPerPerson) {
        this.amountPerPerson = amountPerPerson;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    @Override
    public String toString() {
        return "NodeCost{" +
                "amountPerPerson=" + amountPerPerson +
                ", currency='" + currency + '\'' +
                '}';
    }
}
