Here’s a tight, implementation-ready **PRD for Google Maps integration** aligned to this repo’s current structure. It reflects exactly what exists now and calls out required additions explicitly.

## 1) Goal & scope

- **Target UI location**: `frontend/src/components/TravelPlanner.tsx` right panel (currently renders `WorkflowBuilder`). Map will be added as a new component integrated into this screen.
- The map should:
  - Start in a world view and smoothly zoom toward the itinerary area.
  - Provide a custom control to switch map type: Roadmap, Terrain, Satellite, Hybrid.
  - Render itinerary nodes and support selection/highlight interactions.
- Out of scope: routes overlay, clustering, heatmaps, 3D buildings, offline.

## 2) Dependencies & keys (repo-accurate)

- Enable in GCP: Maps JavaScript API, Places API (New). Routes API optional for future.
- Keys:
  - Browser key for Maps JS SDK: HTTP referrer restricted, API-restricted to Maps JavaScript API.
  - Server key for backend/agents: API-restricted to Places API (New) (and Routes later if used).
- Frontend config context in this repo:
  - API base URL is read from `import.meta.env.VITE_API_BASE_URL` (fallback `http://localhost:8080/api/v1`) in `frontend/src/services/apiClient.ts`.
  - Firebase app is initialized in `frontend/src/config/firebase.ts`. This is unrelated to Maps, but indicates where FE service configs live.
- Required addition (FE): a safe way to inject the Google Maps Browser key, e.g. `VITE_GOOGLE_MAPS_BROWSER_KEY` in `.env` and consumed by a script loader.
- Required addition (BE/Agents): store/use the server Places API key in backend secure config (e.g., Spring `application.yml` or secret manager) – not exposed to FE.

## 3) Data contract (Backend → FE)

- Current backend endpoint returning master JSON: `GET /api/v1/itineraries/{id}/json` from `ItinerariesController#getItineraryJson`.
- Current type exposed to FE: `frontend/src/types/NormalizedItinerary.ts` (via `apiClient.getItinerary` which calls `/itineraries/{id}/json`).
- Present fields: itinerary metadata and per-day `nodes[]` with optional `location.coordinates { lat, lng }` per node.
- Missing today (required additions to backend JSON to enable precise map behavior):
  - `mapBounds: { south, west, north, east }` at itinerary level.
  - `countryCentroid: { lat, lng }` at itinerary level.
  - Optional per-node `details.googleMapsUri` from Places Details.

Exact proposed additions to `NormalizedItinerary` (backend DTO) mirrored in FE types:

```startLine:endLine:src/main/java/com/tripplanner/dto/NormalizedItinerary.java
// Add (server-side):
// @JsonProperty("mapBounds") private MapBounds mapBounds;
// @JsonProperty("countryCentroid") private Coordinates countryCentroid;
```

```startLine:endLine:frontend/src/types/NormalizedItinerary.ts
// Add (FE types):
// mapBounds?: { south: number; west: number; north: number; east: number };
// countryCentroid?: { lat: number; lng: number };
// In NodeDetails optionally: googleMapsUri?: string;
```

## 4) UX requirements (Repo adaptations)

### 4.1 Initial camera & zoom animation

- On first render in `TravelPlanner`:
  - Initialize map at world view (center `{lat:0, lng:0}`, zoom 2–3).
  - After idle, animate toward target derived from itinerary-level `mapBounds` (preferred) or `countryCentroid`. These fields are not present yet – backend must add them.
  - Subsequent loads: persist recent camera in FE state (e.g., `localStorage` keyed by itineraryId).

### 4.2 Terrain selector (custom control)

- A floating control on the map switches map type using `map.setMapTypeId(...)`.
- Persist last chosen type (storage keyed by itineraryId).

### 4.3 Interaction rules

- Clicking a node marker triggers selection in `WorkflowBuilder`/planner right panel context.
- Selecting a segment/day recenters markers but preserves map type.
- Locked nodes indicate lock in marker info.

## 5) Component design (FE)

- New map component: `frontend/src/components/travel-planner/TripMap.tsx` (to be created). It will be owned by `TravelPlanner`.
- Proposed props (derive from current data shape):
  - `itineraryId: string`
  - `mapBounds?: { south: number; west: number; north: number; east: number }` (requires backend addition)
  - `countryCentroid?: { lat: number; lng: number }` (requires backend addition)
  - `nodes: Array<{ id: string; title: string; type: string; location?: { coordinates?: { lat: number; lng: number } }; locked?: boolean; details?: { rating?: number; /* optional */ } }>`
  - `selectedNodeId?: string`
  - `selectedDay?: number`
  - `onMarkerClick?: (nodeId: string) => void`
- Behavior:
  - Load Maps JS with `libraries=places`. Use a script loader that reads the key from `VITE_GOOGLE_MAPS_BROWSER_KEY`.
  - Initialize map once; maintain refs to Map instance and markers.
  - Add a custom `TerrainControl` subcomponent.
  - Animate camera on first render if `mapBounds` or `countryCentroid` available.
  - Render markers from `nodes` that have `location.coordinates`.

- New control component: `frontend/src/components/travel-planner/TerrainControl.tsx` (to be created).
  - Exposes a button → popover with four types; calls `map.setMapTypeId`.

## 6) Map initialization options (unchanged intent)

```js
const map = new google.maps.Map(canvasEl, {
  center: { lat: 0, lng: 0 },
  zoom: 2,
  mapTypeId: google.maps.MapTypeId.ROADMAP,
  gestureHandling: 'greedy',
  fullscreenControl: true,
  streetViewControl: false,
  mapTypeControl: false,
  zoomControl: true,
});
```

## 7) Camera animation algorithm (deterministic)

```js
function animateToBounds(map, bounds) {
  const targetCenter = {
    lat: (bounds.north + bounds.south) / 2,
    lng: (bounds.east + bounds.west) / 2,
  };
  const targetZoom = estimateZoomForBounds(map.getDiv().clientWidth, map.getDiv().clientHeight, bounds);
  map.panTo(targetCenter);
  let z = map.getZoom();
  const step = () => {
    if (z >= targetZoom) return;
    z += 0.5;
    map.setZoom(z);
    requestAnimationFrame(step);
  };
  requestAnimationFrame(step);
}
```

- Fallback: if animation is interrupted or data missing, use `map.fitBounds(bounds, { padding: 48 })` when `bounds` is available.

## 8) Backend support (what FE needs, current vs required)

- Current endpoint used by FE: `GET /api/v1/itineraries/{id}/json`.
- Current JSON does not include `mapBounds` or `countryCentroid`. Required additions for smooth initial camera are listed in section 3.
- Node coordinates: `NormalizedNode.location.coordinates { lat, lng }` already exists in FE types; ensure backend populates when available.
- Optional enrichment for info window: `details.googleMapsUri` via Places Details (server-side only).

## 9) Places data flow (agents only; FE displays)

- Server uses Places API (New) to enrich nodes with `placeId`, `googleMapsUri`, ratings, photos, reviews (if needed).
- Persist minimal subset in the master JSON at node level; FE reads only via `/itineraries/{id}/json`.

## 10) Performance budget

- Initial map render < 1.2s desktop, < 2.0s mid-tier mobile.
- Marker count ≤ 200 without clustering.
- Load Google Maps script async via a loader after Planner UI mounts.
- Reuse Map instance and markers.

## 11) Security

- Browser key: HTTP referrer restricted; API-restricted to Maps JavaScript API only.
- Server key: not exposed to browser; stored in backend secure config.

## 12) Failure handling

- If Maps JS fails: show a toast and a fallback panel in the right pane.
- If nodes lack coordinates: center at `countryCentroid` (when added) or keep world view with an info note.

## 13) Acceptance criteria

1. On first load, map appears as world view, then smoothly zooms toward itinerary area when `mapBounds`/`countryCentroid` are present.
2. A Terrain selector button switches map type instantly and persists the last choice per itinerary.
3. Clicking a marker focuses the corresponding item in the planner; selecting a day recenters markers without changing map type.
4. Locked nodes display a lock indicator in info window.
5. After backend patch events update coordinates, marker positions refresh without re-instantiating the Map.

## 14) Build checklist (repo-specific)

- [ ] Backend adds `mapBounds` and `countryCentroid` to `NormalizedItinerary` DTO and returns them in `/itineraries/{id}/json`.
- [ ] Frontend defines `VITE_GOOGLE_MAPS_BROWSER_KEY` and loads Maps JS via a script loader using that key.
- [ ] Create `frontend/src/components/travel-planner/TripMap.tsx` and `TerrainControl.tsx` and integrate into `TravelPlanner` right panel.
- [ ] Ensure nodes include `location.coordinates` where available; optionally `details.googleMapsUri`.
- [ ] Manual tests: fresh load animation, terrain switch, marker → workflow sync, selection → recenter, patch updates.
