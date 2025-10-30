# Implementation Plan (Continued)

## Phase 4: Wizard, Progress, and Payment Components

### - [ ] 14. Build Progress Stepper component

- [ ] 14.1 Create step data structure
  - Create `src/types/wizard.ts` file
  - Define WizardStep interface with id, label, status (incomplete/current/complete)
  - Define ProgressStepperProps interface
  - Export types for use across wizard components
  - _Requirements: 20.1_

- [ ] 14.2 Implement ProgressStepper component
  - Create `src/components/premium/wizard/ProgressStepper.tsx` file
  - Accept props: steps, currentStep, onStepClick
  - Render horizontal container with flex layout
  - Map steps to individual step indicators
  - Connect steps with progress line
  - _Requirements: 20.1_

- [ ] 14.3 Create StepIndicator sub-component
  - Create StepIndicator function component
  - Accept props: step, index, isActive, isComplete, onClick
  - Render circle with step number or checkmark
  - Apply conditional styling based on state
  - Incomplete: outlined circle with gray
  - Current: filled circle with primary color
  - Complete: filled circle with checkmark icon
  - _Requirements: 20.1, 20.5_

- [ ] 14.4 Add step transition animations
  - Wrap step indicator in motion.div
  - Apply scale animation when step becomes active
  - Animate checkmark with draw-in effect using SVG stroke-dasharray
  - Duration: 400ms with easeInOut
  - _Requirements: 20.2_

- [ ] 14.5 Implement progress line animation
  - Render line between steps with relative positioning
  - Use motion.div for animated fill
  - Calculate fill percentage based on current step
  - Animate width from previous to current percentage
  - Duration: 400ms with easeInOut
  - _Requirements: 20.2_

- [ ] 14.6 Add step labels
  - Render step label below each indicator
  - Style with text-sm, font-medium
  - Apply text-gray-500 for incomplete, text-primary-600 for current, text-gray-900 for complete
  - Truncate long labels with ellipsis
  - _Requirements: 20.1_

- [ ] 14.7 Implement click navigation for completed steps
  - Add onClick handler to completed step indicators
  - Check if step is complete before allowing navigation
  - Call onStepClick callback with step index
  - Apply cursor-pointer to clickable steps, cursor-not-allowed to disabled
  - _Requirements: 20.3, 20.6_

- [ ] 14.8 Add "Save progress" hint
  - Render small text near progress indicator
  - Display "Your progress is automatically saved" with save icon
  - Style with text-xs, text-gray-500
  - Add fade-in animation with delay
  - _Requirements: 20.4_

- [ ] 14.9 Make responsive for mobile
  - On mobile (<768px), stack steps vertically
  - Reduce step indicator size
  - Adjust spacing and font sizes
  - Ensure touch-friendly tap targets (44x44px minimum)
  - _Requirements: 9.2_

### - [ ] 15. Build Step Transition component

- [ ] 15.1 Create StepTransition wrapper
  - Create `src/components/premium/wizard/StepTransition.tsx` file
  - Accept props: children, direction ('forward' | 'backward'), currentStep
  - Wrap children in AnimatePresence with mode="wait"
  - Apply motion.div with key={currentStep}
  - _Requirements: 6.3_

- [ ] 15.2 Implement slide animations
  - Define slideInRight variant for forward navigation
  - Define slideInLeft variant for backward navigation
  - Set initial position based on direction
  - Animate to center position (x: 0)
  - Exit in opposite direction
  - _Requirements: 6.3_

- [ ] 15.3 Add fade overlay during transition
  - Render semi-transparent overlay during transition
  - Fade in/out with 150ms duration
  - Prevents interaction during animation
  - Remove after transition completes
  - _Requirements: 6.3_

- [ ] 15.4 Preserve scroll position
  - Save scroll position before transition
  - Restore scroll position after transition
  - Use useEffect with currentStep dependency
  - Smooth scroll behavior
  - _Requirements: 6.3_

### - [ ] 16. Build Secure Payment Form component

- [ ] 16.1 Create form structure
  - Create `src/components/premium/payment/SecurePaymentForm.tsx` file
  - Use React Hook Form for form state management
  - Define form schema with validation rules
  - Group fields into sections: Card Details, Billing Address, Contact Info
  - _Requirements: 21.1_

- [ ] 16.2 Implement CardInput component
  - Create `src/components/premium/payment/CardInput.tsx` file
  - Accept props: value, onChange, error
  - Apply input mask for card number (groups of 4 digits)
  - Animate character entry with smooth transitions
  - Display card brand logo when detected
  - _Requirements: 21.3, 21.4_

- [ ] 16.3 Add card brand detection
  - Create `src/utils/cardBrand.ts` file
  - Implement detectCardBrand function using regex patterns
  - Support Visa, Mastercard, Amex, Discover
  - Return brand name and logo
  - Update in real-time as user types
  - _Requirements: 21.3_

- [ ] 16.4 Implement inline validation
  - Add validation rules for each field (required, format, length)
  - Display checkmark icon next to valid fields
  - Animate checkmark with fade-in and scale
  - Show error messages below invalid fields
  - Update validation in real-time (on blur)
  - _Requirements: 21.2, 21.7_

- [ ] 16.5 Add input masks
  - Card number: XXXX XXXX XXXX XXXX format
  - Expiry date: MM/YY format with auto-slash insertion
  - CVV: 3-4 digits based on card type
  - Animate character transitions smoothly
  - _Requirements: 21.4_

- [ ] 16.6 Implement autofocus flow
  - Focus first field on mount
  - Auto-advance to next field when current is complete
  - Use refs to manage focus programmatically
  - Smooth transition between fields
  - _Requirements: 21.5_

- [ ] 16.7 Add "Secure payment" badge
  - Render badge with lock icon and "Secure payment" text
  - Display SSL certificate indicator
  - Position near submit button
  - Style with text-sm, text-gray-600, flex items-center
  - _Requirements: 21.6_

- [ ] 16.8 Implement field-level error display
  - Show error message below field when validation fails
  - Animate error with shake effect (translateX: -10px → 10px → 0)
  - Display error icon next to message
  - Style with text-sm, text-red-600
  - _Requirements: 21.7_

- [ ] 16.9 Add loading state for submission
  - Disable all fields during submission
  - Show loading spinner on submit button
  - Display "Processing..." text
  - Prevent double submission
  - _Requirements: 21.2_

- [ ] 16.10 Create form sections with headers
  - Group related fields under section headers
  - Style headers with text-lg, font-semibold, text-gray-900, mb-4
  - Add spacing between sections (mt-6)
  - Ensure clear visual hierarchy
  - _Requirements: 21.1_

### - [ ] 17. Build Interactive Map component

- [ ] 17.1 Set up Google Maps integration
  - Create `src/hooks/data/useGoogleMaps.ts` hook
  - Load Google Maps JavaScript API with API key
  - Initialize map instance with default options
  - Return map ref and loading state
  - Handle API load errors gracefully
  - _Requirements: 22.1_

- [ ] 17.2 Create InteractiveMap component
  - Create `src/components/premium/map/InteractiveMap.tsx` file
  - Accept props: results, selectedResult, onResultSelect
  - Render map container with ref
  - Set initial center and zoom level
  - Apply w-full, h-full, rounded-xl, overflow-hidden
  - _Requirements: 22.1_

- [ ] 17.3 Implement split-view layout
  - Create `src/components/premium/map/MapSplitView.tsx` file
  - Use grid grid-cols-2 for desktop layout
  - Left side: result list, Right side: map
  - On mobile, stack vertically with map on top
  - Synchronize scroll and selection between views
  - _Requirements: 22.1_

- [ ] 17.4 Create PriceMarker component
  - Create `src/components/premium/map/PriceMarker.tsx` file
  - Render custom marker with price display
  - Style as rounded pill with white background, shadow
  - Display starting price with currency symbol
  - Add hover effect (scale: 1.1, shadow increase)
  - _Requirements: 22.2_

- [ ] 17.5 Implement marker clustering
  - Install @googlemaps/markerclusterer library
  - Create ClusterMarker component
  - Group nearby markers when zoomed out
  - Display count badge on cluster
  - Expand cluster on click with zoom animation
  - _Requirements: 22.3_

- [ ] 17.6 Add hover synchronization
  - Listen for hover events on result cards
  - Highlight corresponding map marker with pulse animation
  - Listen for hover events on markers
  - Highlight corresponding result card with border
  - Use shared state or context for synchronization
  - _Requirements: 22.4_

- [ ] 17.7 Implement smooth zoom with inertia
  - Configure map with gestureHandling: 'greedy'
  - Add custom zoom controls with +/- buttons
  - Implement momentum-based easing for zoom
  - Use requestAnimationFrame for smooth animation
  - _Requirements: 22.5_

- [ ] 17.8 Add map controls
  - Create MapControls component with zoom in/out buttons
  - Add "Reset view" button to return to initial position
  - Add "Toggle satellite" button for map type
  - Style controls with bg-white, rounded-lg, shadow, p-2
  - Position absolute at top-right of map
  - _Requirements: 22.6_

- [ ] 17.9 Create location details popup
  - Create InfoWindow component for marker click
  - Display result image, title, price
  - Add "View Details" button
  - Animate popup with scale-in effect
  - Close on click outside or Escape key
  - _Requirements: 22.7_

- [ ] 17.10 Optimize map performance
  - Implement marker recycling for large datasets
  - Use marker clustering for 50+ results
  - Debounce map move events (300ms)
  - Lazy load map component with React.lazy
  - _Requirements: 11.1, 11.6_

---

## Phase 5: Feedback and Notification Components

### - [ ] 18. Build Toast Notification system

- [ ] 18.1 Create Toast component
  - Create `src/components/premium/feedback/Toast.tsx` file
  - Accept props: message, type (success/warning/error/info), duration, onClose
  - Render container with appropriate icon based on type
  - Style with rounded-lg, shadow-xl, p-4, max-w-md
  - Apply semantic colors based on type
  - _Requirements: 23.1, 23.7_

- [ ] 18.2 Implement toast animations
  - Wrap in motion.div with bounce ease-in entrance
  - Slide in from top or bottom based on position prop
  - Duration: 300ms with spring physics
  - Exit with fade-out animation
  - _Requirements: 23.2_

- [ ] 18.3 Add auto-dismiss functionality
  - Use useEffect with setTimeout for auto-dismiss
  - Success: 4 seconds, Warning/Error: 6 seconds
  - Clear timeout on unmount
  - Animate progress bar showing time remaining
  - Fade out before removal
  - _Requirements: 23.3_

- [ ] 18.4 Create ToastContainer
  - Create `src/components/premium/feedback/ToastContainer.tsx` file
  - Position fixed at top-right or bottom-right
  - Stack multiple toasts vertically with 8px gap
  - Animate position changes when toasts are added/removed
  - Apply z-50 for proper layering
  - _Requirements: 23.5_

- [ ] 18.5 Implement toast manager hook
  - Create `src/hooks/feedback/useToast.ts` hook
  - Manage toast queue with useState
  - Provide showToast function to add toasts
  - Provide removeToast function to dismiss
  - Return toast array and control functions
  - _Requirements: 23.1_

- [ ] 18.6 Add manual dismiss button
  - Render X icon button at top-right of toast
  - Style with text-gray-400, hover:text-gray-600
  - Add whileHover scale: 1.1, whileTap scale: 0.9
  - Implement onClick to call onClose
  - _Requirements: 23.6_

- [ ] 18.7 Add accessibility features
  - Include role="status" or role="alert" based on type
  - Add aria-live="polite" or "assertive"
  - Ensure keyboard dismissible with Escape key
  - Provide screen reader announcement
  - _Requirements: 23.4, 10.4_

- [ ] 18.8 Implement sound and haptic feedback
  - Check if browser supports Audio API
  - Play subtle beep for important notifications
  - Check if device supports vibration
  - Trigger haptic feedback on mobile
  - Make optional via user preferences
  - _Requirements: 23.4_

### - [ ] 19. Build Tooltip and Popover components

- [ ] 19.1 Create Tooltip component using Radix
  - Create `src/components/premium/feedback/Tooltip.tsx` file
  - Use @radix-ui/react-tooltip primitives
  - Accept props: content, children, side, delay
  - Wrap children in Tooltip.Trigger
  - Render content in Tooltip.Content
  - _Requirements: 25.1_

- [ ] 19.2 Style tooltip content
  - Apply bg-gray-900, text-white, rounded-lg, px-3, py-2
  - Add shadow-xl for depth
  - Set max-width to prevent overly wide tooltips
  - Use text-sm for readable size
  - _Requirements: 25.5_

- [ ] 19.3 Add arrow pointer
  - Use Tooltip.Arrow component
  - Style to match tooltip background
  - Position automatically based on side prop
  - _Requirements: 25.3_

- [ ] 19.4 Implement show/hide animations
  - Fade in with 200ms delay on hover
  - Fade out with 150ms duration
  - Use AnimatePresence for exit animations
  - _Requirements: 25.2, 25.4, 25.6_

- [ ] 19.5 Add auto-positioning
  - Use Radix's collision detection
  - Adjust position to stay within viewport
  - Flip to opposite side if needed
  - Add offset for spacing from trigger
  - _Requirements: 25.3_

- [ ] 19.6 Implement dismiss behavior
  - Close on click away
  - Close on scroll
  - Close on Escape key press
  - Animate exit smoothly
  - _Requirements: 25.4_

- [ ] 19.7 Add accessibility attributes
  - Include aria-describedby linking tooltip to trigger
  - Ensure keyboard accessible (focus trigger shows tooltip)
  - Provide sufficient contrast (4.5:1 minimum)
  - _Requirements: 25.7, 10.1_

- [ ] 19.8 Create info icon trigger pattern
  - Create InfoTooltip component wrapping Tooltip
  - Render ⓘ icon as trigger
  - Style icon with text-gray-400, hover:text-gray-600
  - Add cursor-help
  - _Requirements: 25.1_

### - [ ] 20. Build Summary Bar component

- [ ] 20.1 Create SummaryBar component
  - Create `src/components/premium/layout/SummaryBar.tsx` file
  - Accept props: searchParams, resultsCount, onModify
  - Render compact horizontal bar
  - Display destination, dates, travelers summary
  - _Requirements: 24.1_

- [ ] 20.2 Implement sticky positioning
  - Apply sticky, top-0, z-40 positioning
  - Add bg-white with border-b
  - Detect when stuck using IntersectionObserver
  - Add shadow when stuck for depth
  - _Requirements: 24.1, 24.3_

- [ ] 20.3 Add slide-in animation
  - Initially hidden (translateY: -100%)
  - Slide in when user scrolls down
  - Animate with 250ms duration, easeOut
  - Slide out when scrolled to top
  - _Requirements: 24.3_

- [ ] 20.4 Implement inline editing
  - Make each summary field clickable
  - Expand field on click with smooth animation
  - Show input/dropdown for editing
  - Save on blur or Enter key
  - Cancel on Escape key
  - _Requirements: 24.2_

- [ ] 20.5 Display results count
  - Show "X trips found" text
  - Update in real-time as filters change
  - Animate count changes with number transition
  - Style with text-sm, font-medium, text-gray-600
  - _Requirements: 24.6_

- [ ] 20.6 Add "Modify Search" button
  - Render button at right side of bar
  - Style with text-primary-600, hover:text-primary-700
  - Implement onClick to scroll to search form
  - Use smooth scroll behavior
  - _Requirements: 24.5_

- [ ] 20.7 Persist context across navigation
  - Store search params in context or global state
  - Maintain summary bar state during page changes
  - Restore state on page reload (localStorage)
  - _Requirements: 24.4_

- [ ] 20.8 Make responsive for mobile
  - On mobile, show abbreviated summary
  - Use icons instead of full text labels
  - Ensure touch-friendly edit buttons
  - Stack vertically if needed
  - _Requirements: 9.1_

---

## Phase 6: Accessibility and Performance

### - [ ] 21. Implement comprehensive accessibility

- [ ] 21.1 Add ARIA labels and roles
  - Audit all interactive elements for proper roles
  - Add aria-label to icon-only buttons
  - Add aria-describedby for form fields with help text
  - Add aria-invalid for fields with errors
  - Add aria-busy for loading states
  - _Requirements: 10.3_

- [ ] 21.2 Implement focus management
  - Create `src/lib/utils/accessibility.ts` file
  - Implement trapFocus function for modals
  - Ensure logical tab order throughout app
  - Add visible focus indicators (2px outline, 2px offset)
  - Restore focus when closing modals
  - _Requirements: 10.2, 10.7_

- [ ] 21.3 Add screen reader announcements
  - Implement announceToScreenReader utility function
  - Use ARIA live regions for dynamic content
  - Announce page transitions
  - Announce filter/sort changes
  - Announce results count updates
  - _Requirements: 10.4_

- [ ] 21.4 Implement keyboard shortcuts
  - Add keyboard shortcut for search (Cmd/Ctrl + K)
  - Add Escape to close modals/dropdowns
  - Add arrow keys for navigation in lists
  - Add Enter to select items
  - Document shortcuts in help section
  - _Requirements: 6.6, 10.2_

- [ ] 21.5 Verify color contrast
  - Implement getContrastRatio utility function
  - Audit all text/background combinations
  - Ensure minimum 4.5:1 for normal text
  - Ensure minimum 3:1 for large text
  - Fix any failing combinations
  - _Requirements: 10.1_

- [ ] 21.6 Add text alternatives
  - Add alt text to all images
  - Add aria-label to icon buttons
  - Add title attributes to abbreviations
  - Ensure decorative images have alt=""
  - _Requirements: 10.6_

- [ ] 21.7 Support reduced motion
  - Check prefers-reduced-motion in all animated components
  - Disable non-essential animations when set
  - Keep essential feedback animations
  - Test with reduced motion enabled
  - _Requirements: 10.5_

### - [ ] 22. Optimize performance

- [ ] 22.1 Implement code splitting
  - Split routes with React.lazy and Suspense
  - Split large components (Map, DatePicker)
  - Create loading fallbacks for each split
  - Measure bundle size reduction
  - _Requirements: 11.4_

- [ ] 22.2 Optimize images
  - Implement OptimizedImage component with blur placeholder
  - Use WebP format with fallbacks
  - Implement lazy loading with Intersection Observer
  - Add fade-in animation when loaded
  - Compress images to appropriate sizes
  - _Requirements: 11.3_

- [ ] 22.3 Implement animation performance optimizations
  - Create `src/lib/utils/performance.ts` file
  - Implement optimizeAnimation function
  - Use will-change CSS property strategically
  - Use transform and opacity for GPU acceleration
  - Clean up will-change after animations
  - _Requirements: 11.5_

- [ ] 22.4 Add performance monitoring
  - Implement measureFrameRate utility
  - Implement measureInteractionTime utility
  - Log performance metrics in development
  - Set up performance budgets
  - Monitor Core Web Vitals
  - _Requirements: 11.1, 11.2_

- [ ] 22.5 Optimize API calls
  - Implement debouncing for search/autocomplete (300ms)
  - Implement throttling for scroll events (300ms)
  - Cache API responses with React Query
  - Implement request deduplication
  - _Requirements: 11.6_

- [ ] 22.6 Implement caching strategy
  - Cache destination images in browser storage
  - Set 24-hour expiration for cached data
  - Implement cache invalidation on updates
  - Use service worker for offline support
  - _Requirements: 11.7_

---

## Phase 7: Integration and Polish

### - [ ] 23. Integrate all components into pages

- [ ] 23.1 Create SearchPage
  - Create `src/pages/SearchPage.tsx` file
  - Integrate UnifiedSearchBar at top
  - Add FilterChipBar and SortDropdown
  - Display ResultGrid with cards
  - Add MapSplitView toggle
  - Implement search functionality
  - _Requirements: 13.1, 18.1, 19.1, 22.1_

- [ ] 23.2 Create WizardPage
  - Create `src/pages/WizardPage.tsx` file
  - Integrate ProgressStepper at top
  - Wrap steps in StepTransition
  - Implement step navigation logic
  - Add DestinationStep, DatesTravelersStep, PreferencesStep, ReviewStep
  - Connect to AgentProgress on submission
  - _Requirements: 20.1, 6.3, 2.1-2.4, 5.1_

- [ ] 23.3 Create PaymentPage
  - Create `src/pages/PaymentPage.tsx` file
  - Integrate ProgressStepper showing payment step
  - Add SecurePaymentForm
  - Display booking summary sidebar
  - Implement payment submission
  - Show success animation on completion
  - _Requirements: 20.1, 21.1_

- [ ] 23.4 Update existing pages with premium components
  - Replace basic search with UnifiedSearchBar on homepage
  - Add SummaryBar to results page
  - Add Toast notifications throughout app
  - Add Tooltips to complex features
  - Ensure consistent styling
  - _Requirements: 13.1, 24.1, 23.1, 25.1_

### - [ ] 24. Add error handling and edge cases

- [ ] 24.1 Create PremiumErrorBoundary
  - Create `src/components/premium/ErrorBoundary.tsx` file
  - Catch and display errors gracefully
  - Show user-friendly error message
  - Provide "Refresh" button
  - Log errors to console/service
  - _Requirements: Error handling from design_

- [ ] 24.2 Handle API errors
  - Create APIError class with statusCode and code
  - Implement handleAPIError utility function
  - Map status codes to user-friendly messages
  - Display errors in Toast notifications
  - Provide retry functionality
  - _Requirements: Error handling from design_

- [ ] 24.3 Handle loading states
  - Create skeleton loaders for all components
  - Use shimmer animation for loading
  - Show loading spinners for async actions
  - Disable interactions during loading
  - Provide loading progress where applicable
  - _Requirements: 8.5_

- [ ] 24.4 Handle empty states
  - Create EmptyState component
  - Show when no results found
  - Provide helpful suggestions
  - Add illustration or icon
  - Offer action to modify search
  - _Requirements: 14.9_

- [ ] 24.5 Handle network errors
  - Detect offline state
  - Show offline indicator
  - Queue actions for when online
  - Retry failed requests automatically
  - Notify user when back online
  - _Requirements: Error handling from design_

### - [ ] 25. Final polish and refinements

- [ ] 25.1 Audit all animations
  - Test all animations at 60fps
  - Verify no frame drops during interactions
  - Check reduced motion support
  - Ensure consistent timing across components
  - Fix any janky animations
  - _Requirements: 11.1_

- [ ] 25.2 Audit accessibility
  - Run Lighthouse accessibility audit
  - Test with screen reader (NVDA/JAWS)
  - Test keyboard navigation throughout
  - Verify all ARIA labels
  - Fix any accessibility issues
  - _Requirements: 10.1-10.7_

- [ ] 25.3 Test responsive design
  - Test on mobile devices (iOS, Android)
  - Test on tablets
  - Test on desktop (various screen sizes)
  - Verify touch interactions work correctly
  - Fix any layout issues
  - _Requirements: 9.1-9.6_

- [ ] 25.4 Cross-browser testing
  - Test on Chrome (latest)
  - Test on Firefox (latest)
  - Test on Safari (latest)
  - Test on Edge (latest)
  - Fix any browser-specific issues
  - _Requirements: Success criteria from design_

- [ ] 25.5 Performance optimization pass
  - Run Lighthouse performance audit
  - Optimize bundle size (target < 200KB gzipped)
  - Optimize images (WebP, lazy loading)
  - Minimize render-blocking resources
  - Achieve performance score > 90
  - _Requirements: 11.1-11.7_

- [ ] 25.6 Visual QA pass
  - Review all components for visual consistency
  - Check spacing and alignment
  - Verify color usage matches design tokens
  - Check typography consistency
  - Fix any visual bugs
  - _Requirements: Success criteria from design_

- [ ] 25.7 Create component documentation
  - Document all premium components in Storybook
  - Add usage examples for each component
  - Document props and variants
  - Add accessibility notes
  - Include do's and don'ts
  - _Requirements: 12.6_

---

## Completion Checklist

- [ ] All Phase 1 tasks completed (Foundation)
- [ ] All Phase 2 tasks completed (Search Components)
- [ ] All Phase 3 tasks completed (Filters & Results)
- [ ] All Phase 4 tasks completed (Wizard & Payment)
- [ ] All Phase 5 tasks completed (Feedback & Notifications)
- [ ] All Phase 6 tasks completed (Accessibility & Performance)
- [ ] All Phase 7 tasks completed (Integration & Polish)
- [ ] Lighthouse scores: Performance > 90, Accessibility = 100
- [ ] All animations running at 60fps
- [ ] All interactions responding < 100ms
- [ ] Cross-browser compatibility verified
- [ ] Mobile responsiveness verified
- [ ] Documentation complete

---

**Total Estimated Time:** 8-10 weeks for complete implementation

**Note:** This is a comprehensive, production-ready implementation plan. Each task is atomic and independently executable. All tasks are required (no optional tasks) to achieve the premium UI experience specified in the requirements.


---

## Phase 8: Missing Core Components (CRITICAL)

### - [ ] 26. Build Popular Destination Cards (Requirement 2.2-2.3)

- [ ] 26.1 Create PopularDestinations component
  - Create `src/components/premium/search/PopularDestinations.tsx` file
  - Accept props: destinations array, onSelect callback
  - Render grid container with grid-cols-2 md:grid-cols-3 lg:grid-cols-4
  - Apply gap-4 for spacing between cards
  - _Requirements: 2.2_

- [ ] 26.2 Create DestinationCard sub-component
  - Create DestinationCard function component
  - Accept props: destination (name, country, image, category), onClick
  - Wrap in motion.div for animations
  - Set minimum image size 400x300px with object-cover
  - Apply rounded-xl, overflow-hidden, cursor-pointer
  - _Requirements: 2.2_

- [ ] 26.3 Implement card hover animations
  - Add whileHover with scale: 1.02
  - Animate shadow from md to lg
  - Animate image brightness from 100% to 110%
  - Set transition duration to 200ms
  - Use easeOut timing function
  - _Requirements: 2.3_

- [ ] 26.4 Add category icons to cards
  - Display icon based on category: 🏖️ beach, 🏙️ city, ⛰️ mountain, 🏛️ cultural
  - Position icon absolute at top-left with bg-white/90 backdrop
  - Style with p-2, rounded-full, shadow-md
  - _Requirements: 2.2, 2.6_

- [ ] 26.5 Display destination information
  - Render destination name with text-lg, font-semibold, text-white
  - Render country with text-sm, text-white/90
  - Position text absolute at bottom with gradient overlay
  - Apply p-4 padding
  - _Requirements: 2.2_

- [ ] 26.6 Integrate into DestinationStep
  - Add PopularDestinations below autocomplete input
  - Show when input is empty or on initial load
  - Hide when user starts typing
  - Pass onSelect to populate input field
  - _Requirements: 2.2_

### - [ ] 27. Build PreferencesStep Component (Requirement 4)

- [ ] 27.1 Create PreferencesStep container
  - Create `src/components/premium/wizard/PreferencesStep.tsx` file
  - Accept props: preferences, onChange, onNext, onBack
  - Render container with max-w-4xl, mx-auto, p-6
  - Organize sections vertically with spacing
  - _Requirements: 4.1_

- [ ] 27.2 Create PreferenceCard sub-component
  - Create PreferenceCard function component
  - Accept props: title, description, image, icon, isSelected, onClick
  - Wrap in motion.div with whileHover animations
  - Set minimum image size 200x150px
  - Apply rounded-xl, border-2, cursor-pointer
  - _Requirements: 4.1_

- [ ] 27.3 Implement preference card hover effect
  - Add whileHover with translateY: -4px
  - Animate shadow from md to lg
  - Add border highlight with primary color
  - Set transition duration to 200ms
  - _Requirements: 4.2_

- [ ] 27.4 Build Budget section
  - Add section header "Budget" with text-xl, font-semibold
  - Integrate BudgetSlider component (already built in Task 10)
  - Display budget range markers ($, $$, $$$, $$$$)
  - Show current selection below slider
  - _Requirements: 4.3_

- [ ] 27.5 Build Activities section
  - Add section header "Activities" with text-xl, font-semibold
  - Create grid of activity cards: Adventure, Culture, Relaxation, Food, Nature, Shopping, Nightlife
  - Each card has icon, label, and selectable state
  - Support multi-select with checkmark overlay
  - Apply grid-cols-2 md:grid-cols-3 lg:grid-cols-4
  - _Requirements: 4.4_

- [ ] 27.6 Implement activity card selection animation
  - Add onClick to toggle selection state
  - Animate border color change to primary
  - Add background tint (bg-primary-50) when selected
  - Fade in checkmark icon with scale animation
  - Duration: 250ms with easeOut
  - _Requirements: 4.5_

- [ ] 27.7 Build Pace section
  - Add section header "Travel Pace" with text-xl, font-semibold
  - Create 3 radio cards: Relaxed (2-3 activities/day), Moderate (4-5 activities/day), Packed (6+ activities/day)
  - Display visual indicators (icons or illustrations)
  - Apply single-select behavior
  - Style selected card with primary border and background
  - _Requirements: 4.6_

- [ ] 27.8 Build Dietary Preferences section
  - Add section header "Dietary Preferences" with text-xl, font-semibold
  - Create chips for: Vegetarian, Vegan, Halal, Kosher, Gluten-Free, No Restrictions
  - Each chip has icon and label
  - Support multi-select with toggle animation
  - Apply flex-wrap layout
  - _Requirements: 4.7_

- [ ] 27.9 Implement dietary chip toggle animation
  - Add onClick to toggle selection
  - Animate background color change
  - Animate border color change
  - Add checkmark icon with fade-in
  - Duration: 200ms
  - _Requirements: 4.7_

- [ ] 27.10 Add navigation buttons
  - Render "Back" button at bottom-left
  - Render "Next" button at bottom-right
  - Style with consistent button design
  - Implement onClick handlers
  - Disable Next if required fields empty
  - _Requirements: 6.4_

### - [ ] 28. Build ReviewStep Component (Requirement 5)

- [ ] 28.1 Create ReviewStep container
  - Create `src/components/premium/wizard/ReviewStep.tsx` file
  - Accept props: tripData, onEdit, onSubmit, onBack
  - Render container with max-w-4xl, mx-auto, p-6
  - Organize summary cards in grid
  - _Requirements: 5.1_

- [ ] 28.2 Create SummaryCard sub-component
  - Create SummaryCard function component
  - Accept props: title, icon, content, onEdit
  - Wrap in motion.div with hover effects
  - Apply bg-white, rounded-xl, border, p-6
  - Display edit button on hover
  - _Requirements: 5.1_

- [ ] 28.3 Implement edit button hover animation
  - Hide edit button by default (opacity: 0)
  - Fade in on card hover within 150ms
  - Position absolute at top-right
  - Style with text-primary-600, hover:text-primary-700
  - Add Edit icon from lucide-react
  - _Requirements: 5.2_

- [ ] 28.4 Build Destination summary card
  - Display destination name with MapPin icon
  - Show "From → To" if applicable
  - Add edit button that calls onEdit('destination')
  - _Requirements: 5.1_

- [ ] 28.5 Build Dates summary card
  - Display date range with Calendar icon
  - Format as "Mon, Jan 15 - Fri, Jan 20, 2024"
  - Show trip duration (e.g., "5 nights, 6 days")
  - Add edit button that calls onEdit('dates')
  - _Requirements: 5.1_

- [ ] 28.6 Build Travelers summary card
  - Display traveler breakdown with Users icon
  - Format as "2 adults, 1 child"
  - Add edit button that calls onEdit('travelers')
  - _Requirements: 5.1_

- [ ] 28.7 Build Preferences summary card
  - Display selected preferences with Sliders icon
  - Show budget range, activities, pace, dietary
  - Format as bullet list
  - Add edit button that calls onEdit('preferences')
  - _Requirements: 5.1_

- [ ] 28.8 Implement edit navigation
  - When edit button clicked, call onEdit with step name
  - Navigate back to specific step
  - Preserve all entered data
  - Smooth page transition animation
  - _Requirements: 5.3_

- [ ] 28.9 Build estimated cost section
  - Create separate card for cost estimate
  - Display price range with DollarSign icon
  - Show confidence indicator (e.g., "Estimated")
  - Add tooltip with calculation factors
  - Style with larger text for emphasis
  - _Requirements: 5.4_

- [ ] 28.10 Build trip highlights section
  - Display 3-5 key features based on preferences
  - Each highlight has icon and brief description
  - Use Sparkles icon for section header
  - Apply grid-cols-1 md:grid-cols-2 layout
  - _Requirements: 5.5_

- [ ] 28.11 Create "Generate Itinerary" button
  - Render prominent button at bottom
  - Apply w-full, py-4, text-lg, font-semibold
  - Use gradient background (bg-gradient-to-r from-primary-500 to-primary-600)
  - Add Sparkles or Wand icon
  - Implement whileHover lift effect (translateY: -2px, shadow increase)
  - _Requirements: 5.6_

- [ ] 28.12 Add loading state to submit button
  - Show loading spinner when clicked
  - Change text to "Generating your itinerary..."
  - Disable button during loading
  - Prevent double submission
  - _Requirements: 5.6_

- [ ] 28.13 Add navigation buttons
  - Render "Back" button at bottom-left
  - Style consistently with other steps
  - Implement onClick to go to previous step
  - _Requirements: 6.4_

### - [ ] 29. Build AgentProgress Component (Requirement 7)

- [ ] 29.1 Create AgentProgress container
  - Create `src/components/premium/wizard/AgentProgress.tsx` file
  - Accept props: status, progress, currentMilestone, destinationFacts
  - Render full-screen overlay with backdrop
  - Center content vertically and horizontally
  - Apply bg-white, rounded-2xl, shadow-2xl, p-8, max-w-2xl
  - _Requirements: 7.1_

- [ ] 29.2 Create animated illustration
  - Use Lottie or custom SVG animation
  - Show travel-themed illustration (plane, map, compass)
  - Loop animation smoothly at 60fps
  - Size: 200x200px minimum
  - Position at top of card
  - _Requirements: 7.1_

- [ ] 29.3 Build progress milestones list
  - Define milestones: "Analyzing preferences", "Finding attractions", "Optimizing route", "Creating schedule"
  - Render as vertical list with icons
  - Show current milestone highlighted
  - Display checkmark for completed milestones
  - Apply spacing between items
  - _Requirements: 7.2_

- [ ] 29.4 Implement milestone completion animation
  - When milestone completes, animate checkmark
  - Use SVG stroke-dasharray for draw-in effect
  - Transition color from gray to green
  - Add scale animation (0.8 → 1.0)
  - Duration: 400ms with easeOut
  - _Requirements: 7.3_

- [ ] 29.5 Build destination facts carousel
  - Display interesting fact about destination
  - Rotate facts every 5 seconds
  - Use AnimatePresence for transitions
  - Fade out old fact, fade in new fact
  - Style with text-center, text-gray-600, italic
  - _Requirements: 7.4_

- [ ] 29.6 Implement time remaining countdown
  - Display "About X seconds remaining"
  - Update every second
  - Format in human-readable way (30s, 1m, 2m)
  - Style with text-sm, text-gray-500
  - Position below milestones
  - _Requirements: 7.5_

- [ ] 29.7 Create success animation
  - When generation completes, show confetti effect
  - Use canvas-confetti library or custom implementation
  - Display large checkmark with scale-in animation
  - Show success message "Your itinerary is ready!"
  - Duration: 800ms
  - _Requirements: 7.6_

- [ ] 29.8 Implement transition to itinerary
  - Fade out AgentProgress component over 500ms
  - Fade in itinerary view over 500ms
  - Use stagger effect for itinerary items
  - Smooth, seamless transition
  - _Requirements: 7.7_

- [ ] 29.9 Add progress bar
  - Display linear progress bar at top of card
  - Animate width based on progress percentage
  - Use gradient fill (primary colors)
  - Smooth animation with spring physics
  - _Requirements: 7.2_

- [ ] 29.10 Handle error states
  - If generation fails, show error message
  - Display "Retry" button
  - Provide helpful error description
  - Maintain same card layout
  - _Requirements: Error handling_

### - [ ] 30. Build Wizard Navigation Buttons (Requirement 6.4-6.5)

- [ ] 30.1 Create WizardNavigation component
  - Create `src/components/premium/wizard/WizardNavigation.tsx` file
  - Accept props: currentStep, totalSteps, onBack, onNext, canGoNext, isLoading
  - Render flex container with justify-between
  - Apply sticky positioning at bottom
  - Add bg-white, border-t, p-4, shadow-lg
  - _Requirements: 6.4_

- [ ] 30.2 Implement Back button
  - Render button with ChevronLeft icon and "Back" text
  - Style with px-6, py-3, border, rounded-lg
  - Apply hover:bg-gray-50 transition
  - Disable on first step
  - Implement onClick to call onBack
  - _Requirements: 6.4_

- [ ] 30.3 Implement Next button
  - Render button with "Next" text and ChevronRight icon
  - Style with px-6, py-3, bg-primary-500, text-white, rounded-lg
  - Apply hover:bg-primary-600 transition
  - Add whileHover scale: 1.02, whileTap scale: 0.98
  - Disable when canGoNext is false
  - _Requirements: 6.4_

- [ ] 30.4 Add loading state to Next button
  - Show loading spinner when isLoading is true
  - Change text to "Processing..."
  - Disable button during loading
  - Maintain button size (no layout shift)
  - _Requirements: 6.4_

- [ ] 30.5 Implement field validation shake
  - When Next clicked with invalid fields, identify invalid fields
  - Apply shake animation to invalid fields
  - Use keyframes: translateX(-10px) → translateX(10px) → translateX(0)
  - Duration: 400ms
  - Display inline error messages below fields
  - _Requirements: 6.5_

- [ ] 30.6 Add keyboard shortcuts
  - Listen for Enter key to trigger Next
  - Listen for Escape key to trigger Back
  - Only active when wizard is focused
  - Prevent default form submission
  - _Requirements: 6.6_

- [ ] 30.7 Integrate into wizard steps
  - Add WizardNavigation to bottom of each step
  - Pass appropriate props from parent wizard
  - Ensure consistent positioning across steps
  - Handle step-specific validation logic
  - _Requirements: 6.4_

---

## Phase 9: Mobile-Specific Features

### - [ ] 31. Implement Mobile Swipe Gestures (Requirement 9.3)

- [ ] 31.1 Create useSwipeGesture hook
  - Create `src/hooks/interactions/useSwipeGesture.ts` file
  - Use touch events (touchstart, touchmove, touchend)
  - Calculate swipe direction and distance
  - Return onSwipeLeft, onSwipeRight callbacks
  - Set minimum swipe distance threshold (50px)
  - _Requirements: 9.3_

- [ ] 31.2 Integrate swipe in wizard
  - Apply useSwipeGesture hook to wizard container
  - Swipe right → go to previous step
  - Swipe left → go to next step
  - Add visual feedback during swipe (drag indicator)
  - Smooth transition animation
  - _Requirements: 9.3_

- [ ] 31.3 Add swipe indicators
  - Show subtle arrows at screen edges
  - Fade in on touch start
  - Fade out on touch end
  - Style with opacity-50, text-gray-400
  - _Requirements: 9.3_

### - [ ] 32. Implement Bottom Sheet Pattern (Requirement 9.5)

- [ ] 32.1 Create BottomSheet component
  - Create `src/components/premium/mobile/BottomSheet.tsx` file
  - Accept props: isOpen, onClose, children, snapPoints
  - Render fixed positioned container at bottom
  - Apply rounded-t-2xl, bg-white, shadow-2xl
  - Support drag-to-dismiss gesture
  - _Requirements: 9.5_

- [ ] 32.2 Implement drag gesture
  - Use Framer Motion drag functionality
  - Set dragConstraints for vertical drag only
  - Calculate drag distance to determine dismiss
  - Dismiss if dragged down > 100px
  - Spring animation back if not dismissed
  - _Requirements: 9.5_

- [ ] 32.3 Add backdrop overlay
  - Render semi-transparent backdrop behind sheet
  - Apply bg-black/50
  - Fade in/out with sheet
  - Click backdrop to dismiss
  - _Requirements: 9.5_

- [ ] 32.4 Implement snap points
  - Support multiple snap points (e.g., 50%, 75%, 100%)
  - Snap to nearest point on drag end
  - Smooth spring animation to snap point
  - _Requirements: 9.5_

- [ ] 32.5 Use for mobile modals
  - Replace desktop modals with BottomSheet on mobile
  - Apply to TravelerSelector, DatePicker, Filters
  - Detect mobile viewport (<768px)
  - Maintain same functionality
  - _Requirements: 9.5_

---

## Final Verification Checklist

- [ ] All 25 requirements have corresponding tasks
- [ ] All acceptance criteria are covered
- [ ] No assumptions made - all tasks based on requirements
- [ ] All components integrated into pages
- [ ] All animations specified with exact timings
- [ ] All accessibility requirements covered
- [ ] All performance requirements covered
- [ ] All mobile requirements covered
- [ ] Error handling for all components
- [ ] Loading states for all async operations

---

**Updated Total Estimated Time:** 10-12 weeks for complete implementation

**Note:** This updated plan now includes ALL missing components identified in the verification. Every requirement from requirements.md is now covered with specific, actionable tasks.
