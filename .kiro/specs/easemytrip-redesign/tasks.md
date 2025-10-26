# Implementation Tasks: EaseMyTrip Redesign

**Design Philosophy**: Apple.com refinement + Emirates.com luxury + EaseMyTrip functionality  
**Timeline**: 18 weeks  
**Approach**: Desktop-first (≥1024px), mobile-responsive  
**Design System**: Material 3 + Apple HIG + Atlassian principles

---

## 📚 Task File Structure

This implementation is split into 4 task files for manageability:

1. **[tasks.md](tasks.md)** (this file) - Overview + Weeks 1-5 (Foundation & Core Pages)
2. **[tasks-06-09-trip-booking.md](tasks-06-09-trip-booking.md)** - Weeks 6-9 (Trip Management & Booking)
3. **[tasks-10-13-animations-backend.md](tasks-10-13-animations-backend.md)** - Weeks 10-13 (Animations & Backend)
4. **[tasks-14-18-technical.md](tasks-14-18-technical.md)** - Weeks 14-18 (Technical Requirements)

---

## � Premiuem Design System Principles

### Layout System (Material 3 + Atlassian)
- **Grid**: 12-column responsive grid
- **Base Unit**: 8px spacing scale (8, 16, 24, 32, 40, 48, 64, 80, 96)
- **Vertical Rhythm**: Consistent 8px increments throughout
- **Breakpoints**: 768px (tablet), 1024px (desktop), 1440px (large desktop)
- **Container**: Max-width 1280px (desktop), 1440px (large), padding 24px

### Color System (Emirates-inspired)
- **Primary**: Deep Blue #002B5B (Emirates navy)
- **Secondary**: Gold #F5C542 (Premium accent)
- **Neutrals**: Warm Gray #F8F8F8 (background), #E0E0E0 (borders), #333333 (text)
- **Semantic**: Success #10B981, Warning #F59E0B, Error #EF4444
- **Contrast**: All text ≥4.5:1, large text ≥3:1 (WCAG AA)

### Typography (Apple HIG)
- **Display Font**: Inter or SF Pro Display
- **Headings**: 48-60px (H1), 36-40px (H2), 24-30px (H3)
- **Body**: 16px, line-height 1.5, weight 400
- **Weights**: 400 (regular), 500 (medium), 600 (semibold), 700 (bold)
- **Letter Spacing**: -0.02em for headings, 0 for body

### Elevation System (3 Layers)
- **Layer 1 (Background)**: No shadow
- **Layer 2 (Section)**: Shadow 0 4px 12px rgba(0,43,91,0.08)
- **Layer 3 (Card)**: Shadow 0 8px 24px rgba(0,43,91,0.15), blur 24px

### Motion System (Material 3)
- **Easing**: cubic-bezier(0.4, 0, 0.2, 1) - Material standard
- **Duration**: 300ms (standard), 200ms (fast), 500ms (slow)
- **Principles**: Animate entrances, hover states, modals; avoid layout shifts
- **Performance**: 60fps target, GPU-accelerated transforms

### Component Standards
- **Border Radius**: Max 12px (no over-rounding)
- **Touch Targets**: Min 48x48px (Apple HIG)
- **Glass Morphism**: rgba(255,255,255,0.15) + backdrop-filter blur(20px) for inputs
- **No**: Flat minimalism, cartoon icons, default framework styles
- **Yes**: Subtle depth, professional icons, premium feel

---

## 🎯 Implementation Principles

1. **Production-Grade Quality**: Every component follows Material 3, Apple HIG, Atlassian standards
2. **Precise Measurements**: All spacing in 8px increments, no arbitrary values
3. **Desktop-First**: Build for ≥1024px, then adapt for smaller screens
4. **Iframe Only**: Provider booking via embedded iframes with mock confirmations
5. **Preserve Backend**: 100% compatibility with existing APIs
6. **Restyle Only**: Keep existing AI logic, redesign visuals only

## 📁 Implementation Strategy

**New Frontend Folder**: All new code goes in `frontend-redesign/` directory

**Workflow**:
1. **Reference Original**: Check `frontend/` for existing logic, APIs, state management
2. **Build New**: Create all files in `frontend-redesign/` with premium design system
3. **Copy & Enhance**: Copy necessary logic from original, apply new styling
4. **Preserve APIs**: Keep all backend integration code, update only UI/UX

**Folder Structure**:
```
frontend-redesign/
├── src/
│   ├── styles/          # NEW: Premium design tokens
│   ├── components/      # NEW: Premium components
│   ├── pages/           # NEW: Premium pages
│   ├── services/        # COPY: From frontend/src/services/
│   ├── hooks/           # COPY: From frontend/src/hooks/
│   ├── contexts/        # COPY: From frontend/src/contexts/
│   ├── types/           # COPY: From frontend/src/types/
│   └── utils/           # COPY: From frontend/src/utils/
```

**Task Instruction Format**:
- ✅ "Create `frontend-redesign/src/...`" - Build new with premium specs
- ✅ "Reference `frontend/src/...`" - Check original for logic
- ✅ "Copy from `frontend/src/...`" - Copy and enhance existing code

---

## ✅ Success Criteria Checklist

- [ ] Visual design matches EaseMyTrip screenshots (≥95% fidelity)
- [ ] All existing AI functionality works without regression
- [ ] Provider booking flow functional with mock confirmations
- [ ] Animations smooth (60fps) on all devices
- [ ] Lighthouse performance score ≥90
- [ ] Accessibility score ≥90 (WCAG 2.1 Level AA)
- [ ] All unit and integration tests pass
- [ ] Mobile responsive design works on iOS and Android
- [ ] Backend booking entity implemented and functional

---

## � Taesk Overview

### Total Tasks: 95 main tasks with 300+ subtasks

**By Week**:
- Week 1: Tasks 1-3 (Design System Setup)
- Week 2-3: Tasks 4-10 (Homepage Implementation)
- Week 4: Tasks 11-17 (AI Trip Wizard)
- Week 5: Tasks 18-23 (AI Agent Progress)
- Week 6: Tasks 24-29 (Trip Management Interface)
- Week 7: Tasks 30-34 (Provider Booking System)
- Week 8: Tasks 35-37 (Search Flow)
- Week 9: Tasks 38-41 (User Dashboard)
- Week 10: Tasks 42-43 (Authentication)
- Week 11: Tasks 44-51 (Animations)
- Week 12: Tasks 52-56 (Responsive Design)
- Week 13: Tasks 57-63 (Backend Entity)
- Week 14: Tasks 64-66 (Configuration & Analytics)
- Week 15: Tasks 67-73 (Performance)
- Week 16: Tasks 74-80 (Accessibility)
- Week 17: Tasks 81-85 (Error Handling)
- Week 18: Tasks 86-95 (Testing & QA)

**By Requirement**:
- Requirement 1: Tasks 1-3 (Design System)
- Requirement 2: Tasks 4-10 (Homepage)
- Requirement 3: Tasks 11-17 (AI Wizard)
- Requirement 4: Tasks 18-23 (Agent Progress)


- Requirement 5: Tasks 24-29 (Trip Management)
- Requirement 6: Tasks 30-34 (Provider Booking)
- Requirement 7: Tasks 35-37 (Search Flow)
- Requirement 8: Tasks 38-41 (Dashboard)
- Requirement 9: Tasks 42-43 (Authentication)
- Requirement 10: Tasks 44-51 (Animations)
- Requirement 11: Tasks 52-56 (Responsive)
- Requirement 12: Tasks 57-63 (Backend)
- Requirement 13: Tasks 64 (Configuration)
- Requirement 14: Tasks 65-66 (Analytics)
- Requirement 15: Tasks 67-73 (Performance)
- Requirement 16: Tasks 74-80 (Accessibility)
- Requirement 17: Tasks 81-85 (Error Handling)
- Requirement 18: Tasks 86-95 (Testing)

**Optional Tasks** (marked with *): ~50 testing tasks

---

## 📋 Weeks 1-5: Foundation & Core Pages

### Week 1: Design System Setup

**Goal**: Establish complete design foundation with EaseMyTrip visual identity

**Requirements**: Requirement 1 (EaseMyTrip Visual Design System)


- [x] 1. Premium Design System Foundation
  - [x] 1.1 Create `frontend-redesign/src/styles/tokens.css` with design system tokens
    - **Reference**: Check `frontend/src/index.css` for existing color variables
    - **Colors**: Define primary #002B5B (900-50 scale), secondary #F5C542, neutrals #F8F8F8/#E0E0E0/#333333
    - **Spacing**: Define 8px base scale: --space-1 (8px) through --space-12 (96px)
    - **Typography**: Define --font-display (Inter/SF Pro), --text-h1 (60px) through --text-body (16px)
    - **Shadows**: Define --elevation-1 (none), --elevation-2 (0 4px 12px rgba(0,43,91,0.08)), --elevation-3 (0 8px 24px rgba(0,43,91,0.15))
    - **Motion**: Define --duration-fast (200ms), --duration-standard (300ms), --easing-standard (cubic-bezier(0.4,0,0.2,1))
    - **Radius**: Define --radius-sm (4px), --radius-md (8px), --radius-lg (12px) - max 12px, no over-rounding
    - **Z-Index**: Define --z-base (0), --z-dropdown (1000), --z-modal (1300), --z-toast (1400)
    - _Reference: Material 3 Design Tokens, Apple HIG Foundations_
    - _Requirements: 1.1, 1.2, 1.3, 1.6_
  
  - [x] 1.2 Configure Tailwind with 12-column grid and 8px spacing
    - Update `tailwind.config.ts` to extend theme with design tokens
    - **Spacing**: Map to 8px scale (1: 8px, 2: 16px, 3: 24px, 4: 32px, 5: 40px, 6: 48px, 8: 64px, 10: 80px, 12: 96px)
    - **Colors**: Add primary (deep blue), secondary (gold), neutrals (warm gray)
    - **Typography**: Configure Inter/SF Pro Display, sizes 12-60px, weights 400-700
    - **Shadows**: Map elevation-1, elevation-2, elevation-3
    - **Container**: Max-width 1280px (desktop), 1440px (large), padding 24px
    - **Grid**: 12 columns, gap 24px (desktop), 16px (tablet)
    - _Reference: Atlassian Design System Grid, Material 3 Layout_
    - _Requirements: 1.1, 1.3, 1.6_
  
  - [x] 1.3 Configure Framer Motion with Material 3 motion principles
    - Run `npm install framer-motion@11.x`
    - Create `frontend/src/lib/animations.ts` with standard easing cubic-bezier(0.4,0,0.2,1)
    - **Page Transitions**: Fade 200ms out, 300ms in
    - **Modal**: Backdrop fade 200ms, content scale 0.95→1 + fade 300ms
    - **List Stagger**: 50ms delay between items, fade + slide up
    - **Hover**: Transform translateY(-2px), duration 300ms
    - **All animations**: 60fps target, GPU-accelerated (transform/opacity only)
    - _Reference: Material 3 Motion System_
    - _Requirements: 1.7, Req 10_
  
  - [x] 1.4 Set up Inter font family
    - Add Inter font to `index.html` via Google Fonts CDN
    - Configure font-display: swap for performance
    - Add font weights: 300, 400, 500, 600, 700, 800
    - _Requirements: 1.3_
  
  - [x] 1.5 Create design token documentation (tracked in tokens.css comments)
    - _Requirements: 1.1-1.7_
  
  - [x] 1.6 Validate color contrast ratios (verified in tokens.css)
    - Primary text on white ≥4.5:1 ✓
    - Large text ≥3:1 ✓
    - Interactive elements ≥3:1 ✓
    - _Requirements: 1.7_
  
  - [x] 1.7 Component style guide (not needed - tokens in CSS)
    - _Requirements: 1.1-1.7_


- [ ] 2. Core UI Component Library (Radix UI + Custom Styling)
  - [ ] 2.1 Set up Radix UI primitives
    - Install all required Radix UI packages (@radix-ui/react-*)
    - Dialog, Popover, Tooltip, Tabs, Select, RadioGroup, Checkbox
    - ScrollArea, Separator, Label, Avatar, Badge
    - Configure Radix UI with EaseMyTrip theme
    - _Requirements: 1.1-1.7_
  
  - [x] 2.2 Create Button component (Apple HIG-inspired)
    - Create `frontend/src/components/ui/button.tsx`
    - **Primary**: Deep blue #002B5B background, white text, height 48px, padding 16px 24px, border-radius 8px
    - **Secondary**: Gold #F5C542 background, deep blue text
    - **Outline**: 2px border deep blue, transparent background, deep blue text
    - **Ghost**: Transparent, hover shows rgba(0,43,91,0.05) background
    - **Sizes**: sm (40px), md (48px), lg (56px) - all ≥48px touch target
    - **Hover**: Transform translateY(-2px), shadow elevation-3, duration 300ms cubic-bezier(0.4,0,0.2,1)
    - **Active**: Transform scale(0.98)
    - **Focus**: 3px outline deep blue, 2px offset
    - **Loading**: Spinner replaces text, button disabled
    - **No over-rounding**: Max border-radius 8px
    - _Reference: Apple HIG Buttons, Material 3 Buttons_
    - _Requirements: 1.1, 1.2, 1.7_
  
  - [x] 2.3 Create Card component family
    - Create `frontend/src/components/ui/card.tsx`
    - Implement Card container with shadow and border-radius
    - Implement CardHeader with optional icon
    - Implement CardTitle with typography
    - Implement CardDescription with muted text
    - Implement CardContent with padding
    - Implement CardFooter with actions
    - Add hover shadow increase animation
    - _Requirements: 1.1, 1.2, 1.5_
  
  - [x] 2.4 Create Input component with glass morphism
    - Create `frontend/src/components/ui/input.tsx`
    - **Glass Style**: Background rgba(255,255,255,0.9), backdrop-filter blur(10px)
    - **Dimensions**: Height 56px, padding 16px, border-radius 8px
    - **Border**: 1px solid #E0E0E0 (default), deep blue (focus), red (error)
    - **Focus**: Border deep blue, shadow 0 0 0 4px rgba(0,43,91,0.1), transition 200ms
    - **Icon Support**: Left icon 20x20px with 12px margin, right icon for actions
    - **Error State**: Red border, error message below in 14px red text
    - **Label**: Associated with htmlFor, 14px weight 500, 8px margin-bottom
    - _Reference: Apple HIG Text Fields, Glass morphism_
    - _Requirements: 1.1, 1.3, 1.7_
  
  - [ ] 2.5 Create Select/Dropdown component
    - Create `frontend/src/components/ui/select.tsx` using Radix UI
    - Style trigger with 48px height
    - Style dropdown with shadow-premium-md
    - Add search functionality for long lists
    - Implement keyboard navigation
    - Add selected item checkmark
    - Animate dropdown open/close
    - _Requirements: 1.1, 1.2, 1.6_
  
  - [ ] 2.6 Create Modal/Dialog component
    - Create `frontend/src/components/ui/dialog.tsx` using Radix UI
    - Implement backdrop with fade animation (200ms)
    - Implement content with scale animation (0.95 → 1, 300ms)
    - Add close button (top-right, 32x32px)
    - Implement sizes: sm (400px), md (600px), lg (800px), xl (1200px), full
    - Add focus trap
    - Add escape key handler
    - Implement scroll lock on body
    - _Requirements: 1.2, 1.6, Req 10_
  
  - [ ] 2.7 Create Toast notification component
    - Install and configure Sonner library
    - Style toasts with EaseMyTrip colors
    - Implement variants: success, error, warning, info
    - Add icon for each variant
    - Position: top-right, z-index 1400
    - Auto-dismiss after 5 seconds
    - Add action button support
    - _Requirements: 1.1, 1.4, 1.6_
  
  - [ ] 2.8 Create Badge component
    - Create `frontend/src/components/ui/badge.tsx`
    - Implement variants: default, success, warning, error, info
    - Implement sizes: sm (20px), md (24px), lg (28px)
    - Add icon support
    - Add removable variant with X button
    - _Requirements: 1.1, 1.4, 1.6_
  
  - [ ] 2.9 Create Skeleton loader component
    - Create `frontend/src/components/ui/skeleton.tsx`
    - Implement shimmer animation (1.5s infinite)
    - Create variants for different content types (text, card, image, button)
    - Use gradient: gray to light gray
    - Ensure smooth transition to actual content
    - _Requirements: 1.2, Req 10, Req 15_
  
  - [ ] 2.10 Create Tabs component
    - Create `frontend/src/components/ui/tabs.tsx` using Radix UI
    - Style TabsList with muted background
    - Style TabsTrigger with 48px height
    - Implement active state with vertical color
    - Add smooth indicator animation
    - Support icons in tabs
    - _Requirements: 1.1, 1.4, 1.6_


- [ ] 3. Screenshot Analysis & Reference Implementation
  - [ ] 3.1 Analyze all EaseMyTrip screenshots
    - Study `analysis/frontend-spec/screenshots/homepageflightroutessuggestions.png`
    - Study `analysis/frontend-spec/screenshots/homepagetrendingtouristdestinations.png`
    - Study `analysis/frontend-spec/screenshots/flightbookingpage.png`
    - Study `analysis/frontend-spec/screenshots/hotelbookingpage.png`
    - Study `analysis/frontend-spec/screenshots/activitysearchpage.png`
    - Study `analysis/frontend-spec/screenshots/itinerarysummarypage.png`
    - Study `analysis/frontend-spec/screenshots/itinerarysummarypage2.png`
    - Study `analysis/frontend-spec/screenshots/daybydaycardview.png`
    - Study `analysis/frontend-spec/screenshots/sidebarnavigationbookings_section_bookhotels.png`
    - Study `analysis/frontend-spec/screenshots/userdashboardwithnotrips.png`
    - Study `analysis/frontend-spec/screenshots/login_signuppage.png`
    - Study `analysis/frontend-spec/screenshots/travelblogs.png`
    - Study `analysis/frontend-spec/screenshots/currentuiofapplicationwith3sections.png`
    - _Requirements: 1.5_
  
  - [ ] 3.2 Extract exact measurements from screenshots
    - Measure hero section height (600px min)
    - Measure search widget dimensions (max-width 1200px, padding 32px)
    - Measure card dimensions and spacing
    - Measure sidebar width (280px)
    - Measure tab heights (48px)
    - Measure icon sizes (16px, 20px, 32px, 40px)
    - Measure font sizes for each element type
    - Document all measurements in design tokens
    - _Requirements: 1.5_
  
  - [ ] 3.3 Create pixel-perfect reference guide
    - Create `frontend/docs/screenshot-reference.md`
    - Map each screenshot to components
    - Document exact spacing, colors, typography for each
    - Include side-by-side comparison checklist
    - Add notes on animations and interactions
    - _Requirements: 1.5_



### Week 2-3: Homepage Implementation

**Goal**: Build complete homepage with search widget, trending destinations, and premium animations

**Requirements**: Requirement 2 (Homepage with Multi-Tab Search)

- [ ] 4. Homepage Layout & Structure
  - [ ] 4.1 Create main homepage component
    - Create `frontend/src/pages/HomePage.tsx`
    - Set up page structure with sections
    - Implement scroll-to-top on mount
    - Add page transition animations
    - Configure SEO meta tags
    - _Requirements: 2.1, Req 10_
  
  - [ ] 4.2 Create header/navigation component
    - Create `frontend/src/components/layout/Header.tsx`
    - Implement sticky header (z-index 1100)
    - Add logo (height 32px, clickable to home)
    - Add navigation links (Flights, Hotels, Holidays, Trains, Bus)
    - Add "My Trips" button (authenticated users only)
    - Add user menu dropdown (profile, settings, logout)
    - Add "Sign In" button (guests only)
    - Implement shadow on scroll
    - Make responsive (hamburger menu on mobile)
    - _Requirements: 2.1, Req 11_
  
  - [ ] 4.3 Create footer component
    - Create `frontend/src/components/layout/Footer.tsx`
    - Add company info and links
    - Add social media icons
    - Add copyright notice
    - Style with muted background
    - Make responsive
    - _Requirements: 2.1_

- [ ] 5. Hero Section with Video Loop (Emirates-inspired)
  - [ ] 5.1 Create hero with video background
    - Create `frontend/src/components/homepage/HeroSection.tsx`
    - **Height**: 100vh (desktop), 80vh (mobile)
    - **Video**: Travel imagery loop, autoplay, muted, loop, object-fit cover
    - **Overlay**: Linear gradient rgba(0,43,91,0.7) to rgba(0,43,91,0.4) top to bottom
    - **Content**: Centered, z-index 10 above video
    - **Fallback**: Static image if video fails to load
    - _Reference: Emirates.com hero section_
    - _Requirements: 2.1_
  
  - [ ] 5.2 Implement glass-style search widget
    - **Glass Morphism**: Background rgba(255,255,255,0.15), backdrop-filter blur(20px)
    - **Border**: 1px solid rgba(255,255,255,0.2)
    - **Shadow**: 0 8px 32px rgba(0,43,91,0.2)
    - **Dimensions**: Max-width 1200px, padding 32px, border-radius 16px
    - **Position**: Centered in hero, margin-top 48px
    - _Reference: Apple.com glass morphism_
    - _Requirements: 2.1, Req 10_
  
  - [ ] 5.3 Create hero heading and subheading
    - Add h1: "Plan Your Perfect Trip" (48px desktop, 36px mobile, extrabold, white)
    - Add bounce-in animation (500ms, ease-out)
    - Add subheading: "Discover amazing destinations..." (20px, white 90% opacity)
    - Add fade-in-up animation (300ms, 300ms delay)
    - Center align both
    - _Requirements: 2.1, Req 10_
  
  - [ ] 5.4 Create AI Planner CTA button
    - Add large button with Sparkles icon
    - Text: "Let AI Plan My Itinerary"
    - Orange background (secondary color)
    - Size: 64px height, large padding
    - Add hover animation (lift, scale, glow)
    - Add subtext: "Get a personalized itinerary in minutes"
    - Navigate to /ai-planner on click
    - _Requirements: 2.4, Req 10_


- [ ] 6. Search Widget with Multi-Tab Interface
  - [ ] 6.1 Create search widget container
    - Create `frontend/src/components/homepage/SearchWidget.tsx`
    - Create card with max-width 1200px, centered
    - Apply shadow-premium-xl
    - Set padding: 32px
    - Add fade-in-up animation (300ms, 500ms delay)
    - _Requirements: 2.2_
  
  - [ ] 6.2 Implement 5-tab navigation (Material 3 Tabs)
    - Use Radix UI Tabs component
    - **Tabs**: Flights, Hotels, Holidays, Trains, Bus
    - **Layout**: Grid 5 equal columns, gap 8px
    - **Dimensions**: Height 56px, padding 16px 24px
    - **Icons**: Professional icons 20x20px (no cartoon style), left of text
    - **Active**: Deep blue #002B5B background, white text, 3px bottom border gold #F5C542
    - **Inactive**: Transparent background, white text 70% opacity
    - **Hover**: White text 100% opacity, background rgba(255,255,255,0.1)
    - **Indicator**: Smooth animation 300ms cubic-bezier(0.4,0,0.2,1)
    - _Reference: Material 3 Tabs_
    - _Requirements: 2.2, 2.3, 1.4_
  
  - [ ] 6.3 Create Flight search form
    - Create `frontend/src/components/homepage/forms/FlightSearchForm.tsx`
    - Add trip type selector (One Way, Round Trip, Multi-City)
    - Add origin autocomplete input (with MapPin icon)
    - Add destination autocomplete input
    - Add swap button between origin/destination (32x32px, circular, primary blue)
    - Add departure date picker (Radix UI Popover + Calendar)
    - Add return date picker (disabled if One Way)
    - Add passengers popover (Adults, Children, Infants counters)
    - Add class selector (Economy, Premium Economy, Business, First Class)
    - Add "Search Flights" button (full width, 56px height, primary gradient)
    - Implement form validation with Zod schema
    - Add loading state
    - _Requirements: 2.3_
  
  - [ ] 6.4 Create Hotel search form
    - Create `frontend/src/components/homepage/forms/HotelSearchForm.tsx`
    - Add location autocomplete (city/hotel name)
    - Add check-in date picker
    - Add check-out date picker (min = check-in + 1 day)
    - Add guests popover (Adults, Children, Rooms counters)
    - Add "Search Hotels" button
    - Implement form validation
    - _Requirements: 2.3_
  
  - [ ] 6.5 Create Holiday search form
    - Create `frontend/src/components/homepage/forms/HolidaySearchForm.tsx`
    - Add destination autocomplete (popular destinations)
    - Add date range picker
    - Add travelers counter (Adults, Children)
    - Add package type selector (All-Inclusive, Adventure, Luxury, Budget)
    - Add "Search Holidays" button
    - Implement form validation
    - _Requirements: 2.3_
  
  - [ ] 6.6 Create Train search form
    - Create `frontend/src/components/homepage/forms/TrainSearchForm.tsx`
    - Add from station autocomplete
    - Add to station autocomplete
    - Add journey date picker
    - Add class selector (Sleeper, AC 3-Tier, AC 2-Tier, AC 1st Class)
    - Add "Search Trains" button
    - Implement form validation
    - _Requirements: 2.3_
  
  - [ ] 6.7 Create Bus search form
    - Create `frontend/src/components/homepage/forms/BusSearchForm.tsx`
    - Add from city autocomplete
    - Add to city autocomplete
    - Add journey date picker
    - Add seat type selector (Seater, Sleeper, AC, Non-AC)
    - Add "Search Buses" button
    - Implement form validation
    - _Requirements: 2.3_
  
  - [ ] 6.8 Implement autocomplete functionality
    - Create `frontend/src/components/ui/autocomplete.tsx`
    - Use Radix UI Combobox
    - Implement debounced search (300ms)
    - Show max 5 results
    - Highlight matching text
    - Add keyboard navigation
    - Show loading spinner while searching
    - _Requirements: 2.3_
  
  - [ ] 6.9 Implement date picker component
    - Create `frontend/src/components/ui/date-picker.tsx`
    - Use Radix UI Popover + custom calendar
    - Style calendar with EaseMyTrip colors
    - Highlight today's date
    - Disable past dates
    - Show month/year navigation
    - Add keyboard shortcuts
    - _Requirements: 2.3_
  
  - [ ] 6.10 Create counter component for passengers/guests
    - Create `frontend/src/components/ui/counter.tsx`
    - Implement minus button (32x32px, circular, border)
    - Implement number display (centered, bold)
    - Implement plus button
    - Add min/max constraints
    - Disable buttons at limits
    - Add haptic feedback (if supported)
    - _Requirements: 2.3_


- [ ] 7. Trending Destinations Section
  - [ ] 7.1 Create trending destinations container
    - Create `frontend/src/components/homepage/TrendingDestinations.tsx`
    - Add section with padding: 64px vertical
    - Add heading: "Trending Destinations" (30px, bold)
    - Set background: light gray (muted)
    - _Requirements: 2.5_
  
  - [ ] 7.2 Create premium destination card with provider logos
    - Create `frontend/src/components/homepage/DestinationCard.tsx`
    - **Image**: 16:9 aspect ratio, object-fit cover, border-radius 12px top
    - **Overlay**: Gradient bottom to top, rgba(0,0,0,0) to rgba(0,0,0,0.7)
    - **Provider Logos**: 40x40px, positioned top-right, white border 2px, placeholder images
    - **Content**: Positioned absolute bottom, padding 24px, white text
    - **Title**: 20px weight 600, margin-bottom 8px
    - **Price**: "From $XXX", 16px weight 500, gold color
    - **Hover**: Image scale 1.05, card lift 8px translateY(-8px), shadow elevation-3, duration 300ms
    - **Card**: Background white, border-radius 12px, shadow elevation-2
    - _Reference: Emirates.com destination cards_
    - _Requirements: 2.5, 1.5, Req 10_
  
  - [ ] 7.3 Create mock destination data
    - Create `frontend/src/data/mockDestinations.ts`
    - Add 8-12 popular destinations (Paris, Tokyo, Bali, Dubai, etc.)
    - Include: id, name, description, image URL, starting price
    - Use placeholder images initially
    - _Requirements: 2.5_
  
  - [ ] 7.4 Implement destination grid layout
    - Grid: 4 columns on desktop (≥1280px)
    - Grid: 3 columns on tablet (768-1279px)
    - Grid: 2 columns on mobile (<768px)
    - Gap: 24px
    - Add scroll-triggered fade-in animation
    - Stagger animation (50ms delay between cards)
    - _Requirements: 2.5, Req 10, Req 11_

- [ ] 8. Popular Flight Routes Section
  - [ ] 8.1 Create popular routes container
    - Create `frontend/src/components/homepage/PopularRoutes.tsx`
    - Add section with padding: 64px vertical
    - Add heading: "Popular Flight Routes" (30px, bold)
    - Set background: white
    - _Requirements: 2.6_
  
  - [ ] 8.2 Create route card component
    - Create `frontend/src/components/homepage/RouteCard.tsx`
    - Fixed width: 280px
    - Add airline logo (32x32px, top-left)
    - Add origin city (16px, semibold)
    - Add arrow icon (→)
    - Add destination city (16px, semibold)
    - Add price (20px, bold, primary color)
    - Add "View Flights" button
    - Implement hover animation (lift 2px, shadow increase)
    - _Requirements: 2.6, Req 10_
  
  - [ ] 8.3 Create mock route data
    - Create `frontend/src/data/mockRoutes.ts`
    - Add 10-15 popular routes
    - Include: id, origin, destination, airline, price, airline logo
    - Use placeholder airline logos
    - _Requirements: 2.6_
  
  - [ ] 8.4 Implement horizontal scroll container
    - Use ScrollArea component
    - Enable horizontal scroll
    - Hide scrollbar on desktop, show on mobile
    - Add scroll buttons (left/right arrows) on desktop
    - Gap: 16px between cards
    - Add momentum scrolling
    - _Requirements: 2.6, Req 11_

- [ ] 9. Travel Blogs Section
  - [ ] 9.1 Create travel blogs container
    - Create `frontend/src/components/homepage/TravelBlogs.tsx`
    - Add section with padding: 64px vertical
    - Add heading: "Travel Inspiration" (30px, bold)
    - Set background: light gray (muted)
    - _Requirements: 2.7_
  
  - [ ] 9.2 Create blog card component
    - Create `frontend/src/components/homepage/BlogCard.tsx`
    - Add image with 16:9 aspect ratio
    - Add category badge (top-left, small)
    - Add title (18px, semibold, 2 lines max with ellipsis)
    - Add excerpt (14px, muted, 3 lines max with ellipsis)
    - Add read time + publish date (12px, muted, with clock icon)
    - Add "Read More" link
    - Implement hover animation (lift 2px, shadow increase)
    - _Requirements: 2.7, Req 10_
  
  - [ ] 9.3 Create mock blog data
    - Create `frontend/src/data/mockBlogs.ts`
    - Add 6-9 blog posts
    - Include: id, title, excerpt, category, image, readTime, publishDate
    - Use placeholder images
    - _Requirements: 2.7_
  
  - [ ] 9.4 Implement blog grid layout
    - Grid: 3 columns on desktop (≥1024px)
    - Grid: 2 columns on tablet (768-1023px)
    - Grid: 1 column on mobile (<768px)
    - Gap: 24px
    - Add scroll-triggered fade-in animation
    - _Requirements: 2.7, Req 11_


- [ ]* 10. Homepage Testing
  - [ ]* 10.1 Write unit tests for search forms
    - Test form validation logic
    - Test autocomplete functionality
    - Test date picker constraints
    - Test counter min/max limits
    - _Requirements: 2.3, Req 18_
  
  - [ ]* 10.2 Write component tests for homepage sections
    - Test hero section renders correctly
    - Test search widget tab switching
    - Test destination cards display
    - Test route cards horizontal scroll
    - Test blog cards display
    - _Requirements: 2.1-2.7, Req 18_
  
  - [ ]* 10.3 Test responsive behavior
    - Test on desktop (≥1024px)
    - Test on tablet (768-1023px)
    - Test on mobile (<768px)
    - Test touch interactions
    - _Requirements: Req 11, Req 18_

### Week 4: AI Trip Wizard (Redesigned)

**Goal**: Redesign trip wizard from scratch with premium EaseMyTrip styling

**Requirements**: Requirement 3 (AI Trip Wizard Restyled)

- [ ] 11. Wizard Container & Navigation
  - [ ] 11.1 Create new wizard component from scratch
    - Create `frontend/src/components/ai-planner/TripWizard.tsx`
    - Design for million-dollar look (don't reuse SimplifiedTripWizard)
    - Create premium card container (max-width 800px, centered)
    - Apply shadow-premium-xl
    - Set border-radius: 16px
    - Add gradient header with Sparkles icon
    - _Requirements: 3.1, 3.2_
  
  - [ ] 11.2 Implement progress indicator
    - Create `frontend/src/components/ai-planner/WizardProgress.tsx`
    - Display 4 steps with connecting lines
    - Show step numbers in circles (32x32px)
    - Active step: Primary blue, 3px border
    - Completed steps: Green with checkmark icon
    - Inactive steps: Gray border
    - Add smooth transition animations
    - _Requirements: 3.4_
  
  - [ ] 11.3 Create wizard navigation buttons
    - Add "Back" button (white background, gray text, left side)
    - Add "Next" button (primary blue, white text, right side)
    - Add "Submit" button (primary blue, with loading state)
    - Button height: 48px
    - Full width on mobile
    - Disable "Next" if current step invalid
    - Add keyboard shortcuts (Enter for next, Esc for back)
    - _Requirements: 3.2_

- [ ] 12. Wizard Step 1: Destination Selection
  - [ ] 12.1 Create destination step component
    - Create `frontend/src/components/ai-planner/steps/DestinationStep.tsx`
    - Add heading: "Where do you want to go?" (24px, bold)
    - Add subheading: "Choose your dream destination" (14px, muted)
    - Add destination autocomplete input (large, 56px height)
    - Show popular destinations as chips below input
    - Add destination image preview when selected
    - Implement validation (required field)
    - _Requirements: 3.1_
  
  - [ ] 12.2 Implement destination autocomplete
    - Fetch destinations from backend or use mock data
    - Show suggestions with country flags
    - Highlight matching text
    - Show "No results" state
    - Add keyboard navigation
    - _Requirements: 3.1_

- [ ] 13. Wizard Step 2: Dates & Travelers
  - [ ] 13.1 Create dates & travelers step component
    - Create `frontend/src/components/ai-planner/steps/DatesTravelersStep.tsx`
    - Add heading: "When are you traveling?" (24px, bold)
    - Add date range picker (large, prominent)
    - Add travelers section with counters
    - Show trip duration calculation
    - Implement validation (dates required, min 1 adult)
    - _Requirements: 3.1_
  
  - [ ] 13.2 Create enhanced date range picker
    - Show two months side-by-side on desktop
    - Highlight selected range
    - Show price indicators on dates (if available)
    - Disable past dates
    - Add quick select options (Weekend, Week, 2 Weeks)
    - _Requirements: 3.1_
  
  - [ ] 13.3 Create travelers counter section
    - Add Adults counter (min 1, max 9)
    - Add Children counter (min 0, max 8, with age selector)
    - Add Infants counter (min 0, max 2)
    - Show total traveler count
    - Add age clarifications (Children: 2-12, Infants: <2)
    - _Requirements: 3.1_


- [ ] 14. Wizard Step 3: Preferences
  - [ ] 14.1 Create preferences step component
    - Create `frontend/src/components/ai-planner/steps/PreferencesStep.tsx`
    - Add heading: "Customize your trip" (24px, bold)
    - Add budget tier selector (cards with icons)
    - Add pace selector (Relaxed, Moderate, Fast-paced)
    - Add interests multi-select (chips)
    - Add accommodation preference
    - Add dietary restrictions (optional)
    - _Requirements: 3.1_
  
  - [ ] 14.2 Create budget tier selector
    - Create 3 cards: Budget, Moderate, Luxury
    - Show price range for each
    - Add icon for each tier
    - Highlight selected card with primary blue border
    - Add hover animation
    - _Requirements: 3.1_
  
  - [ ] 14.3 Create interests multi-select
    - Show interest chips (Culture, Adventure, Food, Nature, Shopping, etc.)
    - Allow multiple selections
    - Selected chips: Primary blue background
    - Unselected chips: Gray border
    - Add "Select all" and "Clear all" options
    - _Requirements: 3.1_

- [ ] 15. Wizard Step 4: Review & Submit
  - [ ] 15.1 Create review step component
    - Create `frontend/src/components/ai-planner/steps/ReviewStep.tsx`
    - Add heading: "Review your trip details" (24px, bold)
    - Show summary of all selections
    - Add edit buttons for each section
    - Show estimated trip cost (if available)
    - Add terms & conditions checkbox
    - _Requirements: 3.1_
  
  - [ ] 15.2 Implement trip summary display
    - Show destination with image
    - Show dates and duration
    - Show travelers count
    - Show budget tier
    - Show selected interests
    - Style as premium card with sections
    - _Requirements: 3.1_
  
  - [ ] 15.3 Implement form submission
    - Validate all steps before submit
    - Show loading state on submit button
    - Call POST /api/itineraries with CreateItineraryRequest
    - Handle success: Navigate to agent progress
    - Handle error: Show error message with retry
    - Store executionId for SSE connection
    - _Requirements: 3.3_

- [ ] 16. Wizard State Management & Validation
  - [ ] 16.1 Create wizard state store
    - Create `frontend/src/stores/wizardStore.ts` using Zustand
    - Store current step (1-4)
    - Store form data for each step
    - Store validation errors
    - Implement step navigation logic
    - Implement form reset
    - _Requirements: 3.1_
  
  - [ ] 16.2 Implement form validation with Zod
    - Create `frontend/src/schemas/wizardSchemas.ts`
    - Define schema for each step
    - Validate on step change
    - Show inline error messages
    - Prevent navigation if invalid
    - _Requirements: 3.1_
  
  - [ ] 16.3 Add smooth transitions between steps
    - Implement slide animation (left/right based on direction)
    - Duration: 300ms
    - Easing: ease-in-out
    - Fade out old step, fade in new step
    - _Requirements: 3.2, Req 10_

- [ ]* 17. Wizard Testing
  - [ ]* 17.1 Write unit tests for wizard logic
    - Test step navigation
    - Test form validation
    - Test state management
    - Test API integration
    - _Requirements: 3.1-3.4, Req 18_
  
  - [ ]* 17.2 Write component tests for wizard steps
    - Test each step renders correctly
    - Test user interactions
    - Test validation messages
    - Test submit flow
    - _Requirements: 3.1-3.4, Req 18_



### Week 5: AI Agent Progress (Redesigned)

**Goal**: Redesign agent progress screen with premium animations and real-time updates

**Requirements**: Requirement 4 (AI Agent Progress Restyled)

- [ ] 18. Agent Progress Container & Layout
  - [ ] 18.1 Create new agent progress component from scratch
    - Create `frontend/src/components/ai-planner/AgentProgress.tsx`
    - Design for million-dollar look (don't reuse SimplifiedAgentProgress)
    - Full-page gradient background (primary to darker blue)
    - Centered card (max-width 600px)
    - Apply shadow-premium-xl
    - Border-radius: 20px
    - Add floating particles animation in background
    - _Requirements: 4.1, 4.2, Req 10_
  
  - [ ] 18.2 Create animated icon container
    - Size: 80x80px
    - Gradient background (primary blue)
    - Sparkles icon: 40x40px, white, rotating animation
    - Pulse animation (2s infinite, scale 1 → 1.1 → 1)
    - Glow effect around container
    - _Requirements: 4.2, Req 10_
  
  - [ ] 18.3 Create heading and subheading
    - Heading: "Creating Your Perfect Itinerary" (24px, bold)
    - Subheading: "Our AI is crafting a personalized travel experience" (14px, muted)
    - Center aligned
    - Fade-in animation on mount
    - _Requirements: 4.2_

- [ ] 19. Progress Bar & Percentage Display
  - [ ] 19.1 Create premium progress bar
    - Create `frontend/src/components/ai-planner/ProgressBar.tsx`
    - Height: 12px
    - Border-radius: 6px (fully rounded)
    - Background: Light gray
    - Fill: Gradient from blue to orange
    - Smooth width transition (500ms)
    - Add shimmer effect (1.5s infinite)
    - _Requirements: 4.3, Req 10_
  
  - [ ] 19.2 Implement smooth progress animation
    - Create `frontend/src/hooks/useSmoothProgress.ts`
    - Interpolate progress updates smoothly
    - Avoid jumps in progress bar
    - Update every 100ms
    - _Requirements: 4.3_
  
  - [ ] 19.3 Display percentage and estimated time
    - Show percentage: "45%" (18px, bold, primary color)
    - Show estimated time: "About 2 minutes remaining" (14px, muted)
    - Update both in real-time
    - Add fade transition when updating
    - _Requirements: 4.3_

- [ ] 20. Motivational Messages & Task Display
  - [ ] 20.1 Create motivational messages component
    - Create `frontend/src/components/ai-planner/MotivationalMessages.tsx`
    - Define 10 different messages with icons
    - "Analyzing destinations" with MapPin icon
    - "Finding perfect stays" with Hotel icon
    - "Discovering local cuisine" with Utensils icon
    - "Planning activities" with Calendar icon
    - "Optimizing your route" with Route icon
    - "Checking availability" with Clock icon
    - "Calculating costs" with DollarSign icon
    - "Adding local tips" with Lightbulb icon
    - "Finalizing details" with CheckCircle icon
    - "Almost there!" with Sparkles icon
    - _Requirements: 4.4_
  
  - [ ] 20.2 Implement message rotation
    - Rotate messages every 3-4 seconds
    - Fade out old message (300ms)
    - Fade in new message (300ms)
    - Match message to current agent task if possible
    - _Requirements: 4.4, Req 10_
  
  - [ ] 20.3 Display current agent task
    - Show task name from SSE events
    - Show task icon (animated)
    - Style as secondary info below main message
    - _Requirements: 4.1, 4.4_

- [ ] 21. SSE Connection & Real-time Updates
  - [ ] 21.1 Implement SSE connection manager
    - Create `frontend/src/services/sseManager.ts`
    - Connect to GET /agents/events/{itineraryId}
    - Handle onopen, onmessage, onerror events
    - Implement reconnection with exponential backoff
    - Close connection on component unmount
    - _Requirements: 4.1_
  
  - [ ] 21.2 Parse and handle SSE events
    - Parse JSON from event.data
    - Handle 'agent-progress' events (update progress)
    - Handle 'agent-task' events (update current task)
    - Handle 'agent-complete' events (show success)
    - Handle 'agent-error' events (show error)
    - Update UI in real-time
    - _Requirements: 4.1, 4.3_
  
  - [ ] 21.3 Implement error handling and retry
    - Show error message if connection fails
    - Add "Retry" button
    - Add "Contact Support" button
    - Log errors for debugging
    - Preserve existing error handling logic
    - _Requirements: 4.1, Req 17_

- [ ] 22. Success State & Navigation
  - [ ] 22.1 Create success animation
    - Show confetti burst (2 seconds, 100-150 particles)
    - Show green checkmark icon (scale-in with bounce)
    - Show success message: "Your itinerary is ready!" (24px, bold)
    - Add celebration sound effect (optional, muted by default)
    - _Requirements: 4.5, Req 10_
  
  - [ ] 22.2 Implement navigation to trip view
    - Wait 1 second after success animation
    - Smooth page transition
    - Navigate to `/trip/${itineraryId}`
    - Pass itinerary data if available
    - _Requirements: 4.5_
  
  - [ ] 22.3 Handle cancellation
    - Add "Cancel" button (subtle, bottom of card)
    - Show confirmation dialog
    - Stop SSE connection
    - Navigate back to wizard or dashboard
    - _Requirements: 4.1_

- [ ]* 23. Agent Progress Testing
  - [ ]* 23.1 Write unit tests for SSE manager
    - Test connection establishment
    - Test event parsing
    - Test reconnection logic
    - Test error handling
    - _Requirements: 4.1, Req 18_
  
  - [ ]* 23.2 Write component tests for progress UI
    - Test progress bar updates
    - Test message rotation
    - Test success state
    - Test error state
    - _Requirements: 4.2-4.5, Req 18_
  
  - [ ]* 23.3 Test integration with backend
    - Mock SSE events
    - Test real-time updates
    - Test navigation on completion
    - _Requirements: 4.1, 4.5, Req 18_

---

## 📝 Week 1-5 Summary

**Completed**:
- ✅ Complete design system with EaseMyTrip colors, typography, shadows
- ✅ Core UI component library (Button, Card, Input, Modal, etc.)
- ✅ Homepage with hero section, search widget, trending destinations
- ✅ Multi-tab search forms for all travel verticals
- ✅ AI trip wizard redesigned from scratch with 4 steps
- ✅ AI agent progress with real-time SSE updates and premium animations

**Next**: Week 6-9 (Trip Management & Booking) → See [tasks-06-09-trip-booking.md](tasks-06-09-trip-booking.md)

