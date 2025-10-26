# Requirements Document: EaseMyTrip-Inspired Redesign

## Introduction

This specification defines the comprehensive requirements for redesigning the existing AI-powered travel itinerary planner to match the visual design, user experience, and feature set of EaseMyTrip.com, while maintaining 100% of existing AI functionality and backend integrations. The redesign aims to create a "million-dollar website" with premium UI/UX, high-energy animations, comprehensive booking capabilities, and pixel-perfect implementation of the EaseMyTrip design language.

### Project Context

**Current Application**: AI-powered travel itinerary planner with:
- AI-driven trip generation using multiple specialized agents (SkeletonPlannerAgent, etc.)
- Day-by-day itinerary planning with drag-and-drop functionality
- Map integration using Google Maps with marker clustering
- Workflow visualization using ReactFlow
- Real-time agent progress tracking via Server-Sent Events (SSE)
- Firebase authentication with Google Sign-In
- React Query for server state management
- Zustand for client state management
- Comprehensive itinerary editing with propose/apply/undo operations
- Revision history and rollback capabilities
- Chat interface for AI-assisted modifications
- Export to PDF and email sharing
- Multi-language support (en, hi, bn, te)

**Target Design**: EaseMyTrip.com-inspired interface with:
- Multi-tab search widget (Flights, Hotels, Holidays, Trains, Bus) with real-time validation
- Trending destinations grid with hover effects and pricing
- Popular flight routes carousel with airline logos
- Travel blogs section with featured articles
- Embedded provider booking via iframes (Booking.com, Expedia, Airbnb, Agoda)
- Premium animations and micro-interactions on every interactive element
- Unified trip management interface with sidebar navigation
- Comprehensive booking management system
- Budget tracking and visualization
- Smart packing list generation
- Travel documents management
- Mobile-responsive design with touch gestures

**Design References**: Screenshots located in `analysis/frontend-spec/screenshots/`:
- `homepageflightroutessuggestions.png` - Homepage layout with search widget and flight routes
- `homepagetrendingtouristdestinations.png` - Trending destinations grid layout
- `flightbookingpage.png` - Flight search results and booking interface
- `hotelbookingpage.png` - Hotel search results with filters and booking
- `activitysearchpage.png` - Activity/attraction search interface
- `itinerarysummarypage.png` - Trip overview with statistics
- `itinerarysummarypage2.png` - Detailed itinerary view
- `daybydaycardview.png` - Day-by-day cards with activities
- `sidebarnavigationbookings_section_bookhotels.png` - Sidebar navigation structure
- `userdashboardwithnotrips.png` - Empty state dashboard
- `login_signuppage.png` - Authentication page layout
- `travelblogs.png` - Blog section design
- `currentuiofapplicationwith3sections.png` - Current application structure for reference

### Key Technical Stack (Preserved and Enhanced)

**Core Framework**:
- React 18.3.1 with TypeScript 5.x
- Vite 6.3.5 build tool with SWC compiler
- React Router DOM 6.30.1 for routing

**State Management**:
- TanStack React Query 5.89.0 for server state (caching, background refetching)
- Zustand 5.0.8 for client state (UI state, current trip)
- React Context API for auth and itinerary state

**UI Components**:
- Radix UI component library (40+ primitives: Dialog, Popover, Tooltip, etc.)
- Tailwind CSS 3.x for styling with custom configuration
- class-variance-authority 0.7.1 for component variants
- Lucide React 0.487.0 for icons

**Authentication & Backend**:
- Firebase 10.13.0 for authentication (Google Sign-In)
- Axios-based API client with retry logic and token management
- Server-Sent Events (SSE) for real-time updates

**Maps & Visualization**:
- Google Maps JavaScript API with @googlemaps/markerclusterer 2.6.2
- ReactFlow for workflow visualization
- Recharts 2.15.2 for data visualization

**Animations (New)**:
- Framer Motion 11.x for complex animations and page transitions
- Tailwind CSS transitions for simple hover effects
- CSS transforms for GPU-accelerated animations

**Forms & Validation**:
- React Hook Form 7.x for form management
- Zod for schema validation

**Internationalization**:
- i18next 25.5.2 with react-i18next
- Supported languages: English (en), Hindi (hi), Bengali (bn), Telugu (te)

**Testing**:
- Vitest 1.0.4 for unit and integration tests
- React Testing Library for component tests
- Playwright (optional) for E2E tests

**Development Tools**:
- ESLint for code linting
- Prettier for code formatting
- TypeScript for type safety

## Glossary

### System Components

- **System**: The EaseMyTrip-inspired travel planning web application, consisting of frontend (React SPA) and backend (Java Spring Boot)
- **Frontend**: React 18.3.1 single-page application with TypeScript, served via Vite
- **Backend**: Java Spring Boot REST API with SSE support, existing and stable
- **Database**: Backend persistence layer (implementation details abstracted)

### User Roles

- **User**: Authenticated traveler using the application to plan trips and make bookings
- **Guest**: Unauthenticated visitor who can view homepage and public content
- **Admin**: System administrator (future role, not in current scope)

### Travel Entities

- **Itinerary**: Complete travel plan containing multiple days, each with activities, meals, accommodations, and transportation. Represented by `NormalizedItinerary` data structure
- **Day**: Single day within an itinerary, containing ordered nodes and daily totals. Represented by `NormalizedDay`
- **Node**: Individual item in an itinerary with type-specific details. Types: `attraction`, `meal`, `hotel`, `transit`. Represented by `NormalizedNode`
- **Destination**: Geographic location that is the focus of travel (city, region, country)
- **Trip**: User-facing term for an itinerary, used in UI labels

### Booking Entities

- **Provider**: Third-party booking service that facilitates reservations. Examples: Booking.com (hotels), Expedia (flights/hotels), Airbnb (accommodations), Agoda (hotels), Skyscanner (flights)
- **Booking**: Reservation made through a provider, tracked by `BookingRecord` entity
- **BookingRecord**: Backend entity storing booking metadata (provider, confirmation number, status, etc.)
- **Booking Reference**: String identifier linking a node to its booking record (stored in `NormalizedNode.bookingRef`)
- **Confirmation Number**: User-facing booking identifier provided by the system (format: EMT{9-char alphanumeric})
- **Provider Booking ID**: Internal booking identifier from the provider's system
- **Mock Confirmation**: Simulated booking confirmation displayed after 2-3 seconds of iframe interaction, before real provider confirmation

### AI Components

- **Agent**: AI service that generates and modifies itineraries. Examples: SkeletonPlannerAgent, EnrichmentAgent
- **Agent Task**: Individual step in itinerary generation (e.g., "Finding attractions", "Booking hotels")
- **Agent Progress**: Real-time status updates from agents during itinerary generation
- **Agent Event**: SSE message containing agent progress information
- **Execution ID**: Unique identifier for an agent execution session

### Technical Terms

- **SSE (Server-Sent Events)**: Unidirectional real-time communication from server to client for agent progress and itinerary patches
- **Iframe**: HTML element embedding external provider website within application modal
- **Iframe Booking**: Booking flow where provider website is embedded in modal instead of opening new tab
- **Modal**: Overlay dialog component (using Radix UI Dialog primitive)
- **Toast**: Temporary notification message (using Sonner library)
- **Skeleton Loader**: Animated placeholder shown during content loading
- **Lazy Loading**: Deferred loading of components or images until needed

### Design Terms

- **Premium Animation**: High-energy, smooth transitions and micro-interactions targeting 60fps performance
- **Micro-interaction**: Small animation triggered by user action (hover, click, focus)
- **Design Token**: Reusable design value defined in CSS custom properties (colors, spacing, typography, shadows)
- **Design System**: Comprehensive set of design tokens, components, and patterns
- **Responsive Breakpoint**: Screen width threshold where layout adapts (sm: 640px, md: 768px, lg: 1024px, xl: 1280px, 2xl: 1536px)
- **Desktop-First**: Development approach starting with desktop layout, then adapting for smaller screens
- **Mobile-Responsive**: Layout that adapts gracefully to mobile screen sizes
- **Touch-Optimized**: Interactions designed for touch input (larger tap targets, swipe gestures)

### Data Structures

- **NormalizedItinerary**: Primary itinerary data structure with days array, settings, and metadata
- **NormalizedDay**: Day structure with nodes array, date, and daily totals
- **NormalizedNode**: Node structure with location, timing, cost, details, and type-specific fields
- **TripData**: Legacy itinerary format (maintained for backward compatibility)
- **ChangeSet**: Collection of proposed changes to an itinerary
- **PatchEvent**: Real-time update event for itinerary modifications

### State Management

- **Server State**: Data fetched from backend, managed by React Query (itineraries, bookings, user data)
- **Client State**: UI state managed by Zustand (current screen, selected trip, UI preferences)
- **Context State**: Shared state via React Context (auth state, itinerary context)
- **Local State**: Component-specific state via useState (form inputs, modal visibility)

### API Terms

- **Endpoint**: Backend API route (e.g., POST /api/itineraries)
- **Request DTO**: Data Transfer Object sent to backend
- **Response DTO**: Data Transfer Object received from backend
- **JWT Token**: JSON Web Token for authentication, obtained from Firebase
- **Bearer Token**: Authorization header format: `Bearer {jwt_token}`
- **Token Refresh**: Automatic renewal of JWT before expiry (5 minutes before)

### Animation Terms

- **Easing Function**: Mathematical curve defining animation acceleration (e.g., ease-in-out, cubic-bezier)
- **Duration**: Animation length in milliseconds
- **Keyframe**: Specific point in animation timeline
- **Transform**: CSS property for position/scale/rotation changes (GPU-accelerated)
- **Transition**: CSS property for smooth property changes
- **Framer Motion Variant**: Named animation state in Framer Motion
- **Spring Animation**: Physics-based animation with natural motion
- **Stagger**: Delayed animation start for list items

### Provider Terms

- **Provider URL Template**: URL pattern with placeholders for search parameters
- **Search Parameters**: Query string values (location, dates, guests, etc.)
- **Provider Logo**: Brand image for provider (40x40px in lists, 60x60px in modals)
- **Provider Configuration**: JSON/TypeScript object defining provider details
- **Vertical**: Travel category (hotel, flight, activity, train, bus)
- **Fallback Provider**: Alternative provider if primary fails to load

## Requirements

### Requirement 1: EaseMyTrip Visual Design System

**User Story:** As a user, I want the application to have a premium, polished look matching EaseMyTrip.com, so that I feel confident using a professional travel planning service.

#### Acceptance Criteria

1. WHEN viewing any page THEN the System SHALL use the EaseMyTrip color palette defined in `src/index.css`:

   **Primary Colors (EaseMyTrip Blue)**:
   - `--easemytrip-primary: hsl(207, 100%, 40%)` - Main brand blue (#0070DA)
     - Used for: Primary buttons, active tabs, links, brand elements
     - Hover state: `--easemytrip-primary-hover: hsl(207, 100%, 35%)` (#0063C3)
     - Light variant: `--easemytrip-primary-light: hsl(207, 100%, 85%)` (#B3D9FF)
     - Used for: Light backgrounds, hover states, disabled states
   
   **Secondary Colors (Orange Accent)**:
   - `--easemytrip-orange: hsl(26, 100%, 55%)` - Accent orange (#FF7A00)
     - Used for: CTA buttons, highlights, important actions, hotel vertical
     - Hover state: `--easemytrip-orange-hover: hsl(26, 100%, 50%)` (#FF6F00)
   
   **Gradient Combinations**:
   - `--easemytrip-gradient-primary: linear-gradient(135deg, hsl(207, 100%, 42%), hsl(207, 100%, 38%))`
     - Used for: Primary buttons, cards, feature sections
   - `--easemytrip-gradient-hero: linear-gradient(135deg, hsl(207, 100%, 40%), hsl(220, 100%, 45%))`
     - Used for: Hero section background, large headers
   
   **Neutral Colors**:
   - `--background: hsl(0, 0%, 100%)` - Pure white (#FFFFFF)
     - Used for: Page backgrounds, card backgrounds
   - `--foreground: hsl(222, 47%, 11%)` - Dark text (#1A1D29)
     - Used for: Primary text, headings
   - `--muted: hsl(210, 40%, 96%)` - Light gray background (#F5F7FA)
     - Used for: Secondary backgrounds, disabled states
   - `--muted-foreground: hsl(215, 16%, 47%)` - Gray text (#6B7280)
     - Used for: Secondary text, placeholders, captions
   - `--border: hsl(214, 32%, 91%)` - Light border (#E5E7EB)
     - Used for: Borders, dividers, separators
   
   **Status Colors**:
   - `--success: hsl(142, 76%, 36%)` - Green (#059669)
     - Used for: Success messages, confirmed bookings, positive states
   - `--warning: hsl(38, 92%, 50%)` - Yellow (#F59E0B)
     - Used for: Warning messages, pending states
   - `--error: hsl(0, 84%, 60%)` - Red (#EF4444)
     - Used for: Error messages, destructive actions, failed states
   
   **Travel Vertical Colors**:
   - `--flight-color: hsl(207, 100%, 40%)` - Blue for flights
   - `--hotel-color: hsl(26, 100%, 55%)` - Orange for hotels
   - `--bus-color: hsl(142, 76%, 36%)` - Green for bus
   - `--train-color: hsl(271, 91%, 65%)` - Purple for trains
   - `--holiday-color: hsl(338, 100%, 50%)` - Pink for holidays

2. WHEN viewing any page THEN the System SHALL apply premium shadows defined in CSS custom properties:

   **Shadow Definitions**:
   - `--shadow-premium-sm: 0 2px 8px rgba(0, 112, 218, 0.1)`
     - Used for: Small cards, dropdowns, tooltips
     - Subtle elevation for minor UI elements
   
   - `--shadow-premium-md: 0 8px 24px rgba(0, 112, 218, 0.15)`
     - Used for: Standard cards, modals, popovers
     - Medium elevation for important content
   
   - `--shadow-premium-lg: 0 16px 48px rgba(0, 112, 218, 0.2)`
     - Used for: Large modals, dialogs, hover states
     - Strong elevation for prominent elements
   
   - `--shadow-premium-xl: 0 24px 64px rgba(0, 112, 218, 0.25)`
     - Used for: Hero cards, featured content, floating elements
     - Maximum elevation for "million-dollar" feel
   
   **Shadow Usage Rules**:
   - Base state: Use sm or md shadow
   - Hover state: Increase shadow by one level (sm → md, md → lg)
   - Active state: Reduce shadow by one level
   - Focus state: Add colored outline with shadow
   - Disabled state: Remove shadow completely

3. WHEN viewing text THEN the System SHALL use Inter font family with precise typography scale:

   **Font Family**:
   - `--font-primary: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif`
   - `--font-heading: 'Inter', sans-serif`
   - `--font-display: 'Inter', sans-serif` (for hero headings)
   - Fallback chain ensures cross-platform consistency
   
   **Font Sizes (8px base scale)**:
   - `--text-xs: 0.75rem` (12px) - Captions, labels, metadata
   - `--text-sm: 0.875rem` (14px) - Secondary text, form labels
   - `--text-base: 1rem` (16px) - Body text, paragraphs
   - `--text-lg: 1.125rem` (18px) - Emphasized text, subheadings
   - `--text-xl: 1.25rem` (20px) - Small headings, card titles
   - `--text-2xl: 1.5rem` (24px) - Section headings
   - `--text-3xl: 1.875rem` (30px) - Page headings
   - `--text-4xl: 2.25rem` (36px) - Large headings
   - `--text-5xl: 3rem` (48px) - Hero headings, display text
   
   **Font Weights**:
   - `--font-light: 300` - Subtle text, decorative elements
   - `--font-normal: 400` - Body text, paragraphs
   - `--font-medium: 500` - Emphasized text, labels
   - `--font-semibold: 600` - Subheadings, important text
   - `--font-bold: 700` - Headings, strong emphasis
   - `--font-extrabold: 800` - Hero text, display headings
   
   **Line Heights**:
   - Headings: 1.2 (tight)
   - Body text: 1.5 (normal)
   - Captions: 1.4 (slightly tight)
   
   **Letter Spacing**:
   - Headings: -0.02em (slightly tighter)
   - Body text: 0 (normal)
   - All caps: 0.05em (slightly wider)

4. WHEN viewing travel verticals THEN the System SHALL use distinct colors with consistent application:

   **Vertical Color Mapping**:
   - **Flights**: `hsl(207, 100%, 40%)` - Blue
     - Tab indicator, icon color, accent elements
     - Matches primary brand color for consistency
   
   - **Hotels**: `hsl(26, 100%, 55%)` - Orange
     - Tab indicator, icon color, accent elements
     - Matches secondary brand color for warmth
   
   - **Bus**: `hsl(142, 76%, 36%)` - Green
     - Tab indicator, icon color, accent elements
     - Eco-friendly association
   
   - **Train**: `hsl(271, 91%, 65%)` - Purple
     - Tab indicator, icon color, accent elements
     - Premium travel association
   
   - **Holidays**: `hsl(338, 100%, 50%)` - Pink
     - Tab indicator, icon color, accent elements
     - Vacation and leisure association
   
   **Color Application Rules**:
   - Active tab: Full vertical color background with white text
   - Inactive tab: Gray background with gray text
   - Hover state: Light vertical color background (10% opacity)
   - Icon: Always uses vertical color
   - Border/underline: 2px solid vertical color when active

5. WHEN viewing any component THEN the System SHALL match screenshot references with pixel-perfect accuracy:

   **Screenshot Reference Mapping**:
   
   - **Homepage Layout** (`homepageflightroutessuggestions.png`):
     - Hero section height: 600px minimum
     - Search widget: Centered, max-width 1200px, padding 32px
     - Flight routes: Horizontal scroll, gap 16px, card width 280px
     - Spacing: 80px between sections
   
   - **Trending Destinations** (`homepagetrendingtouristdestinations.png`):
     - Grid: 4 columns on desktop (≥1280px), 3 on tablet (768-1279px), 2 on mobile
     - Card aspect ratio: 4:3
     - Image height: 200px
     - Card padding: 16px
     - Gap: 24px
     - Hover: Lift 4px, shadow increase to lg
   
   - **Flight Booking** (`flightbookingpage.png`):
     - Filters sidebar: 280px width, sticky position
     - Results area: Flex-grow, max-width 900px
     - Flight card: Full width, padding 24px, border-radius 12px
     - Airline logo: 40x40px, margin-right 16px
     - Price: Right-aligned, font-size 24px, font-weight 700
   
   - **Hotel Booking** (`hotelbookingpage.png`):
     - Similar layout to flights
     - Hotel image: 200x150px, border-radius 8px
     - Star rating: 16px icons, color #FFB800
     - Amenities: Icon grid, 32x32px icons, gap 8px
   
   - **Sidebar Navigation** (`sidebarnavigationbookings_section_bookhotels.png`):
     - Sidebar width: 280px
     - Tab height: 48px
     - Icon size: 20x20px
     - Active tab: Primary blue background, white text
     - Inactive tab: Transparent background, gray text
     - Hover: Light blue background (5% opacity)
   
   - **Itinerary Summary** (`itinerarysummarypage.png`, `itinerarysummarypage2.png`):
     - Header: Full width, padding 32px, gradient background
     - Stats cards: Grid 4 columns, gap 16px, padding 20px
     - Day cards: Full width, margin-bottom 16px, border-radius 12px
   
   - **Day-by-Day Cards** (`daybydaycardview.png`):
     - Card width: 100%, max-width 800px
     - Node card: Padding 16px, border-left 4px solid (type color)
     - Time badge: 12px font, gray background, border-radius 4px
     - Cost badge: Right-aligned, primary color, font-weight 600
   
   - **User Dashboard** (`userdashboardwithnotrips.png`):
     - Empty state: Centered, max-width 400px
     - Icon: 120x120px, gray color
     - Heading: 24px, font-weight 700, margin-bottom 8px
     - Button: Large size, primary color, margin-top 24px
   
   - **Login Page** (`login_signuppage.png`):
     - Split layout: 50/50 on desktop, full width on mobile
     - Left side: Gradient background, centered content
     - Right side: White background, form max-width 400px
     - Form padding: 48px
     - Input height: 48px
     - Button height: 48px
   
   - **Travel Blogs** (`travelblogs.png`):
     - Grid: 3 columns on desktop, 2 on tablet, 1 on mobile
     - Card: Border-radius 12px, overflow hidden
     - Image: Aspect ratio 16:9, object-fit cover
     - Content padding: 20px
     - Read time: 12px, gray color, icon 14x14px

6. WHEN implementing design tokens THEN the System SHALL define all values in `src/index.css` root selector:

   **Border Radius Scale**:
   - `--radius-sm: 4px` - Small elements (badges, tags)
   - `--radius-md: 8px` - Standard elements (inputs, buttons)
   - `--radius-lg: 12px` - Large elements (cards, modals)
   - `--radius-xl: 16px` - Extra large elements (hero cards)
   - `--radius-full: 9999px` - Circular elements (avatars, pills)
   
   **Spacing Scale** (Tailwind default, 4px base):
   - 0, 1 (4px), 2 (8px), 3 (12px), 4 (16px), 5 (20px), 6 (24px), 8 (32px), 10 (40px), 12 (48px), 16 (64px), 20 (80px), 24 (96px)
   
   **Z-Index Layers**:
   - `--z-base: 0` - Base content
   - `--z-dropdown: 1000` - Dropdowns, popovers
   - `--z-sticky: 1100` - Sticky headers
   - `--z-modal-backdrop: 1200` - Modal backdrops
   - `--z-modal: 1300` - Modal content
   - `--z-toast: 1400` - Toast notifications
   - `--z-tooltip: 1500` - Tooltips

7. WHEN applying colors THEN the System SHALL follow color usage guidelines:

   **Primary Blue Usage**:
   - Primary action buttons
   - Active navigation items
   - Links and interactive text
   - Progress bars and loaders
   - Selected states
   - Brand elements
   
   **Orange Accent Usage**:
   - Secondary action buttons (CTAs)
   - Important highlights
   - Hotel vertical elements
   - Promotional badges
   - Special offers
   
   **Neutral Colors Usage**:
   - White: Page backgrounds, card backgrounds, button text
   - Dark: Headings, body text, icons
   - Muted: Secondary backgrounds, disabled states
   - Muted foreground: Secondary text, placeholders
   - Border: Dividers, input borders, card borders
   
   **Status Colors Usage**:
   - Success: Confirmation messages, completed bookings, success icons
   - Warning: Pending states, important notices, caution messages
   - Error: Error messages, failed actions, validation errors
   
   **Color Contrast Requirements**:
   - Text on white: Minimum 4.5:1 contrast ratio
   - Text on colored backgrounds: Minimum 4.5:1 contrast ratio
   - Large text (≥18px): Minimum 3:1 contrast ratio
   - Interactive elements: Minimum 3:1 contrast ratio with adjacent colors

### Requirement 2: Homepage with Multi-Tab Search

**User Story:** As a user, I want a homepage with a prominent search widget for flights, hotels, and other travel services, so that I can quickly start planning my trip.

#### Acceptance Criteria

1. WHEN landing on homepage THEN the System SHALL display a hero section with:
   - Gradient background from `hsl(207, 100%, 40%)` to `hsl(220, 100%, 45%)`
   - Animated background elements (pulsing circles with blur)
   - Hero heading: "Plan Your Perfect Trip" in 48px extrabold
   - Subheading: "Discover amazing destinations with AI-powered itineraries"

2. WHEN viewing hero section THEN the System SHALL display a search widget card with:
   - Premium shadow (extra large)
   - White background with rounded corners
   - Tab navigation for: Flights, Hotels, Holidays, Trains, Bus
   - Each tab with icon and colored indicator when active

3. WHEN clicking a search tab THEN the System SHALL display the corresponding search form:
   - Flights: Origin, destination, dates, passengers, class
   - Hotels: Location, check-in, check-out, guests, rooms
   - Holidays: Destination, dates, travelers, package type
   - Trains: From, to, date, class
   - Bus: From, to, date, seat type

4. WHEN viewing hero section THEN the System SHALL display "Let AI Plan My Itinerary" button:
   - Orange background `hsl(26, 100%, 55%)`
   - Extra large size with icon (Sparkles)
   - Hover animation: lift and scale
   - Navigates to AI trip wizard

5. WHEN scrolling homepage THEN the System SHALL display trending destinations section:
   - Grid layout (3-4 columns on desktop)
   - Destination cards with image, name, starting price
   - Hover effect: lift and shadow increase
   - Matches `homepagetrendingtouristdestinations.png`

6. WHEN scrolling homepage THEN the System SHALL display popular flight routes:
   - Horizontal scrollable cards
   - Route cards with origin, destination, price, airline logo
   - Matches `homepageflightroutessuggestions.png`

7. WHEN scrolling homepage THEN the System SHALL display travel blogs section:
   - Blog cards with image, title, excerpt, read time
   - Matches `travelblogs.png`

### Requirement 3: AI Trip Wizard (Restyled)

**User Story:** As a user, I want the AI trip creation wizard to match the EaseMyTrip design, so that I have a consistent experience throughout the application.

#### Acceptance Criteria

1. WHEN accessing AI trip wizard THEN the System SHALL preserve existing SimplifiedTripWizard functionality:
   - Destination selection with autocomplete
   - Date range picker
   - Traveler count and preferences
   - Budget selection
   - All existing validation and error handling

2. WHEN viewing wizard THEN the System SHALL apply EaseMyTrip styling:
   - Premium card with large shadow
   - EaseMyTrip color scheme
   - Inter font family
   - Smooth transitions between steps

3. WHEN submitting wizard THEN the System SHALL navigate to agent progress with:
   - Same backend API call (POST /itineraries)
   - Same data structure (CreateItineraryRequest)
   - Same SSE connection for progress tracking

4. WHEN viewing wizard THEN the System SHALL display progress indicator:
   - Step numbers with connecting lines
   - Active step highlighted in primary blue
   - Completed steps with checkmark icon

### Requirement 4: AI Agent Progress (Restyled)

**User Story:** As a user, I want to see AI itinerary generation progress with premium animations, so that I feel engaged while waiting.

#### Acceptance Criteria

1. WHEN itinerary is generating THEN the System SHALL preserve existing SimplifiedAgentProgress functionality:
   - SSE connection to GET /agents/events/{itineraryId}
   - Real-time progress updates
   - Agent task tracking
   - Error handling and retry logic

2. WHEN viewing progress THEN the System SHALL display premium UI:
   - Centered card with gradient background
   - Large Sparkles icon with pulse animation
   - Heading: "Creating Your Perfect Itinerary"
   - Subheading: "Our AI is crafting a personalized travel experience"

3. WHEN receiving progress updates THEN the System SHALL animate progress bar:
   - Smooth progress transitions using useSmoothProgress hook
   - Color gradient from blue to orange
   - Percentage display
   - Estimated time remaining

4. WHEN agent is working THEN the System SHALL display motivational messages:
   - Rotating messages: "Analyzing destinations", "Finding perfect stays", "Discovering local cuisine"
   - Fade in/out animations
   - Icon animations matching message type

5. WHEN generation completes THEN the System SHALL navigate to trip view:
   - Smooth page transition
   - Success animation (checkmark with bounce)
   - Navigate to unified trip management interface

### Requirement 5: Unified Trip Management Interface

**User Story:** As a user, I want a comprehensive trip management interface with sidebar navigation, so that I can access all trip features in one place.

#### Acceptance Criteria

1. WHEN viewing a trip THEN the System SHALL display sidebar navigation with tabs:
   - View (trip overview with stats)
   - Plan (destinations and day-by-day)
   - Bookings (provider integration)
   - Budget (cost tracking)
   - Packing (smart packing lists)
   - Docs (travel documents)

2. WHEN clicking sidebar tab THEN the System SHALL display corresponding content:
   - Smooth transition animation
   - Active tab highlighted in primary blue
   - Icon and label for each tab
   - Matches `sidebarnavigationbookings_section_bookhotels.png`

3. WHEN viewing "View" tab THEN the System SHALL display trip overview:
   - Trip header with destination, dates, travelers
   - Key statistics cards (days, activities, budget)
   - Weather forecast for destination
   - Quick actions (share, export, edit)

4. WHEN viewing "Plan" tab THEN the System SHALL display itinerary:
   - Destination selector dropdown
   - Day-by-day view with cards
   - Map integration showing all locations
   - Matches `daybydaycardview.png` and `itinerarysummarypage.png`

5. WHEN viewing day cards THEN the System SHALL preserve existing functionality:
   - Activity, meal, hotel, transit cards
   - Drag-and-drop reordering
   - Inline editing
   - Add/remove nodes
   - Lock/unlock nodes

6. WHEN viewing "Bookings" tab THEN the System SHALL display booking interface:
   - List of bookable nodes (hotels, activities, transportation)
   - Provider selection for each node
   - "Book Now" buttons
   - Booking status indicators

7. WHEN viewing "Budget" tab THEN the System SHALL display cost breakdown:
   - Total budget vs spent
   - Category-wise breakdown (accommodation, food, activities, transport)
   - Charts using Recharts library
   - Cost per day visualization

8. WHEN viewing "Packing" tab THEN the System SHALL display packing list:
   - AI-generated packing suggestions
   - Categorized items (clothing, documents, electronics, toiletries)
   - Checkboxes to mark items as packed
   - Add custom items

9. WHEN viewing "Docs" tab THEN the System SHALL display travel documents:
   - Passport/visa requirements
   - Booking confirmations
   - Travel insurance details
   - Emergency contacts

### Requirement 6: Provider Booking with Embedded Iframes

**User Story:** As a user, I want to book hotels and activities through trusted providers without leaving the application, so that I have a seamless booking experience.

#### Acceptance Criteria

1. WHEN clicking "Book Now" on a node THEN the System SHALL display provider selection modal:
   - List of providers with logos (Booking.com, Expedia, Airbnb, Agoda)
   - Provider ratings and user reviews count
   - Estimated price range for each provider
   - "Select Provider" button for each

2. WHEN selecting a provider THEN the System SHALL open booking modal with embedded iframe:
   - Full-screen modal (max-width: 1200px, height: 80vh)
   - Provider logo and name in header
   - "Secure Booking" badge
   - Close button to cancel

3. WHEN iframe loads THEN the System SHALL construct provider URL with search parameters:
   - For Booking.com hotels: `https://www.booking.com/searchresults.html?ss={location}&checkin={date}&checkout={date}&group_adults={count}`
   - For Expedia hotels: `https://www.expedia.com/Hotel-Search?destination={location}&startDate={date}&endDate={date}&rooms={count}`
   - For Airbnb: `https://www.airbnb.com/s/{location}/homes?checkin={date}&checkout={date}&adults={count}`
   - For activities: Provider-specific URLs with location and date parameters

4. WHEN iframe is loading THEN the System SHALL display loading overlay:
   - Centered spinner with "Loading {provider}..." message
   - "Secure booking in progress" subtext
   - Animated loader

5. WHEN iframe has been open for 2-3 seconds THEN the System SHALL display mock confirmation modal:
   - Overlay on top of iframe
   - Green checkmark icon with bounce animation
   - "Booking Confirmed!" heading
   - Mock confirmation number (format: EMT{random 9-char alphanumeric})
   - Provider logo and name
   - "Continue Planning" button

6. WHEN clicking "Continue Planning" THEN the System SHALL:
   - Close booking modal
   - Call backend API to save booking data
   - Update node with booking reference
   - Show success toast notification
   - Refresh trip view to show booking status

7. WHEN booking is saved THEN the System SHALL persist booking data:
   - User ID
   - Itinerary ID
   - Node ID
   - Provider name
   - Mock confirmation number
   - Booking timestamp
   - Status: "confirmed"

### Requirement 7: Standard Booking Flow (Homepage Search)

**User Story:** As a user, I want to search for flights and hotels from the homepage and book them, so that I can make standalone bookings outside of AI-generated itineraries.

#### Acceptance Criteria

1. WHEN submitting homepage search form THEN the System SHALL display mock search results:
   - Loading state with skeleton loaders (1-2 seconds)
   - Results page with filters sidebar
   - Result cards with provider information
   - Sort options (price, rating, duration)

2. WHEN viewing flight results THEN the System SHALL display flight cards:
   - Airline logo and name
   - Departure and arrival times
   - Duration and stops
   - Price with currency
   - "Book Now" button
   - Matches `flightbookingpage.png`

3. WHEN viewing hotel results THEN the System SHALL display hotel cards:
   - Hotel image gallery
   - Hotel name and star rating
   - Location with map preview
   - Amenities icons
   - Price per night
   - "Book Now" button
   - Matches `hotelbookingpage.png`

4. WHEN clicking "Book Now" on search result THEN the System SHALL:
   - Use same provider iframe booking flow as Requirement 6
   - Show provider selection modal
   - Open iframe with pre-filled search parameters
   - Display mock confirmation after 2-3 seconds

5. WHEN booking from search results THEN the System SHALL:
   - Save booking to user's account
   - Create standalone booking record (not linked to itinerary)
   - Show booking in user's bookings list
   - Send confirmation email (if email service configured)

### Requirement 8: User Dashboard

**User Story:** As a user, I want a dashboard showing all my trips and bookings, so that I can manage my travel plans in one place.

#### Acceptance Criteria

1. WHEN accessing dashboard THEN the System SHALL display user information:
   - User profile picture and name
   - Account settings link
   - Logout button

2. WHEN viewing dashboard with no trips THEN the System SHALL display empty state:
   - Illustration or icon
   - "No trips yet" message
   - "Create Your First Trip" button
   - Matches `userdashboardwithnotrips.png`

3. WHEN viewing dashboard with trips THEN the System SHALL display trip cards:
   - Trip destination and dates
   - Trip thumbnail image
   - Status badge (upcoming, ongoing, completed)
   - Quick actions (view, edit, delete)
   - Grid layout (2-3 columns on desktop)

4. WHEN viewing dashboard THEN the System SHALL display bookings section:
   - Recent bookings list
   - Booking status (confirmed, pending, cancelled)
   - Provider logos
   - "View All Bookings" link

5. WHEN clicking trip card THEN the System SHALL navigate to unified trip view:
   - Load trip data from backend
   - Display in trip management interface
   - Preserve scroll position on back navigation

### Requirement 9: Authentication (Restyled)

**User Story:** As a user, I want a modern login/signup page matching EaseMyTrip design, so that I have a consistent brand experience from the start.

#### Acceptance Criteria

1. WHEN accessing login page THEN the System SHALL preserve existing Firebase authentication:
   - Google Sign-In integration
   - JWT token management
   - AuthContext state management
   - Protected route guards

2. WHEN viewing login page THEN the System SHALL display EaseMyTrip-styled UI:
   - Split layout: left side with branding, right side with form
   - Gradient background on branding side
   - Premium card for login form
   - Matches `login_signuppage.png`

3. WHEN viewing login form THEN the System SHALL display:
   - "Sign in with Google" button with Google logo
   - Email/password fields (if email auth enabled)
   - "Forgot password" link
   - "Don't have an account? Sign up" link

4. WHEN signing in THEN the System SHALL:
   - Call Firebase authentication
   - Store JWT token
   - Update AuthContext state
   - Redirect to dashboard or intended page

### Requirement 10: High-Energy Animations

**User Story:** As a user, I want smooth, premium animations throughout the application, so that I feel I'm using a high-quality, modern service.

#### Acceptance Criteria

1. WHEN viewing any interactive element THEN the System SHALL apply micro-interactions:
   - Buttons: lift on hover (translateY(-2px), scale(1.02))
   - Cards: shadow increase on hover
   - Links: color transition on hover
   - Icons: subtle rotation or scale on hover

2. WHEN page loads THEN the System SHALL animate content entrance:
   - Fade in with slide up (translateY(20px) to 0)
   - Staggered animation for lists (50ms delay between items)
   - Bounce animation for important CTAs

3. WHEN navigating between pages THEN the System SHALL apply page transitions:
   - Fade out current page (200ms)
   - Fade in new page (300ms)
   - Smooth scroll to top

4. WHEN modal opens THEN the System SHALL animate:
   - Backdrop fade in (200ms)
   - Modal scale from 0.95 to 1 with fade in (300ms)
   - Elastic easing for bounce effect

5. WHEN data loads THEN the System SHALL display skeleton loaders:
   - Animated shimmer effect
   - Matches content layout
   - Smooth transition to actual content

6. WHEN scrolling THEN the System SHALL apply scroll-based animations:
   - Parallax effect on hero background
   - Fade in elements as they enter viewport
   - Sticky header with shadow on scroll

7. WHEN hovering destination cards THEN the System SHALL animate:
   - Image zoom (scale 1.05)
   - Overlay fade in with details
   - Smooth 300ms transition

### Requirement 11: Responsive Design (Desktop-First)

**User Story:** As a user, I want the application to work on all devices, so that I can plan trips on desktop, tablet, or mobile.

#### Acceptance Criteria

1. WHEN viewing on desktop (≥1024px) THEN the System SHALL display full layout:
   - Sidebar navigation visible
   - Multi-column grids
   - Hover effects enabled
   - Full-size modals

2. WHEN viewing on tablet (768px-1023px) THEN the System SHALL adapt layout:
   - Collapsible sidebar
   - 2-column grids
   - Touch-optimized interactions
   - Adjusted modal sizes

3. WHEN viewing on mobile (<768px) THEN the System SHALL display mobile layout:
   - Bottom navigation bar
   - Single-column layout
   - Full-screen modals
   - Touch gestures (swipe, pinch)
   - Embedded iframes in full-screen mode

4. WHEN viewing on any device THEN the System SHALL maintain functionality:
   - All features accessible
   - Forms usable
   - Navigation intuitive
   - Content readable

### Requirement 12: Backend Booking Entity

**User Story:** As a developer, I want a dedicated booking entity in the backend, so that booking data is properly structured and queryable.

#### Acceptance Criteria

1. WHEN booking is created THEN the System SHALL persist BookingRecord entity with:
   - id (UUID, primary key)
   - userId (String, not null)
   - itineraryId (String, nullable for standalone bookings)
   - nodeId (String, nullable for standalone bookings)
   - providerName (String, not null)
   - providerBookingId (String, not null)
   - confirmationNumber (String, not null, user-facing)
   - bookingDetailsJson (TEXT, flexible JSON storage)
   - status (ENUM: PENDING, CONFIRMED, CANCELLED, REFUNDED)
   - bookedAt (LocalDateTime, auto-generated)
   - updatedAt (LocalDateTime, auto-updated)
   - totalAmount (BigDecimal, nullable)
   - currency (String, nullable)

2. WHEN booking is created THEN the System SHALL update NormalizedNode:
   - Set bookingRef field to BookingRecord.id
   - Update node status if applicable
   - Trigger itinerary update event

3. WHEN querying bookings THEN the System SHALL provide API endpoints:
   - POST /api/bookings (create booking)
   - GET /api/bookings/user/{userId} (get user's bookings)
   - GET /api/bookings/itinerary/{itineraryId} (get itinerary bookings)
   - PUT /api/bookings/{bookingId}/status (update status)
   - GET /api/bookings/{bookingId} (get booking details)

4. WHEN creating booking THEN the System SHALL validate:
   - User is authenticated
   - Provider name is valid
   - Confirmation number is unique
   - Required fields are present

5. WHEN booking status changes THEN the System SHALL:
   - Update updatedAt timestamp
   - Log status change
   - Notify user if applicable

### Requirement 13: Provider Configuration

**User Story:** As a developer, I want provider URLs and logos to be configurable, so that they can be easily updated without code changes.

#### Acceptance Criteria

1. WHEN application initializes THEN the System SHALL load provider configuration from:
   - Configuration file: `frontend/src/config/providers.ts`
   - Provider logos from: `frontend/public/assets/providers/`

2. WHEN provider configuration is defined THEN the System SHALL include:
   - Provider ID (unique identifier)
   - Provider name (display name)
   - Provider logo path (relative to public folder)
   - Provider URL template with placeholders
   - Supported verticals (hotel, flight, activity, etc.)
   - Active status (boolean)

3. WHEN constructing provider URL THEN the System SHALL:
   - Use URL template from configuration
   - Replace placeholders with actual values
   - Encode parameters properly
   - Validate URL format

4. WHEN displaying provider logo THEN the System SHALL:
   - Load from configured path
   - Show fallback if image fails to load
   - Apply consistent sizing (40x40px for lists, 60x60px for modals)

### Requirement 14: Analytics and Tracking

**User Story:** As a product manager, I want to track user interactions with providers and bookings, so that I can understand user behavior and optimize the experience.

#### Acceptance Criteria

1. WHEN user clicks "Book Now" THEN the System SHALL log event:
   - Event name: "booking_initiated"
   - Properties: nodeId, nodeType, providerName, itineraryId

2. WHEN provider iframe loads THEN the System SHALL log event:
   - Event name: "provider_iframe_loaded"
   - Properties: providerName, loadTime, nodeType

3. WHEN mock confirmation is shown THEN the System SHALL log event:
   - Event name: "booking_confirmed"
   - Properties: providerName, confirmationNumber, nodeType, itineraryId

4. WHEN user searches from homepage THEN the System SHALL log event:
   - Event name: "search_performed"
   - Properties: searchType (flight/hotel/etc), origin, destination, dates

5. WHEN user creates AI trip THEN the System SHALL log event:
   - Event name: "ai_trip_created"
   - Properties: destination, duration, travelers, budget

### Requirement 15: Performance Optimization

**User Story:** As a user, I want the application to load quickly and respond smoothly, so that I have a pleasant experience.

#### Acceptance Criteria

1. WHEN application loads THEN the System SHALL achieve:
   - Initial page load ≤ 2 seconds
   - Time to interactive ≤ 3 seconds
   - Lighthouse performance score ≥ 90

2. WHEN loading images THEN the System SHALL:
   - Use lazy loading for below-fold images
   - Serve optimized image formats (WebP with fallback)
   - Use responsive images with srcset

3. WHEN loading components THEN the System SHALL:
   - Code-split routes with React.lazy
   - Lazy load heavy components (TravelPlanner, WorkflowBuilder)
   - Prefetch critical routes

4. WHEN caching data THEN the System SHALL:
   - Use React Query cache with appropriate staleTime
   - Cache provider logos and static assets
   - Implement service worker for offline support (optional)

5. WHEN animating THEN the System SHALL:
   - Use CSS transforms and opacity (GPU-accelerated)
   - Avoid layout thrashing
   - Throttle scroll event handlers

### Requirement 16: Accessibility

**User Story:** As a user with disabilities, I want the application to be accessible, so that I can use all features independently.

#### Acceptance Criteria

1. WHEN navigating with keyboard THEN the System SHALL:
   - Support tab navigation for all interactive elements
   - Show visible focus indicators
   - Support keyboard shortcuts (Esc to close modals, etc.)

2. WHEN using screen reader THEN the System SHALL:
   - Provide ARIA labels for all interactive elements
   - Announce dynamic content changes
   - Use semantic HTML elements

3. WHEN viewing content THEN the System SHALL:
   - Maintain color contrast ratio ≥ 4.5:1 for text
   - Support text scaling up to 200%
   - Avoid relying solely on color for information

4. WHEN interacting with forms THEN the System SHALL:
   - Associate labels with inputs
   - Provide clear error messages
   - Support autocomplete attributes

### Requirement 17: Error Handling and Edge Cases

**User Story:** As a user, I want clear error messages and graceful handling of failures, so that I understand what went wrong and how to fix it.

#### Acceptance Criteria

1. WHEN API call fails THEN the System SHALL:
   - Display user-friendly error message
   - Provide retry button
   - Log error details for debugging
   - Show fallback UI

2. WHEN provider iframe fails to load THEN the System SHALL:
   - Display error message in modal
   - Provide "Try Another Provider" button
   - Close modal on user request
   - Log iframe error

3. WHEN network is offline THEN the System SHALL:
   - Display offline indicator
   - Queue actions for when online
   - Show cached data if available
   - Prevent form submissions

4. WHEN booking fails THEN the System SHALL:
   - Show error modal with details
   - Do not update node booking status
   - Provide support contact information
   - Log error for investigation

### Requirement 18: Testing Requirements

**User Story:** As a developer, I want comprehensive tests, so that I can confidently make changes without breaking functionality.

#### Acceptance Criteria

1. WHEN running tests THEN the System SHALL have:
   - Unit tests for utility functions (≥80% coverage)
   - Component tests for UI components (≥70% coverage)
   - Integration tests for critical flows
   - E2E tests for booking flow

2. WHEN testing components THEN the System SHALL verify:
   - Rendering with different props
   - User interactions (clicks, inputs)
   - State changes
   - API integration

3. WHEN testing booking flow THEN the System SHALL verify:
   - Provider selection
   - Iframe loading
   - Mock confirmation display
   - Backend API calls
   - State updates

## Constraints and Assumptions

### Technical Constraints

1. Must maintain 100% compatibility with existing backend APIs
2. Must preserve all existing AI functionality
3. Must support existing authentication mechanism (Firebase)
4. Must work with existing data models (NormalizedItinerary, NormalizedNode)
5. Desktop-first development, mobile-responsive required

### Business Constraints

1. All features are high priority - no MVP cuts
2. Must match EaseMyTrip visual design closely
3. Provider logos are placeholders initially
4. Mock confirmation is acceptable for initial release
5. Real provider integration is future enhancement

### Assumptions

1. User is authenticated for all booking operations
2. Backend will implement BookingRecord entity as specified
3. Provider URLs are publicly accessible
4. Iframe embedding is allowed by providers (may need fallback)
5. Screenshots accurately represent desired design
6. Internet connection is available for provider iframes

## Success Criteria

The redesign will be considered successful when:

1. ✅ Visual design matches EaseMyTrip screenshots with ≥95% fidelity
2. ✅ All existing AI functionality works without regression
3. ✅ Provider booking flow is functional with mock confirmations
4. ✅ Animations are smooth (60fps) on target devices
5. ✅ Lighthouse performance score ≥ 90
6. ✅ Accessibility score ≥ 90
7. ✅ All unit and integration tests pass
8. ✅ User testing shows ≥90% satisfaction with new design
9. ✅ Mobile responsive design works on iOS and Android
10. ✅ Backend booking entity is implemented and functional


### Development Approach

**Desktop-First Strategy**:
- Primary development target: Desktop browsers (≥1024px width)
- All components must be built with responsive design in mind
- Mobile breakpoints: sm (640px), md (768px), lg (1024px), xl (1280px), 2xl (1536px)
- Touch-optimized interactions for tablet and mobile
- Future mobile app will reuse responsive components

**Backend Integration**:
- Backend is fully implemented and stable
- Focus entirely on frontend redesign
- Maintain 100% API compatibility
- No changes to request/response formats
- Preserve all existing endpoints and SSE connections

**Data Preservation**:
- Existing `frontend-ui-redesign-spec` folder contains current frontend documentation
- Reference for understanding current UI and backend integration
- Do not modify existing documentation
- New spec (`easemytrip-redesign`) is for redesign project only

**Provider Integration Strategy**:
- Use actual provider URLs (Booking.com, Expedia, Airbnb, Agoda)
- Research latest provider URL formats and parameters
- Embedded iframes for seamless booking experience
- Mock confirmation after 2-3 seconds to simulate booking
- Backend will persist booking data via new BookingRecord entity

**Screenshot-Driven Design**:
- All screenshots in `analysis/frontend-spec/screenshots/` must be analyzed
- Create detailed component specifications based on screenshots
- Translate visual design into code-ready specifications
- Document exact colors, spacing, typography, shadows from screenshots

**Animation Specifications**:
- Separate animation specification document required
- Define exact durations, easing functions, and keyframes
- Specify Framer Motion variants for complex animations
- Document micro-interactions for every interactive element
- Target 60fps performance on all animations

### Quality Standards

**Visual Fidelity**: ≥95% match to EaseMyTrip screenshots
**Performance**: Lighthouse score ≥90
**Accessibility**: WCAG 2.1 Level AA compliance
**Browser Support**: Chrome, Firefox, Safari, Edge (last 2 versions)
**Mobile Support**: iOS Safari, Chrome Mobile
**Test Coverage**: ≥80% for utilities, ≥70% for components
**Code Quality**: ESLint and TypeScript strict mode with zero errors



### Requirement 2: Homepage with Multi-Tab Search

**User Story:** As a user, I want a homepage with a prominent search widget for flights, hotels, and other travel services, so that I can quickly start planning my trip.

#### Acceptance Criteria

1. WHEN landing on homepage THEN the System SHALL display a hero section with precise specifications:

   **Hero Section Structure** (`components/homepage/HeroSection.tsx`):
   
   **Container**:
   - Element: `<section>`
   - Position: Relative
   - Background: `linear-gradient(135deg, hsl(207, 100%, 40%) 0%, hsl(220, 100%, 45%) 100%)`
   - Padding top: 80px (5rem)
   - Padding bottom: 128px (8rem)
   - Overflow: Hidden
   - Min-height: 600px
   
   **Animated Background Elements**:
   - Element 1: Pulsing circle (top-left)
     - Position: Absolute, top: 80px, left: 40px
     - Size: 256px x 256px (w-64 h-64)
     - Background: `rgba(255, 255, 255, 0.1)`
     - Border-radius: 50%
     - Filter: `blur(48px)`
     - Animation: Pulse (2s infinite)
     - Keyframes: Scale 1 → 1.1 → 1, Opacity 0.5 → 0.7 → 0.5
   
   - Element 2: Pulsing circle (bottom-right)
     - Position: Absolute, bottom: 80px, right: 40px
     - Size: 384px x 384px (w-96 h-96)
     - Background: `rgba(255, 122, 0, 0.1)`
     - Border-radius: 50%
     - Filter: `blur(48px)`
     - Animation: Pulse (2s infinite, 1s delay)
     - Keyframes: Scale 1 → 1.15 → 1, Opacity 0.4 → 0.6 → 0.4
   
   **Content Container**:
   - Max-width: 1280px (container mx-auto)
   - Padding: 0 16px (px-4)
   - Position: Relative
   - Z-index: 10
   
   **Hero Heading**:
   - Element: `<h1>`
   - Text: "Plan Your Perfect Trip"
   - Font-size: 48px (text-5xl) on desktop, 36px (text-4xl) on mobile
   - Font-weight: 800 (font-extrabold)
   - Color: White
   - Text-align: Center
   - Margin-bottom: 16px (mb-4)
   - Animation: Bounce-in (500ms, ease-out)
     - Keyframes: Scale 0.3 → 1.05 → 0.9 → 1, Opacity 0 → 1
   
   **Hero Subheading**:
   - Element: `<p>`
   - Text: "Discover amazing destinations with AI-powered itineraries"
   - Font-size: 20px (text-xl)
   - Color: `rgba(255, 255, 255, 0.9)`
   - Text-align: Center
   - Max-width: 672px (max-w-2xl)
   - Margin: 0 auto
   - Animation: Fade-in-up (300ms, 300ms delay)
     - Keyframes: TranslateY(20px) → 0, Opacity 0 → 1

2. WHEN viewing hero section THEN the System SHALL display a search widget card with specifications:

   **Search Widget Card** (`components/homepage/SearchWidget.tsx`):
   
   **Card Container**:
   - Element: Radix UI Card component
   - Max-width: 1200px (max-w-5xl)
   - Margin: 0 auto, 48px top (mt-12)
   - Background: White
   - Border-radius: 16px (rounded-xl)
   - Box-shadow: `--shadow-premium-xl`
   - Padding: 32px (p-8)
   - Animation: Fade-in-up (300ms, 500ms delay)
   
   **Tab Navigation** (Radix UI Tabs):
   - Container: `<TabsList>`
   - Display: Grid
   - Grid-template-columns: `repeat(5, 1fr)` (5 equal columns)
   - Background: `hsl(210, 40%, 96%)` (light gray)
   - Padding: 4px (p-1)
   - Border-radius: 12px (rounded-xl)
   - Gap: 4px
   
   **Individual Tab** (`<TabsTrigger>`):
   - Height: 48px
   - Display: Flex
   - Align-items: Center
   - Justify-content: Center
   - Gap: 8px (between icon and text)
   - Font-size: 14px (text-sm)
   - Font-weight: 500 (font-medium)
   - Border-radius: 8px (rounded-lg)
   - Transition: All 200ms ease
   - Cursor: Pointer
   
   **Tab States**:
   - Inactive:
     - Background: Transparent
     - Color: `hsl(215, 16%, 47%)` (gray)
     - Icon color: `hsl(215, 16%, 47%)`
   
   - Hover (inactive):
     - Background: `rgba(0, 112, 218, 0.05)`
     - Color: `hsl(207, 100%, 40%)`
   
   - Active (data-state="active"):
     - Background: Vertical-specific color (full opacity)
     - Color: White
     - Icon color: White
     - Box-shadow: `0 2px 4px rgba(0, 0, 0, 0.1)`
   
   **Tab Icons**:
   - Size: 16x16px (h-4 w-4)
   - Stroke-width: 2
   - Library: Lucide React
   - Icons:
     - Flights: `<Plane />`
     - Hotels: `<Hotel />`
     - Holidays: `<Palmtree />`
     - Trains: `<Train />`
     - Bus: `<Bus />`
   
   **Tab Content Area**:
   - Margin-top: 24px (mt-6)
   - Animation: Fade-in (200ms) when tab changes
   - Min-height: 200px (to prevent layout shift)

3. WHEN clicking a search tab THEN the System SHALL display the corresponding search form with specifications:

   **Flights Search Form** (`<TabsContent value="flights">`):
   
   **Form Layout**:
   - Display: Grid
   - Grid-template-columns: `repeat(4, 1fr)` on desktop, `1fr` on mobile
   - Gap: 16px (gap-4)
   - Padding: 0
   
   **Trip Type Selector**:
   - Element: Radio group (Radix UI RadioGroup)
   - Options: "One Way", "Round Trip", "Multi-City"
   - Layout: Horizontal flex
   - Gap: 16px
   - Margin-bottom: 16px
   
   **Origin Input**:
   - Label: "From"
   - Element: Autocomplete input (Radix UI Combobox)
   - Placeholder: "Enter origin city"
   - Icon: `<MapPin />` (left side, 20x20px)
   - Height: 56px
   - Border: 1px solid `hsl(214, 32%, 91%)`
   - Border-radius: 8px
   - Padding: 0 16px 0 48px (space for icon)
   - Font-size: 16px
   - Focus: Border color changes to primary blue, box-shadow added
   - Autocomplete: Dropdown with city suggestions, max 5 results
   
   **Destination Input**:
   - Label: "To"
   - Same specifications as Origin
   - Icon: `<MapPin />`
   - Swap button between origin and destination (circular button, 32x32px, primary blue)
   
   **Departure Date**:
   - Label: "Departure"
   - Element: Date picker (Radix UI Popover + Calendar)
   - Placeholder: "Select date"
   - Icon: `<Calendar />` (left side)
   - Height: 56px
   - Min date: Today
   - Max date: 1 year from today
   - Calendar: Month view, week starts Monday, highlight today
   
   **Return Date** (if Round Trip):
   - Label: "Return"
   - Same specifications as Departure
   - Min date: Departure date
   - Disabled if One Way selected
   
   **Passengers Selector**:
   - Label: "Passengers"
   - Element: Popover with counter controls
   - Display: "X Adults, Y Children, Z Infants"
   - Icon: `<Users />` (left side)
   - Popover content:
     - Adults: Counter (min 1, max 9)
     - Children (2-12 years): Counter (min 0, max 8)
     - Infants (under 2): Counter (min 0, max 2)
     - Each counter: Minus button, number display, plus button
     - Button size: 32x32px, circular, border
   
   **Class Selector**:
   - Label: "Class"
   - Element: Select dropdown (Radix UI Select)
   - Options: "Economy", "Premium Economy", "Business", "First Class"
   - Icon: `<Armchair />` (left side)
   - Height: 56px
   
   **Search Button**:
   - Text: "Search Flights"
   - Width: Full width (spans all columns)
   - Height: 56px
   - Background: Primary blue gradient
   - Color: White
   - Font-size: 16px
   - Font-weight: 600
   - Border-radius: 8px
   - Icon: `<Search />` (left side, 20x20px)
   - Hover: Lift 2px, shadow increase, background darken
   - Active: Scale 0.98
   - Loading state: Spinner replaces icon, button disabled
   
   **Hotels Search Form** (`<TabsContent value="hotels">`):
   
   **Form Fields**:
   1. Location (autocomplete, city/hotel name)
   2. Check-in date (date picker)
   3. Check-out date (date picker, min = check-in + 1 day)
   4. Guests (popover: Adults, Children, Rooms)
   5. Search button: "Search Hotels"
   
   **Holidays Search Form** (`<TabsContent value="holidays">`):
   
   **Form Fields**:
   1. Destination (autocomplete, popular destinations)
   2. Travel dates (date range picker)
   3. Travelers (counter: Adults, Children)
   4. Package type (select: All-Inclusive, Adventure, Luxury, Budget)
   5. Search button: "Search Holidays"
   
   **Trains Search Form** (`<TabsContent value="trains">`):
   
   **Form Fields**:
   1. From station (autocomplete)
   2. To station (autocomplete)
   3. Journey date (date picker)
   4. Class (select: Sleeper, AC 3-Tier, AC 2-Tier, AC 1st Class)
   5. Search button: "Search Trains"
   
   **Bus Search Form** (`<TabsContent value="bus">`):
   
   **Form Fields**:
   1. From city (autocomplete)
   2. To city (autocomplete)
   3. Journey date (date picker)
   4. Seat type (select: Seater, Sleeper, AC, Non-AC)
   5. Search button: "Search Buses"
   
   **Form Validation**:
   - All required fields must be filled
   - Dates must be valid and in future
   - Passenger/guest counts must be > 0
   - Show inline error messages below invalid fields
   - Error color: `hsl(0, 84%, 60%)` (red)
   - Error icon: `<AlertCircle />` (16x16px)
   - Disable search button until form is valid

4. WHEN viewing hero section THEN the System SHALL display "Let AI Plan My Itinerary" button with specifications:

   **AI Planner CTA** (`components/homepage/AIPlannerCTA.tsx`):
   
   **Container**:
   - Text-align: Center
   - Margin-top: 48px (mt-12)
   - Animation: Fade-in-up (300ms, 700ms delay)
   
   **Button**:
   - Element: Radix UI Button
   - Size: Extra large
   - Width: Auto (inline-flex)
   - Height: 64px
   - Padding: 0 48px (px-12 py-6)
   - Background: `hsl(26, 100%, 55%)` (orange)
   - Color: White
   - Font-size: 20px (text-xl)
   - Font-weight: 600 (font-semibold)
   - Border-radius: 12px (rounded-xl)
   - Box-shadow: `--shadow-premium-lg`
   - Display: Flex
   - Align-items: Center
   - Gap: 12px
   - Cursor: Pointer
   - Transition: All 300ms cubic-bezier(0.4, 0, 0.2, 1)
   
   **Button Content**:
   - Left icon: `<Sparkles />` (24x24px, h-6 w-6)
   - Text: "Let AI Plan My Itinerary"
   - Right icon: `<ArrowRight />` (24x24px, h-6 w-6)
   
   **Button States**:
   - Hover:
     - Transform: `translateY(-2px) scale(1.02)`
     - Background: `hsl(26, 100%, 50%)` (darker orange)
     - Box-shadow: `--shadow-premium-xl`
   
   - Active:
     - Transform: `scale(0.98)`
     - Box-shadow: `--shadow-premium-md`
   
   - Focus:
     - Outline: 2px solid white
     - Outline-offset: 2px
   
   **Subtext**:
   - Element: `<p>`
   - Text: "Get a personalized itinerary in minutes ✨"
   - Font-size: 18px (text-lg)
   - Color: `rgba(255, 255, 255, 0.9)`
   - Margin-top: 16px (mt-4)
   - Font-weight: 400
   
   **Click Action**:
   - Navigate to: `/ai-planner` route
   - Use: `useNavigate()` from react-router-dom
   - Transition: Fade out current page, fade in new page

5. WHEN scrolling homepage THEN the System SHALL display trending destinations section with specifications:

   **Trending Destinations Section** (`components/homepage/TrendingDestinations.tsx`):
   
   **Section Container**:
   - Padding: 80px 0 (py-20)
   - Background: White
   - Max-width: 1280px (container mx-auto)
   - Padding-x: 16px (px-4)
   
   **Section Header**:
   - Heading: "Trending Destinations"
   - Font-size: 36px (text-4xl)
   - Font-weight: 700 (font-bold)
   - Color: `hsl(222, 47%, 11%)` (dark)
   - Text-align: Center
   - Margin-bottom: 48px (mb-12)
   
   **Subheading**:
   - Text: "Explore the most popular travel destinations"
   - Font-size: 18px (text-lg)
   - Color: `hsl(215, 16%, 47%)` (gray)
   - Text-align: Center
   - Margin-top: 8px
   
   **Grid Layout**:
   - Display: Grid
   - Desktop (≥1280px): 4 columns (`grid-cols-4`)
   - Tablet (768-1279px): 3 columns (`md:grid-cols-3`)
   - Mobile (<768px): 2 columns (`grid-cols-2`)
   - Gap: 24px (gap-6)
   
   **Destination Card** (`components/homepage/DestinationCard.tsx`):
   
   **Card Container**:
   - Element: Radix UI Card
   - Border-radius: 12px (rounded-xl)
   - Overflow: Hidden
   - Box-shadow: `--shadow-premium-sm`
   - Cursor: Pointer
   - Transition: All 300ms ease
   - Position: Relative
   
   **Card Image**:
   - Element: `<img>` with lazy loading
   - Aspect-ratio: 4:3
   - Height: 200px
   - Width: 100%
   - Object-fit: Cover
   - Transition: Transform 300ms ease
   
   **Card Overlay** (on hover):
   - Position: Absolute
   - Inset: 0
   - Background: `linear-gradient(to top, rgba(0, 0, 0, 0.7) 0%, transparent 50%)`
   - Opacity: 0 (default), 1 (on hover)
   - Transition: Opacity 300ms ease
   
   **Card Content**:
   - Padding: 16px (p-4)
   - Background: White
   
   **Destination Name**:
   - Font-size: 18px (text-lg)
   - Font-weight: 600 (font-semibold)
   - Color: `hsl(222, 47%, 11%)`
   - Margin-bottom: 4px
   
   **Starting Price**:
   - Font-size: 14px (text-sm)
   - Color: `hsl(215, 16%, 47%)`
   - Format: "Starting from ₹X,XXX"
   
   **Card Hover State**:
   - Transform: `translateY(-4px)`
   - Box-shadow: `--shadow-premium-lg`
   - Image transform: `scale(1.05)`
   - Overlay opacity: 1
   
   **Card Click Action**:
   - Navigate to: `/destinations/{destination-slug}`
   - Or: Open destination details modal
   - Or: Pre-fill AI wizard with destination
   
   **Mock Data Structure**:
   ```typescript
   interface TrendingDestination {
     id: string;
     name: string;
     slug: string;
     image: string;
     startingPrice: number;
     currency: string;
     country: string;
     description: string;
   }
   ```
   
   **Example Destinations**:
   - Paris, France - ₹45,000
   - Bali, Indonesia - ₹35,000
   - Dubai, UAE - ₹40,000
   - Maldives - ₹80,000
   - Thailand - ₹30,000
   - Singapore - ₹38,000
   - Switzerland - ₹90,000
   - Japan - ₹70,000

6. WHEN scrolling homepage THEN the System SHALL display popular flight routes section with specifications:

   **Popular Flight Routes Section** (`components/homepage/PopularRoutes.tsx`):
   
   **Section Container**:
   - Padding: 80px 0 (py-20)
   - Background: `hsl(210, 40%, 96%)` (light gray)
   - Width: 100%
   
   **Content Container**:
   - Max-width: 1280px (container mx-auto)
   - Padding-x: 16px (px-4)
   
   **Section Header**:
   - Heading: "Popular Flight Routes"
   - Font-size: 36px (text-4xl)
   - Font-weight: 700 (font-bold)
   - Margin-bottom: 48px (mb-12)
   
   **Carousel Container**:
   - Display: Flex
   - Overflow-x: Auto
   - Scroll-behavior: Smooth
   - Gap: 16px (gap-4)
   - Padding-bottom: 16px (for scrollbar)
   - Scrollbar styling: Thin, primary blue thumb
   
   **Route Card** (`components/homepage/RouteCard.tsx`):
   
   **Card Container**:
   - Width: 280px (fixed, w-70)
   - Flex-shrink: 0
   - Background: White
   - Border-radius: 12px (rounded-xl)
   - Padding: 20px (p-5)
   - Box-shadow: `--shadow-premium-sm`
   - Cursor: Pointer
   - Transition: All 300ms ease
   
   **Route Header**:
   - Display: Flex
   - Justify-content: Space-between
   - Align-items: Center
   - Margin-bottom: 16px
   
   **Origin/Destination**:
   - Font-size: 20px (text-xl)
   - Font-weight: 600 (font-semibold)
   - Color: `hsl(222, 47%, 11%)`
   - Format: "DEL → BOM" (airport codes)
   
   **Airline Logo**:
   - Size: 32x32px
   - Border-radius: 4px
   - Object-fit: Contain
   
   **Route Details**:
   - Display: Flex
   - Flex-direction: Column
   - Gap: 8px
   
   **City Names**:
   - Font-size: 14px (text-sm)
   - Color: `hsl(215, 16%, 47%)`
   - Format: "Delhi to Mumbai"
   
   **Price**:
   - Font-size: 24px (text-2xl)
   - Font-weight: 700 (font-bold)
   - Color: `hsl(207, 100%, 40%)` (primary blue)
   - Format: "₹X,XXX"
   
   **Price Label**:
   - Font-size: 12px (text-xs)
   - Color: `hsl(215, 16%, 47%)`
   - Text: "per person"
   
   **Card Hover State**:
   - Transform: `translateY(-2px)`
   - Box-shadow: `--shadow-premium-md`
   
   **Card Click Action**:
   - Pre-fill flights search form
   - Scroll to search widget
   - Or: Navigate to flight search results
   
   **Mock Data Structure**:
   ```typescript
   interface PopularRoute {
     id: string;
     origin: string; // Airport code
     destination: string; // Airport code
     originCity: string;
     destinationCity: string;
     airline: string;
     airlineLogo: string;
     price: number;
     currency: string;
   }
   ```
   
   **Example Routes**:
   - DEL → BOM (Delhi to Mumbai) - ₹3,500
   - BLR → GOI (Bangalore to Goa) - ₹4,200
   - DEL → DXB (Delhi to Dubai) - ₹12,000
   - BOM → SIN (Mumbai to Singapore) - ₹15,000
   - DEL → LHR (Delhi to London) - ₹35,000

7. WHEN scrolling homepage THEN the System SHALL display travel blogs section with specifications:

   **Travel Blogs Section** (`components/homepage/TravelBlogs.tsx`):
   
   **Section Container**:
   - Padding: 80px 0 (py-20)
   - Background: White
   - Max-width: 1280px (container mx-auto)
   - Padding-x: 16px (px-4)
   
   **Section Header**:
   - Heading: "Travel Inspiration"
   - Font-size: 36px (text-4xl)
   - Font-weight: 700 (font-bold)
   - Margin-bottom: 48px (mb-12)
   
   **Grid Layout**:
   - Display: Grid
   - Desktop (≥1024px): 3 columns (`lg:grid-cols-3`)
   - Tablet (768-1023px): 2 columns (`md:grid-cols-2`)
   - Mobile (<768px): 1 column (`grid-cols-1`)
   - Gap: 24px (gap-6)
   
   **Blog Card** (`components/homepage/BlogCard.tsx`):
   
   **Card Container**:
   - Border-radius: 12px (rounded-xl)
   - Overflow: Hidden
   - Box-shadow: `--shadow-premium-sm`
   - Background: White
   - Cursor: Pointer
   - Transition: All 300ms ease
   
   **Blog Image**:
   - Aspect-ratio: 16:9
   - Width: 100%
   - Object-fit: Cover
   - Transition: Transform 300ms ease
   
   **Blog Content**:
   - Padding: 20px (p-5)
   
   **Blog Category**:
   - Font-size: 12px (text-xs)
   - Font-weight: 600 (font-semibold)
   - Color: `hsl(207, 100%, 40%)` (primary blue)
   - Text-transform: Uppercase
   - Letter-spacing: 0.05em
   - Margin-bottom: 8px
   
   **Blog Title**:
   - Font-size: 18px (text-lg)
   - Font-weight: 600 (font-semibold)
   - Color: `hsl(222, 47%, 11%)`
   - Line-height: 1.4
   - Margin-bottom: 8px
   - Max-lines: 2 (line-clamp-2)
   
   **Blog Excerpt**:
   - Font-size: 14px (text-sm)
   - Color: `hsl(215, 16%, 47%)`
   - Line-height: 1.5
   - Margin-bottom: 16px
   - Max-lines: 3 (line-clamp-3)
   
   **Blog Meta**:
   - Display: Flex
   - Align-items: Center
   - Gap: 16px
   - Font-size: 12px (text-xs)
   - Color: `hsl(215, 16%, 47%)`
   
   **Read Time**:
   - Icon: `<Clock />` (14x14px)
   - Text: "X min read"
   
   **Publish Date**:
   - Icon: `<Calendar />` (14x14px)
   - Format: "MMM DD, YYYY"
   
   **Card Hover State**:
   - Transform: `translateY(-4px)`
   - Box-shadow: `--shadow-premium-md`
   - Image transform: `scale(1.05)`
   
   **Card Click Action**:
   - Navigate to: `/blog/{blog-slug}`
   - Or: Open blog in new tab
   
   **Mock Data Structure**:
   ```typescript
   interface BlogPost {
     id: string;
     title: string;
     slug: string;
     excerpt: string;
     image: string;
     category: string;
     readTime: number; // minutes
     publishedAt: string; // ISO date
     author: string;
   }
   ```
   
   **Example Blog Posts**:
   - "10 Hidden Gems in Bali You Must Visit"
   - "Ultimate Guide to Planning a European Vacation"
   - "Best Time to Visit Maldives: A Complete Guide"
   - "Budget Travel Tips for Southeast Asia"
   - "Top 15 Adventure Activities in New Zealand"
   - "Cultural Experiences You Can't Miss in Japan"

8. WHEN viewing homepage on mobile THEN the System SHALL adapt layout:

   **Mobile Adaptations** (<768px):
   
   **Hero Section**:
   - Padding: 40px 0 (py-10)
   - Heading: 36px (text-4xl)
   - Background circles: Smaller (128px, 192px)
   
   **Search Widget**:
   - Padding: 16px (p-4)
   - Tabs: Scrollable horizontal (overflow-x-auto)
   - Tab text: Hidden on small screens, icon only
   - Form: Single column layout
   
   **AI CTA Button**:
   - Width: Full width
   - Font-size: 18px
   - Padding: 0 24px
   
   **Trending Destinations**:
   - Grid: 2 columns
   - Gap: 16px
   - Card image height: 150px
   
   **Popular Routes**:
   - Horizontal scroll maintained
   - Card width: 240px
   
   **Travel Blogs**:
   - Grid: 1 column
   - Full width cards



### Requirement 3: AI Trip Wizard (Restyled)

**User Story:** As a user, I want the AI trip creation wizard to match the EaseMyTrip design, so that I have a consistent experience throughout the application.

#### Acceptance Criteria

1. WHEN accessing AI trip wizard THEN the System SHALL preserve existing SimplifiedTripWizard functionality:

   **Preserved Functionality**:
   - Component: `frontend/src/components/agents/SimplifiedTripWizard.tsx`
   - All existing form fields and validation logic
   - Backend API integration: POST /api/itineraries
   - Request format: CreateItineraryRequest DTO
   - State management: Zustand store for trip data
   - Navigation flow: Wizard → Agent Progress → Trip View
   - Error handling and retry logic
   - Form persistence in local storage
   - Multi-step wizard navigation
   
   **Data Structure (Preserved)**:
   ```typescript
   interface CreateItineraryRequest {
     destination: string;
     startDate: string; // ISO format
     endDate: string; // ISO format
     travelers: {
       adults: number;
       children: number;
       infants: number;
     };
     preferences: {
       budget: 'budget' | 'moderate' | 'luxury';
       pace: 'relaxed' | 'moderate' | 'packed';
       interests: string[]; // e.g., ['culture', 'food', 'adventure']
     };
     accommodation: {
       type: 'hotel' | 'hostel' | 'apartment' | 'resort';
       starRating: number; // 1-5
     };
     transportation: {
       preferred: 'flight' | 'train' | 'bus' | 'car';
     };
   }
   ```

2. WHEN viewing wizard THEN the System SHALL apply EaseMyTrip styling:

   **Wizard Container** (`components/ai-planner/StyledTripWizard.tsx`):
   
   **Page Layout**:
   - Min-height: 100vh
   - Background: `linear-gradient(to bottom, hsl(207, 100%, 40%) 0%, hsl(207, 100%, 45%) 30%, white 30%)`
   - Display: Flex
   - Align-items: Center
   - Justify-content: Center
   - Padding: 40px 16px
   
   **Wizard Card**:
   - Max-width: 800px
   - Width: 100%
   - Background: White
   - Border-radius: 16px (rounded-xl)
   - Box-shadow: `--shadow-premium-xl`
   - Overflow: Hidden
   
   **Wizard Header**:
   - Background: `linear-gradient(135deg, hsl(207, 100%, 40%), hsl(220, 100%, 45%))`
   - Padding: 32px
   - Color: White
   - Text-align: Center
   
   **Header Icon**:
   - Size: 64x64px
   - Background: `rgba(255, 255, 255, 0.2)`
   - Border-radius: 50%
   - Display: Flex
   - Align-items: Center
   - Justify-content: Center
   - Margin: 0 auto 16px
   - Icon: `<Sparkles />` (32x32px, white)
   
   **Header Title**:
   - Font-size: 28px (text-3xl)
   - Font-weight: 700 (font-bold)
   - Margin-bottom: 8px
   - Text: "Plan Your Perfect Trip with AI"
   
   **Header Subtitle**:
   - Font-size: 16px (text-base)
   - Opacity: 0.9
   - Text: "Answer a few questions and let our AI create your personalized itinerary"
   
   **Progress Indicator**:
   - Display: Flex
   - Justify-content: Space-between
   - Padding: 24px 32px
   - Background: `hsl(210, 40%, 96%)`
   - Border-bottom: 1px solid `hsl(214, 32%, 91%)`
   
   **Progress Step**:
   - Display: Flex
   - Flex-direction: Column
   - Align-items: Center
   - Gap: 8px
   - Position: Relative
   
   **Step Circle**:
   - Size: 40px
   - Border-radius: 50%
   - Display: Flex
   - Align-items: Center
   - Justify-content: Center
   - Font-weight: 600
   - Transition: All 300ms ease
   
   **Step States**:
   - Completed:
     - Background: `hsl(142, 76%, 36%)` (green)
     - Color: White
     - Icon: `<Check />` (20x20px)
   
   - Active:
     - Background: `hsl(207, 100%, 40%)` (primary blue)
     - Color: White
     - Border: 3px solid `hsl(207, 100%, 85%)`
     - Box-shadow: `0 0 0 4px rgba(0, 112, 218, 0.1)`
   
   - Inactive:
     - Background: White
     - Color: `hsl(215, 16%, 47%)`
     - Border: 2px solid `hsl(214, 32%, 91%)`
   
   **Step Label**:
   - Font-size: 12px (text-xs)
   - Font-weight: 500
   - Color: `hsl(215, 16%, 47%)` (inactive)
   - Color: `hsl(207, 100%, 40%)` (active)
   - Text-align: Center
   
   **Step Connector Line**:
   - Position: Absolute
   - Top: 20px
   - Left: 50%
   - Width: 100%
   - Height: 2px
   - Background: `hsl(214, 32%, 91%)` (inactive)
   - Background: `hsl(142, 76%, 36%)` (completed)
   - Z-index: -1
   
   **Wizard Content**:
   - Padding: 32px
   - Min-height: 400px
   
   **Form Section**:
   - Margin-bottom: 24px
   
   **Form Label**:
   - Font-size: 14px (text-sm)
   - Font-weight: 600 (font-semibold)
   - Color: `hsl(222, 47%, 11%)`
   - Margin-bottom: 8px
   - Display: Block
   
   **Required Indicator**:
   - Color: `hsl(0, 84%, 60%)` (red)
   - Margin-left: 4px
   - Text: "*"
   
   **Form Input**:
   - Width: 100%
   - Height: 48px
   - Padding: 0 16px
   - Border: 1px solid `hsl(214, 32%, 91%)`
   - Border-radius: 8px (rounded-lg)
   - Font-size: 16px
   - Transition: All 200ms ease
   
   **Input States**:
   - Focus:
     - Border-color: `hsl(207, 100%, 40%)`
     - Box-shadow: `0 0 0 3px rgba(0, 112, 218, 0.1)`
     - Outline: None
   
   - Error:
     - Border-color: `hsl(0, 84%, 60%)`
     - Box-shadow: `0 0 0 3px rgba(239, 68, 68, 0.1)`
   
   - Disabled:
     - Background: `hsl(210, 40%, 96%)`
     - Cursor: Not-allowed
     - Opacity: 0.6
   
   **Error Message**:
   - Font-size: 12px (text-xs)
   - Color: `hsl(0, 84%, 60%)`
   - Margin-top: 4px
   - Display: Flex
   - Align-items: Center
   - Gap: 4px
   - Icon: `<AlertCircle />` (14x14px)
   
   **Helper Text**:
   - Font-size: 12px (text-xs)
   - Color: `hsl(215, 16%, 47%)`
   - Margin-top: 4px
   
   **Wizard Footer**:
   - Display: Flex
   - Justify-content: Space-between
   - Padding: 24px 32px
   - Border-top: 1px solid `hsl(214, 32%, 91%)`
   - Background: `hsl(210, 40%, 96%)`
   
   **Navigation Buttons**:
   - Back Button:
     - Background: White
     - Color: `hsl(222, 47%, 11%)`
     - Border: 1px solid `hsl(214, 32%, 91%)`
     - Height: 48px
     - Padding: 0 24px
     - Border-radius: 8px
     - Font-weight: 500
     - Icon: `<ChevronLeft />` (20x20px, left)
   
   - Next/Submit Button:
     - Background: `hsl(207, 100%, 40%)`
     - Color: White
     - Height: 48px
     - Padding: 0 32px
     - Border-radius: 8px
     - Font-weight: 600
     - Icon: `<ChevronRight />` or `<Check />` (20x20px, right)
     - Hover: Background darken, lift 2px
     - Disabled: Opacity 0.5, cursor not-allowed

3. WHEN viewing Step 1 (Destination) THEN the System SHALL display:

   **Step 1: Destination Selection**:
   
   **Heading**:
   - Text: "Where would you like to go?"
   - Font-size: 24px (text-2xl)
   - Font-weight: 700
   - Margin-bottom: 24px
   
   **Destination Input**:
   - Label: "Destination"
   - Type: Autocomplete (Radix UI Combobox)
   - Placeholder: "Enter city or country"
   - Icon: `<MapPin />` (left side, 20x20px)
   - Autocomplete source: Popular destinations API or static list
   - Min characters: 2
   - Max results: 10
   - Debounce: 300ms
   
   **Popular Destinations Grid**:
   - Heading: "Popular Destinations"
   - Display: Grid
   - Columns: 3 on desktop, 2 on tablet, 1 on mobile
   - Gap: 12px
   - Margin-top: 24px
   
   **Destination Chip**:
   - Padding: 12px 16px
   - Border: 2px solid `hsl(214, 32%, 91%)`
   - Border-radius: 8px
   - Cursor: Pointer
   - Transition: All 200ms ease
   - Display: Flex
   - Align-items: Center
   - Gap: 8px
   
   **Chip Content**:
   - Icon: Country flag emoji or icon
   - Text: Destination name
   - Font-size: 14px
   - Font-weight: 500
   
   **Chip States**:
   - Hover: Border-color primary blue, background light blue (5% opacity)
   - Selected: Border-color primary blue, background primary blue (10% opacity)
   
   **Validation**:
   - Required field
   - Must be valid destination from autocomplete
   - Error: "Please select a destination"

4. WHEN viewing Step 2 (Dates & Travelers) THEN the System SHALL display:

   **Step 2: Travel Details**:
   
   **Heading**:
   - Text: "When are you traveling?"
   - Font-size: 24px (text-2xl)
   - Font-weight: 700
   - Margin-bottom: 24px
   
   **Date Range Picker**:
   - Label: "Travel Dates"
   - Component: Radix UI Popover + Calendar
   - Display: Two inputs side-by-side (Start Date | End Date)
   - Icon: `<Calendar />` (left side of each input)
   - Calendar: Month view, range selection
   - Min date: Today
   - Max date: 1 year from today
   - Highlight: Selected range in primary blue
   
   **Duration Display**:
   - Text: "X nights, Y days"
   - Font-size: 14px
   - Color: `hsl(207, 100%, 40%)`
   - Margin-top: 8px
   - Auto-calculated from date range
   
   **Travelers Section**:
   - Heading: "Who's traveling?"
   - Margin-top: 32px
   
   **Traveler Counter**:
   - Display: Flex
   - Justify-content: Space-between
   - Align-items: Center
   - Padding: 16px
   - Border: 1px solid `hsl(214, 32%, 91%)`
   - Border-radius: 8px
   - Margin-bottom: 12px
   
   **Counter Label**:
   - Font-size: 16px
   - Font-weight: 500
   - Color: `hsl(222, 47%, 11%)`
   
   **Counter Sublabel**:
   - Font-size: 12px
   - Color: `hsl(215, 16%, 47%)`
   - Margin-top: 2px
   
   **Counter Controls**:
   - Display: Flex
   - Align-items: Center
   - Gap: 16px
   
   **Counter Button**:
   - Size: 36x36px
   - Border-radius: 50%
   - Border: 1px solid `hsl(214, 32%, 91%)`
   - Background: White
   - Cursor: Pointer
   - Icon: `<Minus />` or `<Plus />` (16x16px)
   - Hover: Background light blue, border primary blue
   - Disabled: Opacity 0.5, cursor not-allowed
   
   **Counter Value**:
   - Font-size: 18px
   - Font-weight: 600
   - Min-width: 32px
   - Text-align: Center
   
   **Traveler Types**:
   - Adults (18+ years): Min 1, Max 9
   - Children (2-17 years): Min 0, Max 8
   - Infants (under 2): Min 0, Max 2
   
   **Validation**:
   - Start date required
   - End date required
   - End date must be after start date
   - At least 1 adult required
   - Total travelers ≤ 10

5. WHEN viewing Step 3 (Preferences) THEN the System SHALL display:

   **Step 3: Travel Preferences**:
   
   **Heading**:
   - Text: "Tell us about your travel style"
   - Font-size: 24px (text-2xl)
   - Font-weight: 700
   - Margin-bottom: 24px
   
   **Budget Selection**:
   - Label: "Budget per person"
   - Display: Radio group (Radix UI RadioGroup)
   - Layout: Grid 3 columns
   - Gap: 12px
   
   **Budget Option Card**:
   - Padding: 20px
   - Border: 2px solid `hsl(214, 32%, 91%)`
   - Border-radius: 12px
   - Cursor: Pointer
   - Transition: All 200ms ease
   - Text-align: Center
   
   **Budget Options**:
   - Budget:
     - Icon: `<Wallet />` (32x32px, green)
     - Label: "Budget"
     - Range: "₹20,000 - ₹40,000"
     - Description: "Affordable stays and local experiences"
   
   - Moderate:
     - Icon: `<Briefcase />` (32x32px, blue)
     - Label: "Moderate"
     - Range: "₹40,000 - ₹80,000"
     - Description: "Comfortable hotels and popular attractions"
   
   - Luxury:
     - Icon: `<Crown />` (32x32px, gold)
     - Label: "Luxury"
     - Range: "₹80,000+"
     - Description: "Premium stays and exclusive experiences"
   
   **Budget Card States**:
   - Hover: Border-color primary blue, shadow-sm
   - Selected: Border-color primary blue (3px), background light blue (5%)
   
   **Pace Selection**:
   - Label: "Travel Pace"
   - Margin-top: 32px
   - Display: Radio group
   - Layout: Grid 3 columns
   
   **Pace Options**:
   - Relaxed:
     - Icon: `<Coffee />` (24x24px)
     - Label: "Relaxed"
     - Description: "2-3 activities per day"
   
   - Moderate:
     - Icon: `<Footprints />` (24x24px)
     - Label: "Moderate"
     - Description: "4-5 activities per day"
   
   - Packed:
     - Icon: `<Zap />` (24x24px)
     - Label: "Packed"
     - Description: "6+ activities per day"
   
   **Interests Selection**:
   - Label: "Interests (Select all that apply)"
   - Margin-top: 32px
   - Display: Checkbox group
   - Layout: Grid 3 columns on desktop, 2 on mobile
   - Gap: 12px
   
   **Interest Chip**:
   - Padding: 12px 16px
   - Border: 2px solid `hsl(214, 32%, 91%)`
   - Border-radius: 8px
   - Cursor: Pointer
   - Display: Flex
   - Align-items: Center
   - Gap: 8px
   - Transition: All 200ms ease
   
   **Interest Options**:
   - Culture (icon: `<Landmark />`)
   - Food & Dining (icon: `<UtensilsCrossed />`)
   - Adventure (icon: `<Mountain />`)
   - Nature (icon: `<Trees />`)
   - Shopping (icon: `<ShoppingBag />`)
   - Nightlife (icon: `<Music />`)
   - History (icon: `<BookOpen />`)
   - Beach & Relaxation (icon: `<Waves />`)
   - Photography (icon: `<Camera />`)
   
   **Interest Chip States**:
   - Unchecked: Border gray, background white
   - Checked: Border primary blue, background primary blue (10%), checkmark icon
   - Hover: Border primary blue
   
   **Validation**:
   - Budget required
   - Pace required
   - At least 1 interest required

6. WHEN viewing Step 4 (Accommodation & Transport) THEN the System SHALL display:

   **Step 4: Accommodation & Transportation**:
   
   **Heading**:
   - Text: "Choose your preferences"
   - Font-size: 24px (text-2xl)
   - Font-weight: 700
   - Margin-bottom: 24px
   
   **Accommodation Type**:
   - Label: "Accommodation Type"
   - Display: Radio group
   - Layout: Grid 2 columns
   - Gap: 12px
   
   **Accommodation Options**:
   - Hotel:
     - Icon: `<Hotel />` (32x32px)
     - Label: "Hotel"
     - Description: "Traditional hotel stays"
   
   - Hostel:
     - Icon: `<Bed />` (32x32px)
     - Label: "Hostel"
     - Description: "Budget-friendly shared spaces"
   
   - Apartment:
     - Icon: `<Home />` (32x32px)
     - Label: "Apartment"
     - Description: "Self-catering apartments"
   
   - Resort:
     - Icon: `<Palmtree />` (32x32px)
     - Label: "Resort"
     - Description: "All-inclusive resorts"
   
   **Star Rating**:
   - Label: "Preferred Star Rating"
   - Margin-top: 24px
   - Display: Radio group
   - Layout: Horizontal flex
   - Gap: 8px
   
   **Star Rating Options**:
   - 3 Stars: "★★★"
   - 4 Stars: "★★★★"
   - 5 Stars: "★★★★★"
   - Each option: Padding 12px 20px, border, border-radius 8px
   
   **Transportation Preference**:
   - Label: "Preferred Transportation"
   - Margin-top: 32px
   - Display: Radio group
   - Layout: Grid 2 columns
   - Gap: 12px
   
   **Transportation Options**:
   - Flight:
     - Icon: `<Plane />` (32x32px)
     - Label: "Flight"
     - Description: "Fastest option"
   
   - Train:
     - Icon: `<Train />` (32x32px)
     - Label: "Train"
     - Description: "Scenic routes"
   
   - Bus:
     - Icon: `<Bus />` (32x32px)
     - Label: "Bus"
     - Description: "Budget-friendly"
   
   - Car:
     - Icon: `<Car />` (32x32px)
     - Label: "Car"
     - Description: "Flexible travel"
   
   **Validation**:
   - Accommodation type required
   - Star rating required
   - Transportation required

7. WHEN submitting wizard THEN the System SHALL:

   **Submission Process**:
   
   **Loading State**:
   - Disable all form inputs
   - Show loading spinner on submit button
   - Button text: "Creating Your Itinerary..."
   - Prevent navigation away
   
   **API Call**:
   - Endpoint: POST /api/itineraries
   - Headers: Authorization Bearer token
   - Body: CreateItineraryRequest (JSON)
   - Timeout: 30 seconds
   
   **Success Handling**:
   - Store itinerary ID in Zustand store
   - Navigate to: `/generating?itineraryId={id}`
   - Pass trip data to agent progress component
   - Clear wizard form data from local storage
   
   **Error Handling**:
   - Network error: Show retry button, "Connection failed. Please try again."
   - Validation error: Show field-specific errors
   - Server error: Show error modal with support contact
   - Timeout: Show retry button, "Request timed out. Please try again."
   
   **Error Modal**:
   - Title: "Oops! Something went wrong"
   - Icon: `<AlertTriangle />` (48x48px, red)
   - Message: Error description
   - Actions: "Try Again" button, "Contact Support" link
   - Close: X button, Esc key, backdrop click

8. WHEN viewing wizard on mobile THEN the System SHALL adapt:

   **Mobile Adaptations** (<768px):
   
   **Wizard Card**:
   - Max-width: 100%
   - Border-radius: 0 (full screen)
   - Min-height: 100vh
   
   **Progress Indicator**:
   - Horizontal scroll if needed
   - Smaller step circles (32px)
   - Hide step labels, show only on active step
   
   **Form Layout**:
   - Single column for all grids
   - Larger touch targets (min 44x44px)
   - Increased spacing between elements
   
   **Navigation Buttons**:
   - Full width
   - Stack vertically
   - Back button above Next button



### Requirement 4: AI Agent Progress (Restyled)

**User Story:** As a user, I want to see AI itinerary generation progress with premium animations, so that I feel engaged while waiting.

#### Acceptance Criteria

1. WHEN itinerary is generating THEN the System SHALL preserve existing SimplifiedAgentProgress functionality:

   **Preserved Functionality**:
   - Component: `frontend/src/components/agents/SimplifiedAgentProgress.tsx`
   - SSE connection to: GET /agents/events/{itineraryId}
   - Real-time progress updates via Server-Sent Events
   - Agent task tracking and status updates
   - Error handling and automatic retry logic
   - Connection management via sseManager.ts
   - Automatic reconnection with exponential backoff
   - Progress calculation and smooth animations
   - Completion detection and navigation
   
   **SSE Event Types (Preserved)**:
   ```typescript
   interface AgentEvent {
     type: 'connected' | 'agent-progress' | 'agent-complete' | 'agent-error';
     data: {
       itineraryId: string;
       executionId: string;
       progress: number; // 0-100
       currentTask: string;
       completedTasks: string[];
       estimatedTimeRemaining: number; // seconds
       message: string;
     };
   }
   ```
   
   **Progress Calculation**:
   - Total tasks: Dynamic based on trip complexity
   - Completed tasks: Tracked from agent events
   - Progress percentage: (completed / total) * 100
   - Smooth animation: useSmoothProgress hook (eases progress changes)
   - Time estimation: Based on average task completion time

2. WHEN viewing progress THEN the System SHALL display premium UI:

   **Progress Page Layout** (`components/ai-planner/EnhancedAgentProgress.tsx`):
   
   **Page Container**:
   - Min-height: 100vh
   - Background: `linear-gradient(to bottom right, hsl(207, 100%, 40%) 0%, hsl(207, 100%, 45%) 50%, hsl(210, 40%, 96%) 100%)`
   - Display: Flex
   - Align-items: Center
   - Justify-content: Center
   - Padding: 40px 16px
   - Position: Relative
   - Overflow: Hidden
   
   **Animated Background Elements**:
   - Floating particles (10-15 elements)
   - Size: 4-12px circles
   - Color: `rgba(255, 255, 255, 0.3)`
   - Animation: Float up and fade out (5-10s duration)
   - Random horizontal drift
   - Continuous generation
   
   **Progress Card**:
   - Max-width: 600px
   - Width: 100%
   - Background: White
   - Border-radius: 20px (rounded-2xl)
   - Box-shadow: `--shadow-premium-xl`
   - Padding: 48px 40px
   - Position: Relative
   - Z-index: 10
   - Animation: Scale-in (500ms, ease-out)
   
   **Header Section**:
   - Text-align: Center
   - Margin-bottom: 40px
   
   **Icon Container**:
   - Size: 80x80px
   - Background: `linear-gradient(135deg, hsl(207, 100%, 40%), hsl(220, 100%, 45%))`
   - Border-radius: 50%
   - Display: Flex
   - Align-items: Center
   - Justify-content: Center
   - Margin: 0 auto 24px
   - Box-shadow: `0 8px 24px rgba(0, 112, 218, 0.3)`
   - Animation: Pulse (2s infinite)
   
   **Icon**:
   - Element: `<Sparkles />`
   - Size: 40x40px
   - Color: White
   - Animation: Rotate (3s infinite linear)
   
   **Heading**:
   - Text: "Creating Your Perfect Itinerary"
   - Font-size: 28px (text-3xl)
   - Font-weight: 700 (font-bold)
   - Color: `hsl(222, 47%, 11%)`
   - Margin-bottom: 12px
   - Animation: Fade-in (300ms, 200ms delay)
   
   **Subheading**:
   - Text: "Our AI is crafting a personalized travel experience just for you"
   - Font-size: 16px (text-base)
   - Color: `hsl(215, 16%, 47%)`
   - Line-height: 1.5
   - Max-width: 400px
   - Margin: 0 auto
   - Animation: Fade-in (300ms, 400ms delay)

3. WHEN receiving progress updates THEN the System SHALL animate progress bar:

   **Progress Bar Section**:
   
   **Container**:
   - Margin: 40px 0
   - Animation: Fade-in (300ms, 600ms delay)
   
   **Progress Label**:
   - Display: Flex
   - Justify-content: Space-between
   - Align-items: Center
   - Margin-bottom: 12px
   
   **Current Task Text**:
   - Font-size: 14px (text-sm)
   - Font-weight: 600 (font-semibold)
   - Color: `hsl(207, 100%, 40%)`
   - Animation: Fade-in when task changes (200ms)
   
   **Progress Percentage**:
   - Font-size: 24px (text-2xl)
   - Font-weight: 700 (font-bold)
   - Color: `hsl(222, 47%, 11%)`
   - Format: "XX%"
   - Animation: Count-up effect (smooth number transition)
   
   **Progress Bar Track**:
   - Height: 12px
   - Width: 100%
   - Background: `hsl(210, 40%, 96%)`
   - Border-radius: 9999px (rounded-full)
   - Overflow: Hidden
   - Position: Relative
   
   **Progress Bar Fill**:
   - Height: 100%
   - Background: `linear-gradient(90deg, hsl(207, 100%, 40%) 0%, hsl(26, 100%, 55%) 100%)`
   - Border-radius: 9999px
   - Transition: Width 500ms cubic-bezier(0.4, 0, 0.2, 1)
   - Position: Relative
   - Box-shadow: `0 0 10px rgba(0, 112, 218, 0.5)`
   
   **Progress Bar Shimmer**:
   - Position: Absolute
   - Inset: 0
   - Background: `linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent)`
   - Animation: Shimmer (1.5s infinite)
   - Keyframes: TranslateX(-100%) → TranslateX(100%)
   
   **Time Remaining**:
   - Font-size: 12px (text-xs)
   - Color: `hsl(215, 16%, 47%)`
   - Margin-top: 8px
   - Text-align: Right
   - Format: "Estimated time: X min Y sec"
   - Update every second
   - Animation: Fade when updating

4. WHEN agent is working THEN the System SHALL display motivational messages:

   **Motivational Messages Section**:
   
   **Container**:
   - Margin-top: 40px
   - Min-height: 80px
   - Display: Flex
   - Align-items: Center
   - Justify-content: Center
   
   **Message Card**:
   - Display: Flex
   - Align-items: Center
   - Gap: 16px
   - Padding: 20px
   - Background: `hsl(210, 40%, 96%)`
   - Border-radius: 12px
   - Animation: Fade-in-scale (400ms) when message changes
   
   **Message Icon**:
   - Size: 32x32px
   - Color: `hsl(207, 100%, 40%)`
   - Animation: Bounce (1s) when message changes
   
   **Message Text**:
   - Font-size: 14px (text-sm)
   - Color: `hsl(222, 47%, 11%)`
   - Font-weight: 500
   - Line-height: 1.4
   
   **Message Rotation**:
   - Change message every 3-4 seconds
   - Smooth fade transition (300ms)
   - Cycle through all messages
   - Match icon to message type
   
   **Message List**:
   ```typescript
   const motivationalMessages = [
     {
       icon: <MapPin />,
       text: "Analyzing destinations and finding the best spots for you..."
     },
     {
       icon: <Hotel />,
       text: "Finding perfect stays that match your preferences..."
     },
     {
       icon: <UtensilsCrossed />,
       text: "Discovering local cuisine and must-try restaurants..."
     },
     {
       icon: <Camera />,
       text: "Identifying photo-worthy locations and viewpoints..."
     },
     {
       icon: <Compass />,
       text: "Planning optimal routes and daily schedules..."
     },
     {
       icon: <Star />,
       text: "Curating unique experiences based on your interests..."
     },
     {
       icon: <Clock />,
       text: "Optimizing timing for activities and attractions..."
     },
     {
       icon: <Wallet />,
       text: "Balancing your budget across accommodations and activities..."
     },
     {
       icon: <Sun />,
       text: "Checking weather patterns for the best travel experience..."
     },
     {
       icon: <Users />,
       text: "Tailoring recommendations for your group size..."
     }
   ];
   ```

5. WHEN generation completes THEN the System SHALL navigate to trip view:

   **Completion Handling**:
   
   **Success Animation**:
   - Progress bar fills to 100%
   - Icon changes to `<Check />` with green background
   - Heading changes to: "Your Itinerary is Ready!"
   - Subheading: "Get ready for an amazing trip"
   - Animation: Confetti burst (2s)
   - Checkmark: Scale-in with bounce (500ms)
   
   **Confetti Effect**:
   - Library: canvas-confetti or custom implementation
   - Colors: Primary blue, orange, green
   - Duration: 2 seconds
   - Particle count: 100-150
   - Spread: 360 degrees
   - Origin: Center of screen
   
   **Success Card**:
   - Background: `linear-gradient(135deg, hsl(142, 76%, 36%), hsl(142, 76%, 40%))`
   - Color: White
   - Padding: 24px
   - Border-radius: 12px
   - Margin-top: 24px
   - Animation: Slide-up (400ms, 500ms delay)
   
   **Success Message**:
   - Icon: `<Check />` (24x24px)
   - Text: "Itinerary created successfully!"
   - Font-size: 16px
   - Font-weight: 600
   
   **View Itinerary Button**:
   - Text: "View My Itinerary"
   - Width: Full width
   - Height: 56px
   - Background: White
   - Color: `hsl(142, 76%, 36%)`
   - Font-size: 16px
   - Font-weight: 600
   - Border-radius: 8px
   - Margin-top: 16px
   - Icon: `<ArrowRight />` (20x20px, right)
   - Hover: Transform scale(1.02), shadow increase
   
   **Navigation**:
   - Delay: 1 second after completion (allow animation to play)
   - Route: `/trip/${itineraryId}`
   - Transition: Fade out progress page, fade in trip view
   - Pass itinerary data to avoid refetch
   - Update Zustand store with current trip
   
   **Error Handling**:
   - If navigation fails: Show error modal
   - Provide "Try Again" button
   - Log error for debugging

6. WHEN generation fails THEN the System SHALL display error state:

   **Error Handling**:
   
   **Error Detection**:
   - SSE event type: 'agent-error'
   - Connection timeout: 60 seconds
   - Network error: Connection lost
   - Server error: 500 status code
   
   **Error UI**:
   - Icon changes to `<AlertTriangle />` with red background
   - Heading: "Oops! Something went wrong"
   - Subheading: Error message from server or generic message
   - Progress bar: Red color
   - Remove motivational messages
   
   **Error Card**:
   - Background: `hsl(0, 84%, 60%)` (red)
   - Color: White
   - Padding: 24px
   - Border-radius: 12px
   - Margin-top: 24px
   
   **Error Message**:
   - Icon: `<AlertCircle />` (24x24px)
   - Text: Specific error description
   - Font-size: 14px
   - Line-height: 1.5
   
   **Error Actions**:
   - Retry Button:
     - Text: "Try Again"
     - Width: Full width
     - Height: 56px
     - Background: White
     - Color: `hsl(0, 84%, 60%)`
     - Margin-bottom: 12px
     - Click: Restart generation process
   
   - Go Back Button:
     - Text: "Go Back to Wizard"
     - Width: Full width
     - Height: 56px
     - Background: Transparent
     - Color: White
     - Border: 2px solid white
     - Click: Navigate back to wizard with form data preserved
   
   - Contact Support Link:
     - Text: "Contact Support"
     - Font-size: 14px
     - Color: White
     - Text-decoration: Underline
     - Margin-top: 16px
     - Click: Open support modal or email

7. WHEN user cancels generation THEN the System SHALL handle cancellation:

   **Cancellation Feature**:
   
   **Cancel Button**:
   - Position: Bottom of progress card
   - Text: "Cancel"
   - Width: Auto
   - Height: 40px
   - Background: Transparent
   - Color: `hsl(215, 16%, 47%)`
   - Border: 1px solid `hsl(214, 32%, 91%)`
   - Border-radius: 8px
   - Padding: 0 24px
   - Font-size: 14px
   - Hover: Border-color red, color red
   
   **Confirmation Modal**:
   - Title: "Cancel Itinerary Generation?"
   - Message: "Your progress will be lost. Are you sure you want to cancel?"
   - Actions:
     - "Yes, Cancel": Red button, closes SSE, navigates to dashboard
     - "No, Continue": Primary button, closes modal
   
   **Cancellation Process**:
   - Close SSE connection
   - Call backend API: DELETE /api/itineraries/{id} (if applicable)
   - Clear Zustand store
   - Navigate to: Dashboard or wizard
   - Show toast: "Generation cancelled"

8. WHEN viewing progress on mobile THEN the System SHALL adapt:

   **Mobile Adaptations** (<768px):
   
   **Progress Card**:
   - Padding: 32px 24px
   - Border-radius: 16px
   
   **Icon Container**:
   - Size: 64x64px
   - Icon: 32x32px
   
   **Heading**:
   - Font-size: 24px (text-2xl)
   
   **Subheading**:
   - Font-size: 14px (text-sm)
   
   **Progress Bar**:
   - Height: 10px
   
   **Message Card**:
   - Flex-direction: Column
   - Text-align: Center
   - Gap: 12px
   
   **Buttons**:
   - Full width
   - Height: 48px

