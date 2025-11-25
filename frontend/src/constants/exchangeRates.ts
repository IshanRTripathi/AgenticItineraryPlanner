/**
 * Exchange Rates Constants
 * Copied from backend CurrencyConversionService
 * Base currency: USD (1 USD = X foreign currency)
 * Last updated: November 2025
 */

export const BASE_CURRENCY = 'USD';

export const EXCHANGE_RATES: Record<string, number> = {
  // Base currency
  USD: 1.0,
  '$': 1.0, // Symbol alias for USD
  
  // Major currencies
  INR: 83.33,
  '₹': 83.33, // Symbol alias for INR
  EUR: 0.92,
  '€': 0.92, // Symbol alias for EUR
  GBP: 0.79,
  '£': 0.79, // Symbol alias for GBP
  JPY: 154.17,
  '¥': 154.17, // Symbol alias for JPY (also used for CNY)
  CNY: 7.17,
  AUD: 1.50,
  'A$': 1.50, // Symbol alias for AUD
  CAD: 1.42,
  'C$': 1.42, // Symbol alias for CAD
  
  // Southeast Asian currencies
  SGD: 1.33,
  'S$': 1.33, // Symbol alias for SGD
  MYR: 4.67,
  'RM': 4.67, // Symbol alias for MYR
  THB: 35.0,
  '฿': 35.0, // Symbol alias for THB
  IDR: 16000.0,
  'Rp': 16000.0, // Symbol alias for IDR
  PHP: 58.33,
  '₱': 58.33, // Symbol alias for PHP
  VND: 25417.0,
  '₫': 25417.0, // Symbol alias for VND
  KRW: 1375.0,
  '₩': 1375.0, // Symbol alias for KRW
  
  // Middle East currencies
  AED: 3.67,
  'د.إ': 3.67, // Symbol alias for AED
  SAR: 3.75,
  'ر.س': 3.75, // Symbol alias for SAR
  QAR: 3.64,
  'ر.ق': 3.64, // Symbol alias for QAR
  KWD: 0.31,
  'د.ك': 0.31, // Symbol alias for KWD
  OMR: 0.38,
  'ر.ع': 0.38, // Symbol alias for OMR
  BHD: 0.38,
  'د.ب': 0.38, // Symbol alias for BHD
  JOD: 0.71,
  'د.ا': 0.71, // Symbol alias for JOD
  ILS: 3.7,
  '₪': 3.7, // Symbol alias for ILS
  
  // European currencies
  CHF: 0.92,
  SEK: 10.83,
  'kr': 10.83, // Symbol alias for SEK (also used for NOK, DKK)
  NOK: 10.83,
  DKK: 6.92,
  PLN: 4.0,
  'zł': 4.0, // Symbol alias for PLN
  CZK: 23.0,
  'Kč': 23.0, // Symbol alias for CZK
  HUF: 360.0,
  'Ft': 360.0, // Symbol alias for HUF
  RON: 4.6,
  'lei': 4.6, // Symbol alias for RON
  BGN: 1.8,
  ISK: 138.0,
  HRK: 6.9,
  RSD: 110.0,
  
  // Other major currencies
  NZD: 1.67,
  ZAR: 18.33,
  BRL: 5.83,
  MXN: 20.0,
  RUB: 100.0,
  TRY: 34.17,
  HKD: 7.75,
  TWD: 31.67,
  
  // South Asian currencies
  PKR: 279.17,
  BDT: 110.0,
  LKR: 291.67,
  NPR: 133.33,
  MVR: 15.0,
  BTN: 83.33,
  
  // Latin American currencies
  ARS: 950.0,
  CLP: 950.0,
  COP: 4200.0,
  PEN: 3.7,
  UYU: 40.0,
  BOB: 6.9,
  
  // African currencies
  EGP: 49.17,
  MAD: 10.2,
  TND: 3.1,
  KES: 160.0,
  NGN: 1650.0,
  GHS: 15.5,
  
  // Additional currencies
  MMK: 2100.0,
  KHR: 4100.0,
  LAK: 21000.0,
  MNT: 3440.0,
  BND: 1.33,
  MOP: 8.1,
};

export const CURRENCY_SYMBOLS: Record<string, string> = {
  USD: '$',
  EUR: '€',
  GBP: '£',
  JPY: '¥',
  CNY: '¥',
  INR: '₹',
  AUD: 'A$',
  CAD: 'C$',
  CHF: 'CHF',
  SGD: 'S$',
  MYR: 'RM',
  THB: '฿',
  IDR: 'Rp',
  PHP: '₱',
  VND: '₫',
  KRW: '₩',
  AED: 'د.إ',
  SAR: '﷼',
  QAR: 'ر.ق',
  KWD: 'د.ك',
  OMR: 'ر.ع.',
  BHD: 'د.ب',
  JOD: 'د.ا',
  ILS: '₪',
  SEK: 'kr',
  NOK: 'kr',
  DKK: 'kr',
  PLN: 'zł',
  CZK: 'Kč',
  HUF: 'Ft',
  RON: 'lei',
  BGN: 'лв',
  ISK: 'kr',
  HRK: 'kn',
  RSD: 'дин',
  NZD: 'NZ$',
  BRL: 'R$',
  MXN: 'Mex$',
  ARS: '$',
  CLP: '$',
  COP: '$',
  PEN: 'S/',
  UYU: '$U',
  BOB: 'Bs',
  ZAR: 'R',
  EGP: 'E£',
  NGN: '₦',
  KES: 'KSh',
  GHS: '₵',
  MAD: 'د.م.',
  TND: 'د.ت',
  PKR: '₨',
  BDT: '৳',
  LKR: 'Rs',
  NPR: 'Rs',
  MVR: 'Rf',
  BTN: 'Nu',
  RUB: '₽',
  TRY: '₺',
  HKD: 'HK$',
  TWD: 'NT$',
  MOP: 'MOP$',
  MMK: 'K',
  KHR: '៛',
  LAK: '₭',
  BND: 'B$',
};

// Popular currencies for quick selection
export const POPULAR_CURRENCIES = [
  { code: 'USD', name: 'US Dollar', symbol: '$', flag: '🇺🇸' },
  { code: 'EUR', name: 'Euro', symbol: '€', flag: '🇪🇺' },
  { code: 'GBP', name: 'British Pound', symbol: '£', flag: '🇬🇧' },
  { code: 'JPY', name: 'Japanese Yen', symbol: '¥', flag: '🇯🇵' },
  { code: 'CNY', name: 'Chinese Yuan', symbol: '¥', flag: '🇨🇳' },
  { code: 'INR', name: 'Indian Rupee', symbol: '₹', flag: '🇮🇳' },
  { code: 'AUD', name: 'Australian Dollar', symbol: 'A$', flag: '🇦🇺' },
  { code: 'CAD', name: 'Canadian Dollar', symbol: 'C$', flag: '🇨🇦' },
  { code: 'SGD', name: 'Singapore Dollar', symbol: 'S$', flag: '🇸🇬' },
  { code: 'MYR', name: 'Malaysian Ringgit', symbol: 'RM', flag: '🇲🇾' },
  { code: 'THB', name: 'Thai Baht', symbol: '฿', flag: '🇹🇭' },
  { code: 'AED', name: 'UAE Dirham', symbol: 'د.إ', flag: '🇦🇪' },
  { code: 'CHF', name: 'Swiss Franc', symbol: 'CHF', flag: '🇨🇭' },
  { code: 'KRW', name: 'South Korean Won', symbol: '₩', flag: '🇰🇷' },
  { code: 'HKD', name: 'Hong Kong Dollar', symbol: 'HK$', flag: '🇭🇰' },
];
