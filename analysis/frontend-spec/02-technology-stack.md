# 2. Technology Stack & Dependencies

**Last Updated:** January 25, 2025  
**Source:** `frontend/package.json`

---

## 2.1 Core Framework & Runtime

| Package | Version | Purpose |
|---------|---------|---------|
| react | 18.3.1 | Core UI framework |
| react-dom | 18.3.1 | React DOM renderer |
| typescript | (via @types/node) | Type safety and developer experience |

**Build System:**
- **Vite** 6.3.5 - Fast build tool and dev server
- **@vitejs/plugin-react-swc** 3.10.2 - React plugin with SWC compiler for faster builds

---

## 2.2 Routing & Navigation

| Package | Version | Purpose |
|---------|---------|---------|
| react-router-dom | 6.30.1 | Client-side routing and navigation |

**Features Used:**
- Route-based code splitting
- Protected routes with authentication
- Programmatic navigation
- URL parameters and query strings
- Nested routes
- Route guards

---

## 2.3 State Management

| Package | Version | Purpose |
|---------|---------|---------|
| @tanstack/react-query | 5.89.0 | Server state management, caching, and synchronization |
| zustand | 5.0.8 | Client-side state management |

**State Management Architecture:**
- **React Query**: API data fetching, caching, background refetching, optimistic updates
- **Zustand**: Global UI state (currentTrip, authentication, screen navigation)
- **React Context**: Feature-specific state (AuthContext, UnifiedItineraryContext)
- **Local State**: Component-specific UI state (useState)

---

## 2.4 UI Component Library

### Radix UI Primitives (Headless Components)

| Package | Version | Purpose |
|---------|---------|---------|
| @radix-ui/react-accordion | 1.2.3 | Collapsible content sections |
| @radix-ui/react-alert-dialog | 1.1.6 | Modal dialogs for critical actions |
| @radix-ui/react-aspect-ratio | 1.1.2 | Aspect ratio containers |
| @radix-ui/react-avatar | 1.1.3 | User avatar display |
| @radix-ui/react-checkbox | 1.1.4 | Checkbox inputs |
| @radix-ui/react-collapsible | 1.1.3 | Collapsible content |
| @radix-ui/react-context-menu | 2.2.6 | Right-click context menus |
| @radix-ui/react-dialog | 1.1.6 | Modal dialogs |
| @radix-ui/react-dropdown-menu | 2.1.6 | Dropdown menus |
| @radix-ui/react-hover-card | 1.1.6 | Hover cards for additional info |
| @radix-ui/react-label | 2.1.2 | Form labels |
| @radix-ui/react-menubar | 1.1.6 | Menu bars |
| @radix-ui/react-navigation-menu | 1.2.5 | Navigation menus |
| @radix-ui/react-popover | 1.1.6 | Popover overlays |
| @radix-ui/react-progress | 1.1.2 | Progress indicators |
| @radix-ui/react-radio-group | 1.2.3 | Radio button groups |
| @radix-ui/react-scroll-area | 1.2.3 | Custom scrollable areas |
| @radix-ui/react-select | 2.1.6 | Select dropdowns |
| @radix-ui/react-separator | 1.1.2 | Visual separators |
| @radix-ui/react-slider | 1.2.3 | Range sliders |
| @radix-ui/react-slot | 1.1.2 | Composition utility |
| @radix-ui/react-switch | 1.1.3 | Toggle switches |
| @radix-ui/react-tabs | 1.1.3 | Tab navigation |
| @radix-ui/react-toggle | 1.1.2 | Toggle buttons |
| @radix-ui/react-toggle-group | 1.1.2 | Toggle button groups |
| @radix-ui/react-tooltip | 1.1.8 | Tooltips |

**Total:** 26 Radix UI primitives providing accessible, unstyled components

---

## 2.5 Styling & Design

| Package | Version | Purpose |
|---------|---------|---------|
| tailwindcss | (peer dependency) | Utility-first CSS framework |
| class-variance-authority | 0.7.1 | Component variant management |
| clsx | * | Conditional class name composition |
| tailwind-merge | * | Tailwind class merging utility |
| next-themes | 0.4.6 | Theme switching (dark mode support) |
| lucide-react | 0.487.0 | Icon library (tree-shakeable) |

---

## 2.6 Forms & Validation

| Package | Version | Purpose |
|---------|---------|---------|
| react-hook-form | 7.55.0 | Form state management and validation |
| react-day-picker | 8.10.1 | Date picker component |
| input-otp | 1.4.2 | OTP input component |

---

## 2.7 Data Visualization & UI Components

| Package | Version | Purpose |
|---------|---------|---------|
| recharts | 2.15.2 | Chart and data visualization library |
| reactflow | * | Workflow and node-based visualization |
| embla-carousel-react | 8.6.0 | Carousel component |
| react-resizable-panels | 2.1.7 | Resizable panel layouts |
| cmdk | 1.1.1 | Command palette component |
| vaul | 1.1.2 | Drawer component |

---

## 2.8 Maps & Location Services

| Package | Version | Purpose |
|---------|---------|---------|
| @googlemaps/markerclusterer | 2.6.2 | Google Maps marker clustering |

**Note:** Google Maps JavaScript API is loaded dynamically via `googleMapsLoader.ts`

---

## 2.9 Authentication & Real-time Communication

| Package | Version | Purpose |
|---------|---------|---------|
| firebase | 10.13.0 | Authentication (Google Sign-In) |
| @stomp/stompjs | 7.2.0 | STOMP protocol for WebSocket |
| sockjs-client | 1.6.1 | WebSocket fallback |

**Note:** Primary real-time communication uses Server-Sent Events (SSE), not WebSocket

---

## 2.10 Internationalization

| Package | Version | Purpose |
|---------|---------|---------|
| i18next | 25.5.2 | Internationalization framework |
| react-i18next | 15.7.3 | React bindings for i18next |
| i18next-browser-languagedetector | 8.2.0 | Automatic language detection |

**Supported Languages:**
- English (en)
- Hindi (hi)
- Bengali (bn)
- Telugu (te)

**Translation Files:** `frontend/src/i18n/locales/*.json`

---

## 2.11 Utilities & Helpers

| Package | Version | Purpose |
|---------|---------|---------|
| date-fns | * | Date manipulation and formatting |
| sonner | 2.0.3 | Toast notifications |

---

## 2.12 Development Dependencies

### Testing

| Package | Version | Purpose |
|---------|---------|---------|
| vitest | 1.0.4 | Test runner (Vite-native) |
| @vitest/ui | 1.0.4 | Vitest UI for test visualization |
| @testing-library/react | * | React component testing utilities |
| @testing-library/jest-dom | 6.1.4 | Custom Jest matchers for DOM |
| @testing-library/user-event | 14.5.1 | User interaction simulation |
| jsdom | 23.0.1 | DOM implementation for testing |
| fetch-mock | 9.11.0 | HTTP request mocking |

### TypeScript

| Package | Version | Purpose |
|---------|---------|---------|
| @types/node | 20.10.0 | Node.js type definitions |
| @types/jest | 29.5.8 | Jest type definitions |
| @types/sockjs-client | 1.5.4 | SockJS type definitions |

---

## 2.13 Build Scripts

```json
{
  "dev": "vite",                    // Development server
  "build": "vite build",            // Production build
  "test": "vitest",                 // Run tests in watch mode
  "test:ui": "vitest --ui",         // Run tests with UI
  "test:run": "vitest run"          // Run tests once
}
```

---

## 2.14 Environment Variables

**File:** `frontend/.env.local`

| Variable | Purpose | Required |
|----------|---------|----------|
| VITE_API_BASE_URL | Backend API base URL | Yes |
| VITE_GOOGLE_MAPS_BROWSER_KEY | Google Maps API key | Yes |
| VITE_DEBUG | Debug mode flag | No |

**Firebase Configuration:**
Firebase credentials are configured directly in `frontend/src/config/firebase.ts` rather than environment variables. This includes:
- API Key
- Auth Domain
- Project ID
- Storage Bucket
- Messaging Sender ID
- App ID

**Note:** All environment variables are prefixed with `VITE_` to be exposed to the client-side code.

---

## 2.15 Browser Compatibility

**Target Browsers:**
- Chrome (last 2 versions)
- Firefox (last 2 versions)
- Safari (last 2 versions)
- Edge (last 2 versions)
- iOS Safari (last 2 versions)
- Chrome Mobile (last 2 versions)

**Polyfills:** None explicitly configured (relying on Vite's default handling)

---

## 2.16 Dependency Analysis

### Core Dependencies (Production)

**Total:** 47 production dependencies

**Categories:**
- **UI Components:** 26 Radix UI primitives + 6 additional UI libraries (cmdk, vaul, embla-carousel-react, react-resizable-panels, recharts, reactflow)
- **State Management:** 2 libraries (React Query, Zustand)
- **Routing:** 1 library (React Router)
- **Authentication:** 1 library (Firebase)
- **Real-time Communication:** 2 libraries (STOMP, SockJS)
- **Maps:** 1 library (Google Maps Marker Clusterer)
- **Internationalization:** 3 libraries (i18next ecosystem)
- **Forms:** 3 libraries (react-hook-form, react-day-picker, input-otp)
- **Styling:** 5 libraries (class-variance-authority, clsx, tailwind-merge, next-themes, lucide-react)
- **Utilities:** 2 libraries (date-fns, sonner)
- **Core:** 2 libraries (react, react-dom)

### Development Dependencies

**Total:** 11 dev dependencies

**Categories:**
- **Testing:** 7 packages (Vitest, Testing Library, mocks)
- **Build Tools:** 2 packages (Vite, React plugin)
- **Type Definitions:** 3 packages (@types/*)

---

## 2.17 Bundle Size Considerations

**Lazy-Loaded Components:**
- TravelPlanner
- WorkflowBuilder
- SimplifiedAgentProgress
- SimplifiedTripWizard
- CostAndCart
- Checkout
- BookingConfirmation
- ShareView
- TripDashboard
- ItineraryWithChat

**Eager-Loaded Components:**
- LandingPage
- LoginPage
- GoogleSignIn
- Core routing components

**Strategy:** Heavy components are lazy-loaded to optimize initial bundle size and improve Time to Interactive (TTI).

---

## 2.18 Dependency Update Considerations

**Packages with Wildcard Versions (*):**
- @testing-library/react
- clsx
- date-fns
- reactflow
- tailwind-merge

**Recommendation:** Pin these to specific versions for reproducible builds.

**Packages with Caret (^) Versions:**
- All other packages use caret ranges, allowing minor and patch updates

**Security:** Regular dependency audits recommended via `npm audit` or `yarn audit`.

---

**[← Back to Main Specification](../FRONTEND_UI_REDESIGN_SPECIFICATION.md)** | **[Next: Backend API Integration →](03-backend-api-integration.md)**
