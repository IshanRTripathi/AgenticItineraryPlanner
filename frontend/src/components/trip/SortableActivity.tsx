/**
 * Sortable Activity Component
 * Wraps activity cards with drag-and-drop functionality using @dnd-kit
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
        isDragging && 'opacity-50 z-50'
      )}
    >
      {/* Drag Handle - Compact positioning */}
      {!disabled && (
        <div
          {...attributes}
          {...listeners}
          className={cn(
            'absolute left-0 top-1/2 -translate-y-1/2 -translate-x-6',
            'opacity-40 group-hover:opacity-100 transition-opacity duration-200',
            'cursor-grab active:cursor-grabbing',
            'p-0.5 rounded hover:bg-muted',
            'touch-none', // Prevent touch scrolling on drag handle
            'focus-visible:opacity-100 focus-visible:ring-2 focus-visible:ring-primary'
          )}
          title="Drag to reorder"
          aria-label="Drag to reorder activity"
          tabIndex={0}
        >
          <GripVertical className="w-4 h-4 text-muted-foreground" />
        </div>
      )}
      
      {children}
    </div>
  );
}
