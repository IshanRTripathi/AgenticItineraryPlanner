package com.tripplanner.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.ChatRequest;
import com.tripplanner.dto.ChatResponse;
import com.tripplanner.controller.ChatController;
import com.tripplanner.service.OrchestratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private OrchestratorService orchestratorService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(orchestratorService)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testRouteMessage_Success() throws Exception {
        // Given
        ChatRequest request = new ChatRequest();
        request.setItineraryId("it_123");
        request.setScope("trip");
        request.setText("Move lunch to 2pm");
        request.setAutoApply(false);

        ChatResponse response = ChatResponse.success(
            "MOVE_TIME",
            "I'll move lunch to 2pm",
            null,
            null,
            false,
            2
        );

        when(orchestratorService.route(any(ChatRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/chat/route")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.intent").value("MOVE_TIME"))
                .andExpect(jsonPath("$.message").value("I'll move lunch to 2pm"))
                .andExpect(jsonPath("$.applied").value(false))
                .andExpect(jsonPath("$.toVersion").value(2));
    }

    @Test
    void testRouteMessage_Disambiguation() throws Exception {
        // Given
        ChatRequest request = new ChatRequest();
        request.setItineraryId("it_123");
        request.setScope("trip");
        request.setText("Move Sagrada");
        request.setAutoApply(false);

        ChatResponse response = ChatResponse.disambiguation(
            "MOVE_TIME",
            "Which place did you mean?",
            List.of()
        );

        when(orchestratorService.route(any(ChatRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/chat/route")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.intent").value("MOVE_TIME"))
                .andExpect(jsonPath("$.message").value("Which place did you mean?"))
                .andExpect(jsonPath("$.needsDisambiguation").value(true));
    }

    @Test
    void testRouteMessage_InvalidRequest() throws Exception {
        // Given
        ChatRequest request = new ChatRequest();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/v1/chat/route")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRouteMessage_NodeNotFound() throws Exception {
        // Given
        ChatRequest request = new ChatRequest();
        request.setItineraryId("it_123");
        request.setScope("trip");
        request.setText("Move non-existent place");
        request.setAutoApply(false);

        ChatResponse response = ChatResponse.error(
            "No matching places found",
            List.of("No nodes found matching: Move non-existent place")
        );

        when(orchestratorService.route(any(ChatRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/chat/route")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.intent").value("ERROR"))
                .andExpect(jsonPath("$.message").value("No matching places found"))
                .andExpect(jsonPath("$.warnings").isArray());
    }

    @Test
    void testRouteMessage_IntentClassification() throws Exception {
        // Given
        ChatRequest request = new ChatRequest();
        request.setItineraryId("it_123");
        request.setScope("trip");
        request.setText("What's in my itinerary?");
        request.setAutoApply(false);

        ChatResponse response = ChatResponse.explain("Your 3-day Barcelona itinerary includes...");

        when(orchestratorService.route(any(ChatRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/chat/route")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.intent").value("EXPLAIN"))
                .andExpect(jsonPath("$.message").value("Your 3-day Barcelona itinerary includes..."))
                .andExpect(jsonPath("$.applied").value(false));
    }
}
