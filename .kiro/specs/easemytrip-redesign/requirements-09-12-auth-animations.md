# Requirements 9-12: Auth, Animations & Backend

**Design Philosophy**: Apple.com refinement + Emirates.com luxury + EaseMyTrip functionality  
**Design System**: Material 3 + Apple HIG + Atlassian principles

**📍 Source**: This document extracts Requirements 9-12 from the main [requirements.md](requirements.md) file.

**📦 Contents**:
- Requirement 9: Authentication (Restyled with Premium Split Layout)
- Requirement 10: Premium Animations (Material 3 Motion System)
- Requirement 11: Responsive Design (Desktop-First, 12-column grid)
- Requirement 12: Backend Booking Entity

## 🎨 Premium Design Standards (Apply to All Requirements)

- **Layout**: 12-column grid, 8px spacing increments
- **Colors**: Primary #002B5B, Secondary #F5C542, Neutrals #F8F8F8/#E0E0E0
- **Typography**: Inter/SF Pro Display, 16px body, 1.5 line-height
- **Elevation**: 3 layers (elevation-1, elevation-2, elevation-3)
- **Motion**: 300ms cubic-bezier(0.4,0,0.2,1), 60fps target
- **Radius**: Max 12px (no over-rounding)
- **Touch Targets**: Min 48x48px

---

## Requirement 9: Authentication (Restyled)

**📍 Main file location**: Lines 787-816

**User Story:** As a user, I want a modern login/signup page matching EaseMyTrip design, so that I have a consistent brand experience from the start.

### Summary

Restyled authentication with:
- **Firebase Integration**: Preserves existing Google Sign-In
- **Split Layout**: Branding side + form side
- **EaseMyTrip Styling**: Matches design system
- **Protected Routes**: Maintains existing guards

### Key Components

1. **`components/auth/LoginPage.tsx`**
   - Split layout: 50/50 on desktop
   - Left: Gradient background with branding
   - Right: White background with form

2. **Preserves Existing**:
   - Firebase Authentication
   - JWT token management
   - AuthContext state
   - ProtectedRoute component

### Critical Specifications

**Layout**:
- Desktop: 50/50 split
- Mobile: Full width, stacked
- Min-height: 100vh

**Left Side (Branding)**:
- Background: Gradient (primary blue to darker blue)
- Content: Centered
- Logo: Large, white
- Tagline: "Plan Your Perfect Trip with AI"
- Illustration: Travel-themed

**Right Side (Form)**:
- Background: White
- Max-width: 400px, centered
- Padding: 48px
- Form elements: EaseMyTrip styled

**Google Sign-In Button**:
- Height: 48px
- Full width
- Google logo: Left side
- Text: "Sign in with Google"
- Border: 1px solid gray
- Hover: Light gray background

**Alternative Auth** (if enabled):
- Email input: 48px height
- Password input: 48px height
- "Forgot password" link
- "Sign up" link

---

## Requirement 10: High-Energy Animations

**📍 Main file location**: Lines 817-858

**User Story:** As a user, I want smooth, premium animations throughout the application, so that I feel I'm using a high-quality, modern service.

### Summary

Comprehensive animation system with:
- **Micro-interactions**: Hover, click, focus animations
- **Page Transitions**: Smooth route changes
- **Loading States**: Skeleton loaders with shimmer
- **Scroll Animations**: Parallax and fade-in effects
- **Success Animations**: Confetti, checkmarks, celebrations

### Animation Library

**Framer Motion** for complex animations:
- Page transitions
- Modal animations
- List stagger effects
- Spring physics

**Tailwind CSS** for simple animations:
- Hover effects
- Focus states
- Simple transitions

**CSS Transforms** for performance:
- GPU-accelerated
- Transform and opacity only
- 60fps target

### Critical Specifications

**Micro-interactions**:
```css
/* Button hover */
.button:hover {
  transform: translateY(-2px) scale(1.02);
  box-shadow: var(--shadow-premium-lg);
  transition: all 300ms cubic-bezier(0.4, 0, 0.2, 1);
}

/* Card hover */
.card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-premium-md);
}
```

**Page Transitions**:
- Fade out: 200ms
- Fade in: 300ms
- Smooth scroll to top

**Modal Animations**:
- Backdrop: Fade in 200ms
- Content: Scale 0.95 → 1, fade in 300ms
- Elastic easing for bounce effect

**Skeleton Loaders**:
- Shimmer effect: 1.5s infinite
- Gradient: Gray to light gray
- Smooth transition to content

**Scroll Animations**:
- Parallax: Background moves slower than foreground
- Fade-in: Elements appear as they enter viewport
- Stagger: List items animate with 50ms delay

**Success Animations**:
- Confetti: 2 seconds, 100-150 particles
- Checkmark: Scale-in with bounce
- Celebration: Pulse and glow effects

### Animation Tokens

```css
:root {
  /* Durations */
  --duration-instant: 100ms;
  --duration-fast: 200ms;
  --duration-normal: 300ms;
  --duration-slow: 500ms;
  --duration-slower: 700ms;
  
  /* Easing */
  --ease-premium: cubic-bezier(0.4, 0, 0.2, 1);
  --ease-bounce: cubic-bezier(0.68, -0.55, 0.265, 1.55);
  --ease-elastic: cubic-bezier(0.175, 0.885, 0.32, 1.275);
  
  /* Scales */
  --scale-hover: 1.02;
  --scale-active: 0.98;
  --scale-focus: 1.05;
}
```

---

## Requirement 11: Responsive Design (Desktop-First)

**📍 Main file location**: Lines 859-889

**User Story:** As a user, I want the application to work on all devices, so that I can plan trips on desktop, tablet, or mobile.

### Summary

Responsive strategy:
- **Desktop-First**: Primary development target ≥1024px
- **Tablet**: Adapt layout 768-1023px
- **Mobile**: Optimize for <768px
- **Touch-Optimized**: Larger targets, swipe gestures

### Breakpoints

```css
/* Tailwind breakpoints */
sm: 640px   /* Small devices */
md: 768px   /* Tablets */
lg: 1024px  /* Desktops */
xl: 1280px  /* Large desktops */
2xl: 1536px /* Extra large */
```

### Critical Specifications

**Desktop (≥1024px)**:
- Sidebar: Visible, 280px width
- Multi-column grids: 3-4 columns
- Hover effects: Enabled
- Modals: Max-width 1200px

**Tablet (768-1023px)**:
- Sidebar: Collapsible
- Grids: 2-3 columns
- Touch targets: 44x44px minimum
- Modals: Max-width 900px

**Mobile (<768px)**:
- Bottom navigation: Replaces sidebar
- Single column: All grids
- Full-screen modals
- Swipe gestures: Enabled
- Touch targets: 48x48px minimum

**Responsive Patterns**:
- Hide/show elements: `hidden md:block`
- Column changes: `grid-cols-1 md:grid-cols-2 lg:grid-cols-4`
- Padding adjustments: `p-4 md:p-6 lg:p-8`
- Font sizes: `text-base md:text-lg lg:text-xl`

**Mobile-Specific**:
- Bottom nav: Fixed, 64px height
- Hamburger menu: For secondary navigation
- Swipe to dismiss: Modals and sheets
- Pull to refresh: Lists (optional)
- Sticky headers: On scroll

---

## Requirement 12: Backend Booking Entity

**📍 Main file location**: Lines 890-933

**User Story:** As a developer, I want a dedicated booking entity in the backend, so that booking data is properly structured and queryable.

### Summary

New backend entity for bookings:
- **BookingRecord Entity**: Java entity with JPA annotations
- **Repository**: CRUD operations
- **Controller**: REST API endpoints
- **Integration**: Links to NormalizedNode

### Entity Specification

```java
@Entity
@Table(name = "booking_records")
public class BookingRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = true)
    private String itineraryId; // Null for standalone bookings
    
    @Column(nullable = true)
    private String nodeId; // Null for standalone bookings
    
    @Column(nullable = false)
    private String providerName; // "Booking.com", "Expedia", etc.
    
    @Column(nullable = false)
    private String providerBookingId; // Provider's internal ID
    
    @Column(nullable = false)
    private String confirmationNumber; // EMT{9-char}
    
    @Column(columnDefinition = "TEXT")
    private String bookingDetailsJson; // Flexible JSON storage
    
    @Enumerated(EnumType.STRING)
    private BookingStatus status; // PENDING, CONFIRMED, CANCELLED, REFUNDED
    
    @CreationTimestamp
    private LocalDateTime bookedAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    private BigDecimal totalAmount;
    private String currency;
    
    // Getters and setters...
}

enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    REFUNDED
}
```

### API Endpoints

```java
@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    
    // Create booking
    @PostMapping
    public ResponseEntity<BookingRecord> createBooking(
        @RequestBody CreateBookingRequest request
    ) {
        // Validate user authentication
        // Create booking record
        // Update NormalizedNode.bookingRef if applicable
        // Return booking details
    }
    
    // Get user's bookings
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingRecord>> getUserBookings(
        @PathVariable String userId
    ) {
        // Return all bookings for user
    }
    
    // Get itinerary bookings
    @GetMapping("/itinerary/{itineraryId}")
    public ResponseEntity<List<BookingRecord>> getItineraryBookings(
        @PathVariable String itineraryId
    ) {
        // Return all bookings for specific itinerary
    }
    
    // Update booking status
    @PutMapping("/{bookingId}/status")
    public ResponseEntity<BookingRecord> updateBookingStatus(
        @PathVariable String bookingId,
        @RequestBody UpdateBookingStatusRequest request
    ) {
        // Update status (confirm, cancel, refund)
        // Update timestamp
        // Return updated booking
    }
    
    // Get booking details
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingRecord> getBooking(
        @PathVariable String bookingId
    ) {
        // Return booking details
    }
}
```

### Request/Response DTOs

```java
public class CreateBookingRequest {
    private String userId;
    private String itineraryId; // Optional
    private String nodeId; // Optional
    private String providerName;
    private String providerBookingId;
    private String confirmationNumber;
    private String bookingDetailsJson;
    private BigDecimal totalAmount;
    private String currency;
}

public class UpdateBookingStatusRequest {
    private BookingStatus status;
    private String notes; // Optional
}
```

### Integration with NormalizedNode

```java
// Update NormalizedNode when booking is created
@Transactional
public BookingRecord createBooking(CreateBookingRequest request) {
    // Create booking record
    BookingRecord booking = new BookingRecord();
    booking.setUserId(request.getUserId());
    booking.setProviderName(request.getProviderName());
    // ... set other fields
    booking = bookingRepository.save(booking);
    
    // Update node if applicable
    if (request.getNodeId() != null) {
        NormalizedNode node = nodeRepository.findById(request.getNodeId());
        node.setBookingRef(booking.getId());
        nodeRepository.save(node);
    }
    
    return booking;
}
```

### Validation Rules

- User must be authenticated
- Provider name must be valid (from configuration)
- Confirmation number must be unique
- Total amount must be positive (if provided)
- Currency must be valid ISO code (if provided)

---

## Implementation Checklist

### Phase 1: Authentication (Week 10)
- [ ] Restyle login page with split layout
- [ ] Update Google Sign-In button styling
- [ ] Test Firebase integration
- [ ] Verify protected routes
- [ ] Add mobile responsive layout

### Phase 2: Animations (Week 11)
- [ ] Install Framer Motion
- [ ] Create animation tokens in CSS
- [ ] Implement micro-interactions
- [ ] Add page transitions
- [ ] Create skeleton loaders
- [ ] Add scroll animations
- [ ] Test 60fps performance

### Phase 3: Responsive Design (Week 12)
- [ ] Test all breakpoints
- [ ] Implement mobile navigation
- [ ] Add touch gestures
- [ ] Optimize for tablets
- [ ] Test on real devices
- [ ] Verify accessibility

### Phase 4: Backend Entity (Week 13)
- [ ] Create BookingRecord entity
- [ ] Create repository
- [ ] Implement controller
- [ ] Add validation
- [ ] Write unit tests
- [ ] Test API endpoints
- [ ] Update NormalizedNode integration

---

## Testing Strategy

### Animation Testing
- [ ] 60fps on all animations
- [ ] No jank or stuttering
- [ ] Smooth page transitions
- [ ] Skeleton loaders work
- [ ] Confetti renders correctly

### Responsive Testing
- [ ] Desktop ≥1024px
- [ ] Tablet 768-1023px
- [ ] Mobile <768px
- [ ] Portrait and landscape
- [ ] Touch interactions
- [ ] Swipe gestures

### Backend Testing
- [ ] Entity saves correctly
- [ ] API endpoints work
- [ ] Validation rules enforced
- [ ] Node integration works
- [ ] Status updates persist
- [ ] Queries return correct data

---

**📖 For complete implementation details, refer to [requirements.md](requirements.md)**
