/**
 * Premium Dates & Travelers Step
 * Step 2 using DateRangePicker and TravelerSelector premium components
 */

import { useState } from 'react';
import { motion } from 'framer-motion';
import { DateRangePicker } from '@/components/premium/search/DateRangePicker';
import { TravelerSelector, TravelerCounts } from '@/components/premium/search/TravelerSelector';
import { fadeInUp } from '@/lib/animations/variants';
import { Calendar, Users } from 'lucide-react';

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

  // Mock price data for calendar
  const mockPriceData: Record<string, number> = {};
  const today = new Date();
  for (let i = 0; i < 90; i++) {
    const date = new Date(today);
    date.setDate(today.getDate() + i);
    const dateKey = date.toISOString().split('T')[0];
    // Generate random prices with some pattern
    const basePrice = 100 + Math.random() * 200;
    const weekendMultiplier = date.getDay() === 0 || date.getDay() === 6 ? 1.3 : 1;
    mockPriceData[dateKey] = Math.round(basePrice * weekendMultiplier);
  }

  return (
    <div className="space-y-6">
      {/* Compact Header */}
      <motion.div
        className="text-center"
        variants={fadeInUp}
        initial="initial"
        animate="animate"
      >
        <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-primary/10 mb-3">
          <Calendar className="w-6 h-6 text-primary" />
        </div>
        <h2 className="text-2xl font-bold text-foreground mb-1">
          When are you traveling?
        </h2>
        <p className="text-sm text-muted-foreground">
          Select your travel dates and number of travelers
        </p>
      </motion.div>

      {/* Side-by-side Layout: Date Picker (left) + Travelers (right) */}
      <motion.div
        variants={fadeInUp}
        initial="initial"
        animate="animate"
        transition={{ delay: 0.1 }}
        className="grid grid-cols-1 lg:grid-cols-[1fr,320px] gap-6 items-start"
      >
        {/* Date Range Picker */}
        <div>
          <DateRangePicker
            startDate={startDate}
            endDate={endDate}
            onChange={handleDateChange}
            priceData={mockPriceData}
          />
        </div>

        {/* Traveler Selector - Compact */}
        <div className="space-y-4">
          <div className="flex items-center gap-2">
            <Users className="w-5 h-5 text-muted-foreground" />
            <span className="text-sm font-medium text-muted-foreground uppercase tracking-wide">
              Travelers
            </span>
          </div>
          <TravelerSelector value={travelers} onChange={handleTravelersChange} />
          
          {/* Summary */}
          {startDate && endDate && (
            <motion.div
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className="p-4 bg-primary/5 rounded-xl border border-primary/20"
            >
              <div className="text-xs text-muted-foreground mb-1">Your trip</div>
              <div className="font-semibold text-sm text-foreground">
                {Math.ceil((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24))} nights
                {' • '}
                {travelers.adults + travelers.children + travelers.infants} traveler
                {travelers.adults + travelers.children + travelers.infants > 1 ? 's' : ''}
              </div>
            </motion.div>
          )}
        </div>
      </motion.div>
    </div>
  );
}
