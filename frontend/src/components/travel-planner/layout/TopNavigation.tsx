import React from 'react';
import { Button } from '../../ui/button';
import { Share2, Download } from 'lucide-react';
import { TopNavigationProps } from '../shared/types';
import { useTranslation } from 'react-i18next';
import { LanguageSelector } from '../../shared/LanguageSelector';
import { UserProfileButton } from '../../shared/UserProfileButton';

export function TopNavigation({ tripData, onShare, onExportPDF, onBack }: TopNavigationProps) {
  const { t } = useTranslation();
  
  return (
    <div className="h-12 md:h-16 bg-white border-b border-gray-200 flex items-center justify-between px-4 md:px-6">
      <div className="flex items-center space-x-2 md:space-x-4">
        <div className="flex flex-col md:flex-row md:items-center md:space-x-4">
          <h1 className="text-lg md:text-xl font-semibold">
            {t('common.yourTripTo', { destination: tripData.endLocation?.name || tripData.destination || 'Unknown' })}
          </h1>
          <span className="text-sm md:text-base text-gray-500 hidden sm:block">
            {(() => {
              const formatDate = (dateStr: string | undefined) => {
                if (!dateStr) return 'TBD';
                try {
                  const date = new Date(dateStr);
                  if (isNaN(date.getTime())) return 'TBD';
                  return date.toLocaleDateString('en-US', { day: 'numeric', month: 'long' });
                } catch {
                  return 'TBD';
                }
              };
              return `${formatDate(tripData.dates?.start)} - ${formatDate(tripData.dates?.end)}`;
            })()}
          </span>
        </div>
      </div>
      
      <div className="flex items-center space-x-2 md:space-x-3">
        <LanguageSelector />
        {/* Hide some buttons on mobile to save space */}
        <Button variant="outline" size="sm" onClick={onShare} className="hidden sm:flex">
          <Share2 className="w-4 h-4 mr-2" />
          {t('common.save')}
        </Button>
        <Button variant="outline" size="sm" onClick={onShare} className="hidden md:flex">
          <Share2 className="w-4 h-4 mr-2" />
          {t('common.share')}
        </Button>
        <Button variant="outline" size="sm" onClick={onExportPDF} className="hidden lg:flex">
          <Download className="w-4 h-4 mr-2" />
          PDF
        </Button>
        <UserProfileButton />
      </div>
    </div>
  );
}