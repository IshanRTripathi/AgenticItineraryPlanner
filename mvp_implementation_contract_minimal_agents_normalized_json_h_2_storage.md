# MVP Implementation Contract — Minimal Agents, Normalized JSON, H2 Storage

This document locks down the **end‑to‑end implementation** you requested: minimal agents, a single **master JSON** as the source of truth, **Workflow‑only editing** (left pane mirrors), **H2** storage now, and no auth/Firebase/PDF/Email. Razorpay is mocked. The UI only interacts with the backend APIs.

---

## 1) Ground Rules (exact)
- **Auth:** none.
- **Storage now:** **H2** (file mode). **No Firebase**; future migration to NoSQL.
- **Payments:** Razorpay **mocked**.
- **Exports:** no PDF, no emails.
- **Source of truth:** one **master JSON** per itinerary. **All agents** read/write this JSON. UI never reads JSON directly; it calls the backend.
- **Editing surface:** **Workflow canvas only**; left pane is **read‑only** and mirrors the JSON.
- **Zero duplication:** no parallel arrays for the same concept; everything is modeled as **nodes** in a day.
- **Keep it simple:** only required agents and fields.

---

## 2) Minimal Agent Set
- **PlannerAgent**
  - Converts chat/workflow actions → **ChangeSet**.
  - Applies create/move/delete/retime ops to day `nodes[]` and maintains `edges[]`.
  - Honors locks; when blocked, returns suggestions (no destructive changes to locked nodes).
- **EnrichmentAgent** (clubbed)
  - Validates **opening hours vs node timing**; writes node‑level and day‑level warnings.
  - Computes basic **pacing** per day and light **transit durations** if missing.
- **BookingAgent (mock)**
  - On `/book`, marks a node as `locked=true`, adds `labels:["Booked"]`, and sets `bookingRef:"MOCK-…"`.

> The orchestrator is implicit (service layer sequences these agents). No other agents (photo/packing/etc.).

---

## 3) Master JSON — Normalized Contract
The JSON below is the **only** persisted structure. It is rendered into the Planner/Day‑by‑day UI and edited only via Workflow actions or chat.

### 3.1 Root
```json
{
  "itineraryId": "it_123",
  "version": 1,
  "summary": "…",
  "currency": "EUR",
  "themes": ["family"],
  "days": [ /* Day objects */ ],
  "settings": { "autoApply": false, "defaultScope": "trip" },
  "agents": { "planner": {"lastRunAt": null}, "enrichment": {"lastRunAt": null} }
}
```

### 3.2 Day
```json
{
  "dayNumber": 1,
  "date": "2025-10-04",
  "location": "Barcelona",
  "pacing": { "score": "balanced", "transitMin": 0, "stops": 0 },
  "warnings": [],
  "notes": "",
  "totals": { "distanceKm": 0, "cost": 0, "durationHr": 0 },
  "timeWindow": { "start": "09:00", "end": "18:00" },
  "nodes": [ /* Node objects */ ],
  "edges": [ {"from": "n1", "to": "n2"} ]
}
```

### 3.3 Node (single schema for all types)
```json
{
  "id": "n_sagrada",
  "type": "attraction|meal|hotel|transit",
  "title": "Sagrada Familia",
  "location": {
    "name": "…",
    "address": "…",
    "coordinates": { "lat": 41.403, "lng": 2.174 }
  },
  "timing": {
    "startTime": "2025-10-04T09:00:00Z",
    "endTime":   "2025-10-04T10:00:00Z",
    "durationMin": 60
  },
  "cost": { "amount": 70, "currency": "EUR", "per": "person" },
  "details": {
    "rating": 4.8,
    "category": "architecture",
    "tags": ["gaudi"],
    "openingHours": { "monday": {"open": "09:00", "close": "18:00"} }
  },
  "labels": ["Booking Required"],
  "tips":   { "travel": ["Any time"], "warnings": [] },
  "links":  { "book": "…", "details": "…" },
  "transit": { "mode": "taxi", "distanceKm": 3.2, "timeMin": 15 },
  "locked": false,
  "status": "planned|in_progress|skipped|cancelled|completed",
  "updatedBy": "agent|user",
  "updatedAt": "ISO-8601"
}
```

**Notes**
- **One nodes[] array per day.** Former `components/meals/accommodation/transportation` are all nodes of types `attraction|meal|hotel|transit`.
- **Left pane mirrors nodes** (chips, price pill, time window, tips, warnings, book/details buttons) and computes **nights** from `hotel` nodes.

---

## 4) Change Mechanics (for chat + workflow)

### 4.1 ChangeSet (request)
```json
{
  "scope": "trip|day",
  "day": 1,
  "ops": [
    { "op": "move",   "id": "n_sagrada",  "startTime": "2025-10-04T10:00:00Z", "endTime": "2025-10-04T12:30:00Z" },
    { "op": "insert", "after": "n_sagrada", "node": { "id":"n_tapas", "type":"meal", "title":"Tapas", "timing": {"startTime": "+00:15", "durationMin": 60 }, "location": {"name":"…"} } },
    { "op": "delete", "id": "n_breakfast" }
  ],
  "preferences": { "userFirst": true }
}
```

### 4.2 ItineraryDiff (preview)
```json
{ "added": [{"id": "n_tapas", "day": 1}], "removed": [{"id": "n_breakfast", "day": 1}], "updated": [{"id": "n_sagrada", "fields": ["timing"]}] }
```

### 4.3 PatchEvent (SSE)
```json
{
  "type": "PatchEvent",
  "itineraryId": "it_123",
  "fromVersion": 12,
  "toVersion": 13,
  "diff": { "added":[], "removed":[], "updated":[] },
  "summary": "…",
  "updatedBy": "agent"
}
```

**Rules**
- **Preview vs auto‑apply:** user preference. Both are supported.
- **Scope toggle:** planner‑wide by default; user can toggle “current day only.”
- **Locks:** `locked=true` nodes cannot be moved/deleted; time suggestions may be returned but not applied without user action.
- **Revisions:** keep last **50** versions. A **Revision Viewer** compares any two versions and lets the user apply one.
- **Concurrency:** while an agent runs, **disable workflow edits**; on completion, force‑apply its patch and show a toast.
- **Replan from now:** affects **today only**.
- **Audit:** persist `updatedBy` on nodes; Revision rows include `author`.

---

## 5) Backend API (no auth, H2, mocked booking)
Base path: `/api/v1`

### 5.1 Itineraries
- **GET** `/itineraries/{id}` → `200` → returns **master JSON**.
- **POST** `/itineraries/{id}:propose` → `200` → body: **ChangeSet** → `{ proposed, diff, previewVersion }` (no DB write).
- **POST** `/itineraries/{id}:apply` → `200` → body: `{ changeSetId | changeSet }` → `{ toVersion, diff }` (writes JSON + revision).
- **POST** `/itineraries/{id}:undo` → `200` → body: `{ toVersion? }` → `{ toVersion, diff }`.
- **GET** `/itineraries/patches?itineraryId=…` → `200 text/event-stream` → emits **PatchEvent**.

### 5.2 Agents
- **POST** `/agents/run` → `200` → `{ type:"planner"|"enrichment", itineraryId }` → `{ status:"ok" }` (mutates JSON accordingly).

### 5.3 Booking (mock)
- **POST** `/book` → `200` → `{ itineraryId, nodeId }` → `{ bookingRef:"MOCK-123", locked:true }` (updates node in JSON).

---

## 6) Persistence (H2 now, NoSQL later)

### 6.1 Tables
- **ITINERARIES**
  - `id VARCHAR(40) PRIMARY KEY`
  - `version INT NOT NULL`
  - `json CLOB NOT NULL`
  - `updated_at TIMESTAMP NOT NULL`
- **REVISIONS**
  - `id VARCHAR(40) PRIMARY KEY`
  - `itinerary_id VARCHAR(40) NOT NULL`
  - `version INT NOT NULL`
  - `json CLOB NOT NULL`
  - `created_at TIMESTAMP NOT NULL`
  - `author VARCHAR(20) NOT NULL`   // "agent"|"user"

### 6.2 Operations
- `:propose` does not write.
- `:apply` increments `version`, overwrites ITINERARIES.json, and appends to REVISIONS.
- `:undo` restores JSON from a chosen REVISIONS row and updates ITINERARIES.

**Migration note:** later NoSQL: a single `itineraries` collection (doc per itinerary) + `revisions` subcollection. JSON contract stays identical.

---

## 7) Services (thin, simple)
- **ItineraryService** — load/save JSON from H2; versioning; emit PatchEvents on writes.
- **ChangeEngine** — apply ChangeSet → {proposed, diff}; commit path writes + publishes PatchEvent.
- **PlannerAgent** — parse chat/workflow → ChangeSet (or accept ChangeSet) → delegate to ChangeEngine.
- **EnrichmentAgent** — recompute warnings, pacing, and transit durations; write back to JSON; publish PatchEvent.
- **BookingServiceMock** — lock node, label "Booked", set `bookingRef`.
- **SseHub** — in‑memory registry of `SseEmitter`s per itinerary for PatchEvents.

---

## 8) UI Integration (what FE will do)
- Load: `GET /itineraries/{id}` → render **Workflow** and **Day by day** from the same JSON.
- Edit: Workflow actions → build **ChangeSet** → `:propose` → preview diff (or auto‑apply per preference) → `:apply`.
- Subscribe: open SSE to `/itineraries/patches?...` → apply **PatchEvents** to state; disable workflow while agent runs; show toast when applied.
- Book (mock): call `/book` with `nodeId` → FE reflects `locked` and `labels`.

---

## 9) Acceptance Criteria
- A single **nodes[]** array per day drives **both** Workflow and Day‑by‑day; no duplicate arrays anywhere.
- `PlannerAgent` can move/insert/delete nodes and update edges; `EnrichmentAgent` adds warnings/pacing without breaking locks.
- `:propose` returns a valid **diff** without writing; `:apply` increments **version**, persists JSON, and emits a **PatchEvent**; `:undo` restores a revision.
- While an agent is running, the workflow UI is disabled; after completion, the plan reflects changes automatically.
- Booking mock sets `locked=true`, adds `Booked` label, and `bookingRef` on the node.

---

## 10) Deliverables Checklist (for Cursor)
- [ ] H2 schema (two tables) created on app start.
- [ ] Controllers: Itineraries, Agents, Booking (mock), SSE patches.
- [ ] Services: ItineraryService, ChangeEngine, PlannerAgent, EnrichmentAgent, BookingServiceMock, SseHub.
- [ ] DTOs: ChangeSet, ItineraryDiff, PatchEvent.
- [ ] JSON normalizer to convert existing day arrays into **nodes** + **edges**.
- [ ] Sample data: normalized **Barcelona** itinerary.
- [ ] README section: how FE calls **propose/apply/undo** and listens to **patches**.

---

### Appendix A — Minimal examples
**ChangeSet → propose → diff**
```http
POST /api/v1/itineraries/it_123:propose
{
  "scope":"day", "day":1,
  "ops":[{"op":"move","id":"n_sagrada","startTime":"2025-10-04T10:00:00Z"}]
}
```
Response:
```json
{ "proposed": {"version":2, "days":[…]}, "diff": {"updated":[{"id":"n_sagrada","fields":["timing"]}]}, "previewVersion": 2 }
```

**Apply**
```http
POST /api/v1/itineraries/it_123:apply
{ "changeSet": { …same as above… } }
```
Response:
```json
{ "toVersion": 2, "diff": {"updated":[{"id":"n_sagrada","fields":["timing"]}]}}
```

**PatchEvent (SSE frame)**
```json
{"type":"PatchEvent","itineraryId":"it_123","fromVersion":1,"toVersion":2,"diff":{…},"updatedBy":"agent"}
```

