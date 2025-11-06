/**
 * Responsive Modal Component
 * Automatically switches between Dialog (desktop) and BottomSheet (mobile)
 * Task 3.1: Create ResponsiveModal component
 */

import * as React from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { BottomSheet } from '@/components/ui/bottom-sheet';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { cn } from '@/lib/utils';

interface ResponsiveModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title?: string;
  description?: string;
  children: React.ReactNode;
  className?: string;
  footer?: React.ReactNode;
}

/**
 * ResponsiveModal - Adaptive modal component
 * 
 * Renders as:
 * - BottomSheet on mobile (< 768px)
 * - Dialog on desktop (>= 768px)
 * 
 * @example
 * ```tsx
 * <ResponsiveModal
 *   open={isOpen}
 *   onOpenChange={setIsOpen}
 *   title="Confirm Action"
 *   description="Are you sure you want to proceed?"
 * >
 *   <div>Modal content here</div>
 * </ResponsiveModal>
 * ```
 */
export function ResponsiveModal({
  open,
  onOpenChange,
  title,
  description,
  children,
  className,
  footer,
}: ResponsiveModalProps) {
  const isMobile = useMediaQuery('(max-width: 768px)');

  if (isMobile) {
    // Mobile: Bottom Sheet
    return (
      <BottomSheet
        open={open}
        onOpenChange={onOpenChange}
        title={title}
        className={className}
      >
        {description && (
          <p className="text-sm text-muted-foreground mb-4">{description}</p>
        )}
        {children}
        {footer && (
          <div className="mt-6 pt-4 border-t">
            {footer}
          </div>
        )}
      </BottomSheet>
    );
  }

  // Desktop: Dialog
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className={cn('max-w-lg', className)}>
        {(title || description) && (
          <DialogHeader>
            {title && <DialogTitle>{title}</DialogTitle>}
            {description && <DialogDescription>{description}</DialogDescription>}
          </DialogHeader>
        )}
        <div className="py-4">
          {children}
        </div>
        {footer && (
          <DialogFooter>
            {footer}
          </DialogFooter>
        )}
      </DialogContent>
    </Dialog>
  );
}

// Export sub-components for advanced usage
export { DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
