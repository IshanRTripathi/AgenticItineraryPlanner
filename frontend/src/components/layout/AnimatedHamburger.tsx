/**
 * Animated Hamburger Icon
 * Morphs between hamburger and X with smooth animation
 */

import { cn } from '@/lib/utils';

interface AnimatedHamburgerProps {
  isOpen: boolean;
  onClick: () => void;
  className?: string;
}

export function AnimatedHamburger({ isOpen, onClick, className }: AnimatedHamburgerProps) {
  return (
    <button
      onClick={onClick}
      className={cn(
        "relative min-w-[48px] min-h-[48px] w-12 h-12 flex items-center justify-center",
        "rounded-lg hover:bg-muted transition-all duration-200",
        "touch-manipulation active:scale-95",
        "focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2",
        className
      )}
      aria-label={isOpen ? "Close menu" : "Open menu"}
      aria-expanded={isOpen}
    >
      <div className="w-6 h-5 flex flex-col justify-center items-center relative">
        {/* Top line */}
        <span
          className={cn(
            "absolute w-6 h-0.5 bg-current rounded-full transition-all duration-300 ease-out",
            isOpen
              ? "rotate-45 translate-y-0"
              : "rotate-0 -translate-y-2"
          )}
        />
        
        {/* Middle line */}
        <span
          className={cn(
            "absolute w-6 h-0.5 bg-current rounded-full transition-all duration-300 ease-out",
            isOpen ? "opacity-0 scale-0" : "opacity-100 scale-100"
          )}
        />
        
        {/* Bottom line */}
        <span
          className={cn(
            "absolute w-6 h-0.5 bg-current rounded-full transition-all duration-300 ease-out",
            isOpen
              ? "-rotate-45 translate-y-0"
              : "rotate-0 translate-y-2"
          )}
        />
      </div>
    </button>
  );
}
