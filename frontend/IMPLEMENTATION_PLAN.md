# Implementation Plan: PDF Export, Share, Animations & Drag-Drop

## Overview
This document outlines the implementation of three key features:
1. Enhanced PDF Export & Share Functionality
2. Advanced Animations
3. Drag & Drop for Activities

## Current State Analysis

### ✅ Already Implemented
- **PDF Export**: Basic browser print API in `exportService.ts`
- **Share**: Web Share API and clipboard copy
- **Framer Motion**: Installed (`^11.0.0`) and used in SearchResults, Login, Dashboard
- **@dnd-kit**: Installed (`^6.3.1`) but NOT used anywhere

### ❌ Missing/Incomplete
- Enhanced PDF export with proper formatting and backend integration
- Share modal with email functionality
- Comprehensive animation system across all components
- Drag & drop for reordering activities in day-by-day view
- Drag & drop for workflow builder

---

## Feature 1: Enhanced PDF Export & Share

### 1.1 PDF Export Enhancement

**Current Issues:**
- Uses basic browser print dialog
- Limited formatting options
- No backend integration
- No export options modal

**Implementation:**

#### Step 1: Create Export Options Modal
```typescript
// frontend-redesign/src/components/export/ExportOptionsModal.tsx
- Select pages to include (Overview, Day-by-day, Map, Bookings)
- Layout options (Portrait/Landscape, Page size)
- Branding options (Logo, Header/Footer)
```

#### Step 2: Enhance Export Service
```typescript
// frontend-redesign/src/services/exportService.ts
- Add backend API integration: GET /itineraries/{id}/pdf
- Add export options parameter
- Improve HTML template with better styling
- Add option to use jsPDF for client-side generation
```

#### Step 3: Update UI Components
```typescript
// frontend-redesign/src/components/trip/tabs/ViewTab.tsx
// frontend-redesign/src/components/trip/TripSidebar.tsx
- Replace simple export button with modal trigger
- Add loading state during PDF generation
- Add download progress indicator
```

### 1.2 Share Functionality Enhancement

**Current Issues:**
- No email sharing
- No share modal
- Limited share options

**Implementation:**

#### Step 1: Create Share Modal
```typescript
// frontend-redesign/src/components/share/ShareModal.tsx
- Copy link button
- Email share form
- Social media share buttons
- QR code generation
- Public/Private toggle
```

#### Step 2: Create Email Share Form
```typescript
// frontend-redesign/src/components/share/EmailShareForm.tsx
- Recipient email input (multiple)
- Optional message
- Send button with loading state
- Success/error feedback
```

#### Step 3: Backend Integration
```typescript
// API Endpoints needed:
POST /itineraries/{id}:share - Generate share token
POST /email/send - Send email with itinerary link
GET /share/{token} - Access shared itinerary
```

---

## Feature 2: Advanced Animations

### 2.1 Animation System Architecture

**Goal:** Create a consistent, performant animation system using Framer Motion

#### Step 1: Create Animation Utilities
```typescript
// frontend-redesign/src/utils/animations.ts
export const fadeIn = {
  initial: { opacity: 0 },
  animate: { opacity: 1 },
  exit: { opacity: 0 }
};

export const slideUp = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -20 }
};

export const scaleIn = {
  initial: { opacity: 0, scale: 0.9 },
  animate: { opacity: 1, scale: 1 },
  exit: { opacity: 0, scale: 0.9 }
};

export const staggerChildren = {
  animate: {
    transition: {
      staggerChildren: 0.1
    }
  }
};
```

#### Step 2: Create Animated Components
```typescript
// frontend-redesign/src/components/animated/AnimatedCard.tsx
// frontend-redesign/src/components/animated/AnimatedList.tsx
// frontend-redesign/src/components/animated/AnimatedModal.tsx
// frontend-redesign/src/components/animated/AnimatedPage.tsx
```

#### Step 3: Apply Animations to Key Components

**Priority Components:**
1. **DayCard** - Expand/collapse animations
2. **Activity Cards** - Hover effects, entrance animations
3. **Modal Dialogs** - Smooth open/close
4. **Page Transitions** - Route changes
5. **Loading States** - Skeleton screens with shimmer
6. **Success/Error States** - Celebration/error animations

### 2.2 Specific Animation Implementations

#### DayCard Animations
```typescript
// frontend-redesign/src/components/trip/DayCard.tsx
- Smooth expand/collapse with height animation
- Stagger children when expanding
- Hover lift effect
- Activity cards slide in when expanded
```

#### Activity List Animations
```typescript
// frontend-redesign/src/components/trip/tabs/PlanTab.tsx
- Stagger animation for activity list
- Smooth reordering animation (with drag-drop)
- Add/remove animations
```

#### Modal Animations
```typescript
// All modal components
- Backdrop fade in
- Content scale + fade in
- Smooth exit animations
```

#### Page Transitions
```typescript
// frontend-redesign/src/App.tsx or route components
- Fade between routes
- Preserve scroll position
- Loading state during transition
```

---

## Feature 3: Drag & Drop for Activities

### 3.1 Architecture

**Library:** @dnd-kit (already installed)
**Scope:** 
1. Day-by-day view - Reorder activities within a day
2. Day-by-day view - Move activities between days
3. Workflow builder - Position nodes (future)

### 3.2 Implementation

#### Step 1: Create Drag & Drop Context
```typescript
// frontend-redesign/src/contexts/DragDropContext.tsx
import { DndContext, closestCenter, DragEndEvent } from '@dnd-kit/core';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';

export function DragDropProvider({ children }) {
  const handleDragEnd = (event: DragEndEvent) => {
    // Handle reordering logic
  };

  return (
    <DndContext collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
      {children}
    </DndContext>
  );
}
```

#### Step 2: Create Sortable Activity Component
```typescript
// frontend-redesign/src/components/trip/SortableActivity.tsx
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';

export function SortableActivity({ activity, children }) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging
  } = useSortable({ id: activity.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1
  };

  return (
    <div ref={setNodeRef} style={style} {...attributes} {...listeners}>
      {children}
    </div>
  );
}
```

#### Step 3: Update DayCard Component
```typescript
// frontend-redesign/src/components/trip/DayCard.tsx
- Wrap activities in SortableContext
- Use SortableActivity for each activity
- Add drag handle icon
- Update backend on drag end
- Optimistic UI updates
```

#### Step 4: Backend Integration
```typescript
// API Endpoints needed:
PUT /itineraries/{id}/days/{dayId}/reorder
  Body: { nodeIds: string[] }

PUT /itineraries/{id}/days/{dayId}/nodes/{nodeId}/move
  Body: { targetDayId: string, position: number }
```

#### Step 5: Add Visual Feedback
```typescript
- Drag handle icon (GripVertical from lucide-react)
- Hover state on drag handle
- Drop zone indicators
- Smooth animations during drag
- Undo/redo support
```

---

## Implementation Timeline

### Week 1: PDF Export & Share
- **Day 1-2**: Export Options Modal
- **Day 3-4**: Enhanced Export Service + Backend Integration
- **Day 5**: Share Modal + Email Form

### Week 2: Advanced Animations
- **Day 1-2**: Animation utilities + Animated components
- **Day 3-4**: Apply to DayCard, Activity Cards, Modals
- **Day 5**: Page transitions + Polish

### Week 3: Drag & Drop
- **Day 1-2**: DragDrop Context + Sortable Activity
- **Day 3-4**: Update DayCard + Backend Integration
- **Day 5**: Visual feedback + Testing

### Week 4: Testing & Polish
- **Day 1-2**: Comprehensive testing
- **Day 3-4**: Bug fixes + Performance optimization
- **Day 5**: Documentation + Deployment

---

## Success Criteria

### PDF Export & Share
- ✅ Users can customize PDF export options
- ✅ PDF includes all selected sections with proper formatting
- ✅ Users can share via email with custom message
- ✅ Share links work correctly
- ✅ Export/share actions complete in < 3 seconds

### Advanced Animations
- ✅ All major components have smooth animations
- ✅ Animations don't impact performance (60fps)
- ✅ Animations are consistent across the app
- ✅ Reduced motion preference is respected
- ✅ Loading states are engaging

### Drag & Drop
- ✅ Users can reorder activities within a day
- ✅ Users can move activities between days
- ✅ Drag operations are smooth (60fps)
- ✅ Changes persist to backend
- ✅ Undo/redo works correctly
- ✅ Mobile touch support works

---

## Technical Considerations

### Performance
- Use `will-change` CSS property for animated elements
- Implement virtual scrolling for long lists
- Debounce backend updates during drag
- Use `React.memo` for activity components
- Lazy load heavy components

### Accessibility
- Keyboard navigation for drag & drop
- Screen reader announcements
- Focus management in modals
- Reduced motion support
- ARIA labels for interactive elements

### Mobile Support
- Touch events for drag & drop
- Responsive animations
- Mobile-optimized PDF layout
- Native share sheet on mobile
- Haptic feedback on drag

### Error Handling
- Graceful fallbacks for unsupported features
- Retry logic for failed API calls
- User-friendly error messages
- Rollback on failed drag operations
- Offline support considerations

---

## Dependencies

### Required
- ✅ framer-motion: ^11.0.0 (installed)
- ✅ @dnd-kit/core: ^6.3.1 (installed)
- ✅ @dnd-kit/sortable: ^10.0.0 (installed)
- ✅ @dnd-kit/utilities: ^3.2.2 (installed)

### Optional (Future Enhancements)
- jsPDF: For client-side PDF generation
- html2canvas: For capturing screenshots
- qrcode.react: For QR code generation
- react-email: For email templates

---

## Next Steps

1. **Review this plan** with the team
2. **Create tickets** for each feature
3. **Set up feature flags** for gradual rollout
4. **Begin implementation** starting with Week 1
5. **Regular check-ins** to track progress

