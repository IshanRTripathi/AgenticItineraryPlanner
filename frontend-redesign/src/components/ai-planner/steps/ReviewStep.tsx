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
      <div className="text-center mb-8">
        <h2 className="text-2xl font-bold text-foreground mb-2">
          Review your trip details
        </h2>
        <p className="text-muted-foreground">
          Make sure everything looks good before we create your itinerary
        </p>
      </div>

      <div className="grid gap-4">
        {/* Destination */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <MapPin className="w-5 h-5 text-primary" />
              Destination
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-lg font-semibold">{data.destination || 'Not selected'}</p>
          </CardContent>
        </Card>

        {/* Dates */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <Calendar className="w-5 h-5 text-primary" />
              Travel Dates
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-1">
              <p><span className="text-muted-foreground">From:</span> {data.startDate || 'Not selected'}</p>
              <p><span className="text-muted-foreground">To:</span> {data.endDate || 'Not selected'}</p>
            </div>
          </CardContent>
        </Card>

        {/* Travelers */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <Users className="w-5 h-5 text-primary" />
              Travelers
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-1">
              <p><span className="text-muted-foreground">Adults:</span> {data.adults || 0}</p>
              {(data.children || 0) > 0 && (
                <p><span className="text-muted-foreground">Children:</span> {data.children}</p>
              )}
              {(data.infants || 0) > 0 && (
                <p><span className="text-muted-foreground">Infants:</span> {data.infants}</p>
              )}
              <p className="font-semibold mt-2">Total: {totalTravelers} travelers</p>
            </div>
          </CardContent>
        </Card>

        {/* Preferences */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <Heart className="w-5 h-5 text-primary" />
              Preferences
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              <p><span className="text-muted-foreground">Budget:</span> <span className="capitalize">{data.budget || 'Not selected'}</span></p>
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

      <div className="bg-muted/50 rounded-lg p-4 text-center">
        <p className="text-sm text-muted-foreground">
          Click "Create Itinerary" to let our AI craft your perfect trip
        </p>
      </div>
    </div>
  );
}
