# Implementation Plan

## Overview

This implementation plan breaks down the premium UI redesign into atomic, actionable tasks. Each task is designed to be independently executable with clear acceptance criteria and references to specific requirements.

---

## Phase 1: Foundation and Design System

### - [x] 1. Set up project dependencies and configuration

- [x] 1.1 Install core animation libraries
  - Install framer-motion@^10.16.0 with `npm install framer-motion`
  - Install @radix-ui/react-slider@^1.1.2 for budget slider
  - Install @radix-ui/react-dialog@^1.0.5 for modals
  - Install @radix-ui/react-popover@^1.0.7 for tooltips
  - Install lucide-react@^0.294.0 for icons
  - Verify all packages are in package.json with correct versions
  - _Requirements: 1.1, 1.2, 12.1_

- [x] 1.2 Configure TypeScript for strict type checking
  - Update tsconfig.json with strict: true
  - Add paths configuration for @/ alias pointing to src/
  - Enable noUncheckedIndexedAccess for safer array access
  - Add skipLibCheck: true for faster builds
  - Verify TypeScript compilation with `npm run type-check`
  - _Requirements: 12.5_

- [x] 1.3 Set up Tailwind CSS with custom configuration
  - Extend tailwind.config.ts with custom color palette from design tokens
  - Add custom spacing scale (4px base unit)
  - Configure custom font families (Inter for sans, JetBrains Mono for mono)
  - Add custom shadow utilities matching design system
  - Add custom animation utilities for common transitions
  - Test Tailwind build with `npm run build:css`
  - _Requirements: 1.1, 1.3, 1.4, 1.5_

### - [x] 2. Create design token system

- [x] 2.1 Implement color tokens
  - Create `src/lib/design-tokens/colors.ts` file
  - Define primary color palette (50-900 scale) with #0ea5e9 as 500
  - Define semantic colors (success, warning, error) with light/main/dark variants
  - Define neutral gray scale (50-900) with proper contrast ratios
  - Export colors as const object with TypeScript types
  - Verify all colors meet WCAG AA contrast requirements (4.5:1 minimum)
  - _Requirements: 1.1, 10.1_

- [x] 2.2 Implement motion tokens
  - Create `src/lib/design-tokens/motion.ts` file
  - Define duration values (instant: 100ms, fast: 150ms, normal: 250ms, slow: 400ms, slower: 600ms, slowest: 800ms)
  - Define easing functions (easeOut, easeIn, easeInOut, spring) with cubic-bezier values
  - Define Framer Motion spring configurations (gentle, snappy, bouncy)
  - Export motion tokens as const object with TypeScript types
  - _Requirements: 1.2, 6.2, 8.1_

- [x] 2.3 Implement typography tokens
  - Create `src/lib/design-tokens/typography.ts` file
  - Define font families with fallback stacks
  - Define font size scale using modular scale (1.250 ratio)
  - Define font weights (normal: 400, medium: 500, semibold: 600, bold: 700)
  - Define line heights (tight: 1.2, normal: 1.5, relaxed: 1.75)
  - Export typography tokens as const object
  - _Requirements: 1.3_

- [x] 2.4 Implement spacing tokens
  - Create `src/lib/design-tokens/spacing.ts` file
  - Define spacing scale from 0 to 24 using 4px base unit
  - Export spacing tokens as const object
  - _Requirements: 1.4_

- [x] 2.5 Implement shadow tokens
  - Create `src/lib/design-tokens/shadows.ts` file
  - Define elevation-based shadows (none, sm, base, md, lg, xl, 2xl, inner)
  - Use rgba with appropriate opacity values for depth
  - Export shadow tokens as const object
  - _Requirements: 1.5_

- [x] 2.6 Create centralized tokens export
  - Create `src/lib/design-tokens/index.ts` barrel file
  - Re-export all token modules (colors, motion, typography, spacing, shadows)
  - Add JSDoc comments documenting usage examples
  - _Requirements: 12.4_

### - [x] 3. Build animation variants library

- [x] 3.1 Create base animation variants
  - Create `src/lib/animations/variants.ts` file
  - Implement fadeIn variant (opacity: 0 → 1)
  - Implement fadeInUp variant (opacity: 0, y: 20 → opacity: 1, y: 0)
  - Implement scaleIn variant (opacity: 0, scale: 0.95 → opacity: 1, scale: 1)
  - Implement slideInRight variant (x: 100, opacity: 0 → x: 0, opacity: 1)
  - Implement slideInLeft variant (x: -100, opacity: 0 → x: 0, opacity: 1)
  - Use motion tokens for duration and easing
  - Export all variants with proper TypeScript Variants type
  - _Requirements: 6.3, 12.1_

- [x] 3.2 Create interaction animation variants
  - In `src/lib/animations/variants.ts`, add staggerContainer variant
  - Implement hoverScale variant (scale: 1 → 1.02 on hover, 0.98 on tap)
  - Implement hoverLift variant (y: 0, shadow: md → y: -4, shadow: xl on hover)
  - Implement shimmer variant for loading states with background position animation
  - Add proper transition configurations for each variant
  - _Requirements: 8.2, 8.5, 19.2_

- [x] 3.3 Create page transition variants
  - Add pageTransition variant for wizard step changes
  - Implement modal variants (backdrop fade, content scale)
  - Implement dropdown variants (fade + slide from top)
  - Export all variants with JSDoc documentation
  - _Requirements: 6.3, 6.7_

### - [x] 4. Create custom animation hooks

- [x] 4.1 Implement useScrollAnimation hook
  - Create `src/hooks/animations/useScrollAnimation.ts` file
  - Use Framer Motion's useInView hook with once: true, margin: '-100px'
  - Use useAnimation hook for manual control
  - Return ref and controls for component usage
  - Add TypeScript types for return value
  - _Requirements: 8.5_

- [x] 4.2 Implement useFadeIn hook
  - Create `src/hooks/animations/useFadeIn.ts` file
  - Compose useScrollAnimation with fadeInUp variant
  - Accept optional delay parameter (default: 0)
  - Return props object ready to spread on motion components
  - _Requirements: 8.4_

- [x] 4.3 Implement useHoverScale hook
  - Create `src/hooks/animations/useHoverScale.ts` file
  - Use useState to track hover state
  - Return event handlers (onMouseEnter, onMouseLeave) and animate prop
  - Accept optional scale parameter (default: 1.02)
  - _Requirements: 8.1, 19.2_

- [x] 4.4 Implement useStaggerChildren hook
  - Create `src/hooks/animations/useStaggerChildren.ts` file
  - Accept staggerDelay parameter (default: 0.1)
  - Return variants object for parent container
  - _Requirements: 6.7_

- [x] 4.5 Create hooks barrel export
  - Create `src/hooks/animations/index.ts` file
  - Re-export all animation hooks
  - Add usage examples in JSDoc comments
  - _Requirements: 12.2_

### - [x] 5. Implement utility hooks

- [x] 5.1 Create useDebounce hook
  - Create `src/hooks/interactions/useDebounce.ts` file
  - Accept generic value and delay (default: 300ms)
  - Use useState and useEffect to debounce value changes
  - Return debounced value with proper TypeScript generics
  - _Requirements: 11.6, 14.2_

- [x] 5.2 Create useThrottle hook
  - Create `src/hooks/interactions/useThrottle.ts` file
  - Accept generic value and interval (default: 300ms)
  - Use useRef to track last execution time
  - Return throttled value with proper TypeScript generics
  - _Requirements: 11.6_

- [x] 5.3 Create useKeyboardNav hook
  - Create `src/hooks/interactions/useKeyboardNav.ts` file
  - Accept options object with handlers for Enter, Escape, Arrow keys, Tab
  - Use useCallback for memoized event handler
  - Add/remove event listener in useEffect
  - Support event.preventDefault() for arrow keys
  - _Requirements: 6.6, 10.2, 14.4_

- [x] 5.4 Create useReducedMotion hook
  - Create `src/hooks/animations/useReducedMotion.ts` file
  - Check prefers-reduced-motion media query
  - Listen for changes and update state
  - Return boolean indicating user preference
  - _Requirements: 9.6, 10.5_

---

## Phase 2: Core Search Components

### - [x] 6. Build Unified Search Bar component

- [x] 6.1 Create SearchField sub-component
  - Create `src/components/premium/search/SearchField.tsx` file
  - Accept props: icon, label, placeholder, value, onChange, isActive, onFocus, autoFocus
  - Render label with sr-only class for accessibility
  - Render input with proper ARIA attributes (aria-label, aria-required)
  - Apply focus styles with ring-2 ring-primary-500 on focus
  - Implement smooth transition for border and shadow
  - _Requirements: 13.1, 13.6, 10.2_

- [x] 6.2 Implement UnifiedSearchBar container
  - Create `src/components/premium/search/UnifiedSearchBar.tsx` file
  - Create state for from, to, dates, travelers, activeField
  - Wrap in motion.div with fadeInUp animation on mount
  - Render container with flex layout, gap-2, rounded-2xl, shadow-xl
  - Add white background and gray border
  - _Requirements: 13.1, 13.2_

- [x] 6.3 Add placeholder cycling animation
  - Define placeholders array: ['Paris', 'Tokyo', 'New York', 'Barcelona', 'Dubai']
  - Use useState for placeholderIndex
  - Use useEffect with setInterval to cycle every 3 seconds
  - Apply fade transition when placeholder changes
  - Clean up interval on unmount
  - _Requirements: 13.2_

- [x] 6.4 Implement swap button functionality
  - Add swap button between from and to fields
  - Use ArrowLeftRight icon from lucide-react
  - Implement handleSwap function to exchange from/to values
  - Add whileHover scale: 1.1 animation
  - Add whileTap scale: 0.9, rotate: 180 animation
  - Apply rounded-full, hover:bg-gray-100 styles
  - _Requirements: 13.3_

- [x] 6.5 Add Enter key search support
  - Implement onKeyDown handler on all input fields
  - Check for event.key === 'Enter'
  - Call onSearch callback with current values
  - Prevent default form submission
  - _Requirements: 13.4_

- [x] 6.6 Add search button with animations
  - Create search button with Search icon and "Search" text
  - Apply bg-primary-500, text-white, rounded-xl styles
  - Add whileHover scale: 1.05 and shadow animation
  - Add whileTap scale: 0.95 animation
  - Implement onClick handler calling onSearch
  - _Requirements: 13.1, 8.2_

- [x] 6.7 Add microcopy below search bar
  - Render paragraph with "Find your perfect trip in seconds"
  - Apply text-center, text-sm, text-gray-500, mt-3 styles
  - Add fade-in animation with 0.3s delay
  - _Requirements: 13.5_



### - [x] 7. Build Location Autocomplete component

- [x] 7.1 Create usePlacesAutocomplete hook
  - Create `src/hooks/data/usePlacesAutocomplete.ts` file
  - Accept query string parameter
  - Use useDebounce hook with 300ms delay on query
  - Initialize Google Places AutocompleteService
  - Call getPlacePredictions with debounced query
  - Parse predictions into PlaceSuggestion[] format with id, name, type, country, coordinates
  - Implement determineType helper to classify as city/airport/landmark
  - Return suggestions, recentSearches, loading state
  - _Requirements: 14.1, 14.2, 11.6_

- [x] 7.2 Implement suggestion grouping logic
  - In usePlacesAutocomplete, filter suggestions by type
  - Create groupedSuggestions object with cities, airports, landmarks arrays
  - Sort each group alphabetically by name
  - Limit each group to maximum 5 items
  - _Requirements: 14.1_

- [x] 7.3 Create SuggestionItem sub-component
  - Create SuggestionItem function component in LocationAutocomplete file
  - Accept props: icon, primary, secondary, onClick
  - Wrap in motion.button with whileHover x: 4 animation
  - Apply flex layout with gap-3, rounded-lg, hover:bg-gray-50
  - Render icon in gray-400 color
  - Render primary text with text-sm, font-medium, text-gray-900, truncate
  - Render secondary text with text-xs, text-gray-500, truncate
  - _Requirements: 14.5, 14.7_

- [x] 7.4 Implement text highlighting
  - Create highlightMatch helper function
  - Split text by query using regex with case-insensitive flag
  - Map parts to React elements, wrapping matches in <strong> with font-semibold, text-primary-600
  - Return array of React nodes
  - _Requirements: 14.2_

- [x] 7.5 Build dropdown container with animations
  - Wrap dropdown in AnimatePresence for exit animations
  - Conditionally render when isOpen && (value || recentSearches.length > 0)
  - Apply motion.div with initial: opacity 0, y: -10, animate: opacity 1, y: 0
  - Set exit animation to opacity 0, y: -10 with 200ms duration
  - Apply absolute positioning, z-50, w-full, mt-2
  - Style with bg-white, rounded-xl, shadow-2xl, border, max-h-96, overflow-y-auto
  - _Requirements: 14.1, 15.6_

- [x] 7.6 Render recent searches section
  - Check if !value && recentSearches.length > 0
  - Render section header "Recent Searches" with uppercase, text-xs, font-semibold, text-gray-500
  - Map recentSearches to SuggestionItem components
  - Use Clock icon from lucide-react
  - _Requirements: 14.3_

- [x] 7.7 Render grouped suggestions sections
  - For each group (cities, airports, landmarks), check if length > 0
  - Render section header with appropriate label
  - Map suggestions to SuggestionItem components
  - Use MapPin icon for cities, Plane for airports, Landmark for landmarks
  - Apply highlightMatch to suggestion names
  - _Requirements: 14.1, 14.7_

- [x] 7.8 Implement keyboard navigation
  - Add state for selectedIndex
  - Implement useKeyboardNav hook with onArrowUp, onArrowDown, onEnter, onEscape
  - Update selectedIndex on arrow key press (with wrapping)
  - Highlight selected suggestion with bg-gray-100
  - Select suggestion on Enter key
  - Close dropdown on Escape key
  - _Requirements: 14.4, 10.2_

- [x] 7.9 Add "Did you mean?" fallback
  - Check if value && suggestions.length === 0 && !loading
  - Render centered message "No results found"
  - Add "Did you mean:" text with fuzzy-matched alternative
  - Make alternative clickable with text-primary-600, hover:underline
  - _Requirements: 14.6_

- [x] 7.10 Implement focus management
  - Add onFocus handler to set isOpen to true
  - Add onBlur handler with setTimeout (200ms) to set isOpen to false
  - Delay allows click events on suggestions to fire before blur
  - _Requirements: 10.2_

### - [x] 8. Build Date Range Picker component

- [x] 8.1 Create calendar utility functions
  - Create `src/utils/calendar.ts` file
  - Implement getMonthDays function using date-fns eachDayOfInterval
  - Implement isDateInRange function to check if date is between start and end
  - Implement isDateDisabled function for past dates
  - Implement getDaysBetween function for duration calculation
  - Export all utilities with TypeScript types
  - _Requirements: 15.1, 15.3_

- [x] 8.2 Create CalendarDay sub-component
  - Create CalendarDay function component in DateRangePicker file
  - Accept props: date, isSelected, isInRange, isStart, isEnd, priceLevel, onClick, onHover
  - Wrap in motion.button with whileHover scale: 1.05, whileTap scale: 0.95
  - Apply conditional classes based on selection state
  - Render date number with format(date, 'd')
  - Add price dot indicator at bottom if priceLevel exists
  - Color dot based on level: green (low), yellow (medium), red (high)
  - _Requirements: 15.4, 15.7_

- [x] 8.3 Create CalendarMonth sub-component
  - Create CalendarMonth function component
  - Accept props: month, startDate, endDate, hoveredDate, onDateClick, onDateHover, getPriceLevel
  - Get days for month using getMonthDays utility
  - Render 7-column grid for days
  - Map days to CalendarDay components
  - Handle empty cells for days not in current month
  - _Requirements: 15.1_

- [x] 8.4 Implement date selection logic
  - Create state for startDate, endDate, hoveredDate
  - Implement handleDateClick function
  - If no startDate or both dates exist, set startDate to clicked date and clear endDate
  - If startDate exists but no endDate, set endDate (swap if clicked date is before startDate)
  - Call onChange callback with new dates
  - _Requirements: 15.3, 15.7_

- [x] 8.5 Add hover preview for date range
  - Implement onDateHover handler to update hoveredDate state
  - In CalendarDay, check if date is between startDate and hoveredDate
  - Apply bg-primary-100 to hovered range
  - Show duration tooltip when hovering
  - _Requirements: 15.3_

- [x] 8.6 Create quick preset buttons
  - Define presets array: "This Weekend" (2 days), "Next Week" (7 days), "Next Month" (30 days), "Flexible" (null)
  - Render preset buttons above calendar
  - Apply px-4, py-2, text-sm, bg-gray-100, rounded-lg, hover:bg-gray-200
  - Add whileHover scale: 1.05, whileTap scale: 0.95 animations
  - Implement onClick to calculate and set date range
  - _Requirements: 15.2_

- [x] 8.7 Implement dual-month calendar layout
  - Create state for currentMonth
  - Render two CalendarMonth components side by side
  - Use grid grid-cols-2 gap-8 for desktop layout
  - Add navigation buttons (ChevronLeft, ChevronRight) to change months
  - Only show left arrow on first month, right arrow on second month
  - _Requirements: 15.1_

- [x] 8.8 Add month navigation
  - Implement handlePrevMonth to subtract 1 month from currentMonth
  - Implement handleNextMonth to add 1 month to currentMonth
  - Use date-fns addMonths function
  - Apply hover:bg-gray-100, rounded-lg, transition-colors to buttons
  - _Requirements: 15.1_

- [x] 8.9 Display duration tooltip on hover
  - Calculate duration using getDaysBetween utility
  - Format as "X nights, Y days"
  - Render in motion.div with initial/animate fade-in
  - Position below calendar grid
  - Only show when hoveredDate exists and startDate is set
  - _Requirements: 15.3_

- [x] 8.10 Add Clear and Apply buttons
  - Render buttons in footer with flex justify-between
  - Clear button: text-sm, font-medium, text-gray-600, hover:text-gray-900
  - Apply button: px-6, py-2, bg-primary-500, text-white, rounded-lg, font-semibold
  - Disable Apply button when !startDate || !endDate
  - Add whileHover scale: 1.05, whileTap scale: 0.95 to Apply button
  - _Requirements: 15.5_

- [x] 8.11 Integrate price hint dots
  - Accept priceData prop as Record<string, number>
  - Implement getPriceLevel function to determine low/medium/high
  - Pass getPriceLevel to CalendarDay components
  - Render colored dots under dates with pricing data
  - _Requirements: 15.4_

- [x] 8.12 Add smooth fade transitions
  - Wrap calendar in AnimatePresence
  - Apply fadeIn variant to calendar container
  - Add transition duration: 250ms with easeOut
  - Animate month changes with slide effect
  - _Requirements: 15.6_

### - [x] 9. Build Traveler Selector component

- [x] 9.1 Create TravelerRow sub-component
  - Create TravelerRow function component in TravelerSelector file
  - Accept props: label, description, count, onIncrement, onDecrement, minCount, maxCount
  - Render label with font-medium, text-gray-900
  - Render description with text-sm, text-gray-500
  - Create increment/decrement buttons with Plus/Minus icons
  - Style buttons as w-8, h-8, rounded-full, border-2
  - Apply border-primary-500, text-primary-500 when enabled
  - Apply border-gray-200, text-gray-300, cursor-not-allowed when disabled
  - _Requirements: 16.1, 16.3_

- [x] 9.2 Implement count animation
  - Wrap count display in motion.span with key={count}
  - Apply initial scale: 1.2, animate scale: 1 animation
  - Use 200ms duration for bump effect
  - Center count with w-8, text-center, font-semibold
  - _Requirements: 16.4_

- [x] 9.3 Add increment/decrement animations
  - Apply whileHover scale: 1.1 to enabled buttons
  - Apply whileTap scale: 0.9 to enabled buttons
  - No animation on disabled buttons
  - Add transition duration: 150ms
  - _Requirements: 16.4, 8.2_

- [x] 9.4 Implement traveler count logic
  - Create state for adults, children, infants
  - Implement updateCount function accepting type and delta
  - Enforce minimum: 1 for adults, 0 for children/infants
  - Enforce maximum: 9 for all categories
  - Call onChange callback with updated TravelerCounts object
  - _Requirements: 16.1, 16.7_

- [x] 9.5 Create dynamic summary text
  - Implement getSummaryText function
  - Build array of parts: "X adult(s)", "X child(ren)", "X infant(s)"
  - Join with commas
  - Return "Add travelers" if all counts are 0
  - Update trigger button text in real-time
  - _Requirements: 16.2_

- [x] 9.6 Build dropdown modal
  - Wrap in AnimatePresence for exit animations
  - Conditionally render when isOpen
  - Apply motion.div with initial: opacity 0, y: -10, scale: 0.95
  - Animate to opacity 1, y: 0, scale: 1 with 200ms duration
  - Position absolute, z-50, mt-2, w-80
  - Style with bg-white, rounded-xl, shadow-2xl, border, p-4
  - _Requirements: 16.1_

- [x] 9.7 Render traveler category rows
  - Render TravelerRow for Adults with description "18+ years"
  - Render TravelerRow for Children with description "2-17 years"
  - Render TravelerRow for Infants with description "0-2 years"
  - Apply border-b border-gray-100 between rows
  - _Requirements: 16.1, 16.6_

- [x] 9.8 Add Apply button
  - Render button at bottom with w-full, mt-4
  - Style with px-4, py-2, bg-primary-500, text-white, rounded-lg, font-semibold
  - Add whileHover scale: 1.02, whileTap scale: 0.98 animations
  - Implement onClick to close dropdown
  - _Requirements: 16.5_

- [x] 9.9 Implement trigger button
  - Create button to toggle dropdown
  - Display current summary text
  - Style with px-4, py-3, border, border-gray-300, rounded-lg, hover:border-gray-400
  - Add transition-colors for smooth hover effect
  - _Requirements: 16.2_

- [x] 9.10 Add maximum limit messaging
  - Check if any category reaches 9
  - Display informative message "Maximum travelers reached"
  - Style with text-xs, text-gray-500, mt-2
  - Only show when at limit
  - _Requirements: 16.7_

### - [x] 10. Build Budget Slider component

- [x] 10.1 Install and configure Radix Slider
  - Verify @radix-ui/react-slider is installed
  - Import Slider components (Root, Track, Range, Thumb)
  - Review Radix Slider documentation for API
  - _Requirements: 17.1_

- [x] 10.2 Implement slider with dual handles
  - Create Slider.Root with value, onValueChange, min, max, step props
  - Set step to 10 for $10 increments
  - Render Slider.Track with bg-gray-200, h-2, rounded-full
  - Render Slider.Range with gradient background
  - Render two Slider.Thumb components for min/max handles
  - Style thumbs as w-5, h-5, bg-white, border-2, border-primary-500, rounded-full, shadow-lg
  - _Requirements: 17.1_

- [x] 10.3 Add gradient feedback color
  - Implement getGradientColor function
  - Calculate midpoint of selected range
  - Return green gradient if below 70% of total range
  - Return yellow gradient if between 70-130%
  - Return orange gradient if above 130%
  - Apply gradient to Slider.Range className
  - _Requirements: 17.3_

- [x] 10.4 Implement debounced value updates
  - Create localValue state for immediate UI updates
  - Use useDebounce hook with 300ms delay
  - Update localValue immediately on slider change
  - Call onChange callback with debounced value
  - Prevents excessive API calls during dragging
  - _Requirements: 17.4, 11.6_

- [x] 10.5 Display real-time results count
  - Accept resultsCount prop (optional)
  - Render count above slider when defined
  - Wrap in motion.div with key={resultsCount}
  - Apply initial: opacity 0, y: -10, animate: opacity 1, y: 0
  - Format as "X trips found" with text-sm, font-medium, text-gray-700
  - _Requirements: 17.2_

- [x] 10.6 Add inline value display
  - Render min and max values below slider
  - Use flex justify-between layout
  - Format values with toLocaleString() for comma separators
  - Add currency symbol ($) prefix
  - Style with text-sm, font-semibold, text-gray-900
  - _Requirements: 17.1, 17.6_

- [x] 10.7 Create preset budget buttons
  - Define presets: Budget (0-30%), Mid-range (30-70%), Luxury (70-100%), Any (0-100%)
  - Calculate actual values based on min/max range
  - Render buttons above slider with flex gap-2
  - Style with px-3, py-1, text-xs, bg-gray-100, rounded-full, hover:bg-gray-200
  - Add whileHover scale: 1.05, whileTap scale: 0.95 animations
  - Implement onClick to set localValue to preset range
  - _Requirements: 17.7_

- [x] 10.8 Set default range from median
  - Accept defaultRange prop or calculate from median price
  - Set initial localValue to default range
  - Ensures relevant results shown immediately
  - _Requirements: 17.5_

- [x] 10.9 Add hover effects to slider thumbs
  - Apply hover:scale-110 to Slider.Thumb
  - Add focus:outline-none, focus:ring-2, focus:ring-primary-500, focus:ring-offset-2
  - Ensure smooth transition-transform
  - _Requirements: 8.1_

- [x] 10.10 Wrap in container with styling
  - Wrap entire component in div with w-full, p-6, bg-white, rounded-xl, border
  - Add proper spacing between elements
  - Ensure responsive layout
  - _Requirements: 17.1_

---

## Phase 3: Filters and Results Components

### - [ ] 11. Build Filter Chip Bar component

- [ ] 11.1 Create FilterChip sub-component
  - Create FilterChip function component in FilterChipBar file
  - Accept props: label, isActive, onClick, icon (optional)
  - Wrap in motion.button with whileHover scale: 1.05, whileTap scale: 0.95
  - Apply conditional classes: bg-primary-500 text-white when active, bg-gray-100 text-gray-700 when inactive
  - Add checkmark icon when active with fade-in animation
  - Style with px-4, py-2, rounded-full, text-sm, font-medium, transition-colors
  - _Requirements: 18.3_

- [ ] 11.2 Implement horizontal scrollable container
  - Wrap chips in div with flex, gap-2, overflow-x-auto
  - Add scrollbar-hide class for clean appearance
  - Ensure touch-friendly scrolling on mobile
  - Apply pb-2 for scrollbar spacing
  - _Requirements: 18.1_

- [ ] 11.3 Add sticky positioning on scroll
  - Wrap container in div with sticky, top-0, z-40
  - Add bg-white with slight shadow when stuck
  - Implement useEffect with scroll listener to detect stuck state
  - Apply slide-down animation when scrolling up
  - _Requirements: 18.2_

- [ ] 11.4 Create filter categories
  - Define filter categories: Price, Duration, Activities, Rating, Amenities
  - Map categories to FilterChip components
  - Track active filters in state
  - Implement onClick to toggle filter active state
  - _Requirements: 18.1_

- [ ] 11.5 Add "Clear all" button
  - Conditionally render when activeFilters.length > 0
  - Style with text-sm, font-medium, text-gray-600, hover:text-gray-900
  - Add fade-in animation when appearing
  - Implement onClick to clear all active filters
  - _Requirements: 18.4_

- [ ] 11.6 Display active filter count badge
  - Show count on main "Filters" button
  - Format as "Filters (X)" where X is active count
  - Style badge with bg-primary-500, text-white, rounded-full, px-2, py-0.5, text-xs
  - Only show when count > 0
  - _Requirements: 18.7_

- [ ] 11.7 Implement shimmer transition on apply
  - When filters change, apply shimmer animation to results area
  - Use shimmer variant from animations library
  - Duration: 400ms
  - Smooth transition to new results
  - _Requirements: 18.5_

### - [ ] 12. Build Sort Dropdown component

- [ ] 12.1 Create dropdown using Radix Popover
  - Import Popover components from @radix-ui/react-popover
  - Create Popover.Root, Popover.Trigger, Popover.Content structure
  - Style trigger button with current sort option
  - Add ChevronDown icon that rotates when open
  - _Requirements: 18.6_

- [ ] 12.2 Define sort options
  - Create sortOptions array: Recommended, Price: Low to High, Price: High to Low, Duration, Rating
  - Each option has label and value
  - Track current selection in state
  - _Requirements: 18.6_

- [ ] 12.3 Render sort option items
  - Map sortOptions to clickable items
  - Style with px-4, py-2, hover:bg-gray-50, rounded-lg, transition-colors
  - Add checkmark icon next to selected option
  - Implement onClick to update sort and close dropdown
  - _Requirements: 18.6_

- [ ] 12.4 Add dropdown animations
  - Wrap content in AnimatePresence
  - Apply fadeIn + scaleIn animation
  - Set origin to top for natural dropdown feel
  - Duration: 200ms
  - _Requirements: 18.6_

- [ ] 12.5 Position dropdown correctly
  - Use Radix Popover positioning (align: start, side: bottom)
  - Add sideOffset: 8 for spacing
  - Ensure dropdown stays within viewport
  - _Requirements: 18.6_

### - [ ] 13. Build Result Card component

- [ ] 13.1 Create card structure
  - Create `src/components/premium/results/ResultCard.tsx` file
  - Accept props: result (SearchResult type), onClick
  - Wrap in motion.div with whileHover animation
  - Apply bg-white, rounded-xl, border, overflow-hidden, cursor-pointer
  - Use CSS Grid for consistent layout
  - _Requirements: 19.1, 19.5_

- [ ] 13.2 Implement hover elevation effect
  - Add whileHover with translateY: -4, boxShadow: xl
  - Set transition duration: 250ms with easeOut
  - Ensure smooth animation
  - _Requirements: 19.2_

- [ ] 13.3 Add OptimizedImage component
  - Create `src/components/premium/OptimizedImage.tsx` file
  - Implement blur placeholder technique
  - Use loading="lazy" for lazy loading
  - Add fade-in animation when image loads
  - Handle error state with fallback
  - _Requirements: 19.4, 11.3_

- [ ] 13.4 Render card image
  - Use OptimizedImage component for result.image
  - Set aspect ratio to 16:9
  - Apply object-cover for proper cropping
  - Add gradient overlay at bottom for text readability
  - _Requirements: 19.4_

- [ ] 13.5 Display highlighted tags
  - Check for tags like "Best Deal", "Popular", "Recommended"
  - Render as badges with accent background
  - Position absolute at top-right of image
  - Add pulse animation on card hover
  - Style with px-3, py-1, rounded-full, text-xs, font-semibold
  - _Requirements: 19.3_

- [ ] 13.6 Render card content
  - Display title with text-lg, font-semibold, text-gray-900, truncate
  - Display destination with text-sm, text-gray-600
  - Show duration with Clock icon
  - Display rating stars with partial fill for decimals
  - Show review count in muted text
  - _Requirements: 19.1, 19.6_

- [ ] 13.7 Display price with hierarchy
  - Render price large with text-2xl, font-bold, text-gray-900
  - Add "per person" label with text-sm, text-gray-500
  - Show strikethrough original price if discounted
  - Apply text-red-500 to discount price
  - _Requirements: 19.7_

- [ ] 13.8 Add CTA button
  - Render "View Details" button at bottom
  - Style with w-full, py-2, bg-primary-500, text-white, rounded-lg, font-semibold
  - Add hover:bg-primary-600 transition
  - Implement onClick handler
  - _Requirements: 19.1_

- [ ] 13.9 Create ResultGrid container
  - Create `src/components/premium/results/ResultGrid.tsx` file
  - Use CSS Grid with auto-fit and minmax(300px, 1fr)
  - Apply gap-6 for spacing
  - Ensure equal card heights with grid-auto-rows
  - _Requirements: 19.5_

- [ ] 13.10 Implement stagger animation for cards
  - Wrap ResultGrid in motion.div with staggerContainer variant
  - Apply stagger delay of 0.1s between cards
  - Each card fades in with fadeInUp animation
  - _Requirements: 6.7_

