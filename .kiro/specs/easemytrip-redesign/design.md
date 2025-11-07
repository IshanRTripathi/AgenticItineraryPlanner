# Design Document: EaseMyTrip Redesign

## Overview

This design document outlines the technical approach, architecture, component structure, and implementation strategy for the EaseMyTrip-inspired redesign of the AI-powered travel itinerary planner.

### Goals

1. Create a comprehensive technical design that addresses all 18 requirements
2. Define clear architecture patterns and component hierarchies
3. Establish data flow and state management strategies
4. Provide implementation guidance for the 18-week development timeline
5. Ensure 100% backend compatibility while delivering premium frontend experience

### Non-Goals

1. This is NOT a requirements document (see requirements.md)
2. This does NOT modify existing backend APIs
3. This does NOT change existing AI agent logic
4. This does NOT include actual implementation code (see tasks.md for implementation)

---

## Architecture

### System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     EASEMYTRIP REDESIGN                         │
│                   (React 18.3.1 + TypeScript)                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        v                     v                     v
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│   HOMEPAGE    │    │  AI PLANNER   │    │   DASHBOARD   │
│   (Public)    │    │  (Protected)  │    │  (Protected)  │
└───────┬───────┘    └───────┬───────┘    └───────┬───────┘
        │                    │                     │
        │                    v                     │
        │            ┌───────────────┐             │
        │            │ AGENT PROGRESS│             │
        │            │   (SSE Real-  │             │
        │            │    time)      │             │
        │            └───────┬───────┘             │
        │                    │                     │
        └────────────────────┼─────────────────────┘
                             │
                             v
                  ┌──────────────────────┐
                  │  UNIFIED TRIP VIEW   │
                  │  (Main Interface)    │
                  │                      │
                  │  ┌────────────────┐  │
                  │  │ View           │  │
                  │  │ Plan           │  │
                  │  │ Bookings (NEW) │  │
                  │  │ Budget         │  │
                  │  │ Packing        │  │
                  │  │ Docs           │  │
                  │  └────────────────┘  │
                  └──────────────────────┘
                             │
                             v
                  ┌──────────────────────┐
                  │  BACKEND APIs        │
                  │  (Existing - Java)   │
                  │                      │
                  │  - Itineraries       │
                  │  - Agents (SSE)      │
                  │  - Bookings (NEW)    │
                  └──────────────────────┘
```

### Technology Stack

**Frontend Framework**:
- React 18.3.1 with TypeScript 5.x
- Vite 6.3.5 (build tool with SWC)
- React Router DOM 6.30.1 (routing)

**State Management**:
- TanStack React Query 5.89.0 (server state)
- Zustand 5.0.8 (client state)
- React Context (auth, itinerary)

**UI Framework**:
- Radix UI (40+ primitives)
- Tailwind CSS 3.x
- Framer Motion 11.x (animations)
- Lucide React 0.487.0 (icons)

**Backend Integration**:
- Axios (HTTP client)
- EventSource (SSE)
- Firebase 10.13.0 (auth)

**Development Tools**:
- TypeScript strict mode
- ESLint + Prettier
- Vitest 1.0.4 (testing)

---

## Component Architecture

### Component Hierarchy

```
App
├── Router
│   ├── PublicRoutes
│   │   ├── HomePage
│   │   │   ├── HeroSection
│   │   │   │   ├── SearchWidget
│   │   │   │   │   ├── FlightSearchForm
│   │   │   │   │   ├── HotelSearchForm
│   │   │   │   │   ├── HolidaySearchForm
│   │   │   │   │   ├── TrainSearchForm
│   │   │   │   │   └── BusSearchForm
│   │   │   │   └── AIPlannerCTA
│   │   │   ├── TrendingDestinations
│   │   │   │   └── DestinationCard[]
│   │   │   ├── PopularRoutes
│   │   │   │   └── RouteCard[]
│   │   │   └── TravelBlogs
│   │   │       └── BlogCard[]
│   │   └── LoginPage
│   │       └── GoogleSignIn
│   │
│   └── ProtectedRoutes
│       ├── Dashboard
│       │   ├── TripList
│       │   │   └── TripCard[]
│       │   └── EmptyState
│       │
│       ├── AIPlanner
│       │   ├── StyledTripWizard (4 steps)
│       │   └── EnhancedAgentProgress
│       │
│       └── UnifiedTripView
│           ├── TripSidebar
│           │   └── SidebarNavItem[]
│           └── MainContent
│               ├── ViewTab (Overview)
│               ├── PlanTab
│               │   ├── DestinationsView
│               │   └── DayByDayView
│               │       └── NodeCard[]
│               ├── BookingsTab (NEW)
│               │   ├── ProviderSidebar
│               │   │   └── ProviderButton[]
│               │   └── ProviderSearchInterface
│               │       ├── SearchForm
│               │       ├── MockResults
│               │       └── BookingModal
│               ├── BudgetTab
│               │   └── CostBreakdown
│               ├── PackingTab
│               │   └── PackingList
│               └── DocsTab
│                   └── DocumentList
```

### Component Design Patterns

**1. Composition Pattern**:
```typescript
// Use composition for flexible layouts
<Card>
  <CardHeader>
    <CardTitle />
  </CardHeader>
  <CardContent>
    {children}
  </CardContent>
  <CardFooter>
    <CardActions />
  </CardFooter>
</Card>
```

**2. Render Props Pattern**:
```typescript
// For flexible rendering
<DataFetcher
  url="/api/itineraries"
  render={({ data, loading, error }) => (
    loading ? <Skeleton /> : <ItineraryView data={data} />
  )}
/>
```

**3. Compound Components**:
```typescript
// For related components
<Tabs>
  <TabsList>
    <TabsTrigger />
  </TabsList>
  <TabsContent />
</Tabs>
```

**4. Higher-Order Components**:
```typescript
// For cross-cutting concerns
const withAuth = (Component) => (props) => {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Component {...props} /> : <Navigate to="/login" />;
};
```

---

## Data Flow & State Management

### State Management Strategy

**Server State (React Query)**:
```typescript
// Itineraries
useQuery(['itinerary', id], () => apiClient.getItinerary(id))
useMutation(apiClient.createItinerary)
useMutation(apiClient.applyChanges)

// Bookings (NEW)
useQuery(['bookings', userId], () => apiClient.getUserBookings(userId))
useMutation(apiClient.createBooking)
```

**Client State (Zustand)**:
```typescript
interface AppStore {
  // UI State
  currentScreen: string;
  sidebarCollapsed: boolean;
  
  // Trip State
  currentTrip: string | null;
  trips: Trip[];
  
  // Auth State
  isAuthenticated: boolean;
  user: User | null;
  
  // Actions
  setCurrentTrip: (id: string) => void;
  addTrip: (trip: Trip) => void;
  removeTrip: (id: string) => void;
}
```

**Context State**:
```typescript
// Auth Context
interface AuthContextType {
  user: User | null;
  signIn: () => Promise<void>;
  signOut: () => Promise<void>;
  getIdToken: () => Promise<string>;
}

// Itinerary Context
interface ItineraryContextType {
  itinerary: NormalizedItinerary;
  updateNode: (nodeId: string, updates: Partial<NormalizedNode>) => void;
  addBooking: (nodeId: string, booking: BookingData) => void;
}
```

### Data Flow Patterns

**1. Unidirectional Data Flow**:
```
User Action → Event Handler → State Update → Re-render
```

**2. Optimistic Updates**:
```typescript
const mutation = useMutation(apiClient.updateNode, {
  onMutate: async (newNode) => {
    // Cancel outgoing refetches
    await queryClient.cancelQueries(['itinerary', id]);
    
    // Snapshot previous value
    const previous = queryClient.getQueryData(['itinerary', id]);
    
    // Optimistically update
    queryClient.setQueryData(['itinerary', id], (old) => ({
      ...old,
      nodes: old.nodes.map(n => n.id === newNode.id ? newNode : n)
    }));
    
    return { previous };
  },
  onError: (err, newNode, context) => {
    // Rollback on error
    queryClient.setQueryData(['itinerary', id], context.previous);
  },
});
```

**3. Real-time Updates (SSE)**:
```typescript
useEffect(() => {
  const eventSource = new EventSource(`/agents/events/${itineraryId}`);
  
  eventSource.onmessage = (event) => {
    const data = JSON.parse(event.data);
    
    if (data.type === 'agent-progress') {
      setProgress(data.progress);
    }
    
    if (data.type === 'agent-complete') {
      queryClient.invalidateQueries(['itinerary', itineraryId]);
    }
  };
  
  return () => eventSource.close();
}, [itineraryId]);
```

---

## Design System Implementation

### CSS Architecture

**File Structure**:
```
src/
├── index.css              # Global styles + design tokens
├── components/
│   └── ui/
│       ├── button.tsx     # Component + styles
│       ├── card.tsx
│       └── ...
└── styles/
    ├── animations.css     # Framer Motion variants
    └── utilities.css      # Custom Tailwind utilities
```

**Design Tokens (src/index.css)**:
```css
:root {
  /* Colors */
  --primary: 207 100% 40%;
  --secondary: 26 100% 55%;
  
  /* Typography */
  --font-primary: 'Inter', sans-serif;
  --text-base: 1rem;
  
  /* Spacing */
  --spacing-md: 1rem;
  
  /* Shadows */
  --shadow-premium-md: 0 8px 24px rgba(0, 112, 218, 0.15);
  
  /* Animations */
  --duration-normal: 300ms;
  --ease-premium: cubic-bezier(0.4, 0, 0.2, 1);
}
```

### Component Styling Strategy

**1. Tailwind Utility Classes** (Primary):
```tsx
<button className="bg-primary hover:bg-primary-hover text-white px-4 py-2 rounded-lg">
  Click me
</button>
```

**2. CSS Modules** (Complex components):
```tsx
import styles from './Component.module.css';

<div className={styles.container}>
  <div className={styles.header} />
</div>
```

**3. Styled Components** (Dynamic styles):
```tsx
const StyledButton = styled.button<{ variant: string }>`
  background: ${props => props.variant === 'primary' ? 'var(--primary)' : 'transparent'};
`;
```

**4. Class Variance Authority** (Variants):
```typescript
const buttonVariants = cva(
  "inline-flex items-center justify-center rounded-md font-medium",
  {
    variants: {
      variant: {
        primary: "bg-primary text-white",
        secondary: "bg-secondary text-white",
      },
      size: {
        sm: "h-9 px-3",
        md: "h-10 px-4",
      },
    },
  }
);
```

---

## API Integration Design

### API Client Architecture

```typescript
// services/apiClient.ts
class APIClient {
  private axios: AxiosInstance;
  
  constructor() {
    this.axios = axios.create({
      baseURL: import.meta.env.VITE_API_BASE_URL,
      timeout: 30000,
    });
    
    // Request interceptor (add auth token)
    this.axios.interceptors.request.use(async (config) => {
      const token = await getIdToken();
      config.headers.Authorization = `Bearer ${token}`;
      return config;
    });
    
    // Response interceptor (handle errors)
    this.axios.interceptors.response.use(
      (response) => response,
      async (error) => {
        if (error.response?.status === 401) {
          await refreshToken();
          return this.axios.request(error.config);
        }
        throw error;
      }
    );
  }
  
  // Itinerary methods
  async getItinerary(id: string): Promise<NormalizedItinerary> {
    const { data } = await this.axios.get(`/itineraries/${id}/json`);
    return data;
  }
  
  async createItinerary(request: CreateItineraryRequest): Promise<ItineraryResponse> {
    const { data } = await this.axios.post('/itineraries', request);
    return data;
  }
  
  async applyChanges(id: string, changes: ChangeSet): Promise<NormalizedItinerary> {
    const { data } = await this.axios.post(`/itineraries/${id}:apply`, changes);
    return data;
  }
  
  // Booking methods (NEW)
  async createBooking(booking: CreateBookingRequest): Promise<BookingRecord> {
    const { data } = await this.axios.post('/bookings', booking);
    return data;
  }
  
  async getUserBookings(userId: string): Promise<BookingRecord[]> {
    const { data} = await this.axios.get(`/bookings/user/${userId}`);
    return data;
  }
}

export const apiClient = new APIClient();
```

### SSE Manager Design

```typescript
// services/sseManager.ts
class SSEManager {
  private connections: Map<string, EventSource> = new Map();
  
  connect(url: string, handlers: SSEHandlers): () => void {
    if (this.connections.has(url)) {
      return () => this.disconnect(url);
    }
    
    const eventSource = new EventSource(url);
    
    eventSource.onopen = () => {
      console.log('SSE connected:', url);
      handlers.onConnect?.();
    };
    
    eventSource.onmessage = (event) => {
      const data = JSON.parse(event.data);
      handlers.onMessage?.(data);
    };
    
    eventSource.onerror = (error) => {
      console.error('SSE error:', error);
      handlers.onError?.(error);
      
      // Reconnect with exponential backoff
      setTimeout(() => {
        this.disconnect(url);
        this.connect(url, handlers);
      }, 1000);
    };
    
    this.connections.set(url, eventSource);
    
    return () => this.disconnect(url);
  }
  
  disconnect(url: string): void {
    const eventSource = this.connections.get(url);
    if (eventSource) {
      eventSource.close();
      this.connections.delete(url);
    }
  }
}

export const sseManager = new SSEManager();
```

---

## Routing & Navigation Design

### Route Structure

```typescript
// App.tsx
<BrowserRouter>
  <Routes>
    {/* Public Routes */}
    <Route path="/" element={<HomePage />} />
    <Route path="/login" element={<LoginPage />} />
    
    {/* Protected Routes */}
    <Route element={<ProtectedRoute />}>
      <Route path="/dashboard" element={<Dashboard />} />
      <Route path="/ai-planner" element={<AIPlanner />} />
      <Route path="/generating" element={<AgentProgress />} />
      <Route path="/trip/:id" element={<UnifiedTripView />} />
    </Route>
    
    {/* Fallback */}
    <Route path="*" element={<NotFound />} />
  </Routes>
</BrowserRouter>
```

### Protected Route Implementation

```typescript
// components/auth/ProtectedRoute.tsx
export function ProtectedRoute() {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();
  
  if (loading) {
    return <LoadingSpinner />;
  }
  
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  
  return <Outlet />;
}
```

---

## Performance Optimization Strategy

### Code Splitting

```typescript
// Lazy load heavy components
const TravelPlanner = lazy(() => import('./components/TravelPlanner'));
const WorkflowBuilder = lazy(() => import('./components/WorkflowBuilder'));
const AgentProgress = lazy(() => import('./components/agents/SimplifiedAgentProgress'));

// Use Suspense
<Suspense fallback={<LoadingSpinner />}>
  <TravelPlanner />
</Suspense>
```

### Image Optimization

```typescript
// Lazy loading
<img loading="lazy" src={imageUrl} alt={alt} />

// Responsive images
<img
  srcSet={`${image}-small.webp 640w, ${image}-large.webp 1920w`}
  sizes="(max-width: 640px) 640px, 1920px"
/>
```

### React Query Caching

```typescript
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      cacheTime: 10 * 60 * 1000, // 10 minutes
      refetchOnWindowFocus: false,
    },
  },
});
```

---

## Testing Strategy

### Test Pyramid

```
        /\
       /  \
      / E2E \
     /______\
    /        \
   /Integration\
  /____________\
 /              \
/  Unit Tests    \
/________________\
```

**Unit Tests** (80% coverage):
- Utility functions
- Custom hooks
- State management
- Data transformations

**Integration Tests**:
- Component interactions
- API integration
- Form submissions
- Navigation flows

**E2E Tests** (Critical paths):
- User registration/login
- AI trip creation
- Booking flow
- Dashboard navigation

---

## Security Considerations

### Authentication

- Firebase JWT tokens
- Automatic token refresh (5 min before expiry)
- Secure token storage (memory only, no localStorage)
- HTTPS only in production

### API Security

- Bearer token authentication
- CORS configuration
- Request rate limiting
- Input validation

### XSS Prevention

- React's built-in XSS protection
- Sanitize user input
- Content Security Policy headers
- No dangerouslySetInnerHTML

---

## Deployment Architecture

### Build Configuration

```typescript
// vite.config.ts
export default defineConfig({
  build: {
    target: 'es2020',
    outDir: 'dist',
    sourcemap: true,
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'ui-vendor': ['@radix-ui/react-dialog', '@radix-ui/react-popover'],
        },
      },
    },
  },
});
```

### Environment Configuration

```
# .env.production
VITE_API_BASE_URL=https://api.production.com
VITE_FIREBASE_API_KEY=xxx
VITE_GOOGLE_MAPS_API_KEY=xxx
```

---

## Design Decisions

### Decision 1: React Query vs Redux

**Choice**: React Query for server state, Zustand for client state

**Rationale**:
- React Query handles server state caching, refetching, optimistic updates
- Zustand is lightweight for UI state
- Avoids Redux boilerplate
- Better TypeScript support

### Decision 2: Radix UI vs Material-UI

**Choice**: Radix UI

**Rationale**:
- Unstyled primitives allow custom EaseMyTrip design
- Better accessibility out of the box
- Smaller bundle size
- More flexible styling

### Decision 3: Framer Motion vs React Spring

**Choice**: Framer Motion

**Rationale**:
- Simpler API for complex animations
- Better TypeScript support
- Declarative animation variants
- Good performance

### Decision 4: Monolithic vs Micro-frontends

**Choice**: Monolithic SPA

**Rationale**:
- Simpler deployment
- Easier state sharing
- Better performance (no iframe overhead)
- Team size doesn't justify micro-frontends

---

## Summary

This design provides a comprehensive technical foundation for the EaseMyTrip redesign, addressing:

- ✅ Clear architecture patterns
- ✅ Component hierarchy and design patterns
- ✅ State management strategy
- ✅ API integration approach
- ✅ Performance optimization
- ✅ Testing strategy
- ✅ Security considerations
- ✅ Deployment architecture

**Next Steps**: Proceed to tasks.md for detailed implementation tasks broken down by week.
