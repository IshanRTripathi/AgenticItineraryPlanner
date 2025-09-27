# Mobile Development Guidelines
## Agentic Itinerary Planner

This document provides comprehensive guidelines for developing mobile-responsive features in the Agentic Itinerary Planner application.

---

## 📱 **Mobile-First Development Principles**

### **1. Mobile-First Design Approach**
- **Start with mobile**: Design and develop for mobile devices first, then enhance for larger screens
- **Progressive enhancement**: Add features and complexity for larger screens
- **Touch-first interactions**: Prioritize touch interactions over mouse interactions

### **2. Responsive Breakpoints**
```typescript
const MOBILE_BREAKPOINTS = {
  xs: 0,      // Mobile phones (portrait)
  sm: 640,    // Mobile phones (landscape)
  md: 768,    // Tablets
  lg: 1024,   // Small desktops
  xl: 1280,   // Large desktops
  '2xl': 1536 // Extra large desktops
};
```

### **3. Touch Target Guidelines**
- **Minimum size**: 44px × 44px for all interactive elements
- **Spacing**: Minimum 8px between touch targets
- **Visual feedback**: Provide immediate visual feedback for touch interactions

---

## 🎨 **Component Development Guidelines**

### **1. Layout Components**

#### **Sidebar Navigation**
```tsx
// ✅ Good: Mobile-responsive sidebar
<Sidebar variant="sidebar" collapsible="offcanvas">
  <SidebarHeader>
    {/* Header content */}
  </SidebarHeader>
  <SidebarContent>
    <SidebarMenu>
      <SidebarMenuItem>
        <SidebarMenuButton className="min-h-[44px]">
          {/* Menu item content */}
        </SidebarMenuButton>
      </SidebarMenuItem>
    </SidebarMenu>
  </SidebarContent>
</Sidebar>
```

#### **Responsive Grids**
```tsx
// ✅ Good: Mobile-first grid
<div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
  {/* Grid items */}
</div>

// ❌ Bad: Desktop-first grid
<div className="grid grid-cols-3 sm:grid-cols-1 gap-4">
  {/* Grid items */}
</div>
```

### **2. Form Components**

#### **Input Fields**
```tsx
// ✅ Good: Mobile-optimized input
<Input 
  className="min-h-[44px] touch-manipulation"
  type="text"
  placeholder="Enter text..."
/>

// ❌ Bad: Desktop-only input
<Input 
  className="h-8"
  type="text"
  placeholder="Enter text..."
/>
```

#### **Buttons**
```tsx
// ✅ Good: Touch-friendly button
<Button 
  size="default" // h-10 md:h-9 with min-h-[44px]
  className="min-h-[44px] touch-manipulation"
>
  Click me
</Button>

// ❌ Bad: Small button
<Button size="sm">Click me</Button>
```

### **3. Content Components**

#### **Cards**
```tsx
// ✅ Good: Mobile-responsive card
<Card className="p-4 md:p-6">
  <CardHeader className="pb-3">
    <CardTitle className="text-lg md:text-xl">
      {/* Title content */}
    </CardTitle>
  </CardHeader>
  <CardContent className="space-y-3">
    {/* Card content */}
  </CardContent>
</Card>
```

#### **Tables**
```tsx
// ✅ Good: Mobile-responsive table
<Table>
  <TableHeader>
    <TableRow>
      <TableHead className="text-xs md:text-sm px-2 md:px-4">
        {/* Header content */}
      </TableHead>
    </TableRow>
  </TableHeader>
  <TableBody>
    <TableRow>
      <TableCell className="text-xs md:text-sm p-2 md:p-4">
        {/* Cell content */}
      </TableCell>
    </TableRow>
  </TableBody>
</Table>
```

---

## 🚀 **Performance Guidelines**

### **1. Lazy Loading**
```tsx
// ✅ Good: Lazy load heavy components
import { LazyLoad } from '@/components/ui/lazy-load';

<LazyLoad fallback={<SkeletonCard />}>
  <ExpensiveComponent />
</LazyLoad>
```

### **2. Image Optimization**
```tsx
// ✅ Good: Responsive images
import { MobileImage } from '@/components/ui/responsive-image';

<MobileImage
  src="/image.jpg"
  alt="Description"
  aspectRatio="video"
  sizes="(max-width: 768px) 100vw, 50vw"
  priority={false}
/>
```

### **3. Bundle Optimization**
- Use dynamic imports for heavy components
- Implement code splitting for routes
- Optimize images and assets for mobile

---

## 🎯 **Touch Interaction Guidelines**

### **1. Swipe Gestures**
```tsx
// ✅ Good: Implement swipe gestures
import { useSwipeGesture } from '@/hooks/useSwipeGesture';

const { getSwipeHandlers } = useSwipeGesture({
  onSwipeLeft: () => handleSwipeLeft(),
  onSwipeRight: () => handleSwipeRight(),
  threshold: 50,
});

<div {...getSwipeHandlers()}>
  {/* Swipeable content */}
</div>
```

### **2. Touch Feedback**
```tsx
// ✅ Good: Touch feedback
<Button 
  className="active:scale-95 transition-transform touch-manipulation"
  onTouchStart={(e) => e.currentTarget.style.transform = 'scale(0.95)'}
  onTouchEnd={(e) => e.currentTarget.style.transform = 'scale(1)'}
>
  Touch me
</Button>
```

---

## 📐 **Spacing and Typography**

### **1. Mobile Spacing**
```tsx
// ✅ Good: Mobile-first spacing
<div className="p-4 md:p-6 space-y-4 md:space-y-6">
  {/* Content with responsive spacing */}
</div>

// ❌ Bad: Fixed spacing
<div className="p-6 space-y-6">
  {/* Content with fixed spacing */}
</div>
```

### **2. Typography**
```tsx
// ✅ Good: Responsive typography
<h1 className="text-lg md:text-xl lg:text-2xl font-semibold">
  Responsive heading
</h1>

<p className="text-sm md:text-base text-gray-600">
  Responsive paragraph
</p>
```

---

## 🧪 **Testing Guidelines**

### **1. Mobile Testing Utilities**
```tsx
import { 
  isMobile, 
  isTouchDevice, 
  validateTouchTarget,
  testMobileNavigation 
} from '@/utils/mobileTesting';

// Test mobile detection
if (isMobile()) {
  // Mobile-specific logic
}

// Test touch target size
const isValid = validateTouchTarget(element);

// Test navigation accessibility
const navTest = testMobileNavigation();
```

### **2. Responsive Testing**
- Test on actual devices when possible
- Use browser dev tools for responsive testing
- Test touch interactions on touch devices
- Validate performance on slower connections

---

## 🎨 **CSS Best Practices**

### **1. Mobile-First CSS**
```css
/* ✅ Good: Mobile-first approach */
.component {
  padding: 1rem;
  font-size: 0.875rem;
}

@media (min-width: 768px) {
  .component {
    padding: 1.5rem;
    font-size: 1rem;
  }
}

/* ❌ Bad: Desktop-first approach */
.component {
  padding: 1.5rem;
  font-size: 1rem;
}

@media (max-width: 767px) {
  .component {
    padding: 1rem;
    font-size: 0.875rem;
  }
}
```

### **2. Touch-Friendly CSS**
```css
/* ✅ Good: Touch-friendly styles */
.touch-target {
  min-height: 44px;
  min-width: 44px;
  touch-action: manipulation;
  -webkit-tap-highlight-color: transparent;
}

.touch-feedback:active {
  transform: scale(0.95);
  transition: transform 0.1s ease;
}
```

---

## 🔧 **Development Tools**

### **1. Mobile Development Setup**
- Use browser dev tools for responsive testing
- Test on actual mobile devices
- Use network throttling to test performance
- Validate accessibility with screen readers

### **2. Performance Monitoring**
```tsx
import { measurePerformance } from '@/utils/mobileTesting';

// Monitor performance
const perf = measurePerformance();
console.log('Connection:', perf.connection);
console.log('Memory usage:', perf.memory);
```

---

## 📋 **Checklist for Mobile Development**

### **Before Development**
- [ ] Define mobile breakpoints
- [ ] Plan touch interactions
- [ ] Consider performance implications
- [ ] Plan accessibility features

### **During Development**
- [ ] Use mobile-first approach
- [ ] Implement touch-friendly interactions
- [ ] Optimize for performance
- [ ] Test on multiple devices
- [ ] Validate accessibility

### **After Development**
- [ ] Test on real devices
- [ ] Validate performance metrics
- [ ] Check accessibility compliance
- [ ] Document mobile-specific features
- [ ] Update mobile guidelines

---

## 🚨 **Common Pitfalls to Avoid**

### **1. Desktop-First Thinking**
- Don't start with desktop layouts and scale down
- Always consider mobile constraints first

### **2. Ignoring Touch Interactions**
- Don't rely only on hover states
- Provide touch feedback for all interactions

### **3. Performance Neglect**
- Don't load heavy components on mobile
- Optimize images and assets for mobile

### **4. Accessibility Oversights**
- Don't forget about screen readers
- Ensure proper touch target sizes
- Provide keyboard navigation alternatives

---

## 📚 **Resources**

### **Documentation**
- [Mobile Responsiveness Setup Guide](./MOBILE_RESPONSIVENESS_SETUP.md)
- [Component Library Documentation](./frontend/src/components/ui/README.md)

### **Tools**
- [Mobile Testing Utilities](./frontend/src/utils/mobileTesting.ts)
- [Swipe Gesture Hook](./frontend/src/hooks/useSwipeGesture.ts)
- [Mobile Scroll Hook](./frontend/src/hooks/useMobileScroll.ts)

### **Components**
- [Lazy Loading Component](./frontend/src/components/ui/lazy-load.tsx)
- [Responsive Image Component](./frontend/src/components/ui/responsive-image.tsx)

---

*Last updated: January 2025*
*Version: 1.0*
