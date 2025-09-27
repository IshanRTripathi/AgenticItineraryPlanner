import React from 'react';
import { Button } from '../ui/button';
import { ChevronRight, Home, MapPin, Plus, Calendar, Settings } from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import { cn } from '../ui/utils';

interface BreadcrumbItem {
  label: string;
  path?: string;
  icon?: React.ComponentType<{ className?: string }>;
}

interface BreadcrumbNavigationProps {
  className?: string;
  customItems?: BreadcrumbItem[];
}

export function BreadcrumbNavigation({ 
  className = '',
  customItems 
}: BreadcrumbNavigationProps) {
  const navigate = useNavigate();
  const location = useLocation();

  const getBreadcrumbs = (): BreadcrumbItem[] => {
    if (customItems) {
      return customItems;
    }

    const pathSegments = location.pathname.split('/').filter(Boolean);
    const breadcrumbs: BreadcrumbItem[] = [
      { label: 'Home', path: '/', icon: Home }
    ];

    // Map path segments to breadcrumb items
    pathSegments.forEach((segment, index) => {
      const path = '/' + pathSegments.slice(0, index + 1).join('/');
      
      switch (segment) {
        case 'dashboard':
          breadcrumbs.push({ 
            label: 'My Trips', 
            path, 
            icon: MapPin 
          });
          break;
        case 'planner':
          breadcrumbs.push({ 
            label: 'Trip Planner', 
            path, 
            icon: Calendar 
          });
          break;
        case 'generating':
          breadcrumbs.push({ 
            label: 'Generating Trip', 
            path, 
            icon: Calendar 
          });
          break;
        case 'cost':
          breadcrumbs.push({ 
            label: 'Cost & Booking', 
            path, 
            icon: Settings 
          });
          break;
        case 'checkout':
          breadcrumbs.push({ 
            label: 'Checkout', 
            path, 
            icon: Settings 
          });
          break;
        case 'confirmation':
          breadcrumbs.push({ 
            label: 'Booking Confirmation', 
            path, 
            icon: Settings 
          });
          break;
        case 'share':
          breadcrumbs.push({ 
            label: 'Share Trip', 
            path, 
            icon: Settings 
          });
          break;
        default:
          // For dynamic routes like /trip/:id or /itinerary/:id
          if (segment.startsWith('trip-') || segment.startsWith('it_')) {
            breadcrumbs.push({ 
              label: 'Trip Details', 
              path 
            });
          } else {
            breadcrumbs.push({ 
              label: segment.charAt(0).toUpperCase() + segment.slice(1), 
              path 
            });
          }
      }
    });

    return breadcrumbs;
  };

  const breadcrumbs = getBreadcrumbs();
  const isLastItem = (index: number) => index === breadcrumbs.length - 1;

  const handleNavigation = (path: string) => {
    navigate(path);
  };

  if (breadcrumbs.length <= 1) {
    return null; // Don't show breadcrumbs if we're only on the home page
  }

  return (
    <nav className={cn("flex items-center space-x-1 text-sm", className)}>
      {breadcrumbs.map((item, index) => {
        const Icon = item.icon;
        const isLast = isLastItem(index);
        
        return (
          <React.Fragment key={index}>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => item.path && handleNavigation(item.path)}
              disabled={!item.path || isLast}
              className={cn(
                "h-auto p-1 text-gray-600 hover:text-gray-900",
                isLast && "text-gray-900 font-medium cursor-default",
                !item.path && "cursor-default"
              )}
            >
              {Icon && <Icon className="h-3 w-3 mr-1" />}
              <span>{item.label}</span>
            </Button>
            
            {!isLast && (
              <ChevronRight className="h-3 w-3 text-gray-400" />
            )}
          </React.Fragment>
        );
      })}
    </nav>
  );
}
