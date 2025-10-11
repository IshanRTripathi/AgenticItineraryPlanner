# UI-Backend Integration - Complete Requirements

## Introduction

This document outlines the comprehensive requirements for integrating all backend features into the frontend UI layer. Based on a thorough analysis of the backend implementation (with all tests passing) and the current frontend state, we've identified specific gaps where backend capabilities exist but are not exposed or fully utilized in the UI.

**Context**: The backend has a robust, fully-tested implementation with sophisticated features including:
- Real-time itinerary generation with SSE updates
- Change management system (propose, apply, undo)
- Chat-based natural language editing
- Agent orchestration with progress tracking
- Workflow management
- Node locking and booking
- Revision history
- Export and sharing capabilities

**Current State**: The frontend has partial integration of these features, with some working well (chat, basic CRUD, SSE) but many advanced features either missing or underutilized.

## Requirements

### Requirement 1: Change Management UI

**User Story**: As a user, I want to preview, apply, and undo changes to my itinerary through direct UI controls, so that I have full control over modifications without relying solely on chat.

#### Acceptance Criteria

1. WHEN a user makes a change to the itinerary THEN the system SHALL display a preview of the proposed changes before applying them
2. WHEN a user views a change preview THEN the system SHALL show a visual diff highlighting what will be added, removed, or modified
3. WHEN a user approves a change preview THEN the system SHALL apply the changes using the `POST /{id}:apply` endpoint
4. WHEN a user rejects a change preview THEN the system SHALL discard the proposed changes without modifying the itinerary
5. WHEN a user clicks an undo button THEN the system SHALL revert to the previous version using the `POST /{id}:undo` endpoint
6. WHEN a user views the change history THEN the system SHALL display a chronological list of all changes with timestamps and user attribution
7. WHEN a user selects a historical version THEN the system SHALL allow jumping to that specific version
8. WHEN changes are applied THEN the system SHALL update the UI in real-time via SSE events

### Requirement 2: Node Locking Controls

**User Story**: As a user, I want to lock specific activities in my itinerary to prevent them from being modified by AI agents or other changes, so that I can protect important bookings or preferences.

#### Acceptance Criteria

1. WHEN a user views an itinerary node THEN the system SHALL display a lock/unlock toggle button
2. WHEN a user clicks the lock button on an unlocked node THEN the system SHALL call `PUT /itineraries/{id}/nodes/{nodeId}/lock` with `locked: true`
3. WHEN a user clicks the unlock button on a locked node THEN the system SHALL call `PUT /itineraries/{id}/nodes/{nodeId}/lock` with `locked: false`
4. WHEN a node is locked THEN the system SHALL display a visual indicator (lock icon) on the node
5. WHEN a node is locked THEN the system SHALL prevent AI agents from modifying that node
6. WHEN a user attempts to modify a locked node manually THEN the system SHALL allow the modification (user override)
7. WHEN the lock state changes THEN the system SHALL update the UI immediately without requiring a page refresh

### Requirement 3: Workflow-Itinerary Bidirectional Sync

**User Story**: As a user, I want changes made in the workflow builder to automatically update the day-by-day itinerary and vice versa, so that I have a unified editing experience across different views.

#### Acceptance Criteria

1. WHEN a user adds a node in the workflow builder THEN the system SHALL add the corresponding component to the itinerary
2. WHEN a user removes a node from the workflow builder THEN the system SHALL remove the corresponding component from the itinerary
3. WHEN a user modifies a node in the workflow builder THEN the system SHALL update the corresponding component in the itinerary
4. WHEN a user adds a component in the day-by-day view THEN the system SHALL add the corresponding node to the workflow
5. WHEN a user removes a component from the day-by-day view THEN the system SHALL remove the corresponding node from the workflow
6. WHEN a user modifies a component in the day-by-day view THEN the system SHALL update the corresponding node in the workflow
7. WHEN changes occur in either view THEN the system SHALL sync the changes within 500ms
8. WHEN sync conflicts occur THEN the system SHALL resolve them using the most recent change (last-write-wins)
9. WHEN sync fails THEN the system SHALL display an error message and allow retry

### Requirement 4: Agent Execution Controls

**User Story**: As a user, I want to manually trigger specific agents to enrich or modify my itinerary, so that I can control when and how AI assistance is applied.

#### Acceptance Criteria

1. WHEN a user views the itinerary THEN the system SHALL display an agent control panel showing available agents
2. WHEN a user clicks on an agent THEN the system SHALL display the agent's capabilities and configuration options
3. WHEN a user clicks "Execute Agent" THEN the system SHALL call `POST /itineraries/{id}/agents/{agentType}/execute` with the selected parameters
4. WHEN an agent is executing THEN the system SHALL display real-time progress via SSE updates
5. WHEN an agent completes THEN the system SHALL display the results and any changes made
6. WHEN an agent fails THEN the system SHALL display an error message with details
7. WHEN a user views agent history THEN the system SHALL display past executions with timestamps, results, and status
8. WHEN a user clicks on a historical execution THEN the system SHALL display detailed logs and changes made

### Requirement 5: Enhanced Chat Features

**User Story**: As a user, I want better control over chat-based changes including preview before apply, granular auto-apply settings, and improved disambiguation, so that I have more confidence in AI-driven modifications.

#### Acceptance Criteria

1. WHEN a user sends a chat message that would modify the itinerary THEN the system SHALL display a preview of the changes before applying them (if auto-apply is disabled)
2. WHEN a user enables "Preview Before Apply" mode THEN the system SHALL require explicit approval for all chat-driven changes
3. WHEN a user disables "Preview Before Apply" mode THEN the system SHALL automatically apply changes as before
4. WHEN the chat system needs disambiguation THEN the system SHALL display a clear UI with options to select from
5. WHEN a user selects a disambiguation option THEN the system SHALL re-process the request with the selected context
6. WHEN chat changes are applied THEN the system SHALL display a confirmation message with a summary of changes
7. WHEN chat changes fail THEN the system SHALL display an error message and suggest alternatives
8. WHEN a user views chat history THEN the system SHALL display all messages with timestamps and change summaries

### Requirement 6: Revision Management UI

**User Story**: As a user, I want to view and restore previous versions of my itinerary, so that I can recover from unwanted changes or compare different planning options.

#### Acceptance Criteria

1. WHEN a user clicks "View History" THEN the system SHALL display a timeline of all itinerary versions
2. WHEN a user views a version in the timeline THEN the system SHALL display the version number, timestamp, user, and change summary
3. WHEN a user clicks on a version THEN the system SHALL display a detailed diff showing what changed
4. WHEN a user clicks "Restore This Version" THEN the system SHALL call `POST /{id}:undo` with the target version number
5. WHEN a version is restored THEN the system SHALL update the UI to reflect the restored state
6. WHEN a user compares two versions THEN the system SHALL display a side-by-side diff
7. WHEN the revision history is empty THEN the system SHALL display a message indicating no history is available

### Requirement 7: Export and Sharing Enhancements

**User Story**: As a user, I want to export my itinerary in multiple formats and share it with others, so that I can use my travel plans offline or collaborate with travel companions.

#### Acceptance Criteria

1. WHEN a user clicks "Export PDF" THEN the system SHALL call `GET /export/itineraries/{id}/pdf` and download the PDF
2. WHEN a user clicks "Share" THEN the system SHALL call `POST /itineraries/{id}:share` and display a shareable link
3. WHEN a user copies the share link THEN the system SHALL copy it to the clipboard
4. WHEN a user clicks "Send via Email" THEN the system SHALL display an email form with recipient and message fields
5. WHEN a user submits the email form THEN the system SHALL call `POST /export/email/share` with the recipient details
6. WHEN export or sharing fails THEN the system SHALL display an error message with retry option
7. WHEN a shared link is accessed THEN the system SHALL display the itinerary in read-only mode

### Requirement 8: Booking Integration

**User Story**: As a user, I want to book activities and accommodations directly from my itinerary, so that I can seamlessly transition from planning to execution.

#### Acceptance Criteria

1. WHEN a user clicks "Book" on a node THEN the system SHALL display booking options and pricing
2. WHEN a user confirms a booking THEN the system SHALL call `POST /book` with the itinerary and node IDs
3. WHEN a booking is successful THEN the system SHALL display a confirmation with booking reference
4. WHEN a booking is successful THEN the system SHALL mark the node as booked with a visual indicator
5. WHEN a booking fails THEN the system SHALL display an error message and allow retry
6. WHEN a user views booked nodes THEN the system SHALL display booking details and reference numbers
7. WHEN a user cancels a booking THEN the system SHALL call `POST /bookings/{bookingId}:cancel` and update the node status

### Requirement 9: Real-Time Collaboration Indicators

**User Story**: As a user, I want to see when other users are viewing or editing the same itinerary, so that I can avoid conflicts and coordinate changes.

#### Acceptance Criteria

1. WHEN multiple users view the same itinerary THEN the system SHALL display presence indicators showing who is online
2. WHEN another user makes a change THEN the system SHALL display a notification with the user's name and change summary
3. WHEN another user is editing a node THEN the system SHALL display a visual indicator on that node
4. WHEN changes conflict THEN the system SHALL display a conflict resolution UI
5. WHEN a user resolves a conflict THEN the system SHALL apply the resolution and notify other users
6. WHEN a user goes offline THEN the system SHALL remove their presence indicator within 30 seconds

### Requirement 10: Advanced Search and Filtering

**User Story**: As a user, I want to search and filter my itinerary by various criteria, so that I can quickly find specific activities or information.

#### Acceptance Criteria

1. WHEN a user types in the search box THEN the system SHALL filter nodes by name, type, location, or tags
2. WHEN a user applies a filter THEN the system SHALL show only nodes matching the filter criteria
3. WHEN a user selects a node type filter THEN the system SHALL show only nodes of that type
4. WHEN a user selects a day filter THEN the system SHALL show only nodes from that day
5. WHEN a user clears filters THEN the system SHALL show all nodes again
6. WHEN search results are displayed THEN the system SHALL highlight matching text
7. WHEN no results are found THEN the system SHALL display a "No results" message with suggestions

### Requirement 11: Offline Support

**User Story**: As a user, I want to access my itinerary offline, so that I can view my plans without an internet connection while traveling.

#### Acceptance Criteria

1. WHEN a user views an itinerary while online THEN the system SHALL cache the itinerary data locally
2. WHEN a user loses internet connection THEN the system SHALL display a banner indicating offline mode
3. WHEN a user views an itinerary offline THEN the system SHALL load the cached data
4. WHEN a user makes changes offline THEN the system SHALL queue the changes for sync when online
5. WHEN the user regains internet connection THEN the system SHALL sync queued changes automatically
6. WHEN sync conflicts occur THEN the system SHALL display a conflict resolution UI
7. WHEN offline data is stale THEN the system SHALL display a warning and offer to refresh when online

### Requirement 12: Performance Optimization

**User Story**: As a user, I want the application to load and respond quickly, so that I can efficiently plan my trips without delays.

#### Acceptance Criteria

1. WHEN a user loads an itinerary THEN the system SHALL display the initial view within 2 seconds
2. WHEN a user makes a change THEN the system SHALL update the UI within 500ms
3. WHEN a user switches between views THEN the system SHALL render the new view within 1 second
4. WHEN a user scrolls through a large itinerary THEN the system SHALL use virtual scrolling to maintain 60fps
5. WHEN a user loads images THEN the system SHALL use lazy loading and progressive enhancement
6. WHEN a user performs a search THEN the system SHALL debounce input and return results within 300ms
7. WHEN the application is idle THEN the system SHALL minimize background processing to conserve resources

## Success Metrics

### Technical Metrics
- Change preview generation time: <500ms
- Workflow-itinerary sync latency: <200ms
- Agent execution start time: <1s
- UI update latency after SSE event: <100ms
- Error rate for change operations: <1%

### User Experience Metrics
- Feature discovery rate: 70% of users try new features within first session
- Change preview adoption: 60% of users use preview before apply
- Workflow builder usage: 50% of users try workflow builder
- Agent execution rate: 40% of users manually trigger agents
- User satisfaction: 4.5/5 rating for editing experience

### Business Metrics
- User retention: 20% improvement in weekly active users
- Session duration: 25% increase in editing sessions
- Feature engagement: 35% increase in advanced feature usage
- Support tickets: 30% reduction in editing-related issues
