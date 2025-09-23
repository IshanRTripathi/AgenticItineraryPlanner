package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripplanner.data.entity.Itinerary;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DTO for price information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PriceDto(
        @DecimalMin(value = "0.0", message = "Price amount cannot be negative")
        double amount,
        
        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a valid 3-letter ISO code")
        String currency,
        
        @Size(max = 50, message = "Price unit must not exceed 50 characters")
        String per
) {
    
    public PriceDto {
        // Round amount to 2 decimal places
        amount = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
    
    /**
     * Create DTO from entity.
     */
    public static PriceDto fromEntity(Itinerary.Price entity) {
        if (entity == null) {
            return null;
        }
        
        return new PriceDto(
                entity.getAmount(),
                entity.getCurrency(),
                entity.getPer()
        );
    }
    
    /**
     * Convert to entity.
     */
    public Itinerary.Price toEntity() {
        Itinerary.Price entity = new Itinerary.Price();
        entity.setAmount(amount);
        entity.setCurrency(currency);
        entity.setPer(per);
        return entity;
    }
    
    /**
     * Create a simple price with amount and currency.
     */
    public static PriceDto of(double amount, String currency) {
        return new PriceDto(amount, currency, null);
    }
    
    /**
     * Create a price per person.
     */
    public static PriceDto perPerson(double amount, String currency) {
        return new PriceDto(amount, currency, "person");
    }
    
    /**
     * Create a price per group.
     */
    public static PriceDto perGroup(double amount, String currency) {
        return new PriceDto(amount, currency, "group");
    }
    
    /**
     * Create a price per night.
     */
    public static PriceDto perNight(double amount, String currency) {
        return new PriceDto(amount, currency, "night");
    }
    
    /**
     * Get formatted price string.
     */
    public String getFormattedPrice() {
        String formatted = String.format("%.2f %s", amount, currency);
        if (per != null && !per.isEmpty()) {
            formatted += " per " + per;
        }
        return formatted;
    }
    
    /**
     * Get price symbol based on currency.
     */
    public String getCurrencySymbol() {
        return switch (currency.toUpperCase()) {
            case "USD" -> "$";
            case "EUR" -> "€";
            case "GBP" -> "£";
            case "JPY" -> "¥";
            case "INR" -> "₹";
            case "CNY" -> "¥";
            case "KRW" -> "₩";
            default -> currency;
        };
    }
    
    /**
     * Convert to different currency (requires exchange rate).
     */
    public PriceDto convertTo(String targetCurrency, double exchangeRate) {
        if (targetCurrency.equals(this.currency)) {
            return this;
        }
        
        double convertedAmount = amount * exchangeRate;
        return new PriceDto(convertedAmount, targetCurrency, per);
    }
    
    /**
     * Calculate total price for multiple units.
     */
    public PriceDto multiply(int units) {
        return new PriceDto(amount * units, currency, per);
    }
    
    /**
     * Add two prices (must have same currency).
     */
    public PriceDto add(PriceDto other) {
        if (other == null) {
            return this;
        }
        
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add prices with different currencies");
        }
        
        return new PriceDto(amount + other.amount, currency, per);
    }
    
    /**
     * Check if price is free.
     */
    public boolean isFree() {
        return amount == 0.0;
    }
    
    /**
     * Check if price is valid (positive amount, valid currency).
     */
    public boolean isValid() {
        return amount >= 0 && currency != null && currency.matches("[A-Z]{3}");
    }
}

