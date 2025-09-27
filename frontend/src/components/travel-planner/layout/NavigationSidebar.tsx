import React from 'react';
import { 
  Eye, 
  MapPin, 
  Calculator, 
  Package, 
  FileText, 
  FolderOpen,
  Search
} from 'lucide-react';
import { NavigationSidebarProps } from '../shared/types';
import { useTranslation } from 'react-i18next';
import { Button } from '../../ui/button';

export function NavigationSidebar({ activeView, onViewChange }: NavigationSidebarProps) {
  const { t } = useTranslation();
  
  const navigationItems = [
    {
      id: 'view',
      label: t('navigation.view'),
      icon: Eye,
      tooltip: 'Trip Overview & Analytics',
    },
    {
      id: 'plan',
      label: t('navigation.plan'),
      icon: MapPin,
      tooltip: 'Day-by-Day Planning',
    },
    {
      id: 'budget',
      label: t('navigation.budget'),
      icon: Calculator,
      tooltip: 'Budget & Expenses',
    },
    {
      id: 'packing',
      label: t('navigation.packing'),
      icon: Package,
      tooltip: 'Packing Lists',
    },
    {
      id: 'collection',
      label: t('navigation.collection'),
      icon: FolderOpen,
      tooltip: 'Saved Collections',
    },
    {
      id: 'docs',
      label: t('navigation.docs'),
      icon: FileText,
      tooltip: 'Travel Documents',
    },
  ];
  
  return (
    <div className="w-64 bg-white border-r border-gray-200 flex flex-col">
      <div className="border-b border-gray-200 p-4">
        <div className="flex items-center space-x-2">
          <div className="w-8 h-8 bg-gradient-to-r from-teal-400 to-teal-600 rounded-lg flex items-center justify-center">
            <span className="text-white font-bold text-sm">T</span>
          </div>
          <span className="text-xl font-semibold">TravelPlanner</span>
        </div>
      </div>
      
      <div className="flex-1 p-2">
        <nav className="space-y-1">
          {navigationItems.map((item) => {
            const Icon = item.icon;
            return (
              <Button
                key={item.id}
                variant={activeView === item.id ? "default" : "ghost"}
                onClick={() => onViewChange(item.id)}
                className="w-full justify-start min-h-[44px] h-auto py-3 px-3"
                title={item.tooltip}
              >
                <Icon className="w-4 h-4 mr-3" />
                <span>{item.label}</span>
              </Button>
            );
          })}
        </nav>
      </div>
    </div>
  );
}