/**
 * Mobile Tabs Component
 * Horizontal scrollable tabs for mobile trip detail navigation
 */

import { useRef } from 'react';
import { cn } from '@/lib/utils';
import { LucideIcon } from 'lucide-react';

interface Tab {
  id: string;
  label: string;
  icon: LucideIcon;
}

interface MobileTabsProps {
  tabs: Tab[];
  activeTab: string;
  onTabChange: (tabId: string) => void;
}

export function MobileTabs({ tabs, activeTab, onTabChange }: MobileTabsProps) {
  const scrollRef = useRef<HTMLDivElement>(null);

  return (
    <div className="md:hidden sticky top-0 z-40 bg-white/80 backdrop-blur-md border-b border-gray-100">
      <div
        ref={scrollRef}
        className="flex overflow-x-auto scrollbar-hide px-2 py-2 gap-1.5"
        style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
      >
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;

          return (
            <button
              key={tab.id}
              onClick={() => onTabChange(tab.id)}
              className={cn(
                'relative flex flex-col items-center justify-center',
                'px-3 py-1.5 rounded-xl',
                'min-w-[64px] min-h-[52px]',
                'transition-all duration-200',
                'flex-shrink-0 touch-manipulation active:scale-95',
                isActive
                  ? 'bg-primary/10 text-primary'
                  : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'
              )}
            >
              {/* Icon with subtle background */}
              <div className={cn(
                'flex items-center justify-center w-7 h-7 rounded-lg mb-0.5 transition-colors',
                isActive ? 'bg-primary/15' : 'bg-transparent'
              )}>
                <Icon className="w-4 h-4" />
              </div>
              
              {/* Label */}
              <span className={cn(
                'text-[10px] font-medium leading-tight',
                isActive ? 'text-primary' : 'text-gray-600'
              )}>
                {tab.label}
              </span>
              
              {/* Active indicator dot */}
              {isActive && (
                <div className="absolute bottom-0.5 left-1/2 -translate-x-1/2 w-1 h-1 rounded-full bg-primary" />
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}
