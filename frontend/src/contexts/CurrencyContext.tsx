/**
 * Currency Context
 * Provides shared currency state across all components
 */

import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import { EXCHANGE_RATES, CURRENCY_SYMBOLS, BASE_CURRENCY } from '@/constants/exchangeRates';

const CURRENCY_STORAGE_KEY = 'preferred_currency';

interface CurrencyContextType {
  preferredCurrency: string;
  setPreferredCurrency: (currency: string) => void;
  convert: (amount: number, fromCurrency: string, toCurrency: string) => number;
  formatCurrency: (amount: number, currency: string, options?: Intl.NumberFormatOptions) => string;
  getCurrencySymbol: (currency: string) => string;
  isCurrencySupported: (currency: string) => boolean;
  exchangeRates: typeof EXCHANGE_RATES;
  currencySymbols: typeof CURRENCY_SYMBOLS;
}

const CurrencyContext = createContext<CurrencyContextType | undefined>(undefined);

export function CurrencyProvider({ children }: { children: ReactNode }) {
  const [preferredCurrency, setPreferredCurrencyState] = useState<string>(() => {
    if (typeof window !== 'undefined') {
      return localStorage.getItem(CURRENCY_STORAGE_KEY) || BASE_CURRENCY;
    }
    return BASE_CURRENCY;
  });

  const setPreferredCurrency = useCallback((currency: string) => {
    console.log('[CurrencyContext] 🔄 Currency changing:', {
      from: preferredCurrency,
      to: currency
    });
    setPreferredCurrencyState(currency);
    if (typeof window !== 'undefined') {
      localStorage.setItem(CURRENCY_STORAGE_KEY, currency);
    }
  }, [preferredCurrency]);

  const convert = useCallback((amount: number, fromCurrency: string, toCurrency: string): number => {
    if (fromCurrency === toCurrency) return amount;

    const fromRate = EXCHANGE_RATES[fromCurrency];
    const toRate = EXCHANGE_RATES[toCurrency];

    if (!fromRate || !toRate) {
      console.warn(`[CurrencyContext] Exchange rate not found for ${fromCurrency} or ${toCurrency}`);
      return amount;
    }

    const usdAmount = amount / fromRate;
    const result = usdAmount * toRate;
    
    console.log(`[CurrencyContext] Converting: ${amount} ${fromCurrency} → ${result.toFixed(2)} ${toCurrency}`);
    
    return result;
  }, []);

  const formatCurrency = useCallback((
    amount: number,
    currency: string,
    options?: Intl.NumberFormatOptions
  ): string => {
    try {
      return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: currency,
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
        ...options,
      }).format(amount);
    } catch (error) {
      const symbol = CURRENCY_SYMBOLS[currency] || currency;
      return `${symbol}${amount.toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`;
    }
  }, []);

  const getCurrencySymbol = useCallback((currency: string): string => {
    return CURRENCY_SYMBOLS[currency] || currency;
  }, []);

  const isCurrencySupported = useCallback((currency: string): boolean => {
    return currency in EXCHANGE_RATES;
  }, []);

  return (
    <CurrencyContext.Provider value={{
      preferredCurrency,
      setPreferredCurrency,
      convert,
      formatCurrency,
      getCurrencySymbol,
      isCurrencySupported,
      exchangeRates: EXCHANGE_RATES,
      currencySymbols: CURRENCY_SYMBOLS,
    }}>
      {children}
    </CurrencyContext.Provider>
  );
}

export function useCurrency() {
  const context = useContext(CurrencyContext);
  if (context === undefined) {
    throw new Error('useCurrency must be used within a CurrencyProvider');
  }
  return context;
}
