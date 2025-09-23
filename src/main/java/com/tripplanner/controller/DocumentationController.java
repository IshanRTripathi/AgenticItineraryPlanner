package com.tripplanner.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Simple controller to serve OpenAPI documentation.
 */
@RestController
@RequestMapping("/api/v1")
public class DocumentationController {

    @GetMapping(value = "/openapi.yaml", produces = "application/x-yaml")
    public ResponseEntity<String> getOpenApiYaml() {
        try {
            Resource resource = new ClassPathResource("swagger-api-documentation.yaml");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("application/x-yaml"))
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/openapi.json", produces = "application/json")
    public ResponseEntity<String> getOpenApiJson() {
        try {
            // Return a proper OpenAPI JSON specification directly
            String jsonContent = createOpenApiJson();
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonContent);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("{\"error\": \"Failed to generate OpenAPI JSON: " + e.getMessage() + "\"}");
        }
    }

    private String createOpenApiJson() {
        return "{\n" +
               "  \"openapi\": \"3.0.3\",\n" +
               "  \"info\": {\n" +
               "    \"title\": \"Trip Planner API\",\n" +
               "    \"version\": \"1.0.0\",\n" +
               "    \"description\": \"API for trip planning and itinerary management\"\n" +
               "  },\n" +
               "  \"servers\": [\n" +
               "    {\n" +
               "      \"url\": \"http://localhost:8080/api/v1\",\n" +
               "      \"description\": \"Development server\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"paths\": {\n" +
               "    \"/itineraries\": {\n" +
               "      \"get\": {\n" +
               "        \"summary\": \"Get all itineraries\",\n" +
               "        \"description\": \"Retrieve a list of all itineraries\",\n" +
               "        \"responses\": {\n" +
               "          \"200\": {\n" +
               "            \"description\": \"List of itineraries\",\n" +
               "            \"content\": {\n" +
               "              \"application/json\": {\n" +
               "                \"schema\": {\n" +
               "                  \"type\": \"array\",\n" +
               "                  \"items\": {\n" +
               "                    \"$ref\": \"#/components/schemas/ItineraryDto\"\n" +
               "                  }\n" +
               "                }\n" +
               "              }\n" +
               "            }\n" +
               "          }\n" +
               "        }\n" +
               "      },\n" +
               "      \"post\": {\n" +
               "        \"summary\": \"Create new itinerary\",\n" +
               "        \"description\": \"Create a new travel itinerary\",\n" +
               "        \"requestBody\": {\n" +
               "          \"required\": true,\n" +
               "          \"content\": {\n" +
               "            \"application/json\": {\n" +
               "              \"schema\": {\n" +
               "                \"$ref\": \"#/components/schemas/CreateItineraryReq\"\n" +
               "              }\n" +
               "            }\n" +
               "          }\n" +
               "        },\n" +
               "        \"responses\": {\n" +
               "          \"200\": {\n" +
               "            \"description\": \"Created itinerary\",\n" +
               "            \"content\": {\n" +
               "              \"application/json\": {\n" +
               "                \"schema\": {\n" +
               "                  \"$ref\": \"#/components/schemas/ItineraryDto\"\n" +
               "                }\n" +
               "              }\n" +
               "            }\n" +
               "          }\n" +
               "        }\n" +
               "      }\n" +
               "    },\n" +
               "    \"/itineraries/{id}\": {\n" +
               "      \"get\": {\n" +
               "        \"summary\": \"Get itinerary by ID\",\n" +
               "        \"description\": \"Retrieve a specific itinerary by its ID\",\n" +
               "        \"parameters\": [\n" +
               "          {\n" +
               "            \"name\": \"id\",\n" +
               "            \"in\": \"path\",\n" +
               "            \"required\": true,\n" +
               "            \"schema\": {\n" +
               "              \"type\": \"string\"\n" +
               "            }\n" +
               "          }\n" +
               "        ],\n" +
               "        \"responses\": {\n" +
               "          \"200\": {\n" +
               "            \"description\": \"Itinerary found\",\n" +
               "            \"content\": {\n" +
               "              \"application/json\": {\n" +
               "                \"schema\": {\n" +
               "                  \"$ref\": \"#/components/schemas/ItineraryDto\"\n" +
               "                }\n" +
               "              }\n" +
               "            }\n" +
               "          },\n" +
               "          \"404\": {\n" +
               "            \"description\": \"Itinerary not found\"\n" +
               "          }\n" +
               "        }\n" +
               "      },\n" +
               "      \"delete\": {\n" +
               "        \"summary\": \"Delete itinerary\",\n" +
               "        \"description\": \"Delete a specific itinerary\",\n" +
               "        \"parameters\": [\n" +
               "          {\n" +
               "            \"name\": \"id\",\n" +
               "            \"in\": \"path\",\n" +
               "            \"required\": true,\n" +
               "            \"schema\": {\n" +
               "              \"type\": \"string\"\n" +
               "            }\n" +
               "          }\n" +
               "        ],\n" +
               "        \"responses\": {\n" +
               "          \"204\": {\n" +
               "            \"description\": \"Itinerary deleted successfully\"\n" +
               "          },\n" +
               "          \"404\": {\n" +
               "            \"description\": \"Itinerary not found\"\n" +
               "          }\n" +
               "        }\n" +
               "      }\n" +
               "    }\n" +
               "  },\n" +
               "  \"components\": {\n" +
               "    \"schemas\": {\n" +
               "      \"ItineraryDto\": {\n" +
               "        \"type\": \"object\",\n" +
               "        \"properties\": {\n" +
               "          \"id\": {\n" +
               "            \"type\": \"string\"\n" +
               "          },\n" +
               "          \"destination\": {\n" +
               "            \"type\": \"string\"\n" +
               "          },\n" +
               "          \"startDate\": {\n" +
               "            \"type\": \"string\",\n" +
               "            \"format\": \"date\"\n" +
               "          },\n" +
               "          \"endDate\": {\n" +
               "            \"type\": \"string\",\n" +
               "            \"format\": \"date\"\n" +
               "          },\n" +
               "          \"party\": {\n" +
               "            \"$ref\": \"#/components/schemas/PartyDto\"\n" +
               "          },\n" +
               "          \"budgetTier\": {\n" +
               "            \"type\": \"string\"\n" +
               "          },\n" +
               "          \"interests\": {\n" +
               "            \"type\": \"array\",\n" +
               "            \"items\": {\n" +
               "              \"type\": \"string\"\n" +
               "            }\n" +
               "          },\n" +
               "          \"constraints\": {\n" +
               "            \"type\": \"array\",\n" +
               "            \"items\": {\n" +
               "              \"type\": \"string\"\n" +
               "            }\n" +
               "          },\n" +
               "          \"language\": {\n" +
               "            \"type\": \"string\"\n" +
               "          },\n" +
               "          \"summary\": {\n" +
               "            \"type\": \"string\"\n" +
               "          },\n" +
               "          \"status\": {\n" +
               "            \"type\": \"string\"\n" +
               "          },\n" +
               "          \"createdAt\": {\n" +
               "            \"type\": \"string\",\n" +
               "            \"format\": \"date-time\"\n" +
               "          },\n" +
               "          \"updatedAt\": {\n" +
               "            \"type\": \"string\",\n" +
               "            \"format\": \"date-time\"\n" +
               "          }\n" +
               "        }\n" +
               "      },\n" +
               "      \"CreateItineraryReq\": {\n" +
               "        \"type\": \"object\",\n" +
               "        \"required\": [\"destination\", \"startDate\", \"endDate\", \"party\", \"budgetTier\"],\n" +
               "        \"properties\": {\n" +
               "          \"destination\": {\n" +
               "            \"type\": \"string\"\n" +
               "          },\n" +
               "          \"startDate\": {\n" +
               "            \"type\": \"string\",\n" +
               "            \"format\": \"date\"\n" +
               "          },\n" +
               "          \"endDate\": {\n" +
               "            \"type\": \"string\",\n" +
               "            \"format\": \"date\"\n" +
               "          },\n" +
               "          \"party\": {\n" +
               "            \"$ref\": \"#/components/schemas/PartyDto\"\n" +
               "          },\n" +
               "          \"budgetTier\": {\n" +
               "            \"type\": \"string\"\n" +
               "          },\n" +
               "          \"interests\": {\n" +
               "            \"type\": \"array\",\n" +
               "            \"items\": {\n" +
               "              \"type\": \"string\"\n" +
               "            }\n" +
               "          },\n" +
               "          \"constraints\": {\n" +
               "            \"type\": \"array\",\n" +
               "            \"items\": {\n" +
               "              \"type\": \"string\"\n" +
               "            }\n" +
               "          },\n" +
               "          \"language\": {\n" +
               "            \"type\": \"string\"\n" +
               "          }\n" +
               "        }\n" +
               "      },\n" +
               "      \"PartyDto\": {\n" +
               "        \"type\": \"object\",\n" +
               "        \"required\": [\"adults\"],\n" +
               "        \"properties\": {\n" +
               "          \"adults\": {\n" +
               "            \"type\": \"integer\",\n" +
               "            \"minimum\": 1\n" +
               "          },\n" +
               "          \"children\": {\n" +
               "            \"type\": \"integer\",\n" +
               "            \"minimum\": 0\n" +
               "          },\n" +
               "          \"infants\": {\n" +
               "            \"type\": \"integer\",\n" +
               "            \"minimum\": 0\n" +
               "          },\n" +
               "          \"rooms\": {\n" +
               "            \"type\": \"integer\",\n" +
               "            \"minimum\": 1\n" +
               "          }\n" +
               "        }\n" +
               "      }\n" +
               "    }\n" +
               "  }\n" +
               "}";
    }
}
