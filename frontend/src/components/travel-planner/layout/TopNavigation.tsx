import React from 'react';
import { Button } from '../../ui/button';
import { Avatar, AvatarFallback } from '../../ui/avatar';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '../../ui/dropdown-menu';
import { Share2, Download } from 'lucide-react';
import { TopNavigationProps } from '../shared/types';
import { useTranslation } from 'react-i18next';
import { LanguageSelector } from '../../shared/LanguageSelector';

export function TopNavigation({ tripData, onShare, onExportPDF, onBack }: TopNavigationProps) {
  const { t } = useTranslation();
  
  return (
    <div className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6">
      <div className="flex items-center space-x-4">
        <h1 className="text-xl font-semibold">
          {t('common.yourTripTo', { destination: tripData.endLocation?.name || tripData.destination || 'Unknown' })}
        </h1>
        <span className="text-gray-500">
          {tripData.dates?.start ? new Date(tripData.dates.start).toLocaleDateString('en-US', { day: 'numeric', month: 'long' }) : 'TBD'} – {' '}
          {tripData.dates?.end ? new Date(tripData.dates.end).toLocaleDateString('en-US', { day: 'numeric', month: 'long' }) : 'TBD'}
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
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="sm" className="rounded-full">
              <Avatar className="w-8 h-8">
                <AvatarFallback>U</AvatarFallback>
              </Avatar>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={onBack}>
              {t('common.backToOverview')}
            </DropdownMenuItem>
            <DropdownMenuItem>{t('common.settings')}</DropdownMenuItem>
            <DropdownMenuItem>{t('common.help')}</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>
  );
}
