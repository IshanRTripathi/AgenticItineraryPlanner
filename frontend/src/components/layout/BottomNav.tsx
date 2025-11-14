/**
 * Bottom Navigation Component
 * Modern mobile menu with floating center button
 * Uses InteractiveMenu component with custom styling
 */

import { useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from '@/i18n';
import { Home, Search, MapPlus, User, Map } from 'lucide-react';
import { InteractiveMenu, type InteractiveMenuItem } from '@/components/ui/modern-mobile-menu';
import { useMemo, useState, useEffect, useRef } from 'react';

interface BottomNavProps {
  hide?: boolean;
}

export function BottomNav({ hide = false }: BottomNavProps) {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const [isVisible, setIsVisible] = useState(true);
  const lastScrollY = useRef(0);

  // Define menu items
  const menuItems: InteractiveMenuItem[] = useMemo(() => [
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
  ], [t]);

  // Determine active index based on current path
  const activeIndex = useMemo(() => {
    const index = menuItems.findIndex(item => {
      if (item.path === '/') {
        return location.pathname === '/';
      }
      return location.pathname.startsWith(item.path!);
    });
    return index >= 0 ? index : 0;
  }, [location.pathname, menuItems]);

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
    if (path) {
      navigate(path);
    }
  };

  if (hide) {
    return null;
  }

  return (
    <InteractiveMenu
      items={menuItems}
      activeIndex={activeIndex}
      onItemClick={handleItemClick}
      accentColor="hsl(var(--primary))"
      isHidden={!isVisible}
    />
  );
}
