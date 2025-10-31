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
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [hoveredDate, setHoveredDate] = useState<Date | null>(null);
  const [showMaxDaysWarning, setShowMaxDaysWarning] = useState(false);

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
              <p className="text-sm font-medium text-amber-900">Maximum 7 days</p>
              <p className="text-xs text-amber-700 mt-0.5">
                Please select a trip duration of 7 days or less
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

      {/* Duration Display + Clear Button */}
      <div className="flex items-center justify-between mt-4 pt-4 border-t border-gray-200">
        <div className="text-xs text-gray-600 font-medium">
          {startDate && endDate && duration !== null && duration >= 0 ? (
            <motion.span
              key={`${startDate.toISOString()}-${endDate.toISOString()}`}
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.2 }}
            >
              {duration} night{duration !== 1 ? 's' : ''}, {duration + 1} day{duration + 1 !== 1 ? 's' : ''}
            </motion.span>
          ) : (
            <span className="text-gray-400">Select dates</span>
          )}
        </div>
        
        <button
          onClick={() => onChange(null, null)}
          disabled={!startDate && !endDate}
          className="text-xs font-medium text-gray-600 hover:text-gray-900 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
        >
          Clear Dates
        </button>
      </div>
    </motion.div>
  );
}
