import React from 'react';
import { Button } from '../ui/button';
import { 
  Home, 
  MapPin, 
  Plus, 
  Calendar,
  Settings,
  HelpCircle
} from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { cn } from '../ui/utils';

interface GlobalNavigationProps {
  className?: string;
  variant?: 'horizontal' | 'vertical';
  showLabels?: boolean;
}

export function GlobalNavigation({ 
  className = '', 
  variant = 'horizontal',
  showLabels = true 
}: GlobalNavigationProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated } = useAuth();

  const navigationItems = [
    {
      path: '/',
      label: 'Home',
      icon: Home,
      requiresAuth: false
    },
    {
      path: '/dashboard',
      label: 'My Trips',
      icon: MapPin,
      requiresAuth: true
    }
  ];

  const handleNavigation = (path: string) => {
    navigate(path);
  };

  const isActive = (path: string) => {
    if (path === '/') {
      return location.pathname === '/';
    }
    return location.pathname.startsWith(path);
  };

  const filteredItems = navigationItems.filter(item => 
    !item.requiresAuth || isAuthenticated
  );

  if (variant === 'vertical') {
    return (
      <nav className={cn("flex flex-col space-y-2", className)}>
        {filteredItems.map((item) => {
          const Icon = item.icon;
          const active = isActive(item.path);
          
          return (
            <Button
              key={item.path}
              variant={active ? "default" : "ghost"}
              size="sm"
              onClick={() => handleNavigation(item.path)}
              className={cn(
                "justify-start",
                showLabels ? "px-3" : "px-2 w-10 h-10",
                active && "bg-blue-100 text-blue-700 hover:bg-blue-200"
              )}
            >
              <Icon className={cn("h-4 w-4", showLabels && "mr-2")} />
              {showLabels && <span>{item.label}</span>}
            </Button>
          );
        })}
      </nav>
    );
  }

  return (
    <nav className={cn("flex items-center space-x-1", className)}>
      {filteredItems.map((item) => {
        const Icon = item.icon;
        const active = isActive(item.path);
        
        return (
          <Button
            key={item.path}
            variant={active ? "default" : "ghost"}
            size="sm"
            onClick={() => handleNavigation(item.path)}
            className={cn(
              "flex items-center space-x-2",
              active && "bg-blue-100 text-blue-700 hover:bg-blue-200"
            )}
          >
            <Icon className="h-4 w-4" />
            {showLabels && <span>{item.label}</span>}
          </Button>
        );
      })}
    </nav>
  );
}
