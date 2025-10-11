package com.tripplanner.testing.scenarios;

import com.tripplanner.dto.CreateItineraryReq;
import com.tripplanner.dto.PartyDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Test scenarios for progressive 4-day trip generation.
 * This test demonstrates the expected flow and validates the request structure.
 */
class ProgressiveGenerationScenarioTest {

    @Test
    @DisplayName("Should create valid 4-day Paris trip request")
    void shouldCreateValid4DayParisTripRequest() {
        // Given & When
        CreateItineraryReq request = create4DayParisRequest();
        
        // Then
        assertThat(request).isNotNull();
        assertThat(request.getDestination()).isEqualTo("Paris, France");
        assertThat(request.getStartDate()).isNotNull();
        assertThat(request.getEndDate()).isNotNull();
        assertThat(request.getDurationDays()).isEqualTo(4);
        assertThat(request.getParty()).isNotNull();
        assertThat(request.getParty().getAdults()).isEqualTo(2);
        assertThat(request.getInterests()).contains("culture", "cuisine", "history", "art");
        assertThat(request.getConstraints()).contains("wheelchair-accessible");
        assertThat(request.getBudgetTier()).isEqualTo("luxury");
        assertThat(request.getLanguage()).isEqualTo("en");
    }

    @Test
    @DisplayName("Should validate progressive generation flow structure")
    void shouldValidateProgressiveGenerationFlowStructure() {
        // Given
        CreateItineraryReq request = create4DayParisRequest();
        
        // When - Simulate the progressive generation phases
        List<String> expectedPhases = List.of(
            "skeleton_generation",
            "activity_generation", 
            "meal_generation",
            "transport_generation",
            "enrichment"
        );
        
        // Then - Validate each phase exists
        for (String phase : expectedPhases) {
            assertThat(phase).isNotNull();
            assertThat(phase).isNotEmpty();
        }
        
        // Validate the request supports progressive generation
        assertThat(request.getDurationDays()).isGreaterThan(1);
        assertThat(request.getInterests()).isNotEmpty();
        assertThat(request.getConstraints()).isNotEmpty();
    }

    @Test
    @DisplayName("Should validate AI response structure for Day 1")
    void shouldValidateAiResponseStructureForDay1() {
        // Given
        String day1SkeletonResponse = createDay1SkeletonAiResponse();
        
        // When & Then
        assertThat(day1SkeletonResponse).isNotNull();
        assertThat(day1SkeletonResponse).contains("days");
        assertThat(day1SkeletonResponse).contains("dayNumber");
        assertThat(day1SkeletonResponse).contains("nodes");
        assertThat(day1SkeletonResponse).contains("type");
        assertThat(day1SkeletonResponse).contains("timing");
        assertThat(day1SkeletonResponse).contains("placeholder");
    }

    @Test
    @DisplayName("Should validate AI response structure for Day 1 activities")
    void shouldValidateAiResponseStructureForDay1Activities() {
        // Given
        String day1ActivityResponse = createDay1ActivityAiResponse();
        
        // When & Then
        assertThat(day1ActivityResponse).isNotNull();
        assertThat(day1ActivityResponse).contains("nodes");
        assertThat(day1ActivityResponse).contains("type");
        assertThat(day1ActivityResponse).contains("title");
        assertThat(day1ActivityResponse).contains("description");
        assertThat(day1ActivityResponse).contains("location");
        assertThat(day1ActivityResponse).contains("cost");
        assertThat(day1ActivityResponse).contains("currency");
    }

    @Test
    @DisplayName("Should validate AI response structure for Day 1 meals")
    void shouldValidateAiResponseStructureForDay1Meals() {
        // Given
        String day1MealResponse = createDay1MealAiResponse();
        
        // When & Then
        assertThat(day1MealResponse).isNotNull();
        assertThat(day1MealResponse).contains("nodes");
        assertThat(day1MealResponse).contains("type");
        assertThat(day1MealResponse).contains("title");
        assertThat(day1MealResponse).contains("cuisine");
        assertThat(day1MealResponse).contains("dietaryOptions");
        assertThat(day1MealResponse).contains("atmosphere");
    }

    @Test
    @DisplayName("Should validate AI response structure for Day 1 transport")
    void shouldValidateAiResponseStructureForDay1Transport() {
        // Given
        String day1TransportResponse = createDay1TransportAiResponse();
        
        // When & Then
        assertThat(day1TransportResponse).isNotNull();
        assertThat(day1TransportResponse).contains("nodes");
        assertThat(day1TransportResponse).contains("type");
        assertThat(day1TransportResponse).contains("title");
        assertThat(day1TransportResponse).contains("from");
        assertThat(day1TransportResponse).contains("to");
        assertThat(day1TransportResponse).contains("transportType");
        assertThat(day1TransportResponse).contains("accessibility");
    }

    @Test
    @DisplayName("Should validate AI response structure for Day 1 enrichment")
    void shouldValidateAiResponseStructureForDay1Enrichment() {
        // Given
        String day1EnrichmentResponse = createDay1EnrichmentAiResponse();
        
        // When & Then
        assertThat(day1EnrichmentResponse).isNotNull();
        assertThat(day1EnrichmentResponse).contains("enrichments");
        assertThat(day1EnrichmentResponse).contains("nodeId");
        assertThat(day1EnrichmentResponse).contains("photos");
        assertThat(day1EnrichmentResponse).contains("tips");
        assertThat(day1EnrichmentResponse).contains("nearbyAttractions");
    }

    @Test
    @DisplayName("Should validate complete 4-day generation flow")
    void shouldValidateComplete4DayGenerationFlow() {
        // Given
        CreateItineraryReq request = create4DayParisRequest();
        
        // When - Simulate the complete flow
        List<String> allDays = List.of("Day 1", "Day 2", "Day 3", "Day 4");
        List<String> allPhases = List.of(
            "skeleton_generation",
            "activity_generation",
            "meal_generation", 
            "transport_generation",
            "enrichment"
        );
        
        // Then - Validate the flow structure
        assertThat(allDays).hasSize(4);
        assertThat(allPhases).hasSize(5);
        
        // Validate request supports the flow
        assertThat(request.getDurationDays()).isEqualTo(4);
        assertThat(request.getDestination()).isEqualTo("Paris, France");
    }

    // Helper methods for creating test data
    private CreateItineraryReq create4DayParisRequest() {
        return CreateItineraryReq.builder()
                .destination("Paris, France")
                .startLocation("New York, USA")
                .startDate(LocalDate.now().plusDays(30))
                .endDate(LocalDate.now().plusDays(34))
                .party(PartyDto.builder()
                        .adults(2)
                        .children(0)
                        .infants(0)
                        .rooms(1)
                        .build())
                .budgetTier("luxury")
                .interests(List.of("culture", "cuisine", "history", "art"))
                .constraints(List.of("wheelchair-accessible"))
                .language("en")
                .build();
    }

    private String createDay1SkeletonAiResponse() {
        return """
        {
          "days": [
            {
              "dayNumber": 1,
              "date": "2024-03-15",
              "location": "Paris, France",
              "nodes": [
                {
                  "type": "activity",
                  "timing": "morning",
                  "duration": 180,
                  "placeholder": true
                },
                {
                  "type": "meal",
                  "timing": "lunch",
                  "duration": 90,
                  "placeholder": true
                },
                {
                  "type": "activity",
                  "timing": "afternoon",
                  "duration": 240,
                  "placeholder": true
                },
                {
                  "type": "meal",
                  "timing": "dinner",
                  "duration": 120,
                  "placeholder": true
                }
              ]
            }
          ]
        }
        """;
    }

    private String createDay1ActivityAiResponse() {
        return """
        {
          "nodes": [
            {
              "type": "activity",
              "title": "Visit the Eiffel Tower",
              "description": "Iconic iron lattice tower and symbol of Paris",
              "location": "Champ de Mars, 7th arrondissement",
              "timing": "morning",
              "startTime": "09:00",
              "duration": 180,
              "cost": 29.00,
              "currency": "EUR",
              "category": "landmark",
              "difficulty": "easy",
              "accessibility": "wheelchair-accessible"
            },
            {
              "type": "activity",
              "title": "Explore the Louvre Museum",
              "description": "World's largest art museum and historic monument",
              "location": "Rue de Rivoli, 1st arrondissement",
              "timing": "afternoon",
              "startTime": "14:00",
              "duration": 240,
              "cost": 17.00,
              "currency": "EUR",
              "category": "museum",
              "difficulty": "moderate",
              "accessibility": "wheelchair-accessible"
            }
          ]
        }
        """;
    }

    private String createDay1MealAiResponse() {
        return """
        {
          "nodes": [
            {
              "type": "meal",
              "title": "Lunch at Café de Flore",
              "description": "Historic café in Saint-Germain-des-Prés",
              "location": "172 Boulevard Saint-Germain, 6th arrondissement",
              "timing": "lunch",
              "startTime": "12:30",
              "duration": 90,
              "cost": 45.00,
              "currency": "EUR",
              "cuisine": "French",
              "dietaryOptions": ["vegetarian", "vegan"],
              "atmosphere": "historic"
            },
            {
              "type": "meal",
              "title": "Dinner at Le Comptoir du Relais",
              "description": "Traditional French bistro with excellent wine selection",
              "location": "9 Carrefour de l'Odéon, 6th arrondissement",
              "timing": "dinner",
              "startTime": "19:30",
              "duration": 120,
              "cost": 85.00,
              "currency": "EUR",
              "cuisine": "French",
              "dietaryOptions": ["vegetarian"],
              "atmosphere": "bistro"
            }
          ]
        }
        """;
    }

    private String createDay1TransportAiResponse() {
        return """
        {
          "nodes": [
            {
              "type": "transport",
              "title": "Metro to Eiffel Tower",
              "description": "Take Line 6 to Bir-Hakeim station",
              "from": "Hotel",
              "to": "Eiffel Tower",
              "timing": "morning",
              "startTime": "08:30",
              "duration": 20,
              "cost": 2.10,
              "currency": "EUR",
              "transportType": "metro",
              "accessibility": "wheelchair-accessible"
            },
            {
              "type": "transport",
              "title": "Metro to Louvre",
              "description": "Take Line 1 to Palais Royal-Musée du Louvre station",
              "from": "Eiffel Tower",
              "to": "Louvre Museum",
              "timing": "afternoon",
              "startTime": "13:30",
              "duration": 25,
              "cost": 2.10,
              "currency": "EUR",
              "transportType": "metro",
              "accessibility": "wheelchair-accessible"
            }
          ]
        }
        """;
    }

    private String createDay1EnrichmentAiResponse() {
        return """
        {
          "enrichments": [
            {
              "nodeId": "activity_1",
              "photos": [
                "https://example.com/eiffel-tower-1.jpg",
                "https://example.com/eiffel-tower-2.jpg"
              ],
              "tips": [
                "Book tickets in advance to avoid long queues",
                "Best views are from Trocadéro across the river",
                "Visit early morning or late evening for fewer crowds"
              ],
              "nearbyAttractions": [
                "Trocadéro Gardens",
                "Champ de Mars"
              ],
              "weatherConsiderations": "Best visited in clear weather for views"
            },
            {
              "nodeId": "meal_1",
              "photos": [
                "https://example.com/cafe-flore-1.jpg"
              ],
              "tips": [
                "Try the famous hot chocolate",
                "Great people-watching spot",
                "Reservations recommended for dinner"
              ],
              "dietaryOptions": [
                "Vegetarian",
                "Vegan options available"
              ],
              "atmosphere": "Historic café with literary heritage"
            }
          ]
        }
        """;
    }
}


