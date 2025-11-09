/**
 * Sortable Activity Component
 * Wraps activity cards with drag-and-drop functionality using @dnd-kit
 * Drag zone is the top portion of the card, buttons remain clickable
 */

import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { GripVertical } from 'lucide-react';
import { cn } from '@/lib/utils';

interface SortableActivityProps {
  id: string;
  children: React.ReactNode;
  disabled?: boolean;
}

export function SortableActivity({ id, children, disabled = false }: SortableActivityProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    setActivatorNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ 
    id,
    disabled 
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={cn(
        'relative group',
        // Enhanced visual feedback during drag
        isDragging && 'opacity-60 z-50 scale-105 shadow-2xl ring-2 ring-primary/30',
        // Subtle hover effect when not dragging
        !isDragging && !disabled && 'hover:ring-2 hover:ring-primary/10 transition-all duration-200'
      )}
    >
      {/* Drag Zone - Top 80px of card (header area only) */}
      {!disabled && (
        <div
          ref={setActivatorNodeRef}
          {...attributes}
          {...listeners}
          className={cn(
            // Position: covers top portion of card
            'absolute top-0 left-0 right-0 h-20 z-10',
            // Cursor feedback
            'cursor-grab active:cursor-grabbing',
            // Layout
            'flex items-center justify-center',
            // Visibility: hidden by default, show on hover
            'opacity-0 group-hover:opacity-100 transition-opacity duration-200',
            // Visual styling
            'bg-gradient-to-b from-primary/5 to-transparent',
            // Touch handling
            'touch-none'
          )}
          title="Drag to reorder"
          aria-label="Drag to reorder activity"
        >
          {/* Floating badge indicator */}
          <div className="bg-white/95 backdrop-blur-sm rounded-full px-3 py-1.5 shadow-md border border-primary/30 flex items-center gap-1.5">
            <GripVertical className="w-4 h-4 text-primary" />
            <span className="text-xs font-medium text-primary">Drag to reorder</span>
          </div>
        </div>
      )}
      
      {children}
    </div>
  );
}
