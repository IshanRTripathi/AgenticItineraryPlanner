package com.tripplanner.api;

import com.tripplanner.service.ToolsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for travel planning tools.
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ToolsController {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolsController.class);
    
    private final ToolsService toolsService;
    
    public ToolsController(ToolsService toolsService) {
        this.toolsService = toolsService;
    }
    
    /**
     * Generate packing list for a trip.
     */
    @PostMapping("/packing-list")
    public ResponseEntity<PackingListRes> generatePackingList(
            @Valid @RequestBody PackingListReq request) {
        
        logger.info("Generating packing list for user: {}, destination: {}", 
                   "anonymous", request.destination());
        
        PackingListRes response = toolsService.generatePackingList(request);
        
        logger.info("Packing list generated with {} items", response.items().size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get photo spots for a destination.
     */
    @PostMapping("/photo-spots")
    public ResponseEntity<PhotoSpotsRes> getPhotoSpots(
            @Valid @RequestBody PhotoSpotsReq request) {
        
        logger.info("Getting photo spots for user: {}, destination: {}", 
                   "anonymous", request.destination());
        
        PhotoSpotsRes response = toolsService.getPhotoSpots(request);
        
        logger.info("Found {} photo spots", response.spots().size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get must-try foods for a destination.
     */
    @PostMapping("/must-try-foods")
    public ResponseEntity<MustTryFoodsRes> getMustTryFoods(
            @Valid @RequestBody MustTryFoodsReq request) {
        
        logger.info("Getting must-try foods for user: {}, destination: {}", 
                   "anonymous", request.destination());
        
        MustTryFoodsRes response = toolsService.getMustTryFoods(request);
        
        logger.info("Found {} must-try foods", response.items().size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Generate cost estimate for a trip.
     */
    @PostMapping("/cost-estimator")
    public ResponseEntity<CostEstimateRes> generateCostEstimate(
            @Valid @RequestBody CostEstimateReq request) {
        
        logger.info("Generating cost estimate for user: {}, destination: {}", 
                   "anonymous", request.destination());
        
        CostEstimateRes response = toolsService.generateCostEstimate(request);
        
        logger.info("Cost estimate generated: {} {}", response.totals().total(), response.currency());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get weather information for a destination.
     */
    @GetMapping("/tools/weather")
    public ResponseEntity<WeatherRes> getWeather(
            @RequestParam String destination,
            @RequestParam(required = false) String date) {
        
        logger.info("Getting weather for destination: {}, date: {}", destination, date);
        
        WeatherRes response = toolsService.getWeather(destination, date);
        
        logger.info("Weather retrieved for: {}", destination);
        return ResponseEntity.ok(response);
    }
    
    // Request/Response DTOs
    
    /**
     * Request DTO for packing list generation.
     */
    public record PackingListReq(
            @NotBlank(message = "Destination is required")
            String destination,
            
            @NotBlank(message = "Climate is required")
            String climate,
            
            @NotBlank(message = "Season is required")
            String season,
            
            @NotNull(message = "Start date is required")
            LocalDate startDate,
            
            @NotNull(message = "End date is required")
            LocalDate endDate,
            
            @Size(max = 20, message = "Maximum 20 activities allowed")
            List<String> activities,
            
            @Positive(message = "Party size must be positive")
            int partySize
    ) {}
    
    /**
     * Response DTO for packing list.
     */
    public record PackingListRes(
            List<PackingItem> items
    ) {}
    
    /**
     * DTO for packing list item.
     */
    public record PackingItem(
            String name,
            int quantity,
            String group,
            boolean essential,
            String notes
    ) {}
    
    /**
     * Request DTO for photo spots.
     */
    public record PhotoSpotsReq(
            @NotBlank(message = "Destination is required")
            String destination,
            
            @Size(max = 10, message = "Maximum 10 interests allowed")
            List<String> interests,
            
            String timeOfDay,
            String season
    ) {}
    
    /**
     * Response DTO for photo spots.
     */
    public record PhotoSpotsRes(
            List<PhotoSpot> spots
    ) {}
    
    /**
     * DTO for photo spot.
     */
    public record PhotoSpot(
            String name,
            double lat,
            double lng,
            String category,
            String bestTime,
            String tips,
            String difficulty
    ) {}
    
    /**
     * Request DTO for must-try foods.
     */
    public record MustTryFoodsReq(
            @NotBlank(message = "Destination is required")
            String destination,
            
            @Size(max = 10, message = "Maximum 10 dietary preferences allowed")
            List<String> dietaryPreferences,
            
            String budgetTier,
            List<String> cuisineTypes
    ) {}
    
    /**
     * Response DTO for must-try foods.
     */
    public record MustTryFoodsRes(
            List<MustTryFood> items
    ) {}
    
    /**
     * DTO for must-try food item.
     */
    public record MustTryFood(
            String name,
            String description,
            String category,
            List<String> venues,
            String priceRange,
            String tips
    ) {}
    
    /**
     * Request DTO for cost estimation.
     */
    public record CostEstimateReq(
            @NotBlank(message = "Destination is required")
            String destination,
            
            @NotNull(message = "Start date is required")
            LocalDate startDate,
            
            @NotNull(message = "End date is required")
            LocalDate endDate,
            
            @NotBlank(message = "Budget tier is required")
            String budgetTier,
            
            @Positive(message = "Party size must be positive")
            int partySize,
            
            @Size(max = 20, message = "Maximum 20 interests allowed")
            List<String> interests
    ) {}
    
    /**
     * Response DTO for cost estimate.
     */
    public record CostEstimateRes(
            String currency,
            CostBreakdown totals,
            CostBreakdown perDay,
            CostBreakdown perPerson,
            String confidence,
            String notes
    ) {}
    
    /**
     * DTO for cost breakdown.
     */
    public record CostBreakdown(
            double transport,
            double lodging,
            double food,
            double activities,
            double shopping,
            double misc,
            double total
    ) {}
    
    /**
     * Response DTO for weather information.
     */
    public record WeatherRes(
            String destination,
            String date,
            double temperature,
            String condition,
            String description,
            double humidity,
            double windSpeed,
            String unit
    ) {}
}

