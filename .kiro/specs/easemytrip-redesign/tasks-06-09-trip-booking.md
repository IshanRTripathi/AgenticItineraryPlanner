# Implementation Tasks: Weeks 6-9 (Trip Management & Booking)

**Design Philosophy**: Apple.com refinement + Emirates.com luxury + EaseMyTrip functionality  
**Timeline**: Weeks 6-9 of 18-week implementation  
**Focus**: Unified trip management interface and provider booking system  
**Requirements**: Requirements 5-8

## 🎨 Design System Standards (Apply to All Tasks)

- **Layout**: 12-column grid, 8px spacing increments
- **Colors**: Primary #002B5B, Secondary #F5C542, Neutrals #F8F8F8/#E0E0E0
- **Typography**: Inter/SF Pro Display, 16px body, 1.5 line-height
- **Elevation**: 3 layers (elevation-1, elevation-2, elevation-3)
- **Motion**: 300ms cubic-bezier(0.4,0,0.2,1), 60fps target
- **Radius**: Max 12px (no over-rounding)
- **Touch Targets**: Min 48x48px

---

## 📋 Week 6: Unified Trip Management Interface

**Goal**: Create comprehensive trip management interface with sidebar navigation

**Requirements**: Requirement 5 (Unified Trip Management Interface)

- [ ] 24. Trip View Layout & Sidebar Navigation
  - [ ] 24.1 Create unified trip view container
    - Create `frontend/src/pages/TripView.tsx`
    - Implement flex layout (sidebar + main content)
    - Full height (100vh)
    - Background: Light gray (muted)
    - Load trip data from GET /itineraries/{id}/json
    - Handle loading state with skeleton
    - Handle error state
    - _Requirements: 5.1_
  
  - [ ] 24.2 Create trip sidebar component
    - Create `frontend/src/components/trip/TripSidebar.tsx`
    - Fixed width: 280px
    - Full height
    - White background
    - Border-right: 1px solid border color
    - Flex column layout
    - _Requirements: 5.1, 5.2_
  
  - [ ] 24.3 Create sidebar header
    - Add back button (navigate to dashboard)
    - Show trip destination (truncated if long)
    - Show date range (formatted)
    - Show traveler count
    - Padding: 16px
    - Border-bottom: 1px solid border color
    - _Requirements: 5.3_
  
  - [ ] 24.4 Create sidebar navigation items
    - Create `frontend/src/components/trip/SidebarNavItem.tsx`
    - 6 navigation items: View, Plan, Bookings, Budget, Packing, Docs
    - Each item: Icon (20x20px) + Label
    - Height: 48px
    - Padding: 12px 16px
    - Active state: Primary blue background, white text
    - Inactive state: Transparent, gray text
    - Hover state: Light blue background (5% opacity)
    - Smooth transition (200ms)
    - _Requirements: 5.1, 5.2_
  
  - [ ] 24.5 Create sidebar footer with actions
    - Add "Share Trip" button (outline variant)
    - Add "Export PDF" button (outline variant)
    - Add "Delete Trip" button (ghost variant, red text)
    - Padding: 16px
    - Border-top: 1px solid border color
    - _Requirements: 5.3_
  
  - [ ] 24.6 Implement tab state management
    - Use URL parameter for active tab (/trip/:id?tab=view)
    - Update URL on tab change
    - Preserve tab on page refresh
    - Smooth content transition (fade 200ms)
    - _Requirements: 5.2_


- [ ] 25. View Tab: Trip Overview
  - [ ] 25.1 Create view tab component
    - Create `frontend/src/components/trip/tabs/ViewTab.tsx`
    - Full width, padding: 32px
    - Scrollable content
    - _Requirements: 5.3_
  
  - [ ] 25.2 Create trip header section
    - Show destination name (36px, bold)
    - Show destination image (full width, 300px height, rounded)
    - Show date range with calendar icon
    - Show traveler count with users icon
    - Show trip status badge (Upcoming, Ongoing, Completed)
    - _Requirements: 5.3_
  
  - [ ] 25.3 Create statistics cards grid
    - Grid: 4 columns on desktop, 2 on tablet, 1 on mobile
    - Gap: 16px
    - Each card: Icon + Label + Value
    - Cards: Total Days, Activities, Budget, Bookings
    - Padding: 20px
    - Shadow: sm
    - Hover: Shadow increase to md
    - _Requirements: 5.3_
  
  - [ ] 25.4 Create weather forecast widget
    - Show 7-day forecast for destination
    - Each day: Date, Icon, High/Low temp
    - Horizontal scroll on mobile
    - Use weather API or mock data
    - _Requirements: 5.3_
  
  - [ ] 25.5 Create quick actions section
    - Add "Edit Trip" button
    - Add "Share Trip" button
    - Add "Export PDF" button
    - Add "Add to Calendar" button
    - Grid layout: 2 columns
    - _Requirements: 5.3_

- [ ] 26. Plan Tab: Destinations & Day-by-Day
  - [ ] 26.1 Create plan tab container
    - Create `frontend/src/components/trip/tabs/PlanTab.tsx`
    - Implement sub-tabs: "Destinations" and "Day by day"
    - Use Radix UI Tabs
    - _Requirements: 5.4_
  
  - [ ] 26.2 Create destinations view
    - Create `frontend/src/components/trip/DestinationsView.tsx`
    - Split layout: Destination list (left) + Map (right)
    - Destination list: 320px width, scrollable
    - Map: Flex-grow, Google Maps integration
    - _Requirements: 5.4_
  
  - [ ] 26.3 Create destination list component
    - Show all destinations in itinerary
    - Each item: Destination name, days count, image thumbnail
    - Active destination: Highlighted
    - Click to select destination
    - Show destination details on selection
    - _Requirements: 5.4_
  
  - [ ] 26.4 Integrate Google Maps
    - Show all destinations as markers
    - Cluster markers if close together
    - Draw route between destinations
    - Center map on selected destination
    - Add zoom controls
    - Preserve existing map functionality
    - _Requirements: 5.4_
  
  - [ ] 26.5 Create day-by-day view
    - Reuse existing DayByDayView component
    - Apply EaseMyTrip styling
    - Ensure drag-and-drop works
    - Ensure inline editing works
    - Ensure add/remove nodes works
    - Ensure lock/unlock works
    - _Requirements: 5.4, 5.5_
  
  - [ ] 26.6 Redesign node cards for premium look
    - Create `frontend/src/components/trip/NodeCard.tsx`
    - Type-specific styling (attraction, meal, hotel, transit)
    - Icon with colored background (left side)
    - Title, timing, location (center)
    - Cost badge (right side)
    - "Book Now" button (if not booked)
    - "Booked" badge (if booked)
    - Border-left: 4px solid (type color)
    - Hover: Shadow increase, lift 2px
    - _Requirements: 5.5_


- [ ] 27. Budget Tab
  - [ ] 27.1 Create budget tab component
    - Create `frontend/src/components/trip/tabs/BudgetTab.tsx`
    - Show total budget vs spent
    - Category breakdown with Recharts
    - Cost per day visualization
    - _Requirements: 5.7_
  
  - [ ] 27.2 Implement budget charts
    - Pie chart for category breakdown
    - Bar chart for daily costs
    - Line chart for cumulative spending
    - Use Recharts library with EaseMyTrip colors
    - _Requirements: 5.7_

- [ ] 28. Packing Tab
  - [ ] 28.1 Create packing tab component
    - Create `frontend/src/components/trip/tabs/PackingTab.tsx`
    - AI-generated packing suggestions
    - Categorized checklist
    - Add custom items
    - _Requirements: 5.8_

- [ ] 29. Docs Tab
  - [ ] 29.1 Create docs tab component
    - Create `frontend/src/components/trip/tabs/DocsTab.tsx`
    - Passport/visa requirements
    - Booking confirmations
    - Travel insurance
    - Emergency contacts
    - _Requirements: 5.9_

---

## 📋 Week 7: Provider Booking System

**Goal**: Implement provider booking with embedded iframes and mock confirmations

**Requirements**: Requirement 6 (Provider Booking with Embedded Iframes)

- [ ] 30. Provider Configuration
  - [ ] 30.1 Create provider configuration file
    - Create `frontend/src/config/providers.ts`
    - Define Provider interface (id, name, logo, urlTemplate, verticals, active)
    - Add Booking.com configuration
    - Add Expedia configuration
    - Add Airbnb configuration
    - Add Agoda configuration
    - Add Hotels.com, Vio.com, Trip.com, Hostelworld
    - Add RailYatra, RedBus
    - _Requirements: 6.1, Req 13_
  
  - [ ] 30.2 Add provider logos
    - Create `frontend/public/assets/providers/` directory
    - Add placeholder logos (200x200px PNG)
    - booking.png, expedia.png, airbnb.png, agoda.png
    - hotels.png, vio.png, trip.png, hostelworld.png
    - railyatra.png, redbus.png
    - _Requirements: 6.1, Req 13_
  
  - [ ] 30.3 Create URL construction utility
    - Create `frontend/src/utils/providerUrls.ts`
    - Implement constructProviderUrl function
    - Replace placeholders with actual values
    - Encode parameters properly
    - _Requirements: 6.3, Req 13_

- [ ] 31. Bookings Tab Layout
  - [ ] 31.1 Create bookings tab component
    - Create `frontend/src/components/trip/tabs/BookingsTab.tsx`
    - Split layout: Provider sidebar (left) + Content (right)
    - Provider sidebar: 256px width
    - Content: Flex-grow
    - _Requirements: 5.6_
  
  - [ ] 31.2 Create provider sidebar
    - Create `frontend/src/components/trip/ProviderSidebar.tsx`
    - Group providers by category (Flights, Hotels, Transport)
    - Each category: Heading + Provider buttons
    - Scrollable content
    - _Requirements: 5.6_
  
  - [ ] 31.3 Create provider button component
    - Create `frontend/src/components/trip/ProviderButton.tsx`
    - Full width button
    - Provider logo (left, 20x20px)
    - Provider name (center)
    - Checkmark icon (right, if active)
    - Active state: Primary blue background
    - Inactive state: Ghost variant
    - _Requirements: 5.6_


- [ ] 32. Provider Booking Modal with Iframe (Apple HIG Modal)
  - [ ] 32.1 Create premium booking modal
    - Create `frontend/src/components/booking/BookingModal.tsx`
    - **Backdrop**: rgba(0,0,0,0.6), backdrop-filter blur(4px)
    - **Modal**: Max-width 1200px, height 80vh, border-radius 16px, shadow elevation-3
    - **Header**: Provider logo 48x48px, provider name 20px weight 600, "Secure Booking" badge
    - **Close**: Top-right, 40x40px, ghost button style
    - **Animation**: Backdrop fade 200ms, content scale 0.95→1 + fade 300ms
    - _Reference: Apple HIG Modals_
    - _Requirements: 6.2_
  
  - [ ] 32.2 Implement iframe embedding with loading state
    - **Iframe**: Width 100%, height calc(100% - 80px), border none, border-radius 0 0 16px 16px
    - **Sandbox**: allow-same-origin allow-scripts allow-forms
    - **Loading**: Skeleton with shimmer animation, "Loading {provider}..." text
    - **Error**: Show error message if iframe fails, "Try Another Provider" button
    - _Requirements: 6.2, 6.3, 6.4_
  
  - [ ] 32.3 Create loading overlay
    - Centered spinner
    - "Loading {provider}..." message
    - "Secure booking in progress" subtext
    - Animated loader
    - _Requirements: 6.4_
  
  - [ ] 32.4 Implement mock confirmation after 2-3 seconds (Material 3 Confirmation)
    - Create `frontend/src/components/booking/MockConfirmationModal.tsx`
    - **Timing**: Show after 2-3 seconds of iframe load
    - **Overlay**: White background, border-radius 16px, shadow elevation-3, padding 48px, centered
    - **Icon**: Green checkmark 64x64px, scale-in animation with bounce easing
    - **Heading**: "Booking Confirmed!" 24px weight 700, margin-top 16px
    - **Confirmation**: "EMT{9-char alphanumeric}", 18px weight 600, deep blue color
    - **Provider**: Logo 40x40px + name 16px, margin-top 24px
    - **Button**: "Continue Planning", primary style, full width, margin-top 32px
    - **Animation**: Scale-in 300ms with bounce, confetti optional
    - _Reference: Material 3 Confirmation Patterns_
    - _Requirements: 6.5_
  
  - [ ] 32.5 Implement backend booking persistence
    - Call POST /api/bookings on confirmation
    - Send: userId, itineraryId, nodeId, provider, confirmationNumber
    - Update NormalizedNode.bookingRef
    - Show success toast
    - Refresh trip view
    - _Requirements: 6.6, 6.7_

- [ ] 33. Provider Selection Modal
  - [ ] 33.1 Create provider selection modal
    - Create `frontend/src/components/booking/ProviderSelectionModal.tsx`
    - List of providers with logos
    - Provider ratings (mock data)
    - Estimated price range
    - "Select Provider" button for each
    - _Requirements: 6.1_
  
  - [ ] 33.2 Implement provider filtering
    - Filter by vertical (hotel, flight, activity)
    - Show only relevant providers
    - Sort by rating or price
    - _Requirements: 6.1_

- [ ]* 34. Booking System Testing
  - [ ]* 34.1 Test provider URL construction
  - [ ]* 34.2 Test iframe embedding
  - [ ]* 34.3 Test mock confirmation flow
  - [ ]* 34.4 Test backend integration
  - [ ]* 34.5 Test error handling
  - _Requirements: 6.1-6.7, Req 18_

---

## 📋 Week 8: Standard Booking Flow (Homepage Search)

**Goal**: Implement search results and standalone booking flow

**Requirements**: Requirement 7 (Standard Booking Flow)

- [ ] 35. Search Results Page
  - [ ] 35.1 Create search results page
    - Create `frontend/src/pages/SearchResults.tsx`
    - Layout: Filters sidebar + Results area
    - Show loading skeleton (1-2 seconds)
    - Display mock results
    - _Requirements: 7.1_
  
  - [ ] 35.2 Create filters sidebar
    - Width: 280px, sticky position
    - Price range slider
    - Rating filter (stars)
    - Amenities checkboxes
    - Sort options dropdown
    - "Apply Filters" button
    - _Requirements: 7.1_
  
  - [ ] 35.3 Create flight result cards
    - Create `frontend/src/components/search/FlightCard.tsx`
    - Airline logo (40x40px, left)
    - Departure/arrival times (large, bold)
    - Duration and stops
    - Price (right-aligned, 24px, bold, primary color)
    - "Book Now" button
    - Hover: Shadow increase to md
    - _Requirements: 7.2_
  
  - [ ] 35.4 Create hotel result cards
    - Create `frontend/src/components/search/HotelCard.tsx`
    - Hotel image gallery (200x150px, carousel)
    - Hotel name and star rating (gold #FFB800)
    - Location with map preview
    - Amenities icons (32x32px grid)
    - Price per night
    - Reviews count and average
    - "Book Now" button
    - _Requirements: 7.3_
  
  - [ ] 35.5 Create mock search results data
    - Create `frontend/src/data/mockSearchResults.ts`
    - Add 10-20 flight results
    - Add 10-20 hotel results
    - Include all required fields
    - _Requirements: 7.1-7.3_
  
  - [ ] 35.6 Implement pagination
    - Show 10 results per page
    - Page numbers at bottom
    - Previous/Next buttons
    - Scroll to top on page change
    - _Requirements: 7.1_

- [ ] 36. Standalone Booking Flow
  - [ ] 36.1 Connect search results to booking modal
    - Click "Book Now" → Provider selection modal
    - Select provider → Booking modal with iframe
    - After 2-3 seconds → Mock confirmation
    - _Requirements: 7.4_
  
  - [ ] 36.2 Implement standalone booking persistence
    - Save booking to user's account
    - Create standalone booking record (no itineraryId)
    - Show in user's bookings list
    - Send confirmation email (if configured)
    - _Requirements: 7.5_

- [ ]* 37. Search Flow Testing
  - [ ]* 37.1 Test search results display
  - [ ]* 37.2 Test filters functionality
  - [ ]* 37.3 Test booking flow
  - [ ]* 37.4 Test standalone booking persistence
  - _Requirements: 7.1-7.5, Req 18_

---

## 📋 Week 9: User Dashboard

**Goal**: Create user dashboard with trip management

**Requirements**: Requirement 8 (User Dashboard)

- [ ] 38. Dashboard Layout
  - [ ] 38.1 Create dashboard page
    - Create `frontend/src/pages/Dashboard.tsx`
    - Max-width: 1280px, centered
    - Padding: 32px
    - _Requirements: 8.1_
  
  - [ ] 38.2 Create user profile header
    - Profile picture (64x64px, circular)
    - User name (24px, bold)
    - Settings link (icon button)
    - Logout button (secondary style)
    - _Requirements: 8.1_
  
  - [ ] 38.3 Create empty state
    - Create `frontend/src/components/dashboard/EmptyState.tsx`
    - Illustration (120x120px, gray)
    - "No trips yet" message (24px, bold)
    - "Create Your First Trip" button (primary, large)
    - Centered, max-width 400px
    - _Requirements: 8.2_

- [ ] 39. Trip Cards Grid
  - [ ] 39.1 Create trip card component
    - Create `frontend/src/components/dashboard/TripCard.tsx`
    - Destination image (full width, 200px height)
    - Destination name and dates
    - Status badge (top-right: Upcoming, Ongoing, Completed)
    - Quick actions (view, edit, delete)
    - Border-radius: 12px
    - Shadow: sm, increase to md on hover
    - _Requirements: 8.3_
  
  - [ ] 39.2 Implement trip grid layout
    - Grid: 3 columns on desktop, 2 on tablet, 1 on mobile
    - Gap: 24px
    - Sort by date (upcoming first)
    - _Requirements: 8.3_
  
  - [ ] 39.3 Implement trip actions
    - View: Navigate to /trip/{id}
    - Edit: Navigate to wizard with pre-filled data
    - Delete: Show confirmation dialog, call DELETE API
    - _Requirements: 8.3, 8.5_

- [ ] 40. Bookings Section
  - [ ] 40.1 Create bookings section
    - Heading: "Recent Bookings"
    - List last 5 bookings
    - Each item: Provider logo, destination, date, status
    - "View All" link
    - _Requirements: 8.4_
  
  - [ ] 40.2 Implement booking status badges
    - Confirmed: Green background
    - Pending: Yellow background
    - Cancelled: Red background
    - Font-size: 12px, padding: 4px 8px
    - _Requirements: 8.4_

- [ ]* 41. Dashboard Testing
  - [ ]* 41.1 Test empty state display
  - [ ]* 41.2 Test trip cards display
  - [ ]* 41.3 Test trip actions
  - [ ]* 41.4 Test bookings section
  - [ ]* 41.5 Test responsive layout
  - _Requirements: 8.1-8.5, Req 18_

---

## 📝 Week 6-9 Summary

**Completed**:
- ✅ Unified trip management interface with 6 tabs
- ✅ Provider booking system with iframe embedding
- ✅ Mock confirmation flow (2-3 seconds)
- ✅ Search results page with filters
- ✅ Standalone booking flow
- ✅ User dashboard with trip management

**Next**: Week 10-13 (Animations & Backend) → See [tasks-10-13-animations-backend.md](tasks-10-13-animations-backend.md)

