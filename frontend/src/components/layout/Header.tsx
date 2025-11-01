/**
 * Header Component
 * Sticky navigation header with scroll animation
 * Design: Apple.com refinement + Material 3 motion
 */

import { useState, useRef, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Menu, LogOut, User, Settings, HelpCircle } from 'lucide-react';
import { MobileMenu } from './MobileMenu';
import { useStickyHeader } from '@/hooks/useScrollAnimation';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/components/ui/use-toast';
import { cn } from '@/lib/utils';

export function Header() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [profileDropdownOpen, setProfileDropdownOpen] = useState(false);
  const isScrolled = useStickyHeader(20);
  const { user, signOut, isAuthenticated, loading } = useAuth();
  const { toast } = useToast();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setProfileDropdownOpen(false);
      }
    };

    if (profileDropdownOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [profileDropdownOpen]);

  const handleLogout = async () => {
    try {
      setIsLoggingOut(true);
      setProfileDropdownOpen(false);
      await signOut();
      toast({
        title: 'Signed out successfully',
        description: 'You have been logged out of your account',
      });
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

  // Get user initials for avatar
  const getUserInitials = () => {
    if (!user?.email) return 'U';
    const email = user.email;
    return email.charAt(0).toUpperCase();
  };

  return (
    <>
      <header
        className={cn(
          'sticky top-0 z-sticky bg-white border-b border-border transition-all duration-300',
          isScrolled ? 'shadow-elevation-2' : 'shadow-sm'
        )}
      >
        <div className="container">
          <div className="flex items-center justify-between h-16">
            {/* Logo */}
            <a href="/" className="flex items-center gap-2">
              <h1 className="text-2xl font-bold text-primary">EasyTrip</h1>
            </a>

            {/* Desktop Navigation */}
            <nav className="hidden md:flex items-center gap-6">
              <a href="/" className="text-sm font-medium hover:text-primary transition-colors">
                Home
              </a>
              <a href="/planner" className="text-sm font-medium hover:text-primary transition-colors">
                Plan Trip
              </a>
              <a href="/dashboard" className="text-sm font-medium hover:text-primary transition-colors">
                My Trips
              </a>
              <a href="/search" className="text-sm font-medium hover:text-primary transition-colors">
                Search
              </a>
            </nav>

            {/* Right Actions */}
            <div className="flex items-center gap-4">
              {loading ? (
                // Show nothing while loading to prevent flash
                <div className="hidden md:block w-10 h-9" />
              ) : isAuthenticated ? (
                <div className="hidden md:block relative" ref={dropdownRef}>
                  {/* Profile Button */}
                  <button
                    onClick={() => setProfileDropdownOpen(!profileDropdownOpen)}
                    className={cn(
                      "w-10 h-9 rounded-full bg-primary text-white flex items-center justify-center font-semibold text-sm transition-all duration-200",
                      "hover:ring-2 hover:ring-primary/20 hover:shadow-md",
                      profileDropdownOpen && "ring-2 ring-primary/30 shadow-md"
                    )}
                  >
                    {getUserInitials()}
                  </button>

                  {/* Dropdown Menu */}
                  {profileDropdownOpen && (
                    <div className="absolute right-0 mt-2 w-64 bg-white rounded-xl shadow-lg border border-gray-200 py-2 z-50 animate-in fade-in slide-in-from-top-2 duration-200">
                      {/* User Info */}
                      <div className="px-4 py-3 border-b border-gray-100">
                        <div className="flex items-center gap-3">
                          <div className="flex-1 min-w-0">
                            <p className="text-sm font-semibold text-gray-900 truncate">
                              {user?.displayName || 'User'}
                            </p>
                            <p className="text-xs text-muted-foreground truncate">
                              {user?.email}
                            </p>
                          </div>
                        </div>
                      </div>

                      {/* Menu Items */}
                      <div className="py-1">
                        <a
                          href="/profile"
                          className="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                          onClick={() => setProfileDropdownOpen(false)}
                        >
                          <User className="w-4 h-4 text-muted-foreground" />
                          <span>My Profile</span>
                        </a>
                        <a
                          href="/settings"
                          className="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                          onClick={() => setProfileDropdownOpen(false)}
                        >
                          <Settings className="w-4 h-4 text-muted-foreground" />
                          <span>Settings</span>
                        </a>
                        <a
                          href="/help"
                          className="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                          onClick={() => setProfileDropdownOpen(false)}
                        >
                          <HelpCircle className="w-4 h-4 text-muted-foreground" />
                          <span>Help & Support</span>
                        </a>
                      </div>

                      {/* Logout */}
                      <div className="border-t border-gray-100 pt-1">
                        <button
                          onClick={handleLogout}
                          disabled={isLoggingOut}
                          className="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-red-600 hover:bg-red-50 transition-colors disabled:opacity-50"
                        >
                          <LogOut className="w-4 h-4" />
                          <span>{isLoggingOut ? 'Signing out...' : 'Sign Out'}</span>
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <Button
                  onClick={() => (window.location.href = '/login')}
                  className="hidden md:inline-flex"
                >
                  Sign In
                </Button>
              )}
              <button
                className="md:hidden p-2 hover:bg-muted rounded-lg transition-colors"
                onClick={() => setMobileMenuOpen(true)}
              >
                <Menu className="h-6 w-6" />
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Mobile Menu */}
      <MobileMenu
        isOpen={mobileMenuOpen}
        onClose={() => setMobileMenuOpen(false)}
      />
    </>
  );
}
