# Requirements 1-4: Foundation & Core Pages

**Design Philosophy**: Apple.com refinement + Emirates.com luxury + EaseMyTrip functionality  
**Design System**: Material 3 + Apple HIG + Atlassian principles

**📍 Source**: This document extracts Requirements 1-4 from the main [requirements.md](requirements.md) file (lines 213-2800+).

**📦 Contents**:
- Requirement 1: Premium Visual Design System (Material 3 + Apple HIG + Atlassian)
- Requirement 2: Homepage with Video Loop & Glass Morphism
- Requirement 3: AI Trip Wizard (Restyled with Premium Components)
- Requirement 4: AI Agent Progress (Restyled with Premium Animations)

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

**The main `requirements.md` file contains the COMPLETE extremely detailed specifications** including:
- Exact pixel measurements for every element
- Complete component hierarchies with props
- Detailed animation specifications with keyframes
- Full TypeScript code examples
- State management implementation details
- API integration specifics with request/response formats
- Form validation rules and error messages
- Mobile responsive breakpoints and adaptations

**This file provides a structured summary for quick reference.**

---

## Requirement 1: Premium Visual Design System

**📍 Main file location**: Lines 213-485

**Design Philosophy**: Apple.com refinement + Emirates.com luxury + EaseMyTrip functionality

**User Story:** As a user, I want the application to have a premium, polished look following Material 3, Apple HIG, and Atlassian design principles, so that I feel confident using a professional travel planning service.

### Summary

Complete premium design system implementation with:
- **Color Palette**: Primary Deep Blue #002B5B, Secondary Gold #F5C542, Warm Gray neutrals
- **Layout System**: 12-column responsive grid, 8px base spacing, consistent vertical rhythm
- **Typography**: Inter or SF Pro Display, 48-60px headings, 16px body, 1.5 line-height
- **Elevation**: 3-layer shadow system (elevation-1, elevation-2, elevation-3)
- **Motion**: 300ms cubic-bezier(0.4,0,0.2,1), 60fps target, GPU-accelerated
- **Components**: Max 12px border-radius, min 48x48px touch targets, glass morphism for inputs

### Key Deliverables

1. **`frontend-redesign/src/styles/tokens.css`** - Premium design tokens
2. **Design System Documentation** - Material 3 + Apple HIG + Atlassian standards
3. **Color Contrast Validation** - WCAG AA compliance (≥4.5:1)
4. **Component Library** - Production-grade UI components

### Critical Specifications

**Colors** (Emirates-inspired):
- Primary: Deep Blue #002B5B
- Secondary: Gold #F5C542
- Neutrals: Warm Gray #F8F8F8 (background), #E0E0E0 (borders), #333333 (text)
- Semantic: Success #10B981, Warning #F59E0B, Error #EF4444

**Elevation** (3 layers):
- elevation-1: No shadow (background)
- elevation-2: `0 4px 12px rgba(0,43,91,0.08)` (sections)
- elevation-3: `0 8px 24px rgba(0,43,91,0.15)` (cards, modals)

**Typography** (Apple HIG):
- Font: Inter or SF Pro Display with system fallbacks
- Headings: 48-60px (H1), 36-40px (H2), 24-30px (H3)
- Body: 16px, line-height 1.5, weight 400
- Weights: 400 (regular), 500 (medium), 600 (semibold), 700 (bold)

**Motion** (Material 3):
- Easing: cubic-bezier(0.4, 0, 0.2, 1)
- Duration: 300ms (standard), 200ms (fast), 500ms (slow)
- Target: 60fps, GPU-accelerated transforms

---

## Requirement 2: Homepage with Multi-Tab Search

**📍 Main file location**: Lines 486-531 (summary), 1198-1882 (detailed)

**User Story:** As a user, I want a homepage with a prominent search widget for flights, hotels, and other travel services, so that I can quickly start planning my trip.

### Summary

Complete homepage with:
- **Hero Section**: Gradient background, animated elements, heading, subheading
- **Search Widget**: 5-tab interface (Flights, Hotels, Holidays, Trains, Bus)
- **Trending Destinations**: 4-column grid with hover effects
- **Popular Routes**: Horizontal scrolling carousel
- **Travel Blogs**: 3-column grid with featured articles
- **AI Planner CTA**: Prominent button to start AI trip planning

### Key Components

1. **`components/homepage/HeroSection.tsx`**
   - 600px min-height
   - Gradient background with animated circles
   - Centered content with max-width 1280px

2. **`components/homepage/SearchWidget.tsx`**
   - Radix UI Tabs component
   - 5 tabs with vertical-specific colors
   - Form validation for each search type

3. **`components/homepage/TrendingDestinations.tsx`**
   - Grid: 4 cols (desktop), 3 (tablet), 2 (mobile)
   - Card hover: lift 4px, shadow increase
   - Image: 200px height, 4:3 aspect ratio

4. **`components/homepage/PopularRoutes.tsx`**
   - Horizontal scroll container
   - Card width: 280px fixed
   - Airline logos: 32x32px

5. **`components/homepage/TravelBlogs.tsx`**
   - Grid: 3 cols (desktop), 2 (tablet), 1 (mobile)
   - Image: 16:9 aspect ratio
   - Read time and publish date metadata

### Critical Specifications

**Hero Section**:
- Background: `linear-gradient(135deg, hsl(207, 100%, 40%), hsl(220, 100%, 45%))`
- Animated circles: 2 pulsing elements with blur
- Heading: 48px (desktop), 36px (mobile), extrabold
- CTA button: Orange, 64px height, with icons

**Search Widget**:
- Max-width: 1200px
- Padding: 32px
- Shadow: xl
- Tab height: 48px
- Form inputs: 56px height

**Destination Cards**:
- Border-radius: 12px
- Image height: 200px
- Padding: 16px
- Gap: 24px
- Hover transform: `translateY(-4px)`

---

## Requirement 3: AI Trip Wizard (Restyled)

**📍 Main file location**: Lines 532-560 (summary), 1883-2503 (detailed)

**User Story:** As a user, I want the AI trip creation wizard to match the EaseMyTrip design, so that I have a consistent experience throughout the application.

### Summary

4-step wizard with premium styling:
- **Step 1**: Destination selection with autocomplete
- **Step 2**: Dates & travelers with counters
- **Step 3**: Preferences (budget, pace, interests)
- **Step 4**: Accommodation & transportation

**Preserves**: All existing SimplifiedTripWizard functionality, validation, API integration

### Key Components

1. **`components/ai-planner/StyledTripWizard.tsx`**
   - Wraps existing wizard with EaseMyTrip styling
   - 800px max-width card
   - Progress indicator with 4 steps
   - POST /api/itineraries on submit

### Critical Specifications

**Wizard Card**:
- Max-width: 800px
- Border-radius: 16px
- Shadow: xl
- Header: Gradient background with icon

**Progress Indicator**:
- 4 steps with connecting lines
- Active: Primary blue, 3px border
- Completed: Green with checkmark
- Inactive: Gray border

**Form Inputs**:
- Height: 48px
- Border-radius: 8px
- Focus: Primary blue border + shadow
- Error: Red border + inline message

**Navigation Buttons**:
- Back: White background, gray text
- Next/Submit: Primary blue, white text
- Height: 48px
- Full width on mobile

---

## Requirement 4: AI Agent Progress (Restyled)

**📍 Main file location**: Lines 561-594 (summary), 2504-2800+ (detailed)

**User Story:** As a user, I want to see AI itinerary generation progress with premium animations, so that I feel engaged while waiting.

### Summary

Real-time progress display with:
- **SSE Connection**: GET /agents/events/{itineraryId}
- **Progress Bar**: Gradient fill with shimmer effect
- **Motivational Messages**: 10 rotating messages
- **Success State**: Confetti animation + navigation
- **Error State**: Retry and support options

**Preserves**: All existing SimplifiedAgentProgress functionality, SSE management

### Key Components

1. **`components/ai-planner/EnhancedAgentProgress.tsx`**
   - Wraps existing progress component
   - 600px max-width card
   - Gradient background page
   - Animated floating particles

### Critical Specifications

**Progress Card**:
- Max-width: 600px
- Padding: 48px 40px
- Border-radius: 20px
- Shadow: xl
- Animation: Scale-in on mount

**Icon Container**:
- Size: 80x80px
- Gradient background
- Pulse animation (2s infinite)
- Sparkles icon: 40x40px, rotating

**Progress Bar**:
- Height: 12px
- Gradient: Blue to orange
- Shimmer effect: 1.5s infinite
- Smooth width transition: 500ms

**Motivational Messages**:
- 10 different messages
- Rotate every 3-4 seconds
- Fade transition: 300ms
- Icon + text layout

**Success State**:
- Confetti burst: 2 seconds
- Checkmark: Green, scale-in with bounce
- Navigate after 1 second delay
- Route: `/trip/${itineraryId}`

---

## Implementation Checklist

### Phase 1: Design System (Week 1)
- [ ] Create `src/index.css` with all CSS custom properties
- [ ] Configure Tailwind to use custom properties
- [ ] Document design tokens
- [ ] Validate color contrast (WCAG AA)
- [ ] Create component style guide

### Phase 2: Homepage (Week 2-3)
- [ ] Implement hero section with animations
- [ ] Build search widget with 5 tabs
- [ ] Create destination grid with hover effects
- [ ] Implement routes carousel
- [ ] Add blogs section
- [ ] Test responsive breakpoints

### Phase 3: AI Wizard (Week 4)
- [ ] Restyle wizard container
- [ ] Update form inputs styling
- [ ] Add progress indicator
- [ ] Maintain validation logic
- [ ] Test all 4 steps
- [ ] Verify API integration

### Phase 4: Agent Progress (Week 5)
- [ ] Restyle progress container
- [ ] Add premium animations
- [ ] Implement message rotation
- [ ] Add confetti on success
- [ ] Test SSE connection
- [ ] Verify error handling

---

## Testing Strategy

### Visual Testing
- [ ] Screenshot comparison with EaseMyTrip.com (≥95% match)
- [ ] Color contrast validation (all pass WCAG AA)
- [ ] Typography rendering (Chrome, Firefox, Safari, Edge)
- [ ] Animation smoothness (60fps on all devices)

### Functional Testing
- [ ] All form validations work
- [ ] API calls successful
- [ ] SSE connections stable
- [ ] Navigation correct
- [ ] Error states handled

### Responsive Testing
- [ ] Desktop ≥1024px
- [ ] Tablet 768-1023px
- [ ] Mobile <768px
- [ ] Touch interactions
- [ ] Orientation changes

---

**📖 For complete implementation details, refer to [requirements.md](requirements.md) lines 213-2800+**

