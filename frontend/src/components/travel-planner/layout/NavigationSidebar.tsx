import React from 'react';
import { Button } from '../../ui/button';
import { 
  Eye, 
  MapPin, 
  Calculator, 
  Package, 
  FileText, 
  Search 
} from 'lucide-react';
import { NavigationSidebarProps } from '../shared/types';

export function NavigationSidebar({ activeView, onViewChange }: NavigationSidebarProps) {
  return (
    <div className="w-64 bg-white border-r border-gray-200 flex flex-col">
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-center space-x-2">
          <div className="w-8 h-8 bg-gradient-to-r from-teal-400 to-teal-600 rounded-lg flex items-center justify-center">
            <span className="text-white font-bold text-sm">T</span>
          </div>
          <span className="text-xl font-semibold">TravelPlanner</span>
        </div>
      </div>
      
      <nav className="flex-1 p-4">
        <div className="space-y-2">
          <Button 
            variant={activeView === 'view' ? 'default' : 'ghost'} 
            className="w-full justify-start"
            onClick={() => onViewChange('view')}
          >
            <Eye className="w-4 h-4 mr-2" />
            View
          </Button>
          <Button 
            variant={activeView === 'plan' ? 'default' : 'ghost'} 
            className="w-full justify-start"
            onClick={() => onViewChange('plan')}
          >
            <MapPin className="w-4 h-4 mr-2" />
            Plan
          </Button>
          <Button 
            variant={activeView === 'budget' ? 'default' : 'ghost'} 
            className="w-full justify-start"
            onClick={() => onViewChange('budget')}
          >
            <Calculator className="w-4 h-4 mr-2" />
            Budget
          </Button>
          <Button 
            variant={activeView === 'packing' ? 'default' : 'ghost'} 
            className="w-full justify-start"
            onClick={() => onViewChange('packing')}
          >
            <Package className="w-4 h-4 mr-2" />
            Packing
          </Button>
          <Button 
            variant={activeView === 'collection' ? 'default' : 'ghost'} 
            className="w-full justify-start"
            onClick={() => onViewChange('collection')}
          >
            <FileText className="w-4 h-4 mr-2" />
            Collection
          </Button>
          <Button 
            variant={activeView === 'docs' ? 'default' : 'ghost'} 
            className="w-full justify-start"
            onClick={() => onViewChange('docs')}
          >
            <FileText className="w-4 h-4 mr-2" />
            Docs
          </Button>
          <Button 
            variant={activeView === 'discover' ? 'default' : 'ghost'} 
            className="w-full justify-start"
            onClick={() => onViewChange('discover')}
          >
            <Search className="w-4 h-4 mr-2" />
            Discover
          </Button>
        </div>
      </nav>
    </div>
  );
}
