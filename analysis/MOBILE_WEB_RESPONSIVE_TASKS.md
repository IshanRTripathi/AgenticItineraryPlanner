# Mobile/Web Responsive Design - Implementation Tasks

**Reference:** `analysis/MOBILE_WEB_UI_FLOW_ANALYSIS.md`  
**Priority:** P0 (Critical) - P3 (Nice to have)  
**Estimated Total Effort:** 40-50 hours

---

## Phase 1: Critical Navigation & Layout Fixes (P0)

### Task 1.1: Unify Navigation Structure
**Priority:** P0  
**Effort:** 4 hours  
**Files to Modify:**
- `frontend-redesign/src/components/layout/Header.tsx`
- `frontend-redesign/src/components/layout/BottomNav.tsx`
- `frontend-redesign/src/components/layout/MobileMenu.tsx`

**Current State:**
```typescript
// Header.tsx - Desktop nav
<nav className="hidden md:flex items-center gap-6">
  <a href="/">Home</a>
  <a href="/dashboard">My Trips</a>
  <a href="#flights">Flights</a>
  <a href="#hotels">Hotels</a>
</nav>

// BottomNav.tsx - Mobile nav
const NAV_ITEMS = [
  { id: 'home', label: 'Home', path: '/' },
  { id: 'search', label: 'Search', path: '/search-results' },
  { id: 'create', label: 'Plan Trip', path: '/ai-planner' },
  { id: 'trips', label: 'My Trips', path: '/dashboard' },
  { id: 'profile', label: 'Profile', path: '/profile' },
];

// MobileMenu.tsx - Slide-out menu
const menuItems = [
  { icon: Home, label: 'Home', href: '/' },
  { icon: Compass, label: 'My Trips', href: '/dashboard' },
  { icon: Calendar, label: 'Plan Trip', href: '/ai-planner' },
  { icon: User, label: 'Profile', href: '/profile' },
];
```

**Required Changes:**

1. **Update Header.tsx** - Add "Plan Trip" to desktop nav:
```typescript
<nav className="hidden md:flex items-center gap-6">
  <a href="/">Home</a>
  <a href="/ai-planner">Plan Trip</a>
  <a href="/dashboard">My Trips</a>
  <a href="/search-results">Search</a>
</nav>
```

2. **Update MobileMenu.tsx** - Add Flights/Hotels:
```typescript
const menuItems = [
  { icon: Home, label: 'Home', href: '/' },
  { icon: Plane, label: 'Flights', href: '/search-results?type=flight' },
  { icon: Hotel, label: 'Hotels', href: '/search-results?type=hotel' },
  { icon: Calendar, label: 'Plan Trip', href: '/ai-planner' },
  { icon: Compass, label: 'My Trips', href: '/dashboard' },
  { icon: User, label: 'Profile', href: '/profile' },
];
```

3. **Update BottomNav.tsx** - Simplify to 5 core items:
```typescript
const NAV_ITEMS = [
  { id: 'home', label: 'Home', icon: Home, path: '/' },
  { id: 'search', label: 'Search', icon: Search, path: '/search-results' },
  { id: 'create', label: 'Plan', icon: PlusCircle, path: '/ai-planner', highlight: true },
  { id: 'trips', label: 'Trips', icon: Calendar, path: '/dashboard' },
  { id: 'profile', label: 'Profile', icon: User, path: '/profile' },
];
```

**Acceptance Criteria:**
- [ ] Desktop and mobile have same navigation items
- [ ] All navigation paths work on both devices
- [ ] Mobile bottom nav has max 5 items
- [ ] Visual consistency across all nav components

---

### Task 1.2: Fix Touch Target Sizes
**Priority:** P0  
**Effort:** 3 hours  
**Files to Modify:**
- `frontend-redesign/src/components/ui/button.tsx`
- `frontend-redesign/src/components/layout/BottomNav.tsx`
- `frontend-redesign/src/components/trip/TripSidebar.tsx`
- `frontend-redesign/tailwind.config.ts`

**Current State:**
```typescript
// button.tsx - Default sizes may be too small
const buttonVariants = cva(
  "inline-flex items-center justify-center rounded-md text-sm font-medium...",
  {
    variants: {
      size: {
        default: "h-10 px-4 py-2",
        sm: "h-9 rounded-md px-3",
        lg: "h-11 rounded-md px-8",
      }
    }
  }
)
```

**Required Changes:**

1. **Update button.tsx** - Ensure minimum 44px height on mobile:
```typescript
const buttonVariants = cva(
  "inline-flex items-center justify-center rounded-md text-sm font-medium...",
  {
    variants: {
      size: {
        default: "h-11 md:h-10 px-4 py-2", // 44px on mobile, 40px on desktop
        sm: "h-11 md:h-9 rounded-md px-3",  // Never smaller than 44px on mobile
        lg: "h-12 md:h-11 rounded-md px-8",
        icon: "h-11 w-11 md:h-10 md:w-10", // Square touch targets
      }
    }
  }
)
```

2. **Update BottomNav.tsx** - Increase touch area:
```typescript
<button
  className={cn(
    'flex flex-col items-center justify-center gap-1',
    'px-4 py-2 rounded-lg', // Increased padding
    'min-w-[64px] min-h-[56px]', // Minimum touch target
    'transition-all duration-200',
    'active:scale-95'
  )}
>
```

3. **Add to tailwind.config.ts** - Touch target utilities:
```typescript
extend: {
  minHeight: {
    'touch': '44px',
    'touch-lg': '48px',
  },
  minWidth: {
    'touch': '44px',
    'touch-lg': '48px',
  }
}
```

**Acceptance Criteria:**
- [ ] All interactive elements ≥ 44x44px on mobile
- [ ] Buttons have adequate spacing (8px minimum)
- [ ] Touch targets don't overlap
- [ ] Tested on actual mobile devices

---

### Task 1.3: Responsive Trip Detail Layout
**Priority:** P0  
**Effort:** 6 hours  
**Files to Modify:**
- `frontend-redesign/src/pages/TripDetailPage.tsx`
- `frontend-redesign/src/components/trip/TripSidebar.tsx`
- `frontend-redesign/src/components/ui/tabs.tsx`

**Current State:**
```typescript
// TripDetailPage.tsx - Same layout for all devices
<div className="min-h-screen bg-background">
  <div className="container py-8">
    <div className="grid grid-cols-1 lg:grid-cols-[300px_1fr] gap-8">
      <TripSidebar ... />
      <main>{renderTabContent()}</main>
    </div>
  </div>
</div>
```

**Required Changes:**

1. **Update TripDetailPage.tsx** - Mobile-first layout:
```typescript
<div className="min-h-screen bg-background pb-20 md:pb-8">
  {/* Mobile: Tabs at top */}
  <div className="md:hidden sticky top-0 z-10 bg-background border-b">
    <div className="flex overflow-x-auto px-4 py-2 gap-2">
      {TABS.map(tab => (
        <button
          key={tab.id}
          onClick={() => handleTabChange(tab.id)}
          className={cn(
            'px-4 py-2 rounded-full whitespace-nowrap',
            'min-w-[80px] min-h-[44px]', // Touch target
            activeTab === tab.id && 'bg-primary text-primary-foreground'
          )}
        >
          {tab.label}
        </button>
      ))}
    </div>
  </div>

  <div className="container py-4 md:py-8">
    <div className="grid grid-cols-1 lg:grid-cols-[300px_1fr] gap-4 md:gap-8">
      {/* Desktop: Sidebar */}
      <div className="hidden lg:block">
        <TripSidebar ... />
      </div>
      
      {/* Content */}
      <main className="min-h-[calc(100vh-200px)]">
        {renderTabContent()}
      </main>
    </div>
  </div>
</div>
```

2. **Create MobileTabs component** - `frontend-redesign/src/components/trip/MobileTabs.tsx`:
```typescript
export function MobileTabs({ tabs, activeTab, onTabChange }) {
  const scrollRef = useRef<HTMLDivElement>(null);
  
  return (
    <div className="md:hidden sticky top-16 z-10 bg-background border-b">
      <div 
        ref={scrollRef}
        className="flex overflow-x-auto scrollbar-hide px-4 py-2 gap-2"
      >
        {tabs.map(tab => (
          <button
            key={tab.id}
            onClick={() => onTabChange(tab.id)}
            className={cn(
              'flex items-center gap-2 px-4 py-2.5',
              'rounded-full whitespace-nowrap',
              'min-h-[44px] transition-all',
              activeTab === tab.id 
                ? 'bg-primary text-primary-foreground shadow-sm' 
                : 'bg-muted text-muted-foreground'
            )}
          >
            <tab.icon className="w-4 h-4" />
            <span className="text-sm font-medium">{tab.label}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
```

**Acceptance Criteria:**
- [ ] Mobile shows horizontal scrollable tabs at top
- [ ] Desktop shows sidebar navigation
- [ ] Tabs are swipeable on mobile
- [ ] Active tab is clearly indicated
- [ ] Content area adjusts for mobile bottom nav

---

## Phase 2: Component Optimization (P1)

### Task 2.1: Mobile-Optimized Trip Wizard
**Priority:** P1  
**Effort:** 8 hours  
**Files to Modify:**
- `frontend-redesign/src/components/ai-planner/PremiumTripWizard.tsx`
- `frontend-redesign/src/components/ai-planner/WizardProgress.tsx`
- `frontend-redesign/src/components/ai-planner/steps/*.tsx`

**Current State:**
```typescript
// PremiumTripWizard.tsx - Same for all devices
<Card className="w-full max-w-4xl mx-auto">
  <CardHeader>
    <WizardProgress currentStep={currentStep} totalSteps={STEPS.length} />
  </CardHeader>
  <CardContent>
    <AnimatePresence mode="wait">
      {renderStep()}
    </AnimatePresence>
  </CardContent>
</Card>
```

**Required Changes:**

1. **Update PremiumTripWizard.tsx** - Add swipe gestures:
```typescript
import { useSwipeable } from 'react-swipeable';

export function PremiumTripWizard() {
  const handlers = useSwipeable({
    onSwipedLeft: () => currentStep < STEPS.length && handleNext(),
    onSwipedRight: () => currentStep > 1 && handleBack(),
    preventScrollOnSwipe: true,
    trackMouse: false, // Only on touch devices
  });

  return (
    <div {...handlers} className="w-full max-w-4xl mx-auto px-4 md:px-0">
      {/* Mobile: Compact progress */}
      <div className="md:hidden mb-4">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm text-muted-foreground">
            Step {currentStep} of {STEPS.length}
          </span>
          <span className="text-sm font-medium">
            {Math.round((currentStep / STEPS.length) * 100)}%
          </span>
        </div>
        <div className="h-2 bg-muted rounded-full overflow-hidden">
          <div 
            className="h-full bg-primary transition-all duration-300"
            style={{ width: `${(currentStep / STEPS.length) * 100}%` }}
          />
        </div>
      </div>

      {/* Desktop: Full progress */}
      <div className="hidden md:block mb-8">
        <WizardProgress currentStep={currentStep} totalSteps={STEPS.length} />
      </div>

      <Card>
        <CardContent className="p-4 md:p-6">
          <AnimatePresence mode="wait">
            {renderStep()}
          </AnimatePresence>
        </CardContent>
      </Card>
    </div>
  );
}
```

2. **Update all step components** - Mobile-optimized inputs:
```typescript
// Example: PremiumDestinationStep.tsx
<div className="space-y-4 md:space-y-6">
  <LocationAutocomplete
    value={destination}
    onChange={setDestination}
    placeholder="Where do you want to go?"
    className="h-12 md:h-11 text-base" // Larger on mobile
    inputMode="search" // Mobile keyboard optimization
  />
  
  <LocationAutocomplete
    value={origin}
    onChange={setOrigin}
    placeholder="Where are you starting from?"
    className="h-12 md:h-11 text-base"
    inputMode="search"
  />
</div>
```

**Acceptance Criteria:**
- [ ] Swipe left/right to navigate steps on mobile
- [ ] Larger touch targets for form inputs
- [ ] Mobile-optimized keyboard types
- [ ] Compact progress indicator on mobile
- [ ] Smooth animations between steps

---

### Task 2.2: Convert Modals to Bottom Sheets
**Priority:** P1  
**Effort:** 6 hours  
**Files to Create:**
- `frontend-redesign/src/components/ui/bottom-sheet.tsx`

**Files to Modify:**
- `frontend-redesign/src/components/booking/BookingModal.tsx`
- `frontend-redesign/src/components/booking/PremiumBookingModal.tsx`
- `frontend-redesign/src/components/booking/ProviderSelectionModal.tsx`

**Required Changes:**

1. **Create bottom-sheet.tsx** - New component:
```typescript
import { Dialog, DialogContent } from '@/components/ui/dialog';
import { cn } from '@/lib/utils';
import { useMediaQuery } from '@/hooks/useMediaQuery';

interface BottomSheetProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  children: React.ReactNode;
  title?: string;
}

export function BottomSheet({ open, onOpenChange, children, title }: BottomSheetProps) {
  const isMobile = useMediaQuery('(max-width: 768px)');

  if (!isMobile) {
    // Desktop: Regular dialog
    return (
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="max-w-2xl">
          {title && <h2 className="text-xl font-bold mb-4">{title}</h2>}
          {children}
        </DialogContent>
      </Dialog>
    );
  }

  // Mobile: Bottom sheet
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent 
        className={cn(
          'fixed bottom-0 left-0 right-0',
          'rounded-t-2xl border-t',
          'max-h-[90vh] overflow-y-auto',
          'data-[state=open]:slide-in-from-bottom',
          'data-[state=closed]:slide-out-to-bottom'
        )}
      >
        {/* Drag handle */}
        <div className="flex justify-center py-2">
          <div className="w-12 h-1 bg-muted r
ounded-full" />
        </div>
        
        {title && (
          <div className="px-4 pb-4 border-b">
            <h2 className="text-lg font-bold">{title}</h2>
          </div>
        )}
        
        <div className="p-4">
          {children}
        </div>
      </DialogContent>
    </Dialog>
  );
}
```

2. **Update BookingModal.tsx** - Use BottomSheet:
```typescript
import { BottomSheet } from '@/components/ui/bottom-sheet';

export function BookingModal({ isOpen, onClose, type, name }: BookingModalProps) {
  return (
    <BottomSheet 
      open={isOpen} 
      onOpenChange={onClose}
      title={`Book ${type === 'flight' ? 'Flight' : type === 'hotel' ? 'Hotel' : 'Activity'}`}
    >
      {/* Existing modal content */}
    </BottomSheet>
  );
}
```

**Acceptance Criteria:**
- [ ] Modals appear as bottom sheets on mobile
- [ ] Swipe down to dismiss on mobile
- [ ] Regular dialogs on desktop
- [ ] Smooth animations
- [ ] Drag handle visible on mobile

---

### Task 2.3: Responsive Card Components
**Priority:** P1  
**Effort:** 4 hours  
**Files to Modify:**
- `frontend-redesign/src/components/dashboard/TripCard.tsx`
- `frontend-redesign/src/components/booking/BookingCard.tsx`
- `frontend-redesign/src/components/trip/DayCard.tsx`

**Current State:**
```typescript
// TripCard.tsx - Same size for all devices
<Card className="overflow-hidden hover:shadow-lg transition-shadow">
  <div className="aspect-video relative">
    <img src={imageUrl} alt={destination} className="w-full h-full object-cover" />
  </div>
  <CardContent className="p-4">
    {/* Content */}
  </CardContent>
</Card>
```

**Required Changes:**

1. **Update TripCard.tsx** - Mobile-optimized layout:
```typescript
<Card className={cn(
  'overflow-hidden transition-shadow',
  'hover:shadow-lg', // Desktop only
  'active:scale-[0.98] md:active:scale-100' // Mobile press feedback
)}>
  {/* Mobile: Horizontal layout */}
  <div className="flex md:block">
    <div className="w-32 md:w-full aspect-square md:aspect-video relative flex-shrink-0">
      <img 
        src={imageUrl} 
        alt={destination} 
        className="w-full h-full object-cover"
        loading="lazy"
      />
    </div>
    
    <CardContent className="flex-1 p-3 md:p-4">
      <h3 className="font-bold text-base md:text-lg line-clamp-1">
        {destination}
      </h3>
      <p className="text-sm text-muted-foreground mt-1">
        {formatDateRange(startDate, endDate)}
      </p>
      
      {/* Mobile: Compact info */}
      <div className="flex items-center gap-2 mt-2 md:mt-3">
        <Badge variant="secondary" className="text-xs">
          {dayCount} days
        </Badge>
        <Badge variant="outline" className="text-xs">
          {status}
        </Badge>
      </div>
    </CardContent>
  </div>
</Card>
```

2. **Update DayCard.tsx** - Touch-friendly expansion:
```typescript
<Card className="overflow-hidden">
  <button
    onClick={() => setExpanded(!expanded)}
    className={cn(
      'w-full text-left p-4 md:p-6',
      'flex items-center justify-between',
      'min-h-[60px] md:min-h-auto', // Touch target
      'active:bg-muted transition-colors'
    )}
  >
    <div className="flex-1">
      <h3 className="font-bold text-base md:text-lg">Day {dayNumber}</h3>
      <p className="text-sm text-muted-foreground">{date}</p>
    </div>
    <ChevronDown className={cn(
      'w-5 h-5 transition-transform',
      expanded && 'rotate-180'
    )} />
  </button>
  
  {expanded && (
    <CardContent className="p-4 md:p-6 pt-0">
      {/* Day content */}
    </CardContent>
  )}
</Card>
```

**Acceptance Criteria:**
- [ ] Cards use horizontal layout on mobile
- [ ] Vertical layout on desktop
- [ ] Touch feedback on mobile
- [ ] Lazy loading for images
- [ ] Proper text truncation

---

## Phase 3: Form & Input Optimization (P1)

### Task 3.1: Mobile-Optimized Form Inputs
**Priority:** P1  
**Effort:** 5 hours  
**Files to Modify:**
- `frontend-redesign/src/components/ui/input.tsx`
- `frontend-redesign/src/components/ui/date-picker.tsx`
- `frontend-redesign/src/components/ui/autocomplete.tsx`
- `frontend-redesign/src/components/ui/select.tsx`

**Current State:**
```typescript
// input.tsx - Generic input
const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, type, ...props }, ref) => {
    return (
      <input
        type={type}
        className={cn(
          "flex h-10 w-full rounded-md border...",
          className
        )}
        ref={ref}
        {...props}
      />
    )
  }
)
```

**Required Changes:**

1. **Update input.tsx** - Add mobile optimizations:
```typescript
interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  inputMode?: 'none' | 'text' | 'tel' | 'url' | 'email' | 'numeric' | 'decimal' | 'search';
}

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, type, inputMode, ...props }, ref) => {
    return (
      <input
        type={type}
        inputMode={inputMode}
        className={cn(
          "flex w-full rounded-md border",
          "h-12 md:h-10", // Larger on mobile
          "text-base md:text-sm", // Prevent zoom on iOS
          "px-4 md:px-3 py-2",
          "focus:outline-none focus:ring-2 focus:ring-primary",
          className
        )}
        ref={ref}
        {...props}
      />
    )
  }
)
```

2. **Update date-picker.tsx** - Native picker on mobile:
```typescript
import { useMediaQuery } from '@/hooks/useMediaQuery';

export function DatePicker({ value, onChange, ...props }: DatePickerProps) {
  const isMobile = useMediaQuery('(max-width: 768px)');

  if (isMobile) {
    // Native mobile date picker
    return (
      <input
        type="date"
        value={value ? format(value, 'yyyy-MM-dd') : ''}
        onChange={(e) => onChange(new Date(e.target.value))}
        className={cn(
          "flex h-12 w-full rounded-md border",
          "px-4 py-2 text-base",
          "focus:outline-none focus:ring-2 focus:ring-primary"
        )}
        {...props}
      />
    );
  }

  // Desktop: Custom calendar picker
  return (
    <Popover>
      {/* Existing calendar implementation */}
    </Popover>
  );
}
```

3. **Update autocomplete.tsx** - Touch-friendly dropdown:
```typescript
<Command className="rounded-lg border shadow-md">
  <CommandInput 
    placeholder={placeholder}
    className="h-12 md:h-10 text-base md:text-sm"
  />
  <CommandList className="max-h-[60vh] md:max-h-[300px]">
    <CommandEmpty>No results found.</CommandEmpty>
    <CommandGroup>
      {options.map((option) => (
        <CommandItem
          key={option.value}
          onSelect={() => onSelect(option)}
          className={cn(
            "flex items-center gap-2",
            "min-h-[48px] md:min-h-[36px]", // Touch target
            "px-4 md:px-2 py-3 md:py-2",
            "cursor-pointer"
          )}
        >
          {option.label}
        </CommandItem>
      ))}
    </CommandGroup>
  </CommandList>
</Command>
```

**Acceptance Criteria:**
- [ ] Input height ≥ 48px on mobile
- [ ] Font size ≥ 16px (prevents iOS zoom)
- [ ] Native date picker on mobile
- [ ] Dropdown items ≥ 48px tall
- [ ] Proper keyboard types (numeric, email, etc.)

---

## Phase 4: Performance & Polish (P2)

### Task 4.1: Add Pull-to-Refresh
**Priority:** P2  
**Effort:** 3 hours  
**Files to Create:**
- `frontend-redesign/src/hooks/usePullToRefresh.ts`

**Files to Modify:**
- `frontend-redesign/src/pages/DashboardPage.tsx`
- `frontend-redesign/src/pages/TripDetailPage.tsx`

**Implementation:**

1. **Create usePullToRefresh.ts**:
```typescript
import { useEffect, useRef, useState } from 'react';

export function usePullToRefresh(onRefresh: () => Promise<void>) {
  const [isPulling, setIsPulling] = useState(false);
  const [pullDistance, setPullDistance] = useState(0);
  const startY = useRef(0);
  const threshold = 80;

  useEffect(() => {
    let touchStartY = 0;

    const handleTouchStart = (e: TouchEvent) => {
      if (window.scrollY === 0) {
        touchStartY = e.touches[0].clientY;
      }
    };

    const handleTouchMove = (e: TouchEvent) => {
      if (window.scrollY === 0 && touchStartY > 0) {
        const currentY = e.touches[0].clientY;
        const distance = Math.max(0, currentY - touchStartY);
        
        if (distance > 0) {
          e.preventDefault();
          setPullDistance(Math.min(distance, threshold * 1.5));
          setIsPulling(distance > threshold);
        }
      }
    };

    const handleTouchEnd = async () => {
      if (pullDistance > threshold) {
        await onRefresh();
      }
      setPullDistance(0);
      setIsPulling(false);
    };

    document.addEventListener('touchstart', handleTouchStart);
    document.addEventListener('touchmove', handleTouchMove, { passive: false });
    document.addEventListener('touchend', handleTouchEnd);

    return () => {
      document.removeEventListener('touchstart', handleTouchStart);
      document.removeEventListener('touchmove', handleTouchMove);
      document.removeEventListener('touchend', handleTouchEnd);
    };
  }, [onRefresh, pullDistance]);

  return { isPulling, pullDistance };
}
```

2. **Update DashboardPage.tsx**:
```typescript
export function DashboardPage() {
  const { refetch } = useItineraries();
  const { isPulling, pullDistance } = usePullToRefresh(async () => {
    await refetch();
  });

  return (
    <div className="relative">
      {/* Pull indicator */}
      <div 
        className="md:hidden fixed top-16 left-0 right-0 flex justify-center transition-opacity"
        style={{ 
          opacity: pullDistance / 80,
          transform: `translateY(${Math.min(pullDistance, 80)}px)`
        }}
      >
        <Loader2 className={cn(
          "w-6 h-6 text-primary",
          isPulling && "animate-spin"
        )} />
      </div>

      {/* Page content */}
      <div className="container py-8">
        {/* ... */}
      </div>
    </div>
  );
}
```

**Acceptance Criteria:**
- [ ] Pull down to refresh on mobile
- [ ] Visual feedback during pull
- [ ] Spinner animation when refreshing
- [ ] Works only when scrolled to top
- [ ] Desktop unaffected

---

### Task 4.2: Add Swipe Gestures for Cards
**Priority:** P2  
**Effort:** 4 hours  
**Files to Modify:**
- `frontend-redesign/src/components/dashboard/TripCard.tsx`
- `frontend-redesign/src/components/booking/BookingCard.tsx`

**Implementation:**

```typescript
import { useSwipeable } from 'react-swipeable';

export function TripCard({ trip, onDelete, onShare }: TripCardProps) {
  const [swipeOffset, setSwipeOffset] = useState(0);
  const [showActions, setShowActions] = useState(false);

  const handlers = useSwipeable({
    onSwiping: (eventData) => {
      if (eventData.dir === 'Left') {
        setSwipeOffset(Math.max(-120, eventData.deltaX));
      }
    },
    onSwiped: (eventData) => {
      if (Math.abs(swipeOffset) > 60) {
        setSwipeOffset(-120);
        setShowActions(true);
      } else {
        setSwipeOffset(0);
        setShowActions(false);
      }
    },
    trackMouse: false,
  });

  return (
    <div className="relative overflow-hidden">
      {/* Action buttons (revealed on swipe) */}
      <div className="absolute right-0 top-0 bottom-0 flex items-center gap-2 pr-4">
        <Button
          size="icon"
          variant="ghost"
          onClick={() => onShare(trip.id)}
          className="h-12 w-12"
        >
          <Share2 className="w-5 h-5" />
        </Button>
        <Button
          size="icon"
          variant="destructive"
          onClick={() => onDelete(trip.id)}
          className="h-12 w-12"
        >
          <Trash2 className="w-5 h-5" />
        </Button>
      </div>

      {/* Card content */}
      <div
        {...handlers}
        style={{ transform: `translateX(${swipeOffset}px)` }}
        className="transition-transform bg-background"
      >
        <Card>
          {/* Existing card content */}
        </Card>
      </div>
    </div>
  );
}
```

**Acceptance Criteria:**
- [ ] Swipe left reveals actions
- [ ] Swipe right hides actions
- [ ] Smooth animations
- [ ] Actions: Share, Delete
- [ ] Desktop: Hover shows actions

---

## Phase 5: Testing & Documentation (P2)

### Task 5.1: Mobile Device Testing
**Priority:** P2  
**Effort:** 6 hours  
**Devices to Test:**
- iPhone SE (small screen)
- iPhone 14 Pro (notch)
- Samsung Galaxy S21 (Android)
- iPad (tablet)

**Test Cases:**
1. Navigation flow on all devices
2. Touch target accessibility
3. Form input behavior
4. Modal/bottom sheet behavior
5. Swipe gestures
6. Pull-to-refresh
7. Orientation changes
8. Keyboard behavior

**Files to Create:**
- `analysis/MOBILE_TESTING_REPORT.md`

---

### Task 5.2: Update Documentation
**Priority:** P2  
**Effort:** 2 hours  
**Files to Update:**
- `frontend-redesign/README.md`
- `frontend-redesign/DOCUMENTATION.md`

**Add Sections:**
- Mobile-first development guidelines
- Responsive breakpoints
- Touch target requirements
- Mobile-specific components
- Testing procedures

---

## Summary

**Total Tasks:** 12  
**Total Estimated Effort:** 51 hours  
**Priority Breakdown:**
- P0 (Critical): 13 hours
- P1 (High): 23 hours
- P2 (Medium): 15 hours

**Key Files Modified:** 25+  
**New Files Created:** 5

**Dependencies:**
- `react-swipeable` package
- `useMediaQuery` hook
- Mobile testing devices

**Success Metrics:**
- [ ] 100% navigation parity mobile/web
- [ ] 100% touch target compliance
- [ ] < 3s mobile page load
- [ ] > 90 Lighthouse mobile score
- [ ] Zero layout shift on mobile
