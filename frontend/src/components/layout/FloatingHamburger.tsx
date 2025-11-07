/**
 * Floating Hamburger Button
 * Glassmorphism design with better icon positioned below header on the left
 */

import { Menu } from 'lucide-react';
import { cn } from '@/lib/utils';

interface FloatingHamburgerProps {
  isOpen: boolean;
  onClick: () => void;
}

export function FloatingHamburger({ isOpen, onClick }: FloatingHamburgerProps) {
  // Hide the button when menu is open
  if (isOpen) return null;

  return (
    <button
      onClick={onClick}
      className={cn(
        "md:hidden fixed left-3 z-[1150]",
        "w-6 h-5 flex items-center justify-center",
        // Glassmorphism effect
        "bg-white/80 backdrop-blur-xl",
        "border border-white/40 shadow-lg",
        "rounded-xl",
        "transition-all duration-300 ease-out",
        "touch-manipulation active:scale-90",
        "hover:bg-white/90 hover:shadow-xl hover:border-primary/30",
        "focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2",
        // Position below header (64px) + some spacing
        "top-20",
        // Add subtle gradient overlay
        "before:absolute before:inset-0 before:rounded-xl",
        "before:bg-gradient-to-br before:from-white/50 before:to-transparent",
        "before:pointer-events-none"
      )}
      aria-label="Open menu"
      aria-expanded={false}
    >
      {/* Icon */}
      <div className="relative z-10">
        <Menu className="w-4 h-4 text-gray-700" />
      </div>
    </button>
  );
}
