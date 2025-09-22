package com.tripplanner.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.api.dto.*;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.SampleDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for the new normalized JSON API endpoints
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=password",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureWebMvc
@ActiveProfiles("test")
class NormalizedItineraryIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ItineraryJsonService itineraryJsonService;

    @Autowired
    private SampleDataGenerator sampleDataGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String testItineraryId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Generate sample data
        sampleDataGenerator.generateAllSamples();
        
        // Use the Barcelona sample for testing
        testItineraryId = "it_barcelona_comprehensive";
    }

    @Test
    void testGetItineraryJson() throws Exception {
        mockMvc.perform(get("/api/v1/itineraries/{id}/json", testItineraryId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.itineraryId").value(testItineraryId))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.days").isArray())
                .andExpect(jsonPath("$.days[0].dayNumber").exists())
                .andExpect(jsonPath("$.days[0].nodes").isArray());
    }

    @Test
    void testProposeChanges() throws Exception {
        // Create a change set to move a node
        ChangeSet changeSet = new ChangeSet();
        changeSet.setScope("day");
        changeSet.setDay(1);
        
        ChangeOperation operation = new ChangeOperation();
        operation.setOp("move");
        operation.setId("n_airport_arrival");
        operation.setAfter("n_hotel_checkin");
        
        changeSet.setOps(List.of(operation));
        
        // Create preferences
        ChangePreferences preferences = new ChangePreferences();
        preferences.setUserFirst(true);
        preferences.setAutoApply(false);
        preferences.setRespectLocks(true);
        changeSet.setPreferences(preferences);

        mockMvc.perform(post("/api/v1/itineraries/{id}:propose", testItineraryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeSet)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.proposed").exists())
                .andExpect(jsonPath("$.proposed.itineraryId").value(testItineraryId))
                .andExpect(jsonPath("$.diff").exists());
    }

    @Test
    void testApplyChanges() throws Exception {
        // Create an ApplyRequest with a change set
        ApplyRequest applyRequest = new ApplyRequest();
        applyRequest.setChangeSetId("test-changeset-1");
        
        ChangeSet changeSet = new ChangeSet();
        changeSet.setScope("day");
        changeSet.setDay(1);
        
        ChangeOperation operation = new ChangeOperation();
        operation.setOp("move");
        operation.setId("n_airport_arrival");
        operation.setAfter("n_hotel_checkin");
        
        changeSet.setOps(List.of(operation));
        
        ChangePreferences preferences = new ChangePreferences();
        preferences.setUserFirst(true);
        preferences.setAutoApply(false);
        preferences.setRespectLocks(true);
        changeSet.setPreferences(preferences);
        
        applyRequest.setChangeSet(changeSet);

        // Apply the change
        mockMvc.perform(post("/api/v1/itineraries/{id}:apply", testItineraryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(applyRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.toVersion").exists())
                .andExpect(jsonPath("$.diff").exists());
    }

    @Test
    void testUndoChanges() throws Exception {
        // First apply a change to create a version to undo to
        ApplyRequest applyRequest = new ApplyRequest();
        applyRequest.setChangeSetId("test-changeset-1");
        
        ChangeSet changeSet = new ChangeSet();
        changeSet.setScope("day");
        changeSet.setDay(1);
        
        ChangeOperation operation = new ChangeOperation();
        operation.setOp("move");
        operation.setId("n_airport_arrival");
        operation.setAfter("n_hotel_checkin");
        
        changeSet.setOps(List.of(operation));
        
        ChangePreferences preferences = new ChangePreferences();
        preferences.setUserFirst(true);
        preferences.setAutoApply(false);
        preferences.setRespectLocks(true);
        changeSet.setPreferences(preferences);
        
        applyRequest.setChangeSet(changeSet);

        // Apply the change first
        mockMvc.perform(post("/api/v1/itineraries/{id}:apply", testItineraryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(applyRequest)))
                .andExpect(status().isOk());

        // Now undo to version 1 (the original version)
        UndoRequest undoRequest = new UndoRequest();
        undoRequest.setToVersion(1);

        mockMvc.perform(post("/api/v1/itineraries/{id}:undo", testItineraryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(undoRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.toVersion").exists())
                .andExpect(jsonPath("$.diff").exists());
    }

    @Test
    void testProcessUserRequest() throws Exception {
        ProcessRequestRequest request = new ProcessRequestRequest();
        request.setItineraryId(testItineraryId);
        request.setUserRequest("Add a visit to Park Güell in the afternoon");

        mockMvc.perform(post("/api/v1/agents/process-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.itineraryId").value(testItineraryId))
                .andExpect(jsonPath("$.status").value("processing"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testMockBooking() throws Exception {
        MockBookingRequest request = new MockBookingRequest();
        request.setItineraryId(testItineraryId);
        request.setNodeId("n_airport_arrival");

        mockMvc.perform(post("/api/v1/book")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.itineraryId").value(testItineraryId))
                .andExpect(jsonPath("$.nodeId").value("n_airport_arrival"))
                .andExpect(jsonPath("$.bookingRef").exists())
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.message").exists());
    }

    // Inner classes to match the controller's DTOs
    public static class ApplyRequest {
        private String changeSetId;
        private ChangeSet changeSet;

        public String getChangeSetId() {
            return changeSetId;
        }

        public void setChangeSetId(String changeSetId) {
            this.changeSetId = changeSetId;
        }

        public ChangeSet getChangeSet() {
            return changeSet;
        }

        public void setChangeSet(ChangeSet changeSet) {
            this.changeSet = changeSet;
        }
    }

    public static class UndoRequest {
        private Integer toVersion;

        public Integer getToVersion() {
            return toVersion;
        }

        public void setToVersion(Integer toVersion) {
            this.toVersion = toVersion;
        }
    }

    public static class ProcessRequestRequest {
        private String itineraryId;
        private String userRequest;

        public String getItineraryId() {
            return itineraryId;
        }

        public void setItineraryId(String itineraryId) {
            this.itineraryId = itineraryId;
        }

        public String getUserRequest() {
            return userRequest;
        }

        public void setUserRequest(String userRequest) {
            this.userRequest = userRequest;
        }
    }

    public static class MockBookingRequest {
        private String itineraryId;
        private String nodeId;
        private String provider;
        private String confirmationId;

        public String getItineraryId() {
            return itineraryId;
        }

        public void setItineraryId(String itineraryId) {
            this.itineraryId = itineraryId;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getConfirmationId() {
            return confirmationId;
        }

        public void setConfirmationId(String confirmationId) {
            this.confirmationId = confirmationId;
        }
    }
}
