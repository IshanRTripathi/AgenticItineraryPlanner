/**
 * Currency Selector Component
 * UI component for switching between currencies
 * Matches LanguageSelector style and patterns
 */

import React from 'react';
import { useCurrency } from '@/hooks/useCurrency';
import { POPULAR_CURRENCIES } from '@/constants/exchangeRates';
import { cn } from '@/lib/utils';
import { DollarSign } from 'lucide-react';

interface CurrencySelectorProps {
  variant?: 'dropdown' | 'inline' | 'compact';
  showFlags?: boolean;
  className?: string;
}

export function CurrencySelector({
  variant = 'dropdown',
  showFlags = true,
  className
}: CurrencySelectorProps) {
  const { preferredCurrency, setPreferredCurrency } = useCurrency();

  const handleCurrencyChange = (newCurrency: string) => {
    console.log('[CurrencySelector] 💰 Setting preferred currency:', {
      old: preferredCurrency,
      new: newCurrency
    });
    setPreferredCurrency(newCurrency);
  };

  // Dropdown variant (for desktop header)
  if (variant === 'dropdown') {
    return (
      <div className={cn('relative', className)}>
        <select
          value={preferredCurrency}
          onChange={(e) => handleCurrencyChange(e.target.value)}
          className={cn(
            'appearance-none bg-white border border-gray-300 rounded-lg',
            'px-3 py-2 pr-8 text-sm font-medium',
            'hover:border-primary focus:border-primary focus:ring-2 focus:ring-primary/20',
            'transition-colors cursor-pointer',
            'min-h-[44px]'
          )}
          aria-label="Select currency"
        >
          {POPULAR_CURRENCIES.map((curr) => (
            <option key={curr.code} value={curr.code}>
              {showFlags && `${curr.flag} `}
              {curr.code} - {curr.name}
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

  // Inline variant (for mobile menu / profile page)
  if (variant === 'inline') {
    return (
      <div className={cn('space-y-1', className)}>
        {POPULAR_CURRENCIES.map((curr) => (
          <button
            key={curr.code}
            onClick={() => handleCurrencyChange(curr.code)}
            className={cn(
              'w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors min-h-[48px]',
              'touch-manipulation active:scale-98',
              preferredCurrency === curr.code
                ? 'bg-primary/10 text-primary font-semibold'
                : 'hover:bg-gray-50 text-gray-700'
            )}
            aria-label={`Switch to ${curr.name}`}
            aria-current={preferredCurrency === curr.code ? 'true' : undefined}
          >
            {showFlags && (
              <span className="text-2xl flex-shrink-0">{curr.flag}</span>
            )}
            <div className="flex-1 text-left">
              <div className="text-base font-medium">{curr.code} - {curr.name}</div>
              <div className="text-xs text-gray-500">{curr.symbol}</div>
            </div>
            {preferredCurrency === curr.code && (
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
    const [isOpen, setIsOpen] = React.useState(false);
    const currentCurr = POPULAR_CURRENCIES.find(c => c.code === preferredCurrency);
    
    const handleSelect = (code: string) => {
      console.log('[CurrencySelector] 🔄 Currency changed:', {
        from: preferredCurrency,
        to: code
      });
      handleCurrencyChange(code);
      setIsOpen(false);
    };
    
    return (
      <div className={cn('relative', className)}>
        <button
          onClick={() => setIsOpen(!isOpen)}
          className={cn(
            'flex items-center gap-2 px-3 py-2 rounded-lg',
            'hover:bg-gray-100 transition-colors',
            'min-w-[44px] min-h-[44px]',
            'touch-manipulation active:scale-95'
          )}
          aria-label="Change currency"
          aria-expanded={isOpen}
        >
          <DollarSign className="w-5 h-5 text-gray-600" />
          <span className="text-sm font-medium">{currentCurr?.code}</span>
          <svg 
            className={cn(
              'w-4 h-4 text-gray-500 transition-transform',
              isOpen && 'rotate-180'
            )} 
            fill="none" 
            viewBox="0 0 24 24" 
            stroke="currentColor"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </button>

        {/* Dropdown menu */}
        {isOpen && (
          <>
            {/* Backdrop to close dropdown */}
            <div 
              className="fixed inset-0 z-40" 
              onClick={() => setIsOpen(false)}
            />
            
            <div className={cn(
              'absolute right-0 mt-2 w-56 bg-white rounded-lg shadow-lg border border-gray-200',
              'z-50 max-h-96 overflow-y-auto'
            )}>
              {POPULAR_CURRENCIES.map((curr) => (
                <button
                  key={curr.code}
                  onClick={() => handleSelect(curr.code)}
                  className={cn(
                    'w-full flex items-center gap-3 px-4 py-2.5 text-left',
                    'hover:bg-gray-50 transition-colors',
                    'first:rounded-t-lg last:rounded-b-lg',
                    preferredCurrency === curr.code && 'bg-primary/10 text-primary font-semibold'
                  )}
                >
                  {showFlags && <span className="text-lg">{curr.flag}</span>}
                  <div className="flex-1">
                    <div className="text-sm font-medium">{curr.code}</div>
                    <div className="text-xs text-gray-500">{curr.name}</div>
                  </div>
                  {preferredCurrency === curr.code && (
                    <svg className="w-4 h-4 ml-auto text-primary" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                    </svg>
                  )}
                </button>
              ))}
            </div>
          </>
        )}
      </div>
    );
  }

  return null;
}
