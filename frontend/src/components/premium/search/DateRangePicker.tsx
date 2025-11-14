/**
 * DateRangePicker Component
 * Premium date range selector with dual-month calendar and price hints
 */

import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { format, addMonths, isSameDay, startOfDay } from 'date-fns';
import {
  getMonthDays,
  isDateInRange,
  isDateDisabled,
  getDaysBetween,
} from '@/utils/calendar';
import { useTranslation } from '@/i18n';

interface DateRangePickerProps {
  startDate: Date | null;
  endDate: Date | null;
  onChange: (start: Date | null, end: Date | null) => void;
  priceData?: Record<string, number>;
}

interface CalendarDayProps {
  date: Date;
  isSelected: boolean;
  isInRange: boolean;
  isStart: boolean;
  isEnd: boolean;
  isDisabled: boolean;
  onClick: () => void;
  onHover: () => void;
}

function CalendarDay({
  date,
  isSelected,
  isInRange,
  isStart,
  isEnd,
  isDisabled,
  onClick,
  onHover,
}: CalendarDayProps) {

  return (
    <motion.button
      onClick={onClick}
      onMouseEnter={onHover}
      disabled={isDisabled}
      className={`
        relative aspect-square p-1.5 rounded-md text-xs font-semibold transition-all
        ${isDisabled ? 'text-gray-300 cursor-not-allowed bg-gray-50' : 'cursor-pointer'}
        ${isSelected ? 'bg-blue-600 text-white shadow-md hover:bg-blue-700' : ''}
        ${isInRange && !isSelected ? 'bg-blue-100 text-blue-900' : ''}
        ${!isSelected && !isInRange && !isDisabled ? 'hover:bg-gray-100 text-gray-900 bg-white' : ''}
        ${(isStart || isEnd) && !isDisabled ? 'ring-2 ring-blue-600 ring-offset-1' : ''}
      `}
      whileHover={!isDisabled ? { scale: 1.05 } : {}}
      whileTap={!isDisabled ? { scale: 0.95 } : {}}
      transition={{ duration: 0.15 }}
    >
      <div className="relative z-10">{format(date, 'd')}</div>
    </motion.button>
  );
}

interface CalendarMonthProps {
  month: Date;
  startDate: Date | null;
  endDate: Date | null;
  hoveredDate: Date | null;
  onDateClick: (date: Date) => void;
  onDateHover: (date: Date) => void;
  isDateBeyondLimit: (date: Date) => boolean;
}

function CalendarMonth({
  month,
  startDate,
  endDate,
  hoveredDate,
  onDateClick,
  onDateHover,
  isDateBeyondLimit,
}: CalendarMonthProps) {
  const days = getMonthDays(month);
  const weekDays = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'];

  // Calculate first day offset
  const firstDayOfMonth = days[0];
  const firstDayOffset = firstDayOfMonth.getDay();

  return (
    <div>
      {/* Month Header - More Compact */}
      <h3 className="text-base font-semibold text-center mb-3">
        {format(month, 'MMMM yyyy')}
      </h3>

      {/* Day Headers - More Compact */}
      <div className="grid grid-cols-7 gap-0.5 mb-1">
        {weekDays.map((day) => (
          <div key={day} className="text-center text-xs font-semibold text-gray-500 py-1">
            {day}
          </div>
        ))}
      </div>

      {/* Calendar Days - More Compact */}
      <div className="grid grid-cols-7 gap-0.5">
        {/* Empty cells for offset */}
        {Array.from({ length: firstDayOffset }).map((_, i) => (
          <div key={`empty-${i}`} />
        ))}
        
        {/* Actual days */}
        {days.map((date) => {
          const normalizedDate = startOfDay(date);
          const isSelected = !!(
            (startDate && isSameDay(normalizedDate, startDate)) ||
            (endDate && isSameDay(normalizedDate, endDate))
          );
          const isInRange = !!(isDateInRange(normalizedDate, startDate, endDate) ||
            (startDate && hoveredDate && !endDate && isDateInRange(normalizedDate, startDate, hoveredDate)));
          const isStart = !!(startDate && isSameDay(normalizedDate, startDate));
          const isEnd = !!(endDate && isSameDay(normalizedDate, endDate));
          const isBasicallyDisabled = isDateDisabled(normalizedDate);
          const isBeyondLimit = isDateBeyondLimit(normalizedDate);
          const isDisabled = isBasicallyDisabled || isBeyondLimit;

          return (
            <CalendarDay
              key={date.toISOString()}
              date={normalizedDate}
              isSelected={isSelected}
              isInRange={isInRange}
              isStart={isStart}
              isEnd={isEnd}
              isDisabled={isDisabled}
              onClick={() => !isDisabled && onDateClick(normalizedDate)}
              onHover={() => !isDisabled && onDateHover(normalizedDate)}
            />
          );
        })}
      </div>
    </div>
  );
}

export function DateRangePicker({
  startDate,
  endDate,
  onChange,
  priceData = {},
}: DateRangePickerProps) {
  const { t } = useTranslation();
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [hoveredDate, setHoveredDate] = useState<Date | null>(null);
  const [showMaxDaysWarning, setShowMaxDaysWarning] = useState(false);
  const [isSuggesting, setIsSuggesting] = useState(false);

  const handleDateClick = (date: Date) => {
    if (!startDate || (startDate && endDate)) {
      // Start new selection
      onChange(date, null);
      setShowMaxDaysWarning(false);
    } else {
      // Complete selection
      const earlierDate = date < startDate ? date : startDate;
      const laterDate = date < startDate ? startDate : date;
      
      // Check if duration exceeds 7 days
      const daysDiff = getDaysBetween(earlierDate, laterDate);
      if (daysDiff > 7) {
        // Show warning banner
        setShowMaxDaysWarning(true);
        setTimeout(() => setShowMaxDaysWarning(false), 3000);
        return;
      }
      
      setShowMaxDaysWarning(false);
      onChange(earlierDate, laterDate);
    }
  };
  
  // Check if a date should be disabled based on 7-day limit
  const isDateBeyondLimit = (date: Date): boolean => {
    if (!startDate || endDate) return false;
    const daysDiff = Math.abs(getDaysBetween(startDate, date));
    return daysDiff > 7;
  };

  // Calculate duration only when both dates are selected (not on hover)
  const duration = startDate && endDate 
    ? getDaysBetween(startDate, endDate)
    : null;

  // Calculate potential duration on hover
  const potentialDuration = startDate && hoveredDate && !endDate
    ? getDaysBetween(startDate, hoveredDate)
    : null;
  const wouldExceedLimit = potentialDuration !== null && potentialDuration > 7;

  // Suggest best travel dates - randomly select 3-5 days in next 2 months
  const handleSuggestDates = async () => {
    setIsSuggesting(true);
    
    // Simulate AI thinking
    await new Promise(resolve => setTimeout(resolve, 800));
    
    const today = new Date();
    const twoMonthsFromNow = new Date(today);
    twoMonthsFromNow.setMonth(today.getMonth() + 2);
    
    // Random start date between 7 days from now and 2 months from now
    const minDaysFromNow = 7;
    const maxDaysFromNow = 60; // ~2 months
    const randomStartOffset = Math.floor(Math.random() * (maxDaysFromNow - minDaysFromNow)) + minDaysFromNow;
    
    const suggestedStart = new Date(today);
    suggestedStart.setDate(today.getDate() + randomStartOffset);
    
    // Random duration between 3-5 days
    const randomDuration = Math.floor(Math.random() * 3) + 3; // 3, 4, or 5 days
    const suggestedEnd = new Date(suggestedStart);
    suggestedEnd.setDate(suggestedStart.getDate() + randomDuration);
    
    // Navigate calendar to show the suggested start date
    setCurrentMonth(new Date(suggestedStart.getFullYear(), suggestedStart.getMonth(), 1));
    
    onChange(suggestedStart, suggestedEnd);
    setIsSuggesting(false);
  };

  return (
    <motion.div
      className="bg-white rounded-xl shadow-lg border border-gray-200 p-4 w-full"
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.25 }}
    >
      {/* Warning Banner */}
      <AnimatePresence>
        {showMaxDaysWarning && (
          <motion.div
            initial={{ opacity: 0, height: 0, marginBottom: 0 }}
            animate={{ opacity: 1, height: 'auto', marginBottom: 16 }}
            exit={{ opacity: 0, height: 0, marginBottom: 0 }}
            className="bg-amber-50 border border-amber-200 rounded-lg p-3 flex items-start gap-2"
          >
            <div className="flex-shrink-0 w-5 h-5 rounded-full bg-amber-500 text-white flex items-center justify-center text-xs font-bold mt-0.5">
              !
            </div>
            <div className="flex-1">
              <p className="text-sm font-medium text-amber-900">{t('components.dateRangePicker.maxDaysTitle')}</p>
              <p className="text-xs text-amber-700 mt-0.5">
                {t('components.dateRangePicker.maxDaysDescription')}
              </p>
            </div>
          </motion.div>
        )}
      </AnimatePresence>



      {/* Dual Calendar Grid - More Compact */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Current Month */}
        <div>
          <div className="flex items-center justify-between mb-3">
            <button
              onClick={() => setCurrentMonth(addMonths(currentMonth, -1))}
              className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <div className="flex-1" />
          </div>
          <CalendarMonth
            month={currentMonth}
            startDate={startDate}
            endDate={endDate}
            hoveredDate={hoveredDate}
            onDateClick={handleDateClick}
            onDateHover={setHoveredDate}
            isDateBeyondLimit={isDateBeyondLimit}
          />
        </div>

        {/* Next Month */}
        <div>
          <div className="flex items-center justify-between mb-3">
            <div className="flex-1" />
            <button
              onClick={() => setCurrentMonth(addMonths(currentMonth, 1))}
              className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
          <CalendarMonth
            month={addMonths(currentMonth, 1)}
            startDate={startDate}
            endDate={endDate}
            hoveredDate={hoveredDate}
            onDateClick={handleDateClick}
            onDateHover={setHoveredDate}
            isDateBeyondLimit={isDateBeyondLimit}
          />
        </div>
      </div>

      {/* Duration Display + Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mt-4 pt-4 border-t border-gray-200">
        {/* Duration Display */}
        <div className="text-xs text-gray-600 font-medium flex-1 min-w-0">
          {startDate && endDate && duration !== null && duration >= 0 ? (
            <motion.span
              key={`${startDate.toISOString()}-${endDate.toISOString()}`}
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.2 }}
              className="block truncate"
            >
              {t('components.dateRangePicker.duration', { nights: duration, days: duration + 1 })}
            </motion.span>
          ) : (
            <span className="text-gray-400">{t('components.dateRangePicker.selectDates')}</span>
          )}
        </div>
        
        {/* Action Buttons */}
        <div className="flex items-center gap-2 flex-shrink-0">
          <motion.button
            onClick={handleSuggestDates}
            disabled={isSuggesting}
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            className="px-2.5 sm:px-3 py-1.5 text-xs font-medium bg-gradient-to-r from-primary to-primary/80 hover:from-primary/90 hover:to-primary/70 text-white rounded-md shadow-sm hover:shadow transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1.5 whitespace-nowrap touch-manipulation active:scale-95"
          >
            {isSuggesting ? (
              <>
                <motion.div
                  animate={{ rotate: 360 }}
                  transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
                  className="w-3 h-3 border-2 border-white border-t-transparent rounded-full flex-shrink-0"
                />
                <span className="hidden sm:inline">{t('components.dateRangePicker.suggesting')}</span>
                <span className="sm:hidden">...</span>
              </>
            ) : (
              <>
                <svg className="w-3 h-3 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                </svg>
                <span className="hidden sm:inline">{t('components.dateRangePicker.suggestDates')}</span>
                <span className="sm:hidden">Suggest</span>
              </>
            )}
          </motion.button>
          
          <button
            onClick={() => onChange(null, null)}
            disabled={!startDate && !endDate}
            className="text-xs font-medium text-gray-600 hover:text-gray-900 transition-colors disabled:opacity-40 disabled:cursor-not-allowed px-2 py-1.5 whitespace-nowrap touch-manipulation active:scale-95"
          >
            {t('common.actions.clear')}
          </button>
        </div>
      </div>
    </motion.div>
  );
}
