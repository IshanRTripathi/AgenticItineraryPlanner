import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Alert, AlertDescription } from '../ui/alert';
import { MapPin, Calendar, Users, DollarSign, ExternalLink, AlertTriangle } from 'lucide-react';
import { apiClient } from '../../services/apiClient';

interface ShareLinkPreviewProps {
  shareId: string;
}

export const ShareLinkPreview: React.FC<ShareLinkPreviewProps> = ({
  shareId
}) => {
  const [itinerary, setItinerary] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadSharedItinerary();
  }, [shareId]);

  const loadSharedItinerary = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getPublicItinerary(shareId);
      setItinerary(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load shared itinerary');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="w-16 h-16 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
          <p className="text-gray-600">Loading itinerary...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen p-4">
        <Alert variant="destructive" className="max-w-md">
          <AlertTriangle className="h-4 w-4" />
          <AlertDescription>
            {error}
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  if (!itinerary) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="bg-white border-b">
        <div className="max-w-4xl mx-auto px-4 py-6">
          <div className="flex items-start justify-between">
            <div>
              <h1 className="text-3xl font-bold mb-2">{itinerary.destination}</h1>
              <p className="text-gray-600">{itinerary.summary}</p>
            </div>
            <Badge className="bg-blue-100 text-blue-800">
              Shared Itinerary
            </Badge>
          </div>

          <div className="flex flex-wrap gap-4 mt-4">
            {itinerary.startDate && (
              <div className="flex items-center gap-2 text-sm text-gray-600">
                <Calendar className="h-4 w-4" />
                {new Date(itinerary.startDate).toLocaleDateString()} - {new Date(itinerary.endDate).toLocaleDateString()}
              </div>
            )}
            {itinerary.party && (
              <div className="flex items-center gap-2 text-sm text-gray-600">
                <Users className="h-4 w-4" />
                {itinerary.party.adults} adults
                {itinerary.party.children > 0 && `, ${itinerary.party.children} children`}
              </div>
            )}
            {itinerary.budget && (
              <div className="flex items-center gap-2 text-sm text-gray-600">
                <DollarSign className="h-4 w-4" />
                {itinerary.budget.tier} budget
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="space-y-6">
          {itinerary.itinerary?.days?.map((day: any, index: number) => (
            <Card key={index}>
              <CardHeader>
                <CardTitle>Day {index + 1}</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {day.components?.map((component: any, compIndex: number) => (
                    <div key={compIndex} className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg">
                      <div className="flex-1">
                        <h4 className="font-medium">{component.name}</h4>
                        <div className="flex items-center gap-2 mt-1 text-sm text-gray-600">
                          <Badge variant="outline">{component.type}</Badge>
                          {component.location && (
                            <div className="flex items-center gap-1">
                              <MapPin className="h-3 w-3" />
                              {component.location.name}
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>

        <div className="mt-8 p-6 bg-blue-50 border border-blue-200 rounded-lg text-center">
          <h3 className="text-lg font-semibold mb-2">Create Your Own Itinerary</h3>
          <p className="text-sm text-gray-600 mb-4">
            Start planning your perfect trip with our AI-powered travel planner
          </p>
          <Button>
            <ExternalLink className="h-4 w-4 mr-2" />
            Get Started
          </Button>
        </div>
      </div>
    </div>
  );
};
