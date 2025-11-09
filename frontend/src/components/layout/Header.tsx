/**
 * Header Component
 * Sticky navigation header with scroll animation
 * Design: Apple.com refinement + Material 3 motion
 * Mobile-optimized with bottom sheet profile menu
 */

import { useState, useRef, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { LogOut, User, Settings, HelpCircle } from 'lucide-react';

import { BottomSheet } from '@/components/ui/bottom-sheet';
import { useStickyHeader } from '@/hooks/useScrollAnimation';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/components/ui/use-toast';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { LanguageSelector } from '@/i18n/components/LanguageSelector';
import { useTranslation } from '@/i18n';
import { cn } from '@/lib/utils';

export function Header() {
  const { t } = useTranslation();
  const [profileDropdownOpen, setProfileDropdownOpen] = useState(false);
  const [profileSheetOpen, setProfileSheetOpen] = useState(false);
  const isScrolled = useStickyHeader(20);
  const { user, signOut, isAuthenticated, loading } = useAuth();
  const { toast } = useToast();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const isMobile = useMediaQuery('(max-width: 768px)');

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
      setProfileSheetOpen(false);
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

  const handleProfileClick = () => {
    if (isMobile) {
      setProfileSheetOpen(true);
    } else {
      setProfileDropdownOpen(!profileDropdownOpen);
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
          'sticky top-0 z-sticky bg-white border-b border-border transition-all duration-300 safe-top',
          isScrolled ? 'shadow-elevation-2' : 'shadow-sm'
        )}
      >
        <div className="container">
          <div className="flex items-center justify-between h-16">
            {/* Logo */}
            <a href="/" className="flex items-center gap-2">
              <h1 className="text-2xl font-bold text-primary">EasyTrip</h1>
            </a>

            {/* Desktop Navigation - Hidden on mobile and tablet (< 768px) */}
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
            <div className="flex items-center gap-3 sm:gap-4">
              {/* Language Selector - Desktop */}
              <div className="hidden md:block">
                <LanguageSelector variant="compact" showFlags={true} />
              </div>

              {loading ? (
                // Show nothing while loading to prevent flash
                <div className="hidden lg:block w-10 h-9" />
              ) : isAuthenticated ? (
                <>
                  {/* Desktop Profile Dropdown */}
                  <div className="hidden md:block relative" ref={dropdownRef}>
                    {/* Profile Button */}
                    <button
                      onClick={handleProfileClick}
                      className={cn(
                        "min-w-[44px] min-h-[44px] w-11 h-11 rounded-full bg-primary text-white flex items-center justify-center font-semibold text-sm transition-all duration-200",
                        "hover:ring-2 hover:ring-primary/20 hover:shadow-md touch-manipulation active:scale-95",
                        profileDropdownOpen && "ring-2 ring-primary/30 shadow-md"
                      )}
                    >
                      {getUserInitials()}
                    </button>

                    {/* Dropdown Menu */}
                    {profileDropdownOpen && (
                      <div className="absolute right-0 mt-2 w-64 bg-white rounded-xl shadow-lg border border-gray-200 py-1.5 z-50 animate-in fade-in slide-in-from-top-2 duration-200">
                        {/* User Info */}
                        <div className="px-3 py-2.5 border-b border-gray-100">
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
                            className="flex items-center gap-3 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                            onClick={() => setProfileDropdownOpen(false)}
                          >
                            <User className="w-4 h-4 text-muted-foreground" />
                            <span>{t('components.header.profile.myProfile')}</span>
                          </a>
                          <a
                            href="/settings"
                            className="flex items-center gap-3 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                            onClick={() => setProfileDropdownOpen(false)}
                          >
                            <Settings className="w-4 h-4 text-muted-foreground" />
                            <span>{t('components.header.profile.settings')}</span>
                          </a>
                          <a
                            href="/help"
                            className="flex items-center gap-3 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                            onClick={() => setProfileDropdownOpen(false)}
                          >
                            <HelpCircle className="w-4 h-4 text-muted-foreground" />
                            <span>{t('components.header.profile.help')}</span>
                          </a>
                        </div>

                        {/* Logout */}
                        <div className="border-t border-gray-100 pt-1">
                          <button
                            onClick={handleLogout}
                            disabled={isLoggingOut}
                            className="w-full flex items-center gap-3 px-3 py-2 text-sm text-red-600 hover:bg-red-50 transition-colors disabled:opacity-50"
                          >
                            <LogOut className="w-4 h-4" />
                            <span>{isLoggingOut ? t('components.header.profile.signingOut') : t('components.header.profile.signOut')}</span>
                          </button>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Mobile/Tablet Profile Button */}
                  <button
                    onClick={handleProfileClick}
                    className={cn(
                      "md:hidden min-w-[44px] min-h-[44px] w-11 h-11 rounded-full bg-primary text-white flex items-center justify-center font-semibold text-sm transition-all duration-200",
                      "touch-manipulation active:scale-95"
                    )}
                  >
                    {getUserInitials()}
                  </button>
                </>
              ) : (
                <Button
                  onClick={() => (window.location.href = '/login')}
                  className="hidden md:inline-flex min-h-[44px]"
                >
                  Sign In
                </Button>
              )}
            </div>
          </div>
        </div>
      </header>

      {/* Mobile Profile Bottom Sheet */}
      <BottomSheet
        open={profileSheetOpen}
        onOpenChange={setProfileSheetOpen}
        title="Account"
      >
        {/* User Info */}
        <div className="flex items-center gap-3 p-4 bg-muted/50 rounded-lg mb-4">
          <div className="w-12 h-12 rounded-full bg-primary text-white flex items-center justify-center font-semibold text-lg">
            {getUserInitials()}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-semibold text-gray-900 truncate">
              {user?.displayName || 'User'}
            </p>
            <p className="text-xs text-muted-foreground truncate">
              {user?.email}
            </p>
          </div>
        </div>

        {/* Menu Items */}
        <div className="space-y-1">
          <a
            href="/profile"
            className="flex items-center gap-3 px-4 py-3 text-base text-gray-700 hover:bg-gray-50 rounded-lg transition-colors min-h-[48px]"
            onClick={() => setProfileSheetOpen(false)}
          >
            <User className="w-5 h-5 text-muted-foreground" />
            <span>My Profile</span>
          </a>
          <a
            href="/settings"
            className="flex items-center gap-3 px-4 py-3 text-base text-gray-700 hover:bg-gray-50 rounded-lg transition-colors min-h-[48px]"
            onClick={() => setProfileSheetOpen(false)}
          >
            <Settings className="w-5 h-5 text-muted-foreground" />
            <span>Settings</span>
          </a>
          <a
            href="/help"
            className="flex items-center gap-3 px-4 py-3 text-base text-gray-700 hover:bg-gray-50 rounded-lg transition-colors min-h-[48px]"
            onClick={() => setProfileSheetOpen(false)}
          >
            <HelpCircle className="w-5 h-5 text-muted-foreground" />
            <span>Help & Support</span>
          </a>
        </div>

        {/* Logout Button */}
        <div className="mt-6 pt-4 border-t">
          <button
            onClick={handleLogout}
            disabled={isLoggingOut}
            className="w-full flex items-center justify-center gap-3 px-4 py-3 text-base text-red-600 bg-red-50 hover:bg-red-100 rounded-lg transition-colors disabled:opacity-50 min-h-[48px] font-medium"
          >
            <LogOut className="w-5 h-5" />
            <span>{isLoggingOut ? 'Signing out...' : 'Sign Out'}</span>
          </button>
        </div>
      </BottomSheet>
    </>
  );
}
