/**
 * Review Step
 * Step 4 of trip wizard - Final review before submission
 * Enhanced with icon-based cards and better visual design
 */

import { MapPin, Calendar, Users, DollarSign, Gauge, Heart, Sparkles } from 'lucide-react';
import { motion } from 'framer-motion';
import { useTranslation } from '@/i18n';

interface ReviewStepProps {
  data: any;
  onDataChange: (data: any) => void;
}

export function ReviewStep({ data }: ReviewStepProps) {
  const { t } = useTranslation();
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
    if (!dateStr) return t('pages.planner.review.notSelected');
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
    if (!range) return t('pages.planner.review.notSet');
    const [, max] = range;
    if (max <= 1000) return `💰 ${t('pages.planner.preferences.budget.budget')}`;
    if (max <= 2500) return `💵 ${t('pages.planner.preferences.budget.moderate')}`;
    return `💎 ${t('pages.planner.preferences.budget.luxury')}`;
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
            <div className="flex-shrink-0 w-8 h-8 sm:w-14 sm:h-14 rounded-lg sm:rounded-xl bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center shadow-md">
              <MapPin className="w-4 h-4 sm:w-7 sm:h-7 text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[10px] sm:text-xs font-medium text-muted-foreground mb-1">{t('pages.planner.review.destination')}</p>
              <p className="text-xs sm:text-base font-bold text-foreground line-clamp-2 sm:line-clamp-1">
                {data.destination || t('pages.planner.review.notSelected')}
              </p>
              {data.origin && (
                <p className="text-[10px] sm:text-xs text-muted-foreground mt-1 line-clamp-1 hidden sm:block">
                  {t('pages.planner.review.from')}: {data.origin}
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
            <div className="flex-shrink-0 w-8 h-8 sm:w-14 sm:h-14 rounded-lg sm:rounded-xl bg-gradient-to-br from-purple-500 to-purple-600 flex items-center justify-center shadow-md">
              <Calendar className="w-4 h-4 sm:w-7 sm:h-7 text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[10px] sm:text-xs font-medium text-muted-foreground mb-1">{t('pages.planner.review.dates')}</p>
              <p className="text-xs sm:text-base font-bold text-foreground line-clamp-1">
                {formatDate(data.startDate)}
              </p>
              <p className="text-xs sm:text-base font-bold text-foreground line-clamp-1">
                {formatDate(data.endDate)}
              </p>
              {duration && duration > 0 && (
                <p className="text-[10px] sm:text-xs text-primary font-semibold mt-1">
                  {duration} {duration === 1 ? t('pages.planner.review.day') : t('pages.planner.review.days')}
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
            <div className="flex-shrink-0 w-8 h-8 sm:w-14 sm:h-14 rounded-lg sm:rounded-xl bg-gradient-to-br from-green-500 to-green-600 flex items-center justify-center shadow-md">
              <Users className="w-4 h-4 sm:w-7 sm:h-7 text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[10px] sm:text-xs font-medium text-muted-foreground mb-1">{t('pages.planner.review.travelers')}</p>
              <p className="text-xs sm:text-base font-bold text-foreground">
                {totalTravelers} {t(totalTravelers === 1 ? 'pages.planner.review.person' : 'pages.planner.review.people')}
              </p>
              <div className="flex flex-wrap gap-1 sm:gap-2 mt-1 text-[10px] sm:text-xs text-muted-foreground">
                {data.adults > 0 && <span>{data.adults} {t(data.adults > 1 ? 'pages.planner.review.adults' : 'pages.planner.review.adult')}</span>}
                {data.children > 0 && <span>• {data.children} {t(data.children > 1 ? 'pages.planner.review.children' : 'pages.planner.review.child')}</span>}
                {data.infants > 0 && <span>• {data.infants} {t(data.infants > 1 ? 'pages.planner.review.infants' : 'pages.planner.review.infant')}</span>}
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
            <div className="flex-shrink-0 w-8 h-8 sm:w-14 sm:h-14 rounded-lg sm:rounded-xl bg-gradient-to-br from-amber-500 to-amber-600 flex items-center justify-center shadow-md">
              <DollarSign className="w-4 h-4 sm:w-7 sm:h-7 text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[10px] sm:text-xs font-medium text-muted-foreground mb-1">{t('pages.planner.review.budget')}</p>
              <p className="text-xs sm:text-base font-bold text-foreground line-clamp-1">
                {getBudgetLabel(data.budgetRange)}
              </p>
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
            <div className="flex-shrink-0 w-8 h-8 sm:w-14 sm:h-14 rounded-lg sm:rounded-xl bg-gradient-to-br from-orange-500 to-orange-600 flex items-center justify-center shadow-md">
              <Gauge className="w-4 h-4 sm:w-7 sm:h-7 text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[10px] sm:text-xs font-medium text-muted-foreground mb-1">{t('pages.planner.review.pace')}</p>
              <p className="text-xs sm:text-base font-bold text-foreground capitalize line-clamp-2 sm:line-clamp-1">
                {getPaceEmoji(data.pace)} {data.pace ? t(`pages.planner.preferences.pace.${data.pace}`) : t('pages.planner.review.notSelected')}
              </p>
              <p className="text-[10px] sm:text-xs text-muted-foreground mt-1 line-clamp-2 sm:line-clamp-1">
                {data.pace && t(`pages.planner.preferences.pace.${data.pace}Desc`)}
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
            <div className="flex-shrink-0 w-8 h-8 sm:w-14 sm:h-14 rounded-lg sm:rounded-xl bg-gradient-to-br from-pink-500 to-pink-600 flex items-center justify-center shadow-md">
              <Heart className="w-4 h-4 sm:w-7 sm:h-7 text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[10px] sm:text-xs font-medium text-muted-foreground mb-1">{t('pages.planner.review.interests')}</p>
              {data.interests && data.interests.length > 0 ? (
                <div className="flex flex-wrap gap-1 sm:gap-1.5 mt-1">
                  {data.interests.slice(0, 3).map((interest: string) => (
                    <span
                      key={interest}
                      className="px-1.5 sm:px-2 py-0.5 rounded-md bg-pink-100 text-pink-700 text-[10px] sm:text-xs font-medium capitalize line-clamp-1"
                    >
                      {t(`pages.planner.preferences.interestsList.${interest}`)}
                    </span>
                  ))}
                  {data.interests.length > 3 && (
                    <span className="px-1.5 sm:px-2 py-0.5 rounded-md bg-pink-100 text-pink-700 text-[10px] sm:text-xs font-medium">
                      +{data.interests.length - 3}
                    </span>
                  )}
                </div>
              ) : (
                <p className="text-xs sm:text-base font-bold text-muted-foreground">{t('pages.planner.review.none')}</p>
              )}
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  );
}
