/**
 * TypeScript types for i18n translation system
 * Provides type safety for translation keys, parameters, and configuration
 */

// Supported language codes
export type LanguageCode = 'en' | 'hi' | 'es' | 'kn' | 'te' | 'ml' | 'pa' | 'bn';

// Translation namespaces
export type Namespace = 
  | 'common' 
  | 'pages' 
  | 'components' 
  | 'errors' 
  | 'validation';

// Translation key type (will be extended with actual keys)
export type TranslationKey = string;

// Parameters for interpolation in translations
export interface TranslationParams {
  [key: string]: string | number | boolean | Date;
}

// Options for translation function
export interface TranslateOptions {
  defaultValue?: string;
  count?: number;
  context?: string;
}

// Options for dynamic translation (Google Translate)
export interface DynamicTranslateOptions {
  from?: LanguageCode;
  to: LanguageCode;
  cache?: boolean;
  format?: 'text' | 'html';
  skipIfProperNoun?: boolean;
}

// Translation function type
export type TranslateFunction = (
  key: TranslationKey,
  params?: TranslationParams,
  options?: TranslateOptions
) => string;

// Dynamic translation function type (async)
export type DynamicTranslateFunction = (
  text: string,
  options?: DynamicTranslateOptions
) => Promise<string>;

// Language information
export interface LanguageInfo {
  code: LanguageCode;
  name: string;
  nativeName: string;
  flag: string;
  direction: 'ltr' | 'rtl';
}

// Translation context value
export interface TranslationContextValue {
  language: LanguageCode;
  setLanguage: (lang: LanguageCode) => void;
  isLoading: boolean;
  translations: Record<string, any>;
  t: TranslateFunction;
  td: DynamicTranslateFunction;
}

// Translation provider props
export interface TranslationProviderProps {
  children: React.ReactNode;
  defaultLanguage?: LanguageCode;
  supportedLanguages?: LanguageCode[];
}

// Translation file structure
export interface TranslationFile {
  [key: string]: string | TranslationFile;
}

// Cache entry for translations
export interface CacheEntry {
  value: string;
  timestamp: number;
  language: LanguageCode;
}

// Translation cache interface
export interface TranslationCache {
  get(key: string): string | null;
  set(key: string, value: string): void;
  has(key: string): boolean;
  clear(): void;
  size(): number;
}

// Google Translate API response
export interface GoogleTranslateResponse {
  data: {
    translations: Array<{
      translatedText: string;
      detectedSourceLanguage?: string;
    }>;
  };
}

// Translatable field configuration for API interceptor
export interface TranslatableField {
  path: string;
  type: 'text' | 'html';
  skipIfProperNoun?: boolean;
}

// API interceptor configuration
export interface InterceptorConfig {
  enabled: boolean;
  fields: Record<string, TranslatableField[]>; // endpoint pattern -> fields
  autoDetect: boolean;
}

// Translation metrics
export interface TranslationMetrics {
  totalTranslations: number;
  cacheHitRate: number;
  apiCallCount: number;
  averageTranslationTime: number;
  failureRate: number;
  missingKeyCount: number;
}

// Formatter options
export interface FormatterOptions {
  locale?: string;
  timeZone?: string;
  currency?: string;
}

// Date format options
export type DateFormatStyle = 'full' | 'long' | 'medium' | 'short';

// Number format options
export interface NumberFormatOptions extends Intl.NumberFormatOptions {
  style?: 'decimal' | 'currency' | 'percent';
}

// Supported languages configuration
export const SUPPORTED_LANGUAGES: LanguageInfo[] = [
  {
    code: 'en',
    name: 'English',
    nativeName: 'English',
    flag: '🇬🇧',
    direction: 'ltr'
  },
  {
    code: 'hi',
    name: 'Hindi',
    nativeName: 'हिन्दी',
    flag: '🇮🇳',
    direction: 'ltr'
  },
  {
    code: 'es',
    name: 'Spanish',
    nativeName: 'Español',
    flag: '🇪🇸',
    direction: 'ltr'
  },
  {
    code: 'kn',
    name: 'Kannada',
    nativeName: 'ಕನ್ನಡ',
    flag: '🇮🇳',
    direction: 'ltr'
  },
  {
    code: 'te',
    name: 'Telugu',
    nativeName: 'తెలుగు',
    flag: '🇮🇳',
    direction: 'ltr'
  },
  {
    code: 'ml',
    name: 'Malayalam',
    nativeName: 'മലയാളം',
    flag: '🇮🇳',
    direction: 'ltr'
  },
  {
    code: 'pa',
    name: 'Punjabi',
    nativeName: 'ਪੰਜਾਬੀ',
    flag: '🇮🇳',
    direction: 'ltr'
  },
  {
    code: 'bn',
    name: 'Bengali',
    nativeName: 'বাংলা',
    flag: '🇮🇳',
    direction: 'ltr'
  }
];

// Default language
export const DEFAULT_LANGUAGE: LanguageCode = 'en';

// Storage keys
export const STORAGE_KEYS = {
  LANGUAGE: 'easytrip_language',
  TRANSLATION_CACHE: 'easytrip_translation_cache'
} as const;

// API configuration
export const API_CONFIG = {
  GOOGLE_TRANSLATE_ENDPOINT: 'https://translation.googleapis.com/language/translate/v2',
  MAX_BATCH_SIZE: 100,
  RATE_LIMIT_PER_MINUTE: 100,
  CACHE_TTL: 24 * 60 * 60 * 1000, // 24 hours
  RETRY_ATTEMPTS: 3,
  RETRY_DELAY: 1000 // ms
} as const;
