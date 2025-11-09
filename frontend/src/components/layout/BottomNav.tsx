/**
 * Bottom Navigation Component
 * Mobile-only bottom navigation bar with enhanced UI/UX
 * Features: Auto-hide on scroll, smaller icons, proper alignment
 * Design: Material 3 + Apple HIG + Modern iOS
 */

import { Home, Search, PlusCircle, Calendar, User } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { useState, useEffect, useRef } from 'react';
import { useTranslation } from '@/i18n';

interface NavItem {
  id: string;
  label: string;
  icon: typeof Home;
  path: string;
  highlight?: boolean;
  badge?: number;
}

// Navigation items configuration (labels will be translated)
const getNavItems = (t: (key: string) => string): NavItem[] => [
  {
    id: 'home',
    label: t('components.bottomNav.home'),
    icon: Home,
    path: '/',
  },
  {
    id: 'search',
    label: t('components.bottomNav.search'),
    icon: Search,
    path: '/search',
  },
  {
    id: 'create',
    label: t('components.bottomNav.plan'),
    icon: PlusCircle,
    path: '/planner',
    highlight: true,
  },
  {
    id: 'trips',
    label: t('components.bottomNav.trips'),
    icon: Calendar,
    path: '/dashboard',
  },
  {
    id: 'profile',
    label: t('components.bottomNav.profile'),
    icon: User,
    path: '/profile',
  },
];

interface BottomNavProps {
  hide?: boolean;
}

export function BottomNav({ hide = false }: BottomNavProps) {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const [rippleEffect, setRippleEffect] = useState<string | null>(null);
  const [isVisible, setIsVisible] = useState(true);
  const lastScrollY = useRef(0);
  
  // Get translated nav items
  const NAV_ITEMS = getNavItems(t);

  const isActive = (path: string) => {
    if (path === '/') {
      return location.pathname === '/';
    }
    return location.pathname.startsWith(path);
  };

  const handleNavClick = (item: NavItem) => {
    setRippleEffect(item.id);
    setTimeout(() => setRippleEffect(null), 600);
    navigate(item.path);
  };

  // Auto-hide on scroll down, show on scroll up
  useEffect(() => {
    const handleScroll = () => {
      const currentScrollY = window.scrollY;
      const scrollDifference = currentScrollY - lastScrollY.current;

      // Only hide/show if scrolled more than 10px
      if (Math.abs(scrollDifference) > 10) {
        if (scrollDifference > 0 && currentScrollY > 100) {
          // Scrolling down & past 100px - hide
          setIsVisible(false);
        } else if (scrollDifference < 0) {
          // Scrolling up - show
          setIsVisible(true);
        }
        lastScrollY.current = currentScrollY;
      }
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, []);

  return (
    <nav 
      className={cn(
        "md:hidden fixed bottom-0 left-0 right-0 z-sticky",
        "bg-white/95 backdrop-blur-xl border-t-2 border-gray-200/80",
        "shadow-[0_-4px_16px_rgba(0,0,0,0.08)]",
        "transition-transform duration-300 ease-in-out",
        (isVisible && !hide) ? "translate-y-0" : "translate-y-full"
      )}
      role="navigation"
      aria-label="Main navigation"
    >
      <div className="flex items-center justify-around px-2 py-2 pb-safe">
        {NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const active = isActive(item.path);
          const isHighlight = item.highlight;
          const showRipple = rippleEffect === item.id;

          // Floating Action Button (FAB) for primary action
          if (isHighlight) {
            return (
              <button
                key={item.id}
                onClick={() => handleNavClick(item)}
                className={cn(
                  'relative flex flex-col items-center justify-center',
                  'transition-all duration-300 ease-out',
                  'touch-manipulation',
                  'group',
                  '-mt-4' // Slightly elevate above nav bar
                )}
                aria-label={item.label}
                aria-current={active ? 'page' : undefined}
              >
                {/* FAB Circle */}
                <div
                  className={cn(
                    'relative flex items-center justify-center',
                    'w-11 h-11 rounded-full',
                    'bg-gradient-to-br from-primary to-primary/90',
                    'shadow-[0_4px_12px_rgba(59,130,246,0.35)]',
                    'transition-all duration-300',
                    'group-active:scale-90 group-active:shadow-[0_2px_8px_rgba(59,130,246,0.3)]',
                    'overflow-hidden'
                  )}
                >
                  <Icon className="w-4 h-4 text-white relative z-10" />
                  
                  {/* Ripple effect */}
                  {showRipple && (
                    <span className="absolute inset-0 bg-white/30 rounded-full animate-ping" />
                  )}
                </div>

                {/* Label */}
                <span className="text-[9px] font-semibold text-primary mt-1">
                  {item.label}
                </span>
              </button>
            );
          }

          // Regular nav items
          return (
            <button
              key={item.id}
              onClick={() => handleNavClick(item)}
              className={cn(
                'relative flex flex-col items-center justify-center gap-0.5',
                'px-2 py-1.5 rounded-xl',
                'min-w-[60px] min-h-[56px]',
                'transition-all duration-300 ease-out',
                'touch-manipulation',
                'group',
                'active:scale-95'
              )}
              aria-label={item.label}
              aria-current={active ? 'page' : undefined}
            >
              {/* Background pill for active state */}
              <div
                className={cn(
                  'absolute inset-0 rounded-xl transition-all duration-300',
                  active && 'bg-primary/10 scale-100',
                  !active && 'bg-transparent scale-95 opacity-0'
                )}
              />

              {/* Icon container */}
              <div className="relative flex items-center justify-center h-5">
                <div
                  className={cn(
                    'relative transition-all duration-300',
                    active && 'scale-105'
                  )}
                >
                  <Icon 
                    className={cn(
                      'w-4 h-4 transition-all duration-300',
                      active && 'text-primary stroke-[2.5]',
                      !active && 'text-gray-500 stroke-[2]'
                    )} 
                  />
                  
                  {/* Badge indicator */}
                  {item.badge && item.badge > 0 && (
                    <span className="absolute -top-1 -right-1 flex items-center justify-center min-w-[14px] h-[14px] px-0.5 bg-red-500 text-white text-[8px] font-bold rounded-full border-2 border-white shadow-sm">
                      {item.badge > 99 ? '99+' : item.badge}
                    </span>
                  )}
                </div>

                {/* Active indicator line */}
                {active && (
                  <div className="absolute -bottom-0.5 left-1/2 -translate-x-1/2 w-3 h-0.5 rounded-full bg-primary animate-in slide-in-from-bottom-1 duration-300" />
                )}
              </div>

              {/* Label */}
              <span 
                className={cn(
                  'text-[9px] font-medium transition-all duration-300 relative z-10 mt-0.5',
                  active && 'text-primary font-semibold',
                  !active && 'text-gray-600'
                )}
              >
                {item.label}
              </span>

              {/* Ripple effect */}
              {showRipple && (
                <span className="absolute inset-0 bg-primary/10 rounded-xl animate-ping" />
              )}
            </button>
          );
        })}
      </div>

      {/* Safe area spacer for devices with home indicator */}
      <div className="h-safe-bottom bg-white/95 backdrop-blur-xl" />
    </nav>
  );
}
