/**
 * Premium Dates & Travelers Step
 * Step 2 using DateRangePicker and TravelerSelector premium components
 */

import { useState } from 'react';
import { motion } from 'framer-motion';
import { DateRangePicker } from '@/components/premium/search/DateRangePicker';
import { TravelerSelector, TravelerCounts } from '@/components/premium/search/TravelerSelector';
import { fadeInUp } from '@/lib/animations/variants';

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
    <div className="space-y-5 sm:space-y-6">
      {/* Stack on mobile, side-by-side on desktop */}
      <motion.div
        variants={fadeInUp}
        initial="initial"
        animate="animate"
        className="grid grid-cols-1 lg:grid-cols-[1fr,320px] gap-4 sm:gap-6 items-start"
      >
        {/* Date Range Picker with integrated suggest button */}
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
