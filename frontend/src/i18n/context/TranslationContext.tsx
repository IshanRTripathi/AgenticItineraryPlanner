/**
 * Translation Context
 * Provides translation state and functions to all components
 */

import { createContext } from 'react';
import type {
  TranslationContextValue,
  LanguageCode,
  TranslationParams,
  TranslateOptions,
  DynamicTranslateOptions
} from '../types';

// Default translation function (returns key as fallback)
const defaultTranslate = (key: string, params?: TranslationParams, options?: TranslateOptions): string => {
  if (process.env.NODE_ENV === 'development') {
    console.warn(`Translation context not initialized. Key: ${key}`);
  }
  return key;
};

// Default dynamic translation function (returns original text)
const defaultDynamicTranslate = async (text: string, options?: DynamicTranslateOptions): Promise<string> => {
  if (process.env.NODE_ENV === 'development') {
    console.warn(`Dynamic translation context not initialized. Text: ${text.substring(0, 50)}...`);
  }
  return text;
};

// Default context value
const defaultContextValue: TranslationContextValue = {
  language: 'en',
  setLanguage: () => {
    console.warn('TranslationProvider not found. Wrap your app with TranslationProvider.');
  },
  isLoading: false,
  translations: {},
  t: defaultTranslate,
  td: defaultDynamicTranslate
};

// Create context
export const TranslationContext = createContext<TranslationContextValue>(defaultContextValue);

// Display name for debugging
TranslationContext.displayName = 'TranslationContext';
