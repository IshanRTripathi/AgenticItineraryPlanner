# Agents, Gemini Prompts, Response Structures, and Persistence

This document captures, with exact file references, how agents participate in itinerary creation/modification, what prompts they use with Gemini, the JSON structures expected/returned, and how data is persisted.

## Agents involved

- PlannerAgent (`src/main/java/com/tripplanner/service/agents/PlannerAgent.java`)
  - Purpose: Generate initial itineraries and propose change sets from user requests.
  - Dependencies: `GeminiClient`, `ItineraryJsonService`, `ChangeEngine`, `AgentEventBus`.
  - Key entry points:
    - Itinerary generation flow (builds system + user prompts and requests structured JSON)
    - ChangeSet generation flow from free-text user requests

- EnrichmentAgent (`src/main/java/com/tripplanner/service/agents/EnrichmentAgent.java`)
  - Purpose: Pure-logic validation, pacing, and transit computations (no LLM).
  - Dependencies: `ItineraryJsonService`, `ChangeEngine`, `AgentEventBus`.

- PlacesAgent (`src/main/java/com/tripplanner/service/agents/PlacesAgent.java`)
  - Purpose: Analyze destination and provide place metadata (prompt scaffolding present; actual calls may be disabled in current flow).
  - Dependencies: `GeminiClient`, `AgentEventBus`.

- AgentOrchestrator (`src/main/java/com/tripplanner/service/agents/AgentOrchestrator.java`)
  - Purpose: Sequences PlannerAgent → EnrichmentAgent for generation; also routes change processing.

## Gemini client and prompts

- GeminiClient (`src/main/java/com/tripplanner/service/GeminiClient.java`)
  - Config: Conditional on property `google.ai.api-key`. Mock mode via `google.ai.mock-mode`.
  - Methods:
    - `generateContent(userPrompt, systemPrompt?)` → returns text from Gemini. When mock-mode is true, serves responses from `logs/gemini-responses` or cached `responses/` files; otherwise calls `v1beta/models/gemini-1.5-pro:generateContent`.
    - `generateStructuredContent(prompt, jsonSchema, systemPrompt)` → prepends schema requirement and returns `generateContent(...)` result.
  - Request payload: single text prompt encoded under `contents.parts[].text` (see buildRequestPayload).
  - Response parsing: reads `candidates[0].content.parts[0].text`.
  - Mock response chain: `logs/gemini-responses/*.json` → `responses/gemini_response_*.json` → `getDefaultMockResponse()` which loads `logs/gemini-responses/barcelona_3day_family_normalized.json` or a minimal fallback.

- PlannerAgent prompts (`PlannerAgent`)
  - System prompt for itinerary generation: see method near buildUserPrompt; guidelines include constraints, realistic flow, and always respond with valid JSON matching schema.
  - User prompt (itinerary generation): constructed from `CreateItineraryReq` fields (destination, dates, duration, party, budget tier, language, interests, constraints).
  - JSON schema requested for itinerary generation: `buildItineraryJsonSchema()` includes fields: `summary`, `highlights[]`, `totalEstimatedCost`, `currency`, and `days[]` with per-day `activities[]`, `meals[]`, `accommodation`, `transportation[]`, and `notes`.
  - ChangeSet prompt: built from current itinerary summary (# days, per-day counts) + user request; asks Gemini to generate a `ChangeSet` respecting locked nodes.

Note: In the current code, some flows short-circuit to file-based responses (mock) inside `PlannerAgent` with `loadLatestGeminiResponse()`; live calls occur when `google.ai.mock-mode=false` and a valid API key is provided.

## Where mock data is used

- `GeminiClient` mock mode: controlled by `google.ai.mock-mode` (env `GEMINI_MOCK_MODE`).
- `PlannerAgent`: comments/paths that load from file instead of live API (`loadLatestGeminiResponse()`).
- `ChangeEngine`: temporary mock replacement node when `replace` op lacks a provided node.
- `OrchestratorService`: sets `replaceOp.setNode(null)` to defer node creation to `ChangeEngine` mock.
- `SampleDataGenerator` and test controllers: `generateBarcelonaSample()` invoked by `TestJsonController`/`TestSampleDataController` to persist synthetic data for dev/testing.
- `ToolsService`: returns mock weather data at present.

## Data structures and persistence

- Normalized itinerary DTOs (`src/main/java/com/tripplanner/api/dto/`):
  - `NormalizedItinerary`, `NormalizedDay`, `NormalizedNode`, plus supporting types (`NodeLocation`, `NodeTiming`, `NodeCost`, `NodeDetails`, etc.).
  - These are the single source of truth for itinerary content used across services.

- Persistence layer (Firestore)
  - `ItineraryJsonService` (`src/main/java/com/tripplanner/service/ItineraryJsonService.java`)
    - Serializes `NormalizedItinerary` to JSON via `ObjectMapper` and delegates to `DatabaseService`.
    - CRUD: `createItinerary`, `updateItinerary`, `getItinerary(String)`, `getAllItineraries()`, `deleteItinerary`.
    - Revisions: `saveRevision(itineraryId, NormalizedItinerary)`, `getRevision(itineraryId, version)` store/load from subcollection `revisions`.
    - Flexible lookup: `getItineraryByAnyId(String)` resolves known mappings (e.g., "1" → "it_barcelona_comprehensive").
  - `DatabaseService` interface (`src/main/java/com/tripplanner/service/DatabaseService.java`)
    - Methods: `save`, `findById`, `existsById`, `findAllOrderByUpdatedAtDesc`, `findByUpdatedAtAfter`, `deleteById`, `saveRevision`, `findRevisionByItineraryIdAndVersion`, `getDatabaseType`.
  - Firestore implementation: `FirestoreDatabaseService` (`src/main/java/com/tripplanner/service/FirestoreDatabaseService.java`)
    - Collections: `itineraries/{id}` with fields `id`, `version`, `json`, `updatedAt` (as `Timestamp`).
    - Revisions subcollection: `itineraries/{id}/revisions/{version}` storing the same fields.
    - Entity: `FirestoreItinerary` (`src/main/java/com/tripplanner/data/entity/FirestoreItinerary.java`).

- Change application
  - `ChangeEngine` (`src/main/java/com/tripplanner/service/ChangeEngine.java`)
    - `propose`/`apply`/`undo` on `NormalizedItinerary` in-memory; then persists via `ItineraryJsonService` (`updateItinerary`, `saveRevision`) and syncs a minimal summary to the legacy `Itinerary` JPA entity.
    - Replace/move/insert/delete operations with audit updates.

## Configuration

- `src/main/resources/application.yml`
  - Gemini: `google.ai.api-key`, `google.ai.model`, `google.ai.temperature`, `google.ai.max-tokens`, `google.ai.mock-mode`.
  - Firestore: `firestore.project-id`, `firestore.credentials` (optional), `firestore.use-emulator` (defaults false now), `firestore.enabled`.
  - App: `app.database.type` defaults to `firestore`.

## Live path vs mock path summary

- Live path: `PlannerAgent` builds prompts → `GeminiClient.generateStructuredContent` (mock-mode=false) → JSON text parsed to DTO → saved via `ItineraryJsonService` (Firestore) → `EnrichmentAgent` pure-logic pass → front-end reads via API.
- Mock path: `PlannerAgent` loads latest file or `GeminiClient` mock returns normalized JSON from `logs/gemini-responses`/`responses` → same downstream handling.

## Action items to go fully live

1. Disable mock mode: set `google.ai.mock-mode=false` (or `GEMINI_MOCK_MODE=false`).
2. Ensure `PlannerAgent` calls `GeminiClient.generateStructuredContent` for both generation and change requests (remove file-loading short-circuit).
3. Stop generating mock replacement nodes in `ChangeEngine`; require `ChangeSet.op.node` to be provided by the agent for replace operations.
4. Persist via `ItineraryJsonService` (already in place); revisions are stored under `itineraries/{id}/revisions/{version}`.


