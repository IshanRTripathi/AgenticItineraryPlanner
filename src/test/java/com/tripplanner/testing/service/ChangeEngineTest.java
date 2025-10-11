package com.tripplanner.testing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.service.*;
import com.tripplanner.testing.BaseServiceTest;
import com.tripplanner.testing.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Atomic tests for ChangeEngine with mocked dependencies.
 */
class ChangeEngineTest extends BaseServiceTest {
    
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
            mockNodeIdGenerator
        );
    }
    
    @Override
    protected void setupSpecificMocks() {
        // Setup TraceManager to execute the lambda function directly
        lenient().doNothing().when(mockTraceManager).setItineraryContext(anyString());
        try {
            lenient().when(mockTraceManager.executeTraced(anyString(), any(TraceManager.TracedOperation.class)))
                    .thenAnswer(invocation -> {
                        TraceManager.TracedOperation<?> operation = invocation.getArgument(1);
                        try {
                            return operation.execute();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (Exception e) {
            // This should not happen in test setup
            throw new RuntimeException("Failed to setup TraceManager mock", e);
        }
        
        // Setup ConflictResolver mocks with lenient stubbing
        ConflictResolver.ConflictDetectionResult noConflictsResult = mock(ConflictResolver.ConflictDetectionResult.class);
        lenient().when(noConflictsResult.hasConflicts()).thenReturn(false);
        lenient().when(noConflictsResult.getConflicts()).thenReturn(java.util.Collections.emptyList());
        
        lenient().when(mockConflictResolver.detectConflicts(any(NormalizedItinerary.class), any(ChangeSet.class)))
                .thenReturn(noConflictsResult);
        
        // Setup other common mocks with lenient stubbing
        // Note: LockManager and IdempotencyManager methods have different signatures
        // We'll mock them in individual tests as needed
    }
    
    @Test
    @DisplayName("Should propose changes without persisting to database")
    void shouldProposeChangesWithoutPersistingToDatabase() throws Exception {
        // Given
        String itineraryId = "it_bali_luxury_001";
        NormalizedItinerary currentItinerary = testDataFactory.createBaliLuxuryItinerary();
        ChangeSet changeSet = createInsertNodeChangeSet();
        
        when(mockItineraryJsonService.getItinerary(itineraryId))
                .thenReturn(Optional.of(currentItinerary));
        
        // When
        ChangeEngine.ProposeResult result = changeEngine.propose(itineraryId, changeSet);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProposed()).isNotNull();
        assertThat(result.getProposed().getVersion()).isEqualTo(currentItinerary.getVersion() + 1);
        assertThat(result.getDiff()).isNotNull();
        assertThat(result.getPreviewVersion()).isEqualTo(currentItinerary.getVersion() + 1);
        
        // Verify no persistence operations were called
        verify(mockItineraryJsonService).getItinerary(itineraryId);
        verify(mockItineraryJsonService, never()).updateItinerary(any());
        verify(mockRevisionService, never()).saveRevision(anyString(), any());
        
        logger.info("Propose changes test passed");
    }
    
    @Test
    @DisplayName("Should throw exception when proposing changes for non-existent itinerary")
    void shouldThrowExceptionWhenProposingChangesForNonExistentItinerary() {
        // Given
        String itineraryId = "non-existent-itinerary";
        ChangeSet changeSet = createInsertNodeChangeSet();
        
        when(mockItineraryJsonService.getItinerary(itineraryId))
                .thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> changeEngine.propose(itineraryId, changeSet))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to propose changes");
        
        verify(mockItineraryJsonService).getItinerary(itineraryId);
        
        logger.info("Propose changes for non-existent itinerary test passed");
    }
    
    @Test
    @DisplayName("Should apply changes and persist to database")
    void shouldApplyChangesAndPersistToDatabase() {
        // Given
        String itineraryId = "it_bali_luxury_001";
        NormalizedItinerary currentItinerary = testDataFactory.createBaliLuxuryItinerary();
        ChangeSet changeSet = createInsertNodeChangeSet();
        
        when(mockItineraryJsonService.getItinerary(itineraryId))
                .thenReturn(Optional.of(currentItinerary));
        doNothing().when(mockRevisionService).saveRevision(anyString(), any());
        when(mockItineraryJsonService.updateItinerary(any())).thenReturn(null);
        
        // When
        ChangeEngine.ApplyResult result = changeEngine.apply(itineraryId, changeSet);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getToVersion()).isEqualTo(currentItinerary.getVersion() + 1);
        assertThat(result.getDiff()).isNotNull();
        assertThat(result.getDiff().getAdded()).isNotEmpty();
        
        verify(mockItineraryJsonService).getItinerary(itineraryId);
        verify(mockRevisionService).saveRevision(eq(itineraryId), any(RevisionRecord.class));
        verify(mockItineraryJsonService).updateItinerary(argThat(itinerary -> 
            itinerary.getVersion() == currentItinerary.getVersion() + 1
        ));
        
        logger.info("Apply changes test passed");
    }
    
    @Test
    @DisplayName("Should handle idempotent operations")
    void shouldHandleIdempotentOperations() {
        // Given
        String itineraryId = "it_bali_luxury_001";
        String idempotencyKey = "idempotent-key-123";
        ChangeSet changeSet = createInsertNodeChangeSet();
        changeSet.setIdempotencyKey(idempotencyKey);
        
        ChangeEngine.ApplyResult cachedResult = new ChangeEngine.ApplyResult(2, new ItineraryDiff());
        IdempotencyManager.IdempotencyRecord existingRecord = 
                new IdempotencyManager.IdempotencyRecord(idempotencyKey, cachedResult, "change_application", 
                    java.time.Instant.now(), java.time.Instant.now().plusSeconds(3600));
        
        when(mockIdempotencyManager.isValidIdempotencyKey(idempotencyKey)).thenReturn(true);
        when(mockIdempotencyManager.getExistingOperation(idempotencyKey))
                .thenReturn(Optional.of(existingRecord));
        
        // When
        ChangeEngine.ApplyResult result = changeEngine.apply(itineraryId, changeSet);
        
        // Then
        assertThat(result).isEqualTo(cachedResult);
        
        verify(mockIdempotencyManager).isValidIdempotencyKey(idempotencyKey);
        verify(mockIdempotencyManager).getExistingOperation(idempotencyKey);
        verifyNoInteractions(mockItineraryJsonService);
        
        logger.info("Idempotent operations test passed");
    }
    
    @Test
    @DisplayName("Should validate version when baseVersion is specified")
    void shouldValidateVersionWhenBaseVersionIsSpecified() {
        // Given
        String itineraryId = "it_bali_luxury_001";
        NormalizedItinerary currentItinerary = testDataFactory.createBaliLuxuryItinerary();
        currentItinerary.setVersion(5);
        
        ChangeSet changeSet = createInsertNodeChangeSet();
        changeSet.setBaseVersion(3); // Different from current version
        
        // Mock ConflictResolver to detect conflicts for version mismatch
        ConflictResolver.ConflictDetectionResult conflictsResult = mock(ConflictResolver.ConflictDetectionResult.class);
        when(conflictsResult.hasConflicts()).thenReturn(true);
        when(conflictsResult.getConflicts()).thenReturn(java.util.Collections.singletonList(
            mock(ConflictResolver.Conflict.class)
        ));
        
        ConflictResolver.ConflictResolutionResult resolutionResult = mock(ConflictResolver.ConflictResolutionResult.class);
        when(resolutionResult.isFullyResolved()).thenReturn(false);
        
        when(mockConflictResolver.detectConflicts(any(NormalizedItinerary.class), any(ChangeSet.class)))
                .thenReturn(conflictsResult);
        when(mockConflictResolver.attemptAutoResolution(any(NormalizedItinerary.class), any(ChangeSet.class), any()))
                .thenReturn(resolutionResult);
        
        when(mockItineraryJsonService.getItinerary(itineraryId))
                .thenReturn(Optional.of(currentItinerary));
        
        // When/Then
        assertThatThrownBy(() -> changeEngine.apply(itineraryId, changeSet))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to apply changes");
        
        verify(mockItineraryJsonService).getItinerary(itineraryId);
        verify(mockConflictResolver).detectConflicts(any(NormalizedItinerary.class), any(ChangeSet.class));
        
        logger.info("Version validation test passed");
    }
    
    @Test
    @DisplayName("Should skip version bump for no-op changes")
    void shouldSkipVersionBumpForNoOpChanges() {
        // Given
        String itineraryId = "it_bali_luxury_001";
        NormalizedItinerary currentItinerary = testDataFactory.createBaliLuxuryItinerary();
        ChangeSet emptyChangeSet = createEmptyChangeSet();
        
        when(mockItineraryJsonService.getItinerary(itineraryId))
                .thenReturn(Optional.of(currentItinerary));
        
        // When
        ChangeEngine.ApplyResult result = changeEngine.apply(itineraryId, emptyChangeSet);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getToVersion()).isEqualTo(currentItinerary.getVersion()); // No version bump
        assertThat(result.getDiff()).isNotNull();
        assertThat(result.getDiff().getAdded()).isEmpty();
        assertThat(result.getDiff().getRemoved()).isEmpty();
        assertThat(result.getDiff().getUpdated()).isEmpty();
        
        verify(mockItineraryJsonService).getItinerary(itineraryId);
        verify(mockRevisionService, never()).saveRevision(anyString(), any());
        verify(mockItineraryJsonService, never()).updateItinerary(any());
        
        logger.info("No-op changes test passed");
    }
    
    @Test
    @DisplayName("Should undo changes by restoring from revision")
    void shouldUndoChangesByRestoringFromRevision() {
        // Given
        String itineraryId = "it_bali_luxury_001";
        Integer toVersion = 3;
        
        NormalizedItinerary currentItinerary = testDataFactory.createBaliLuxuryItinerary();
        currentItinerary.setVersion(5);
        
        NormalizedItinerary revisionItinerary = testDataFactory.createBaliLuxuryItinerary();
        revisionItinerary.setVersion(3);
        
        when(mockItineraryJsonService.getRevision(itineraryId, toVersion))
                .thenReturn(Optional.of(revisionItinerary));
        when(mockItineraryJsonService.getItinerary(itineraryId))
                .thenReturn(Optional.of(currentItinerary));
        when(mockItineraryJsonService.updateItinerary(any())).thenReturn(null);
        
        // When
        ChangeEngine.UndoResult result = changeEngine.undo(itineraryId, toVersion);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getToVersion()).isEqualTo(3);
        assertThat(result.getDiff()).isNotNull();
        
        verify(mockItineraryJsonService).getRevision(itineraryId, toVersion);
        verify(mockItineraryJsonService).getItinerary(itineraryId);
        verify(mockItineraryJsonService).updateItinerary(revisionItinerary);
        
        logger.info("Undo changes test passed");
    }
    
    @Test
    @DisplayName("Should throw exception when undoing to non-existent revision")
    void shouldThrowExceptionWhenUndoingToNonExistentRevision() {
        // Given
        String itineraryId = "it_bali_luxury_001";
        Integer toVersion = 999;
        
        when(mockItineraryJsonService.getRevision(itineraryId, toVersion))
                .thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> changeEngine.undo(itineraryId, toVersion))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to undo changes");
        
        verify(mockItineraryJsonService).getRevision(itineraryId, toVersion);
        
        logger.info("Undo to non-existent revision test passed");
    }
    
    @Test
    @DisplayName("Should handle revision save failure during apply")
    void shouldHandleRevisionSaveFailureDuringApply() {
        // Given
        String itineraryId = "it_bali_luxury_001";
        NormalizedItinerary currentItinerary = testDataFactory.createBaliLuxuryItinerary();
        ChangeSet changeSet = createInsertNodeChangeSet();
        
        when(mockItineraryJsonService.getItinerary(itineraryId))
                .thenReturn(Optional.of(currentItinerary));
        doThrow(new RuntimeException("Revision save failed"))
                .when(mockRevisionService).saveRevision(anyString(), any());
        
        // When/Then
        assertThatThrownBy(() -> changeEngine.apply(itineraryId, changeSet))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to apply changes");
        
        verify(mockItineraryJsonService).getItinerary(itineraryId);
        verify(mockRevisionService).saveRevision(eq(itineraryId), any());
        verify(mockItineraryJsonService, never()).updateItinerary(any());
        
        logger.info("Revision save failure test passed");
    }
    
    @Test
    @DisplayName("Should store result in idempotency manager after successful apply")
    void shouldStoreResultInIdempotencyManagerAfterSuccessfulApply() {
        // Given
        String itineraryId = "it_bali_luxury_001";
        String idempotencyKey = "idempotent-key-456";
        NormalizedItinerary currentItinerary = testDataFactory.createBaliLuxuryItinerary();
        ChangeSet changeSet = createInsertNodeChangeSet();
        changeSet.setIdempotencyKey(idempotencyKey);
        
        when(mockItineraryJsonService.getItinerary(itineraryId))
                .thenReturn(Optional.of(currentItinerary));
        when(mockIdempotencyManager.isValidIdempotencyKey(idempotencyKey)).thenReturn(true);
        when(mockIdempotencyManager.getExistingOperation(idempotencyKey))
                .thenReturn(Optional.empty());
        doNothing().when(mockRevisionService).saveRevision(anyString(), any());
        when(mockItineraryJsonService.updateItinerary(any())).thenReturn(null);
        doNothing().when(mockIdempotencyManager).storeOperationResult(anyString(), any(), anyString());
        
        // When
        ChangeEngine.ApplyResult result = changeEngine.apply(itineraryId, changeSet);
        
        // Then
        assertThat(result).isNotNull();
        
        verify(mockIdempotencyManager).storeOperationResult(
            eq(idempotencyKey),
            eq(result),
            eq("change_application")
        );
        
        logger.info("Idempotency result storage test passed");
    }
    
    // Helper methods to create test data
    
    private ChangeSet createInsertNodeChangeSet() {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setScope("day");
        changeSet.setDay(1);
        
        ChangeOperation insertOp = new ChangeOperation();
        insertOp.setOp("insert");
        insertOp.setAfter("existing-node-id");
        
        NormalizedNode newNode = new NormalizedNode("new-node-123", "restaurant", "New Restaurant");
        NodeLocation location = new NodeLocation();
        location.setName("New Restaurant");
        location.setAddress("123 New Street, Ubud");
        location.setCoordinates(new Coordinates(-8.7467, 115.1671));
        newNode.setLocation(location);
        
        NodeCost cost = new NodeCost(75.0, "USD");
        newNode.setCost(cost);
        
        insertOp.setNode(newNode);
        
        changeSet.setOps(Arrays.asList(insertOp));
        
        ChangePreferences preferences = new ChangePreferences();
        preferences.setUserFirst(true);
        preferences.setAutoApply(false);
        changeSet.setPreferences(preferences);
        
        return changeSet;
    }
    
    private ChangeSet createEmptyChangeSet() {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setScope("day");
        changeSet.setDay(1);
        changeSet.setOps(Arrays.asList()); // Empty operations
        
        ChangePreferences preferences = new ChangePreferences();
        preferences.setUserFirst(true);
        preferences.setAutoApply(false);
        changeSet.setPreferences(preferences);
        
        return changeSet;
    }
}