/**
 * Preview Settings Context
 * Manages user preferences for change preview display
 */

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';

interface PreviewSettings {
  useAdvancedDiff: boolean;
  defaultViewMode: 'side-by-side' | 'unified';
  showUnchanged: boolean;
  cachePreferences: boolean;
}

interface PreviewSettingsContextType {
  settings: PreviewSettings;
  updateSettings: (updates: Partial<PreviewSettings>) => void;
  resetSettings: () => void;
}

const defaultSettings: PreviewSettings = {
  useAdvancedDiff: false,
  defaultViewMode: 'side-by-side',
  showUnchanged: false,
  cachePreferences: true,
};

const STORAGE_KEY = 'tripplanner_preview_settings';

const PreviewSettingsContext = createContext<PreviewSettingsContextType | undefined>(undefined);

export function PreviewSettingsProvider({ children }: { children: ReactNode }) {
  const [settings, setSettings] = useState<PreviewSettings>(() => {
    // Load from localStorage if available
    if (typeof window !== 'undefined') {
      try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
          return { ...defaultSettings, ...JSON.parse(stored) };
        }
      } catch (error) {
        console.error('Failed to load preview settings:', error);
      }
    }
    return defaultSettings;
  });

  // Save to localStorage whenever settings change
  useEffect(() => {
    if (settings.cachePreferences && typeof window !== 'undefined') {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
      } catch (error) {
        console.error('Failed to save preview settings:', error);
      }
    }
  }, [settings]);

  const updateSettings = (updates: Partial<PreviewSettings>) => {
    setSettings(prev => ({ ...prev, ...updates }));
  };

  const resetSettings = () => {
    setSettings(defaultSettings);
    if (typeof window !== 'undefined') {
      localStorage.removeItem(STORAGE_KEY);
    }
  };

  return (
    <PreviewSettingsContext.Provider value={{ settings, updateSettings, resetSettings }}>
      {children}
    </PreviewSettingsContext.Provider>
  );
}

export function usePreviewSettings() {
  const context = useContext(PreviewSettingsContext);
  if (!context) {
    throw new Error('usePreviewSettings must be used within PreviewSettingsProvider');
  }
  return context;
}
