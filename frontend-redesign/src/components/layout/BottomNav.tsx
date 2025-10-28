/**
 * Bottom Navigation Component
 * Mobile-only bottom navigation bar
 * Design: Material 3 + Apple HIG
 */

import { Home, Search, PlusCircle, Calendar, User } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import { cn } from '@/lib/utils';

const NAV_ITEMS = [
  {
    id: 'home',
    label: 'Home',
    icon: Home,
    path: '/',
  },
  {
    id: 'search',
    label: 'Search',
    icon: Search,
    path: '/search-results',
  },
  {
    id: 'create',
    label: 'Plan',
    icon: PlusCircle,
    path: '/ai-planner',
    highlight: true,
  },
  {
    id: 'trips',
    label: 'Trips',
    icon: Calendar,
    path: '/dashboard',
  },
  {
    id: 'profile',
    label: 'Profile',
    icon: User,
    path: '/profile',
  },
];

export function BottomNav() {
  const location = useLocation();
  const navigate = useNavigate();

  const isActive = (path: string) => {
    if (path === '/') {
      return location.pathname === '/';
    }
    return location.pathname.startsWith(path);
  };

  return (
    <nav className="md:hidden fixed bottom-0 left-0 right-0 z-sticky bg-white border-t border-border shadow-elevation-3">
      <div className="flex items-center justify-around h-16 px-2">
        {NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const active = isActive(item.path);

          return (
            <button
              key={item.id}
              onClick={() => navigate(item.path)}
              className={cn(
                'flex flex-col items-center justify-center gap-1',
                'px-4 py-2 rounded-lg',
                'min-w-[64px] min-h-[56px]',
                'transition-all duration-200',
                active && 'text-primary',
                !active && 'text-muted-foreground',
                item.highlight && !active && 'text-secondary',
                'active:scale-95'
              )}
            >
              <div
                className={cn(
                  'relative transition-all duration-200',
                  active && 'scale-110',
                  item.highlight && 'bg-secondary/10 p-2 rounded-full'
                )}
              >
                <Icon 
                  className={cn(
                    'w-5 h-5',
                    item.highlight && 'text-secondary'
                  )} 
                />
                {active && (
                  <div className="absolute -bottom-1 left-1/2 -translate-x-1/2 w-1 h-1 rounded-full bg-primary" />
                )}
              </div>
              <span 
                className={cn(
                  'text-xs font-medium transition-all duration-200',
                  active && 'font-semibold'
                )}
              >
                {item.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
}
