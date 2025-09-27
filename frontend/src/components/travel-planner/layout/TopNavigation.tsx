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
    <div className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6">
      <div className="flex items-center space-x-4">
        <h1 className="text-xl font-semibold">
          {t('common.yourTripTo', { destination: tripData.endLocation?.name || tripData.destination || 'Unknown' })}
        </h1>
        <span className="text-gray-500">
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
      
      <div className="flex items-center space-x-3">
        <LanguageSelector />
        <Button variant="outline" size="sm" onClick={onShare}>
          <Share2 className="w-4 h-4 mr-2" />
          {t('common.save')}
        </Button>
        <Button variant="outline" size="sm" onClick={onShare}>
          <Share2 className="w-4 h-4 mr-2" />
          {t('common.share')}
        </Button>
        <Button variant="outline" size="sm" onClick={onExportPDF}>
          <Download className="w-4 h-4 mr-2" />
          PDF
        </Button>
        <UserProfileButton />
      </div>
    </div>
  );
}
