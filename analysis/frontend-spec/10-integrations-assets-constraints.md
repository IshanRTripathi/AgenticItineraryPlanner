# 10. Third-Party Integrations, Assets & Constraints

**Last Updated:** January 25, 2025  
**Purpose:** Document external integrations, styling system, and redesign constraints

---

## 10.1 Third-Party Integrations

### 10.1.1 Google Maps Integration

**Version:** @googlemaps/markerclusterer 2.6.2

**API Key Configuration:**
```typescript
// Environment variable
VITE_GOOGLE_MAPS_BROWSER_KEY=your_api_key_here

// Usage
const apiKey = import.meta.env.VITE_GOOGLE_MAPS_BROWSER_KEY;
```

**Components Using Maps:**
- `TripMap` - Main map display
- `MarkerInfoWindow` - Location details popup
- `TerrainControl` - Map type switcher
- `MapErrorBoundary` - Error handling

**Features Used:**
- **Markers:** Custom icons for different node types
- **Clustering:** @googlemaps/markerclusterer for performance
- **Info Windows:** Popup details on marker click
- **Bounds Fitting:** Auto-zoom to show all markers
- **Terrain Control:** Switch between roadmap/satellite/terrain
- **Geocoding:** Address ↔ coordinates conversion

**Services:**
- `geocodingService.ts` - Address/coordinate conversion
- `googleMapsLoader.ts` - Async API loading
- `mapUtils.ts` - Map utility functions

**Hooks:**
- `useGoogleMaps()` - API initialization
- `useMapState()` - Map state management

**API Endpoints Used:**
- Maps JavaScript API
- Geocoding API
- Places API (for search)

**Performance Optimizations:**
- Marker clustering for 50+ markers
- Lazy loading of map component
- Debounced map events
- Viewport-based marker rendering

### 10.1.2 Firebase Authentication

**Version:** Firebase 10.13.0

**Configuration:**
```typescript
// firebase.ts
const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID
};

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
```

**Authentication Methods:**
- Google OAuth (primary method)
- Email/Password (not currently used)

**Components:**
- `GoogleSignIn` - Sign-in UI
- `AuthContext` - Auth state provider
- `ProtectedRoute` - Route protection

**Services:**
- `authService.ts` - Firebase wrapper
- `firebaseService.ts` - Firebase initialization

**Auth Flow:**
```
User clicks "Sign in with Google"
      ↓
Firebase popup opens
      ↓
User authenticates with Google
      ↓
Firebase returns ID token
      ↓
POST /auth/google (backend token exchange)
      ↓
Backend returns JWT
      ↓
Store JWT in memory + localStorage
      ↓
Redirect to dashboard
```

**Token Management:**
- ID tokens stored in memory
- Automatic refresh before expiry
- Proactive refresh (5 min before expiry)
- Logout clears all tokens

### 10.1.3 Razorpay Payment Gateway

**Integration:** Razorpay Checkout

**Flow:**
```
User clicks "Pay Now"
      ↓
POST /payments/razorpay/order
      ↓
Receive order_id
      ↓
Open Razorpay modal (third-party UI)
      ↓
User completes payment
      ↓
Razorpay callback with payment_id
      ↓
Verify signature
      ↓
POST /providers/{vertical}/{provider}:book
      ↓
Booking confirmed
```

**Components:**
- `Checkout` - Payment interface
- `BookingConfirmation` - Success page

**Security:**
- Payment signature verification
- Server-side order creation
- No sensitive data in frontend

### 10.1.4 Other Integrations

| Integration | Version | Purpose | Components |
|-------------|---------|---------|------------|
| **ReactFlow** | Latest | Workflow visualization | WorkflowBuilder |
| **Recharts** | 2.15.2 | Data visualization | Charts in overview |
| **i18next** | 25.5.2 | Internationalization | All components |
| **Sonner** | Latest | Toast notifications | Global toasts |
| **date-fns** | Latest | Date manipulation | Formatters |
| **Embla Carousel** | Latest | Image carousels | Media galleries |

---

## 10.2 Assets & Styling

### 10.2.1 Styling Approach

**Framework:** Tailwind CSS 3.x

**Configuration:** `tailwind.config.js`
```javascript
module.exports = {
  content: ['./src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: { /* ... */ },
        secondary: { /* ... */ },
        accent: { /* ... */ }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      spacing: { /* 8px grid */ },
      borderRadius: { /* ... */ },
      boxShadow: { /* ... */ }
    }
  },
  plugins: [
    require('@tailwindcss/forms'),
    require('@tailwindcss/typography'),
  ]
}
```

**Utility Libraries:**
- `class-variance-authority` - Component variants
- `clsx` - Conditional classes
- `tailwind-merge` - Class merging

**Global Styles:** `frontend/src/index.css`
```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  :root {
    --background: 0 0% 100%;
    --foreground: 222.2 84% 4.9%;
    /* ... more CSS variables */
  }
}
```

### 10.2.2 Design Tokens

**Colors:**
```css
/* Primary Colors */
--primary: 222.2 84% 4.9%;           /* Dark blue-gray */
--primary-foreground: 210 40% 98%;   /* Light text */

/* Secondary Colors */
--secondary: 210 40% 96%;            /* Light gray */
--secondary-foreground: 222.2 84% 4.9%;

/* Accent Colors */
--accent: 210 40% 96%;
--accent-foreground: 222.2 84% 4.9%;

/* Status Colors */
--destructive: 0 84.2% 60.2%;        /* Red */
--warning: 38 92% 50%;               /* Orange */
--success: 142 76% 36%;              /* Green */
--info: 221 83% 53%;                 /* Blue */

/* Neutral Colors */
--muted: 210 40% 96.1%;
--muted-foreground: 215.4 16.3% 46.9%;
--border: 214.3 31.8% 91.4%;
--input: 214.3 31.8% 91.4%;
--ring: 222.2 84% 4.9%;
```

**Typography:**
```css
/* Font Families */
font-sans: Inter, system-ui, sans-serif
font-mono: 'Fira Code', monospace

/* Font Sizes */
text-xs: 0.75rem    (12px)
text-sm: 0.875rem   (14px)
text-base: 1rem     (16px)
text-lg: 1.125rem   (18px)
text-xl: 1.25rem    (20px)
text-2xl: 1.5rem    (24px)
text-3xl: 1.875rem  (30px)
text-4xl: 2.25rem   (36px)

/* Font Weights */
font-normal: 400
font-medium: 500
font-semibold: 600
font-bold: 700
```

**Spacing (8px grid):**
```css
0: 0px
1: 0.25rem  (4px)
2: 0.5rem   (8px)
3: 0.75rem  (12px)
4: 1rem     (16px)
5: 1.25rem  (20px)
6: 1.5rem   (24px)
8: 2rem     (32px)
10: 2.5rem  (40px)
12: 3rem    (48px)
16: 4rem    (64px)
20: 5rem    (80px)
24: 6rem    (96px)
```

**Border Radius:**
```css
rounded-none: 0px
rounded-sm: 0.125rem  (2px)
rounded: 0.25rem      (4px)
rounded-md: 0.375rem  (6px)
rounded-lg: 0.5rem    (8px)
rounded-xl: 0.75rem   (12px)
rounded-2xl: 1rem     (16px)
rounded-full: 9999px
```

### 10.2.3 Icon Library

**Library:** Lucide React 0.487.0

**Usage:**
```typescript
import { MapPin, Calendar, Users, ArrowRight } from 'lucide-react';

<MapPin className="h-5 w-5 text-gray-600" />
```

**Common Icons:**
- Navigation: Menu, X, ChevronLeft, ChevronRight, ArrowLeft, ArrowRight
- Actions: Plus, Minus, Edit, Trash, Save, Share, Download
- Status: Check, X, AlertCircle, Info, Loader
- Content: MapPin, Calendar, Users, Clock, DollarSign
- UI: Search, Filter, Settings, MoreVertical, MoreHorizontal

**Icon Sizing:**
```typescript
// Small: h-4 w-4 (16px)
// Medium: h-5 w-5 (20px)
// Large: h-6 w-6 (24px)
// Extra Large: h-8 w-8 (32px)
```

### 10.2.4 Image Assets

**Directory Structure:**
```
public/
├── images/
│   ├── destinations/
│   ├── activities/
│   ├── hotels/
│   └── placeholders/
├── icons/
│   ├── favicon.ico
│   └── logo.svg
└── fonts/
```

**Image Optimization:**
- Lazy loading with `useLazyLoad` hook
- Responsive images with srcset
- WebP format with fallbacks
- Placeholder images during load

**Components:**
- `ImageWithFallback` - Image with error fallback
- `ResponsiveImage` - Responsive image component

---

## 10.3 Internationalization (i18n)

**Library:** i18next 25.5.2

**Supported Languages:**
- English (en) - Primary
- Hindi (hi)
- Telugu (te)
- Bengali (bn)

**Configuration:** `frontend/src/i18n/index.ts`
```typescript
i18n
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: enTranslations },
      hi: { translation: hiTranslations },
      te: { translation: teTranslations },
      bn: { translation: bnTranslations }
    },
    lng: 'en',
    fallbackLng: 'en',
    interpolation: {
      escapeValue: false
    }
  });
```

**Usage:**
```typescript
import { useTranslation } from 'react-i18next';

const { t, i18n } = useTranslation();

// Translate
<h1>{t('navigation.view')}</h1>

// Change language
i18n.changeLanguage('hi');
```

**Translation Files:**
- `frontend/src/i18n/locales/en.json`
- `frontend/src/i18n/locales/hi.json`
- `frontend/src/i18n/locales/te.json`
- `frontend/src/i18n/locales/bn.json`

**Components:**
- `LanguageSelector` - Language switcher dropdown

---

## 10.4 Redesign Constraints & Requirements

### 10.4.1 Functional Constraints

**Must Preserve:**
- ✅ All user workflows (trip creation, editing, booking, sharing)
- ✅ All data operations (CRUD, real-time updates)
- ✅ All integrations (Google Maps, Firebase, Razorpay)
- ✅ All accessibility features
- ✅ Multi-language support (4 languages)
- ✅ Mobile responsiveness
- ✅ Offline capabilities (where applicable)

**Cannot Change:**
- Backend API contracts
- Data structures (TripData, NormalizedItinerary)
- Authentication mechanism
- Payment flow
- WebSocket message formats

### 10.4.2 API Compatibility

**All API Endpoints Must Remain Compatible:**
- Request/response formats unchanged
- Authentication headers preserved
- Error response structures maintained
- WebSocket topic names unchanged
- Query parameters preserved

**Backward Compatibility:**
- Support both TripData and NormalizedItinerary formats
- Adapters must continue to work
- Legacy components can coexist during migration

### 10.4.3 Data Structure Constraints

**Must Support:**
- Both TripData (legacy) and NormalizedItinerary (current) formats
- Bidirectional conversion via adapters
- Change tracking and undo/redo
- Real-time updates via WebSocket
- Optimistic UI updates

**Data Integrity:**
- No data loss during format conversion
- Preserve all node properties
- Maintain graph relationships (edges)
- Keep booking references intact

### 10.4.4 Performance Requirements

**Targets:**
- Initial load time: < 3 seconds
- Time to interactive: < 3 seconds
- Largest Contentful Paint: < 2.5 seconds
- First Input Delay: < 100ms
- Cumulative Layout Shift: < 0.1

**Optimizations to Maintain:**
- Code splitting (lazy loading)
- React Query caching
- Virtual scrolling for large lists
- Marker clustering on maps
- Debounced search/input
- Optimistic updates

### 10.4.5 Browser Compatibility

**Supported Browsers:**
- Chrome (last 2 versions)
- Firefox (last 2 versions)
- Safari (last 2 versions)
- Edge (last 2 versions)
- iOS Safari (last 2 versions)
- Chrome Mobile (last 2 versions)

**Progressive Enhancement:**
- Core functionality works without JavaScript
- Graceful degradation for older browsers
- Polyfills for missing features

### 10.4.6 Accessibility Requirements

**WCAG 2.1 Level AA Compliance:**
- ✅ Keyboard navigation for all interactive elements
- ✅ Screen reader support (ARIA labels)
- ✅ Color contrast ratios (4.5:1 for normal text)
- ✅ Focus indicators visible
- ✅ Alt text for images
- ✅ Form labels and error messages
- ✅ Skip links for navigation

**Keyboard Shortcuts:**
- Must preserve existing shortcuts
- Document all shortcuts
- Provide shortcut help modal

### 10.4.7 Testing Requirements

**Test Coverage:**
- Unit tests for utilities and hooks
- Component tests for UI components
- Integration tests for features
- E2E tests for critical user flows

**Testing Tools:**
- Vitest for unit/component tests
- @testing-library/react for component tests
- MSW for API mocking
- Playwright for E2E tests (planned)

**Critical Flows to Test:**
- Trip creation and generation
- Itinerary editing
- Chat interactions
- Booking flow
- Sharing and export

---

**[← Back to Main Specification](../FRONTEND_UI_REDESIGN_SPECIFICATION.md)** | **[Next: Implementation Guide →](11-implementation-guide.md)**
