/**
 * Review Step
 * Step 4 of trip wizard - Final review before submission
 * Enhanced with icon-based cards and better visual design
 */

import { MapPin, Calendar, Users, DollarSign, Gauge, Heart, Sparkles } from 'lucide-react';
import { motion } from 'framer-motion';

interface ReviewStepProps {
  data: any;
  onDataChange: (data: any) => void;
}

export function ReviewStep({ data }: ReviewStepProps) {
  const totalTravelers = (data.adults || 0) + (data.children || 0) + (data.infants || 0);
  
  // Calculate trip duration
  const getDuration = () => {
    if (!data.startDate || !data.endDate) return null;
    const start = new Date(data.startDate);
    const end = new Date(data.endDate);
    const days = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;
    return days;
  };

  const duration = getDuration();

  // Format dates
  const formatDate = (dateStr: string) => {
    if (!dateStr) return 'Not selected';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  };

  // Get pace emoji
  const getPaceEmoji = (pace: string) => {
    switch (pace) {
      case 'relaxed': return '🧘';
      case 'moderate': return '🚶';
      case 'fast': return '🏃';
      default: return '🚶';
    }
  };

  // Get budget label
  const getBudgetLabel = (range: [number, number]) => {
    if (!range) return 'Not set';
    const [, max] = range;
    if (max <= 1000) return '💰 Budget';
    if (max <= 2500) return '💵 Moderate';
    return '💎 Luxury';
  };

  return (
    <div className="space-y-5 sm:space-y-6">
      

      {/* Review Cards Grid - 2 columns on all screens */}
      <div className="grid grid-cols-2 gap-2.5 sm:gap-4">
        {/* Destination Card */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="group relative overflow-hidden rounded-xl sm:rounded-2xl border-2 border-border hover:border-primary/50 bg-gradient-to-br from-blue-50 to-white p-3 sm:p-5 transition-all hover:shadow-lg"
        >
          <div className="flex flex-col sm:flex-row items-start gap-2 sm:gap-4">
            <div className="flex-shrink-0 w-10 h-10 sm:w-14 sm:h-14 rounded-lg sm:rounded-xl bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center shadow-md">
              <MapPin className="w-5 h-5 sm:w-7 sm:h-7 text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[10px] sm:text-xs font-medium text-muted-foreground mb-0.5 sm:mb-1">Destination</p>
              <p className="text-xs sm:text-base font-bold text-foreground line-clamp-1">
                {data.destination || 'Not selected'}
              </p>
              {data.origin && (
                <p className="text-[9px] sm:text-xs text-muted-foreground mt-0.5 sm:mt-1 line-clamp-1 hidden sm:block">
                  From: {data.origin}
                </p>
              )}
            </div>
          </div>
        </motion.div>

        {/* Dates Card */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15 }}
          className="group relative overflow-hidden rounded-xl sm:rounded-2xl border-2 border-border hover:border-primary/50 bg-gradient-to-br from-purple-50 to-white p-3 sm:p-5 transition-all hover:shadow-lg"
        >
          <div className="flex flex-col sm:flex-row items-start gap-2 sm:gap-4">
            <div className="flex-shrink-0 w-10 h-10 sm:w-14 sm:h-14 rounded-lg sm:rounded-xl bg-gradient-to-br from-purple-500 to-purple-600 flex items-center justify-center shadow-md">
              <Calendar className="w-5 h-5 sm:w-7 sm:h-7 text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[10px] sm:text-xs font-medium text-muted-foreground mb-0.5 sm:mb-1">Dates</p>
              <p className="text-[10px] sm:text-sm font-semibold text-foreground line-clamp-1">
                {formatDate(data.startDate)}
              </p>
              <p className="text-[10px] sm:text-sm font-semibold text-foreground line-clamp-1">
                {formatDate(data.endDate)}
              </p>
              {duration && (
                <p className="text-[9px] sm:text-xs text-primary font-medium mt-0.5 sm:mt-1">
                  {duration} {duration === 1 ? 'day' : 'days'}
                </p>
              )}
            </div>
          </div>
        </motion.div>

        {/* Travelers Card */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="group relative overflow-hidden rounded-xl sm:rounded-2xl border-2 border-border hover:border-primary/50 bg-gradient-to-br from-green-50 to-white p-3 sm:p-5 transition-all hover:shadow-lg"
        >
          <div className="flex flex-col sm:flex-row items-start gap-2 sm:gap-4">
            <div className="flex-shrink-0 w-10 h-10 sm:w-14 sm:h-14 rounded-lg sm:rounded-xl bg-gradient-to-br from-green-500 to-green-600 flex items-center justify-center shadow-md">
              <Users className="w-5 h-5 sm:w-7 sm:h-7 text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[10px] sm:text-xs font-medium text-muted-foreground mb-0.5 sm:mb-1">Travelers</p>
              <p className="text-sm sm:text-xl font-bold text-foreground">
                {totalTravelers} {totalTravelers === 1 ? 'Person' : 'People'}
              </p>
              <div className="flex flex-wrap gap-1 sm:gap-2 mt-0.5 sm:mt-1 text-[9px] sm:text-xs text-muted-foreground">
                {data.adults > 0 && <span>{data.adults} Adult{data.adults > 1 ? 's' : ''}</span>}
                {data.children > 0 && <span>• {data.children} Child{data.children > 1 ? 'ren' : ''}</span>}
                {data.infants > 0 && <span>• {data.infants} Infant{data.infants > 1 ? 's' : ''}</span>}
              </div>
            </div>
          </div>
        </motion.div>

        {/* Budget Card */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.25 }}
          className="group relative overflow-hidden rounded-xl sm:rounded-2xl border-2 border-border hover:border-primary/50 bg-gradient-to-br from-amber-50 to-white p-3 sm:p-5 transition-all hover:shadow-lg"
        >
          <div className="flex flex-col sm:flex-row items-start gap-2 sm:gap-4">
            <div className="flex-shrink-0 w-10 h-10 sm:w-14 sm:h-14 rounded-lg sm:rounded-xl bg-gradient-to-br from-amber-500 to-amber-600 flex items-center justify-center shadow-md">
              <DollarSign className="w-5 h-5 sm:w-7 sm:h-7 text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[10px] sm:text-xs font-medium text-muted-foreground mb-0.5 sm:mb-1">Budget</p>
              <p className="text-xs sm:text-base font-bold text-foreground line-clamp-1">
                {getBudgetLabel(data.budgetRange)}
              </p>
              {data.budgetRange && (
                <p className="text-[9px] sm:text-xs text-muted-foreground mt-0.5 sm:mt-1">
                  ${data.budgetRange[0]} - ${data.budgetRange[1]}
                </p>
              )}
            </div>
          </div>
        </motion.div>

        {/* Pace Card */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="group relative overflow-hidden rounded-xl sm:rounded-2xl border-2 border-border hover:border-primary/50 bg-gradient-to-br from-orange-50 to-white p-3 sm:p-5 transition-all hover:shadow-lg"
        >
          <div className="flex flex-col sm:flex-row items-start gap-2 sm:gap-4">
            <div className="flex-shrink-0 w-10 h-10 sm:w-14 sm:h-14 rounded-lg sm:rounded-xl bg-gradient-to-br from-orange-500 to-orange-600 flex items-center justify-center shadow-md">
              <Gauge className="w-5 h-5 sm:w-7 sm:h-7 text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[10px] sm:text-xs font-medium text-muted-foreground mb-0.5 sm:mb-1">Pace</p>
              <p className="text-xs sm:text-base font-bold text-foreground capitalize line-clamp-1">
                {getPaceEmoji(data.pace)} {data.pace || 'Not selected'}
              </p>
              <p className="text-[9px] sm:text-xs text-muted-foreground mt-0.5 sm:mt-1 line-clamp-1">
                {data.pace === 'relaxed' && '2-3 activities'}
                {data.pace === 'moderate' && '4-5 activities'}
                {data.pace === 'fast' && '6+ activities'}
              </p>
            </div>
          </div>
        </motion.div>

        {/* Interests Card */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.35 }}
          className="group relative overflow-hidden rounded-xl sm:rounded-2xl border-2 border-border hover:border-primary/50 bg-gradient-to-br from-pink-50 to-white p-3 sm:p-5 transition-all hover:shadow-lg"
        >
          <div className="flex flex-col sm:flex-row items-start gap-2 sm:gap-4">
            <div className="flex-shrink-0 w-10 h-10 sm:w-14 sm:h-14 rounded-lg sm:rounded-xl bg-gradient-to-br from-pink-500 to-pink-600 flex items-center justify-center shadow-md">
              <Heart className="w-5 h-5 sm:w-7 sm:h-7 text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[10px] sm:text-xs font-medium text-muted-foreground mb-0.5 sm:mb-1">Interests</p>
              {data.interests && data.interests.length > 0 ? (
                <div className="flex flex-wrap gap-1 sm:gap-1.5 mt-0.5 sm:mt-1">
                  {data.interests.slice(0, 3).map((interest: string) => (
                    <span
                      key={interest}
                      className="px-1.5 sm:px-2 py-0.5 rounded-md bg-pink-100 text-pink-700 text-[9px] sm:text-xs font-medium capitalize line-clamp-1"
                    >
                      {interest}
                    </span>
                  ))}
                  {data.interests.length > 3 && (
                    <span className="px-1.5 sm:px-2 py-0.5 rounded-md bg-pink-100 text-pink-700 text-[9px] sm:text-xs font-medium">
                      +{data.interests.length - 3}
                    </span>
                  )}
                </div>
              ) : (
                <p className="text-[10px] sm:text-sm text-muted-foreground">None</p>
              )}
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  );
}
