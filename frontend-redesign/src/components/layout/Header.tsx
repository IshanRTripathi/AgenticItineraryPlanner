/**
 * Header Component
 * Sticky navigation header with mobile menu
 */

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Menu } from 'lucide-react';
import { MobileMenu } from './MobileMenu';

export function Header() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <>
      <header className="sticky top-0 z-sticky bg-white border-b border-border shadow-sm">
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
              <a href="/dashboard" className="text-sm font-medium hover:text-primary transition-colors">
                My Trips
              </a>
              <a href="#flights" className="text-sm font-medium hover:text-primary transition-colors">
                Flights
              </a>
              <a href="#hotels" className="text-sm font-medium hover:text-primary transition-colors">
                Hotels
              </a>
            </nav>

            {/* Right Actions */}
            <div className="flex items-center gap-4">
              <Button 
                onClick={() => (window.location.href = '/login')}
                className="hidden md:inline-flex"
              >
                Sign In
              </Button>
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
