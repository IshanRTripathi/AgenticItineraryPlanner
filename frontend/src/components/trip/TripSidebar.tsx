/**
 * Trip Sidebar Component
 * Fixed sidebar navigation for trip management
 * Mobile-optimized: Drawer on mobile/tablet, fixed sidebar on desktop
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Drawer } from '@/components/ui/drawer';
import {
  ArrowLeft,
  Eye,
  Map,
  CreditCard,
  Wallet,
  Backpack,
  FileText,
  Share2,
  Download,
  Trash2,
  Calendar,
  Users,
  Loader2,
  MessageSquare,
  Menu,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { exportService } from '@/services/exportService';
import { useToast } from '@/components/ui/use-toast';
import { useUnifiedItinerary } from '@/contexts/UnifiedItineraryContext';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { ExportOptionsModal, ExportOptions } from '@/components/export/ExportOptionsModal';
import { ShareModal } from '@/components/share/ShareModal';
import { useTranslation } from '@/i18n';

interface TripSidebarProps {
  tripId: string;
  destination: string;
  dateRange: string;
  travelerCount: number;
  activeTab: string;
  onTabChange: (tab: string) => void;
  isOpen?: boolean;
  onClose?: () => void;
}

export function TripSidebar({
  tripId,
  destination,
  dateRange,
  travelerCount,
  activeTab,
  onTabChange,
  isOpen = true,
  onClose,
}: TripSidebarProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { toast } = useToast();
  const { state } = useUnifiedItinerary();
  const [isExporting, setIsExporting] = useState(false);
  const [isSharing, setIsSharing] = useState(false);
  const [isExportModalOpen, setIsExportModalOpen] = useState(false);
  const [isShareModalOpen, setIsShareModalOpen] = useState(false);
  const isMobile = useMediaQuery('(max-width: 1024px)');

  const NAV_ITEMS = [
    { id: 'view', label: t('pages.tripDetail.tabs.view'), icon: Eye },
    { id: 'plan', label: t('pages.tripDetail.tabs.plan'), icon: Map },
    { id: 'chat', label: t('pages.tripDetail.tabs.chat'), icon: MessageSquare },
    { id: 'bookings', label: t('pages.tripDetail.tabs.bookings'), icon: CreditCard },
    { id: 'budget', label: t('pages.tripDetail.tabs.budget'), icon: Wallet },
    { id: 'packing', label: t('pages.tripDetail.tabs.packing'), icon: Backpack },
    { id: 'docs', label: t('pages.tripDetail.tabs.docs'), icon: FileText },
  ];

  const handleBack = () => {
    navigate('/dashboard');
  };

  const handleShare = async () => {
    setIsSharing(true);
    try {
      const shareLink = await exportService.generateShareLink(tripId);

      toast({
        title: 'Link Copied!',
        description: 'Shareable link copied to clipboard',
      });

      // Try Web Share API if available
      if (typeof navigator.share === 'function' && state.itinerary) {
        try {
          await exportService.shareViaWebAPI(state.itinerary as any);
        } catch (e) {
          // User cancelled or not supported, link already copied
        }
      }
    } catch (error) {
      toast({
        title: 'Share Failed',
        description: 'Could not generate shareable link',
        variant: 'destructive',
      });
    } finally {
      setIsSharing(false);
    }
  };

  const handleExport = async (options: ExportOptions) => {
    if (!state.itinerary) {
      toast({
        title: 'Export Failed',
        description: 'Itinerary not loaded',
        variant: 'destructive',
      });
      return;
    }

    setIsExporting(true);
    try {
      // TODO: Use options for enhanced export
      await exportService.exportToPDF(state.itinerary as any);

      toast({
        title: 'Export Successful',
        description: 'Your itinerary is ready to print or save as PDF',
      });
      setIsExportModalOpen(false);
    } catch (error) {
      toast({
        title: 'Export Failed',
        description: 'Could not export PDF',
        variant: 'destructive',
      });
    } finally {
      setIsExporting(false);
    }
  };

  const handleDelete = () => {
    if (confirm('Are you sure you want to delete this trip? This action cannot be undone.')) {
      // TODO: Call delete API
      console.log('Delete trip:', tripId);
      navigate('/dashboard');
    }
  };

  const handleTabClick = (tabId: string) => {
    onTabChange(tabId);
    if (isMobile && onClose) {
      onClose();
    }
  };

  const sidebarContent = (
    <>
      {/* Header with Trip Info */}
      <div className="p-6 border-b border-border bg-gradient-to-br from-primary/5 to-transparent">
        <Button
          variant="ghost"
          size="sm"
          onClick={handleBack}
          className="min-w-[44px] min-h-[44px] -ml-2 mb-4 touch-manipulation active:scale-95 hover:bg-white/50"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          {t('pages.tripDetail.sidebar.backToDashboard')}
        </Button>
      </div>

      {/* Navigation Items */}
      <nav className="flex-1 overflow-y-auto py-4 px-3">
        <div className="space-y-1">
          {NAV_ITEMS.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;

            return (
              <button
                key={item.id}
                onClick={() => handleTabClick(item.id)}
                className={cn(
                  'w-full flex items-center gap-3 px-4 py-3 rounded-lg',
                  'text-sm font-medium transition-all duration-200',
                  'touch-manipulation active:scale-95',
                  isActive
                    ? 'bg-primary text-primary-foreground shadow-sm'
                    : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'
                )}
              >
                <Icon className="w-5 h-5 flex-shrink-0" />
                <span>{item.label}</span>
              </button>
            );
          })}
        </div>
      </nav>

      {/* Footer Actions */}
      <div className="p-4 border-t border-border bg-gray-50/50">
        <div className="grid grid-cols-3 gap-2 mb-3">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setIsShareModalOpen(true)}
            className="flex flex-col items-center gap-1 h-auto py-3 touch-manipulation active:scale-95 hover:bg-white hover:border-primary hover:text-primary"
          >
            <Share2 className="w-5 h-5" />
            <span className="text-xs">{t('pages.tripDetail.sidebar.share')}</span>
          </Button>

          <Button
            variant="outline"
            size="sm"
            onClick={() => setIsExportModalOpen(true)}
            className="flex flex-col items-center gap-1 h-auto py-3 touch-manipulation active:scale-95 hover:bg-white hover:border-primary hover:text-primary"
          >
            <Download className="w-5 h-5" />
            <span className="text-xs">{t('pages.tripDetail.sidebar.export')}</span>
          </Button>

          <Button
            variant="outline"
            size="sm"
            className="flex flex-col items-center gap-1 h-auto py-3 text-destructive hover:text-destructive hover:bg-destructive/10 hover:border-destructive touch-manipulation active:scale-95"
            onClick={handleDelete}
          >
            <Trash2 className="w-5 h-5" />
            <span className="text-xs">{t('pages.tripDetail.sidebar.delete')}</span>
          </Button>
        </div>
      </div>

      {/* Export Options Modal */}
      <ExportOptionsModal
        isOpen={isExportModalOpen}
        onClose={() => setIsExportModalOpen(false)}
        onExport={handleExport}
        isExporting={isExporting}
      />

      {/* Share Modal */}
      <ShareModal
        isOpen={isShareModalOpen}
        onClose={() => setIsShareModalOpen(false)}
        itineraryId={tripId}
        itinerary={state.itinerary}
      />
    </>
  );

  return (
    <>
      {/* Mobile/Tablet: Drawer */}
      {isMobile ? (
        <Drawer open={isOpen} onClose={onClose || (() => {})} side="left">
          <div className="flex flex-col h-full bg-white">
            {sidebarContent}
          </div>
        </Drawer>
      ) : (
        /* Desktop: Fixed Sidebar */
        <aside className="w-[280px] h-screen bg-gradient-to-b from-gray-50 to-white border-r border-gray-200 flex flex-col shadow-sm">
          {sidebarContent}
        </aside>
      )}
    </>
  );
}
