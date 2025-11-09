/**
 * Translation Provider
 * Root-level provider that initializes and manages translation system
 */

import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { TranslationContext } from './context/TranslationContext';
import { detectBrowserLanguage } from './utils/languageDetector';
import { DynamicTranslator } from './services/DynamicTranslator';
import type {
  TranslationProviderProps,
  LanguageCode,
  TranslationParams,
  TranslateOptions,
  DynamicTranslateOptions,
  TranslationFile
} from './types';
import { DEFAULT_LANGUAGE, STORAGE_KEYS } from './types';

export function TranslationProvider({
  children,
  defaultLanguage = DEFAULT_LANGUAGE,
  supportedLanguages = ['en', 'hi', 'es', 'kn', 'te', 'ml', 'pa', 'bn']
}: TranslationProviderProps) {
  const [language, setLanguageState] = useState<LanguageCode>(defaultLanguage);
  const [isLoading, setIsLoading] = useState(true);
  const [translations, setTranslations] = useState<Record<string, TranslationFile>>({});
  const [renderKey, setRenderKey] = useState(0);
  
  // Initialize dynamic translator (only once)
  const dynamicTranslator = useRef<DynamicTranslator | null>(null);
  if (!dynamicTranslator.current) {
    dynamicTranslator.current = new DynamicTranslator();
  }

  // Load translations for a specific language
  const loadTranslations = useCallback(async (lang: LanguageCode) => {
    try {
      setIsLoading(true);
      
      // Load all namespace files for the language
      const [common, pages, components, errors, validation] = await Promise.all([
        import(`./locales/${lang}/common.json`),
        import(`./locales/${lang}/pages.json`),
        import(`./locales/${lang}/components.json`),
        import(`./locales/${lang}/errors.json`),
        import(`./locales/${lang}/validation.json`)
      ]);

      setTranslations({
        common: common.default || common,
        pages: pages.default || pages,
        components: components.default || components,
        errors: errors.default || errors,
        validation: validation.default || validation
      });

      console.log(`[TranslationProvider] Loaded translations for language: ${lang}`);
    } catch (error) {
      console.error(`[TranslationProvider] Failed to load translations for ${lang}:`, error);
      
      // Fallback to English if loading fails
      if (lang !== 'en') {
        console.warn('[TranslationProvider] Falling back to English');
        await loadTranslations('en');
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Initialize: Load language from localStorage, browser, or use default
  useEffect(() => {
    const savedLanguage = localStorage.getItem(STORAGE_KEYS.LANGUAGE) as LanguageCode;
    const browserLanguage = detectBrowserLanguage();
    
    // Priority: saved > browser > default
    const initialLanguage = 
      (savedLanguage && supportedLanguages.includes(savedLanguage)) ? savedLanguage :
      (browserLanguage && supportedLanguages.includes(browserLanguage)) ? browserLanguage :
      defaultLanguage;

    console.log('[TranslationProvider] Initializing with language:', initialLanguage, {
      saved: savedLanguage,
      browser: browserLanguage,
      default: defaultLanguage
    });

    setLanguageState(initialLanguage);
    loadTranslations(initialLanguage);
    
    // Set initial HTML lang attribute
    document.documentElement.lang = initialLanguage;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // Only run once on mount

  // Change language handler
  const setLanguage = useCallback(async (newLanguage: LanguageCode) => {
    if (newLanguage === language) return;

    console.log(`[TranslationProvider] Changing language from ${language} to ${newLanguage}`);
    
    // Save to localStorage
    localStorage.setItem(STORAGE_KEYS.LANGUAGE, newLanguage);
    
    // Update HTML lang attribute
    document.documentElement.lang = newLanguage;
    
    // Load new translations first
    await loadTranslations(newLanguage);
    
    // Then update state to trigger re-render with new translations
    setLanguageState(newLanguage);
    
    // Force re-render by updating key
    setRenderKey(prev => prev + 1);
  }, [language, loadTranslations]);

  // Translation function for static content
  // Include language and renderKey to force recreation when language changes
  const t = useCallback((key: string, params?: TranslationParams, options?: TranslateOptions): string => {
    try {
      // Split key into namespace and path (e.g., "common.actions.save")
      const parts = key.split('.');
      const namespace = parts[0];
      const path = parts.slice(1);

      // Get translation from namespace
      let value: any = translations[namespace];
      
      if (!value) {
        if (process.env.NODE_ENV === 'development') {
          console.warn(`[Translation] Namespace not found: ${namespace} for language: ${language}`);
        }
        return options?.defaultValue || key;
      }

      // Navigate through nested keys
      for (const part of path) {
        value = value?.[part];
        if (value === undefined) {
          if (process.env.NODE_ENV === 'development') {
            console.warn(`[Translation] Key not found: ${key} for language: ${language}`);
          }
          return options?.defaultValue || key;
        }
      }

      // Handle pluralization
      if (options?.count !== undefined && typeof options.count === 'number') {
        // For pluralization, look for key_plural in the same location
        const lastKey = path[path.length - 1];
        const pluralKey = options.count === 1 ? lastKey : `${lastKey}_plural`;
        
        // Navigate to parent object
        let parentValue: any = translations[namespace];
        for (const part of path.slice(0, -1)) {
          parentValue = parentValue?.[part];
        }
        
        // Try to get plural form
        const pluralValue = parentValue?.[pluralKey];
        if (pluralValue && typeof pluralValue === 'string') {
          value = pluralValue;
        }
      }

      // Ensure we have a string
      if (typeof value !== 'string') {
        if (process.env.NODE_ENV === 'development') {
          console.warn(`[Translation] Value is not a string for key: ${key}`);
        }
        return options?.defaultValue || key;
      }

      // Interpolate parameters
      if (params) {
        return value.replace(/\{\{(\w+)\}\}/g, (match: string, paramKey: string) => {
          const paramValue = params[paramKey];
          return paramValue !== undefined ? String(paramValue) : match;
        });
      }

      return value;
    } catch (error) {
      console.error(`[Translation] Error translating key: ${key}`, error);
      return options?.defaultValue || key;
    }
  }, [translations, language, renderKey]);

  // Dynamic translation function using Google Translate API
  const td = useCallback(async (text: string, options?: DynamicTranslateOptions): Promise<string> => {
    if (!dynamicTranslator.current) {
      console.warn('[TranslationProvider] Dynamic translator not initialized');
      return text;
    }

    // Use current language as target if not specified
    const targetLanguage = options?.to || language;
    
    // If target is English, no translation needed
    if (targetLanguage === 'en') {
      return text;
    }

    try {
      return await dynamicTranslator.current.translate(text, {
        from: options?.from || 'en',
        to: targetLanguage,
        cache: options?.cache !== false, // Default to true
        format: options?.format || 'text',
        skipIfProperNoun: options?.skipIfProperNoun || false
      });
    } catch (error) {
      console.error('[TranslationProvider] Dynamic translation failed:', error);
      return text; // Return original on error
    }
  }, [language]);

  // Memoize context value - language change will trigger re-render
  const contextValue = useMemo(() => {
    console.log('[TranslationProvider] Context value updated:', { 
      language, 
      renderKey,
      translationsLoaded: Object.keys(translations).length > 0
    });
    return {
      language,
      setLanguage,
      isLoading,
      translations,
      t,
      td
    };
  }, [language, renderKey, setLanguage, isLoading, translations, t, td]);

  // Don't render children until translations are loaded
  if (isLoading || Object.keys(translations).length === 0) {
    return (
      <TranslationContext.Provider value={contextValue}>
        <div className="min-h-screen flex items-center justify-center">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
            <p className="text-sm text-muted-foreground">Loading...</p>
          </div>
        </div>
      </TranslationContext.Provider>
    );
  }

  return (
    <TranslationContext.Provider value={contextValue}>
      {children}
    </TranslationContext.Provider>
  );
}
