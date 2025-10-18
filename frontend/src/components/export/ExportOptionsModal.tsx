import React, { useState } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '../ui/dialog';
import { Button } from '../ui/button';
import { Label } from '../ui/label';
import { Checkbox } from '../ui/checkbox';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { RadioGroup, RadioGroupItem } from '../ui/radio-group';
import { Download } from 'lucide-react';

interface ExportOptionsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onExport: (options: ExportOptions) => void;
}

interface ExportOptions {
  format: 'pdf' | 'json' | 'csv';
  pageSize: 'A4' | 'Letter' | 'A5';
  includeSections: {
    summary: boolean;
    itinerary: boolean;
    budget: boolean;
    map: boolean;
    notes: boolean;
  };
  orientation: 'portrait' | 'landscape';
}

export const ExportOptionsModal: React.FC<ExportOptionsModalProps> = ({
  isOpen,
  onClose,
  onExport
}) => {
  const [options, setOptions] = useState<ExportOptions>({
    format: 'pdf',
    pageSize: 'A4',
    includeSections: {
      summary: true,
      itinerary: true,
      budget: true,
      map: false,
      notes: true
    },
    orientation: 'portrait'
  });

  const handleSectionToggle = (section: keyof ExportOptions['includeSections']) => {
    setOptions(prev => ({
      ...prev,
      includeSections: {
        ...prev.includeSections,
        [section]: !prev.includeSections[section]
      }
    }));
  };

  const handleExport = () => {
    onExport(options);
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Export Options</DialogTitle>
          <DialogDescription>
            Customize your export settings
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="space-y-2">
            <Label>Format</Label>
            <Select
              value={options.format}
              onValueChange={(value: any) => setOptions(prev => ({ ...prev, format: value }))}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="pdf">PDF Document</SelectItem>
                <SelectItem value="json">JSON Data</SelectItem>
                <SelectItem value="csv">CSV Spreadsheet</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {options.format === 'pdf' && (
            <>
              <div className="space-y-2">
                <Label>Page Size</Label>
                <Select
                  value={options.pageSize}
                  onValueChange={(value: any) => setOptions(prev => ({ ...prev, pageSize: value }))}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="A4">A4</SelectItem>
                    <SelectItem value="Letter">Letter</SelectItem>
                    <SelectItem value="A5">A5</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>Orientation</Label>
                <RadioGroup
                  value={options.orientation}
                  onValueChange={(value: any) => setOptions(prev => ({ ...prev, orientation: value }))}
                >
                  <div className="flex items-center space-x-2">
                    <RadioGroupItem value="portrait" id="portrait" />
                    <Label htmlFor="portrait">Portrait</Label>
                  </div>
                  <div className="flex items-center space-x-2">
                    <RadioGroupItem value="landscape" id="landscape" />
                    <Label htmlFor="landscape">Landscape</Label>
                  </div>
                </RadioGroup>
              </div>
            </>
          )}

          <div className="space-y-2">
            <Label>Include Sections</Label>
            <div className="space-y-2">
              {Object.entries(options.includeSections).map(([key, value]) => (
                <div key={key} className="flex items-center space-x-2">
                  <Checkbox
                    id={key}
                    checked={value}
                    onCheckedChange={() => handleSectionToggle(key as any)}
                  />
                  <Label htmlFor={key} className="capitalize cursor-pointer">
                    {key}
                  </Label>
                </div>
              ))}
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={handleExport}>
            <Download className="h-4 w-4 mr-2" />
            Export
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
