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
          Select your travel dates (maximum 7 days)
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
          />
        </div>

        {/* Traveler Selector */}
        <div>
          <TravelerSelector value={travelers} onChange={handleTravelersChange} />
        </div>
      </motion.div>
    </div>
  );
}
