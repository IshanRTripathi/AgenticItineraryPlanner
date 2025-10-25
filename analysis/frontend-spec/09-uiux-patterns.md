# 9. UI/UX Patterns

**Last Updated:** January 25, 2025  
**Purpose:** Document navigation, layouts, loading states, forms, interactions, and responsive design patterns

---

## 9.1 Navigation & Routing

### 9.1.1 Route Structure

**All Routes:**

| Path | Component | Protection | Purpose |
|------|-----------|------------|---------|
| `/` | LandingPage | Public | Landing page |
| `/login` | LoginPage | Public | Authentication |
| `/wizard` | SimplifiedTripWizard | Protected | Trip creation |
| `/generating` | SimplifiedAgentProgress | Protected + Trip | Generation progress |
| `/planner` | TravelPlanner | Protected + Trip | Main planner |
| `/cost` | CostAndCart | Protected + Trip | Shopping cart |
| `/checkout` | Checkout | Protected + Trip | Payment |
| `/confirmation` | BookingConfirmation | Protected + Trip | Booking success |
| `/share` | ShareView | Protected + Trip | Share options |
| `/dashboard` | TripDashboard | Protected | Trip list |
| `/trip/:id` | TripRouteLoader | Protected | Load trip |
| `/itinerary/:id` | ItineraryRouteLoader | Protected | Load itinerary |
| `/itinerary/:id/chat` | ItineraryWithChat | Protected | Chat interface |

### 9.1.2 Navigation Components

#### GlobalNavigation
**Variants:**
- Horizontal (desktop header)
- Vertical (mobile sidebar)

**Items:**
- Overview
- Plan
- Map
- Workflow
- Chat
- Settings

#### NavigationSidebar (TravelPlanner)
**Structure:**
```
Logo
├── View Tabs
│   ├── Overview
│   ├── Day-by-Day
│   ├── Map
│   ├── Workflow
│   └── Chat
├── Actions
│   ├── Save
│   ├── Share
│   ├── Export
│   └── Settings
└── User Menu
```

#### Breadcrumbs
**Pattern:**
```
Dashboard > My Trips > Barcelona Trip > Day 2
```

### 9.1.3 Programmatic Navigation

```typescript
const navigate = useNavigate();

// Simple navigation
navigate('/dashboard');

// With state
navigate('/planner', { state: { tripId: 'it_123' } });

// Replace (no history)
navigate('/login', { replace: true });

// Go back
navigate(-1);
```

---

## 9.2 Layout Patterns

### 9.2.1 App Layout

**Desktop Layout:**
```
┌─────────────────────────────────────┐
│         TopNavigation               │
├──────┬──────────────────────────────┤
│      │                              │
│ Side │      Main Content            │
│ bar  │                              │
│      │                              │
└──────┴──────────────────────────────┘
```

**Mobile Layout:**
```
┌─────────────────────┐
│   TopNavigation     │
├─────────────────────┤
│                     │
│   Main Content      │
│   (Full Width)      │
│                     │
├─────────────────────┤
│  Bottom Navigation  │
└─────────────────────┘
```

### 9.2.2 Sidebar Patterns

**Collapsible Sidebar:**
```typescript
const [collapsed, setCollapsed] = useState(false);

<aside className={collapsed ? 'w-16' : 'w-64'}>
  {/* Sidebar content */}
</aside>
```

**Resizable Sidebar:**
```typescript
import { ResizablePanel } from 'react-resizable-panels';

<ResizablePanel defaultSize={20} minSize={15} maxSize={30}>
  {/* Sidebar content */}
</ResizablePanel>
```

### 9.2.3 Modal Patterns

**Dialog Modal:**
```typescript
<Dialog open={isOpen} onOpenChange={setIsOpen}>
  <DialogContent>
    <DialogHeader>
      <DialogTitle>Modal Title</DialogTitle>
      <DialogDescription>Description</DialogDescription>
    </DialogHeader>
    {/* Content */}
    <DialogFooter>
      <Button onClick={() => setIsOpen(false)}>Cancel</Button>
      <Button onClick={handleSave}>Save</Button>
    </DialogFooter>
  </DialogContent>
</Dialog>
```

**Sheet (Side Panel):**
```typescript
<Sheet open={isOpen} onOpenChange={setIsOpen}>
  <SheetContent side="right">
    <SheetHeader>
      <SheetTitle>Panel Title</SheetTitle>
    </SheetHeader>
    {/* Content */}
  </SheetContent>
</Sheet>
```

### 9.2.4 Split View Patterns

**Horizontal Split:**
```typescript
<div className="flex h-screen">
  <div className="w-1/2 border-r">
    {/* Left pane */}
  </div>
  <div className="w-1/2">
    {/* Right pane */}
  </div>
</div>
```

**Vertical Split:**
```typescript
<div className="flex flex-col h-screen">
  <div className="h-1/2 border-b">
    {/* Top pane */}
  </div>
  <div className="h-1/2">
    {/* Bottom pane */}
  </div>
</div>
```

---

## 9.3 Loading & Error States

### 9.3.1 Loading Patterns

#### Skeleton Loader
```typescript
<div className="space-y-4">
  <Skeleton className="h-12 w-full" />
  <Skeleton className="h-32 w-full" />
  <Skeleton className="h-8 w-3/4" />
</div>
```

#### Spinner
```typescript
<LoadingState 
  variant="fullPage" 
  message="Loading itinerary..." 
  size="lg" 
/>
```

#### Progress Bar
```typescript
<Progress value={progress} className="w-full" />
<p className="text-sm text-gray-600">{progress}% complete</p>
```

#### Suspense Boundaries
```typescript
<Suspense fallback={<LoadingState variant="inline" />}>
  <LazyComponent />
</Suspense>
```

### 9.3.2 Error Patterns

#### Error Display
```typescript
<ErrorDisplay
  title="Failed to load itinerary"
  message={error.message}
  onRetry={refetch}
  showDetails={isDevelopment}
/>
```

#### Error Boundary
```typescript
<ErrorBoundary
  fallback={<ErrorFallback />}
  onError={(error, errorInfo) => {
    logger.error('Component error', { error, errorInfo });
  }}
>
  <Component />
</ErrorBoundary>
```

#### Toast Notifications
```typescript
import { toast } from 'sonner';

// Success
toast.success('Itinerary saved successfully');

// Error
toast.error('Failed to save itinerary');

// Loading
const toastId = toast.loading('Saving...');
// Later: toast.success('Saved!', { id: toastId });

// Custom
toast.custom((t) => (
  <div className="bg-white p-4 rounded-lg shadow-lg">
    <p>Custom notification</p>
    <button onClick={() => toast.dismiss(t)}>Dismiss</button>
  </div>
));
```

### 9.3.3 Empty States

```typescript
<AutoRefreshEmptyState
  title="No trips yet"
  description="Create your first trip to get started"
  action={
    <Button onClick={() => navigate('/wizard')}>
      Create Trip
    </Button>
  }
  icon={<MapPin className="h-12 w-12 text-gray-400" />}
/>
```

---

## 9.4 Forms & Validation

### 9.4.1 Form Pattern (react-hook-form)

```typescript
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';

const schema = z.object({
  destination: z.string().min(1, 'Destination is required'),
  startDate: z.string().min(1, 'Start date is required'),
  endDate: z.string().min(1, 'End date is required'),
});

function TripForm() {
  const form = useForm({
    resolver: zodResolver(schema),
    defaultValues: {
      destination: '',
      startDate: '',
      endDate: '',
    }
  });

  const onSubmit = (data) => {
    createTrip(data);
  };

  return (
    <form onSubmit={form.handleSubmit(onSubmit)}>
      <div>
        <Label htmlFor="destination">Destination</Label>
        <Input
          id="destination"
          {...form.register('destination')}
        />
        {form.formState.errors.destination && (
          <p className="text-sm text-red-600">
            {form.formState.errors.destination.message}
          </p>
        )}
      </div>
      
      <Button type="submit" disabled={form.formState.isSubmitting}>
        {form.formState.isSubmitting ? 'Creating...' : 'Create Trip'}
      </Button>
    </form>
  );
}
```

### 9.4.2 Form Components

**Input with Label:**
```typescript
<div className="space-y-2">
  <Label htmlFor="email">Email</Label>
  <Input
    id="email"
    type="email"
    placeholder="you@example.com"
    {...register('email')}
  />
  {errors.email && (
    <p className="text-sm text-red-600">{errors.email.message}</p>
  )}
</div>
```

**Select:**
```typescript
<Select onValueChange={(value) => setValue('budget', value)}>
  <SelectTrigger>
    <SelectValue placeholder="Select budget" />
  </SelectTrigger>
  <SelectContent>
    <SelectItem value="budget">Budget</SelectItem>
    <SelectItem value="mid-range">Mid-range</SelectItem>
    <SelectItem value="luxury">Luxury</SelectItem>
  </SelectContent>
</Select>
```

**Checkbox:**
```typescript
<div className="flex items-center space-x-2">
  <Checkbox
    id="terms"
    checked={acceptedTerms}
    onCheckedChange={setAcceptedTerms}
  />
  <Label htmlFor="terms">Accept terms and conditions</Label>
</div>
```

### 9.4.3 Validation Patterns

**Inline Validation:**
```typescript
<Input
  {...register('email', {
    required: 'Email is required',
    pattern: {
      value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
      message: 'Invalid email address'
    }
  })}
/>
```

**Async Validation:**
```typescript
const validateUsername = async (value) => {
  const exists = await checkUsernameExists(value);
  return !exists || 'Username already taken';
};

<Input
  {...register('username', {
    validate: validateUsername
  })}
/>
```

---

## 9.5 Interactive Elements

### 9.5.1 Drag and Drop

**ReactFlow (Workflow):**
```typescript
<ReactFlow
  nodes={nodes}
  edges={edges}
  onNodesChange={onNodesChange}
  onEdgesChange={onEdgesChange}
  onConnect={onConnect}
  nodesDraggable={true}
/>
```

**Sortable List:**
```typescript
import { DndContext, closestCenter } from '@dnd-kit/core';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';

<DndContext collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
  <SortableContext items={items} strategy={verticalListSortingStrategy}>
    {items.map(item => (
      <SortableItem key={item.id} id={item.id} />
    ))}
  </SortableContext>
</DndContext>
```

### 9.5.2 Context Menus

```typescript
<ContextMenu>
  <ContextMenuTrigger>
    <div>Right-click me</div>
  </ContextMenuTrigger>
  <ContextMenuContent>
    <ContextMenuItem onClick={handleEdit}>Edit</ContextMenuItem>
    <ContextMenuItem onClick={handleDuplicate}>Duplicate</ContextMenuItem>
    <ContextMenuSeparator />
    <ContextMenuItem onClick={handleDelete} className="text-red-600">
      Delete
    </ContextMenuItem>
  </ContextMenuContent>
</ContextMenu>
```

### 9.5.3 Tooltips

```typescript
<Tooltip>
  <TooltipTrigger asChild>
    <Button variant="ghost" size="icon">
      <Info className="h-4 w-4" />
    </Button>
  </TooltipTrigger>
  <TooltipContent>
    <p>Additional information</p>
  </TooltipContent>
</Tooltip>
```

### 9.5.4 Popovers

```typescript
<Popover>
  <PopoverTrigger asChild>
    <Button variant="outline">Open</Button>
  </PopoverTrigger>
  <PopoverContent className="w-80">
    <div className="space-y-2">
      <h4 className="font-medium">Popover Title</h4>
      <p className="text-sm text-gray-600">Content goes here</p>
    </div>
  </PopoverContent>
</Popover>
```

### 9.5.5 Keyboard Shortcuts

```typescript
useKeyboardShortcut('ctrl+s', (e) => {
  e.preventDefault();
  saveItinerary();
});

useKeyboardShortcut('ctrl+z', () => {
  undo();
});

useKeyboardShortcut('ctrl+shift+z', () => {
  redo();
});
```

---

## 9.6 Responsive Design

### 9.6.1 Breakpoints

**Tailwind Breakpoints:**
```css
sm: 640px   /* Small tablets */
md: 768px   /* Tablets */
lg: 1024px  /* Small desktops */
xl: 1280px  /* Large desktops */
2xl: 1536px /* Extra large */
```

**Usage:**
```typescript
<div className="
  w-full 
  sm:w-1/2 
  md:w-1/3 
  lg:w-1/4
">
  {/* Responsive width */}
</div>
```

### 9.6.2 Mobile-Specific Components

**Mobile Layout:**
```typescript
const { isMobile } = useDeviceDetection();

return isMobile ? (
  <MobileLayout />
) : (
  <DesktopLayout />
);
```

**Mobile Navigation:**
```typescript
<div className="md:hidden">
  {/* Mobile navigation */}
  <Sheet>
    <SheetTrigger>
      <Menu className="h-6 w-6" />
    </SheetTrigger>
    <SheetContent side="left">
      <NavigationMenu />
    </SheetContent>
  </Sheet>
</div>

<div className="hidden md:block">
  {/* Desktop navigation */}
  <NavigationSidebar />
</div>
```

### 9.6.3 Touch Optimizations

**Touch Targets:**
```typescript
// Minimum 44px touch target
<button className="min-h-[44px] min-w-[44px] p-3">
  <Icon className="h-6 w-6" />
</button>
```

**Swipe Gestures:**
```typescript
const { onTouchStart, onTouchMove, onTouchEnd } = useSwipeGesture({
  onSwipeLeft: () => nextDay(),
  onSwipeRight: () => previousDay(),
  threshold: 50
});

<div
  onTouchStart={onTouchStart}
  onTouchMove={onTouchMove}
  onTouchEnd={onTouchEnd}
>
  {/* Swipeable content */}
</div>
```

### 9.6.4 Responsive Typography

```typescript
<h1 className="
  text-2xl 
  sm:text-3xl 
  md:text-4xl 
  lg:text-5xl 
  font-bold
">
  Responsive Heading
</h1>
```

### 9.6.5 Responsive Images

```typescript
<img
  src={image.url}
  srcSet={`
    ${image.small} 640w,
    ${image.medium} 1024w,
    ${image.large} 1920w
  `}
  sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
  alt={image.alt}
  loading="lazy"
/>
```

---

**[← Back to Main Specification](../FRONTEND_UI_REDESIGN_SPECIFICATION.md)** | **[Next: Third-Party Integrations →](10-third-party-integrations.md)**
