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
      <Card className={cn(
        "overflow-hidden transition-all duration-300 group",
        "hover:shadow-elevation-3 md:hover:-translate-y-0.5",
        "active:scale-[0.98] md:active:scale-100"
      )}>
        {/* Mobile: Horizontal layout, Desktop: Vertical */}
        <div className="flex md:block">
          {/* Image */}
          <div className="relative w-32 md:w-full aspect-square md:aspect-video overflow-hidden flex-shrink-0">
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
            <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
            
            {/* Status Badge */}
            <div className="absolute top-3 right-3">
              <span
                className={cn(
                  'px-3 py-1 rounded-full text-xs font-semibold shadow-sm',
                  trip.status === 'upcoming'
                    ? 'bg-primary text-white'
                    : 'bg-white/90 text-muted-foreground'
                )}
              >
                {trip.status === 'upcoming' ? t('common.status.upcoming') : t('common.status.completed')}
              </span>
            </div>

            {/* Destination */}
            <div className="absolute bottom-3 left-3 right-3">
              <h3 className="text-xl font-bold text-white flex items-center gap-2 drop-shadow-lg">
                <MapPin className="w-5 h-5" />
                {trip.destination}
              </h3>
            </div>
          </div>
        </div>

        {/* Content */}
        <CardContent className="flex-1 p-3 md:p-4 space-y-2 md:space-y-3">
          {/* Dates */}
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Calendar className="w-4 h-4" />
            <span>
              {formatDate(trip.startDate)} - {formatDate(trip.endDate)}
            </span>
          </div>

          {/* Details */}
          <div className="flex items-center gap-4 text-sm">
            <div className="flex items-center gap-1.5">
              <Users className="w-4 h-4 text-muted-foreground" />
              <span>{t('components.tripCard.travelers', { count: trip.travelers }, { count: trip.travelers })}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <DollarSign className="w-4 h-4 text-muted-foreground" />
              <span className="capitalize">{t(`components.tripCard.budget`, { amount: trip.budget })}</span>
            </div>
          </div>

          <div className="text-sm font-medium text-primary">
            {getDuration()}
          </div>
        </CardContent>

        {/* Footer */}
        <CardFooter className="p-4 pt-0 flex gap-2">
          <Button
            variant="outline"
            className="flex-1 gap-2"
            onClick={() => window.location.href = `/trip/${trip.id}`}
          >
            <Eye className="w-4 h-4" />
            {t('common.actions.view')}
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="px-3 hover:bg-destructive/10 hover:text-destructive"
            onClick={handleDeleteClick}
          >
            <Trash2 className="w-4 h-4" />
          </Button>
        </CardFooter>
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
