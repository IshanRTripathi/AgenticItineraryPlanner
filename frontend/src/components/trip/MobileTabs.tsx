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
    <div className="md:hidden sticky top-0 z-40 bg-background border-b shadow-sm">
      <div
        ref={scrollRef}
        className="flex overflow-x-auto scrollbar-hide px-3 py-2 gap-2"
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
                'flex items-center gap-2 px-3 py-2',
                'rounded-full whitespace-nowrap',
                'min-h-[40px] transition-all duration-200',
                'flex-shrink-0 touch-manipulation active:scale-95',
                isActive
                  ? 'bg-primary text-primary-foreground shadow-sm'
                  : 'bg-muted text-muted-foreground hover:bg-muted/80'
              )}
            >
              <Icon className="w-4 h-4" />
              <span className="text-xs font-medium">{tab.label}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
