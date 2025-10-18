import React, { ReactNode } from 'react';
import { MainToolbar } from '../toolbar/MainToolbar';

interface AppLayoutProps {
  children: ReactNode;
  showToolbar?: boolean;
}

export function AppLayout({ children, showToolbar = true }: AppLayoutProps) {
  return (
    <div className="flex flex-col h-screen">
      {showToolbar && <MainToolbar />}
      <main className="flex-1 overflow-auto">
        {children}
      </main>
    </div>
  );
}
