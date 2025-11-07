# Requirements 5-8: Trip Management & Booking

**Design Philosophy**: Apple.com refinement + Emirates.com luxury + EaseMyTrip functionality  
**Design System**: Material 3 + Apple HIG + Atlassian principles

**📍 Source**: This document extracts Requirements 5-8 from the main [requirements.md](requirements.md) file.

**📦 Contents**:
- Requirement 5: Unified Trip Management Interface
- Requirement 6: Provider Booking with Embedded Iframes (Apple HIG Modals)
- Requirement 7: Standard Booking Flow (Homepage Search)
- Requirement 8: User Dashboard

## 🎨 Premium Design Standards (Apply to All Requirements)

- **Layout**: 12-column grid, 8px spacing increments
- **Colors**: Primary #002B5B, Secondary #F5C542, Neutrals #F8F8F8/#E0E0E0
- **Typography**: Inter/SF Pro Display, 16px body, 1.5 line-height
- **Elevation**: 3 layers (elevation-1, elevation-2, elevation-3)
- **Motion**: 300ms cubic-bezier(0.4,0,0.2,1), 60fps target
- **Radius**: Max 12px (no over-rounding)
- **Touch Targets**: Min 48x48px

---

## ⚠️ IMPORTANT: Full Detail Location

**The main `requirements.md` file contains the COMPLETE extremely detailed specifications** for all requirements. This file provides structured summaries for quick reference.

---

## Requirement 5: Unified Trip Management Interface

**📍 Main file location**: Lines 595-657

**User Story:** As a user, I want a comprehensive trip management interface with sidebar navigation, so that I can access all trip features in one place.

### Summary

Unified interface with sidebar navigation containing:
- **View Tab**: Trip overview with statistics
- **Plan Tab**: Destinations and day-by-day itinerary
- **Bookings Tab**: Provider integration and booking management
- **Budget Tab**: Cost tracking and visualization
- **Packing Tab**: Smart packing list generation
- **Docs Tab**: Travel documents management

### Key Components

1. **`components/trip-management/UnifiedTripView.tsx`**
   - Main container with sidebar + content area
   - Sidebar: 280px width, fixed position
   - Content: Flex-grow, responsive

2. **Sidebar Navigation**
   - 6 tabs with icons and labels
   - Active: Primary blue background
   - Hover: Light blue background (5% opacity)
   - Tab height: 48px, icon 20x20px

3. **Tab Content Components**:
   - `ViewTab.tsx` - Overview with stats cards
   - `PlanTab.tsx` - Day-by-day itinerary (preserves existing functionality)
   - `BookingsTab.tsx` - Provider booking interface
   - `BudgetTab.tsx` - Cost breakdown with charts
   - `PackingTab.tsx` - Checklist with categories
   - `DocsTab.tsx` - Document management

### Critical Specifications

**Layout**:
- Sidebar: 280px fixed width, full height
- Content: Remaining width, padding 32px
- Mobile: Bottom navigation, full-width content

**View Tab**:
- Header: Destination, dates, travelers
- Stats cards: 4-column grid, key metrics
- Weather forecast widget
- Quick actions: Share, export, edit

**Plan Tab**:
- Destination selector dropdown
- Day-by-day view (existing DayByDayView component)
- Map integration showing all locations
- Preserves drag-and-drop, inline editing

**Bookings Tab**:
- List of bookable nodes
- Provider selection for each
- "Book Now" buttons
- Status indicators (planned, booked, confirmed)

**Budget Tab**:
- Total budget vs spent
- Category breakdown (accommodation, food, activities, transport)
- Charts using Recharts
- Cost per day visualization

**Packing Tab**:
- AI-generated suggestions
- Categories: Clothing, documents, electronics, toiletries
- Checkboxes to mark packed
- Add custom items

**Docs Tab**:
- Passport/visa requirements
- Booking confirmations
- Travel insurance
- Emergency contacts

---

## Requirement 6: Provider Booking with Embedded Iframes

**📍 Main file location**: Lines 658-710

**User Story:** As a user, I want to book hotels and activities through trusted providers without leaving the application, so that I have a seamless booking experience.

### Summary

Seamless booking flow with:
- **Provider Selection**: Modal with provider list
- **Embedded Iframe**: Provider website in modal
- **Mock Confirmation**: Simulated booking after 2-3 seconds
- **Backend Persistence**: Save booking data via API

### Key Components

1. **`components/booking/ProviderSelectionModal.tsx`**
   - List of providers with logos
   - Ratings and price estimates
   - "Select Provider" buttons

2. **`components/booking/BookingModal.tsx`**
   - Full-screen modal (max-width 1200px, height 80vh)
   - Embedded iframe with provider URL
   - Loading overlay
   - Close button

3. **`components/booking/MockConfirmationModal.tsx`**
   - Success animation (green checkmark, bounce)
   - Confirmation number (format: EMT{9-char})
   - Provider logo and name
   - "Continue Planning" button

### Critical Specifications

**Provider URLs**:
- Booking.com: `https://www.booking.com/searchresults.html?ss={location}&checkin={date}&checkout={date}&group_adults={count}`
- Expedia: `https://www.expedia.com/Hotel-Search?destination={location}&startDate={date}&endDate={date}&rooms={count}`
- Airbnb: `https://www.airbnb.com/s/{location}/homes?checkin={date}&checkout={date}&adults={count}`

**Iframe Specifications**:
- Width: 100%
- Height: 100%
- Sandbox: `allow-same-origin allow-scripts allow-forms`
- Loading overlay: Spinner + "Loading {provider}..."

**Mock Confirmation Timing**:
- Show after 2-3 seconds of iframe load
- Overlay on top of iframe
- Confetti animation optional
- Auto-close after user clicks "Continue"

**Backend Integration**:
- POST /api/bookings
- Update NormalizedNode.bookingRef
- Store: userId, itineraryId, nodeId, provider, confirmationNumber, status
- Show success toast

---

## Requirement 7: Standard Booking Flow (Homepage Search)

**📍 Main file location**: Lines 711-751

**User Story:** As a user, I want to search for flights and hotels from the homepage and book them, so that I can make standalone bookings outside of AI-generated itineraries.

### Summary

Complete search-to-booking flow:
- **Search Results**: Mock results with filters
- **Result Cards**: Flight/hotel cards with details
- **Booking Flow**: Same provider iframe system
- **Standalone Bookings**: Not linked to itinerary

### Key Components

1. **`components/search/SearchResults.tsx`**
   - Filters sidebar (280px width)
   - Results grid/list
   - Sort options
   - Pagination

2. **`components/search/FlightCard.tsx`**
   - Airline logo: 40x40px
   - Departure/arrival times
   - Duration and stops
   - Price: Right-aligned, 24px, bold
   - "Book Now" button

3. **`components/search/HotelCard.tsx`**
   - Hotel image: 200x150px
   - Star rating: 16px icons
   - Location with map preview
   - Amenities icons: 32x32px
   - Price per night
   - "Book Now" button

### Critical Specifications

**Search Results Page**:
- Layout: Sidebar + results area
- Loading: Skeleton loaders (1-2 seconds)
- Results: 10-20 per page
- Filters: Price range, rating, amenities, etc.

**Flight Cards**:
- Full width, padding 24px
- Border-radius: 12px
- Shadow: sm, increase to md on hover
- Airline logo: Left side
- Times: Large, bold
- Price: Right side, primary color

**Hotel Cards**:
- Image gallery: Carousel
- Star rating: Gold color #FFB800
- Amenities: Icon grid below description
- Location: With distance from center
- Reviews: Count and average rating

**Booking Flow**:
- Click "Book Now" → Provider selection modal
- Select provider → Booking modal with iframe
- After 2-3 seconds → Mock confirmation
- Save to user's bookings (not linked to itinerary)

---

## Requirement 8: User Dashboard

**📍 Main file location**: Lines 752-786

**User Story:** As a user, I want a dashboard showing all my trips and bookings, so that I can manage my travel plans in one place.

### Summary

Comprehensive dashboard with:
- **User Profile**: Picture, name, settings
- **Trip Cards**: Grid of all trips
- **Bookings Section**: Recent bookings list
- **Empty State**: When no trips exist

### Key Components

1. **`components/dashboard/TripDashboard.tsx`**
   - Main dashboard container
   - User profile header
   - Trip grid
   - Bookings section

2. **`components/dashboard/TripCard.tsx`**
   - Destination and dates
   - Thumbnail image
   - Status badge (upcoming, ongoing, completed)
   - Quick actions (view, edit, delete)

3. **`components/dashboard/EmptyState.tsx`**
   - Illustration: 120x120px
   - "No trips yet" message
   - "Create Your First Trip" button

### Critical Specifications

**Dashboard Layout**:
- Max-width: 1280px
- Padding: 32px
- User header: Full width, margin-bottom 32px

**User Profile Header**:
- Profile picture: 64x64px, circular
- Name: 24px, bold
- Settings link: Icon button
- Logout button: Secondary style

**Trip Grid**:
- Columns: 3 (desktop), 2 (tablet), 1 (mobile)
- Gap: 24px
- Card aspect ratio: 16:9 for image

**Trip Card**:
- Border-radius: 12px
- Shadow: sm, increase to md on hover
- Image: Full width, 200px height
- Content padding: 20px
- Status badge: Top-right corner

**Status Badges**:
- Upcoming: Blue background
- Ongoing: Green background
- Completed: Gray background
- Font-size: 12px, padding 4px 8px

**Bookings Section**:
- Heading: "Recent Bookings"
- List: Last 5 bookings
- Each item: Provider logo, destination, date, status
- "View All" link

**Empty State**:
- Centered: max-width 400px
- Icon: Gray color, large size
- Heading: 24px, bold
- Button: Primary color, large size

---

## Implementation Checklist

### Phase 1: Trip Management Interface (Week 6)
- [ ] Create unified trip view layout
- [ ] Implement sidebar navigation
- [ ] Build View tab with stats
- [ ] Integrate existing Plan tab (DayByDayView)
- [ ] Create Bookings tab structure
- [ ] Add Budget tab with charts
- [ ] Implement Packing tab
- [ ] Add Docs tab

### Phase 2: Provider Booking (Week 7)
- [ ] Create provider configuration
- [ ] Build provider selection modal
- [ ] Implement booking modal with iframe
- [ ] Add mock confirmation system
- [ ] Create backend API endpoints
- [ ] Test iframe embedding
- [ ] Verify booking persistence

### Phase 3: Search Flow (Week 8)
- [ ] Create search results page
- [ ] Build flight card component
- [ ] Build hotel card component
- [ ] Add filters sidebar
- [ ] Implement sort options
- [ ] Connect to booking flow
- [ ] Test standalone bookings

### Phase 4: Dashboard (Week 9)
- [ ] Create dashboard layout
- [ ] Build trip card component
- [ ] Add empty state
- [ ] Implement trip actions
- [ ] Add bookings section
- [ ] Test responsive layout

---

## Testing Strategy

### Functional Testing
- [ ] Sidebar navigation works
- [ ] All tabs load correctly
- [ ] Provider iframe embeds successfully
- [ ] Mock confirmation appears after 2-3 seconds
- [ ] Booking data persists to backend
- [ ] Search results display correctly
- [ ] Dashboard shows all trips
- [ ] Trip actions (view, edit, delete) work

### Integration Testing
- [ ] Booking flow end-to-end
- [ ] Search to booking flow
- [ ] Dashboard to trip view navigation
- [ ] Backend API integration
- [ ] SSE connections (if applicable)

### UI/UX Testing
- [ ] Sidebar responsive on mobile
- [ ] Iframe loads without errors
- [ ] Confirmation animation smooth
- [ ] Cards hover effects work
- [ ] Empty states display correctly

---

**📖 For complete implementation details, refer to [requirements.md](requirements.md)**
