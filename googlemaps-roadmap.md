# Google Maps Integration Roadmap
## AgenticItineraryPlanner - End-to-End Implementation Plan

### Overview
This roadmap provides a systematic approach to integrating Google Maps into the AgenticItineraryPlanner, minimizing errors and knowledge gaps through phased implementation with clear validation gates.

---

## Phase 1: Foundation & Backend Data Model (Week 1)

### 1.1 Backend Schema Extensions
**Objective**: Add map-specific fields to the normalized itinerary structure

**Tasks**:
- [x] **Extend `NormalizedItinerary` DTO**
  - File: `src/main/java/com/tripplanner/dto/NormalizedItinerary.java`
  - Add fields:
    ```java
    @JsonProperty("mapBounds")
    private MapBounds mapBounds;
    
    @JsonProperty("countryCentroid") 
    private Coordinates countryCentroid;
    ```

- [x] **Create supporting DTOs**
  - File: `src/main/java/com/tripplanner/dto/MapBounds.java`
    ```java
    public class MapBounds {
        private double south;
        private double west; 
        private double north;
        private double east;
        // getters/setters
    }
    ```

- [x] **Extend `NodeDetails` for Google Maps URI**
  - File: `src/main/java/com/tripplanner/dto/NodeDetails.java`
  - Add: `private String googleMapsUri;`

**Validation**:
- [x] Backend builds successfully (compiles with DTO changes)
- [x] Manual verification of JSON serialization/deserialization paths
- [ ] Add unit tests for DTOs (pending)

### 1.2 Backend Service Layer Updates
**Objective**: Implement map bounds calculation and coordinate enrichment

**Tasks**:
- [x] **Create `MapBoundsCalculator` service**
  - File: `src/main/java/com/tripplanner/service/MapBoundsCalculator.java`
  - Methods:
    - `calculateBounds(List<NormalizedNode> nodes): MapBounds`
    - `calculateCentroid(List<NormalizedNode> nodes): Coordinates`
    - `validateCoordinates(Coordinates coords): boolean`

- [x] **Update `ItineraryJsonService`**
  - File: `src/main/java/com/tripplanner/service/ItineraryJsonService.java`
  - Modify `getItinerary()` to populate `mapBounds` and `countryCentroid`
  - Add coordinate validation before bounds calculation

- [ ] **Add Google Places API integration (server-side)**
  - File: `src/main/java/com/tripplanner/service/GooglePlacesService.java`
  - Methods:
    - `enrichNodeWithPlaceDetails(NormalizedNode node): NormalizedNode`
    - `getPlaceDetails(String placeId): PlaceDetails`
  - Use server-side Places API key (not exposed to frontend)

**Validation**:
- [x] Backend build succeeded after service updates
- [ ] Integration tests for bounds calculation (pending)
- [ ] Test with various coordinate scenarios (pending)
- [ ] Verify Places API integration with mock responses (future)
- [ ] Test error handling for missing/invalid coordinates (pending)

### 1.3 Configuration & Security
**Objective**: Set up secure API key management

**Tasks**:
- [ ] **Add Google Maps configuration**
  - File: `src/main/resources/application.yml`
  - Add:
    ```yaml
    google:
      maps:
        places-api-key: ${GOOGLE_PLACES_API_KEY:}
        browser-key: ${GOOGLE_MAPS_BROWSER_KEY:}
    ```

- [ ] **Create configuration class**
  - File: `src/main/java/com/tripplanner/config/GoogleMapsConfig.java`
  - Properties for API keys and endpoints

- [ ] **Update deployment configuration**

**CORS Update (completed)**:
- [x] Backend CORS allows local dev origins including `http://localhost:3001` via `CorsConfig` and `@CrossOrigin` on `ItinerariesController`.
  - File: `cloudbuild.yaml`
  - Add environment variables for API keys
  - Ensure server key is not exposed in frontend builds

**Validation**:
- [ ] Verify keys are properly injected in different environments
- [ ] Test that server key is never exposed to frontend
- [ ] Validate API key restrictions in Google Cloud Console

---

## Phase 2: Frontend Type System & Data Flow (Week 1-2)

### 2.1 TypeScript Type Extensions
**Objective**: Mirror backend changes in frontend type system

**Tasks**:
- [x] **Update `NormalizedItinerary` interface**
  - File: `frontend/src/types/NormalizedItinerary.ts`
  - Add:
    ```typescript
    mapBounds?: {
      south: number;
      west: number;
      north: number;
      east: number;
    };
    countryCentroid?: {
      lat: number;
      lng: number;
    };
    ```

- [x] **Extend `NodeDetails` interface**
  - Add: `googleMapsUri?: string;`

- [x] **Create map-specific types**
  - File: `frontend/src/types/MapTypes.ts`
  - Types for map configuration, markers, bounds, etc.

**Validation**:
- [x] TypeScript compilation without errors (frontend build passed)
- [x] Types match backend DTOs
- [x] IDE type inference verified

### 2.2 API Client Updates
**Objective**: Ensure frontend can consume new map data

- [x] **Update `apiClient.getItinerary()`**
  - File: `frontend/src/services/apiClient.ts`
  - Verify it correctly transforms new fields from backend

- [x] **Update data transformers**
  - File: `frontend/src/services/normalizedDataTransformer.ts`
  - Ensure `mapBounds` and `countryCentroid` are properly mapped

**Validation**:
- [x] Frontend build passed with new fields
- [x] Transformer maps `mapBounds` and `countryCentroid`
- [ ] Manual API validation in UI (pending)

---

## Phase 3: Google Maps Script Loading & Configuration (Week 2)

### 3.1 Environment Configuration
**Objective**: Set up secure frontend API key management

**Tasks**:
- [ ] **Add environment variable**
  - File: `frontend/.env.example`
  - Add: `VITE_GOOGLE_MAPS_BROWSER_KEY=your_browser_key_here`

- [ ] **Update build configuration**
  - File: `frontend/vite.config.ts`
  - Ensure environment variables are properly loaded

- [ ] **Create configuration service**
  - File: `frontend/src/config/maps.ts`
  - Centralized maps configuration management

**Validation**:
- [ ] Verify environment variable is loaded in development
- [ ] Test that key is not exposed in client-side code inspection
- [ ] Validate API key restrictions work correctly

### 3.2 Google Maps Script Loader
**Objective**: Implement robust script loading with error handling

**Tasks**:
- [ ] **Create script loader utility**
  - File: `frontend/src/utils/googleMapsLoader.ts`
  - Features:
    - Async script loading
    - Error handling and retry logic
    - Loading state management
    - Duplicate loading prevention

- [ ] **Create React hook for Maps loading**
  - File: `frontend/src/hooks/useGoogleMaps.ts`
  - Returns loading state, error state, and Google Maps API instance

**Validation**:
- [ ] Test script loading in different network conditions
- [ ] Verify error handling for invalid keys
- [ ] Test loading state management
- [ ] Ensure no memory leaks on component unmount

---

## Phase 4: Core Map Components (Week 2-3)

### 4.1 Base Map Component
**Objective**: Create the foundational map component

**Tasks**:
- [ ] **Create `TripMap` component**
  - File: `frontend/src/components/travel-planner/TripMap.tsx`
  - Features:
    - Map initialization with proper options
    - Camera animation system
    - Marker management
    - Event handling (marker clicks, map interactions)
    - Error boundaries and fallback UI

- [ ] **Create map utilities**
  - File: `frontend/src/utils/mapUtils.ts`
  - Functions:
    - `calculateOptimalZoom(bounds, viewport): number`
    - `animateToBounds(map, bounds): Promise<void>`
    - `createMarkerIcon(type, status): string`
    - `validateCoordinates(coords): boolean`

**Validation**:
- [ ] Test map initialization with various configurations
- [ ] Verify camera animation works smoothly
- [ ] Test marker creation and positioning
- [ ] Validate error handling and fallback states

### 4.2 Terrain Control Component
**Objective**: Implement map type switching functionality

**Tasks**:
- [ ] **Create `TerrainControl` component**
  - File: `frontend/src/components/travel-planner/TerrainControl.tsx`
  - Features:
    - Floating button with current map type indicator
    - Popover with four map type options
    - Local storage persistence
    - Smooth transitions between map types

**Validation**:
- [ ] Test all map type switches work correctly
- [ ] Verify persistence across page reloads
- [ ] Test UI responsiveness and accessibility
- [ ] Validate popover positioning and behavior

### 4.3 Marker System
**Objective**: Implement intelligent marker rendering and management

**Tasks**:
- [ ] **Create marker management system**
  - File: `frontend/src/components/travel-planner/MarkerManager.tsx`
  - Features:
    - Efficient marker creation/updating
    - Marker clustering for large datasets
    - Status-based styling (locked, completed, etc.)
    - Info window management

- [ ] **Create marker components**
  - File: `frontend/src/components/travel-planner/MarkerInfoWindow.tsx`
  - Features:
    - Node information display
    - Lock status indication
    - Google Maps link integration
    - Responsive design

**Validation**:
- [ ] Test marker rendering with various node types
- [ ] Verify marker updates on data changes
- [ ] Test info window functionality
- [ ] Validate performance with large marker sets

---

## Phase 5: Integration with TravelPlanner (Week 3)

### 5.1 TravelPlanner Integration
**Objective**: Integrate map into the main planner interface

**Tasks**:
- [ ] **Update `TravelPlanner` component**
  - File: `frontend/src/components/TravelPlanner.tsx`
  - Changes:
    - Add map state management
    - Integrate `TripMap` in right panel
    - Handle map interactions with workflow
    - Implement responsive layout

- [ ] **Create map-workflow synchronization**
  - File: `frontend/src/hooks/useMapWorkflowSync.ts`
  - Features:
    - Sync marker selection with workflow selection
    - Handle day/segment selection → map recentering
    - Manage bidirectional state updates

**Validation**:
- [ ] Test map integration doesn't break existing workflow
- [ ] Verify responsive behavior on different screen sizes
- [ ] Test synchronization between map and workflow
- [ ] Validate performance with complex itineraries

### 5.2 State Management
**Objective**: Implement proper state management for map interactions

**Tasks**:
- [ ] **Extend app state for map data**
  - File: `frontend/src/state/hooks.ts`
  - Add map-specific state management

- [ ] **Create map context**
  - File: `frontend/src/context/MapContext.tsx`
  - Provide map state and actions to child components

**Validation**:
- [ ] Test state persistence across component re-renders
- [ ] Verify state updates trigger proper re-renders
- [ ] Test state synchronization between components

---

## Phase 6: Advanced Features & Optimization (Week 4)

### 6.1 Performance Optimization
**Objective**: Ensure smooth performance with large datasets

**Tasks**:
- [ ] **Implement marker clustering**
  - Use Google Maps marker clustering library
  - Optimize for 200+ markers

- [ ] **Add lazy loading**
  - Load map only when needed
  - Implement progressive enhancement

- [ ] **Optimize re-renders**
  - Use React.memo for map components
  - Implement proper dependency arrays
  - Debounce map updates

**Validation**:
- [ ] Performance testing with large itineraries
- [ ] Memory leak detection
- [ ] Network usage optimization
- [ ] Mobile performance validation

### 6.2 Error Handling & Resilience
**Objective**: Implement comprehensive error handling

**Tasks**:
- [ ] **Create error boundaries**
  - File: `frontend/src/components/travel-planner/MapErrorBoundary.tsx`
  - Graceful degradation when map fails

- [ ] **Implement retry mechanisms**
  - Script loading retries
  - API call retries
  - Network failure handling

- [ ] **Add fallback UI**
  - Static map images as fallback
  - List view when map unavailable
  - Clear error messages

**Validation**:
- [ ] Test error scenarios (network failures, invalid keys, etc.)
- [ ] Verify fallback UI works correctly
- [ ] Test retry mechanisms
- [ ] Validate error reporting

---

## Phase 7: Testing & Quality Assurance (Week 4-5)

### 7.1 Unit Testing
**Objective**: Comprehensive test coverage for all components

**Tasks**:
- [ ] **Test map utilities**
  - File: `frontend/src/utils/__tests__/mapUtils.test.ts`
  - Test coordinate validation, zoom calculations, etc.

- [ ] **Test map components**
  - File: `frontend/src/components/travel-planner/__tests__/`
  - Test component rendering, props handling, event handling

- [ ] **Test backend services**
  - File: `src/test/java/com/tripplanner/service/`
  - Test bounds calculation, coordinate validation, etc.

**Validation**:
- [ ] Achieve >90% test coverage for map-related code
- [ ] All tests pass in CI/CD pipeline
- [ ] Test edge cases and error scenarios

### 7.2 Integration Testing
**Objective**: End-to-end testing of map functionality

**Tasks**:
- [ ] **Create E2E tests**
  - File: `frontend/src/__tests__/e2e/map-integration.test.ts`
  - Test complete user workflows

- [ ] **Test API integration**
  - Verify backend returns correct map data
  - Test error handling for API failures

**Validation**:
- [ ] E2E tests pass in staging environment
- [ ] Test with real Google Maps API
- [ ] Validate cross-browser compatibility

### 7.3 Performance Testing
**Objective**: Ensure performance meets requirements

**Tasks**:
- [ ] **Load testing**
  - Test with large itineraries (200+ nodes)
  - Measure initial load times
  - Test memory usage

- [ ] **Mobile testing**
  - Test on various mobile devices
  - Validate touch interactions
  - Test network conditions

**Validation**:
- [ ] Initial map render < 1.2s desktop, < 2.0s mobile
- [ ] Smooth animations and interactions
- [ ] No memory leaks detected

---

## Phase 8: Deployment & Monitoring (Week 5)

### 8.1 Production Deployment
**Objective**: Deploy to production with proper monitoring

**Tasks**:
- [ ] **Update deployment scripts**
  - File: `cloudbuild.yaml`
  - Add environment variables for production
  - Configure API key restrictions

- [ ] **Set up monitoring**
  - Google Maps API usage monitoring
  - Error tracking for map failures
  - Performance monitoring

**Validation**:
- [ ] Successful deployment to production
- [ ] API keys properly configured
- [ ] Monitoring dashboards active

### 8.2 Documentation & Training
**Objective**: Document the implementation for future maintenance

**Tasks**:
- [ ] **Create technical documentation**
  - API documentation for new endpoints
  - Component documentation
  - Configuration guide

- [ ] **Create user documentation**
  - Map feature guide
  - Troubleshooting guide

**Validation**:
- [ ] Documentation is complete and accurate
- [ ] Team can maintain and extend the feature
- [ ] Users can effectively use map features

---

## Future Scope: Phase 2 - Location/Places Agent (Post-MVP)

### Advanced Location Intelligence
**Objective**: Implement intelligent place discovery and recommendation system

**Planned Features**:
- [ ] **Intelligent Place Discovery**
  - AI-powered place recommendations based on user preferences
  - Context-aware place suggestions (time of day, weather, events)
  - Personalized place ranking and filtering

- [ ] **Advanced Geocoding Services**
  - Address resolution and validation
  - Place ID resolution and caching
  - Reverse geocoding for coordinate-based searches

- [ ] **Rich Place Data Enrichment**
  - Photos, reviews, and detailed place information
  - Real-time place data (hours, availability, pricing)
  - Place categorization and tagging

- [ ] **Location-Based Itinerary Optimization**
  - Route optimization between places
  - Time-based scheduling suggestions
  - Distance and travel time calculations

- [ ] **Place Recommendation Engine**
  - Machine learning-based place suggestions
  - User preference learning and adaptation
  - Collaborative filtering for similar users

**Note**: This phase will be implemented after the core map visualization is complete and stable.

---

## Risk Mitigation & Contingency Plans

### High-Risk Areas
1. **API Key Security**: Implement multiple validation layers
2. **Performance with Large Datasets**: Implement clustering and lazy loading
3. **Cross-Browser Compatibility**: Extensive testing on target browsers
4. **Mobile Performance**: Optimize for mobile constraints

### Contingency Plans
1. **Google Maps API Failures**: Implement fallback to static maps
2. **Performance Issues**: Implement progressive loading and clustering
3. **Security Issues**: Implement key rotation and monitoring
4. **Integration Issues**: Implement feature flags for gradual rollout

---

## Success Metrics

### Technical Metrics
- [ ] Initial map load time < 1.2s (desktop), < 2.0s (mobile)
- [ ] Zero security vulnerabilities in API key handling
- [ ] >90% test coverage for map-related code
- [ ] Zero memory leaks in map components

### User Experience Metrics
- [ ] Smooth camera animations (60fps)
- [ ] Responsive map interactions
- [ ] Clear error messages and fallbacks
- [ ] Intuitive terrain switching

### Business Metrics
- [ ] Increased user engagement with itinerary visualization
- [ ] Reduced support tickets related to map functionality
- [ ] Successful feature adoption rate

---

## Dependencies & Prerequisites

### External Dependencies
- Google Maps JavaScript API access
- Google Places API access
- Valid API keys with proper restrictions
- SSL certificate for production domain

### Internal Dependencies
- Backend `NormalizedItinerary` structure
- Frontend `TravelPlanner` component
- Existing itinerary data with coordinates
- CI/CD pipeline for testing and deployment

---

## Timeline Summary

- **Week 1**: Backend data model and API integration
- **Week 2**: Frontend types, script loading, and core components
- **Week 3**: Integration with TravelPlanner and state management
- **Week 4**: Advanced features, optimization, and testing
- **Week 5**: Deployment, monitoring, and documentation

**Total Estimated Time**: 5 weeks with 1-2 developers

---

## Next Steps

1. **Immediate**: Set up Google Cloud Console projects and API keys
2. **Week 1 Start**: Begin backend schema extensions
3. **Parallel**: Start frontend type system updates
4. **Ongoing**: Regular code reviews and testing at each phase

This roadmap ensures a systematic, error-minimized approach to Google Maps integration while maintaining code quality and user experience standards.
