/**
 * useTranslation Hook
 * Primary API for components to access translation functions
 */

import { useContext } from 'react';
import { TranslationContext } from '../context/TranslationContext';
import type { Namespace } from '../types';

/**
 * Hook to access translation functions
 * 
 * @param namespace - Optional namespace to scope translations (not enforced, just for organization)
 * @returns Translation context with t, td, language, setLanguage, isLoading
 * 
 * @example
 * ```typescript
 * const { t, language, setLanguage } = useTranslation('common');
 * 
 * // Simple translation
 * <button>{t('common.actions.save')}</button>
 * 
 * // With parameters
 * <p>{t('pages.dashboard.welcome', { name: user.name })}</p>
 * 
 * // With pluralization
 * <span>{t('components.tripCard.travelers', { count: 2 }, { count: 2 })}</span>
 * 
 * // Change language
 * <button onClick={() => setLanguage('hi')}>हिन्दी</button>
 * ```
 */
export function useTranslation(namespace?: Namespace) {
  const context = useContext(TranslationContext);

  if (!context) {
    throw new Error(
      'useTranslation must be used within a TranslationProvider. ' +
      'Make sure your app is wrapped with <TranslationProvider>.'
    );
  }

  // Note: namespace parameter is optional and not enforced
  // It's mainly for developer organization and future lazy loading
  // Components can still access any namespace using full key path
  // e.g., t('common.actions.save') works regardless of namespace parameter

  return context;
}
