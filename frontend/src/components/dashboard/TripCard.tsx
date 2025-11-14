/**
 * Trip Card Component
 * Displays individual trip summary with backend integration
 * 
 * Design Specifications:
 * - Card: 100% width, border-radius 12px, shadow-elevation-2
 * - Image: Height 192px (h-48), object-fit cover, 16:9 aspect ratio
 * - Hover: Lift 2px (translateY(-2px)), shadow-elevation-3, image scale 1.05
 * - Status Badge: Top-right 12px, padding 12px 16px, border-radius 20px (pill)
 * - Destination: Bottom-left 12px, text-xl font-bold, white with text-shadow
 * - Content: Padding 16px, space-y 12px
 * - Icons: 16x16px, muted-foreground color
 * - Text: 14px regular, muted-foreground for metadata
 * - Duration: 14px font-medium, primary color
 * - Buttons: Full-width outline button, ghost icon button
 * - Animation: All transitions 300ms cubic-bezier(0.4,0,0.2,1)
 */

import { useState } from 'react';
import { Card, CardContent, CardFooter } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Calendar, Users, DollarSign, MapPin, MoreVertical, Trash2, Eye, Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useTranslation } from '@/i18n';
import { useAuth } from '@/contexts/AuthContext';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

interface Trip {
  id: string;
  destination: string;
  startDate: string;
  endDate: string;
  status: 'upcoming' | 'completed';
  imageUrl?: string;
  travelers: number;
  budget: string;
}

interface TripCardProps {
  trip: Trip;
  onDelete?: (tripId: string) => void;
}

export function TripCard({ trip, onDelete }: TripCardProps) {
  const { t } = useTranslation();
  const { isGuest } = useAuth();
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  };

  const getDuration = () => {
    const start = new Date(trip.startDate);
    const end = new Date(trip.endDate);
    const days = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
    return t('components.tripCard.days', { count: days }, { count: days });
  };

  const handleDeleteClick = () => {
    setShowDeleteDialog(true);
  };

  const handleConfirmDelete = async () => {
    if (!onDelete) return;
    
    setIsDeleting(true);
    try {
      await onDelete(trip.id);
      setShowDeleteDialog(false);
    } catch (error) {
      // Error is handled by parent component
      console.error('Delete failed:', error);
    } finally {
      setIsDeleting(false);
    }
  };

  // Generate placeholder image based on destination
  const getPlaceholderImage = () => {
    if (trip.imageUrl) return trip.imageUrl;
    
    // Use a gradient based on destination name
    const colors = [
      'from-blue-500 to-purple-600',
      'from-green-500 to-teal-600',
      'from-orange-500 to-red-600',
      'from-pink-500 to-rose-600',
      'from-indigo-500 to-blue-600',
    ];
    const index = trip.destination.length % colors.length;
    return colors[index];
  };

  return (
    <>
      <Card 
        className={cn(
          "overflow-hidden transition-all duration-300 group cursor-pointer",
          "hover:shadow-elevation-3 md:hover:-translate-y-0.5",
          "active:scale-[0.98] md:active:scale-100"
        )}
        onClick={() => window.location.href = `/trip/${trip.id}`}
      >
        {/* Image/Background - Full width on mobile, aspect-video on desktop */}
        <div className="relative h-32 sm:h-36 md:h-auto md:aspect-video overflow-hidden">
          {trip.imageUrl ? (
            <img
              src={trip.imageUrl}
              alt={trip.destination}
              className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
            />
          ) : (
            <div className={cn(
              'w-full h-full bg-gradient-to-br',
              getPlaceholderImage(),
              'transition-transform duration-300 group-hover:scale-105'
            )} />
          )}
          
          {/* Gradient overlay for text contrast */}
          <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent" />
          
          {/* Status Badge */}
          <div className="absolute top-2 right-2 md:top-3 md:right-3 z-10">
            <span
              className={cn(
                'px-2.5 py-1 md:px-3 rounded-full text-xs font-semibold shadow-sm',
                trip.status === 'upcoming'
                  ? 'bg-primary text-white'
                  : 'bg-white/90 text-muted-foreground'
              )}
            >
              {trip.status === 'upcoming' ? t('common.status.upcoming') : t('common.status.completed')}
            </span>
          </div>
          
          {/* Destination - On gradient overlay */}
          <div className="absolute bottom-2 left-2 right-2 md:bottom-3 md:left-3 md:right-3 z-10">
            <h3 className="text-sm md:text-xl font-bold text-white flex items-center gap-1 md:gap-2 drop-shadow-[0_2px_8px_rgba(0,0,0,0.8)]">
              <MapPin className="w-3.5 h-3.5 md:w-5 md:h-5 flex-shrink-0" />
              <span className="truncate">{trip.destination}</span>
            </h3>
          </div>
        </div>

        {/* Content */}
        <CardContent className="flex-1 p-3 md:p-4 space-y-2 md:space-y-3">

          {/* Dates */}
          <div className="flex items-center gap-2 text-xs md:text-sm text-muted-foreground">
            <Calendar className="w-3.5 h-3.5 md:w-4 md:h-4" />
            <span className="truncate">
              {formatDate(trip.startDate)} - {formatDate(trip.endDate)}
            </span>
          </div>

          {/* Details */}
          <div className="flex items-center gap-3 md:gap-4 text-xs md:text-sm">
            <div className="flex items-center gap-1.5">
              <Users className="w-3.5 h-3.5 md:w-4 md:h-4 text-muted-foreground" />
              <span>{t('components.tripCard.travelers', { count: trip.travelers }, { count: trip.travelers })}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <DollarSign className="w-3.5 h-3.5 md:w-4 md:h-4 text-muted-foreground" />
              <span className="capitalize">{t(`components.tripCard.budget`, { amount: trip.budget })}</span>
            </div>
          </div>

          <div className="flex items-center justify-between">
            <div className="text-xs md:text-sm font-medium text-primary">
              {getDuration()}
            </div>
            
            {/* Delete button - Only show for authenticated users, not guests */}
            {!isGuest && (
              <Button
                variant="ghost"
                size="sm"
                className="h-8 w-8 p-0 hover:bg-destructive/10 hover:text-destructive"
                onClick={(e) => {
                  e.stopPropagation(); // Prevent card click
                  handleDeleteClick();
                }}
              >
                <Trash2 className="w-4 h-4" />
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Delete Confirmation Dialog */}
      <Dialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t('common.actions.delete')} Trip</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete your trip to <strong>{trip.destination}</strong>? 
              This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="gap-2 sm:gap-0">
            <Button
              variant="outline"
              onClick={() => setShowDeleteDialog(false)}
              disabled={isDeleting}
            >
              {t('common.actions.cancel')}
            </Button>
            <Button
              variant="primary"
              onClick={handleConfirmDelete}
              disabled={isDeleting}
              className="gap-2 bg-red-600 hover:bg-red-700 text-white"
            >
              {isDeleting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  {t('common.status.loading')}
                </>
              ) : (
                <>
                  <Trash2 className="w-4 h-4" />
                  {t('common.actions.delete')} Trip
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
