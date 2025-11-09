/**
 * Formatter Service
 * Provides locale-aware formatting for dates, numbers, and currency
 */

import type { LanguageCode, DateFormatStyle, NumberFormatOptions } from '../types';

export class FormatterService {
  private language: LanguageCode;
  private locale: string;

  constructor(language: LanguageCode) {
    this.language = language;
    this.locale = this.getLocaleString(language);
  }

  /**
   * Get locale string for Intl APIs
   */
  private getLocaleString(language: LanguageCode): string {
    const localeMap: Record<LanguageCode, string> = {
      en: 'en-US',
      hi: 'hi-IN',
      es: 'es-ES',
      kn: 'kn-IN',
      te: 'te-IN',
      ml: 'ml-IN',
      pa: 'pa-IN',
      bn: 'bn-IN'
    };
    return localeMap[language] || 'en-US';
  }

  /**
   * Update language
   */
  setLanguage(language: LanguageCode): void {
    this.language = language;
    this.locale = this.getLocaleString(language);
  }

  /**
   * Format date
   */
  formatDate(date: Date | string, style: DateFormatStyle = 'medium'): string {
    try {
      const dateObj = typeof date === 'string' ? new Date(date) : date;
      
      if (isNaN(dateObj.getTime())) {
        console.warn('[FormatterService] Invalid date:', date);
        return String(date);
      }

      const options: Intl.DateTimeFormatOptions = this.getDateFormatOptions(style);
      return new Intl.DateTimeFormat(this.locale, options).format(dateObj);
    } catch (error) {
      console.error('[FormatterService] Error formatting date:', error);
      return String(date);
    }
  }

  /**
   * Format time
   */
  formatTime(date: Date | string, includeSeconds: boolean = false): string {
    try {
      const dateObj = typeof date === 'string' ? new Date(date) : date;
      
      if (isNaN(dateObj.getTime())) {
        console.warn('[FormatterService] Invalid date:', date);
        return String(date);
      }

      const options: Intl.DateTimeFormatOptions = {
        hour: '2-digit',
        minute: '2-digit',
        ...(includeSeconds && { second: '2-digit' })
      };

      return new Intl.DateTimeFormat(this.locale, options).format(dateObj);
    } catch (error) {
      console.error('[FormatterService] Error formatting time:', error);
      return String(date);
    }
  }

  /**
   * Format date and time
   */
  formatDateTime(date: Date | string, dateStyle: DateFormatStyle = 'medium'): string {
    try {
      const dateObj = typeof date === 'string' ? new Date(date) : date;
      
      if (isNaN(dateObj.getTime())) {
        console.warn('[FormatterService] Invalid date:', date);
        return String(date);
      }

      const options: Intl.DateTimeFormatOptions = {
        ...this.getDateFormatOptions(dateStyle),
        hour: '2-digit',
        minute: '2-digit'
      };

      return new Intl.DateTimeFormat(this.locale, options).format(dateObj);
    } catch (error) {
      console.error('[FormatterService] Error formatting datetime:', error);
      return String(date);
    }
  }

  /**
   * Format number
   */
  formatNumber(num: number, options?: NumberFormatOptions): string {
    try {
      return new Intl.NumberFormat(this.locale, options).format(num);
    } catch (error) {
      console.error('[FormatterService] Error formatting number:', error);
      return String(num);
    }
  }

  /**
   * Format currency
   */
  formatCurrency(amount: number, currency: string = 'INR'): string {
    try {
      return new Intl.NumberFormat(this.locale, {
        style: 'currency',
        currency: currency,
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
      }).format(amount);
    } catch (error) {
      console.error('[FormatterService] Error formatting currency:', error);
      return `${currency} ${amount}`;
    }
  }

  /**
   * Format relative time (e.g., "2 hours ago", "in 3 days")
   */
  formatRelativeTime(date: Date | string): string {
    try {
      const dateObj = typeof date === 'string' ? new Date(date) : date;
      
      if (isNaN(dateObj.getTime())) {
        console.warn('[FormatterService] Invalid date:', date);
        return String(date);
      }

      const now = new Date();
      const diffMs = dateObj.getTime() - now.getTime();
      const diffSec = Math.round(diffMs / 1000);
      const diffMin = Math.round(diffSec / 60);
      const diffHour = Math.round(diffMin / 60);
      const diffDay = Math.round(diffHour / 24);

      // Use Intl.RelativeTimeFormat if available
      if (typeof Intl.RelativeTimeFormat !== 'undefined') {
        const rtf = new Intl.RelativeTimeFormat(this.locale, { numeric: 'auto' });

        if (Math.abs(diffDay) >= 1) {
          return rtf.format(diffDay, 'day');
        } else if (Math.abs(diffHour) >= 1) {
          return rtf.format(diffHour, 'hour');
        } else if (Math.abs(diffMin) >= 1) {
          return rtf.format(diffMin, 'minute');
        } else {
          return rtf.format(diffSec, 'second');
        }
      }

      // Fallback for browsers without RelativeTimeFormat
      const abs = Math.abs;
      if (abs(diffDay) >= 1) {
        return `${abs(diffDay)} day${abs(diffDay) > 1 ? 's' : ''} ${diffDay > 0 ? 'from now' : 'ago'}`;
      } else if (abs(diffHour) >= 1) {
        return `${abs(diffHour)} hour${abs(diffHour) > 1 ? 's' : ''} ${diffHour > 0 ? 'from now' : 'ago'}`;
      } else if (abs(diffMin) >= 1) {
        return `${abs(diffMin)} minute${abs(diffMin) > 1 ? 's' : ''} ${diffMin > 0 ? 'from now' : 'ago'}`;
      } else {
        return 'just now';
      }
    } catch (error) {
      console.error('[FormatterService] Error formatting relative time:', error);
      return String(date);
    }
  }

  /**
   * Get date format options based on style
   */
  private getDateFormatOptions(style: DateFormatStyle): Intl.DateTimeFormatOptions {
    const styleMap: Record<DateFormatStyle, Intl.DateTimeFormatOptions> = {
      full: {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      },
      long: {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      },
      medium: {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      },
      short: {
        year: '2-digit',
        month: '2-digit',
        day: '2-digit'
      }
    };

    return styleMap[style] || styleMap.medium;
  }
}
