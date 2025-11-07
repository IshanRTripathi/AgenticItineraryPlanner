/**
 * Mobile Menu Component
 * Enhanced slide-out navigation menu for mobile devices
 * Features: Swipe to close, keyboard support, active states, better UX
 */

import { useState, useEffect, useRef } from 'react';
import { Button } from '@/components/ui/button';
import { X, Home, Compass, Calendar, User, LogIn, LogOut, Search } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/components/ui/use-toast';
import { useLocation } from 'react-router-dom';
import { cn } from '@/lib/utils';

interface MobileMenuProps {
  isOpen: boolean;
  onClose: () => void;
}

export function MobileMenu({ isOpen, onClose }: MobileMenuProps) {
  const { user, signOut, isAuthenticated, loading } = useAuth();
  const { toast } = useToast();
  const location = useLocation();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const [touchStart, setTouchStart] = useState<number | null>(null);
  const [touchEnd, setTouchEnd] = useState<number | null>(null);

  const handleLogout = async () => {
    try {
      setIsLoggingOut(true);
      await signOut();
      toast({
        title: 'Signed out successfully',
        description: 'You have been logged out of your account',
      });
      onClose();
      window.location.href = '/';
    } catch (error: any) {
      toast({
        title: 'Logout failed',
        description: error.message || 'Please try again',
        variant: 'destructive',
      });
    } finally {
      setIsLoggingOut(false);
    }
  };

  const menuItems = [
    { icon: Home, label: 'Home', href: '/' },
    { icon: Search, label: 'Search', href: '/search' },
    { icon: Calendar, label: 'Plan Trip', href: '/planner' },
    { icon: Compass, label: 'My Trips', href: '/dashboard' },
    { icon: User, label: 'Profile', href: '/profile' },
  ];

  const isActive = (href: string) => {
    if (href === '/') {
      return location.pathname === '/';
    }
    return location.pathname.startsWith(href);
  };

  // Get user initials
  const getUserInitials = () => {
    if (!user?.email) return 'U';
    return user.email.charAt(0).toUpperCase();
  };

  // Keyboard support - ESC to close
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };

    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
      // Prevent body scroll when menu is open
      document.body.style.overflow = 'hidden';
    }

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [isOpen, onClose]);

  // Focus trap
  useEffect(() => {
    if (isOpen && menuRef.current) {
      const focusableElements = menuRef.current.querySelectorAll(
        'button, a, input, select, textarea, [tabindex]:not([tabindex="-1"])'
      );
      const firstElement = focusableElements[0] as HTMLElement;
      const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement;

      const handleTabKey = (e: KeyboardEvent) => {
        if (e.key === 'Tab') {
          if (e.shiftKey && document.activeElement === firstElement) {
            e.preventDefault();
            lastElement?.focus();
          } else if (!e.shiftKey && document.activeElement === lastElement) {
            e.preventDefault();
            firstElement?.focus();
          }
        }
      };

      document.addEventListener('keydown', handleTabKey);
      firstElement?.focus();

      return () => {
        document.removeEventListener('keydown', handleTabKey);
      };
    }
  }, [isOpen]);

  // Swipe to close
  const minSwipeDistance = 50;

  const onTouchStart = (e: React.TouchEvent) => {
    setTouchEnd(null);
    setTouchStart(e.targetTouches[0].clientX);
  };

  const onTouchMove = (e: React.TouchEvent) => {
    setTouchEnd(e.targetTouches[0].clientX);
  };

  const onTouchEnd = () => {
    if (!touchStart || !touchEnd) return;
    const distance = touchStart - touchEnd;
    const isLeftSwipe = distance > minSwipeDistance;
    if (isLeftSwipe) {
      onClose();
    }
  };

  return (
    <>
      {/* Backdrop */}
      <div
        className={cn(
          'fixed inset-0 bg-black/60 backdrop-blur-sm z-40 transition-opacity duration-300 md:hidden',
          isOpen ? 'opacity-100' : 'opacity-0 pointer-events-none'
        )}
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Menu */}
      <div
        ref={menuRef}
        className={cn(
          'fixed top-0 left-0 h-full w-64 max-w-[75vw] bg-white shadow-2xl z-50 transition-transform duration-300 ease-out md:hidden',
          'flex flex-col',
          isOpen ? 'translate-x-0' : '-translate-x-full'
        )}
        onTouchStart={onTouchStart}
        onTouchMove={onTouchMove}
        onTouchEnd={onTouchEnd}
        role="dialog"
        aria-modal="true"
        aria-label="Navigation menu"
      >
        {/* Header with gradient - Add top padding for header clearance */}
        <div className="bg-gradient-to-br from-primary to-primary/90 text-white p-3 pt-20">
          <h2 className="text-lg font-bold mb-3">EasyTrip</h2>

          {/* User Info Card - Compact */}
          {!loading && isAuthenticated && (
            <div className="flex items-center gap-2 p-2 bg-white/10 backdrop-blur-sm rounded-lg">
              <div className="w-9 h-9 rounded-full bg-white text-primary flex items-center justify-center font-semibold text-sm flex-shrink-0">
                {getUserInitials()}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs font-semibold truncate">
                  {user?.displayName || 'User'}
                </p>
                <p className="text-[10px] text-white/80 truncate">
                  {user?.email}
                </p>
              </div>
            </div>
          )}
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto p-3">
          <ul className="space-y-0.5">
            {menuItems.map((item) => {
              const Icon = item.icon;
              const active = isActive(item.href);
              
              return (
                <li key={item.href}>
                  <a
                    href={item.href}
                    className={cn(
                      "flex items-center gap-3 px-3 py-3 rounded-lg transition-all duration-200",
                      "min-h-[48px] touch-manipulation active:scale-98",
                      "group relative overflow-hidden",
                      active && "bg-primary/10 text-primary font-semibold",
                      !active && "hover:bg-gray-50 text-gray-700"
                    )}
                    onClick={onClose}
                  >
                    {/* Active indicator */}
                    {active && (
                      <div className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-6 bg-primary rounded-r-full" />
                    )}
                    
                    <Icon 
                      className={cn(
                        "w-5 h-5 transition-all duration-200 flex-shrink-0",
                        active && "text-primary scale-105",
                        !active && "text-gray-500 group-hover:text-primary group-hover:scale-105"
                      )} 
                    />
                    <span className="text-sm">{item.label}</span>
                  </a>
                </li>
              );
            })}
          </ul>
        </nav>

        {/* Footer - Add proper spacing */}
        <div className="p-3 border-t bg-gray-50/50">
          {loading ? (
            <div className="h-10 animate-pulse bg-gray-200 rounded-lg mb-4" />
          ) : isAuthenticated ? (
            <Button
              className="w-full min-h-[44px] text-sm mb-4"
              variant="outline"
              onClick={handleLogout}
              disabled={isLoggingOut}
            >
              <LogOut className="w-4 h-4 mr-2" />
              {isLoggingOut ? 'Signing out...' : 'Sign Out'}
            </Button>
          ) : (
            <Button
              className="w-full min-h-[44px] text-sm mb-4"
              onClick={() => {
                window.location.href = '/login';
                onClose();
              }}
            >
              <LogIn className="w-4 h-4 mr-2" />
              Sign In
            </Button>
          )}
          {/* Safe area spacer */}
          <div className="h-safe-bottom" />
        </div>
      </div>
    </>
  );
}
