import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

// Import translation files
import en from './locales/en.json';
import hi from './locales/hi.json';
import te from './locales/te.json';
import bn from './locales/bn.json';

const resources = {
  en: {
    translation: en
  },
  hi: {
    translation: hi
  },
  te: {
    translation: te
  },
  bn: {
    translation: bn
  }
};

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources,
    fallbackLng: 'en',
    debug: false,
    
    detection: {
      order: ['localStorage', 'navigator', 'htmlTag'],
      caches: ['localStorage'],
    },

    interpolation: {
      escapeValue: false, // React already does escaping
    },

    // Language-specific configurations
    supportedLngs: ['en', 'hi', 'te', 'bn'],
    
    // Indian language specific settings
    load: 'languageOnly', // Load only language, not region (e.g., 'hi' not 'hi-IN')
    
    // RTL support for languages that need it
    lng: 'en',
    
    // Namespace configuration
    defaultNS: 'translation',
    ns: ['translation'],
  });

export default i18n;

