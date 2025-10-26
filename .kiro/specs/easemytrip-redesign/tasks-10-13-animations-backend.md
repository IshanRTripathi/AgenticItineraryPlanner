# Implementation Tasks: Weeks 10-13 (Animations & Backend)

**Design Philosophy**: Apple.com refinement + Emirates.com luxury + EaseMyTrip functionality  
**Timeline**: Weeks 10-13 of 18-week implementation  
**Focus**: Authentication, animations, responsive design, backend booking entity  
**Requirements**: Requirements 9-12

## 🎨 Design System Standards (Apply to All Tasks)

- **Layout**: 12-column grid, 8px spacing increments
- **Colors**: Primary #002B5B, Secondary #F5C542, Neutrals #F8F8F8/#E0E0E0
- **Typography**: Inter/SF Pro Display, 16px body, 1.5 line-height
- **Elevation**: 3 layers (elevation-1, elevation-2, elevation-3)
- **Motion**: 300ms cubic-bezier(0.4,0,0.2,1), 60fps target
- **Radius**: Max 12px (no over-rounding)
- **Touch Targets**: Min 48x48px

---

## 📋 Week 10: Authentication (Redesigned)

**Goal**: Redesign login/signup page with EaseMyTrip styling

**Requirements**: Requirement 9 (Authentication Restyled)

- [ ] 42. Login Page Redesign
  - [ ] 42.1 Create new login page from scratch
    - Create `frontend/src/pages/LoginPage.tsx`
    - Design for million-dollar look
    - Split layout: 50/50 on desktop, stacked on mobile
    - Min-height: 100vh
    - _Requirements: 9.1, 9.2_
  
  - [ ] 42.2 Create branding side (left)
    - Gradient background (primary blue to darker blue)
    - Centered content
    - Large logo (white, 80px height)
    - Tagline: "Plan Your Perfect Trip with AI" (24px, white)
    - Travel-themed illustration
    - Animated floating elements
    - _Requirements: 9.2_
  
  - [ ] 42.3 Create form side (right)
    - White background
    - Max-width: 400px, centered
    - Padding: 48px
    - _Requirements: 9.2_
  
  - [ ] 42.4 Create Google Sign-In button
    - Height: 48px, full width
    - Google logo (left side, 20x20px)
    - Text: "Sign in with Google"
    - Border: 1px solid gray
    - Hover: Light gray background
    - Preserve existing Firebase integration
    - _Requirements: 9.1, 9.3_
  
  - [ ] 42.5 Add alternative auth options (if enabled)
    - Email input (48px height)
    - Password input (48px height, with visibility toggle)
    - "Forgot password" link
    - "Don't have an account? Sign up" link
    - _Requirements: 9.3_
  
  - [ ] 42.6 Implement authentication flow
    - Call Firebase authentication
    - Store JWT token
    - Update AuthContext state
    - Redirect to dashboard or intended page
    - Handle errors with toast notifications
    - _Requirements: 9.1, 9.4_
  
  - [ ] 42.7 Preserve existing auth infrastructure
    - Keep Firebase Authentication
    - Keep JWT token management
    - Keep AuthContext state
    - Keep ProtectedRoute component
    - _Requirements: 9.1_

- [ ]* 43. Authentication Testing
  - [ ]* 43.1 Test Google Sign-In flow
  - [ ]* 43.2 Test protected routes
  - [ ]* 43.3 Test token refresh
  - [ ]* 43.4 Test error handling
  - _Requirements: 9.1-9.4, Req 18_

---

## 📋 Week 11: High-Energy Animations

**Goal**: Implement comprehensive animation system throughout application

**Requirements**: Requirement 10 (High-Energy Animations)


- [ ] 44. Micro-Interactions
  - [ ] 44.1 Implement button animations
    - Hover: translateY(-2px) + scale(1.02) + shadow increase
    - Active: scale(0.98)
    - Focus: Ring with primary color
    - Transition: 300ms cubic-bezier(0.4, 0, 0.2, 1)
    - Apply to all Button components
    - _Requirements: 10.1, Req 10_
  
  - [ ] 44.2 Implement card animations
    - Hover: translateY(-4px) + shadow increase
    - Transition: 300ms
    - Apply to all Card components
    - _Requirements: 10.1, Req 10_
  
  - [ ] 44.3 Implement link animations
    - Hover: Color transition to primary
    - Underline animation (width 0 → 100%)
    - Transition: 200ms
    - _Requirements: 10.1, Req 10_
  
  - [ ] 44.4 Implement icon animations
    - Hover: Subtle rotation (5deg) or scale (1.1)
    - Transition: 200ms
    - Apply to interactive icons
    - _Requirements: 10.1, Req 10_

- [ ] 45. Page Transitions
  - [ ] 45.1 Create page transition wrapper
    - Create `frontend/src/components/layout/PageTransition.tsx`
    - Use Framer Motion AnimatePresence
    - Fade out: 200ms
    - Fade in: 300ms
    - Smooth scroll to top
    - _Requirements: 10.3, Req 10_
  
  - [ ] 45.2 Apply to all routes
    - Wrap each route component
    - Ensure unique keys for AnimatePresence
    - Test transitions between pages
    - _Requirements: 10.3, Req 10_

- [ ] 46. Modal Animations
  - [ ] 46.1 Implement modal animations
    - Backdrop: Fade in (200ms)
    - Content: Scale 0.95 → 1 + fade in (300ms)
    - Elastic easing for bounce effect
    - Apply to all Dialog components
    - _Requirements: 10.4, Req 10_
  
  - [ ] 46.2 Implement drawer animations
    - Slide in from side (300ms)
    - Backdrop fade in (200ms)
    - Apply to mobile navigation
    - _Requirements: 10.4, Req 10_

- [ ] 47. Loading States
  - [ ] 47.1 Enhance skeleton loaders
    - Shimmer effect: 1.5s infinite
    - Gradient: Gray to light gray
    - Smooth transition to content (fade 300ms)
    - Apply to all loading states
    - _Requirements: 10.5, Req 10_
  
  - [ ] 47.2 Create loading spinner component
    - Create `frontend/src/components/ui/spinner.tsx`
    - Rotating animation (1s infinite linear)
    - Multiple sizes (sm, md, lg)
    - Primary color
    - _Requirements: 10.5, Req 10_

- [ ] 48. Scroll Animations
  - [ ] 48.1 Implement parallax effect
    - Hero background moves slower than foreground
    - Use Framer Motion useScroll hook
    - Smooth 60fps performance
    - _Requirements: 10.6, Req 10_
  
  - [ ] 48.2 Implement fade-in on scroll
    - Create `frontend/src/hooks/useScrollAnimation.ts`
    - Detect elements entering viewport
    - Fade in + slide up animation
    - Apply to homepage sections
    - _Requirements: 10.6, Req 10_
  
  - [ ] 48.3 Implement sticky header animation
    - Add shadow on scroll
    - Smooth transition (200ms)
    - Reduce height slightly on scroll (optional)
    - _Requirements: 10.6, Req 10_

- [ ] 49. Destination Card Animations
  - [ ] 49.1 Implement image zoom on hover
    - Image: scale(1.05)
    - Overflow: hidden on card
    - Transition: 300ms
    - _Requirements: 10.7, Req 10_
  
  - [ ] 49.2 Implement overlay fade-in
    - Overlay with details fades in on hover
    - Transition: 300ms
    - _Requirements: 10.7, Req 10_

- [ ] 50. List Stagger Animations
  - [ ] 50.1 Implement stagger for destination cards
    - Use Framer Motion staggerChildren
    - Delay: 50ms between items
    - Apply to TrendingDestinations
    - _Requirements: 10.2, Req 10_
  
  - [ ] 50.2 Implement stagger for search results
    - Apply to flight/hotel cards
    - Delay: 50ms between items
    - _Requirements: 10.2, Req 10_

- [ ]* 51. Animation Testing
  - [ ]* 51.1 Test 60fps performance
  - [ ]* 51.2 Test on different devices
  - [ ]* 51.3 Test animation smoothness
  - [ ]* 51.4 Test no jank or stuttering
  - _Requirements: Req 10, Req 15, Req 18_

---

## 📋 Week 12: Responsive Design

**Goal**: Ensure application works perfectly on all devices

**Requirements**: Requirement 11 (Responsive Design Desktop-First)

- [ ] 52. Desktop Optimization (≥1024px)
  - [ ] 52.1 Verify all layouts on desktop
    - Sidebar: Visible, 280px width
    - Multi-column grids: 3-4 columns
    - Hover effects: Enabled
    - Modals: Max-width 1200px
    - _Requirements: 11.1, Req 11_
  
  - [ ] 52.2 Test all interactions
    - Mouse hover effects
    - Click interactions
    - Keyboard navigation
    - _Requirements: 11.1, Req 11_

- [ ] 53. Tablet Adaptation (768-1023px)
  - [ ] 53.1 Adapt layouts for tablet
    - Sidebar: Collapsible with toggle button
    - Grids: 2-3 columns
    - Touch targets: 44x44px minimum
    - Modals: Max-width 900px
    - _Requirements: 11.2, Req 11_
  
  - [ ] 53.2 Implement collapsible sidebar
    - Add hamburger button (top-left)
    - Sidebar slides in/out
    - Backdrop when open
    - Close on backdrop click
    - _Requirements: 11.2, Req 11_
  
  - [ ] 53.3 Test touch interactions
    - Tap targets large enough
    - Swipe gestures work
    - No hover-dependent functionality
    - _Requirements: 11.2, Req 11_

- [ ] 54. Mobile Optimization (<768px)
  - [ ] 54.1 Implement bottom navigation
    - Create `frontend/src/components/layout/BottomNav.tsx`
    - Fixed position, bottom: 0
    - Height: 64px
    - 5 main navigation items
    - Active item highlighted
    - _Requirements: 11.3, Req 11_
  
  - [ ] 54.2 Adapt all layouts for mobile
    - Single column: All grids
    - Full-screen modals
    - Stack form fields vertically
    - Increase font sizes slightly
    - _Requirements: 11.3, Req 11_
  
  - [ ] 54.3 Implement swipe gestures
    - Swipe to dismiss modals
    - Swipe between tabs
    - Swipe to navigate carousel
    - Use touch events or library
    - _Requirements: 11.3, Req 11_
  
  - [ ] 54.4 Optimize touch targets
    - All interactive elements: 48x48px minimum
    - Increase button padding
    - Increase tap area for icons
    - _Requirements: 11.3, Req 11_
  
  - [ ] 54.5 Implement mobile-specific features
    - Hamburger menu for secondary navigation
    - Pull to refresh (optional)
    - Sticky headers on scroll
    - _Requirements: 11.3, Req 11_

- [ ] 55. Responsive Testing
  - [ ] 55.1 Test on real devices
    - iPhone (iOS Safari)
    - Android (Chrome Mobile)
    - iPad (Safari)
    - Android tablet
    - _Requirements: 11.4, Req 11_
  
  - [ ] 55.2 Test orientation changes
    - Portrait to landscape
    - Landscape to portrait
    - Layout adapts correctly
    - _Requirements: 11.4, Req 11_
  
  - [ ] 55.3 Test responsive breakpoints
    - Test at exact breakpoint values
    - Test between breakpoints
    - Ensure smooth transitions
    - _Requirements: 11.1-11.3, Req 11_

- [ ]* 56. Responsive Testing (Automated)
  - [ ]* 56.1 Write responsive tests
  - [ ]* 56.2 Test with different viewport sizes
  - [ ]* 56.3 Test touch interactions
  - _Requirements: 11.1-11.4, Req 18_

---

## 📋 Week 13: Backend Booking Entity

**Goal**: Implement backend booking entity and API endpoints

**Requirements**: Requirement 12 (Backend Booking Entity)

- [ ] 57. BookingRecord Entity
  - [ ] 57.1 Create BookingRecord entity
    - Create `src/main/java/com/tripplanner/entity/BookingRecord.java`
    - Add @Entity and @Table annotations
    - Define all fields with proper types
    - Add JPA annotations (@Id, @Column, @Enumerated, etc.)
    - Add @CreationTimestamp and @UpdateTimestamp
    - _Requirements: 12.1, Req 12_
  
  - [ ] 57.2 Define BookingStatus enum
    - Create `src/main/java/com/tripplanner/entity/BookingStatus.java`
    - Values: PENDING, CONFIRMED, CANCELLED, REFUNDED
    - _Requirements: 12.1, Req 12_
  
  - [ ] 57.3 Add getters and setters
    - Generate all getters and setters
    - Add builder pattern (optional)
    - Add toString, equals, hashCode
    - _Requirements: 12.1, Req 12_

- [ ] 58. BookingRecord Repository
  - [ ] 58.1 Create repository interface
    - Create `src/main/java/com/tripplanner/repository/BookingRecordRepository.java`
    - Extend JpaRepository<BookingRecord, String>
    - Add custom query methods
    - findByUserId, findByItineraryId, findByNodeId
    - _Requirements: 12.3, Req 12_

- [ ] 59. Booking Controller & API Endpoints
  - [ ] 59.1 Create BookingController
    - Create `src/main/java/com/tripplanner/controller/BookingController.java`
    - Add @RestController and @RequestMapping("/api/bookings")
    - Inject BookingRecordRepository
    - _Requirements: 12.3, Req 12_
  
  - [ ] 59.2 Implement POST /api/bookings
    - Create booking endpoint
    - Validate user authentication
    - Create booking record
    - Update NormalizedNode.bookingRef if applicable
    - Return booking details
    - _Requirements: 12.3, 12.4, Req 12_
  
  - [ ] 59.3 Implement GET /api/bookings/user/{userId}
    - Get user's bookings endpoint
    - Return all bookings for user
    - Sort by bookedAt descending
    - _Requirements: 12.3, Req 12_
  
  - [ ] 59.4 Implement GET /api/bookings/itinerary/{itineraryId}
    - Get itinerary bookings endpoint
    - Return all bookings for specific itinerary
    - _Requirements: 12.3, Req 12_
  
  - [ ] 59.5 Implement PUT /api/bookings/{bookingId}/status
    - Update booking status endpoint
    - Validate status transition
    - Update timestamp
    - Return updated booking
    - _Requirements: 12.3, 12.5, Req 12_
  
  - [ ] 59.6 Implement GET /api/bookings/{bookingId}
    - Get booking details endpoint
    - Return booking by ID
    - _Requirements: 12.3, Req 12_

- [ ] 60. Request/Response DTOs
  - [ ] 60.1 Create CreateBookingRequest DTO
    - Create `src/main/java/com/tripplanner/dto/CreateBookingRequest.java`
    - Define all fields
    - Add validation annotations
    - _Requirements: 12.3, Req 12_
  
  - [ ] 60.2 Create UpdateBookingStatusRequest DTO
    - Create `src/main/java/com/tripplanner/dto/UpdateBookingStatusRequest.java`
    - Define status and notes fields
    - _Requirements: 12.3, Req 12_

- [ ] 61. Integration with NormalizedNode
  - [ ] 61.1 Update NormalizedNode entity
    - Add bookingRef field (String, nullable)
    - Add getter and setter
    - _Requirements: 12.2, Req 12_
  
  - [ ] 61.2 Implement booking creation with node update
    - Create @Transactional method
    - Create booking record
    - Update node.bookingRef if nodeId provided
    - Save both entities
    - _Requirements: 12.2, Req 12_

- [ ] 62. Validation & Error Handling
  - [ ] 62.1 Implement validation rules
    - User must be authenticated
    - Provider name must be valid
    - Confirmation number must be unique
    - Total amount must be positive (if provided)
    - Currency must be valid ISO code (if provided)
    - _Requirements: 12.4, Req 12_
  
  - [ ] 62.2 Implement error handling
    - Handle validation errors
    - Handle database errors
    - Return appropriate HTTP status codes
    - Return error messages
    - _Requirements: 12.4, Req 17_

- [ ]* 63. Backend Testing
  - [ ]* 63.1 Write unit tests for entity
  - [ ]* 63.2 Write unit tests for repository
  - [ ]* 63.3 Write unit tests for controller
  - [ ]* 63.4 Write integration tests for API endpoints
  - [ ]* 63.5 Test validation rules
  - [ ]* 63.6 Test error handling
  - _Requirements: 12.1-12.5, Req 18_

---

## 📝 Week 10-13 Summary

**Completed**:
- ✅ Authentication page redesigned with EaseMyTrip styling
- ✅ Comprehensive animation system (60fps)
- ✅ Responsive design for desktop, tablet, mobile
- ✅ Backend BookingRecord entity and API endpoints
- ✅ Integration with NormalizedNode

**Next**: Week 14-18 (Technical Requirements) → See [tasks-14-18-technical.md](tasks-14-18-technical.md)

