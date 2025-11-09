/**
 * Premium Destination Step
 * Step 1 using LocationAutocomplete premium component
 */

import { useState } from 'react';
import { motion } from 'framer-motion';
import { LocationInput } from '@/components/ai-planner/LocationInput';
import { fadeInUp } from '@/lib/animations/variants';
import { TrendingUp } from 'lucide-react';
import { useTranslation } from '@/i18n';

interface PremiumDestinationStepProps {
  data: any;
  onDataChange: (data: any) => void;
}

const POPULAR_DESTINATIONS = [
  { name: 'Paris, France', emoji: '🗼' },
  { name: 'Tokyo, Japan', emoji: '🗾' },
  { name: 'Bali, Indonesia', emoji: '🏝️' },
  { name: 'Dubai, UAE', emoji: '🏙️' },
  { name: 'New York, USA', emoji: '🗽' },
  { name: 'London, UK', emoji: '🎡' },
  { name: 'Barcelona, Spain', emoji: '🏖️' },
  { name: 'Maldives', emoji: '🌴' },
];

export function PremiumDestinationStep({ data, onDataChange }: PremiumDestinationStepProps) {
  const { t } = useTranslation();
  const [origin, setOrigin] = useState(data.origin || '');
  const [destination, setDestination] = useState(data.destination || '');

  const handleOriginChange = (value: string) => {
    setOrigin(value);
    onDataChange({ origin: value });
  };

  const handleDestinationChange = (value: string) => {
    setDestination(value);
    onDataChange({ destination: value });
  };

  return (
    <div className="space-y-5 sm:space-y-6">
      {/* Origin and Destination - Stack on mobile */}
      <motion.div
        variants={fadeInUp}
        initial="initial"
        animate="animate"
        className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4 max-w-4xl mx-auto"
      >
        <div>
          <label className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-2 block">
            {t('pages.planner.destination.fromLabel')}
          </label>
          <LocationInput
            value={origin}
            onChange={handleOriginChange}
            placeholder={t('pages.planner.destination.fromPlaceholder')}
          />
        </div>
        <div>
          <label className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-2 block">
            {t('pages.planner.destination.toLabel')}
          </label>
          <LocationInput
            value={destination}
            onChange={handleDestinationChange}
            placeholder={t('pages.planner.destination.toPlaceholder')}
          />
        </div>
      </motion.div>

      {/* Popular Destinations - 2 cols on mobile, 4 on desktop */}
      <motion.div
        variants={fadeInUp}
        initial="initial"
        animate="animate"
        transition={{ delay: 0.1 }}
      >
        <div className="flex items-center gap-2 mb-3">
          <TrendingUp className="w-4 h-4 text-muted-foreground" />
          <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
            {t('pages.planner.destination.popularDestinations')}
          </span>
        </div>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
          {POPULAR_DESTINATIONS.slice(0, 4).map((dest) => (
            <motion.button
              key={dest.name}
              onClick={() => handleDestinationChange(dest.name)}
              className="p-2.5 sm:p-3 rounded-lg border-2 border-border hover:border-primary hover:bg-primary/5 transition-all text-left group touch-manipulation active:scale-95"
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              <div className="text-xl sm:text-2xl mb-1">{dest.emoji}</div>
              <div className="text-[10px] sm:text-xs font-medium text-foreground group-hover:text-primary transition-colors line-clamp-1">
                {dest.name}
              </div>
            </motion.button>
          ))}
        </div>
      </motion.div>
    </div>
  );
}
