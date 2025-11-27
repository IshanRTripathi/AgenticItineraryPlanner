package com.tripplanner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.enums.ToolType;
import com.tripplanner.service.ai.AiClient;
import com.tripplanner.service.cache.ToolCacheService;
import com.tripplanner.util.ToolCacheKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@ConditionalOnBean(AiClient.class)
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);
    private static final String WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final int FORECAST_THRESHOLD_DAYS = 5;

    @Value("${openweather.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    // Optional: Tool cache service (only injected if enabled)
    @Autowired(required = false)
    private ToolCacheService toolCacheService;

    public WeatherService(RestTemplate restTemplate, AiClient aiClient) {
        this.restTemplate = restTemplate;
        this.aiClient = aiClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get weather data with caching support.
     * Uses ToolCacheService if available, otherwise calls API directly.
     * 
     * @param itineraryId The itinerary ID for cache scoping (null for global cache)
     * @param location    Location name
     * @param date        Date in ISO format (yyyy-MM-dd)
     * @return Weather data
     */
    public WeatherData getWeather(String itineraryId, String location, String date) {
        logger.info("🔍 getWeather called: itineraryId={}, location={}, date={}, toolCacheService={}", 
            itineraryId, location, date, toolCacheService != null ? "INJECTED" : "NULL");

        // Use cache if available
        if (toolCacheService != null && itineraryId != null) {
            String cacheKey = ToolCacheKeyGenerator.forWeather(location, date);
            logger.info("🔍 Attempting cache lookup: key={}", cacheKey);

            try {
                WeatherData cached = toolCacheService.getOrCompute(
                        itineraryId,
                        ToolType.GET_WEATHER.getValue(),
                        cacheKey,
                        Map.of("location", location, "date", date),
                        () -> fetchWeatherData(itineraryId, location, date),
                        WeatherData.class);

                if (cached != null) {
                    logger.info("✅ Returning cached weather for {} on {}", location, date);
                    return cached;
                }
                logger.warn("⚠️ Cache returned null for {} on {}", location, date);
            } catch (Exception e) {
                logger.error("❌ Cache lookup failed: {}", e.getMessage(), e);
            }
        } else {
            logger.warn("⚠️ Cache bypassed: toolCacheService={}, itineraryId={}", 
                toolCacheService != null ? "present" : "null", 
                itineraryId != null ? "present" : "null");
        }

        // No cache or cache failed - fetch directly
        return fetchWeatherData(itineraryId, location, date);
    }

    /**
     * Legacy method for backward compatibility (no caching).
     * 
     * @deprecated Use getWeather(itineraryId, location, date) instead
     */
    @Deprecated
    public WeatherData getWeather(String location, String date) {
        return getWeather(null, location, date);
    }

    /**
     * Fetch weather data from API or LLM (actual implementation).
     */
    private WeatherData fetchWeatherData(String itineraryId, String location, String date) {
        logger.info("Fetching weather from API for location: {}, date: {}", location, date);

        try {
            LocalDate tripDate = LocalDate.parse(date);
            LocalDate today = LocalDate.now();
            long daysUntilTrip = ChronoUnit.DAYS.between(today, tripDate);

            if (daysUntilTrip <= 0) {
                logger.info("Using current weather API for today/past date");
                return getOpenWeatherCurrent(location);

            } else if (daysUntilTrip <= FORECAST_THRESHOLD_DAYS) {
                logger.info("Using OpenWeather API for near-term forecast ({} days)", daysUntilTrip);
                return getOpenWeatherCurrent(location);

            } else {
                logger.info("Using LLM climate prediction for long-term forecast ({} days)", daysUntilTrip);
                return getLLMClimatePrediction(itineraryId, location, tripDate);
            }

        } catch (Exception e) {
            logger.error("Failed to get weather: {}", e.getMessage(), e);
            return getFallbackWeather();
        }
    }

    private WeatherData getOpenWeatherCurrent(String location) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("OpenWeather API key not configured");
            return getFallbackWeather();
        }

        try {
            String url = String.format("%s?q=%s&appid=%s&units=metric",
                    WEATHER_API_URL, location, apiKey);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                WeatherData data = parseWeatherResponse(response.getBody());
                data.setIsHistoricalEstimate(false);
                data.setConfidence("high");
                return data;
            }

        } catch (Exception e) {
            logger.error("Failed to fetch weather from API: {}", e.getMessage());
        }

        return getFallbackWeather();
    }

    private WeatherData getLLMClimatePrediction(String itineraryId, String location, LocalDate tripDate) {
        String month = tripDate.getMonth().toString();
        int day = tripDate.getDayOfMonth();

        // Use cache if available (cache by month to save tokens)
        if (toolCacheService != null && itineraryId != null) {
            String cacheKey = location + "_" + month;
            try {
                return toolCacheService.getOrCompute(
                        itineraryId,
                        ToolType.GET_WEATHER.getValue(),
                        cacheKey,
                        Map.of("location", location, "month", month),
                        () -> generateClimatePrediction(location, month, day),
                        WeatherData.class);
            } catch (Exception e) {
                logger.warn("Climate cache lookup failed: {}", e.getMessage());
            }
        }

        return generateClimatePrediction(location, month, day);
    }

    private WeatherData generateClimatePrediction(String location, String month, int day) {
        String prompt = String.format("""
                Provide typical weather conditions for %s on %s %d based on historical climate data.

                Return ONLY a valid JSON object with this exact structure (no markdown, no code blocks, no explanation):
                {
                  "temperatureCelsius": <average temperature as number>,
                  "temperatureMin": <typical minimum as number>,
                  "temperatureMax": <typical maximum as number>,
                  "condition": "<Clear|Clouds|Rain|Storm>",
                  "precipitationChance": <percentage 0-100 as number>,
                  "humidity": <percentage 0-100 as number>,
                  "description": "<brief description>",
                  "seasonalNotes": "<important seasonal information>",
                  "confidence": "<high|medium|low>"
                }

                Base your response on:
                - Historical climate patterns for this location
                - Typical seasonal weather for this month
                - Regional climate characteristics
                - Known weather patterns (monsoons, dry seasons, etc.)

                Be realistic and conservative. Use actual climate data knowledge.
                """, location, month, day);

        try {
            String systemPrompt = "You are a climate data expert. Provide accurate historical weather patterns based on real climate data.";
            String response = aiClient.generateContent(prompt, systemPrompt);
            WeatherData data = parseClimateResponse(response, location, month);
            data.setIsHistoricalEstimate(true);
            return data;

        } catch (Exception e) {
            logger.error("LLM climate prediction failed for {} in {}: {}", location, month, e.getMessage());
            // We need a way to return fallback data here, but this method returns
            // WeatherData.
            // We can't easily access getFallbackClimateForMonth from here without passing
            // more context or changing structure.
            // For now, let's throw/catch or handle it.
            // Actually, getFallbackClimateForMonth requires Month enum.
            // Let's parse the month string back to enum or pass it.
            try {
                return getFallbackClimateForMonth(location, java.time.Month.valueOf(month));
            } catch (Exception ex) {
                return getFallbackWeather();
            }
        }
    }

    private WeatherData parseClimateResponse(String jsonResponse, String location, String month) {
        try {
            // Clean response (remove markdown code blocks if present)
            String cleaned = jsonResponse.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            }

            JsonNode root = objectMapper.readTree(cleaned);

            WeatherData data = new WeatherData();
            data.setTemperatureCelsius(root.get("temperatureCelsius").asDouble());
            data.setTemperatureMin(root.get("temperatureMin").asDouble());
            data.setTemperatureMax(root.get("temperatureMax").asDouble());
            data.setCondition(root.get("condition").asText());
            data.setPrecipitationChance(root.get("precipitationChance").asInt());
            data.setHumidity(root.get("humidity").asInt());

            String baseDescription = root.get("description").asText();
            String confidence = root.get("confidence").asText();
            data.setDescription(baseDescription + " (Historical average - " + confidence + " confidence)");

            data.setSeasonalNotes(root.get("seasonalNotes").asText());
            data.setConfidence(confidence);
            data.setIsHistoricalEstimate(true);

            logger.info("LLM climate prediction for {} in {}: {}°C, {}, {}% rain",
                    location, month, data.getTemperatureCelsius(),
                    data.getCondition(), data.getPrecipitationChance());

            return data;

        } catch (Exception e) {
            logger.error("Failed to parse LLM climate response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse climate data", e);
        }
    }

    private WeatherData getFallbackClimateForMonth(String location, java.time.Month month) {
        // Basic seasonal fallback for India (most common use case)
        WeatherData data = new WeatherData();
        data.setIsHistoricalEstimate(true);
        data.setConfidence("low");

        switch (month) {
            case DECEMBER, JANUARY, FEBRUARY:
                data.setTemperatureCelsius(20.0);
                data.setTemperatureMin(15.0);
                data.setTemperatureMax(25.0);
                data.setCondition("Clear");
                data.setPrecipitationChance(5);
                data.setDescription("Cool and dry winter season (fallback estimate)");
                break;
            case MARCH, APRIL, MAY:
                data.setTemperatureCelsius(32.0);
                data.setTemperatureMin(28.0);
                data.setTemperatureMax(38.0);
                data.setCondition("Clear");
                data.setPrecipitationChance(15);
                data.setDescription("Hot summer season (fallback estimate)");
                break;
            case JUNE, JULY, AUGUST, SEPTEMBER:
                data.setTemperatureCelsius(28.0);
                data.setTemperatureMin(24.0);
                data.setTemperatureMax(32.0);
                data.setCondition("Rain");
                data.setPrecipitationChance(75);
                data.setDescription("Monsoon season with frequent rainfall (fallback estimate)");
                break;
            case OCTOBER, NOVEMBER:
                data.setTemperatureCelsius(26.0);
                data.setTemperatureMin(22.0);
                data.setTemperatureMax(30.0);
                data.setCondition("Clouds");
                data.setPrecipitationChance(30);
                data.setDescription("Pleasant post-monsoon season (fallback estimate)");
                break;
        }

        logger.warn("Using fallback climate data for {} in {}", location, month);
        return data;
    }

    @SuppressWarnings("unchecked")
    private WeatherData parseWeatherResponse(Map<String, Object> response) {
        WeatherData data = new WeatherData();

        try {
            Map<String, Object> main = (Map<String, Object>) response.get("main");
            java.util.List<Map<String, Object>> weatherList = (java.util.List<Map<String, Object>>) response
                    .get("weather");

            if (main != null) {
                data.setTemperatureCelsius(((Number) main.get("temp")).doubleValue());
            }

            if (weatherList != null && !weatherList.isEmpty()) {
                Map<String, Object> weather = weatherList.get(0);
                data.setCondition((String) weather.get("main"));
                data.setDescription((String) weather.get("description"));
            }

            if (main != null && main.containsKey("humidity")) {
                int humidity = ((Number) main.get("humidity")).intValue();
                data.setPrecipitationChance(humidity > 70 ? 60 : humidity > 50 ? 30 : 10);
            }

        } catch (Exception e) {
            logger.error("Error parsing weather response", e);
            return getFallbackWeather();
        }

        return data;
    }

    private WeatherData getFallbackWeather() {
        WeatherData data = new WeatherData();
        data.setCondition("Clear");
        data.setTemperatureCelsius(22.0);
        data.setPrecipitationChance(10);
        data.setDescription("Pleasant weather");
        return data;
    }

    public static class WeatherData {
        private String condition;
        private Double temperatureCelsius;
        private Double temperatureMin;
        private Double temperatureMax;
        private Integer precipitationChance;
        private Integer humidity;
        private String description;
        private String seasonalNotes;
        private String confidence; // high, medium, low
        private Boolean isHistoricalEstimate; // true if from LLM, false if from API

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public Double getTemperatureCelsius() {
            return temperatureCelsius;
        }

        public void setTemperatureCelsius(Double temperatureCelsius) {
            this.temperatureCelsius = temperatureCelsius;
        }

        public Double getTemperatureMin() {
            return temperatureMin;
        }

        public void setTemperatureMin(Double temperatureMin) {
            this.temperatureMin = temperatureMin;
        }

        public Double getTemperatureMax() {
            return temperatureMax;
        }

        public void setTemperatureMax(Double temperatureMax) {
            this.temperatureMax = temperatureMax;
        }

        public Integer getPrecipitationChance() {
            return precipitationChance;
        }

        public void setPrecipitationChance(Integer precipitationChance) {
            this.precipitationChance = precipitationChance;
        }

        public Integer getHumidity() {
            return humidity;
        }

        public void setHumidity(Integer humidity) {
            this.humidity = humidity;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getSeasonalNotes() {
            return seasonalNotes;
        }

        public void setSeasonalNotes(String seasonalNotes) {
            this.seasonalNotes = seasonalNotes;
        }

        public String getConfidence() {
            return confidence;
        }

        public void setConfidence(String confidence) {
            this.confidence = confidence;
        }

        public Boolean getIsHistoricalEstimate() {
            return isHistoricalEstimate;
        }

        public void setIsHistoricalEstimate(Boolean isHistoricalEstimate) {
            this.isHistoricalEstimate = isHistoricalEstimate;
        }
    }
}
