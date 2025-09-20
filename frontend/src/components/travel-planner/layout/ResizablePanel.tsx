import React, { useCallback, useEffect, useRef } from 'react';
import { ResizablePanelProps } from '../shared/types';

export function ResizablePanel({ 
  leftPanelWidth, 
  onWidthChange, 
  leftContent, 
  rightContent 
}: ResizablePanelProps) {
  const [isResizing, setIsResizing] = React.useState(false);
  const resizeRef = useRef<HTMLDivElement>(null);

  // Resize handlers
  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    setIsResizing(true);
  }, []);

  const handleMouseMove = useCallback((e: MouseEvent) => {
    if (!isResizing || !resizeRef.current) return;
    
    const container = resizeRef.current.parentElement;
    if (!container) return;
    
    const containerRect = container.getBoundingClientRect();
    const newWidth = ((e.clientX - containerRect.left) / containerRect.width) * 100;
    
    // Constrain between 20% and 80%
    const constrainedWidth = Math.min(Math.max(newWidth, 20), 80);
    onWidthChange(constrainedWidth);
  }, [isResizing, onWidthChange]);

  const handleMouseUp = useCallback(() => {
    setIsResizing(false);
  }, []);

  useEffect(() => {
    if (isResizing) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
    } else {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    }

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };
  }, [isResizing, handleMouseMove, handleMouseUp]);

  return (
    <div className="flex flex-1" ref={resizeRef}>
      <div style={{ width: `${leftPanelWidth}%` }} className="flex-shrink-0 flex flex-col">
        {leftContent}
      </div>
      
      {/* Resize Handle */}
      <div 
        className={`w-1 bg-gray-300 hover:bg-blue-400 cursor-col-resize flex-shrink-0 relative group ${isResizing ? 'bg-blue-500' : ''}`}
        onMouseDown={handleMouseDown}
        title="Drag to resize panels"
      >
        <div className="absolute inset-y-0 -left-1 -right-1 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
          <div className="w-3 h-8 bg-blue-400 rounded-sm flex items-center justify-center">
            <div className="w-0.5 h-4 bg-white rounded"></div>
          </div>
        </div>
      </div>
      
      <div style={{ width: `${100 - leftPanelWidth}%` }} className="bg-gray-100 relative border-l border-gray-200 flex-shrink-0 flex flex-col overflow-hidden">
        {rightContent}
      </div>
    </div>
  );
}
