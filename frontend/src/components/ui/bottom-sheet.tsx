/**
 * Bottom Sheet Component
 * Mobile-optimized modal that slides up from bottom
 * Desktop: Regular dialog
 */

import * as React from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { cn } from '@/lib/utils';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { X } from 'lucide-react';

interface BottomSheetProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  children: React.ReactNode;
  title?: string;
  className?: string;
}

export function BottomSheet({
  open,
  onOpenChange,
  children,
  title,
  className,
}: BottomSheetProps) {
  const isMobile = useMediaQuery('(max-width: 768px)');

  if (!isMobile) {
    // Desktop: Regular dialog
    return (
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className={cn('max-w-2xl', className)}>
          {title && (
            <DialogHeader>
              <DialogTitle>{title}</DialogTitle>
            </DialogHeader>
          )}
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
          'fixed bottom-0 left-0 right-0 top-auto',
          'rounded-t-2xl border-t p-0',
          'max-h-[90vh] overflow-hidden',
          'data-[state=open]:slide-in-from-bottom',
          'data-[state=closed]:slide-out-to-bottom',
          'data-[state=open]:animate-in',
          'data-[state=closed]:animate-out',
          'data-[state=closed]:duration-300',
          'data-[state=open]:duration-300',
          className
        )}
      >
        {/* Drag handle */}
        <div className="flex justify-center py-3 border-b">
          <div className="w-12 h-1 bg-muted rounded-full" />
        </div>

        {/* Header */}
        {title && (
          <div className="flex items-center justify-between px-4 py-2.5 border-b">
            <h2 className="text-lg font-bold">{title}</h2>
            <button
              onClick={() => onOpenChange(false)}
              className="p-1.5 hover:bg-muted rounded-full transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        )}

        {/* Content */}
        <div className="overflow-y-auto max-h-[calc(90vh-80px)] p-4">
          {children}
        </div>
      </DialogContent>
    </Dialog>
  );
}
