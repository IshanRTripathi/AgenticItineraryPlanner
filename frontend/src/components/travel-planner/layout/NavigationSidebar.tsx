import React, { useEffect, useRef, useState } from 'react';
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
import { NavigationSidebarProps } from '../shared/types';
import { useTranslation } from 'react-i18next';
import { Button } from '../../ui/button';
import { useDeviceDetection } from '../../../hooks/useDeviceDetection';

export function NavigationSidebar({ activeView, onViewChange }: NavigationSidebarProps) {
  const { t } = useTranslation();
  const { isMobile, isTablet } = useDeviceDetection();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const mountedRef = useRef(true);

  // Debug: Log component render
  console.log('NavigationSidebar: Component render, sidebarOpen:', sidebarOpen, 'isMobile:', isMobile, 'isTablet:', isTablet);

  useEffect(() => {
    return () => { mountedRef.current = false; }
  }, []);

  // Debug: Track sidebarOpen state changes
  useEffect(() => {
    console.log('NavigationSidebar: sidebarOpen state changed to:', sidebarOpen);
  }, [sidebarOpen]);

  const navigationItems = [
    { id: 'view', label: t('navigation.view'), icon: Eye, tooltip: 'Trip Overview & Analytics' },
    { id: 'plan', label: t('navigation.plan'), icon: MapPin, tooltip: 'Day-by-Day Planning' },
    { id: 'budget', label: t('navigation.budget'), icon: Calculator, tooltip: 'Budget & Expenses' },
    { id: 'packing', label: t('navigation.packing'), icon: Package, tooltip: 'Packing Lists' },
    { id: 'collection', label: t('navigation.collection'), icon: FolderOpen, tooltip: 'Saved Collections' },
    { id: 'docs', label: t('navigation.docs'), icon: FileText, tooltip: 'Travel Documents' },
  ];

  const handleItemClick = (itemId: string) => {
    // Debug logging
    console.log('NavigationSidebar: handleItemClick', itemId);
    onViewChange(itemId);

    // Close the sidebar on mobile/tablet — but guard against updating state after unmount
    if ((isMobile || isTablet) && mountedRef.current) {
      console.log('NavigationSidebar: will close sidebar on mobile');
      setSidebarOpen(false);
    }
  };

  const handleSidebarToggle = (e?: React.MouseEvent) => {
    console.log('NavigationSidebar: toggle sidebar, sidebarOpen before:', sidebarOpen);
    e?.stopPropagation();
    setSidebarOpen(prev => {
      console.log('NavigationSidebar: setSidebarOpen called, prev:', prev, 'new:', !prev);
      return !prev;
    });
  };

  // Desktop: Always show sidebar as part of layout
  if (!isMobile && !isTablet) {
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
                  onClick={() => handleItemClick(item.id)}
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

  // Mobile/Tablet: Show hamburger button and overlay sidebar
  return (
    <>
      {/* Hamburger Menu Button - hide while sidebarOpen to avoid it sitting above overlay */}
      <button
        onClick={(e) => {
          console.log('NavigationSidebar: hamburger button clicked, current sidebarOpen:', sidebarOpen);
          handleSidebarToggle(e);
        }}
        aria-expanded={sidebarOpen}
        aria-controls="mobile-sidebar"
        className={`fixed top-4 left-4 z-50 w-12 h-12 bg-white shadow-lg border border-gray-200 rounded-lg flex items-center justify-center hover:bg-gray-50 transition-colors ${sidebarOpen ? 'hidden' : 'block'}`}
        style={{ display: sidebarOpen ? 'none' : 'flex' }}
      >
        <Menu className="w-5 h-5 text-gray-700" />
      </button>

      {/* Transparent overlay - allows clicking outside to close sidebar */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40"
          onClick={(e) => { 
            e.stopPropagation(); 
            console.log('NavigationSidebar: outside area clicked, closing sidebar');
            setSidebarOpen(false); 
          }}
          onKeyDown={(e) => { if (e.key === 'Escape') setSidebarOpen(false); }}
          role="presentation"
        />
      )}

      {/* Sidebar Drawer */}
      <aside
        id="mobile-sidebar"
        className={`fixed top-0 left-0 h-full w-64 bg-white shadow-xl z-50 transition-transform duration-300 ease-in-out ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}`}
        aria-hidden={!sidebarOpen}
        onClick={(e) => e.stopPropagation()}
        style={{ 
          transform: sidebarOpen ? 'translateX(0)' : 'translateX(-100%)',
          display: 'block'
        }}
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
            onClick={() => {
              console.log('NavigationSidebar: close button clicked, current sidebarOpen:', sidebarOpen);
              setSidebarOpen(false);
              console.log('NavigationSidebar: setSidebarOpen(false) called');
            }}
            aria-label="Close navigation"
            className="w-10 h-10 flex items-center justify-center hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5 text-gray-600" />
          </button>
        </div>

        {/* Navigation Items */}
        <div className="flex-1 p-2">
          <nav className="space-y-1">
            {navigationItems.map((item) => {
              const Icon = item.icon;
              return (
                <Button
                  key={item.id}
                  variant={activeView === item.id ? "default" : "ghost"}
                  onClick={() => handleItemClick(item.id)}
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
      </aside>
    </>
  );
}