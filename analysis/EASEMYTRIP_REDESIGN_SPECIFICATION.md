# EaseMyTrip Frontend Redesign - Complete Specification

**Last Updated:** 2025-10-25  
**Status:** Final Requirements Document  
**Target:** 100% Accurate Implementation

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Design & Branding Requirements](#2-design--branding-requirements)
3. [Architecture & Integration Strategy](#3-architecture--integration-strategy)
4. [Feature Specifications](#4-feature-specifications)
5. [Page-by-Page Requirements](#5-page-by-page-requirements)
6. [Component Specifications](#6-component-specifications)
7. [Data Flow & Backend Integration](#7-data-flow--backend-integration)
8. [Mobile & Responsive Design](#8-mobile--responsive-design)
9. [Implementation Roadmap](#9-implementation-roadmap)
10. [Quality Standards](#10-quality-standards)

---

## 1. Executive Summary

### 1.1 Project Goal

Create a production-ready frontend that:
- **Mirrors EaseMyTrip's UI/UX exactly** for standard booking flows
- **Integrates existing AI-powered itinerary generation** seamlessly
- **Provides unified booking management** across all travel verticals
- **Maintains backend compatibility** with existing APIs
- **Delivers million-dollar website experience** with consistent, polished UI

### 1.2 Core Principles

1. **Exact Visual Match**: EaseMyTrip's color scheme, fonts, layouts, spacing
2. **Hybrid Functionality**: Standard booking + AI-powered trip planning
3. **Single Source of Truth**: All colors from centralized theme file
4. **Backend Compatibility**: Zero changes to existing API contracts
5. **Production Quality**: Enterprise-grade code, performance, accessibility

---

## 2. Design & Branding Requirements

### 2.1 Color System

#### Primary Color (EaseMyTrip Blue)
```css
/* Exact EaseMyTrip Blue Gradient */
--easemytrip-primary-start: hsl(207, 100%, 42%);  /* #0074E8 */
--easemytrip-primary-end: hsl(207, 100%, 38%);    /* #0066CC */
--easemytrip-primary: hsl(207, 100%, 40%);        /* #0070DA - Main blue */

/* Usage: Linear gradient for headers, buttons, highlights */
background: linear-gradient(135deg, 
  hsl(var(--easemytrip-primary-start)), 
  hsl(var(--easemytrip-primary-end))
);
```

#### Complete Color Palette
```css
/* src/index.css - Single source of truth for all colors */

:root {
  /* === PRIMARY COLORS (EaseMyTrip Blue) === */
  --primary: 207 100% 40%;              /* Main blue #0070DA */
  --primary-hover: 207 100% 38%;        /* Darker blue on hover */
  --primary-foreground: 0 0% 100%;      /* White text on blue */
  
  /* === SECONDARY COLORS (Orange accent) === */
  --secondary: 26 100% 55%;             /* Orange #FF7A00 */
  --secondary-hover: 26 100% 50%;
  --secondary-foreground: 0 0% 100%;
  
  /* === NEUTRAL COLORS === */
  --background: 0 0% 100%;              /* Pure white */
  --foreground: 222 47% 11%;            /* Dark text #1A1D29 */
  --muted: 210 40% 96%;                 /* Light gray backgrounds */
  --muted-foreground: 215 16% 47%;      /* Gray text */
  --border: 214 32% 91%;                /* Light borders */
  --input: 214 32% 91%;                 /* Input borders */
  
  /* === STATUS COLORS === */
  --success: 142 76% 36%;               /* Green #059669 */
  --warning: 38 92% 50%;                /* Yellow #F59E0B */
  --error: 0 84% 60%;                   /* Red #EF4444 */
  --info: 207 100% 40%;                 /* Blue (same as primary) */
  
  /* === SEMANTIC COLORS === */
  --flight-color: 207 100% 40%;         /* Blue */
  --hotel-color: 26 100% 55%;           /* Orange */
  --bus-color: 142 76% 36%;             /* Green */
  --train-color: 271 91% 65%;           /* Purple */
  --activity-color: 338 100% 50%;       /* Pink */
  
  /* === SHADOWS === */
  --shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
  --shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.1);
  --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.1);
  --shadow-xl: 0 20px 25px -5px rgb(0 0 0 / 0.1);
}

/* Dark mode (if needed later) */
.dark {
  --background: 222 47% 11%;
  --foreground: 0 0% 95%;
  /* ... other dark mode colors */
}
```

#### Tailwind Configuration
```typescript
// tailwind.config.ts - Extends the CSS variables
export default {
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: 'hsl(var(--primary))',
          hover: 'hsl(var(--primary-hover))',
          foreground: 'hsl(var(--primary-foreground))',
        },
        secondary: {
          DEFAULT: 'hsl(var(--secondary))',
          hover: 'hsl(var(--secondary-hover))',
          foreground: 'hsl(var(--secondary-foreground))',
        },
        easemytrip: {
          blue: 'hsl(var(--primary))',
          orange: 'hsl(var(--secondary))',
          flight: 'hsl(var(--flight-color))',
          hotel: 'hsl(var(--hotel-color))',
          bus: 'hsl(var(--bus-color))',
          train: 'hsl(var(--train-color))',
        },
        // ... rest of semantic colors
      },
    },
  },
};
```

### 2.2 Typography

#### Font Family
```css
/* Exact match to EaseMyTrip */
--font-primary: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
--font-heading: 'Inter', sans-serif;
--font-mono: 'Fira Code', monospace;
```

#### Font Sizes & Weights
```css
/* Typography scale (8px base) */
--text-xs: 0.75rem;      /* 12px */
--text-sm: 0.875rem;     /* 14px */
--text-base: 1rem;       /* 16px */
--text-lg: 1.125rem;     /* 18px */
--text-xl: 1.25rem;      /* 20px */
--text-2xl: 1.5rem;      /* 24px */
--text-3xl: 1.875rem;    /* 30px */
--text-4xl: 2.25rem;     /* 36px */

/* Font weights */
--font-normal: 400;
--font-medium: 500;
--font-semibold: 600;
--font-bold: 700;
```

#### Usage Guidelines
- **Headers (H1-H6)**: font-semibold to font-bold
- **Body text**: font-normal
- **Buttons**: font-medium to font-semibold
- **Labels**: font-medium
- **Prices**: font-bold

### 2.3 Spacing & Layout

#### 8px Grid System
```css
/* All spacing must be multiples of 8px */
--spacing-xs: 0.25rem;   /* 4px */
--spacing-sm: 0.5rem;    /* 8px */
--spacing-md: 1rem;      /* 16px */
--spacing-lg: 1.5rem;    /* 24px */
--spacing-xl: 2rem;      /* 32px */
--spacing-2xl: 3rem;     /* 48px */
--spacing-3xl: 4rem;     /* 64px */
```

#### Border Radius
```css
--radius-sm: 0.25rem;    /* 4px */
--radius-md: 0.5rem;     /* 8px */
--radius-lg: 0.75rem;    /* 12px */
--radius-xl: 1rem;       /* 16px */
--radius-2xl: 1.5rem;    /* 24px */
--radius-full: 9999px;   /* Fully rounded */
```

---

## 3. Architecture & Integration Strategy

### 3.1 Hybrid Approach (Option B)

```
┌─────────────────────────────────────────────────────────────┐
│                   APPLICATION ARCHITECTURE                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────┐
│   EASEMYTRIP HOMEPAGE       │
│  - Hero with search         │
│  - Flights, Hotels, etc.    │
│  - Trending destinations    │
│  - "Let AI Plan" CTA        │
└────────────┬────────────────┘
             │
    ┌────────┴─────────┐
    │                  │
    v                  v
┌───────────┐    ┌─────────────────────┐
│  STANDARD │    │   AI TRIP PLANNER   │
│  BOOKING  │    │   (Existing flow)   │
│   FLOW    │    └──────────┬──────────┘
└───────────┘               │
                            v
                   ┌────────────────────┐
                   │  ITINERARY CREATED │
                   │  (NormalizedData)  │
                   └─────────┬──────────┘
                             │
                             v
        ┌────────────────────────────────────────┐
        │   UNIFIED TRIP MANAGEMENT INTERFACE    │
        │                                        │
        │  Sidebar Navigation:                   │
        │  ✓ View (Overview)                     │
        │  ✓ Plan (Day-by-day + Destinations)    │
        │  ✓ Bookings (NEW - Multi-vertical)     │
        │  ✓ Budget                              │
        │  ✓ Packing                             │
        │  ✓ Docs                                │
        │                                        │
        │  Main Content Area:                    │
        │  - Itinerary timeline                  │
        │  - Provider selection cards            │
        │  - Booking management                  │
        └────────────────────────────────────────┘
```

### 3.2 Key Integration Points

#### Entry Points
1. **Homepage → Standard Booking**: EaseMyTrip-style search → Provider results
2. **Homepage → AI Planner**: "Let AI Plan" button → Wizard → Generation → Unified Interface
3. **Dashboard → Existing Trips**: List view → Click trip → Unified Interface

#### Data Flow
```typescript
// User creates trip via AI
POST /itineraries → AgentProgress → NormalizedItinerary

// Load in unified interface
GET /itineraries/{id}/json → UnifiedTripView

// User books hotel from "Bookings" tab
Select Provider → Search → Book → Update itinerary.nodes[].bookingRef
```

---

## 4. Feature Specifications

### 4.1 Homepage Features

#### Search Section
```tsx
<div className="bg-gradient-to-r from-primary to-primary-hover">
  {/* Multi-tab search */}
  <Tabs defaultValue="flights">
    <TabsList>
      <TabsTrigger value="flights">✈️ Flights</TabsTrigger>
      <TabsTrigger value="hotels">🏨 Hotels</TabsTrigger>
      <TabsTrigger value="holidays">🌴 Holidays</TabsTrigger>
      <TabsTrigger value="trains">🚂 Trains</TabsTrigger>
      <TabsTrigger value="bus">🚌 Bus</TabsTrigger>
    </TabsList>
    
    {/* Flight search form */}
    <TabsContent value="flights">
      <FlightSearchForm />
    </TabsContent>
    
    {/* ... other tabs */}
  </Tabs>
  
  {/* Prominent AI Planner CTA */}
  <Button 
    size="lg" 
    variant="secondary" 
    className="mt-6"
    onClick={() => navigate('/ai-planner')}
  >
    🤖 Let AI Plan My Itinerary
  </Button>
</div>
```

#### Trending Destinations Section
```tsx
<section className="py-12">
  <h2 className="text-3xl font-bold mb-6">Trending Destinations</h2>
  <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-6">
    {destinations.map(dest => (
      <TrendingDestinationCard
        key={dest.id}
        image={dest.image}
        name={dest.name}
        startingPrice={dest.price}
        onClick={() => navigateToDestination(dest)}
      />
    ))}
  </div>
</section>
```

### 4.2 AI Trip Planner Flow

#### Wizard Steps (Existing - Restyled)
1. **Destination Selection**
2. **Dates Selection** (Calendar picker)
3. **Party Details** (Adults, children, rooms)
4. **Budget Tier** (Economy, Mid-range, Luxury)
5. **Interests** (Multi-select chips)
6. **Review & Create**

#### Agent Progress (Existing - Enhanced UI)
```tsx
<SimplifiedAgentProgress 
  tripData={createdTrip}
  onComplete={() => navigate(`/trip/${tripId}`)}
  // Enhanced with EaseMyTrip styling
/>
```

### 4.3 Unified Trip Management Interface

#### Sidebar Navigation
```typescript
const navigationItems = [
  { 
    id: 'view', 
    label: 'View', 
    icon: Eye, 
    description: 'Trip overview & summary' 
  },
  { 
    id: 'plan', 
    label: 'Plan', 
    icon: MapPin, 
    tabs: ['Destinations', 'Day by day'],
    description: 'Manage destinations and daily activities'
  },
  { 
    id: 'bookings', 
    label: 'Bookings', 
    icon: ShoppingCart, 
    tabs: ['Flights', 'Hotels', 'Bus/Train', 'Activities'],
    description: 'Search and book across providers',
    NEW: true  // Highlight as new feature
  },
  { 
    id: 'budget', 
    label: 'Budget', 
    icon: DollarSign,
    description: 'Track expenses and costs'
  },
  { 
    id: 'packing', 
    label: 'Packing', 
    icon: Package,
    description: 'Packing list and checklist'
  },
  { 
    id: 'docs', 
    label: 'Docs', 
    icon: FileText,
    description: 'Travel documents and confirmations'
  },
];
```

#### Plan Tab - Destinations View
```tsx
<div className="flex h-screen">
  {/* Left sidebar: Destination list */}
  <div className="w-80 border-r">
    <div className="p-4 border-b">
      <h3 className="font-semibold">Destinations</h3>
      <Tabs defaultValue="destinations">
        <TabsList>
          <TabsTrigger value="destinations">Destinations</TabsTrigger>
          <TabsTrigger value="day-by-day">Day by day</TabsTrigger>
        </TabsList>
      </Tabs>
    </div>
    
    <ScrollArea className="h-[calc(100vh-200px)]">
      {itinerary.days.map(day => (
        <DayCard
          key={day.dayNumber}
          day={day}
          onClick={() => setSelectedDay(day.dayNumber)}
          className={selectedDay === day.dayNumber ? 'bg-muted' : ''}
        />
      ))}
    </ScrollArea>
  </div>
  
  {/* Right content: Day details */}
  <div className="flex-1 p-6">
    <DayDetailView day={selectedDayData} />
  </div>
</div>
```

#### Plan Tab - Day by Day View
```tsx
<div className="space-y-6">
  {/* Day selector tabs */}
  <div className="flex gap-2 overflow-x-auto">
    {[1, 2, 3, 4].map(day => (
      <Button
        key={day}
        variant={selectedDay === day ? 'default' : 'outline'}
        onClick={() => setSelectedDay(day)}
      >
        Day {day}
      </Button>
    ))}
  </div>
  
  {/* Activity cards timeline */}
  <div className="space-y-4">
    {dayNodes.map(node => (
      <NodeCard
        key={node.id}
        node={node}
        type={node.type}
        onBook={() => openBookingModal(node)}
        showBookingButton={!node.bookingRef}
      />
    ))}
  </div>
</div>
```

### 4.4 Bookings Tab (NEW FEATURE)

#### Layout Structure
```tsx
<div className="flex h-screen">
  {/* Left sidebar: Provider list */}
  <div className="w-64 border-r bg-muted/30">
    <div className="p-4 border-b">
      <h3 className="font-semibold">Select Provider</h3>
    </div>
    
    <ScrollArea>
      {/* Flight providers */}
      <div className="p-4">
        <h4 className="text-sm font-medium mb-2">Flights</h4>
        <ProviderButton icon={BookingIcon} name="Booking.com" onClick={() => setProvider('booking')} />
        <ProviderButton icon={ExpediaIcon} name="Expedia" onClick={() => setProvider('expedia')} />
        <ProviderButton icon={AgodaIcon} name="Agoda" onClick={() => setProvider('agoda')} />
      </div>
      
      {/* Hotel providers */}
      <div className="p-4">
        <h4 className="text-sm font-medium mb-2">Hotels</h4>
        <ProviderButton icon={HotelsIcon} name="Hotels.com" />
        <ProviderButton icon={VioIcon} name="Vio.com" />
        <ProviderButton icon={TripIcon} name="Trip.com" />
        <ProviderButton icon={HostelIcon} name="Hostelworld" />
        <ProviderButton icon={AirbnbIcon} name="Airbnb" />
      </div>
      
      {/* Transport providers */}
      <div className="p-4">
        <h4 className="text-sm font-medium mb-2">Transport</h4>
        <ProviderButton icon={TrainIcon} name="RailYatra" />
        <ProviderButton icon={BusIcon} name="RedBus" />
      </div>
    </ScrollArea>
  </div>
  
  {/* Right content: Provider search/results */}
  <div className="flex-1 p-6">
    {selectedProvider ? (
      <ProviderSearchInterface 
        provider={selectedProvider}
        itinerary={itinerary}
        onBook={handleBooking}
      />
    ) : (
      <EmptyState 
        title="Select a provider to get started"
        description="Choose from popular booking platforms to search for your travel needs"
      />
    )}
  </div>
</div>
```

#### Provider Card Grid (Alternative View)
```tsx
{/* When no destination selected, show all providers in grid */}
<div className="grid grid-cols-2 md:grid-cols-4 gap-4">
  {providers.map(provider => (
    <Card 
      key={provider.id}
      className="cursor-pointer hover:shadow-lg transition-shadow"
      onClick={() => openProviderSearch(provider)}
    >
      <CardContent className="p-6 text-center">
        <img src={provider.logo} alt={provider.name} className="h-12 mx-auto mb-3" />
        <h4 className="font-medium">{provider.name}</h4>
        <p className="text-xs text-muted-foreground mt-1">
          {provider.dates}
        </p>
        <Button size="sm" className="mt-3">
          Search {provider.type}
        </Button>
      </CardContent>
    </Card>
  ))}
</div>
```

#### Booking Flow
```typescript
// 1. User selects provider (e.g., Booking.com)
setSelectedProvider('booking');

// 2. Show provider-specific search form
<BookingSearchForm 
  provider="booking"
  destination={currentDestination}
  dates={itinerary.startDate, itinerary.endDate}
  guests={itinerary.party}
/>

// 3. Mock results (hardcoded)
const mockResults = [
  {
    id: 'hotel-1',
    name: 'Hotel Paradise',
    rating: 4.5,
    price: 5000,
    image: '/images/hotels/paradise.jpg',
    provider: 'booking'
  },
  // ... more results
];

// 4. User clicks "Book Now"
<Button onClick={() => handleMockBooking(hotel)}>
  Book on {provider.name}
</Button>

// 5. Update itinerary with booking reference
const updatedNode = {
  ...node,
  bookingRef: `${provider}-${hotel.id}`,
  status: 'booked',
  bookedVia: provider,
  bookingDetails: hotel
};

// 6. Save to backend
await apiClient.applyChanges(itineraryId, {
  ops: [{ op: 'update', id: node.id, node: updatedNode }]
});
```

---

## 5. Page-by-Page Requirements

### 5.1 Homepage (`/`)

#### Header
```tsx
<header className="sticky top-0 z-50 bg-white shadow-sm">
  <div className="container mx-auto px-4">
    <div className="flex items-center justify-between h-16">
      {/* Logo */}
      <Link to="/">
        <img src="/logo.svg" alt="EasyTrip" className="h-8" />
      </Link>
      
      {/* Navigation */}
      <nav className="hidden md:flex gap-6">
        <NavLink to="/flights">Flights</NavLink>
        <NavLink to="/hotels">Hotels</NavLink>
        <NavLink to="/holidays">Holidays</NavLink>
        <NavLink to="/trains">Trains</NavLink>
        <NavLink to="/bus">Bus</NavLink>
      </nav>
      
      {/* Right actions */}
      <div className="flex items-center gap-4">
        {isAuthenticated ? (
          <>
            <Button variant="ghost" onClick={() => navigate('/dashboard')}>
              My Trips
            </Button>
            <UserMenu />
          </>
        ) : (
          <Button onClick={() => navigate('/login')}>
            Sign In
          </Button>
        )}
      </div>
    </div>
  </div>
</header>
```

#### Hero Section
```tsx
<section className="bg-gradient-to-r from-primary via-primary-hover to-primary pt-12 pb-16">
  <div className="container mx-auto px-4">
    <h1 className="text-4xl md:text-5xl font-bold text-white text-center mb-8">
      Plan Your Perfect Trip
    </h1>
    
    {/* Search widget */}
    <Card className="max-w-4xl mx-auto">
      <CardContent className="p-6">
        <SearchWidget />
      </CardContent>
    </Card>
    
    {/* AI Planner CTA */}
    <div className="text-center mt-8">
      <Button 
        size="lg" 
        variant="secondary"
        className="text-lg px-8 py-6"
        onClick={() => navigate('/ai-planner')}
      >
        <Sparkles className="mr-2" />
        Let AI Plan My Itinerary
      </Button>
      <p className="text-white/90 text-sm mt-2">
        Get a personalized itinerary in minutes
      </p>
    </div>
  </div>
</section>
```

#### Trending Destinations
```tsx
<section className="py-16 bg-gray-50">
  <div className="container mx-auto px-4">
    <h2 className="text-3xl font-bold mb-8">Trending Destinations</h2>
    
    <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-6">
      {MOCK_DESTINATIONS.map(dest => (
        <Card key={dest.id} className="overflow-hidden hover:shadow-lg transition-shadow cursor-pointer">
          <div className="aspect-video relative">
            <img src={dest.image} alt={dest.name} className="w-full h-full object-cover" />
            <Badge className="absolute top-2 right-2 bg-secondary">
              Popular
            </Badge>
          </div>
          <CardContent className="p-4">
            <h3 className="font-semibold text-lg">{dest.name}</h3>
            <p className="text-sm text-muted-foreground">{dest.description}</p>
            <div className="mt-3 flex items-center justify-between">
              <span className="text-sm">Starting from</span>
              <span className="text-xl font-bold text-primary">
                ₹{dest.startingPrice.toLocaleString()}
              </span>
            </div>
            <Button className="w-full mt-3" onClick={() => navigateToDestination(dest)}>
              Explore
            </Button>
          </CardContent>
        </Card>
      ))}
    </div>
  </div>
</section>
```

### 5.2 AI Trip Planner (`/ai-planner`)

**Flow:** Wizard → Generation Progress → Unified Trip View

#### Wizard (Existing - Restyled)
- Keep existing wizard logic from `SimplifiedTripWizard`
- Apply EaseMyTrip design system (colors, spacing, typography)
- Enhance transitions between steps
- Add progress indicator

#### Generation Progress (Existing - Restyled)
- Keep `SimplifiedAgentProgress` functionality
- Apply EaseMyTrip styling
- Enhance progress animation
- Add motivational messages

### 5.3 Unified Trip View (`/trip/:id`)

**Primary Interface for All Created Trips**

#### Layout
```tsx
<div className="flex h-screen bg-gray-50">
  {/* Sidebar */}
  <TripSidebar 
    activeTab={activeTab}
    onTabChange={setActiveTab}
    trip={trip}
  />
  
  {/* Main content */}
  <main className="flex-1 overflow-auto">
    {activeTab === 'view' && <TripOverview trip={trip} />}
    {activeTab === 'plan' && <PlanView trip={trip} />}
    {activeTab === 'bookings' && <BookingsView trip={trip} />}
    {activeTab === 'budget' && <BudgetView trip={trip} />}
    {activeTab === 'packing' && <PackingView trip={trip} />}
    {activeTab === 'docs' && <DocsView trip={trip} />}
  </main>
</div>
```

### 5.4 Dashboard (`/dashboard`)

#### Trip List
```tsx
<div className="container mx-auto px-4 py-8">
  <div className="flex items-center justify-between mb-6">
    <h1 className="text-3xl font-bold">My Trips</h1>
    <Button onClick={() => navigate('/ai-planner')}>
      <Plus className="mr-2" />
      Create New Trip
    </Button>
  </div>
  
  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
    {trips.map(trip => (
      <TripCard
        key={trip.id}
        trip={trip}
        onClick={() => navigate(`/trip/${trip.id}`)}
      />
    ))}
  </div>
</div>
```

---

## 6. Component Specifications

### 6.1 Core UI Components

#### Button Variants
```typescript
// components/ui/button.tsx
const buttonVariants = cva(
  "inline-flex items-center justify-center rounded-md font-medium transition-colors focus-visible:outline-none disabled:opacity-50",
  {
    variants: {
      variant: {
        primary: "bg-primary hover:bg-primary-hover text-primary-foreground shadow",
        secondary: "bg-secondary hover:bg-secondary-hover text-secondary-foreground",
        outline: "border-2 border-primary text-primary hover:bg-primary/10",
        ghost: "hover:bg-muted hover:text-foreground",
        link: "text-primary underline-offset-4 hover:underline",
      },
      size: {
        sm: "h-9 px-3 text-sm",
        md: "h-10 px-4 text-base",
        lg: "h-12 px-6 text-lg",
        xl: "h-14 px-8 text-xl",
        icon: "h-10 w-10",
      },
    },
    defaultVariants: {
      variant: "primary",
      size: "md",
    },
  }
);
```

#### Card Components
```typescript
// NodeCard.tsx - Generic card for all node types
interface NodeCardProps {
  node: NormalizedNode;
  type: 'attraction' | 'meal' | 'hotel' | 'transit';
  showBookingButton?: boolean;
  onBook?: () => void;
  onEdit?: () => void;
  onDelete?: () => void;
}

export function NodeCard({ node, type, showBookingButton, onBook }: NodeCardProps) {
  // Type-specific styling
  const typeStyles = {
    attraction: { icon: MapPin, color: 'text-blue-600', bg: 'bg-blue-50' },
    meal: { icon: Utensils, color: 'text-orange-600', bg: 'bg-orange-50' },
    hotel: { icon: Hotel, color: 'text-purple-600', bg: 'bg-purple-50' },
    transit: { icon: Train, color: 'text-green-600', bg: 'bg-green-50' },
  }[type];
  
  return (
    <Card className="hover:shadow-md transition-shadow">
      <CardContent className="p-4">
        <div className="flex items-start gap-3">
          {/* Icon */}
          <div className={cn("p-2 rounded-lg", typeStyles.bg)}>
            <typeStyles.icon className={cn("h-5 w-5", typeStyles.color)} />
          </div>
          
          {/* Content */}
          <div className="flex-1">
            <h4 className="font-semibold">{node.title}</h4>
            {node.timing && (
              <p className="text-sm text-muted-foreground">
                {formatTime(node.timing.startTime)} - {formatTime(node.timing.endTime)}
              </p>
            )}
            {node.location && (
              <p className="text-sm text-muted-foreground mt-1">
                📍 {node.location.name}
              </p>
            )}
          </div>
          
          {/* Actions */}
          <div className="flex flex-col gap-2">
            {showBookingButton && (
              <Button size="sm" onClick={onBook}>
                Book Now
              </Button>
            )}
            {node.bookingRef && (
              <Badge variant="success">Booked</Badge>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
```

#### Provider Button
```typescript
// ProviderButton.tsx
interface ProviderButtonProps {
  icon: string;  // URL to provider logo
  name: string;
  onClick: () => void;
  isActive?: boolean;
}

export function ProviderButton({ icon, name, onClick, isActive }: ProviderButtonProps) {
  return (
    <Button
      variant={isActive ? "default" : "ghost"}
      className="w-full justify-start gap-3 mb-2"
      onClick={onClick}
    >
      <img src={icon} alt={name} className="h-5 w-5" />
      <span>{name}</span>
      {isActive && <Check className="ml-auto h-4 w-4" />}
    </Button>
  );
}
```

### 6.2 Feature Components

#### SearchWidget
```typescript
// SearchWidget.tsx
export function SearchWidget() {
  const [searchType, setSearchType] = useState('flights');
  
  return (
    <Tabs value={searchType} onValueChange={setSearchType}>
      <TabsList className="grid w-full grid-cols-5">
        <TabsTrigger value="flights">✈️ Flights</TabsTrigger>
        <TabsTrigger value="hotels">🏨 Hotels</TabsTrigger>
        <TabsTrigger value="holidays">🌴 Holidays</TabsTrigger>
        <TabsTrigger value="trains">🚂 Trains</TabsTrigger>
        <TabsTrigger value="bus">🚌 Bus</TabsTrigger>
      </TabsList>
      
      <TabsContent value="flights">
        <FlightSearchForm />
      </TabsContent>
      
      {/* ... other tabs */}
    </Tabs>
  );
}
```

#### TripSidebar
```typescript
// TripSidebar.tsx
export function TripSidebar({ activeTab, onTabChange, trip }: TripSidebarProps) {
  return (
    <aside className="w-64 bg-white border-r flex flex-col">
      {/* Trip header */}
      <div className="p-4 border-b">
        <Button variant="ghost" size="icon" onClick={() => navigate('/dashboard')}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <h2 className="font-semibold mt-2 truncate">{trip.destination}</h2>
        <p className="text-sm text-muted-foreground">
          {formatDateRange(trip.startDate, trip.endDate)}
        </p>
      </div>
      
      {/* Navigation items */}
      <ScrollArea className="flex-1">
        {navigationItems.map(item => (
          <SidebarNavItem
            key={item.id}
            item={item}
            isActive={activeTab === item.id}
            onClick={() => onTabChange(item.id)}
          />
        ))}
      </ScrollArea>
      
      {/* Footer actions */}
      <div className="p-4 border-t space-y-2">
        <Button variant="outline" className="w-full">
          <Share2 className="mr-2 h-4 w-4" />
          Share Trip
        </Button>
        <Button variant="outline" className="w-full">
          <Download className="mr-2 h-4 w-4" />
          Export PDF
        </Button>
      </div>
    </aside>
  );
}
```

---

## 7. Data Flow & Backend Integration

### 7.1 API Integration Points

**No changes to existing API contracts** - frontend adapts to existing backend.

#### Itinerary APIs (Existing)
```typescript
// CREATE new itinerary via AI
POST /itineraries
Request: CreateItineraryRequest
Response: { itinerary: ItineraryResponse, executionId: string }

// FETCH itinerary data
GET /itineraries/{id}/json
Response: NormalizedItinerary

// APPLY changes (e.g., booking updates)
POST /itineraries/{id}:apply
Request: { ops: ChangeOperation[] }
Response: { itinerary: NormalizedItinerary }
```

#### Real-time Updates (Existing)
```typescript
// WebSocket connection for agent progress
WebSocket: /topic/agent/{itineraryId}
Message: { type: 'agent_progress', progress: 75, status: 'running' }

// WebSocket for itinerary changes
WebSocket: /topic/itinerary/{itineraryId}
Message: { type: 'itinerary_updated', itinerary: NormalizedItinerary }
```

### 7.2 Mock Data for Providers

**Booking providers are HARDCODED (not real integrations)**

```typescript
// mockProviders.ts
export const MOCK_PROVIDERS = {
  flights: [
    { id: 'booking', name: 'Booking.com', logo: '/logos/booking.png' },
    { id: 'expedia', name: 'Expedia', logo: '/logos/expedia.png' },
    { id: 'agoda', name: 'Agoda', logo: '/logos/agoda.png' },
  ],
  hotels: [
    { id: 'hotels-com', name: 'Hotels.com', logo: '/logos/hotels.png' },
    { id: 'vio', name: 'Vio.com', logo: '/logos/vio.png' },
    { id: 'trip', name: 'Trip.com', logo: '/logos/trip.png' },
    { id: 'hostelworld', name: 'Hostelworld', logo: '/logos/hostelworld.png' },
    { id: 'airbnb', name: 'Airbnb', logo: '/logos/airbnb.png' },
  ],
  transport: [
    { id: 'railyatra', name: 'RailYatra', logo: '/logos/railyatra.png' },
    { id: 'redbus', name: 'RedBus', logo: '/logos/redbus.png' },
  ],
};

export const MOCK_HOTEL_RESULTS = [
  {
    id: 'hotel-1',
    name: 'Hotel Paradise',
    rating: 4.5,
    reviews: 1234,
    price: 5000,
    currency: 'INR',
    image: '/images/hotels/paradise.jpg',
    amenities: ['WiFi', 'Pool', 'Spa'],
    provider: 'booking',
  },
  // ... more results
];
```

---

## 8. Mobile & Responsive Design

### 8.1 Breakpoints

```css
/* Tailwind breakpoints */
sm: 640px   /* Mobile landscape */
md: 768px   /* Tablet */
lg: 1024px  /* Desktop */
xl: 1280px  /* Large desktop */
2xl: 1536px /* Extra large */
```

### 8.2 Mobile Navigation

#### Bottom Tab Bar (Mobile)
```tsx
<div className="md:hidden fixed bottom-0 left-0 right-0 bg-white border-t z-50">
  <div className="flex justify-around py-2">
    <MobileNavButton icon={Eye} label="View" active={activeTab === 'view'} />
    <MobileNavButton icon={MapPin} label="Plan" active={activeTab === 'plan'} />
    <MobileNavButton icon={ShoppingCart} label="Bookings" active={activeTab === 'bookings'} />
    <MobileNavButton icon={DollarSign} label="Budget" active={activeTab === 'budget'} />
    <MobileNavButton icon={Menu} label="More" onClick={openMobileMenu} />
  </div>
</div>
```

### 8.3 Touch Interactions

- **Swipe**: Day-to-day navigation
- **Tap**: Card selection
- **Long press**: Context menu
- **Pull to refresh**: Reload itinerary

---

## 9. Implementation Roadmap

### 9.1 Phase 1: Foundation (Week 1-2)

#### Week 1: Design System
- [ ] Setup color system in `index.css` and `tailwind.config.ts`
- [ ] Create design tokens documentation
- [ ] Build core UI components (Button, Card, Input, etc.)
- [ ] Setup typography and spacing
- [ ] Create Storybook stories for components

#### Week 2: Core Layout
- [ ] Build homepage shell
- [ ] Create unified trip view layout
- [ ] Implement sidebar navigation
- [ ] Setup routing structure
- [ ] Create mobile layout variants

### 9.2 Phase 2: Core Features (Week 3-4)

#### Week 3: AI Planner
- [ ] Restyle wizard with EaseMyTrip theme
- [ ] Enhance agent progress UI
- [ ] Integrate with existing backend
- [ ] Test complete AI flow

#### Week 4: Trip Management
- [ ] Build "View" tab (overview)
- [ ] Build "Plan" tab (destinations + day-by-day)
- [ ] Integrate itinerary data
- [ ] Add edit/delete functionality

### 9.3 Phase 3: Bookings (Week 5-6)

#### Week 5: Bookings Tab
- [ ] Build provider sidebar
- [ ] Create provider selection UI
- [ ] Build provider card grid
- [ ] Add mock provider data

#### Week 6: Booking Flow
- [ ] Build search forms per provider
- [ ] Create mock results display
- [ ] Implement "Book Now" flow
- [ ] Update itinerary with bookings

### 9.4 Phase 4: Polish (Week 7-8)

#### Week 7: Secondary Features
- [ ] Build budget tab
- [ ] Build packing tab
- [ ] Build docs tab
- [ ] Dashboard trip list

#### Week 8: Final Polish
- [ ] Animations and transitions
- [ ] Performance optimization
- [ ] Mobile testing and fixes
- [ ] Accessibility audit
- [ ] Production deployment

---

## 10. Quality Standards

### 10.1 Performance Targets

- **Initial Load**: < 2 seconds
- **Time to Interactive**: < 2.5 seconds
- **Lighthouse Score**: > 90
- **Bundle Size**: < 300KB (gzipped)

### 10.2 Browser Support

- Chrome (last 2 versions)
- Firefox (last 2 versions)
- Safari (last 2 versions)
- Edge (last 2 versions)
- Mobile Safari, Chrome Mobile

### 10.3 Accessibility

- **WCAG 2.1 Level AA** compliance
- Keyboard navigation for all interactions
- Screen reader support (ARIA labels)
- Color contrast ratios 4.5:1 minimum
- Focus indicators visible

### 10.4 Code Quality

- **TypeScript**: Strict mode enabled
- **ESLint**: Zero errors, < 5 warnings
- **Prettier**: Consistent formatting
- **Component Size**: < 300 lines per file
- **Test Coverage**: > 70% overall

---

## Appendix A: Mock Data Structure

### Trending Destinations
```typescript
export const MOCK_DESTINATIONS = [
  {
    id: 'goa',
    name: 'Goa',
    description: 'Beach paradise',
    image: '/images/destinations/goa.jpg',
    startingPrice: 15000,
    popular: true,
  },
  // ... 12 total destinations
];
```

### Provider Logos
```
public/logos/
├── booking.png
├── expedia.png
├── agoda.png
├── hotels.png
├── vio.png
├── trip.png
├── hostelworld.png
├── airbnb.png
├── railyatra.png
└── redbus.png
```

---

## Appendix B: Component Checklist

### UI Primitives
- [ ] Button (5 variants, 4 sizes)
- [ ] Card (with header, content, footer)
- [ ] Input (text, email, tel, search)
- [ ] Select (single, multi, searchable)
- [ ] Tabs (horizontal, vertical)
- [ ] Badge (status colors)
- [ ] Avatar (with fallback)
- [ ] Progress (linear, circular)
- [ ] Skeleton (loading states)
- [ ] Toast (success, error, info, warning)

### Feature Components
- [ ] SearchWidget (5 search types)
- [ ] TrendingDestinationCard
- [ ] TripCard (dashboard)
- [ ] NodeCard (activity, meal, hotel, transport)
- [ ] ProviderButton
- [ ] ProviderCard
- [ ] DayCard
- [ ] TripSidebar
- [ ] MobileNavBar
- [ ] BookingModal

---

## Questions & Clarifications

Before implementation begins, confirm:

1. ✅ **Color confirmation**: Are the extracted EaseMyTrip colors accurate?
2. ✅ **Provider logos**: Do we have permission to use provider logos, or should we use text names only?
3. ✅ **Mock booking**: Should clicking "Book Now" open provider website in new tab, or stay in our app with mock confirmation?
4. ✅ **Mobile priority**: Should we prioritize mobile-first, desktop-first, or concurrent development?
5. ✅ **Animation level**: How much motion? (subtle, moderate, high-energy)

---

**End of Specification Document**

This document provides 100% clarity on all requirements. Implementation should follow this specification exactly, with any deviations requiring explicit approval.
