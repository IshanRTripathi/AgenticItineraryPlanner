/**
 * Trip Sidebar Component
 * Fixed sidebar navigation for trip management
 * Task 24.2-24.5
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
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
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { exportService } from '@/services/exportService';
import { useToast } from '@/components/ui/use-toast';
import { useUnifiedItinerary } from '@/contexts/UnifiedItineraryContext';
import { ExportOptionsModal, ExportOptions } from '@/components/export/ExportOptionsModal';
import { ShareModal } from '@/components/share/ShareModal';

interface TripSidebarProps {
  tripId: string;
  destination: string;
  dateRange: string;
  travelerCount: number;
  activeTab: string;
  onTabChange: (tab: string) => void;
}

const NAV_ITEMS = [
  { id: 'view', label: 'View', icon: Eye },
  { id: 'plan', label: 'Plan', icon: Map },
  { id: 'chat', label: 'Chat', icon: MessageSquare },
  { id: 'bookings', label: 'Bookings', icon: CreditCard },
  { id: 'budget', label: 'Budget', icon: Wallet },
  { id: 'packing', label: 'Packing', icon: Backpack },
  { id: 'docs', label: 'Documents', icon: FileText },
];

export function TripSidebar({
  tripId,
  destination,
  dateRange,
  travelerCount,
  activeTab,
  onTabChange,
}: TripSidebarProps) {
  const navigate = useNavigate();
  const { toast } = useToast();
  const { state } = useUnifiedItinerary();
  const [isExporting, setIsExporting] = useState(false);
  const [isSharing, setIsSharing] = useState(false);
  const [isExportModalOpen, setIsExportModalOpen] = useState(false);
  const [isShareModalOpen, setIsShareModalOpen] = useState(false);

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

  return (
    <aside className="w-[280px] h-screen bg-white border-r border-border flex flex-col">
      {/* Header */}
      <div className="p-4 border-b border-border">
        <Button
          variant="ghost"
          size="sm"
          onClick={handleBack}
          className="mb-3 -ml-2"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          Back to Dashboard
        </Button>

        <h2 className="text-lg font-semibold text-foreground truncate mb-1">
          {destination}
        </h2>

        <div className="flex items-center gap-3 text-sm text-muted-foreground">
          <div className="flex items-center gap-1">
            <Calendar className="w-4 h-4" />
            <span>{dateRange}</span>
          </div>
          <div className="flex items-center gap-1">
            <Users className="w-4 h-4" />
            <span>{travelerCount}</span>
          </div>
        </div>
      </div>

      {/* Navigation Items */}
      <nav className="flex-1 overflow-y-auto py-2">
        {NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;

          return (
            <button
              key={item.id}
              onClick={() => onTabChange(item.id)}
              className={cn(
                'w-full flex items-center gap-3 px-4 h-12 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-primary text-primary-foreground'
                  : 'text-muted-foreground hover:bg-primary/5 hover:text-foreground'
              )}
            >
              <Icon className="w-5 h-5" />
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Footer Actions */}
      <div className="p-4 border-t border-border flex items-center justify-around">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setIsShareModalOpen(true)}
          title="Share Trip"
        >
          <Share2 className="w-5 h-5" />
        </Button>

        <Button
          variant="ghost"
          size="icon"
          onClick={() => setIsExportModalOpen(true)}
          title="Export PDF"
        >
          <Download className="w-5 h-5" />
        </Button>

        <Button
          variant="ghost"
          size="icon"
          className="text-destructive hover:text-destructive hover:bg-destructive/10"
          onClick={handleDelete}
          title="Delete Trip"
        >
          <Trash2 className="w-5 h-5" />
        </Button>
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
    </aside>
  );
}
