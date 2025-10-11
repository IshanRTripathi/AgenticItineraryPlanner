# UI-Backend Integration - Implementation Tasks

## Task Overview

This document outlines the specific implementation tasks for integrating all backend features into the frontend UI. Tasks are organized by feature area and prioritized based on dependencies and user value.

**Total Estimated Effort**: 320 hours (8 weeks)
**Team Size**: 2 frontend developers
**Sprint Duration**: 2 weeks

## Phase 1: Change Management UI (Weeks 1-2)

### Epic 1: Change Preview System

- [x] 1. Implement Change Preview Modal Component




  - Create `ChangePreviewModal.tsx` component with diff viewer
  - Integrate with `apiClient.proposeChanges()` endpoint
  - Add visual diff display for added/removed/modified items
  - Implement approve/reject workflow with loading states
  - Add keyboard shortcuts (Enter to approve, Esc to cancel)
  - _Requirements: 1.1, 1.2, 1.3, 1.4_
  - _Estimated: 16 hours_




- [x] 2. Create Diff Viewer Component


  - Build `DiffViewer.tsx` for displaying changes
  - Implement side-by-side comparison view
  - Add color coding (green=added, red=removed, yellow=modified)
  - Support collapsible sections for large diffs
  - Add search/filter functionality for diffs


  - _Requirements: 1.2_
  - _Estimated: 12 hours_


- [x] 3. Integrate Change Preview with Existing Edit Flows

  - Update `DayByDayView.tsx` to use change preview
  - Update `WorkflowBuilder.tsx` to use change preview
  - Add preview mode toggle in settings
  - Implement preview caching for performance
  - _Requirements: 1.1, 1.3, 1.4_
  - _Estimated: 14 hours_

- [ ]* 3.1 Write unit tests for ChangePreviewModal



  - Test rendering with different diff types
  - Test approve/reject workflows
  - Test keyboard shortcuts
  - Test error handling
  - _Requirements: 1.1, 1.2, 1.3, 1.4_
  - _Estimated: 6 hours_

### Epic 2: Undo/Redo System

- [x] 4. Implement Undo/Redo Controls Component



  - Create `UndoRedoControls.tsx` with undo/redo buttons
  - Integrate with `apiClient.undoChanges()` endpoint
  - Add keyboard shortcuts (Ctrl+Z, Ctrl+Y)
  - Display current version number
  - Show undo/redo stack depth
  - _Requirements: 1.5_
  - _Estimated: 10 hours_

- [x] 5. Create Change History Panel Component



  - Build `ChangeHistoryPanel.tsx` for displaying history
  - Integrate with `GET /{id}/revisions` endpoint
  - Display chronological list with timestamps
  - Add user attribution for each change
  - Implement jump-to-version functionality
  - _Requirements: 1.6, 1.7_
  - _Estimated: 14 hours_

- [x] 6. Add Revision Detail View


  - Create `RevisionDetailView.tsx` component
  - Integrate with `GET /{id}/revisions/{revisionId}` endpoint
  - Display detailed change information
  - Show diff for selected revision
  - Add restore button with confirmation
  - _Requirements: 1.6, 1.7_
  - _Estimated: 12 hours_


- [ ] 6.1 Write unit tests for undo/redo functionality

  - Test undo/redo button states
  - Test keyboard shortcuts
  - Test version navigation
  - Test error handling
  - _Requirements: 1.5, 1.6, 1.7_
  - _Estimated: 6 hours_

### Epic 3: Real-Time Change Updates

- [x] 7. Enhance SSE Integration for Change Events


  - Update `SseConnectionManager` to handle change events
  - Add event handlers for patch_applied events
  - Implement optimistic UI updates
  - Add rollback on SSE error
  - _Requirements: 1.8_
  - _Estimated: 10 hours_

- [x] 8. Add Change Notification System


  - Create `ChangeNotification.tsx` component
  - Display toast notifications for changes
  - Show who made the change (for collaboration)
  - Add undo option in notification
  - _Requirements: 1.8_
  - _Estimated: 8 hours_

## Phase 2: Node Locking Controls (Weeks 2-3)

### Epic 4: Node Lock UI

- [x] 9. Implement Node Lock Toggle Component


  - Create `NodeLockToggle.tsx` component
  - Integrate with `PUT /{id}/nodes/{nodeId}/lock` endpoint
  - Add lock/unlock icon button
  - Display loading state during API call
  - Show success/error feedback
  - _Requirements: 2.1, 2.2, 2.3_
  - _Estimated: 8 hours_

- [x] 10. Add Locked Node Visual Indicators


  - Create `LockedNodeIndicator.tsx` component
  - Add lock icon overlay to locked nodes
  - Implement color coding (locked=red border)
  - Add animation on lock state change
  - Display tooltip explaining lock status
  - _Requirements: 2.4_
  - _Estimated: 6 hours_

- [x] 11. Integrate Lock Controls in Day-by-Day View


  - Add lock toggle to each node card in `DayByDayView.tsx`
  - Update node rendering to show lock status
  - Add bulk lock/unlock functionality
  - Implement lock state persistence
  - _Requirements: 2.1, 2.2, 2.3, 2.4_
  - _Estimated: 10 hours_

- [x] 12. Integrate Lock Controls in Workflow Builder


  - Add lock toggle to workflow nodes
  - Update node styling for locked nodes
  - Prevent drag/drop of locked nodes (optional)
  - Sync lock state with itinerary
  - _Requirements: 2.1, 2.2, 2.3, 2.4_
  - _Estimated: 10 hours_




- [ ]* 12.1 Write unit tests for node locking

  - Test lock/unlock toggle
  - Test visual indicators
  - Test API integration
  - Test error handling
  - _Requirements: 2.1, 2.2, 2.3, 2.4_
  - _Estimated: 6 hours_

### Epic 5: Lock Enforcement

- [x] 13. Update Agent Logic to Respect Locks


  - Modify agent execution to check lock status
  - Display warning when attempting to modify locked nodes
  - Add override option for user-initiated changes
  - Update change preview to highlight locked nodes
  - _Requirements: 2.5, 2.6_
  - _Estimated: 8 hours_


- [x] 14. Add Lock State Debugging Tools

  - Create debug panel showing all lock states
  - Integrate with `GET /{id}/lock-states` endpoint
  - Add bulk unlock for debugging
  - Display lock state in browser console
  - _Requirements: 2.7_
  - _Estimated: 4 hours_

## Phase 3: Workflow-Itinerary Bidirectional Sync (Weeks 3-5)

### Epic 6: Sync Service Implementation

- [x] 15. Create Workflow Sync Service


  - Implement `WorkflowSyncService.ts` class
  - Add node-to-component conversion logic
  - Add component-to-node conversion logic
  - Implement sync queue with debouncing
  - Add conflict detection logic
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_
  - _Estimated: 20 hours_

- [x] 16. Implement Sync Status Indicators


  - Create `SyncStatusIndicator.tsx` component
  - Display syncing/synced/error states
  - Show pending changes count
  - Add manual sync trigger button
  - Display last sync timestamp
  - _Requirements: 3.7_
  - _Estimated: 8 hours_

- [x] 17. Add Conflict Resolution UI


  - Create `ConflictResolutionModal.tsx` component
  - Display conflicting versions side-by-side
  - Add options to keep workflow/itinerary/merge
  - Implement merge logic for conflicts
  - Show conflict history
  - _Requirements: 3.8, 3.9_
  - _Estimated: 16 hours_

- [ ]* 17.1 Write unit tests for sync service

  - Test node-to-component conversion
  - Test component-to-node conversion
  - Test sync queue and debouncing
  - Test conflict detection
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_
  - _Estimated: 10 hours_

### Epic 7: Workflow Builder Integration

- [x] 18. Update Workflow Builder for Sync



  - Modify `WorkflowBuilder.tsx` to use sync service
  - Add real-time sync on node changes
  - Implement sync error handling
  - Add sync status display
  - Update node positions via `PUT /{id}/workflow`
  - _Requirements: 3.1, 3.2, 3.3, 3.7_
  - _Estimated: 16 hours_



- [ ] 19. Update Day-by-Day View for Sync
  - Modify `DayByDayView.tsx` to use sync service
  - Add real-time sync on component changes
  - Implement sync error handling
  - Add sync status display


  - _Requirements: 3.4, 3.5, 3.6, 3.7_
  - _Estimated: 16 hours_

- [ ] 20. Implement Unified State Management
  - Update `UnifiedItineraryContext.tsx` for sync
  - Add sync state to context
  - Implement sync action dispatchers
  - Add sync event listeners
  - Optimize re-renders during sync
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_
  - _Estimated: 14 hours_

- [ ] 20.1 Write integration tests for sync

  - Test workflow-to-itinerary sync
  - Test itinerary-to-workflow sync
  - Test bidirectional sync
  - Test conflict resolution
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9_
  - _Estimated: 12 hours_

## Phase 4: Agent Execution Controls (Weeks 5-6)

### Epic 8: Agent Control Panel

- [x] 21. Create Agent Control Panel Component



  - Build `AgentControlPanel.tsx` component
  - Display grid of available agents with icons
  - Show agent capabilities and descriptions
  - Add agent configuration modal
  - Integrate with agent registry
  - _Requirements: 4.1, 4.2_
  - _Estimated: 14 hours_



- [ ] 22. Implement Agent Configuration Modal
  - Create `AgentConfigModal.tsx` component
  - Display agent-specific parameters
  - Add form validation for parameters
  - Show parameter descriptions and examples
  - Save configuration preferences
  - _Requirements: 4.2_
  - _Estimated: 10 hours_

- [x] 23. Add Agent Execution Trigger


  - Integrate with `POST /{id}/agents/{agentType}/execute` endpoint
  - Add execute button with confirmation
  - Display execution start notification
  - Handle execution errors
  - Add cancel execution option
  - _Requirements: 4.3_
  - _Estimated: 8 hours_

- [ ]* 23.1 Write unit tests for agent controls

  - Test agent panel rendering
  - Test agent configuration
  - Test execution trigger
  - Test error handling
  - _Requirements: 4.1, 4.2, 4.3_
  - _Estimated: 6 hours_



### Epic 9: Agent Progress Monitoring

- [ ] 24. Create Agent Execution Progress Component
  - Build `AgentExecutionProgress.tsx` component
  - Connect to SSE for real-time updates
  - Display progress bar and percentage
  - Show current step and status
  - Add log viewer for execution details
  - _Requirements: 4.4_
  - _Estimated: 12 hours_

- [x] 25. Implement Agent Results Display


  - Create `AgentResultsPanel.tsx` component
  - Display execution results and changes
  - Show before/after comparison
  - Add apply/reject options for results

  - Display execution metrics (time, tokens, etc.)
  - _Requirements: 4.5_
  - _Estimated: 10 hours_


- [ ] 26. Add Agent Error Handling
  - Create `AgentErrorDisplay.tsx` component
  - Display detailed error messages
  - Show error context and suggestions
  - Add retry button with backoff
  - Log errors for debugging


  - _Requirements: 4.6_
  - _Estimated: 6 hours_

### Epic 10: Agent History


- [ ] 27. Create Agent History Panel
  - Build `AgentHistoryPanel.tsx` component
  - Display list of past executions
  - Show execution status, time, and results
  - Add filtering by agent type and status
  - Implement pagination for large histories
  - _Requirements: 4.7_
  - _Estimated: 12 hours_

- [x] 28. Implement Agent Execution Detail View

  - Create `AgentExecutionDetail.tsx` component
  - Display detailed execution information
  - Show full logs and timeline
  - Display changes made by agent
  - Add replay/re-execute option
  - _Requirements: 4.8_
  - _Estimated: 10 hours_

- [ ]* 28.1 Write integration tests for agent execution

  - Test agent execution flow
  - Test progress monitoring
  - Test results display
  - Test error handling
  - _Requirements: 4.3, 4.4, 4.5, 4.6_
  - _Estimated: 8 hours_

## Phase 5: Enhanced Chat Features (Weeks 6-7)

### Epic 11: Chat Preview Mode

- [x] 29. Implement Chat Preview Mode Toggle





  - Create `ChatPreviewMode.tsx` component
  - Add toggle switch in chat settings
  - Save preference to user settings

  - Display mode indicator in chat
  - _Requirements: 5.1, 5.2, 5.3_
  - _Estimated: 6 hours_

- [ ] 30. Integrate Preview with Chat Responses
  - Update `ChatInterface.tsx` to support preview mode
  - Display change preview for chat responses
  - Add approve/reject buttons in chat
  - Show preview before auto-apply
  - _Requirements: 5.1, 5.2, 5.3_
  - _Estimated: 12 hours_

- [ ] 31. Add Chat Change Confirmation
  - Create `ChatChangeConfirmation.tsx` component
  - Display summary of changes
  - Add confirmation dialog for major changes
  - Show affected nodes/days
  - _Requirements: 5.6_


  - _Estimated: 8 hours_

- [ ] 31.1 Write unit tests for chat preview

  - Test preview mode toggle
  - Test preview display
  - Test approve/reject workflow


  - Test confirmation dialogs
  - _Requirements: 5.1, 5.2, 5.3, 5.6_
  - _Estimated: 6 hours_

### Epic 12: Enhanced Disambiguation

- [ ] 32. Improve Disambiguation Panel UI
  - Update `DisambiguationPanel.tsx` component


  - Add visual previews for each option
  - Show context for disambiguation
  - Improve option descriptions
  - Add keyboard navigation
  - _Requirements: 5.4, 5.5_
  - _Estimated: 10 hours_



- [ ] 33. Add Disambiguation Context Display
  - Show why disambiguation is needed
  - Display relevant itinerary context
  - Highlight ambiguous terms
  - Add "None of these" option
  - _Requirements: 5.4, 5.5_
  - _Estimated: 8 hours_

### Epic 13: Chat History and Context

- [ ] 34. Implement Chat History Persistence
  - Integrate with `GET /{id}/chat/history` endpoint
  - Display chat history on load

  - Add search/filter for chat history
  - Show change summaries in history
  - _Requirements: 5.8_
  - _Estimated: 10 hours_

- [ ] 35. Add Chat Context Management
  - Display current chat context (day/trip)
  - Add context switching UI
  - Show context in chat messages
  - Persist context across sessions
  - _Requirements: 5.8_
  - _Estimated: 8 hours_

- [ ] 36. Improve Chat Error Handling
  - Create `ChatErrorDisplay.tsx` component
  - Display detailed error messages
  - Show suggested alternatives
  - Add retry with modified input
  - _Requirements: 5.7_
  - _Estimated: 6 hours_



- [ ] 36.1 Write integration tests for enhanced chat

  - Test preview mode workflow
  - Test disambiguation flow
  - Test chat history
  - Test error handling


  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8_
  - _Estimated: 8 hours_

## Phase 6: Revision Management UI (Weeks 7-8)

### Epic 14: Revision Timeline


- [ ] 37. Create Revision Timeline Component
  - Build `RevisionTimeline.tsx` component
  - Integrate with `GET /{id}/revisions` endpoint
  - Display vertical timeline of versions
  - Show version metadata (time, user, changes)
  - Add filtering and search
  - _Requirements: 6.1, 6.2_
  - _Estimated: 14 hours_

- [ ] 38. Implement Revision Card Component
  - Create `RevisionCard.tsx` component
  - Display version summary
  - Show change count and type
  - Add hover preview of changes
  - Include restore button
  - _Requirements: 6.2_
  - _Estimated: 8 hours_



- [ ] 39. Add Revision Navigation
  - Implement version navigation controls
  - Add next/previous version buttons
  - Show current version indicator
  - Add jump-to-version input
  - _Requirements: 6.4_
  - _Estimated: 6 hours_



- [ ] 39.1 Write unit tests for revision timeline

  - Test timeline rendering
  - Test version navigation
  - Test filtering and search

  - Test restore functionality
  - _Requirements: 6.1, 6.2, 6.4_
  - _Estimated: 6 hours_

### Epic 15: Revision Diff Viewer

- [ ] 40. Create Revision Diff Viewer Component
  - Build `RevisionDiffViewer.tsx` component
  - Integrate with `GET /{id}/revisions/{revisionId}` endpoint
  - Display side-by-side comparison
  - Highlight changes with color coding


  - Add expand/collapse for sections
  - _Requirements: 6.3, 6.6_
  - _Estimated: 14 hours_

- [ ] 41. Implement Version Comparison
  - Add compare mode for two versions


  - Display differences between versions
  - Show timeline of changes between versions
  - Add export comparison option
  - _Requirements: 6.6_
  - _Estimated: 10 hours_

- [ ] 42. Add Revision Restore Functionality
  - Integrate with `POST /{id}/revisions/{revisionId}/rollback` endpoint
  - Add confirmation dialog for restore
  - Display preview of restore changes
  - Show success/error feedback
  - Update UI after restore
  - _Requirements: 6.4, 6.5_
  - _Estimated: 8 hours_

- [ ] 42.1 Write integration tests for revision management

  - Test revision timeline
  - Test diff viewer
  - Test version comparison
  - Test restore functionality
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_
  - _Estimated: 8 hours_

### Epic 16: Revision History Integration

- [ ] 43. Add Revision History Access Points
  - Add "View History" button in main UI
  - Add history icon in toolbar
  - Add keyboard shortcut (Ctrl+H)
  - Display history panel as sidebar
  - _Requirements: 6.1_
  - _Estimated: 6 hours_



- [ ] 44. Implement Empty State for History
  - Create empty state component
  - Display helpful message
  - Show example of how history works
  - Add link to documentation
  - _Requirements: 6.7_
  - _Estimated: 4 hours_

## Phase 7: Export and Sharing Enhancements (Week 8)

### Epic 17: Export Functionality

- [x] 45. Enhance PDF Export


  - Update PDF export button


  - Integrate with `GET /export/itineraries/{id}/pdf` endpoint
  - Add loading state during generation
  - Display success notification



  - Handle export errors gracefully
  - _Requirements: 7.1, 7.6_
  - _Estimated: 6 hours_

- [ ] 46. Add Export Options Modal
  - Create `ExportOptionsModal.tsx` component
  - Add options for PDF customization
  - Include/exclude sections selector
  - Add format options (A4, Letter, etc.)
  - Save export preferences
  - _Requirements: 7.1_
  - _Estimated: 8 hours_

### Epic 18: Sharing Functionality

- [x] 47. Implement Share Modal


  - Create `ShareModal.tsx` component
  - Integrate with `POST /itineraries/{id}:share` endpoint
  - Display shareable link
  - Add copy-to-clipboard button
  - Show share settings (public/private)
  - _Requirements: 7.2, 7.3_
  - _Estimated: 8 hours_

- [ ] 48. Add Email Sharing
  - Create `EmailShareForm.tsx` component
  - Integrate with `POST /export/email/share` endpoint
  - Add recipient email input
  - Add personal message field
  - Display send confirmation
  - _Requirements: 7.4, 7.5_
  - _Estimated: 8 hours_

- [ ] 49. Implement Share Link Preview
  - Create public share view
  - Display itinerary in read-only mode
  - Add "Create Your Own" CTA
  - Handle invalid share links
  - _Requirements: 7.7_
  - _Estimated: 10 hours_

- [x] 49.1 Write integration tests for export and sharing


  - Test PDF export
  - Test share link generation
  - Test email sharing
  - Test public share view
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7_
  - _Estimated: 6 hours_


## Phase 8: Booking Integration (Week 8)

### Epic 19: Booking UI



- [ ] 50. Create Booking Modal Component
  - Build `BookingModal.tsx` component


  - Display booking options and pricing
  - Show node details for booking
  - Add booking form fields
  - Integrate with `POST /book` endpoint
  - _Requirements: 8.1, 8.2_
  - _Estimated: 12 hours_

- [ ] 51. Add Booking Confirmation Display
  - Create `BookingConfirmation.tsx` component
  - Display booking reference number
  - Show booking details
  - Add calendar export option
  - Send confirmation email
  - _Requirements: 8.3_
  - _Estimated: 8 hours_

- [ ] 52. Implement Booked Node Indicators
  - Add "Booked" badge to nodes
  - Display booking reference on node
  - Show booking status (confirmed/pending)
  - Add view booking details option
  - _Requirements: 8.4, 8.6_
  - _Estimated: 6 hours_

- [x] 53. Add Booking Error Handling

  - Display booking errors clearly
  - Show retry option
  - Add alternative booking suggestions
  - Log booking failures
  - _Requirements: 8.5_
  - _Estimated: 4 hours_

- [x] 54. Implement Booking Cancellation

  - Add cancel booking button
  - Integrate with `POST /bookings/{bookingId}:cancel` endpoint
  - Display cancellation confirmation
  - Update node status after cancellation
  - _Requirements: 8.7_
  - _Estimated: 6 hours_

- [ ]* 54.1 Write integration tests for booking

  - Test booking flow
  - Test booking confirmation
  - Test booked node indicators
  - Test booking cancellation
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7_

  - _Estimated: 6 hours_

## Phase 9: Performance Optimization and Polish (Week 8)

### Epic 20: Performance Optimization






- [x] 55. Implement Debouncing for User Input

  - Add debouncing to search input (300ms)
  - Add debouncing to workflow sync (300ms)
  - Add debouncing to auto-save (1000ms)
  - Optimize re-renders with React.memo
  - _Requirements: 12.6_
  - _Estimated: 6 hours_


- [ ] 56. Add Caching Layer
  - Implement in-memory cache for itinerary data
  - Cache workflow positions in localStorage
  - Cache agent results (5 min TTL)
  - Add cache invalidation logic
  - _Requirements: 12.1, 12.2_
  - _Estimated: 8 hours_

- [ ] 57. Implement Virtual Scrolling
  - Add virtual scrolling to day-by-day view
  - Add virtual scrolling to change history
  - Add virtual scrolling to agent logs
  - Optimize rendering for large lists
  - _Requirements: 12.4_
  - _Estimated: 10 hours_

- [x] 58. Add Lazy Loading

  - Lazy load change history
  - Lazy load agent history
  - Lazy load revision details
  - Lazy load images with progressive enhancement
  - _Requirements: 12.3, 12.5_
  - _Estimated: 8 hours_

- [x] 59. Implement Optimistic Updates

  - Update UI immediately on user action
  - Rollback on backend failure
  - Show loading state during sync
  - Add retry logic for failed updates
  - _Requirements: 12.2_
  - _Estimated: 8 hours_

- [ ]* 59.1 Write performance tests

  - Test initial load time (<2s)
  - Test change preview generation (<500ms)
  - Test workflow sync latency (<200ms)
  - Test UI update after SSE (<100ms)
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6_
  - _Estimated: 6 hours_



### Epic 21: Final Polish and Testing

- [x] 60. Add Loading States

  - Implement skeleton loaders
  - Add progress indicators
  - Show loading overlays
  - Add loading text descriptions
  - _Requirements: 12.1, 12.2, 12.3_
  - _Estimated: 6 hours_

- [x] 61. Improve Error Messages

  - Review all error messages
  - Add helpful suggestions
  - Include error codes



  - Add links to documentation
  - _Requirements: All error handling requirements_
  - _Estimated: 6 hours_

- [x] 62. Add Keyboard Shortcuts

  - Document all keyboard shortcuts
  - Add keyboard shortcut help modal
  - Implement global shortcuts
  - Add shortcut hints in UI
  - _Requirements: Various_
  - _Estimated: 6 hours_

- [x] 63. Implement Accessibility Improvements

  - Add ARIA labels
  - Ensure keyboard navigation
  - Add screen reader support
  - Test with accessibility tools
  - _Requirements: All UI requirements_
  - _Estimated: 8 hours_

- [x] 64. Add Analytics Tracking


  - Track feature usage
  - Track error rates
  - Track performance metrics
  - Track user flows
  - _Requirements: Success metrics_
  - _Estimated: 6 hours_

- [ ]* 64.1 Write end-to-end tests

  - Test complete user workflows
  - Test error scenarios
  - Test performance under load
  - Test cross-browser compatibility
  - _Requirements: All requirements_
  - _Estimated: 12 hours_

## Summary

**Total Tasks**: 64 main tasks + 21 optional test tasks
**Total Estimated Effort**: 
- Main tasks: 564 hours
- Optional test tasks: 156 hours
- **Total with tests**: 720 hours

**Recommended Approach**: Focus on main tasks first, add tests incrementally

**Critical Path**:
1. Change Management (Phase 1) → Foundation for all other features
2. Workflow Sync (Phase 3) → Enables unified editing experience
3. Agent Controls (Phase 4) → Unlocks AI-powered features
4. Enhanced Chat (Phase 5) → Improves user interaction
5. Revision Management (Phase 6) → Adds safety net for users

**Quick Wins** (High value, low effort):
- Node Locking (Phase 2) - 8-10 hours per component
- Export Enhancements (Phase 7) - 6-8 hours per feature
- Booking UI (Phase 8) - 12 hours for basic flow

**Dependencies**:
- Phase 3 (Workflow Sync) depends on Phase 1 (Change Management)
- Phase 5 (Enhanced Chat) depends on Phase 1 (Change Management)
- Phase 6 (Revision Management) depends on Phase 1 (Change Management)
- Phase 8 (Booking) can be done in parallel with other phases
