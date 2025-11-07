# Requirements 13-18: Technical Requirements

**Design Philosophy**: Apple.com refinement + Emirates.com luxury + EaseMyTrip functionality  
**Design System**: Material 3 + Apple HIG + Atlassian principles

**📍 Source**: This document extracts Requirements 13-18 from the main [requirements.md](requirements.md) file.

**📦 Contents**:
- Requirement 13: Provider Configuration
- Requirement 14: Analytics and Tracking
- Requirement 15: Performance Optimization (Lighthouse ≥90, 60fps)
- Requirement 16: Accessibility (WCAG 2.1 Level AA)
- Requirement 17: Error Handling and Edge Cases
- Requirement 18: Testing Requirements

## 🎨 Premium Design Standards (Apply to All Requirements)

- **Layout**: 12-column grid, 8px spacing increments
- **Colors**: Primary #002B5B, Secondary #F5C542, Neutrals #F8F8F8/#E0E0E0
- **Typography**: Inter/SF Pro Display, 16px body, 1.5 line-height
- **Elevation**: 3 layers (elevation-1, elevation-2, elevation-3)
- **Motion**: 300ms cubic-bezier(0.4,0,0.2,1), 60fps target
- **Radius**: Max 12px (no over-rounding)
- **Touch Targets**: Min 48x48px

---

## Requirement 13: Provider Configuration

**📍 Main file location**: Lines 934-962

**User Story:** As a developer, I want provider URLs and logos to be configurable, so that they can be easily updated without code changes.

### Summary

Centralized provider configuration:
- **Configuration File**: `frontend/src/config/providers.ts`
- **Logo Assets**: `frontend/public/assets/providers/`
- **URL Templates**: With placeholders for parameters
- **Easy Updates**: Replace files without code changes

### Configuration Structure

```typescript
// frontend/src/config/providers.ts
export interface Provider {
  id: string;
  name: string;
  logo: string; // Path relative to public folder
  urlTemplate: string; // With {placeholders}
  verticals: string[]; // ['hotel', 'flight', 'activity']
  active: boolean;
}

export const providers: Provider[] = [
  {
    id: 'booking-com',
    name: 'Booking.com',
    logo: '/assets/providers/booking.png',
    urlTemplate: 'https://www.booking.com/searchresults.html?ss={location}&checkin={checkin}&checkout={checkout}&group_adults={adults}',
    verticals: ['hotel'],
    active: true
  },
  {
    id: 'expedia',
    name: 'Expedia',
    logo: '/assets/providers/expedia.png',
    urlTemplate: 'https://www.expedia.com/Hotel-Search?destination={location}&startDate={checkin}&endDate={checkout}&rooms={rooms}',
    verticals: ['hotel', 'flight'],
    active: true
  },
  {
    id: 'airbnb',
    name: 'Airbnb',
    logo: '/assets/providers/airbnb.png',
    urlTemplate: 'https://www.airbnb.com/s/{location}/homes?checkin={checkin}&checkout={checkout}&adults={adults}',
    verticals: ['hotel'],
    active: true
  },
  {
    id: 'agoda',
    name: 'Agoda',
    logo: '/assets/providers/agoda.png',
    urlTemplate: 'https://www.agoda.com/search?city={location}&checkIn={checkin}&checkOut={checkout}&rooms={rooms}&adults={adults}',
    verticals: ['hotel'],
    active: true
  }
];
```

### URL Construction

```typescript
// Utility function to construct provider URL
export function constructProviderUrl(
  provider: Provider,
  params: {
    location: string;
    checkin?: string;
    checkout?: string;
    adults?: number;
    children?: number;
    rooms?: number;
  }
): string {
  let url = provider.urlTemplate;
  
  // Replace placeholders
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined) {
      url = url.replace(`{${key}}`, encodeURIComponent(String(value)));
    }
  });
  
  return url;
}
```

### Logo Management

**Directory Structure**:
```
frontend/public/assets/providers/
├── booking-placeholder.png
├── expedia-placeholder.png
├── airbnb-placeholder.png
├── agoda-placeholder.png
└── README.md (instructions for replacing)
```

**Logo Specifications**:
- Format: PNG with transparency
- Size: 200x200px (will be scaled)
- Display sizes:
  - Lists: 40x40px
  - Modals: 60x60px
  - Cards: 48x48px

**Fallback**:
- If logo fails to load, show provider name text
- Generic travel icon as backup

---

## Requirement 14: Analytics and Tracking

**📍 Main file location**: Lines 963-988

**User Story:** As a product manager, I want to track user interactions with providers and bookings, so that I can understand user behavior and optimize the experience.

### Summary

Comprehensive analytics tracking:
- **Event Tracking**: User actions and interactions
- **Provider Analytics**: Booking attempts and completions
- **Performance Metrics**: Load times and errors
- **User Journey**: Flow through application

### Event Categories

**1. Booking Events**:
```typescript
// Booking initiated
analytics.track('booking_initiated', {
  nodeId: string,
  nodeType: 'hotel' | 'flight' | 'activity',
  providerName: string,
  itineraryId: string,
  timestamp: Date
});

// Provider iframe loaded
analytics.track('provider_iframe_loaded', {
  providerName: string,
  loadTime: number, // milliseconds
  nodeType: string,
  timestamp: Date
});

// Booking confirmed
analytics.track('booking_confirmed', {
  providerName: string,
  confirmationNumber: string,
  nodeType: string,
  itineraryId: string,
  amount: number,
  currency: string,
  timestamp: Date
});
```

**2. Search Events**:
```typescript
// Search performed
analytics.track('search_performed', {
  searchType: 'flight' | 'hotel' | 'holiday' | 'train' | 'bus',
  origin: string,
  destination: string,
  dates: { start: string, end: string },
  travelers: number,
  timestamp: Date
});
```

**3. AI Events**:
```typescript
// AI trip created
analytics.track('ai_trip_created', {
  destination: string,
  duration: number, // days
  travelers: number,
  budget: 'budget' | 'moderate' | 'luxury',
  timestamp: Date
});

// Agent progress
analytics.track('agent_progress', {
  itineraryId: string,
  progress: number, // 0-100
  currentTask: string,
  timestamp: Date
});
```

**4. Navigation Events**:
```typescript
// Page view
analytics.track('page_view', {
  path: string,
  referrer: string,
  timestamp: Date
});

// Feature used
analytics.track('feature_used', {
  feature: string,
  context: object,
  timestamp: Date
});
```

### Analytics Integration

**Google Analytics 4**:
```typescript
// Initialize
import ReactGA from 'react-ga4';
ReactGA.initialize('G-XXXXXXXXXX');

// Track page views
useEffect(() => {
  ReactGA.send({ hitType: 'pageview', page: location.pathname });
}, [location]);
```

**Custom Analytics Service**:
```typescript
// frontend/src/services/analytics.ts
class AnalyticsService {
  track(event: string, properties: object) {
    // Send to backend
    fetch('/api/analytics/events', {
      method: 'POST',
      body: JSON.stringify({ event, properties, timestamp: new Date() })
    });
    
    // Send to Google Analytics
    ReactGA.event({ category: 'User', action: event, ...properties });
  }
  
  identify(userId: string, traits: object) {
    // Identify user
  }
  
  page(path: string) {
    // Track page view
  }
}

export const analytics = new AnalyticsService();
```

---

## Requirement 15: Performance Optimization

**📍 Main file location**: Lines 989-1019

**User Story:** As a user, I want the application to load quickly and respond smoothly, so that I have a pleasant experience.

### Summary

Performance targets:
- **Initial Load**: ≤2 seconds
- **Time to Interactive**: ≤3 seconds
- **Lighthouse Score**: ≥90
- **Animation FPS**: 60fps

### Optimization Strategies

**1. Code Splitting**:
```typescript
// Lazy load routes
const TravelPlanner = lazy(() => import('./components/TravelPlanner'));
const WorkflowBuilder = lazy(() => import('./components/WorkflowBuilder'));
const SimplifiedAgentProgress = lazy(() => import('./components/agents/SimplifiedAgentProgress'));

// Wrap in Suspense
<Suspense fallback={<LoadingSpinner />}>
  <TravelPlanner />
</Suspense>
```

**2. Image Optimization**:
```typescript
// Lazy loading
<img loading="lazy" src={imageUrl} alt={alt} />

// Responsive images
<img
  srcSet={`${image}-small.webp 640w, ${image}-medium.webp 1024w, ${image}-large.webp 1920w`}
  sizes="(max-width: 640px) 640px, (max-width: 1024px) 1024px, 1920px"
  src={`${image}-medium.webp`}
  alt={alt}
/>

// WebP with fallback
<picture>
  <source srcSet={`${image}.webp`} type="image/webp" />
  <img src={`${image}.jpg`} alt={alt} />
</picture>
```

**3. React Query Caching**:
```typescript
// Configure cache times
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      cacheTime: 10 * 60 * 1000, // 10 minutes
      refetchOnWindowFocus: false,
      retry: 3
    }
  }
});
```

**4. Bundle Optimization**:
```typescript
// vite.config.ts
export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'ui-vendor': ['@radix-ui/react-dialog', '@radix-ui/react-popover'],
          'maps': ['@googlemaps/markerclusterer'],
          'charts': ['recharts']
        }
      }
    },
    chunkSizeWarningLimit: 1000
  }
});
```

**5. Animation Performance**:
```css
/* Use transform and opacity only */
.animated-element {
  will-change: transform, opacity;
  transform: translateZ(0); /* Force GPU acceleration */
}

/* Avoid layout thrashing */
.avoid {
  /* Don't animate: width, height, top, left, margin, padding */
}
```

**6. Debouncing and Throttling**:
```typescript
// Debounce search input
const debouncedSearch = useMemo(
  () => debounce((value: string) => {
    performSearch(value);
  }, 300),
  []
);

// Throttle scroll handler
const throttledScroll = useMemo(
  () => throttle(() => {
    handleScroll();
  }, 100),
  []
);
```

---

## Requirement 16: Accessibility

**📍 Main file location**: Lines 1020-1045

**User Story:** As a user with disabilities, I want the application to be accessible, so that I can use all features independently.

### Summary

WCAG 2.1 Level AA compliance:
- **Keyboard Navigation**: All interactive elements
- **Screen Reader Support**: ARIA labels and announcements
- **Color Contrast**: ≥4.5:1 for text
- **Focus Indicators**: Visible on all elements

### Accessibility Checklist

**1. Keyboard Navigation**:
- [ ] Tab through all interactive elements
- [ ] Enter/Space activates buttons
- [ ] Escape closes modals
- [ ] Arrow keys navigate lists
- [ ] Focus trap in modals
- [ ] Skip to main content link

**2. Screen Reader Support**:
```tsx
// ARIA labels
<button aria-label="Close modal">
  <X />
</button>

// ARIA descriptions
<input
  aria-describedby="email-error"
  aria-invalid={hasError}
/>
<span id="email-error" role="alert">
  {errorMessage}
</span>

// Live regions
<div aria-live="polite" aria-atomic="true">
  {statusMessage}
</div>

// Landmarks
<nav aria-label="Main navigation">
<main>
<aside aria-label="Filters">
```

**3. Color Contrast**:
- Text on white: ≥4.5:1
- Large text (≥18px): ≥3:1
- Interactive elements: ≥3:1
- Use tools: WebAIM Contrast Checker

**4. Focus Indicators**:
```css
/* Visible focus */
*:focus-visible {
  outline: 2px solid var(--easemytrip-primary);
  outline-offset: 2px;
}

/* Custom focus for buttons */
.button:focus-visible {
  box-shadow: 0 0 0 3px rgba(0, 112, 218, 0.3);
}
```

**5. Form Accessibility**:
```tsx
// Associated labels
<label htmlFor="email">Email</label>
<input id="email" type="email" />

// Error messages
<input aria-describedby="email-error" />
<span id="email-error" role="alert">Invalid email</span>

// Required fields
<input required aria-required="true" />

// Autocomplete
<input autocomplete="email" />
```

**6. Image Alt Text**:
```tsx
// Descriptive alt
<img src="paris.jpg" alt="Eiffel Tower at sunset" />

// Decorative images
<img src="decoration.svg" alt="" role="presentation" />
```

---

## Requirement 17: Error Handling and Edge Cases

**📍 Main file location**: Lines 1046-1075

**User Story:** As a user, I want clear error messages and graceful handling of failures, so that I understand what went wrong and how to fix it.

### Summary

Comprehensive error handling:
- **API Errors**: Retry logic and user-friendly messages
- **Network Errors**: Offline detection and queuing
- **Validation Errors**: Inline field errors
- **Iframe Errors**: Fallback providers

### Error Handling Strategies

**1. API Error Handling**:
```typescript
// API client with retry
async function apiCall(endpoint: string, options: RequestInit) {
  let lastError;
  
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const response = await fetch(endpoint, options);
      
      if (!response.ok) {
        if (response.status === 401) {
          // Refresh token and retry
          await refreshToken();
          continue;
        }
        throw new Error(`HTTP ${response.status}`);
      }
      
      return await response.json();
    } catch (error) {
      lastError = error;
      await delay(Math.pow(2, attempt) * 1000); // Exponential backoff
    }
  }
  
  throw lastError;
}
```

**2. Error Messages**:
```typescript
// User-friendly error messages
const errorMessages = {
  'NETWORK_ERROR': 'Connection failed. Please check your internet and try again.',
  'TIMEOUT': 'Request timed out. Please try again.',
  'VALIDATION_ERROR': 'Please check your input and try again.',
  'SERVER_ERROR': 'Something went wrong on our end. Please try again later.',
  'NOT_FOUND': 'The requested resource was not found.',
  'UNAUTHORIZED': 'Please sign in to continue.',
  'FORBIDDEN': 'You don\'t have permission to access this resource.'
};
```

**3. Error UI Components**:
```tsx
// Error modal
<ErrorModal
  isOpen={hasError}
  title="Oops! Something went wrong"
  message={errorMessage}
  onRetry={handleRetry}
  onClose={handleClose}
/>

// Inline error
<FormField error={fieldError}>
  <Input />
  {fieldError && (
    <ErrorMessage>
      <AlertCircle /> {fieldError}
    </ErrorMessage>
  )}
</FormField>

// Toast notification
toast.error('Failed to save changes', {
  action: {
    label: 'Retry',
    onClick: handleRetry
  }
});
```

**4. Iframe Error Handling**:
```typescript
// Detect iframe load failure
<iframe
  src={providerUrl}
  onError={() => {
    setIframeError(true);
    showErrorModal({
      title: 'Failed to load provider',
      message: 'Please try another provider or contact support.',
      actions: [
        { label: 'Try Another Provider', onClick: showProviderSelection },
        { label: 'Contact Support', onClick: openSupport }
      ]
    });
  }}
/>
```

**5. Network Status**:
```typescript
// Detect offline
const [isOnline, setIsOnline] = useState(navigator.onLine);

useEffect(() => {
  const handleOnline = () => setIsOnline(true);
  const handleOffline = () => setIsOnline(false);
  
  window.addEventListener('online', handleOnline);
  window.addEventListener('offline', handleOffline);
  
  return () => {
    window.removeEventListener('online', handleOnline);
    window.removeEventListener('offline', handleOffline);
  };
}, []);

// Show offline indicator
{!isOnline && (
  <OfflineBanner>
    You're offline. Some features may not work.
  </OfflineBanner>
)}
```

---

## Requirement 18: Testing Requirements

**📍 Main file location**: Lines 1076-1197

**User Story:** As a developer, I want comprehensive tests, so that I can confidently make changes without breaking functionality.

### Summary

Testing strategy:
- **Unit Tests**: ≥80% coverage for utilities
- **Component Tests**: ≥70% coverage for components
- **Integration Tests**: Critical user flows
- **E2E Tests**: Booking flow (optional)

### Testing Stack

**Vitest** for unit and integration tests:
```typescript
// vitest.config.ts
export default defineConfig({
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: ['node_modules/', 'src/test/']
    }
  }
});
```

**React Testing Library** for component tests:
```typescript
// Example component test
import { render, screen, fireEvent } from '@testing-library/react';
import { SearchWidget } from './SearchWidget';

describe('SearchWidget', () => {
  it('renders all tabs', () => {
    render(<SearchWidget />);
    expect(screen.getByText('Flights')).toBeInTheDocument();
    expect(screen.getByText('Hotels')).toBeInTheDocument();
  });
  
  it('switches tabs on click', () => {
    render(<SearchWidget />);
    fireEvent.click(screen.getByText('Hotels'));
    expect(screen.getByLabelText('Location')).toBeInTheDocument();
  });
  
  it('validates form before submit', async () => {
    render(<SearchWidget />);
    fireEvent.click(screen.getByText('Search'));
    expect(await screen.findByText('Please select a destination')).toBeInTheDocument();
  });
});
```

**Playwright** for E2E tests (optional):
```typescript
// e2e/booking-flow.spec.ts
import { test, expect } from '@playwright/test';

test('complete booking flow', async ({ page }) => {
  // Navigate to homepage
  await page.goto('/');
  
  // Search for hotel
  await page.click('text=Hotels');
  await page.fill('[placeholder="Enter location"]', 'Paris');
  await page.fill('[placeholder="Check-in"]', '2025-06-01');
  await page.fill('[placeholder="Check-out"]', '2025-06-05');
  await page.click('text=Search Hotels');
  
  // Select result
  await page.waitForSelector('.hotel-card');
  await page.click('.hotel-card >> text=Book Now');
  
  // Select provider
  await page.click('text=Booking.com');
  
  // Wait for iframe
  await page.waitForSelector('iframe');
  
  // Wait for mock confirmation
  await page.waitForSelector('text=Booking Confirmed', { timeout: 5000 });
  
  // Verify confirmation
  expect(await page.textContent('.confirmation-number')).toMatch(/EMT[A-Z0-9]{9}/);
});
```

### Test Coverage Goals

**Unit Tests** (≥80%):
- Utility functions
- Data transformations
- Validation logic
- API client methods
- State management

**Component Tests** (≥70%):
- Rendering with props
- User interactions
- State changes
- Conditional rendering
- Error states

**Integration Tests**:
- Form submission flows
- API integration
- SSE connections
- Navigation flows
- State persistence

**E2E Tests** (Critical paths):
- User registration/login
- AI trip creation
- Booking flow
- Search and results
- Dashboard navigation

---

## Implementation Checklist

### Phase 1: Configuration & Analytics (Week 14)
- [ ] Create provider configuration file
- [ ] Add provider logos (placeholders)
- [ ] Implement URL construction utility
- [ ] Set up analytics service
- [ ] Add event tracking
- [ ] Test analytics integration

### Phase 2: Performance (Week 15)
- [ ] Implement code splitting
- [ ] Optimize images (WebP, lazy loading)
- [ ] Configure React Query caching
- [ ] Optimize bundle size
- [ ] Test Lighthouse score
- [ ] Verify 60fps animations

### Phase 3: Accessibility (Week 16)
- [ ] Add ARIA labels
- [ ] Implement keyboard navigation
- [ ] Test with screen reader
- [ ] Verify color contrast
- [ ] Add focus indicators
- [ ] Test with keyboard only

### Phase 4: Error Handling (Week 17)
- [ ] Implement retry logic
- [ ] Add error messages
- [ ] Create error UI components
- [ ] Handle iframe errors
- [ ] Add offline detection
- [ ] Test error scenarios

### Phase 5: Testing (Week 18)
- [ ] Write unit tests
- [ ] Write component tests
- [ ] Write integration tests
- [ ] Set up E2E tests (optional)
- [ ] Achieve coverage goals
- [ ] Run CI/CD pipeline

---

**📖 For complete implementation details, refer to [requirements.md](requirements.md)**
