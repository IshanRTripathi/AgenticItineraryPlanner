/**
 * Calendar Utility Functions
 * Helper functions for date range picker component
 */

import { 
  eachDayOfInterval, 
  startOfMonth, 
  endOfMonth, 
  isWithinInterval,
  isBefore,
  isAfter,
  differenceInDays,
  addDays,
  format
} from 'date-fns';

export function getMonthDays(date: Date): Date[] {
  const start = startOfMonth(date);
  const end = endOfMonth(date);
  return eachDayOfInterval({ start, end });
}

export function isDateInRange(
  date: Date,
  startDate: Date | null,
  endDate: Date | null
): boolean {
  if (!startDate || !endDate) return false;
  return isWithinInterval(date, { start: startDate, end: endDate });
}

export function isDateDisabled(date: Date): boolean {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return isBefore(date, today);
}

export function getDaysBetween(startDate: Date, endDate: Date): number {
  return differenceInDays(endDate, startDate);
}

export function getDateRangePreset(preset: string): { start: Date; end: Date } | null {
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  switch (preset) {
    case 'weekend':
      // Next weekend (Saturday-Sunday)
      const daysUntilSaturday = (6 - today.getDay() + 7) % 7 || 7;
      const saturday = addDays(today, daysUntilSaturday);
      const sunday = addDays(saturday, 1);
      return { start: saturday, end: sunday };
    
    case 'week':
      return { start: today, end: addDays(today, 7) };
    
    case 'month':
      return { start: today, end: addDays(today, 30) };
    
    default:
      return null;
  }
}

export function formatDateRange(startDate: Date | null, endDate: Date | null): string {
  if (!startDate) return '';
  if (!endDate) return format(startDate, 'MMM d');
  
  const nights = getDaysBetween(startDate, endDate);
  const days = nights + 1;
  
  return `${format(startDate, 'MMM d')} - ${format(endDate, 'MMM d')} (${nights} night${nights !== 1 ? 's' : ''}, ${days} day${days !== 1 ? 's' : ''})`;
}
