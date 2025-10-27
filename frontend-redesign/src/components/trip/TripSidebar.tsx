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
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { exportService } from '@/services/exportService';
import { useToast } from '@/components/ui/use-toast';

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
  const [isExporting, setIsExporting] = useState(false);
  const [isSharing, setIsSharing] = useState(false);

  const handleBack = () => {
    navigate('/dashboard');
  };

  const handleShare = async () => {
    setIsSharing(true);
    try {
      const result = await exportService.getShareableLink(tripId);
      
      if (result.success && result.link) {
        // Copy to clipboard
        await navigator.clipboard.writeText(result.link);
        toast({
          title: 'Link Copied!',
          description: 'Shareable link copied to clipboard',
        });
      } else {
        throw new Error(result.error);
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

  const handleExport = async () => {
    setIsExporting(true);
    try {
      const result = await exportService.downloadPdf(
        tripId,
        `${destination.replace(/\s+/g, '-')}-itinerary.pdf`,
        {
          includeImages: true,
          includeMap: true,
          includeBookings: true,
        }
      );

      if (result.success) {
        toast({
          title: 'Export Successful',
          description: 'Your itinerary has been downloaded',
        });
      } else {
        throw new Error(result.error);
      }
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
          onClick={handleShare}
          title="Share Trip"
          disabled={isSharing}
        >
          {isSharing ? (
            <Loader2 className="w-5 h-5 animate-spin" />
          ) : (
            <Share2 className="w-5 h-5" />
          )}
        </Button>

        <Button
          variant="ghost"
          size="icon"
          onClick={handleExport}
          title="Export PDF"
          disabled={isExporting}
        >
          {isExporting ? (
            <Loader2 className="w-5 h-5 animate-spin" />
          ) : (
            <Download className="w-5 h-5" />
          )}
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
    </aside>
  );
}
