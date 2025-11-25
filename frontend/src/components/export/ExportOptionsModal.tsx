/**
 * Export Options Modal
 * Allows users to customize PDF export settings
 */

import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
// RadioGroup not available, using custom implementation
import { Loader2, Download } from 'lucide-react';
import { analytics } from '@/services/analytics';

export interface ExportOptions {
  includeOverview: boolean;
  includeDayByDay: boolean;
  includeMap: boolean;
  includeBookings: boolean;
  layout: 'portrait' | 'landscape';
  pageSize: 'A4' | 'Letter';
  includeBranding: boolean;
  includeBackgroundGraphics: boolean;
}

interface ExportOptionsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onExport: (options: ExportOptions) => Promise<void>;
  isExporting?: boolean;
}

const defaultOptions: ExportOptions = {
  includeOverview: true,
  includeDayByDay: true,
  includeMap: true,
  includeBookings: true,
  layout: 'portrait',
  pageSize: 'A4',
  includeBranding: true,
  includeBackgroundGraphics: true, // Default to checked
};

export function ExportOptionsModal({
  isOpen,
  onClose,
  onExport,
  isExporting = false,
}: ExportOptionsModalProps) {
  const [options, setOptions] = useState<ExportOptions>(defaultOptions);

  const handleExport = async () => {
    try {
      // Track PDF export initiated
      analytics.track('pdf_export_initiated', {
        format: 'pdf',
        layout: options.layout,
        pageSize: options.pageSize
      });
      
      await onExport(options);
      
      // Track PDF export completed
      analytics.track('pdf_export_completed', {
        format: 'pdf',
        layout: options.layout,
        pageSize: options.pageSize
      });
    } catch (error) {
      // Track PDF export failed
      analytics.track('pdf_export_failed', {
        error: error instanceof Error ? error.message : 'Unknown error'
      });
      throw error;
    }
  };

  const updateOption = <K extends keyof ExportOptions>(
    key: K,
    value: ExportOptions[K]
  ) => {
    setOptions((prev) => ({ ...prev, [key]: value }));
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[480px] max-h-[90vh] flex flex-col p-0">
        <div className="flex-shrink-0 p-6 pb-4">
          <DialogHeader>
            <DialogTitle className="text-base sm:text-lg">Export PDF Options</DialogTitle>
            <DialogDescription className="text-xs sm:text-sm">
              Customize your PDF export settings
            </DialogDescription>
          </DialogHeader>
        </div>

        <div className="flex-1 overflow-y-auto px-6 pb-48 sm:pb-6">
          <div className="space-y-3">
          {/* Content Selection */}
          <div className="space-y-2">
            <Label className="text-xs sm:text-sm font-semibold">Include Sections</Label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5">
              <label
                htmlFor="overview"
                className={`flex items-center space-x-2 p-2 rounded-md border-2 cursor-pointer transition-all text-xs sm:text-sm ${
                  options.includeOverview
                    ? 'border-primary bg-primary/10 shadow-sm'
                    : 'border-gray-200 hover:border-gray-300 hover:bg-accent/50'
                }`}
              >
                <Checkbox
                  id="overview"
                  checked={options.includeOverview}
                  onCheckedChange={(checked) =>
                    updateOption('includeOverview', !!checked)
                  }
                  className="pointer-events-none h-3.5 w-3.5 sm:h-4 sm:w-4"
                />
                <span className="font-medium flex-1">Trip Overview</span>
              </label>

              <label
                htmlFor="dayByDay"
                className={`flex items-center space-x-2 p-2 rounded-md border-2 cursor-pointer transition-all text-xs sm:text-sm ${
                  options.includeDayByDay
                    ? 'border-primary bg-primary/10 shadow-sm'
                    : 'border-gray-200 hover:border-gray-300 hover:bg-accent/50'
                }`}
              >
                <Checkbox
                  id="dayByDay"
                  checked={options.includeDayByDay}
                  onCheckedChange={(checked) =>
                    updateOption('includeDayByDay', !!checked)
                  }
                  className="pointer-events-none h-3.5 w-3.5 sm:h-4 sm:w-4"
                />
                <span className="font-medium flex-1">Day-by-Day Details</span>
              </label>

              <label
                htmlFor="map"
                className={`flex items-center space-x-2 p-2 rounded-md border-2 cursor-pointer transition-all text-xs sm:text-sm ${
                  options.includeMap
                    ? 'border-primary bg-primary/10 shadow-sm'
                    : 'border-gray-200 hover:border-gray-300 hover:bg-accent/50'
                }`}
              >
                <Checkbox
                  id="map"
                  checked={options.includeMap}
                  onCheckedChange={(checked) =>
                    updateOption('includeMap', !!checked)
                  }
                  className="pointer-events-none h-3.5 w-3.5 sm:h-4 sm:w-4"
                />
                <span className="font-medium flex-1">Map & Locations</span>
              </label>

              <label
                htmlFor="bookings"
                className={`flex items-center space-x-2 p-2 rounded-md border-2 cursor-pointer transition-all text-xs sm:text-sm ${
                  options.includeBookings
                    ? 'border-primary bg-primary/10 shadow-sm'
                    : 'border-gray-200 hover:border-gray-300 hover:bg-accent/50'
                }`}
              >
                <Checkbox
                  id="bookings"
                  checked={options.includeBookings}
                  onCheckedChange={(checked) =>
                    updateOption('includeBookings', !!checked)
                  }
                  className="pointer-events-none h-3.5 w-3.5 sm:h-4 sm:w-4"
                />
                <span className="font-medium flex-1">Booking Confirmations</span>
              </label>
            </div>
          </div>

          {/* Layout & Page Size */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 sm:gap-4">
            {/* Layout Options */}
            <div className="space-y-2">
              <Label className="text-xs sm:text-sm font-semibold">Layout</Label>
              <div className="flex gap-1.5">
                <label
                  htmlFor="portrait"
                  className={`flex-1 flex items-center justify-center p-2 rounded-md border-2 cursor-pointer transition-all text-xs sm:text-sm ${
                    options.layout === 'portrait'
                      ? 'border-primary bg-primary/10 shadow-sm'
                      : 'border-gray-200 hover:border-gray-300 hover:bg-accent/50'
                  }`}
                >
                  <input
                    type="radio"
                    id="portrait"
                    name="layout"
                    value="portrait"
                    checked={options.layout === 'portrait'}
                    onChange={(e) => updateOption('layout', e.target.value as 'portrait' | 'landscape')}
                    className="sr-only"
                  />
                  <span className="font-medium">Portrait</span>
                </label>
                <label
                  htmlFor="landscape"
                  className={`flex-1 flex items-center justify-center p-2 rounded-md border-2 cursor-pointer transition-all text-xs sm:text-sm ${
                    options.layout === 'landscape'
                      ? 'border-primary bg-primary/10 shadow-sm'
                      : 'border-gray-200 hover:border-gray-300 hover:bg-accent/50'
                  }`}
                >
                  <input
                    type="radio"
                    id="landscape"
                    name="layout"
                    value="landscape"
                    checked={options.layout === 'landscape'}
                    onChange={(e) => updateOption('layout', e.target.value as 'portrait' | 'landscape')}
                    className="sr-only"
                  />
                  <span className="font-medium">Landscape</span>
                </label>
              </div>
            </div>

            {/* Page Size */}
            <div className="space-y-2">
              <Label className="text-xs sm:text-sm font-semibold">Page Size</Label>
              <div className="flex gap-1.5">
                <label
                  htmlFor="a4"
                  className={`flex-1 flex items-center justify-center p-2 rounded-md border-2 cursor-pointer transition-all text-xs sm:text-sm ${
                    options.pageSize === 'A4'
                      ? 'border-primary bg-primary/10 shadow-sm'
                      : 'border-gray-200 hover:border-gray-300 hover:bg-accent/50'
                  }`}
                >
                  <input
                    type="radio"
                    id="a4"
                    name="pageSize"
                    value="A4"
                    checked={options.pageSize === 'A4'}
                    onChange={(e) => updateOption('pageSize', e.target.value as 'A4' | 'Letter')}
                    className="sr-only"
                  />
                  <span className="font-medium">A4</span>
                </label>
                <label
                  htmlFor="letter"
                  className={`flex-1 flex items-center justify-center p-2 rounded-md border-2 cursor-pointer transition-all text-xs sm:text-sm ${
                    options.pageSize === 'Letter'
                      ? 'border-primary bg-primary/10 shadow-sm'
                      : 'border-gray-200 hover:border-gray-300 hover:bg-accent/50'
                  }`}
                >
                  <input
                    type="radio"
                    id="letter"
                    name="pageSize"
                    value="Letter"
                    checked={options.pageSize === 'Letter'}
                    onChange={(e) => updateOption('pageSize', e.target.value as 'A4' | 'Letter')}
                    className="sr-only"
                  />
                  <span className="font-medium">Letter</span>
                </label>
              </div>
            </div>
          </div>

          {/* Print Options */}
          <div className="space-y-2 p-2.5 bg-accent/30 rounded-md border border-border">
            <Label className="text-xs sm:text-sm font-semibold">Print Options</Label>
            <div className="space-y-1.5">
              <label
                htmlFor="backgroundGraphics"
                className={`flex items-center space-x-2 p-2 rounded-md border-2 cursor-pointer transition-all text-xs sm:text-sm ${
                  options.includeBackgroundGraphics
                    ? 'border-primary bg-primary/10 shadow-sm'
                    : 'border-gray-200 hover:border-gray-300 hover:bg-background'
                }`}
              >
                <Checkbox
                  id="backgroundGraphics"
                  checked={options.includeBackgroundGraphics}
                  onCheckedChange={(checked) =>
                    updateOption('includeBackgroundGraphics', !!checked)
                  }
                  className="pointer-events-none h-3.5 w-3.5 sm:h-4 sm:w-4"
                />
                <span className="font-medium leading-tight flex-1">
                  Background graphics (colors, images, charts)
                </span>
              </label>
              
              <label
                htmlFor="branding"
                className={`flex items-center space-x-2 p-2 rounded-md border-2 cursor-pointer transition-all text-xs sm:text-sm ${
                  options.includeBranding
                    ? 'border-primary bg-primary/10 shadow-sm'
                    : 'border-gray-200 hover:border-gray-300 hover:bg-background'
                }`}
              >
                <Checkbox
                  id="branding"
                  checked={options.includeBranding}
                  onCheckedChange={(checked) =>
                    updateOption('includeBranding', !!checked)
                  }
                  className="pointer-events-none h-3.5 w-3.5 sm:h-4 sm:w-4"
                />
                <span className="font-medium leading-tight flex-1">
                  Include EaseMyTrip branding
                </span>
              </label>
            </div>
          </div>
        </div>
        </div>

        {/* Floating buttons on mobile, normal footer on desktop */}
        <div className="fixed sm:relative bottom-0 left-0 right-0 sm:bottom-auto sm:left-auto sm:right-auto bg-background border-t sm:border-t-0 p-4 sm:p-6 sm:pt-2 shadow-lg sm:shadow-none z-50">
          <div className="flex flex-col-reverse sm:flex-row gap-2 max-w-[480px] mx-auto">
            <Button 
              variant="outline" 
              onClick={onClose} 
              disabled={isExporting}
              className="w-full sm:w-auto h-9 sm:h-9 text-sm sm:text-sm px-4 sm:px-4"
            >
              Cancel
            </Button>
            <Button 
              onClick={handleExport} 
              disabled={isExporting}
              className="w-full sm:w-auto h-9 sm:h-9 text-sm sm:text-sm px-4 sm:px-4"
            >
              {isExporting ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 sm:w-3.5 sm:h-3.5 mr-2 sm:mr-2 animate-spin" />
                  <span>Generating...</span>
                </>
              ) : (
                <>
                  <Download className="w-3.5 h-3.5 sm:w-3.5 sm:h-3.5 mr-2 sm:mr-2" />
                  <span>Export PDF</span>
                </>
              )}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
