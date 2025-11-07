/**
 * Premium Dates & Travelers Step
 * Step 2 using DateRangePicker and TravelerSelector premium components
 */

import { useState } from 'react';
import { motion } from 'framer-motion';
import { DateRangePicker } from '@/components/premium/search/DateRangePicker';
import { TravelerSelector, TravelerCounts } from '@/components/premium/search/TravelerSelector';
import { fadeInUp } from '@/lib/animations/variants';
import { Calendar, Users, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

interface PremiumDatesTravelersStepProps {
  data: any;
  onDataChange: (data: any) => void;
}

export function PremiumDatesTravelersStep({ data, onDataChange }: PremiumDatesTravelersStepProps) {
  const [startDate, setStartDate] = useState<Date | null>(
    data.startDate ? new Date(data.startDate) : null
  );
  const [endDate, setEndDate] = useState<Date | null>(
    data.endDate ? new Date(data.endDate) : null
  );
  const [travelers, setTravelers] = useState<TravelerCounts>({
    adults: data.adults || 2,
    children: data.children || 0,
    infants: data.infants || 0,
  });
  const [suggestingDates, setSuggestingDates] = useState(false);

  const handleDateChange = (start: Date | null, end: Date | null) => {
    setStartDate(start);
    setEndDate(end);
    onDataChange({
      startDate: start?.toISOString().split('T')[0],
      endDate: end?.toISOString().split('T')[0],
    });
  };

  const handleTravelersChange = (counts: TravelerCounts) => {
    setTravelers(counts);
    onDataChange({
      adults: counts.adults,
      children: counts.children,
      infants: counts.infants,
    });
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
    
    setStartDate(suggestedStart);
    setEndDate(suggestedEnd);
    onDataChange({
      startDate: suggestedStart.toISOString().split('T')[0],
      endDate: suggestedEnd.toISOString().split('T')[0],
    });
    setSuggestingDates(false);
  };

  return (
    <div className="space-y-5 sm:space-y-6">
      {/* Stack on mobile, side-by-side on desktop */}
      <motion.div
        variants={fadeInUp}
        initial="initial"
        animate="animate"
        className="grid grid-cols-1 lg:grid-cols-[1fr,320px] gap-4 sm:gap-6 items-start"
      >
        {/* Date Range Picker */}
        <div>
          <DateRangePicker
            startDate={startDate}
            endDate={endDate}
            onChange={handleDateChange}
          />
        </div>

        {/* Traveler Selector */}
        <div className="space-y-3 sm:space-y-4">
          <TravelerSelector value={travelers} onChange={handleTravelersChange} />
          
          {/* Suggest Best Dates Button */}
          <Button
            onClick={handleSuggestBestDates}
            disabled={suggestingDates}
            className="w-full h-11 sm:h-12 px-4 sm:px-6 bg-gradient-to-r from-primary to-primary/80 hover:from-primary/90 hover:to-primary/70 text-white shadow-md hover:shadow-lg transition-all font-medium text-sm sm:text-base touch-manipulation active:scale-95"
          >
            {suggestingDates ? 'Analyzing best dates...' : 'Suggest Best Travel Date'}
          </Button>
        </div>
      </motion.div>
    </div>
  );
}
