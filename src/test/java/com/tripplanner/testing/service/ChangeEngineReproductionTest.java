package com.tripplanner.testing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.service.*;
import com.tripplanner.service.analytics.TraceManager;
import com.tripplanner.service.utilities.IdempotencyManager;
import com.tripplanner.service.utilities.LockManager;
import com.tripplanner.service.utilities.NodeIdGenerator;
import com.tripplanner.testing.BaseServiceTest;
import com.tripplanner.testing.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ChangeEngineReproductionTest extends BaseServiceTest {

    @Mock
    private ItineraryJsonService mockItineraryJsonService;
    @Mock
    private UserDataService mockUserDataService;
    @Mock
    private RevisionService mockRevisionService;
    @Mock
    private ConflictResolver mockConflictResolver;
    @Mock
    private LockManager mockLockManager;
    @Mock
    private IdempotencyManager mockIdempotencyManager;
    @Mock
    private TraceManager mockTraceManager;
    @Mock
    private NodeIdGenerator mockNodeIdGenerator;
    @Mock
    private EnrichmentService mockEnrichmentService;
    @Mock
    private ItineraryValidator mockItineraryValidator;

    private ChangeEngine changeEngine;
    private TestDataFactory testDataFactory;
    private ObjectMapper objectMapper;

    @BeforeEach
    protected void setUp() {
        super.setUp();
        objectMapper = new ObjectMapper();
        testDataFactory = new TestDataFactory(objectMapper);

        changeEngine = new ChangeEngine(
                mockItineraryJsonService,
                mockUserDataService,
                objectMapper,
                mockRevisionService,
                mockConflictResolver,
                mockLockManager,
                mockIdempotencyManager,
                mockTraceManager,
                mockNodeIdGenerator,
                mockEnrichmentService,
                mockItineraryValidator);
    }

    @Override
    protected void setupSpecificMocks() {
        lenient().doNothing().when(mockTraceManager).setItineraryContext(anyString());
        try {
            lenient().when(mockTraceManager.executeTraced(anyString(), any(TraceManager.TracedOperation.class)))
                    .thenAnswer(invocation -> {
                        TraceManager.TracedOperation<?> operation = invocation.getArgument(1);
                        return operation.execute();
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should preserve node ID on replace and allow subsequent insert referencing it")
    void shouldPreserveNodeIdOnReplaceAndAllowSubsequentInsert() {
        // Given
        String itineraryId = "test-itinerary";
        NormalizedItinerary itinerary = testDataFactory.createBaliLuxuryItinerary();

        // Get the first node ID to replace
        NormalizedNode originalNode = itinerary.getDays().get(0).getNodes().get(0);
        String targetNodeId = originalNode.getId();

        // Create ChangeSet with REPLACE followed by INSERT
        ChangeSet changeSet = new ChangeSet();
        changeSet.setScope("day");
        changeSet.setDay(1);

        // Op 1: Replace node (without specifying ID in the new node)
        ChangeOperation replaceOp = new ChangeOperation();
        replaceOp.setOp("replace");
        replaceOp.setId(targetNodeId);

        NormalizedNode replacementNode = new NormalizedNode(); // No ID set!
        replacementNode.setTitle("Replaced Node Title");
        replacementNode.setType("attraction");
        replacementNode.setLocation(new NodeLocation());
        replacementNode.getLocation().setName("New Location");
        replacementNode.getLocation().setAddress("New Address");

        replaceOp.setNode(replacementNode);

        // Op 2: Insert after the replaced node (referencing the original ID)
        ChangeOperation insertOp = new ChangeOperation();
        insertOp.setOp("insert");
        insertOp.setAfter(targetNodeId); // Referencing the ID we just replaced

        NormalizedNode newNode = new NormalizedNode();
        newNode.setId("new-inserted-node");
        newNode.setTitle("Inserted Node");
        newNode.setType("meal");
        newNode.setLocation(new NodeLocation());
        newNode.getLocation().setName("Inserted Location");
        newNode.getLocation().setAddress("Inserted Address");

        insertOp.setNode(newNode);

        changeSet.setOps(Arrays.asList(replaceOp, insertOp));

        when(mockItineraryJsonService.getItinerary(itineraryId)).thenReturn(Optional.of(itinerary));
        when(mockItineraryJsonService.updateItinerary(any())).thenReturn(null);
        when(mockItineraryValidator.validate(any())).thenReturn(new ItineraryValidator.ValidationResult(true));

        // When
        ChangeEngine.ApplyResult result = changeEngine.apply(itineraryId, changeSet);

        // Then
        // 1. Verify the replaced node has the SAME ID as original
        NormalizedNode replacedNodeInResult = result.getDiff().getAdded().stream()
                .filter(item -> item.getTitle().equals("Replaced Node Title"))
                .findFirst()
                .map(item -> {
                    // In a real scenario we'd check the itinerary, but here we check the diff or
                    // the mock call
                    // But wait, apply returns the diff.
                    // If the ID was preserved, the diff might show "updated" or "removed+added"
                    // with same ID.
                    return null;
                }).orElse(null);

        // Let's capture the updated itinerary passed to updateItinerary
        verify(mockItineraryJsonService).updateItineraryWithLock(argThat(updatedItinerary -> {
            NormalizedDay day1 = updatedItinerary.getDays().get(0);

            // Check if the node at index 0 has the original ID
            NormalizedNode nodeAt0 = day1.getNodes().get(0);
            boolean idPreserved = nodeAt0.getId().equals(targetNodeId);
            boolean titleUpdated = nodeAt0.getTitle().equals("Replaced Node Title");

            // Check if the node at index 1 is the inserted node
            NormalizedNode nodeAt1 = day1.getNodes().get(1);
            boolean insertCorrect = nodeAt1.getId().equals("new-inserted-node");

            return idPreserved && titleUpdated && insertCorrect;
        }));
    }
}
