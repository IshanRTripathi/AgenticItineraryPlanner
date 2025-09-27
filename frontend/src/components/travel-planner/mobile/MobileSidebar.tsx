import React from 'react';
import { Button } from '../../ui/button';
import { 
  Eye, 
  MapPin, 
  Calculator, 
  Package, 
  FileText, 
  FolderOpen,
  Menu,
  X
} from 'lucide-react';
import { useTranslation } from 'react-i18next';

interface MobileSidebarProps {
  activeView: string;
  onViewChange: (view: string) => void;
  isOpen: boolean;
  onToggle: () => void;
}

export function MobileSidebar({ 
  activeView, 
  onViewChange, 
  isOpen, 
  onToggle 
}: MobileSidebarProps) {
  const { t } = useTranslation();
  
  // Debug logging
  console.log('MobileSidebar render:', { activeView, isOpen });
  
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

  const handleItemClick = (itemId: string) => {
    onViewChange(itemId);
    onToggle(); // Close sidebar after selection
  };

  return (
    <>
      {/* Hamburger Menu Button */}
      <button
        onClick={onToggle}
        className="fixed top-4 left-4 z-[60] min-h-[44px] min-w-[44px] bg-white shadow-lg border border-gray-200 rounded-lg flex items-center justify-center"
        style={{ zIndex: 60 }}
      >
        <Menu className="w-5 h-5" />
      </button>

      {/* Overlay */}
      {isOpen && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50"
          onClick={onToggle}
          style={{ zIndex: 55 }}
        />
      )}

      {/* Sidebar Drawer */}
      <div
        className={`fixed top-0 left-0 h-full w-64 bg-white shadow-xl transform transition-transform duration-300 ease-in-out ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
        style={{ zIndex: 60 }}
      >
        {/* Header */}
        <div className="border-b border-gray-200 p-4 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 bg-gradient-to-r from-teal-400 to-teal-600 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-sm">T</span>
            </div>
            <span className="text-xl font-semibold">TravelPlanner</span>
          </div>
          <button
            onClick={onToggle}
            className="min-h-[44px] min-w-[44px] p-2 flex items-center justify-center hover:bg-gray-100 rounded"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
        
        {/* Navigation Items */}
        <div className="p-2">
          <nav className="space-y-1">
            {navigationItems.map((item) => {
              const Icon = item.icon;
              return (
                <button
                  key={item.id}
                  onClick={() => handleItemClick(item.id)}
                  className={`w-full flex items-center justify-start min-h-[44px] h-auto py-3 px-3 text-left rounded hover:bg-gray-100 ${
                    activeView === item.id ? 'bg-blue-100 text-blue-900' : 'text-gray-700'
                  }`}
                  title={item.tooltip}
                >
                  <Icon className="w-4 h-4 mr-3 flex-shrink-0" />
                  <span className="truncate">{item.label}</span>
                </button>
              );
            })}
          </nav>
        </div>
      </div>
    </>
  );
}
