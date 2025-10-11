import React, { ReactNode } from 'react';
import { NotificationProvider } from '../components/notifications/NotificationContainer';
import { PreviewSettingsProvider } from './PreviewSettingsContext';

interface AppProvidersProps {
  children: ReactNode;
}

export function AppProviders({ children }: AppProvidersProps) {
  return (
    <PreviewSettingsProvider>
      <NotificationProvider>
        {children}
      </NotificationProvider>
    </PreviewSettingsProvider>
  );
}
