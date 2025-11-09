/**
 * Language Selector Component
 * UI component for switching between languages
 * Supports dropdown, inline, and compact variants
 */

import React from 'react';
import { useTranslation } from '../hooks/useTranslation';
import { SUPPORTED_LANGUAGES } from '../types';
import type { LanguageCode } from '../types';
import { cn } from '@/lib/utils';

interface LanguageSelectorProps {
  variant?: 'dropdown' | 'inline' | 'compact';
  showFlags?: boolean;
  className?: string;
}

export function LanguageSelector({
  variant = 'dropdown',
  showFlags = true,
  className
}: LanguageSelectorProps) {
  const { language, setLanguage } = useTranslation();

  const handleLanguageChange = (newLanguage: LanguageCode) => {
    setLanguage(newLanguage);
  };

  // Dropdown variant (for desktop header)
  if (variant === 'dropdown') {
    return (
      <div className={cn('relative', className)}>
        <select
          value={language}
          onChange={(e) => handleLanguageChange(e.target.value as LanguageCode)}
          className={cn(
            'appearance-none bg-white border border-gray-300 rounded-lg',
            'px-3 py-2 pr-8 text-sm font-medium',
            'hover:border-primary focus:border-primary focus:ring-2 focus:ring-primary/20',
            'transition-colors cursor-pointer',
            'min-h-[44px]'
          )}
          aria-label="Select language"
        >
          {SUPPORTED_LANGUAGES.map((lang) => (
            <option key={lang.code} value={lang.code}>
              {showFlags && `${lang.flag} `}
              {lang.nativeName}
            </option>
          ))}
        </select>
        <div className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none">
          <svg className="w-4 h-4 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </div>
      </div>
    );
  }

  // Inline variant (for mobile menu)
  if (variant === 'inline') {
    return (
      <div className={cn('space-y-1', className)}>
        {SUPPORTED_LANGUAGES.map((lang) => (
          <button
            key={lang.code}
            onClick={() => handleLanguageChange(lang.code)}
            className={cn(
              'w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors min-h-[48px]',
              'touch-manipulation active:scale-98',
              language === lang.code
                ? 'bg-primary/10 text-primary font-semibold'
                : 'hover:bg-gray-50 text-gray-700'
            )}
            aria-label={`Switch to ${lang.name}`}
            aria-current={language === lang.code ? 'true' : undefined}
          >
            {showFlags && (
              <span className="text-2xl flex-shrink-0">{lang.flag}</span>
            )}
            <div className="flex-1 text-left">
              <div className="text-base font-medium">{lang.nativeName}</div>
              <div className="text-xs text-gray-500">{lang.name}</div>
            </div>
            {language === lang.code && (
              <svg className="w-5 h-5 text-primary" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
              </svg>
            )}
          </button>
        ))}
      </div>
    );
  }

  // Compact variant (icon only with popover)
  if (variant === 'compact') {
    const currentLang = SUPPORTED_LANGUAGES.find(l => l.code === language);
    
    return (
      <div className={cn('relative group', className)}>
        <button
          className={cn(
            'flex items-center gap-2 px-3 py-2 rounded-lg',
            'hover:bg-gray-100 transition-colors',
            'min-w-[44px] min-h-[44px]',
            'touch-manipulation active:scale-95'
          )}
          aria-label="Change language"
        >
          <span className="text-xl">{currentLang?.flag}</span>
          <svg className="w-4 h-4 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </button>

        {/* Dropdown menu */}
        <div className={cn(
          'absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200',
          'opacity-0 invisible group-hover:opacity-100 group-hover:visible',
          'transition-all duration-200 z-50'
        )}>
          {SUPPORTED_LANGUAGES.map((lang) => (
            <button
              key={lang.code}
              onClick={() => handleLanguageChange(lang.code)}
              className={cn(
                'w-full flex items-center gap-3 px-4 py-2.5 text-left',
                'hover:bg-gray-50 transition-colors',
                'first:rounded-t-lg last:rounded-b-lg',
                language === lang.code && 'bg-primary/10 text-primary font-semibold'
              )}
            >
              <span className="text-lg">{lang.flag}</span>
              <span className="text-sm">{lang.nativeName}</span>
              {language === lang.code && (
                <svg className="w-4 h-4 ml-auto text-primary" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                </svg>
              )}
            </button>
          ))}
        </div>
      </div>
    );
  }

  return null;
}
