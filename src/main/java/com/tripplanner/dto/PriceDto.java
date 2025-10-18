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
        @DecimalMin(value = "0.0", message = "Price amountPerPerson cannot be negative")
        double amountPerPerson,
        
        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a valid 3-letter ISO code")
        String currency
) {
    
    public PriceDto {
        // Round amountPerPerson to 2 decimal places
        amountPerPerson = BigDecimal.valueOf(amountPerPerson).setScale(2, RoundingMode.HALF_UP).doubleValue();
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
                entity.getCurrency()
        );
    }
    
    /**
     * Convert to entity.
     */
    public Itinerary.Price toEntity() {
        Itinerary.Price entity = new Itinerary.Price();
        entity.setAmount(amountPerPerson);
        entity.setCurrency(currency);
        return entity;
    }
    
    /**
     * Create a simple price with amountPerPerson and currency.
     */
    public static PriceDto of(double amount, String currency) {
        return new PriceDto(amount, currency);
    }
    
    /**
     * Create a price per person.
     */
    public static PriceDto perPerson(double amount, String currency) {
        return new PriceDto(amount, currency);
    }

    /**
     * Check if price is valid (positive amountPerPerson, valid currency).
     */
    public boolean isValid() {
        return amountPerPerson >= 0 && currency != null && currency.matches("[A-Z]{3}");
    }
}

