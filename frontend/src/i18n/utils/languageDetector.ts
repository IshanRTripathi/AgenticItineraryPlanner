/**
 * Language Detector
 * Detects user's preferred language from browser settings
 */

import type { LanguageCode } from '../types';
import { SUPPORTED_LANGUAGES } from '../types';

/**
 * Get browser language preference
 * Returns the first supported language from browser settings
 */
export function detectBrowserLanguage(): LanguageCode | null {
  // Get browser languages in order of preference
  const browserLanguages = navigator.languages || [navigator.language];

  for (const browserLang of browserLanguages) {
    // Extract language code (e.g., 'en-US' -> 'en')
    const langCode = browserLang.split('-')[0].toLowerCase();

    // Check if it's a supported language
    const supported = SUPPORTED_LANGUAGES.find(lang => lang.code === langCode);
    if (supported) {
      return supported.code;
    }
  }

  return null;
}

/**
 * Get language direction (LTR or RTL)
 */
export function getLanguageDirection(language: LanguageCode): 'ltr' | 'rtl' {
  const langInfo = SUPPORTED_LANGUAGES.find(lang => lang.code === language);
  return langInfo?.direction || 'ltr';
}

/**
 * Get language display name
 */
export function getLanguageName(language: LanguageCode, native: boolean = false): string {
  const langInfo = SUPPORTED_LANGUAGES.find(lang => lang.code === language);
  if (!langInfo) return language;
  return native ? langInfo.nativeName : langInfo.name;
}

/**
 * Check if a language is supported
 */
export function isLanguageSupported(language: string): language is LanguageCode {
  return SUPPORTED_LANGUAGES.some(lang => lang.code === language);
}
