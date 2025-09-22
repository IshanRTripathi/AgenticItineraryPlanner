import React, { useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { ResizablePanelProps } from '../shared/types';

export function ResizablePanel({ 
  leftPanelWidth, 
  onWidthChange, 
  leftContent, 
  rightContent 
}: ResizablePanelProps) {
  const [isExpanded, setIsExpanded] = useState(false);

  // Toggle between collapsed and expanded states
  const togglePanel = () => {
    if (isExpanded) {
      // Collapse to 25% (minimal view)
      onWidthChange(25);
      setIsExpanded(false);
    } else {
      // Expand to 45% (default view)
      onWidthChange(45);
      setIsExpanded(true);
    }
  };

  return (
    <div className="flex flex-1">
      <div 
        style={{ 
          width: `${leftPanelWidth}%`,
          transition: 'width 0.3s cubic-bezier(0.4, 0, 0.2, 1)'
        }} 
        className="flex-shrink-0 flex flex-col"
      >
        {leftContent}
      </div>
      
      {/* Toggle Button */}
      <div className="w-1 bg-gray-300 flex-shrink-0 relative group">
        <button
          onClick={togglePanel}
          className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-6 h-8 bg-white rounded-sm shadow-sm border border-gray-200 flex items-center justify-center hover:bg-gray-50 transition-all duration-200 z-10"
          title={isExpanded ? "Collapse panel" : "Expand panel"}
        >
          {isExpanded ? (
            <ChevronLeft className="w-4 h-4 text-blue-600 transition-transform duration-200" />
          ) : (
            <ChevronRight className="w-4 h-4 text-blue-600 transition-transform duration-200" />
          )}
        </button>
      </div>
      
      <div 
        style={{ 
          width: `${100 - leftPanelWidth}%`,
          transition: 'width 0.3s cubic-bezier(0.4, 0, 0.2, 1)'
        }} 
        className="bg-gray-100 relative border-l border-gray-200 flex-shrink-0 flex flex-col overflow-hidden"
      >
        {rightContent}
      </div>
    </div>
  );
}
