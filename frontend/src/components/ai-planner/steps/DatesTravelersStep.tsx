/**
 * Dates & Travelers Step
 * Step 2 of trip wizard
 */

import { useState } from 'react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Calendar, Users, Minus, Plus, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

interface DatesTravelersStepProps {
  data: any;
  onDataChange: (data: any) => void;
}

export function DatesTravelersStep({ data, onDataChange }: DatesTravelersStepProps) {
  const [startDate, setStartDate] = useState(data.startDate || '');
  const [endDate, setEndDate] = useState(data.endDate || '');
  const [adults, setAdults] = useState(data.adults || 2);
  const [children, setChildren] = useState(data.children || 0);
  const [infants, setInfants] = useState(data.infants || 0);
  const [suggestingDates, setSuggestingDates] = useState(false);

  const handleDataChange = (updates: any) => {
    onDataChange({ ...data, ...updates });
  };

  const handleSuggestBestDates = async () => {
    setSuggestingDates(true);
    // Simulate AI response
    await new Promise(resolve => setTimeout(resolve, 1500));
    
    // Set suggested dates (e.g., 7 days from now for 5 days)
    const today = new Date();
    const suggestedStart = new Date(today);
    suggestedStart.setDate(today.getDate() + 7);
    const suggestedEnd = new Date(suggestedStart);
    suggestedEnd.setDate(suggestedStart.getDate() + 5);
    
    const startDateStr = suggestedStart.toISOString().split('T')[0];
    const endDateStr = suggestedEnd.toISOString().split('T')[0];
    
    setStartDate(startDateStr);
    setEndDate(endDateStr);
    handleDataChange({ startDate: startDateStr, endDate: endDateStr });
    setSuggestingDates(false);
  };

  const updateCount = (type: 'adults' | 'children' | 'infants', delta: number) => {
    const current = { adults, children, infants }[type];
    const min = type === 'adults' ? 1 : 0;
    const max = type === 'adults' ? 9 : type === 'children' ? 8 : 2;
    const newValue = Math.max(min, Math.min(max, current + delta));

    if (type === 'adults') setAdults(newValue);
    if (type === 'children') setChildren(newValue);
    if (type === 'infants') setInfants(newValue);

    handleDataChange({ [type]: newValue });
  };

  return (
    <div className="space-y-6">
      {/* Dates */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <Label htmlFor="startDate">Start Date</Label>
          <div className="relative mt-2">
            <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
            <Input
              id="startDate"
              type="date"
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value);
                handleDataChange({ startDate: e.target.value });
              }}
              className="pl-10 h-12"
            />
          </div>
        </div>

        <div>
          <Label htmlFor="endDate">End Date</Label>
          <div className="relative mt-2">
            <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
            <Input
              id="endDate"
              type="date"
              value={endDate}
              onChange={(e) => {
                setEndDate(e.target.value);
                handleDataChange({ endDate: e.target.value });
              }}
              className="pl-10 h-12"
              min={startDate}
            />
          </div>
        </div>
      </div>

      {/* Travelers */}
      <div>
        <Label className="mb-3 flex items-center gap-2">
          <Users className="w-5 h-5" />
          Travelers
        </Label>

        <div className="space-y-3">
          {/* Adults */}
          <div className="flex items-center justify-between p-3 rounded-lg border border-border">
            <div>
              <div className="font-medium text-sm">Adults</div>
              <div className="text-xs text-muted-foreground">Age 13+</div>
            </div>
            <div className="flex items-center gap-3">
              <Button
                variant="outline"
                size="sm"
                onClick={() => updateCount('adults', -1)}
                disabled={adults <= 1}
                className="w-8 h-8 p-0 rounded-full"
              >
                <Minus className="w-4 h-4" />
              </Button>
              <span className="w-8 text-center font-semibold">{adults}</span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => updateCount('adults', 1)}
                disabled={adults >= 9}
                className="w-8 h-8 p-0 rounded-full"
              >
                <Plus className="w-4 h-4" />
              </Button>
            </div>
          </div>

          {/* Children */}
          <div className="flex items-center justify-between p-3 rounded-lg border border-border">
            <div>
              <div className="font-medium text-sm">Children</div>
              <div className="text-xs text-muted-foreground">Age 2-12</div>
            </div>
            <div className="flex items-center gap-3">
              <Button
                variant="outline"
                size="sm"
                onClick={() => updateCount('children', -1)}
                disabled={children <= 0}
                className="w-8 h-8 p-0 rounded-full"
              >
                <Minus className="w-4 h-4" />
              </Button>
              <span className="w-8 text-center font-semibold">{children}</span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => updateCount('children', 1)}
                disabled={children >= 8}
                className="w-8 h-8 p-0 rounded-full"
              >
                <Plus className="w-4 h-4" />
              </Button>
            </div>
          </div>

          {/* Infants */}
          <div className="flex items-center justify-between p-3 rounded-lg border border-border">
            <div>
              <div className="font-medium text-sm">Infants</div>
              <div className="text-xs text-muted-foreground">Under 2</div>
            </div>
            <div className="flex items-center gap-3">
              <Button
                variant="outline"
                size="sm"
                onClick={() => updateCount('infants', -1)}
                disabled={infants <= 0}
                className="w-8 h-8 p-0 rounded-full"
              >
                <Minus className="w-4 h-4" />
              </Button>
              <span className="w-8 text-center font-semibold">{infants}</span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => updateCount('infants', 1)}
                disabled={infants >= 2}
                className="w-8 h-8 p-0 rounded-full"
              >
                <Plus className="w-4 h-4" />
              </Button>
            </div>
          </div>
        </div>

        {/* Suggest Best Dates Button */}
        <Button
          onClick={handleSuggestBestDates}
          disabled={suggestingDates}
          className="w-full mt-4 h-12 px-6 bg-gradient-to-r from-primary to-primary/80 hover:from-primary/90 hover:to-primary/70 text-white shadow-md hover:shadow-lg transition-all font-medium"
        >
          {suggestingDates ? 'Analyzing best dates...' : 'Suggest Best Travel Date'}
        </Button>
      </div>
    </div>
  );
}
