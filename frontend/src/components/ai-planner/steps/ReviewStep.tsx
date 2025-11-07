/**
 * Review Step
 * Step 4 of trip wizard - Final review before submission
 */

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { MapPin, Calendar, Users, Heart } from 'lucide-react';

interface ReviewStepProps {
  data: any;
  onDataChange: (data: any) => void;
}

export function ReviewStep({ data }: ReviewStepProps) {
  const totalTravelers = (data.adults || 0) + (data.children || 0) + (data.infants || 0);

  return (
    <div className="space-y-6">
      <div className="text-center mb-6">
        <h2 className="text-2xl font-bold text-foreground mb-1">
          Review your trip details
        </h2>
        <p className="text-sm text-muted-foreground">
          Make sure everything looks good before we create your itinerary
        </p>
      </div>

      {/* Two Column Layout for Review */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Destination */}
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-base">
              <MapPin className="w-4 h-4 text-primary" />
              Destination
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="font-semibold">{data.destination || 'Not selected'}</p>
          </CardContent>
        </Card>

        {/* Dates */}
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-base">
              <Calendar className="w-4 h-4 text-primary" />
              Travel Dates
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-0.5 text-sm">
              <p><span className="text-muted-foreground">From:</span> {data.startDate || 'Not selected'}</p>
              <p><span className="text-muted-foreground">To:</span> {data.endDate || 'Not selected'}</p>
            </div>
          </CardContent>
        </Card>

        {/* Travelers */}
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-base">
              <Users className="w-4 h-4 text-primary" />
              Travelers
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-0.5 text-sm">
              <p><span className="text-muted-foreground">Adults:</span> {data.adults || 0}</p>
              {(data.children || 0) > 0 && (
                <p><span className="text-muted-foreground">Children:</span> {data.children}</p>
              )}
              {(data.infants || 0) > 0 && (
                <p><span className="text-muted-foreground">Infants:</span> {data.infants}</p>
              )}
              <p className="font-semibold mt-1">Total: {totalTravelers} travelers</p>
            </div>
          </CardContent>
        </Card>

        {/* Preferences */}
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-base">
              <Heart className="w-4 h-4 text-primary" />
              Preferences
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-0.5 text-sm">
              {data.budgetRange && (
                <p><span className="text-muted-foreground">Budget:</span> ${data.budgetRange[0]} - ${data.budgetRange[1]}</p>
              )}
              <p><span className="text-muted-foreground">Pace:</span> <span className="capitalize">{data.pace || 'Not selected'}</span></p>
              {data.interests && data.interests.length > 0 && (
                <p>
                  <span className="text-muted-foreground">Interests:</span>{' '}
                  <span className="capitalize">{data.interests.join(', ')}</span>
                </p>
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="bg-muted/50 rounded-lg p-3 text-center">
        <p className="text-xs text-muted-foreground">
          Click "Create Itinerary" to let our AI craft your perfect trip
        </p>
      </div>
    </div>
  );
}
