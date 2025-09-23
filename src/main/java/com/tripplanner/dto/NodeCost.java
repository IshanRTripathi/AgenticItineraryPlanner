package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Node cost information.
 */
public class NodeCost {
    
    @JsonProperty("amount")
    private Double amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("per")
    private String per; // "person", "group", "night", etc.
    
    public NodeCost() {}
    
    public NodeCost(Double amount, String currency, String per) {
        this.amount = amount;
        this.currency = currency;
        this.per = per;
    }
    
    // Getters and Setters
    public Double getAmount() {
        return amount;
    }
    
    public void setAmount(Double amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getPer() {
        return per;
    }
    
    public void setPer(String per) {
        this.per = per;
    }
    
    @Override
    public String toString() {
        return "NodeCost{" +
                "amount=" + amount +
                ", currency='" + currency + '\'' +
                ", per='" + per + '\'' +
                '}';
    }
}
