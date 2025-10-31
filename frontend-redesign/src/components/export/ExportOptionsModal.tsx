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

export interface ExportOptions {
  includeOverview: boolean;
  includeDayByDay: boolean;
  includeMap: boolean;
  includeBookings: boolean;
  layout: 'portrait' | 'landscape';
  pageSize: 'A4' | 'Letter';
  includeBranding: boolean;
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
};

export function ExportOptionsModal({
  isOpen,
  onClose,
  onExport,
  isExporting = false,
}: ExportOptionsModalProps) {
  const [options, setOptions] = useState<ExportOptions>(defaultOptions);

  const handleExport = async () => {
    await onExport(options);
  };

  const updateOption = <K extends keyof ExportOptions>(
    key: K,
    value: ExportOptions[K]
  ) => {
    setOptions((prev) => ({ ...prev, [key]: value }));
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>Export PDF Options</DialogTitle>
          <DialogDescription>
            Customize your PDF export settings
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 py-4">
          {/* Content Selection */}
          <div className="space-y-3">
            <Label className="text-base font-semibold">Include Sections</Label>
            <div className="space-y-2">
              <div className="flex items-center space-x-2">
                <Checkbox
                  id="overview"
                  checked={options.includeOverview}
                  onCheckedChange={(checked) =>
                    updateOption('includeOverview', checked as boolean)
                  }
                />
                <label
                  htmlFor="overview"
                  className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
                >
                  Trip Overview
                </label>
              </div>

              <div className="flex items-center space-x-2">
                <Checkbox
                  id="dayByDay"
                  checked={options.includeDayByDay}
                  onCheckedChange={(checked) =>
                    updateOption('includeDayByDay', checked as boolean)
                  }
                />
                <label
                  htmlFor="dayByDay"
                  className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
                >
                  Day-by-Day Details
                </label>
              </div>

              <div className="flex items-center space-x-2">
                <Checkbox
                  id="map"
                  checked={options.includeMap}
                  onCheckedChange={(checked) =>
                    updateOption('includeMap', checked as boolean)
                  }
                />
                <label
                  htmlFor="map"
                  className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
                >
                  Map & Locations
                </label>
              </div>

              <div className="flex items-center space-x-2">
                <Checkbox
                  id="bookings"
                  checked={options.includeBookings}
                  onCheckedChange={(checked) =>
                    updateOption('includeBookings', checked as boolean)
                  }
                />
                <label
                  htmlFor="bookings"
                  className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
                >
                  Booking Confirmations
                </label>
              </div>
            </div>
          </div>

          {/* Layout Options */}
          <div className="space-y-3">
            <Label className="text-base font-semibold">Layout</Label>
            <div className="space-y-2">
              <div className="flex items-center space-x-2">
                <input
                  type="radio"
                  id="portrait"
                  name="layout"
                  value="portrait"
                  checked={options.layout === 'portrait'}
                  onChange={(e) => updateOption('layout', e.target.value as 'portrait' | 'landscape')}
                  className="w-4 h-4 text-primary border-gray-300 focus:ring-primary"
                />
                <Label htmlFor="portrait" className="font-normal cursor-pointer">
                  Portrait
                </Label>
              </div>
              <div className="flex items-center space-x-2">
                <input
                  type="radio"
                  id="landscape"
                  name="layout"
                  value="landscape"
                  checked={options.layout === 'landscape'}
                  onChange={(e) => updateOption('layout', e.target.value as 'portrait' | 'landscape')}
                  className="w-4 h-4 text-primary border-gray-300 focus:ring-primary"
                />
                <Label htmlFor="landscape" className="font-normal cursor-pointer">
                  Landscape
                </Label>
              </div>
            </div>
          </div>

          {/* Page Size */}
          <div className="space-y-3">
            <Label className="text-base font-semibold">Page Size</Label>
            <div className="space-y-2">
              <div className="flex items-center space-x-2">
                <input
                  type="radio"
                  id="a4"
                  name="pageSize"
                  value="A4"
                  checked={options.pageSize === 'A4'}
                  onChange={(e) => updateOption('pageSize', e.target.value as 'A4' | 'Letter')}
                  className="w-4 h-4 text-primary border-gray-300 focus:ring-primary"
                />
                <Label htmlFor="a4" className="font-normal cursor-pointer">
                  A4 (210 × 297 mm)
                </Label>
              </div>
              <div className="flex items-center space-x-2">
                <input
                  type="radio"
                  id="letter"
                  name="pageSize"
                  value="Letter"
                  checked={options.pageSize === 'Letter'}
                  onChange={(e) => updateOption('pageSize', e.target.value as 'A4' | 'Letter')}
                  className="w-4 h-4 text-primary border-gray-300 focus:ring-primary"
                />
                <Label htmlFor="letter" className="font-normal cursor-pointer">
                  Letter (8.5 × 11 in)
                </Label>
              </div>
            </div>
          </div>

          {/* Branding */}
          <div className="space-y-3">
            <Label className="text-base font-semibold">Branding</Label>
            <div className="flex items-center space-x-2">
              <Checkbox
                id="branding"
                checked={options.includeBranding}
                onCheckedChange={(checked) =>
                  updateOption('includeBranding', checked as boolean)
                }
              />
              <label
                htmlFor="branding"
                className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
              >
                Include EaseMyTrip branding
              </label>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={isExporting}>
            Cancel
          </Button>
          <Button onClick={handleExport} disabled={isExporting}>
            {isExporting ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                Generating...
              </>
            ) : (
              <>
                <Download className="w-4 h-4 mr-2" />
                Export PDF
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
