# Premium UI Redesign - Design Document

## Overview

This design document outlines the technical architecture and implementation strategy for transforming the travel planning interface into a premium, world-class user experience. The design draws inspiration from industry leaders like Airbnb, Booking.com, Stripe, and Linear, implementing their best practices while maintaining our unique value proposition.

### Design Philosophy

The redesign follows three core principles:

1. **Delightful Interactions**: Every user action receives immediate, satisfying feedback through micro-animations and transitions
2. **Progressive Disclosure**: Information is revealed incrementally to reduce cognitive load and guide users naturally through the flow
3. **Performance First**: All animations and interactions are optimized for 60fps performance across devices

### Success Metrics

- First Contentful Paint (FCP): < 1.5s
- Time to Interactive (TTI): < 3.5s
- Animation frame rate: 60fps (no drops > 5%)
- Interaction response time: < 100ms
- Accessibility score: 100 (Lighthouse)

## Architecture

### Technology Stack

#### Core Framework
- **React 18.2+**: Leveraging concurrent features for smooth animations
- **TypeScript 5.0+**: Type-safe component development
- **Vite 4.0+**: Fast development and optimized production builds

#### Animation & Motion
- **Framer Motion 10.0+**: Declarative animations with spring physics
- **React Spring**: Physics-based animations for natural motion
- **GSAP** (optional): Complex timeline animations for loading sequences

#### UI Components
- **Radix UI**: Accessible, unstyled component primitives
- **Tailwind CSS 3.3+**: Utility-first styling with custom design tokens
- **CVA (Class Variance Authority)**: Type-safe component variants

#### State Management
- **Zustand**: Lightweight state management for UI state
- **React Query**: Server state management with caching
- **React Hook Form**: Performant form state management

#### External Integrations
- **Google Places API**: Location autocomplete
- **Google Maps JavaScript API**: Interactive map component
- **Unsplash API**: High-quality destination images



### Component Architecture

```
src/
├── components/
│   ├── premium/
│   │   ├── search/
│   │   │   ├── UnifiedSearchBar.tsx
│   │   │   ├── LocationAutocomplete.tsx
│   │   │   ├── DateRangePicker.tsx
│   │   │   ├── TravelerSelector.tsx
│   │   │   └── BudgetSlider.tsx
│   │   ├── filters/
│   │   │   ├── FilterChipBar.tsx
│   │   │   ├── SortDropdown.tsx
│   │   │   └── ActiveFiltersBar.tsx
│   │   ├── results/
│   │   │   ├── ResultCard.tsx
│   │   │   ├── ResultGrid.tsx
│   │   │   └── ResultSkeleton.tsx
│   │   ├── wizard/
│   │   │   ├── ProgressStepper.tsx
│   │   │   ├── StepTransition.tsx
│   │   │   └── WizardContainer.tsx
│   │   ├── payment/
│   │   │   ├── SecurePaymentForm.tsx
│   │   │   ├── CardInput.tsx
│   │   │   └── PaymentBrandDetector.tsx
│   │   ├── map/
│   │   │   ├── InteractiveMap.tsx
│   │   │   ├── PriceMarker.tsx
│   │   │   ├── ClusterMarker.tsx
│   │   │   └── MapControls.tsx
│   │   ├── feedback/
│   │   │   ├── Toast.tsx
│   │   │   ├── Tooltip.tsx
│   │   │   └── Popover.tsx
│   │   └── layout/
│   │       ├── SummaryBar.tsx
│   │       └── StickyHeader.tsx
│   └── ui/
│       ├── primitives/
│       │   ├── Button.tsx
│       │   ├── Input.tsx
│       │   ├── Card.tsx
│       │   └── Badge.tsx
│       └── animated/
│           ├── AnimatedButton.tsx
│           ├── AnimatedCard.tsx
│           └── AnimatedInput.tsx
├── hooks/
│   ├── animations/
│   │   ├── useScrollAnimation.ts
│   │   ├── useFadeIn.ts
│   │   ├── useHoverScale.ts
│   │   └── useStaggerChildren.ts
│   ├── interactions/
│   │   ├── useDebounce.ts
│   │   ├── useThrottle.ts
│   │   └── useKeyboardNav.ts
│   └── data/
│       ├── usePlacesAutocomplete.ts
│       ├── useSearchResults.ts
│       └── usePriceData.ts
├── lib/
│   ├── animations/
│   │   ├── variants.ts
│   │   ├── transitions.ts
│   │   └── spring-configs.ts
│   ├── design-tokens/
│   │   ├── colors.ts
│   │   ├── spacing.ts
│   │   ├── typography.ts
│   │   └── shadows.ts
│   └── utils/
│       ├── animation-utils.ts
│       ├── performance.ts
│       └── accessibility.ts
└── styles/
    ├── tokens.css
    ├── animations.css
    └── utilities.css
```

## Components and Interfaces

### 1. Design Token System

#### Color Palette
Inspired by Stripe's sophisticated color system with semantic meaning:

```typescript
// lib/design-tokens/colors.ts
export const colors = {
  // Brand Colors
  primary: {
    50: '#f0f9ff',
    100: '#e0f2fe',
    200: '#bae6fd',
    300: '#7dd3fc',
    400: '#38bdf8',
    500: '#0ea5e9',  // Main brand color
    600: '#0284c7',
    700: '#0369a1',
    800: '#075985',
    900: '#0c4a6e',
  },
  
  // Semantic Colors
  success: {
    light: '#10b981',
    main: '#059669',
    dark: '#047857',
  },
  warning: {
    light: '#f59e0b',
    main: '#d97706',
    dark: '#b45309',
  },
  error: {
    light: '#ef4444',
    main: '#dc2626',
    dark: '#b91c1c',
  },
  
  // Neutral Grays (high contrast)
  gray: {
    50: '#f9fafb',
    100: '#f3f4f6',
    200: '#e5e7eb',
    300: '#d1d5db',
    400: '#9ca3af',
    500: '#6b7280',
    600: '#4b5563',
    700: '#374151',
    800: '#1f2937',
    900: '#111827',
  },
  
  // Overlay & Backdrop
  overlay: 'rgba(0, 0, 0, 0.5)',
  backdrop: 'rgba(0, 0, 0, 0.25)',
} as const;
```



#### Motion Tokens
Based on Material Design 3 and Apple's Human Interface Guidelines:

```typescript
// lib/design-tokens/motion.ts
export const motion = {
  // Duration (in milliseconds)
  duration: {
    instant: 100,      // Micro-interactions (hover, focus)
    fast: 150,         // Button clicks, toggles
    normal: 250,       // Card animations, dropdowns
    slow: 400,         // Page transitions, modals
    slower: 600,       // Complex animations
    slowest: 800,      // Success animations, celebrations
  },
  
  // Easing Functions
  easing: {
    // Entrances (ease-out)
    easeOut: 'cubic-bezier(0.0, 0.0, 0.2, 1)',
    easeOutQuart: 'cubic-bezier(0.165, 0.84, 0.44, 1)',
    
    // Exits (ease-in)
    easeIn: 'cubic-bezier(0.4, 0.0, 1, 1)',
    easeInQuart: 'cubic-bezier(0.895, 0.03, 0.685, 0.22)',
    
    // Movements (ease-in-out)
    easeInOut: 'cubic-bezier(0.4, 0.0, 0.2, 1)',
    easeInOutQuart: 'cubic-bezier(0.77, 0, 0.175, 1)',
    
    // Spring (natural motion)
    spring: 'cubic-bezier(0.68, -0.55, 0.265, 1.55)',
  },
  
  // Spring Configurations (for Framer Motion)
  spring: {
    gentle: { type: 'spring', stiffness: 120, damping: 14 },
    snappy: { type: 'spring', stiffness: 300, damping: 25 },
    bouncy: { type: 'spring', stiffness: 400, damping: 10 },
  },
} as const;
```

#### Typography Scale
Following a modular scale (1.250 - Major Third):

```typescript
// lib/design-tokens/typography.ts
export const typography = {
  fontFamily: {
    sans: '"Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    mono: '"JetBrains Mono", "Fira Code", monospace',
  },
  
  fontSize: {
    xs: '0.75rem',    // 12px
    sm: '0.875rem',   // 14px
    base: '1rem',     // 16px
    lg: '1.125rem',   // 18px
    xl: '1.25rem',    // 20px
    '2xl': '1.5rem',  // 24px
    '3xl': '1.875rem',// 30px
    '4xl': '2.25rem', // 36px
    '5xl': '3rem',    // 48px
  },
  
  fontWeight: {
    normal: 400,
    medium: 500,
    semibold: 600,
    bold: 700,
  },
  
  lineHeight: {
    tight: 1.2,
    normal: 1.5,
    relaxed: 1.75,
  },
} as const;
```

#### Spacing System
Based on 4px grid:

```typescript
// lib/design-tokens/spacing.ts
export const spacing = {
  0: '0',
  1: '0.25rem',   // 4px
  2: '0.5rem',    // 8px
  3: '0.75rem',   // 12px
  4: '1rem',      // 16px
  5: '1.25rem',   // 20px
  6: '1.5rem',    // 24px
  8: '2rem',      // 32px
  10: '2.5rem',   // 40px
  12: '3rem',     // 48px
  16: '4rem',     // 64px
  20: '5rem',     // 80px
  24: '6rem',     // 96px
} as const;
```

#### Shadow System
Elevation-based shadows inspired by Material Design:

```typescript
// lib/design-tokens/shadows.ts
export const shadows = {
  none: 'none',
  sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
  base: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)',
  md: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
  lg: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
  xl: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
  '2xl': '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
  inner: 'inset 0 2px 4px 0 rgba(0, 0, 0, 0.06)',
} as const;
```

### 2. Animation Variants Library

Reusable Framer Motion variants for consistent animations:

```typescript
// lib/animations/variants.ts
import { Variants } from 'framer-motion';
import { motion } from './transitions';

export const fadeIn: Variants = {
  initial: { opacity: 0 },
  animate: { opacity: 1 },
  exit: { opacity: 0 },
};

export const fadeInUp: Variants = {
  initial: { opacity: 0, y: 20 },
  animate: { 
    opacity: 1, 
    y: 0,
    transition: { duration: motion.duration.normal / 1000, ease: motion.easing.easeOut }
  },
  exit: { 
    opacity: 0, 
    y: -20,
    transition: { duration: motion.duration.fast / 1000, ease: motion.easing.easeIn }
  },
};

export const scaleIn: Variants = {
  initial: { opacity: 0, scale: 0.95 },
  animate: { 
    opacity: 1, 
    scale: 1,
    transition: { duration: motion.duration.normal / 1000, ease: motion.easing.easeOut }
  },
  exit: { 
    opacity: 0, 
    scale: 0.95,
    transition: { duration: motion.duration.fast / 1000, ease: motion.easing.easeIn }
  },
};

export const slideInRight: Variants = {
  initial: { x: 100, opacity: 0 },
  animate: { 
    x: 0, 
    opacity: 1,
    transition: { duration: motion.duration.slow / 1000, ease: motion.easing.easeOut }
  },
  exit: { 
    x: -100, 
    opacity: 0,
    transition: { duration: motion.duration.slow / 1000, ease: motion.easing.easeIn }
  },
};

export const slideInLeft: Variants = {
  initial: { x: -100, opacity: 0 },
  animate: { 
    x: 0, 
    opacity: 1,
    transition: { duration: motion.duration.slow / 1000, ease: motion.easing.easeOut }
  },
  exit: { 
    x: 100, 
    opacity: 0,
    transition: { duration: motion.duration.slow / 1000, ease: motion.easing.easeIn }
  },
};

export const staggerContainer: Variants = {
  animate: {
    transition: {
      staggerChildren: 0.1,
    },
  },
};

export const hoverScale: Variants = {
  rest: { scale: 1 },
  hover: { 
    scale: 1.02,
    transition: { duration: motion.duration.fast / 1000, ease: motion.easing.easeOut }
  },
  tap: { 
    scale: 0.98,
    transition: { duration: motion.duration.instant / 1000 }
  },
};

export const hoverLift: Variants = {
  rest: { y: 0, boxShadow: shadows.md },
  hover: { 
    y: -4,
    boxShadow: shadows.xl,
    transition: { duration: motion.duration.normal / 1000, ease: motion.easing.easeOut }
  },
};

// Shimmer loading animation
export const shimmer: Variants = {
  initial: { backgroundPosition: '-200% 0' },
  animate: {
    backgroundPosition: '200% 0',
    transition: {
      duration: 1.5,
      ease: 'linear',
      repeat: Infinity,
    },
  },
};
```



### 3. Custom Animation Hooks

Reusable hooks for common animation patterns:

```typescript
// hooks/animations/useScrollAnimation.ts
import { useEffect, useRef } from 'react';
import { useInView, useAnimation } from 'framer-motion';

export function useScrollAnimation() {
  const ref = useRef(null);
  const isInView = useInView(ref, { once: true, margin: '-100px' });
  const controls = useAnimation();

  useEffect(() => {
    if (isInView) {
      controls.start('animate');
    }
  }, [isInView, controls]);

  return { ref, controls };
}

// hooks/animations/useFadeIn.ts
import { useScrollAnimation } from './useScrollAnimation';
import { fadeInUp } from '@/lib/animations/variants';

export function useFadeIn(delay = 0) {
  const { ref, controls } = useScrollAnimation();
  
  return {
    ref,
    initial: 'initial',
    animate: controls,
    variants: fadeInUp,
    transition: { delay },
  };
}

// hooks/animations/useHoverScale.ts
import { useState } from 'react';

export function useHoverScale(scale = 1.02) {
  const [isHovered, setIsHovered] = useState(false);

  return {
    onMouseEnter: () => setIsHovered(true),
    onMouseLeave: () => setIsHovered(false),
    animate: { scale: isHovered ? scale : 1 },
    transition: { duration: 0.2, ease: 'easeOut' },
  };
}

// hooks/animations/useStaggerChildren.ts
export function useStaggerChildren(staggerDelay = 0.1) {
  return {
    variants: {
      animate: {
        transition: {
          staggerChildren: staggerDelay,
        },
      },
    },
  };
}
```

### 4. Core Component Designs

#### Unified Search Bar

Inspired by Airbnb's search experience with enhanced interactions:

```typescript
// components/premium/search/UnifiedSearchBar.tsx
import { motion, AnimatePresence } from 'framer-motion';
import { useState, useEffect } from 'react';
import { MapPin, Calendar, Users, Search, ArrowLeftRight } from 'lucide-react';

interface SearchBarProps {
  onSearch: (params: SearchParams) => void;
}

export function UnifiedSearchBar({ onSearch }: SearchBarProps) {
  const [activeField, setActiveField] = useState<string | null>(null);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [placeholderIndex, setPlaceholderIndex] = useState(0);
  
  const placeholders = ['Paris', 'Tokyo', 'New York', 'Barcelona', 'Dubai'];
  
  // Cycle through placeholder examples
  useEffect(() => {
    const interval = setInterval(() => {
      setPlaceholderIndex((prev) => (prev + 1) % placeholders.length);
    }, 3000);
    return () => clearInterval(interval);
  }, []);
  
  const handleSwap = () => {
    const temp = from;
    setFrom(to);
    setTo(temp);
  };

  return (
    <motion.div
      className="relative w-full max-w-5xl mx-auto"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: 'easeOut' }}
    >
      <div className="flex items-center gap-2 p-2 bg-white rounded-2xl shadow-xl border border-gray-200">
        {/* From Field */}
        <SearchField
          icon={<MapPin className="w-5 h-5" />}
          label="From"
          placeholder={placeholders[placeholderIndex]}
          value={from}
          onChange={setFrom}
          isActive={activeField === 'from'}
          onFocus={() => setActiveField('from')}
          autoFocus
        />
        
        {/* Swap Button */}
        <motion.button
          onClick={handleSwap}
          className="p-2 rounded-full hover:bg-gray-100 transition-colors"
          whileHover={{ scale: 1.1 }}
          whileTap={{ scale: 0.9, rotate: 180 }}
          transition={{ duration: 0.3 }}
        >
          <ArrowLeftRight className="w-5 h-5 text-gray-600" />
        </motion.button>
        
        {/* To Field */}
        <SearchField
          icon={<MapPin className="w-5 h-5" />}
          label="To"
          placeholder="Where to?"
          value={to}
          onChange={setTo}
          isActive={activeField === 'to'}
          onFocus={() => setActiveField('to')}
        />
        
        {/* Dates Field */}
        <SearchField
          icon={<Calendar className="w-5 h-5" />}
          label="Dates"
          placeholder="Add dates"
          isActive={activeField === 'dates'}
          onFocus={() => setActiveField('dates')}
        />
        
        {/* Travelers Field */}
        <SearchField
          icon={<Users className="w-5 h-5" />}
          label="Travelers"
          placeholder="Add guests"
          isActive={activeField === 'travelers'}
          onFocus={() => setActiveField('travelers')}
        />
        
        {/* Search Button */}
        <motion.button
          onClick={() => onSearch({ from, to })}
          className="flex items-center gap-2 px-6 py-3 bg-primary-500 text-white rounded-xl font-semibold"
          whileHover={{ scale: 1.05, boxShadow: '0 10px 20px rgba(14, 165, 233, 0.3)' }}
          whileTap={{ scale: 0.95 }}
          transition={{ duration: 0.2 }}
        >
          <Search className="w-5 h-5" />
          <span>Search</span>
        </motion.button>
      </div>
      
      {/* Microcopy */}
      <motion.p
        className="text-center text-sm text-gray-500 mt-3"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.3 }}
      >
        Find your perfect trip in seconds
      </motion.p>
    </motion.div>
  );
}
```



#### Location Autocomplete

Inspired by Booking.com's grouped suggestions:

```typescript
// components/premium/search/LocationAutocomplete.tsx
import { motion, AnimatePresence } from 'framer-motion';
import { useState, useEffect } from 'react';
import { MapPin, Plane, Landmark, Clock } from 'lucide-react';
import { usePlacesAutocomplete } from '@/hooks/data/usePlacesAutocomplete';

interface LocationAutocompleteProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

export function LocationAutocomplete({ value, onChange, placeholder }: LocationAutocompleteProps) {
  const [isOpen, setIsOpen] = useState(false);
  const { suggestions, recentSearches, loading } = usePlacesAutocomplete(value);
  
  const groupedSuggestions = {
    cities: suggestions.filter(s => s.type === 'city'),
    airports: suggestions.filter(s => s.type === 'airport'),
    landmarks: suggestions.filter(s => s.type === 'landmark'),
  };
  
  const highlightMatch = (text: string, query: string) => {
    const parts = text.split(new RegExp(`(${query})`, 'gi'));
    return parts.map((part, i) => 
      part.toLowerCase() === query.toLowerCase() ? 
        <strong key={i} className="font-semibold text-primary-600">{part}</strong> : 
        part
    );
  };

  return (
    <div className="relative">
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onFocus={() => setIsOpen(true)}
        onBlur={() => setTimeout(() => setIsOpen(false), 200)}
        placeholder={placeholder}
        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all"
      />
      
      <AnimatePresence>
        {isOpen && (value || recentSearches.length > 0) && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.2 }}
            className="absolute z-50 w-full mt-2 bg-white rounded-xl shadow-2xl border border-gray-200 max-h-96 overflow-y-auto"
          >
            {/* Recent Searches */}
            {!value && recentSearches.length > 0 && (
              <div className="p-2">
                <div className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                  Recent Searches
                </div>
                {recentSearches.map((search, index) => (
                  <SuggestionItem
                    key={`recent-${index}`}
                    icon={<Clock className="w-4 h-4" />}
                    primary={search.name}
                    secondary={search.country}
                    onClick={() => onChange(search.name)}
                  />
                ))}
              </div>
            )}
            
            {/* Cities */}
            {groupedSuggestions.cities.length > 0 && (
              <div className="p-2">
                <div className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                  Cities
                </div>
                {groupedSuggestions.cities.map((suggestion) => (
                  <SuggestionItem
                    key={suggestion.id}
                    icon={<MapPin className="w-4 h-4" />}
                    primary={highlightMatch(suggestion.name, value)}
                    secondary={suggestion.country}
                    onClick={() => onChange(suggestion.name)}
                  />
                ))}
              </div>
            )}
            
            {/* Airports */}
            {groupedSuggestions.airports.length > 0 && (
              <div className="p-2">
                <div className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                  Airports
                </div>
                {groupedSuggestions.airports.map((suggestion) => (
                  <SuggestionItem
                    key={suggestion.id}
                    icon={<Plane className="w-4 h-4" />}
                    primary={highlightMatch(suggestion.name, value)}
                    secondary={`${suggestion.code} · ${suggestion.country}`}
                    onClick={() => onChange(suggestion.name)}
                  />
                ))}
              </div>
            )}
            
            {/* Landmarks */}
            {groupedSuggestions.landmarks.length > 0 && (
              <div className="p-2">
                <div className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                  Landmarks
                </div>
                {groupedSuggestions.landmarks.map((suggestion) => (
                  <SuggestionItem
                    key={suggestion.id}
                    icon={<Landmark className="w-4 h-4" />}
                    primary={highlightMatch(suggestion.name, value)}
                    secondary={suggestion.city}
                    onClick={() => onChange(suggestion.name)}
                  />
                ))}
              </div>
            )}
            
            {/* No Results / Did You Mean */}
            {value && suggestions.length === 0 && !loading && (
              <div className="p-4 text-center text-gray-500">
                <p className="text-sm">No results found</p>
                <p className="text-xs mt-1">Did you mean: <span className="text-primary-600 cursor-pointer hover:underline">Paris</span>?</p>
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function SuggestionItem({ icon, primary, secondary, onClick }) {
  return (
    <motion.button
      onClick={onClick}
      className="w-full flex items-center gap-3 px-3 py-2 rounded-lg hover:bg-gray-50 transition-colors text-left"
      whileHover={{ x: 4 }}
      transition={{ duration: 0.15 }}
    >
      <div className="text-gray-400">{icon}</div>
      <div className="flex-1 min-w-0">
        <div className="text-sm font-medium text-gray-900 truncate">{primary}</div>
        <div className="text-xs text-gray-500 truncate">{secondary}</div>
      </div>
    </motion.button>
  );
}
```



#### Date Range Picker

Inspired by Airbnb's calendar with price hints:

```typescript
// components/premium/search/DateRangePicker.tsx
import { motion } from 'framer-motion';
import { useState } from 'react';
import { ChevronLeft, ChevronRight, X } from 'lucide-react';
import { format, addMonths, startOfMonth, endOfMonth, eachDayOfInterval, isSameMonth, isToday, isSameDay } from 'date-fns';

interface DateRangePickerProps {
  startDate: Date | null;
  endDate: Date | null;
  onChange: (start: Date | null, end: Date | null) => void;
  priceData?: Record<string, number>;
}

export function DateRangePicker({ startDate, endDate, onChange, priceData }: DateRangePickerProps) {
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [hoveredDate, setHoveredDate] = useState<Date | null>(null);
  
  const presets = [
    { label: 'This Weekend', days: 2 },
    { label: 'Next Week', days: 7 },
    { label: 'Next Month', days: 30 },
    { label: 'Flexible', days: null },
  ];
  
  const handleDateClick = (date: Date) => {
    if (!startDate || (startDate && endDate)) {
      // Start new selection
      onChange(date, null);
    } else {
      // Complete selection
      if (date < startDate) {
        onChange(date, startDate);
      } else {
        onChange(startDate, date);
      }
    }
  };
  
  const getDuration = () => {
    if (!startDate || !endDate) return null;
    const days = Math.ceil((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24));
    return `${days} night${days > 1 ? 's' : ''}, ${days + 1} days`;
  };
  
  const getPriceLevel = (date: Date): 'low' | 'medium' | 'high' | null => {
    if (!priceData) return null;
    const dateKey = format(date, 'yyyy-MM-dd');
    const price = priceData[dateKey];
    if (!price) return null;
    if (price < 100) return 'low';
    if (price < 200) return 'medium';
    return 'high';
  };

  return (
    <div className="bg-white rounded-xl shadow-2xl border border-gray-200 p-6 w-full max-w-4xl">
      {/* Quick Presets */}
      <div className="flex gap-2 mb-6">
        {presets.map((preset) => (
          <motion.button
            key={preset.label}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
          >
            {preset.label}
          </motion.button>
        ))}
      </div>
      
      {/* Calendar Grid */}
      <div className="grid grid-cols-2 gap-8">
        {[0, 1].map((offset) => {
          const month = addMonths(currentMonth, offset);
          return (
            <div key={offset}>
              {/* Month Header */}
              <div className="flex items-center justify-between mb-4">
                {offset === 0 && (
                  <button
                    onClick={() => setCurrentMonth(addMonths(currentMonth, -1))}
                    className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                  >
                    <ChevronLeft className="w-5 h-5" />
                  </button>
                )}
                <h3 className="text-lg font-semibold flex-1 text-center">
                  {format(month, 'MMMM yyyy')}
                </h3>
                {offset === 1 && (
                  <button
                    onClick={() => setCurrentMonth(addMonths(currentMonth, 1))}
                    className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                  >
                    <ChevronRight className="w-5 h-5" />
                  </button>
                )}
              </div>
              
              {/* Day Headers */}
              <div className="grid grid-cols-7 gap-1 mb-2">
                {['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'].map((day) => (
                  <div key={day} className="text-center text-xs font-semibold text-gray-500 py-2">
                    {day}
                  </div>
                ))}
              </div>
              
              {/* Calendar Days */}
              <CalendarMonth
                month={month}
                startDate={startDate}
                endDate={endDate}
                hoveredDate={hoveredDate}
                onDateClick={handleDateClick}
                onDateHover={setHoveredDate}
                getPriceLevel={getPriceLevel}
              />
            </div>
          );
        })}
      </div>
      
      {/* Duration Tooltip */}
      {hoveredDate && startDate && !endDate && (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="mt-4 text-center text-sm text-gray-600"
        >
          {getDuration()}
        </motion.div>
      )}
      
      {/* Action Buttons */}
      <div className="flex items-center justify-between mt-6 pt-6 border-t border-gray-200">
        <button
          onClick={() => onChange(null, null)}
          className="text-sm font-medium text-gray-600 hover:text-gray-900 transition-colors"
        >
          Clear
        </button>
        <motion.button
          onClick={() => {/* Apply logic */}}
          disabled={!startDate || !endDate}
          className="px-6 py-2 bg-primary-500 text-white rounded-lg font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
        >
          Apply
        </motion.button>
      </div>
    </div>
  );
}

function CalendarDay({ date, isSelected, isInRange, isStart, isEnd, priceLevel, onClick, onHover }) {
  const priceDotColors = {
    low: 'bg-green-500',
    medium: 'bg-yellow-500',
    high: 'bg-red-500',
  };

  return (
    <motion.button
      onClick={() => onClick(date)}
      onMouseEnter={() => onHover(date)}
      className={`
        relative aspect-square p-2 rounded-lg text-sm font-medium transition-all
        ${isSelected ? 'bg-primary-500 text-white' : ''}
        ${isInRange && !isSelected ? 'bg-primary-100 text-primary-900' : ''}
        ${!isSelected && !isInRange ? 'hover:bg-gray-100 text-gray-900' : ''}
        ${isStart || isEnd ? 'ring-2 ring-primary-500' : ''}
      `}
      whileHover={{ scale: 1.05 }}
      whileTap={{ scale: 0.95 }}
    >
      {format(date, 'd')}
      {priceLevel && (
        <div className={`absolute bottom-1 left-1/2 transform -translate-x-1/2 w-1 h-1 rounded-full ${priceDotColors[priceLevel]}`} />
      )}
    </motion.button>
  );
}
```



#### Traveler Selector

Inspired by Airbnb's guest selector:

```typescript
// components/premium/search/TravelerSelector.tsx
import { motion, AnimatePresence } from 'framer-motion';
import { useState } from 'react';
import { Plus, Minus } from 'lucide-react';

interface TravelerSelectorProps {
  value: TravelerCounts;
  onChange: (value: TravelerCounts) => void;
}

interface TravelerCounts {
  adults: number;
  children: number;
  infants: number;
}

export function TravelerSelector({ value, onChange }: TravelerSelectorProps) {
  const [isOpen, setIsOpen] = useState(false);
  
  const getSummaryText = () => {
    const parts = [];
    if (value.adults > 0) parts.push(`${value.adults} adult${value.adults > 1 ? 's' : ''}`);
    if (value.children > 0) parts.push(`${value.children} child${value.children > 1 ? 'ren' : ''}`);
    if (value.infants > 0) parts.push(`${value.infants} infant${value.infants > 1 ? 's' : ''}`);
    return parts.join(', ') || 'Add travelers';
  };
  
  const updateCount = (type: keyof TravelerCounts, delta: number) => {
    const newValue = { ...value };
    newValue[type] = Math.max(type === 'adults' ? 1 : 0, newValue[type] + delta);
    newValue[type] = Math.min(9, newValue[type]); // Max 9 per category
    onChange(newValue);
  };

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="px-4 py-3 border border-gray-300 rounded-lg hover:border-gray-400 transition-colors text-left w-full"
      >
        {getSummaryText()}
      </button>
      
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: -10, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -10, scale: 0.95 }}
            transition={{ duration: 0.2 }}
            className="absolute z-50 mt-2 w-80 bg-white rounded-xl shadow-2xl border border-gray-200 p-4"
          >
            <TravelerRow
              label="Adults"
              description="18+ years"
              count={value.adults}
              onIncrement={() => updateCount('adults', 1)}
              onDecrement={() => updateCount('adults', -1)}
              minCount={1}
            />
            
            <TravelerRow
              label="Children"
              description="2-17 years"
              count={value.children}
              onIncrement={() => updateCount('children', 1)}
              onDecrement={() => updateCount('children', -1)}
              minCount={0}
            />
            
            <TravelerRow
              label="Infants"
              description="0-2 years"
              count={value.infants}
              onIncrement={() => updateCount('infants', 1)}
              onDecrement={() => updateCount('infants', -1)}
              minCount={0}
            />
            
            <motion.button
              onClick={() => setIsOpen(false)}
              className="w-full mt-4 px-4 py-2 bg-primary-500 text-white rounded-lg font-semibold"
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              Apply
            </motion.button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function TravelerRow({ label, description, count, onIncrement, onDecrement, minCount }) {
  const isAtMin = count <= minCount;
  const isAtMax = count >= 9;

  return (
    <div className="flex items-center justify-between py-4 border-b border-gray-100 last:border-0">
      <div>
        <div className="font-medium text-gray-900">{label}</div>
        <div className="text-sm text-gray-500">{description}</div>
      </div>
      
      <div className="flex items-center gap-3">
        <motion.button
          onClick={onDecrement}
          disabled={isAtMin}
          className={`
            w-8 h-8 rounded-full border-2 flex items-center justify-center transition-all
            ${isAtMin 
              ? 'border-gray-200 text-gray-300 cursor-not-allowed' 
              : 'border-primary-500 text-primary-500 hover:bg-primary-50'
            }
          `}
          whileHover={!isAtMin ? { scale: 1.1 } : {}}
          whileTap={!isAtMin ? { scale: 0.9 } : {}}
        >
          <Minus className="w-4 h-4" />
        </motion.button>
        
        <motion.span
          key={count}
          initial={{ scale: 1.2 }}
          animate={{ scale: 1 }}
          className="w-8 text-center font-semibold text-gray-900"
        >
          {count}
        </motion.span>
        
        <motion.button
          onClick={onIncrement}
          disabled={isAtMax}
          className={`
            w-8 h-8 rounded-full border-2 flex items-center justify-center transition-all
            ${isAtMax 
              ? 'border-gray-200 text-gray-300 cursor-not-allowed' 
              : 'border-primary-500 text-primary-500 hover:bg-primary-50'
            }
          `}
          whileHover={!isAtMax ? { scale: 1.1 } : {}}
          whileTap={!isAtMax ? { scale: 0.9 } : {}}
        >
          <Plus className="w-4 h-4" />
        </motion.button>
      </div>
    </div>
  );
}
```

#### Budget Slider

Inspired by Zillow's price range selector:

```typescript
// components/premium/search/BudgetSlider.tsx
import { motion } from 'framer-motion';
import { useState, useEffect } from 'react';
import * as Slider from '@radix-ui/react-slider';

interface BudgetSliderProps {
  min: number;
  max: number;
  value: [number, number];
  onChange: (value: [number, number]) => void;
  resultsCount?: number;
}

export function BudgetSlider({ min, max, value, onChange, resultsCount }: BudgetSliderProps) {
  const [localValue, setLocalValue] = useState(value);
  
  useEffect(() => {
    const timeout = setTimeout(() => {
      onChange(localValue);
    }, 300); // Debounce
    return () => clearTimeout(timeout);
  }, [localValue]);
  
  const getGradientColor = () => {
    const midpoint = (max - min) / 2;
    const currentMid = (localValue[0] + localValue[1]) / 2;
    if (currentMid < midpoint * 0.7) return 'from-green-500 to-green-600';
    if (currentMid < midpoint * 1.3) return 'from-yellow-500 to-yellow-600';
    return 'from-orange-500 to-orange-600';
  };
  
  const presets = [
    { label: 'Budget', range: [min, min + (max - min) * 0.3] },
    { label: 'Mid-range', range: [min + (max - min) * 0.3, min + (max - min) * 0.7] },
    { label: 'Luxury', range: [min + (max - min) * 0.7, max] },
    { label: 'Any', range: [min, max] },
  ];

  return (
    <div className="w-full p-6 bg-white rounded-xl border border-gray-200">
      {/* Results Count */}
      {resultsCount !== undefined && (
        <motion.div
          key={resultsCount}
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-4 text-sm font-medium text-gray-700"
        >
          {resultsCount} trips found
        </motion.div>
      )}
      
      {/* Preset Buttons */}
      <div className="flex gap-2 mb-6">
        {presets.map((preset) => (
          <motion.button
            key={preset.label}
            onClick={() => setLocalValue(preset.range as [number, number])}
            className="px-3 py-1 text-xs font-medium text-gray-700 bg-gray-100 rounded-full hover:bg-gray-200 transition-colors"
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
          >
            {preset.label}
          </motion.button>
        ))}
      </div>
      
      {/* Slider */}
      <div className="relative px-2">
        <Slider.Root
          className="relative flex items-center select-none touch-none w-full h-5"
          value={localValue}
          onValueChange={setLocalValue}
          min={min}
          max={max}
          step={10}
        >
          <Slider.Track className="bg-gray-200 relative grow rounded-full h-2">
            <Slider.Range className={`absolute bg-gradient-to-r ${getGradientColor()} h-full rounded-full`} />
          </Slider.Track>
          
          {[0, 1].map((index) => (
            <Slider.Thumb
              key={index}
              className="block w-5 h-5 bg-white border-2 border-primary-500 rounded-full shadow-lg hover:scale-110 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 transition-transform"
            />
          ))}
        </Slider.Root>
      </div>
      
      {/* Value Labels */}
      <div className="flex justify-between mt-4">
        <div className="text-sm font-semibold text-gray-900">
          ${localValue[0].toLocaleString()}
        </div>
        <div className="text-sm font-semibold text-gray-900">
          ${localValue[1].toLocaleString()}
        </div>
      </div>
    </div>
  );
}
```



## Data Models

### Search State

```typescript
// types/search.ts
export interface SearchParams {
  from: string;
  to: string;
  startDate: Date | null;
  endDate: Date | null;
  travelers: TravelerCounts;
  budget: [number, number];
  filters: SearchFilters;
}

export interface TravelerCounts {
  adults: number;
  children: number;
  infants: number;
}

export interface SearchFilters {
  duration?: [number, number];
  activities?: string[];
  rating?: number;
  amenities?: string[];
}

export interface SearchResult {
  id: string;
  title: string;
  destination: string;
  image: string;
  price: number;
  originalPrice?: number;
  rating: number;
  reviewCount: number;
  duration: number;
  highlights: string[];
  tags: string[];
  coordinates: { lat: number; lng: number };
}
```

### Animation State

```typescript
// types/animation.ts
export interface AnimationConfig {
  duration: number;
  easing: string;
  delay?: number;
}

export interface ScrollAnimationState {
  isInView: boolean;
  hasAnimated: boolean;
}

export interface HoverState {
  isHovered: boolean;
  isFocused: boolean;
  isPressed: boolean;
}
```

## Error Handling

### Graceful Degradation

```typescript
// components/premium/ErrorBoundary.tsx
import { Component, ReactNode } from 'react';
import { motion } from 'framer-motion';
import { AlertTriangle } from 'lucide-react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class PremiumErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: any) {
    console.error('Premium UI Error:', error, errorInfo);
    // Log to error tracking service
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback || (
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          className="flex flex-col items-center justify-center p-8 bg-red-50 rounded-xl border border-red-200"
        >
          <AlertTriangle className="w-12 h-12 text-red-500 mb-4" />
          <h3 className="text-lg font-semibold text-red-900 mb-2">
            Something went wrong
          </h3>
          <p className="text-sm text-red-700 text-center mb-4">
            We're sorry, but something unexpected happened. Please try refreshing the page.
          </p>
          <button
            onClick={() => window.location.reload()}
            className="px-4 py-2 bg-red-500 text-white rounded-lg font-medium hover:bg-red-600 transition-colors"
          >
            Refresh Page
          </button>
        </motion.div>
      );
    }

    return this.props.children;
  }
}
```

### API Error Handling

```typescript
// lib/utils/error-handling.ts
export class APIError extends Error {
  constructor(
    message: string,
    public statusCode: number,
    public code: string
  ) {
    super(message);
    this.name = 'APIError';
  }
}

export function handleAPIError(error: unknown): string {
  if (error instanceof APIError) {
    switch (error.statusCode) {
      case 400:
        return 'Invalid request. Please check your input.';
      case 401:
        return 'Please log in to continue.';
      case 403:
        return 'You don't have permission to do that.';
      case 404:
        return 'The requested resource was not found.';
      case 429:
        return 'Too many requests. Please try again later.';
      case 500:
        return 'Server error. Please try again later.';
      default:
        return 'Something went wrong. Please try again.';
    }
  }
  return 'An unexpected error occurred.';
}
```

## Testing Strategy

### Component Testing

```typescript
// components/premium/search/__tests__/UnifiedSearchBar.test.tsx
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { UnifiedSearchBar } from '../UnifiedSearchBar';

describe('UnifiedSearchBar', () => {
  it('should autofocus the from field on mount', () => {
    render(<UnifiedSearchBar onSearch={jest.fn()} />);
    const fromInput = screen.getByPlaceholderText(/paris|tokyo|new york/i);
    expect(fromInput).toHaveFocus();
  });

  it('should swap from and to values when swap button is clicked', () => {
    render(<UnifiedSearchBar onSearch={jest.fn()} />);
    const fromInput = screen.getByLabelText('From');
    const toInput = screen.getByLabelText('To');
    const swapButton = screen.getByRole('button', { name: /swap/i });

    fireEvent.change(fromInput, { target: { value: 'Paris' } });
    fireEvent.change(toInput, { target: { value: 'London' } });
    fireEvent.click(swapButton);

    expect(fromInput).toHaveValue('London');
    expect(toInput).toHaveValue('Paris');
  });

  it('should trigger search on Enter key press', () => {
    const onSearch = jest.fn();
    render(<UnifiedSearchBar onSearch={onSearch} />);
    const fromInput = screen.getByLabelText('From');

    fireEvent.change(fromInput, { target: { value: 'Paris' } });
    fireEvent.keyDown(fromInput, { key: 'Enter', code: 'Enter' });

    expect(onSearch).toHaveBeenCalled();
  });
});
```

### Animation Testing

```typescript
// hooks/animations/__tests__/useScrollAnimation.test.ts
import { renderHook } from '@testing-library/react-hooks';
import { useScrollAnimation } from '../useScrollAnimation';

describe('useScrollAnimation', () => {
  it('should return ref and controls', () => {
    const { result } = renderHook(() => useScrollAnimation());
    expect(result.current.ref).toBeDefined();
    expect(result.current.controls).toBeDefined();
  });

  it('should trigger animation when element is in view', async () => {
    const { result } = renderHook(() => useScrollAnimation());
    // Mock IntersectionObserver
    // Test animation trigger
  });
});
```

### Performance Testing

```typescript
// lib/utils/__tests__/performance.test.ts
import { measureFrameRate, measureInteractionTime } from '../performance';

describe('Performance Utilities', () => {
  it('should measure frame rate above 55fps', async () => {
    const fps = await measureFrameRate(1000);
    expect(fps).toBeGreaterThan(55);
  });

  it('should measure interaction time below 100ms', async () => {
    const time = await measureInteractionTime(() => {
      // Simulate interaction
    });
    expect(time).toBeLessThan(100);
  });
});
```

## Performance Optimization

### Code Splitting

```typescript
// App.tsx
import { lazy, Suspense } from 'react';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';

// Lazy load premium components
const UnifiedSearchBar = lazy(() => import('@/components/premium/search/UnifiedSearchBar'));
const InteractiveMap = lazy(() => import('@/components/premium/map/InteractiveMap'));
const DateRangePicker = lazy(() => import('@/components/premium/search/DateRangePicker'));

export function App() {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <UnifiedSearchBar onSearch={handleSearch} />
      {/* Other components */}
    </Suspense>
  );
}
```

### Image Optimization

```typescript
// components/premium/OptimizedImage.tsx
import { useState } from 'react';
import { motion } from 'framer-motion';

interface OptimizedImageProps {
  src: string;
  alt: string;
  width: number;
  height: number;
  className?: string;
}

export function OptimizedImage({ src, alt, width, height, className }: OptimizedImageProps) {
  const [isLoaded, setIsLoaded] = useState(false);
  const [error, setError] = useState(false);
  
  // Generate blur placeholder
  const blurDataURL = `data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ${width} ${height}'%3E%3Cfilter id='b' color-interpolation-filters='sRGB'%3E%3CfeGaussianBlur stdDeviation='20'/%3E%3C/filter%3E%3Cimage filter='url(%23b)' width='100%25' height='100%25' href='${src}'/%3E%3C/svg%3E`;

  return (
    <div className={`relative overflow-hidden ${className}`}>
      {/* Blur Placeholder */}
      {!isLoaded && !error && (
        <img
          src={blurDataURL}
          alt=""
          className="absolute inset-0 w-full h-full object-cover"
        />
      )}
      
      {/* Actual Image */}
      <motion.img
        src={src}
        alt={alt}
        width={width}
        height={height}
        loading="lazy"
        onLoad={() => setIsLoaded(true)}
        onError={() => setError(true)}
        initial={{ opacity: 0 }}
        animate={{ opacity: isLoaded ? 1 : 0 }}
        transition={{ duration: 0.3 }}
        className="w-full h-full object-cover"
      />
      
      {/* Error State */}
      {error && (
        <div className="absolute inset-0 flex items-center justify-center bg-gray-100">
          <span className="text-gray-400 text-sm">Failed to load image</span>
        </div>
      )}
    </div>
  );
}
```



### Animation Performance

```typescript
// lib/utils/performance.ts
export function useReducedMotion() {
  const [prefersReducedMotion, setPrefersReducedMotion] = useState(false);

  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    setPrefersReducedMotion(mediaQuery.matches);

    const handleChange = () => setPrefersReducedMotion(mediaQuery.matches);
    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, []);

  return prefersReducedMotion;
}

export function optimizeAnimation(element: HTMLElement) {
  // Force GPU acceleration
  element.style.willChange = 'transform, opacity';
  element.style.transform = 'translateZ(0)';
  
  // Cleanup after animation
  setTimeout(() => {
    element.style.willChange = 'auto';
  }, 1000);
}

export function measureFrameRate(duration: number = 1000): Promise<number> {
  return new Promise((resolve) => {
    let frames = 0;
    let lastTime = performance.now();
    
    function countFrame() {
      frames++;
      const currentTime = performance.now();
      
      if (currentTime - lastTime >= duration) {
        resolve((frames / duration) * 1000);
      } else {
        requestAnimationFrame(countFrame);
      }
    }
    
    requestAnimationFrame(countFrame);
  });
}
```

### Debouncing and Throttling

```typescript
// hooks/interactions/useDebounce.ts
import { useEffect, useState } from 'react';

export function useDebounce<T>(value: T, delay: number = 300): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => clearTimeout(handler);
  }, [value, delay]);

  return debouncedValue;
}

// hooks/interactions/useThrottle.ts
import { useEffect, useRef, useState } from 'react';

export function useThrottle<T>(value: T, interval: number = 300): T {
  const [throttledValue, setThrottledValue] = useState<T>(value);
  const lastExecuted = useRef<number>(Date.now());

  useEffect(() => {
    if (Date.now() >= lastExecuted.current + interval) {
      lastExecuted.current = Date.now();
      setThrottledValue(value);
    } else {
      const timerId = setTimeout(() => {
        lastExecuted.current = Date.now();
        setThrottledValue(value);
      }, interval);

      return () => clearTimeout(timerId);
    }
  }, [value, interval]);

  return throttledValue;
}
```

## Accessibility Implementation

### Keyboard Navigation

```typescript
// hooks/interactions/useKeyboardNav.ts
import { useEffect, useCallback } from 'react';

interface KeyboardNavOptions {
  onEnter?: () => void;
  onEscape?: () => void;
  onArrowUp?: () => void;
  onArrowDown?: () => void;
  onArrowLeft?: () => void;
  onArrowRight?: () => void;
  onTab?: () => void;
}

export function useKeyboardNav(options: KeyboardNavOptions) {
  const handleKeyDown = useCallback((event: KeyboardEvent) => {
    switch (event.key) {
      case 'Enter':
        options.onEnter?.();
        break;
      case 'Escape':
        options.onEscape?.();
        break;
      case 'ArrowUp':
        event.preventDefault();
        options.onArrowUp?.();
        break;
      case 'ArrowDown':
        event.preventDefault();
        options.onArrowDown?.();
        break;
      case 'ArrowLeft':
        options.onArrowLeft?.();
        break;
      case 'ArrowRight':
        options.onArrowRight?.();
        break;
      case 'Tab':
        options.onTab?.();
        break;
    }
  }, [options]);

  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);
}
```

### Focus Management

```typescript
// lib/utils/accessibility.ts
export function trapFocus(element: HTMLElement) {
  const focusableElements = element.querySelectorAll(
    'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
  );
  
  const firstElement = focusableElements[0] as HTMLElement;
  const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement;

  function handleTabKey(e: KeyboardEvent) {
    if (e.key !== 'Tab') return;

    if (e.shiftKey) {
      if (document.activeElement === firstElement) {
        lastElement.focus();
        e.preventDefault();
      }
    } else {
      if (document.activeElement === lastElement) {
        firstElement.focus();
        e.preventDefault();
      }
    }
  }

  element.addEventListener('keydown', handleTabKey);
  return () => element.removeEventListener('keydown', handleTabKey);
}

export function announceToScreenReader(message: string, priority: 'polite' | 'assertive' = 'polite') {
  const announcement = document.createElement('div');
  announcement.setAttribute('role', 'status');
  announcement.setAttribute('aria-live', priority);
  announcement.setAttribute('aria-atomic', 'true');
  announcement.className = 'sr-only';
  announcement.textContent = message;
  
  document.body.appendChild(announcement);
  
  setTimeout(() => {
    document.body.removeChild(announcement);
  }, 1000);
}

export function getContrastRatio(foreground: string, background: string): number {
  // Convert hex to RGB
  const fgRGB = hexToRGB(foreground);
  const bgRGB = hexToRGB(background);
  
  // Calculate relative luminance
  const fgLuminance = getRelativeLuminance(fgRGB);
  const bgLuminance = getRelativeLuminance(bgRGB);
  
  // Calculate contrast ratio
  const lighter = Math.max(fgLuminance, bgLuminance);
  const darker = Math.min(fgLuminance, bgLuminance);
  
  return (lighter + 0.05) / (darker + 0.05);
}

function hexToRGB(hex: string): [number, number, number] {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result
    ? [parseInt(result[1], 16), parseInt(result[2], 16), parseInt(result[3], 16)]
    : [0, 0, 0];
}

function getRelativeLuminance([r, g, b]: [number, number, number]): number {
  const [rs, gs, bs] = [r, g, b].map((c) => {
    c = c / 255;
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
  });
  return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs;
}
```

### ARIA Labels and Roles

```typescript
// components/premium/search/AccessibleSearchBar.tsx
export function AccessibleSearchBar() {
  return (
    <div role="search" aria-label="Travel search">
      <div className="search-fields" role="group" aria-labelledby="search-heading">
        <h2 id="search-heading" className="sr-only">
          Search for your trip
        </h2>
        
        <label htmlFor="from-input" className="sr-only">
          Departure location
        </label>
        <input
          id="from-input"
          type="text"
          aria-required="true"
          aria-describedby="from-help"
          aria-invalid={hasError}
        />
        <span id="from-help" className="sr-only">
          Enter your departure city or airport
        </span>
        
        <button
          type="button"
          aria-label="Swap departure and destination"
          onClick={handleSwap}
        >
          <ArrowLeftRight aria-hidden="true" />
        </button>
        
        <button
          type="submit"
          aria-label="Search for trips"
          aria-busy={isSearching}
        >
          <Search aria-hidden="true" />
          <span>Search</span>
        </button>
      </div>
      
      <div
        role="status"
        aria-live="polite"
        aria-atomic="true"
        className="sr-only"
      >
        {resultsCount > 0 && `${resultsCount} trips found`}
      </div>
    </div>
  );
}
```

## Integration Points

### Google Places API

```typescript
// hooks/data/usePlacesAutocomplete.ts
import { useState, useEffect } from 'react';
import { useDebounce } from '@/hooks/interactions/useDebounce';

interface PlaceSuggestion {
  id: string;
  name: string;
  type: 'city' | 'airport' | 'landmark';
  country: string;
  code?: string;
  coordinates: { lat: number; lng: number };
}

export function usePlacesAutocomplete(query: string) {
  const [suggestions, setSuggestions] = useState<PlaceSuggestion[]>([]);
  const [loading, setLoading] = useState(false);
  const [recentSearches, setRecentSearches] = useState<PlaceSuggestion[]>([]);
  const debouncedQuery = useDebounce(query, 300);

  useEffect(() => {
    if (!debouncedQuery) {
      setSuggestions([]);
      return;
    }

    setLoading(true);
    
    const autocompleteService = new google.maps.places.AutocompleteService();
    
    autocompleteService.getPlacePredictions(
      {
        input: debouncedQuery,
        types: ['(cities)', 'airport', 'establishment'],
      },
      (predictions, status) => {
        setLoading(false);
        
        if (status === google.maps.places.PlacesServiceStatus.OK && predictions) {
          const formatted = predictions.map((prediction) => ({
            id: prediction.place_id,
            name: prediction.structured_formatting.main_text,
            type: determineType(prediction.types),
            country: prediction.structured_formatting.secondary_text,
            coordinates: { lat: 0, lng: 0 }, // Fetch separately if needed
          }));
          
          setSuggestions(formatted);
        }
      }
    );
  }, [debouncedQuery]);

  return { suggestions, recentSearches, loading };
}

function determineType(types: string[]): 'city' | 'airport' | 'landmark' {
  if (types.includes('airport')) return 'airport';
  if (types.includes('locality') || types.includes('administrative_area_level_1')) return 'city';
  return 'landmark';
}
```

### Backend API Integration

```typescript
// services/api/search.ts
import { SearchParams, SearchResult } from '@/types/search';

export async function searchTrips(params: SearchParams): Promise<SearchResult[]> {
  const response = await fetch('/api/search', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params),
  });

  if (!response.ok) {
    throw new APIError(
      'Failed to search trips',
      response.status,
      'SEARCH_ERROR'
    );
  }

  return response.json();
}

export async function getPriceData(destination: string, dateRange: [Date, Date]): Promise<Record<string, number>> {
  const response = await fetch(`/api/prices?destination=${destination}&start=${dateRange[0].toISOString()}&end=${dateRange[1].toISOString()}`);
  
  if (!response.ok) {
    throw new APIError(
      'Failed to fetch price data',
      response.status,
      'PRICE_ERROR'
    );
  }

  return response.json();
}
```

## Design Rationale

### Why Framer Motion over CSS Animations?

1. **Declarative API**: Easier to reason about complex animation sequences
2. **Spring Physics**: Natural, realistic motion out of the box
3. **Gesture Support**: Built-in drag, hover, and tap handlers
4. **Layout Animations**: Automatic FLIP animations for layout changes
5. **TypeScript Support**: Full type safety for animation props

### Why Radix UI for Primitives?

1. **Accessibility**: WCAG 2.1 AA compliant out of the box
2. **Unstyled**: Complete control over visual design
3. **Composable**: Build complex components from simple primitives
4. **Keyboard Navigation**: Built-in keyboard support
5. **Focus Management**: Automatic focus trapping and restoration

### Color Palette Decisions

The color palette is designed for:
- **High Contrast**: All text meets WCAG AA standards (4.5:1 minimum)
- **Semantic Meaning**: Colors convey status (success, warning, error)
- **Brand Consistency**: Primary blue evokes trust and professionalism
- **Accessibility**: Works for users with color blindness

### Animation Timing Rationale

- **100ms (Instant)**: Immediate feedback for micro-interactions
- **150ms (Fast)**: Button clicks feel responsive
- **250ms (Normal)**: Card animations feel smooth without lag
- **400ms (Slow)**: Page transitions feel intentional
- **600ms+ (Slower)**: Success animations feel celebratory

## Real-World Examples

### Airbnb Search Experience
- **What we're adopting**: Unified search bar, visual calendar, guest selector
- **Why it works**: Reduces cognitive load by keeping all inputs in one place
- **Our implementation**: UnifiedSearchBar component with enhanced animations

### Booking.com Autocomplete
- **What we're adopting**: Grouped suggestions, recent searches, keyboard navigation
- **Why it works**: Helps users find exactly what they're looking for quickly
- **Our implementation**: LocationAutocomplete with Google Places integration

### Stripe Payment Forms
- **What we're adopting**: Inline validation, auto-brand detection, smooth transitions
- **Why it works**: Builds trust through polish and attention to detail
- **Our implementation**: SecurePaymentForm with real-time validation

### Linear Animations
- **What we're adopting**: Smooth page transitions, micro-interactions, spring physics
- **Why it works**: Makes the interface feel alive and responsive
- **Our implementation**: Framer Motion variants library with spring configs

### Zillow Price Slider
- **What we're adopting**: Dual-handle slider, real-time results, gradient feedback
- **Why it works**: Visual feedback helps users understand their selections
- **Our implementation**: BudgetSlider with debounced updates

## Implementation Phases

### Phase 1: Foundation (Week 1)
- Set up design token system
- Create animation variants library
- Build base UI components
- Implement custom hooks

### Phase 2: Search Components (Week 2)
- UnifiedSearchBar
- LocationAutocomplete
- DateRangePicker
- TravelerSelector
- BudgetSlider

### Phase 3: Results & Filters (Week 3)
- ResultCard with animations
- FilterChipBar
- SortDropdown
- InteractiveMap

### Phase 4: Wizard & Forms (Week 4)
- ProgressStepper
- StepTransition
- SecurePaymentForm
- SummaryBar

### Phase 5: Polish & Optimization (Week 5)
- Performance optimization
- Accessibility audit
- Cross-browser testing
- Animation refinement

## Success Criteria

### Visual Quality
- ✅ Professional, modern appearance
- ✅ Consistent design language across all components
- ✅ Smooth 60fps animations with no drops
- ✅ No visual glitches or layout shifts

### User Experience
- ✅ Intuitive, self-explanatory interface
- ✅ Clear feedback for all interactions
- ✅ Fast response times (<100ms)
- ✅ Mobile-friendly and touch-optimized

### Technical Quality
- ✅ Zero TypeScript errors
- ✅ WCAG 2.1 AA accessibility compliance
- ✅ Lighthouse performance score > 90
- ✅ Cross-browser compatibility (Chrome, Firefox, Safari, Edge)

### Business Impact
- ✅ Increased user engagement
- ✅ Higher conversion rates
- ✅ Reduced bounce rates
- ✅ Positive user feedback
