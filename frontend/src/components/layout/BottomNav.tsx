/**
 * Bottom Navigation Component
 * Modern mobile menu with floating center button
 * Uses InteractiveMenu component with custom styling
 */

import { useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from '@/i18n';
import { Home, Search, MapPlus, User, Map, Eye, MessageSquare, CreditCard, MoreHorizontal } from 'lucide-react';
import { InteractiveMenu, type InteractiveMenuItem } from '@/components/ui/modern-mobile-menu';
import { useMemo, useState, useEffect, useRef } from 'react';
import { TripNavModal } from './TripNavModal';

interface BottomNavProps {
  hide?: boolean;
  mode?: 'global' | 'trip';
}

export function BottomNav({ hide = false, mode = 'global' }: BottomNavProps) {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const [isVisible, setIsVisible] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const lastScrollY = useRef(0);

  // Extract trip ID from pathname (e.g., /trip/abc123 or /trip/abc123?tab=view)
  const tripId = useMemo(() => {
    // Match /trip/ followed by any characters until ? or end of string
    const match = location.pathname.match(/^\/trip\/([^/?#]+)/);
    const extractedId = match ? match[1] : null;
    
    // Debug log to help troubleshoot
    if (mode === 'trip') {
      console.log('[BottomNav] Trip ID extraction:', {
        pathname: location.pathname,
        extractedId,
        mode
      });
    }
    
    return extractedId;
  }, [location.pathname, mode]);

  // Get current tab from URL query params
  const searchParams = new URLSearchParams(location.search);
  const currentTab = searchParams.get('tab') || 'view';

  // If in trip mode but no tripId found, fall back to global navigation
  const effectiveMode = (mode === 'trip' && tripId) ? 'trip' : 'global';

  // Define menu items based on effective mode
  const menuItems: InteractiveMenuItem[] = useMemo(() => {
    if (effectiveMode === 'trip' && tripId) {
      // Trip navigation - only if we have a valid tripId
      return [
        {
          label: t('components.tripNav.view'),
          icon: Eye,
          path: `/trip/${tripId}?tab=view`,
        },
        {
          label: t('components.tripNav.plan'),
          icon: Map,
          path: `/trip/${tripId}?tab=plan`,
        },
        {
          label: t('components.tripNav.chat'),
          icon: MessageSquare,
          path: `/trip/${tripId}?tab=chat`,
          isCenter: true,
        },
        {
          label: t('components.tripNav.bookings'),
          icon: CreditCard,
          path: `/trip/${tripId}?tab=bookings`,
        },
        {
          label: t('components.tripNav.others'),
          icon: MoreHorizontal,
          path: 'modal', // Special path to trigger modal
        },
      ];
    }

    // Global navigation
    return [
      {
        label: t('components.bottomNav.home'),
        icon: Home,
        path: '/',
      },
      {
        label: t('components.bottomNav.search'),
        icon: Search,
        path: '/search',
      },
      {
        label: t('components.bottomNav.plan'),
        icon: MapPlus,
        path: '/planner',
        isCenter: true,
      },
      {
        label: t('components.bottomNav.trips'),
        icon: Map,
        path: '/dashboard',
      },
      {
        label: t('components.bottomNav.profile'),
        icon: User,
        path: '/profile',
      },
    ];
  }, [t, effectiveMode, tripId]);

  // Determine active index based on current path/tab
  const activeIndex = useMemo(() => {
    if (effectiveMode === 'trip') {
      // Check if current tab is in "others" category OR if modal is open
      const otherTabs = ['budget', 'packing', 'docs'];
      if (otherTabs.includes(currentTab) || isModalOpen) {
        return 4; // "Others" button index
      }

      // Find matching tab
      const tabIndex = ['view', 'plan', 'chat', 'bookings'].indexOf(currentTab);
      return tabIndex >= 0 ? tabIndex : 0;
    }

    // Global navigation
    const index = menuItems.findIndex(item => {
      if (item.path === '/') {
        return location.pathname === '/';
      }
      return location.pathname.startsWith(item.path!);
    });
    return index >= 0 ? index : 0;
  }, [location.pathname, currentTab, menuItems, effectiveMode, isModalOpen]);

  // Handle scroll behavior - instant response
  useEffect(() => {
    let ticking = false;
    
    const handleScroll = () => {
      if (!ticking) {
        window.requestAnimationFrame(() => {
          const currentScrollY = window.scrollY;
          
          // At top of page - always show
          if (currentScrollY < 10) {
            setIsVisible(true);
          }
          // Scrolling down - hide
          else if (currentScrollY > lastScrollY.current && currentScrollY > 80) {
            setIsVisible(false);
          }
          // Scrolling up - show
          else if (currentScrollY < lastScrollY.current) {
            setIsVisible(true);
          }
          
          lastScrollY.current = currentScrollY;
          ticking = false;
        });
        
        ticking = true;
      }
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Handle navigation
  const handleItemClick = (_index: number, path?: string) => {
    if (path === 'modal') {
      // Toggle modal instead of just opening
      setIsModalOpen(!isModalOpen);
    } else if (path) {
      navigate(path);
    }
  };

  // Handle modal navigation
  const handleModalNavigate = (tab: string) => {
    navigate(`/trip/${tripId}?tab=${tab}`);
  };

  // Don't render if hidden
  if (hide) {
    return null;
  }

  return (
    <>
      <InteractiveMenu
        items={menuItems}
        activeIndex={activeIndex}
        onItemClick={handleItemClick}
        accentColor="hsl(var(--primary))"
        isHidden={!isVisible}
      />

      {/* Trip navigation modal */}
      {effectiveMode === 'trip' && tripId && (
        <TripNavModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onNavigate={handleModalNavigate}
          activeTab={currentTab}
        />
      )}
    </>
  );
}
