import React from 'react';
import { Button } from '../../ui/button';
import { ArrowLeft } from 'lucide-react';
import { TopNavigationProps } from '../shared/types';
import { UserProfileButton } from '../../shared/UserProfileButton';

export function TopNavigation({ tripData, onShare, onExportPDF, onBack }: TopNavigationProps) {
  return (
    <div className="bg-white border-b">
      <div className="max-w-6xl mx-auto px-3 sm:px-4 py-3 sm:py-4">
        <div className="flex items-center justify-between gap-3 sm:gap-4">
          {/* Left Section */}
          <div className="flex items-center gap-3 sm:gap-4">
            <Button variant="ghost" size="sm" onClick={onBack} className="p-2 sm:p-2">
              <ArrowLeft className="h-4 w-4 sm:mr-2" />
              <span className="hidden sm:inline">Back</span>
            </Button>
            <div className="min-w-0 flex-1">
              <h1 className="text-xl sm:text-2xl font-semibold truncate">Plan</h1>
            </div>
          </div>
          
          {/* Right Section - User Profile Only */}
          <div className="flex items-center">
            <UserProfileButton />
          </div>
        </div>
      </div>
    </div>
  );
}