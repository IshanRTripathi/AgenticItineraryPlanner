/**
 * useFormatter Hook
 * Provides locale-aware formatting functions
 */

import { useMemo } from 'react';
import { useTranslation } from './useTranslation';
import { FormatterService } from '../services/FormatterService';
import type { DateFormatStyle, NumberFormatOptions } from '../types';

/**
 * Hook to access formatting functions
 * Automatically updates when language changes
 * 
 * @example
 * ```typescript
 * const { formatDate, formatCurrency, formatRelativeTime } = useFormatter();
 * 
 * <span>{formatDate(trip.startDate)}</span>
 * <span>{formatCurrency(trip.budget, 'INR')}</span>
 * <span>{formatRelativeTime(trip.createdAt)}</span>
 * ```
 */
export function useFormatter() {
  const { language } = useTranslation();

  // Create formatter instance, memoized by language
  const formatter = useMemo(() => {
    return new FormatterService(language);
  }, [language]);

  // Return formatter methods
  return useMemo(() => ({
    formatDate: (date: Date | string, style?: DateFormatStyle) => 
      formatter.formatDate(date, style),
    
    formatTime: (date: Date | string, includeSeconds?: boolean) => 
      formatter.formatTime(date, includeSeconds),
    
    formatDateTime: (date: Date | string, dateStyle?: DateFormatStyle) => 
      formatter.formatDateTime(date, dateStyle),
    
    formatNumber: (num: number, options?: NumberFormatOptions) => 
      formatter.formatNumber(num, options),
    
    formatCurrency: (amount: number, currency?: string) => 
      formatter.formatCurrency(amount, currency),
    
    formatRelativeTime: (date: Date | string) => 
      formatter.formatRelativeTime(date),
    
    // Expose language for conditional logic if needed
    language
  }), [formatter, language]);
}
