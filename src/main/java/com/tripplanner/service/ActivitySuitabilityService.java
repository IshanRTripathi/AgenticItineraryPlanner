package com.tripplanner.service;

import com.tripplanner.dto.tools.SuggestBestTimeResult.TimeSlot;
import com.tripplanner.service.WeatherService.WeatherData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to determine optimal times for activities based on weather,
 * temperature, and activity type.
 * Provides intelligent scheduling recommendations for outdoor, indoor, scenic,
 * and water activities.
 */
@Service
public class ActivitySuitabilityService {

    private static final Logger logger = LoggerFactory.getLogger(ActivitySuitabilityService.class);

    private final WeatherService weatherService;

    public ActivitySuitabilityService(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * Suggest best times for an activity based on weather and activity type
     */
    public List<TimeSlot> suggestBestTimes(
            String activityName,
            ActivityType activityType,
            String location,
            LocalDate date,
            int durationMinutes,
            String itineraryId) {

        logger.info("Suggesting best times for: {} (type: {}) at {} on {}",
                activityName, activityType, location, date);

        // Get weather forecast (with caching)
        WeatherData weather = weatherService.getWeather(itineraryId, location, date.toString());

        logger.info("Weather: {}°C, {}", weather.getTemperatureCelsius(), weather.getCondition());

        // Calculate suitability for each hour
        List<TimeSlot> slots = new ArrayList<>();

        for (int hour = 6; hour < 22; hour++) {
            // Estimate temperature for this specific hour
            double hourlyTemp = estimateHourlyTemperature(weather, hour);

            // Calculate heat index if humidity available
            double feelsLikeTemp = hourlyTemp;
            if (weather.getHumidity() != null && weather.getHumidity() > 0) {
                feelsLikeTemp = calculateHeatIndex(hourlyTemp, weather.getHumidity());
            }

            int score = calculateSuitabilityScore(activityType, weather, hour, hourlyTemp, feelsLikeTemp);
            String reason = generateReason(activityType, weather, hour, score, hourlyTemp, feelsLikeTemp);

            if (score > 30) { // Only include reasonably suitable times
                slots.add(new TimeSlot(hour, score, reason));
            }
        }

        // Sort by score descending
        slots.sort((a, b) -> Integer.compare(b.getSuitabilityScore(), a.getSuitabilityScore()));

        logger.info("Found {} suitable time slots for {}", slots.size(), activityName);

        return slots;
    }

    /**
     * Activity types with different weather/time sensitivities
     */
    public enum ActivityType {
        OUTDOOR_PARK, // Parks, gardens - temperature sensitive
        OUTDOOR_MONUMENT, // Historical sites - temperature sensitive
        OUTDOOR_TREK, // Trekking, hiking - extreme weather capable
        OUTDOOR_ADVENTURE, // Adventure sports - weather dependent
        SCENIC_SPOT, // Photography spots - lighting critical
        BEACH_WATER, // Water activities - hot weather preferred
        INDOOR_MUSEUM, // Museums - weather independent
        INDOOR_MALL, // Shopping - AC refuge during heat
        INDOOR_TEMPLE, // Religious sites - weather independent
        NIGHT_ACTIVITY, // Night markets, shows - evening only
        FLEXIBLE; // Can be done anytime

        public static ActivityType fromString(String type) {
            if (type == null)
                return FLEXIBLE;
            String normalized = type.toUpperCase().replace("-", "_");
            try {
                return ActivityType.valueOf(normalized);
            } catch (IllegalArgumentException e) {
                return FLEXIBLE;
            }
        }
    }

    /**
     * Calculate heat index (feels like temperature) based on temperature and
     * humidity
     * Uses simplified and moderated heat index formula for realistic results
     */
    private double calculateHeatIndex(double tempCelsius, int humidity) {
        // Heat index only significant in hot weather
        if (tempCelsius < 27 || humidity < 40) {
            return tempCelsius; // Heat index not significant
        }

        // Convert to Fahrenheit for calculation
        double tempF = (tempCelsius * 9 / 5) + 32;

        // Simplified heat index formula (more moderate than full Steadman)
        // Based on: HI = c1 + c2*T + c3*RH + c4*T*RH + c5*T^2 + c6*RH^2
        double hi = -42.379 + 2.04901523 * tempF + 10.14333127 * humidity
                - 0.22475541 * tempF * humidity - 0.00683783 * tempF * tempF
                - 0.05481717 * humidity * humidity + 0.00122874 * tempF * tempF * humidity
                + 0.00085282 * tempF * humidity * humidity - 0.00000199 * tempF * tempF * humidity * humidity;

        // Convert back to Celsius
        double hiCelsius = (hi - 32) * 5 / 9;

        // Cap the heat index increase to be more realistic
        // Maximum increase: +8°C for extreme humidity
        double maxIncrease = 8.0;
        double increase = hiCelsius - tempCelsius;

        if (increase > maxIncrease) {
            hiCelsius = tempCelsius + maxIncrease;
        }

        // Also ensure heat index is never less than actual temperature
        return Math.max(tempCelsius, hiCelsius);
    }

    /**
     * Estimate temperature for specific hour based on daily min/max
     * Uses sinusoidal model: coolest at sunrise, hottest at 3 PM
     */
    private double estimateHourlyTemperature(WeatherData weather, int hour) {
        Double tempMin = weather.getTemperatureMin();
        Double tempMax = weather.getTemperatureMax();
        Double tempAvg = weather.getTemperatureCelsius();

        // If no min/max available, use average
        if (tempMin == null || tempMax == null) {
            return tempAvg;
        }

        // Temperature peaks around 3 PM (15:00), lowest at sunrise (6:00)
        // Use sinusoidal approximation
        double amplitude = (tempMax - tempMin) / 2.0;
        double midpoint = (tempMax + tempMin) / 2.0;

        // Phase shift: minimum at 6 AM, maximum at 3 PM
        // Convert hour to radians (0-24 hours -> 0-2π)
        double hourAngle = ((hour - 6) * Math.PI) / 12.0; // Shifted so 6 AM = 0

        // Sinusoidal temperature variation
        double temp = midpoint + amplitude * Math.sin(hourAngle);

        // Clamp to min/max range
        return Math.max(tempMin, Math.min(tempMax, temp));
    }

    /**
     * Calculate required hydration based on temperature, duration, and activity
     * intensity
     */
    private double calculateHydrationLiters(double tempCelsius, int durationMinutes, ActivityType activityType) {
        // Base hydration: 0.5L per hour
        double baseRate = 0.5;

        // Temperature multiplier
        double tempMultiplier = 1.0;
        if (tempCelsius > 35)
            tempMultiplier = 2.5;
        else if (tempCelsius > 30)
            tempMultiplier = 2.0;
        else if (tempCelsius > 25)
            tempMultiplier = 1.5;

        // Activity intensity multiplier
        double activityMultiplier = 1.0;
        if (activityType == ActivityType.OUTDOOR_TREK || activityType == ActivityType.OUTDOOR_ADVENTURE) {
            activityMultiplier = 1.5; // Strenuous activity
        }

        // Calculate total
        double hours = durationMinutes / 60.0;
        double liters = baseRate * hours * tempMultiplier * activityMultiplier;

        // Round to nearest 0.5L
        return Math.ceil(liters * 2) / 2.0;
    }

    /**
     * Calculate suitability score (0-100) for activity at specific hour
     * Now uses hourly temperature and heat index for more accurate scoring
     */
    private int calculateSuitabilityScore(ActivityType type, WeatherData weather, int hour,
            double hourlyTemp, double feelsLikeTemp) {
        int score = 100; // Start with perfect score

        double temp = hourlyTemp; // Use hourly temperature instead of daily average
        double feelsLike = feelsLikeTemp; // Heat index (feels like)
        String condition = weather.getCondition().toLowerCase();
        int precipChance = weather.getPrecipitationChance();
        Integer humidity = weather.getHumidity();

        switch (type) {
            case OUTDOOR_TREK:
            case OUTDOOR_ADVENTURE:
                // Treks can handle extreme conditions but with proper preparation
                // Less penalty for cold, more emphasis on clear weather
                if (temp < -15)
                    score -= 30; // Extreme cold but doable with gear
                else if (temp < -5)
                    score -= 15; // Cold but manageable for treks
                else if (temp < 0)
                    score -= 5; // Freezing but acceptable
                else if (temp < 10)
                    score -= 10; // Cool

                // Use heat index for hot weather (accounts for humidity)
                // Adventure activities can handle heat better with proper preparation
                if (feelsLike > 43)
                    score -= 50; // Dangerous heat index
                else if (feelsLike > 40)
                    score -= 40; // Extreme heat index
                else if (feelsLike > 38)
                    score -= 30; // Very high heat index
                else if (feelsLike > 35)
                    score -= 20; // High heat index
                else if (feelsLike > 32)
                    score -= 10; // Elevated heat index
                else if (temp >= 10 && temp <= 25)
                    score += 10; // Perfect for trekking

                // Clear weather is critical for treks
                if (condition.contains("clear"))
                    score += 15;
                if (condition.contains("storm"))
                    score -= 90; // Dangerous
                if (condition.contains("rain"))
                    score -= 40; // Slippery trails
                if (precipChance > 70)
                    score -= 30;

                break;

            case OUTDOOR_PARK:
            case OUTDOOR_MONUMENT:
                // Casual outdoor activities - more temperature sensitive
                if (temp < 10)
                    score -= 50; // Too cold for casual visit
                else if (temp < 15)
                    score -= 30; // Cold

                // Use heat index for hot weather (more sensitive for casual visitors)
                if (feelsLike > 43)
                    score -= 60; // Dangerous - strongly discourage
                else if (feelsLike > 40)
                    score -= 50; // Extreme heat index
                else if (feelsLike > 38)
                    score -= 40; // Very high heat index
                else if (feelsLike > 35)
                    score -= 30; // High heat index
                else if (feelsLike > 32)
                    score -= 20; // Elevated heat index
                else if (temp >= 15 && temp <= 28)
                    score += 10; // Perfect temp

                // Peak hot hours penalty (12 PM - 4 PM) - now uses actual hourly temp
                if (hour >= 12 && hour <= 16 && feelsLike > 35) {
                    score -= 30; // Extra penalty during peak heat
                }

                // Weather penalties - BALANCED approach for rain
                // Don't completely eliminate outdoor activities during rain
                // Agents should maintain variety with covered/sheltered options
                if (condition.contains("storm") || condition.contains("thunder")) {
                    score -= 80; // Storms are genuinely dangerous
                } else if (condition.contains("rain")) {
                    // Moderate penalty for rain, not elimination
                    // Covered monuments, sheltered areas still viable
                    score -= 30; // Reduced from -50
                } else if (precipChance > 70) {
                    score -= 25; // Reduced from -40
                } else if (precipChance > 50) {
                    score -= 15; // Reduced from -20
                }

                // Clear weather bonus
                if (condition.contains("clear") || condition.contains("sun"))
                    score += 5;

                break;

            case SCENIC_SPOT:
                // Golden hour bonuses (best lighting for photography)
                if (hour >= 6 && hour <= 9)
                    score += 30; // Morning golden hour
                if (hour >= 16 && hour <= 19)
                    score += 30; // Evening golden hour

                // Harsh midday light penalty
                if (hour >= 11 && hour <= 15)
                    score -= 25;

                // Weather for photography
                if (condition.contains("clear"))
                    score += 10;
                if (condition.contains("partly") && condition.contains("cloud"))
                    score += 5; // Soft light
                if (condition.contains("overcast"))
                    score -= 10; // Flat light
                if (condition.contains("rain"))
                    score -= 40;

                // Temperature still matters
                if (temp < 10 || temp > 38)
                    score -= 20;

                break;

            case BEACH_WATER:
                // Hot weather preferred for water activities
                if (temp > 30)
                    score += 20;
                else if (temp > 28)
                    score += 10;
                else if (temp < 20)
                    score -= 50;
                else if (temp < 25)
                    score -= 20;

                // Clear weather essential for water activities
                if (condition.contains("clear") || condition.contains("sun"))
                    score += 20;
                if (condition.contains("storm"))
                    score -= 90; // Dangerous
                if (condition.contains("rain"))
                    score -= 60; // Strong penalty but not elimination
                if (precipChance > 70)
                    score -= 50;
                else if (precipChance > 50)
                    score -= 30;

                // Midday is fine for beach
                if (hour >= 10 && hour <= 17)
                    score += 10;

                break;

            case INDOOR_MUSEUM:
            case INDOOR_MALL:
            case INDOOR_TEMPLE:
                // Peak heat hours bonus (AC refuge)
                if (hour >= 12 && hour <= 16 && temp > 32) {
                    score += 25;
                }

                // Rainy weather bonus (indoor refuge) - MODERATE bonus
                // Don't over-prioritize indoor activities during rain
                // Maintain balanced itinerary with outdoor options too
                if (condition.contains("storm")) {
                    score += 30; // Strong preference during storms
                } else if (condition.contains("rain") || precipChance > 70) {
                    score += 15; // Moderate bonus, not overwhelming
                } else if (precipChance > 50) {
                    score += 10;
                }

                // Otherwise weather independent
                break;

            case NIGHT_ACTIVITY:
                // Only evening/night suitable
                if (hour < 18) {
                    score = 0; // Not suitable during day
                } else {
                    score += 30; // Evening bonus

                    // Weather still matters a bit
                    if (condition.contains("rain"))
                        score -= 30;
                    if (temp < 15)
                        score -= 15;
                    if (temp > 35)
                        score -= 10;
                }
                break;

            case FLEXIBLE:
                // Avoid extreme conditions
                if (temp < 10 || temp > 38)
                    score -= 20;
                if (condition.contains("storm"))
                    score -= 40;
                if (condition.contains("rain"))
                    score -= 20;
                break;
        }

        // Ensure score is within 0-100 range
        return Math.max(0, Math.min(100, score));
    }

    /**
     * Generate human-readable reason for the suitability score
     * Now includes heat index information when relevant
     */
    private String generateReason(ActivityType type, WeatherData weather, int hour, int score,
            double hourlyTemp, double feelsLikeTemp) {
        double temp = hourlyTemp;
        double feelsLike = feelsLikeTemp;
        String condition = weather.getCondition();
        Integer humidity = weather.getHumidity();

        StringBuilder reason = new StringBuilder();

        // Time-based reasons
        if (type == ActivityType.SCENIC_SPOT) {
            if (hour >= 6 && hour <= 9) {
                reason.append("Golden hour lighting perfect for photography, ");
            } else if (hour >= 16 && hour <= 19) {
                reason.append("Evening golden hour with beautiful lighting, ");
            } else if (hour >= 11 && hour <= 15) {
                reason.append("Harsh midday light not ideal for photos, ");
            }
        }

        if (type == ActivityType.NIGHT_ACTIVITY && hour >= 18) {
            reason.append("Perfect evening time for night activities, ");
        }

        // Temperature-based reasons (with heat index when significant)
        boolean highHumidity = humidity != null && humidity > 65;
        double tempDiff = feelsLike - temp;
        boolean showHeatIndex = highHumidity && tempDiff > 4; // Only show if difference is significant

        if (temp >= 15 && temp <= 28) {
            reason.append(String.format("comfortable temperature (%.0f°C)", temp));
        } else if (feelsLike > 42) {
            // Extreme heat index
            if (showHeatIndex) {
                reason.append(String.format("extreme heat (%.0f°C, feels like %.0f°C)", temp, feelsLike));
            } else {
                reason.append(String.format("extreme heat (%.0f°C)", temp));
            }
        } else if (feelsLike > 38 && hour >= 12 && hour <= 16) {
            // Peak heat hours
            if (showHeatIndex) {
                reason.append(String.format("peak heat (%.0f°C, feels like %.0f°C) - indoor activities recommended",
                        temp, feelsLike));
            } else {
                reason.append(String.format("peak heat (%.0f°C) - consider indoor activities", temp));
            }
        } else if (temp > 32) {
            if (showHeatIndex) {
                reason.append(String.format("hot (%.0f°C, feels like %.0f°C)", temp, feelsLike));
            } else {
                reason.append(String.format("hot (%.0f°C) but manageable", temp));
            }
        } else if (temp < 15) {
            reason.append(String.format("cool temperature (%.0f°C)", temp));
        }

        // Weather-based reasons
        if (condition.toLowerCase().contains("storm")) {
            if (reason.length() > 0)
                reason.append(", ");
            reason.append("stormy weather - indoor activities strongly recommended");
        } else if (condition.toLowerCase().contains("clear") || condition.toLowerCase().contains("sun")) {
            if (reason.length() > 0)
                reason.append(", ");
            reason.append("clear weather");
        } else if (condition.toLowerCase().contains("rain")) {
            if (reason.length() > 0)
                reason.append(", ");
            // Balanced messaging - don't force everything indoors
            if (type == ActivityType.OUTDOOR_MONUMENT || type == ActivityType.OUTDOOR_PARK) {
                reason.append("rainy conditions - covered/sheltered areas preferred");
            } else if (type == ActivityType.INDOOR_MUSEUM || type == ActivityType.INDOOR_MALL
                    || type == ActivityType.INDOOR_TEMPLE) {
                reason.append("rainy conditions - good time for indoor activities");
            } else {
                reason.append("rainy conditions");
            }
        } else if (condition.toLowerCase().contains("cloud")) {
            if (reason.length() > 0)
                reason.append(", ");
            reason.append("cloudy conditions");
        }

        // Indoor activity bonus during bad weather - balanced messaging
        // Only add if not already mentioned in rain condition above
        if ((type == ActivityType.INDOOR_MUSEUM || type == ActivityType.INDOOR_MALL
                || type == ActivityType.INDOOR_TEMPLE)) {
            if (condition.toLowerCase().contains("storm")) {
                if (reason.length() > 0)
                    reason.append(", ");
                reason.append("ideal time for indoor activities");
            } else if (temp > 38) {
                if (reason.length() > 0)
                    reason.append(", ");
                reason.append("ideal time for AC refuge");
            } else if (temp > 32 && !condition.toLowerCase().contains("rain")) {
                // Only add if not rain (already handled above)
                if (reason.length() > 0)
                    reason.append(", ");
                reason.append("good time for indoor activities");
            }
        }

        return reason.toString();
    }

    /**
     * Detect if weather conditions are extreme and require special preparation
     */
    public static class ExtremeConditions {
        private boolean isExtreme;
        private String severity; // "moderate", "severe", "extreme"
        private List<String> warnings;
        private List<String> gearRequirements;

        public ExtremeConditions() {
            this.warnings = new ArrayList<>();
            this.gearRequirements = new ArrayList<>();
        }

        public boolean isExtreme() {
            return isExtreme;
        }

        public void setExtreme(boolean extreme) {
            isExtreme = extreme;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public void setWarnings(List<String> warnings) {
            this.warnings = warnings;
        }

        public List<String> getGearRequirements() {
            return gearRequirements;
        }

        public void setGearRequirements(List<String> gearRequirements) {
            this.gearRequirements = gearRequirements;
        }
    }

    /**
     * Analyze weather for extreme conditions
     * Now includes heat index and hydration calculations
     */
    public ExtremeConditions analyzeExtremeConditions(WeatherData weather, ActivityType activityType,
            String activityName, int durationMinutes) {
        ExtremeConditions conditions = new ExtremeConditions();
        double temp = weather.getTemperatureCelsius();
        String condition = weather.getCondition().toLowerCase();
        int precipChance = weather.getPrecipitationChance();
        Integer humidity = weather.getHumidity();

        // Calculate heat index if humidity available
        double feelsLike = temp;
        if (humidity != null && humidity > 0) {
            feelsLike = calculateHeatIndex(temp, humidity);
        }

        // Detect extreme cold (< 0°C)
        if (temp < 0) {
            conditions.setExtreme(true);
            if (temp < -15) {
                conditions.setSeverity("extreme");
                conditions.getWarnings()
                        .add("EXTREME COLD: Temperatures below -15°C. Risk of frostbite and hypothermia.");
                conditions.getGearRequirements().add("Heavy winter gear: Down jacket, thermal layers, insulated boots");
                conditions.getGearRequirements().add("Face protection: Balaclava, goggles, hand warmers");
                conditions.getGearRequirements().add("Emergency supplies: First aid, emergency blanket");
            } else if (temp < -5) {
                conditions.setSeverity("severe");
                conditions.getWarnings().add("SEVERE COLD: Sub-zero temperatures. Proper winter gear essential.");
                conditions.getGearRequirements().add("Winter clothing: Insulated jacket, thermal wear, winter boots");
                conditions.getGearRequirements().add("Accessories: Gloves, warm hat, neck warmer");
            } else {
                conditions.setSeverity("moderate");
                conditions.getWarnings().add("COLD CONDITIONS: Freezing temperatures. Warm clothing required.");
                conditions.getGearRequirements().add("Warm layers: Jacket, sweater, warm pants");
                conditions.getGearRequirements().add("Basic accessories: Gloves, hat");
            }

            // Trek-specific warnings for extreme cold
            if (activityType == ActivityType.OUTDOOR_TREK ||
                    activityName.toLowerCase().contains("trek") ||
                    activityName.toLowerCase().contains("hike")) {
                conditions.getWarnings()
                        .add("TREK ALERT: Extended outdoor exposure in extreme cold. Professional guide recommended.");
                conditions.getGearRequirements().add("Trek essentials: Crampons, trekking poles, high-altitude gear");
            }
        }

        // Detect extreme heat (using heat index for more accurate assessment)
        if (feelsLike > 43 || temp > 42) {
            conditions.setExtreme(true);
            conditions.setSeverity("extreme");

            if (feelsLike > temp + 4) {
                conditions.getWarnings().add(String.format(
                        "EXTREME HEAT: Temperature %.0f°C feels like %.0f°C with high humidity. Risk of heat stroke and dehydration.",
                        temp, feelsLike));
            } else {
                conditions.getWarnings()
                        .add("EXTREME HEAT: Temperatures above 42°C. Risk of heat stroke and dehydration.");
            }

            // Calculate required hydration
            double waterLiters = calculateHydrationLiters(feelsLike, durationMinutes, activityType);

            conditions.getGearRequirements().add("Sun protection: Hat, sunglasses, sunscreen SPF 50+");
            conditions.getGearRequirements()
                    .add(String.format("Hydration: Carry %.1f liters of water per person", waterLiters));
            conditions.getGearRequirements().add("Light clothing: Breathable, light-colored fabrics");
            conditions.getGearRequirements().add("Cooling aids: Wet towel, cooling vest if available");

        } else if (feelsLike > 40 || temp > 38) {
            conditions.setExtreme(true);
            conditions.setSeverity("severe");

            if (feelsLike > temp + 4) {
                conditions.getWarnings().add(String.format(
                        "SEVERE HEAT: Temperature %.0f°C feels like %.0f°C with high humidity. Limit outdoor exposure during peak hours.",
                        temp, feelsLike));
            } else {
                conditions.getWarnings()
                        .add("SEVERE HEAT: Very high temperatures. Limit outdoor exposure during peak hours.");
            }

            // Calculate required hydration
            double waterLiters = calculateHydrationLiters(feelsLike, durationMinutes, activityType);

            conditions.getGearRequirements().add("Sun protection: Hat, sunglasses, sunscreen SPF 30+");
            conditions.getGearRequirements()
                    .add(String.format("Hydration: Carry %.1f liters of water per person", waterLiters));
            conditions.getGearRequirements().add("Light clothing: Breathable fabrics");

        } else if (feelsLike > 37 && humidity != null && humidity > 65) {
            // High heat with high humidity
            conditions.setExtreme(true);
            conditions.setSeverity("moderate");
            conditions.getWarnings().add(String.format(
                    "HIGH HEAT WITH HUMIDITY: Temperature %.0f°C with high humidity. Stay well hydrated.",
                    temp));

            double waterLiters = calculateHydrationLiters(feelsLike, durationMinutes, activityType);
            conditions.getGearRequirements().add("Sun protection: Hat, sunglasses, sunscreen");
            conditions.getGearRequirements().add(String.format("Hydration: Carry %.1f liters of water", waterLiters));
        }

        // Detect storms
        if (condition.contains("storm") || condition.contains("thunder")) {
            conditions.setExtreme(true);
            conditions.setSeverity("extreme");
            conditions.getWarnings().add("STORM WARNING: Dangerous weather conditions. Avoid outdoor activities.");
            conditions.getGearRequirements().add("Stay indoors or seek shelter immediately");
        }

        // Heavy rain with trek
        if (precipChance > 80 && (activityType == ActivityType.OUTDOOR_TREK ||
                activityName.toLowerCase().contains("trek"))) {
            conditions.setExtreme(true);
            conditions.setSeverity("severe");
            conditions.getWarnings().add("HEAVY RAIN: Trek conditions may be dangerous. Trails could be slippery.");
            conditions.getGearRequirements().add("Rain gear: Waterproof jacket, rain pants, waterproof backpack cover");
            conditions.getGearRequirements().add("Safety: Non-slip trekking shoes, trekking poles");
        }

        return conditions;
    }

    /**
     * Get alternative activity suggestions during adverse weather
     * Maintains balanced itinerary even during rain
     */
    public List<String> getAlternativeActivities(ActivityType originalType, WeatherData weather) {
        List<String> alternatives = new ArrayList<>();

        String condition = weather.getCondition().toLowerCase();
        double temp = weather.getTemperatureCelsius();
        int precipChance = weather.getPrecipitationChance();

        // During storms - strongly suggest indoor alternatives
        if (condition.contains("storm")) {
            alternatives.add("Consider indoor museums, galleries, or shopping malls");
            alternatives.add("Visit covered temples or religious sites");
            alternatives.add("Explore indoor markets or food courts");
            return alternatives;
        }

        // During rain - suggest BALANCED alternatives (not just indoor)
        if (condition.contains("rain") || precipChance > 60) {
            if (originalType == ActivityType.OUTDOOR_PARK || originalType == ActivityType.OUTDOOR_MONUMENT) {
                alternatives.add("Visit covered monuments or forts with sheltered areas");
                alternatives.add("Explore museums or indoor cultural sites");
                alternatives.add("Consider covered markets or bazaars");
                // Note: NOT forcing everything indoors - maintaining variety
            } else if (originalType == ActivityType.BEACH_WATER) {
                alternatives.add("Visit aquariums or marine museums");
                alternatives.add("Explore coastal temples or covered viewpoints");
                alternatives.add("Indoor water parks or spa experiences");
            }
        }

        // During extreme heat
        if (temp > 38) {
            alternatives.add("Visit air-conditioned museums or malls during peak heat (12-4 PM)");
            alternatives.add("Schedule outdoor activities for early morning (6-9 AM) or evening (5-7 PM)");
            alternatives.add("Consider water-based activities or shaded gardens");
        }

        return alternatives;
    }

    /**
     * Classify activity type from title/description
     */
    public ActivityType classifyActivity(String title, String description) {
        if (title == null)
            title = "";
        if (description == null)
            description = "";

        String combined = (title + " " + description).toLowerCase();

        // Night activities
        if (combined.contains("night market") || combined.contains("night show") ||
                combined.contains("night view") || combined.contains("evening") ||
                combined.contains("sunset") || combined.contains("nightlife")) {
            return ActivityType.NIGHT_ACTIVITY;
        }

        // Water activities
        if (combined.contains("beach") || combined.contains("water park") ||
                combined.contains("swimming") || combined.contains("snorkeling") ||
                combined.contains("diving") || combined.contains("surf")) {
            return ActivityType.BEACH_WATER;
        }

        // Scenic spots
        if (combined.contains("viewpoint") || combined.contains("photography") ||
                combined.contains("scenic") || combined.contains("panorama") ||
                combined.contains("white town") || combined.contains("heritage walk")) {
            return ActivityType.SCENIC_SPOT;
        }

        // Indoor activities
        if (combined.contains("museum") || combined.contains("gallery") ||
                combined.contains("exhibition")) {
            return ActivityType.INDOOR_MUSEUM;
        }

        if (combined.contains("mall") || combined.contains("shopping") ||
                combined.contains("market") && !combined.contains("night")) {
            return ActivityType.INDOOR_MALL;
        }

        if (combined.contains("temple") || combined.contains("church") ||
                combined.contains("mosque") || combined.contains("shrine") ||
                combined.contains("cathedral") || combined.contains("ashram")) {
            return ActivityType.INDOOR_TEMPLE;
        }

        // Trek and adventure activities
        if (combined.contains("trek") || combined.contains("hike") ||
                combined.contains("hiking") || combined.contains("trekking") ||
                combined.contains("mountain") || combined.contains("climb")) {
            return ActivityType.OUTDOOR_TREK;
        }

        if (combined.contains("rafting") || combined.contains("paragliding") ||
                combined.contains("skiing") || combined.contains("adventure") ||
                combined.contains("bungee") || combined.contains("zip")) {
            return ActivityType.OUTDOOR_ADVENTURE;
        }

        // Outdoor activities
        if (combined.contains("park") || combined.contains("garden") ||
                combined.contains("botanical") || combined.contains("zoo")) {
            return ActivityType.OUTDOOR_PARK;
        }

        if (combined.contains("fort") || combined.contains("palace") ||
                combined.contains("monument") || combined.contains("memorial") ||
                combined.contains("ruins") || combined.contains("historical")) {
            return ActivityType.OUTDOOR_MONUMENT;
        }

        // Default to flexible
        return ActivityType.FLEXIBLE;
    }
}
