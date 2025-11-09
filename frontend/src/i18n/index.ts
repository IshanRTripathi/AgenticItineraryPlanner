/**
 * i18n Module - Central Export Point
 * Provides a clean API for the translation system
 */

// Provider
export { TranslationProvider } from './TranslationProvider';

// Context
export { TranslationContext } from './context/TranslationContext';

// Hooks
export { useTranslation } from './hooks/useTranslation';
export { useFormatter } from './hooks/useFormatter';

// Components
export { LanguageSelector } from './components/LanguageSelector';

// Services (for advanced usage)
export { GoogleTranslateService } from './services/GoogleTranslateService';
export { DynamicTranslator } from './services/DynamicTranslator';
export { TranslationCache } from './services/TranslationCache';
export { FormatterService } from './services/FormatterService';

// Types
export type {
  LanguageCode,
  Namespace,
  TranslationKey,
  TranslationParams,
  TranslateOptions,
  DynamicTranslateOptions,
  TranslateFunction,
  DynamicTranslateFunction,
  LanguageInfo,
  TranslationContextValue,
  TranslationProviderProps,
  TranslationFile,
  CacheEntry,
  GoogleTranslateResponse,
  TranslatableField,
  InterceptorConfig,
  TranslationMetrics,
  FormatterOptions,
  DateFormatStyle,
  NumberFormatOptions
} from './types';

// Constants
export {
  SUPPORTED_LANGUAGES,
  DEFAULT_LANGUAGE,
  STORAGE_KEYS,
  API_CONFIG
} from './types';
