/**
 * Mobile Menu Component
 * Slide-out navigation menu for mobile devices
 */

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { X, Home, Compass, Calendar, User, LogIn, LogOut, Plane, Hotel, Search } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/components/ui/use-toast';
import { cn } from '@/lib/utils';

interface MobileMenuProps {
  isOpen: boolean;
  onClose: () => void;
}

export function MobileMenu({ isOpen, onClose }: MobileMenuProps) {
  const { user, signOut, isAuthenticated, loading } = useAuth();
  const { toast } = useToast();
  const [isLoggingOut, setIsLoggingOut] = useState(false);

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
    { icon: Plane, label: 'Flights', href: '/search?type=flight' },
    { icon: Hotel, label: 'Hotels', href: '/search?type=hotel' },
    { icon: Calendar, label: 'Plan Trip', href: '/planner' },
    { icon: Compass, label: 'My Trips', href: '/dashboard' },
    { icon: User, label: 'Profile', href: '/profile' },
  ];

  return (
    <>
      {/* Backdrop */}
      <div
        className={cn(
          'fixed inset-0 bg-black/50 z-40 transition-opacity duration-300 lg:hidden',
          isOpen ? 'opacity-100' : 'opacity-0 pointer-events-none'
        )}
        onClick={onClose}
      />

      {/* Menu */}
      <div
        className={cn(
          'fixed top-0 right-0 h-full w-80 max-w-[85vw] bg-background shadow-elevation-3 z-50 transition-transform duration-300 lg:hidden',
          isOpen ? 'translate-x-0' : 'translate-x-full'
        )}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-3 border-b">
          <h2 className="text-xl font-bold text-primary">EasyTrip</h2>
          <button
            onClick={onClose}
            className="p-1.5 hover:bg-muted rounded-full transition-colors"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="p-3">
          <ul className="space-y-2">
            {menuItems.map((item) => {
              const Icon = item.icon;
              return (
                <li key={item.href}>
                  <a
                    href={item.href}
                    className="flex items-center gap-3 p-3 rounded-lg hover:bg-muted transition-colors"
                    onClick={onClose}
                  >
                    <Icon className="w-5 h-5 text-muted-foreground" />
                    <span className="font-medium">{item.label}</span>
                  </a>
                </li>
              );
            })}
          </ul>
        </nav>

        {/* Footer */}
        <div className="absolute bottom-0 left-0 right-0 p-3 border-t bg-background">
          {loading ? (
            // Show nothing while loading
            <div className="h-12" />
          ) : isAuthenticated ? (
            <div className="space-y-3">
              <div className="flex items-center gap-2 px-3 py-2 bg-muted rounded-lg">
                <User className="w-4 h-4 text-muted-foreground" />
                <span className="text-sm text-muted-foreground truncate">{user?.email}</span>
              </div>
              <Button
                className="w-full"
                variant="outline"
                onClick={handleLogout}
                disabled={isLoggingOut}
              >
                <LogOut className="w-4 h-4 mr-2" />
                {isLoggingOut ? 'Signing out...' : 'Sign Out'}
              </Button>
            </div>
          ) : (
            <Button
              className="w-full"
              onClick={() => {
                window.location.href = '/login';
                onClose();
              }}
            >
              <LogIn className="w-4 h-4 mr-2" />
              Sign In
            </Button>
          )}
        </div>
      </div>
    </>
  );
}
