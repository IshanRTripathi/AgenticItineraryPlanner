import React, { useState } from 'react';
import { Button } from './ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Badge } from './ui/badge';
import { Input } from './ui/input';
import { Switch } from './ui/switch';
import { Label } from './ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './ui/tabs';
import { 
  ArrowLeft, 
  Share2, 
  Copy, 
  Eye,
  EyeOff,
  MapPin,
  Calendar,
  Users,
  ThumbsUp,
  ThumbsDown,
  MessageCircle,
  Link2,
  QrCode
} from 'lucide-react';
import { TripData } from '../types/TripData';

interface ShareViewProps {
  tripData: TripData;
  onBack: () => void;
}

export function ShareView({ tripData, onBack }: ShareViewProps) {
  const [isPrivate, setIsPrivate] = useState(false);
  const [showPrices, setShowPrices] = useState(false);
  const [shareLink] = useState(`https://tripai.com/share/${tripData.id || 'demo123'}`);
  const [comments] = useState([
    {
      id: 1,
      author: 'Sarah M.',
      text: 'This looks amazing! Can\'t wait to see your photos 📸',
      timestamp: '2 hours ago',
      likes: 3
    },
    {
      id: 2,
      author: 'Mike R.',
      text: 'That heritage walk sounds incredible. Have you been before?',
      timestamp: '4 hours ago',
      likes: 1
    }
  ]);
  const [votes] = useState({ thumbsUp: 12, thumbsDown: 1 });

  const copyShareLink = () => {
    navigator.clipboard.writeText(shareLink);
    // Could add toast notification here
  };

  const shareViaWhatsApp = () => {
    const message = `Check out my upcoming trip to ${tripData.destination || tripData.endLocation?.name || 'Unknown'}! ${shareLink}`;
    const whatsappUrl = `https://wa.me/?text=${encodeURIComponent(message)}`;
    window.open(whatsappUrl, '_blank');
  };

  const shareViaEmail = () => {
    const subject = `My Trip to ${tripData.destination}`;
    const body = `Hi!\n\nI wanted to share my upcoming trip to ${tripData.destination} with you. Check out the itinerary here: ${shareLink}\n\nLooking forward to this adventure!\n\nCheers!`;
    const mailtoUrl = `mailto:?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
    window.open(mailtoUrl);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric'
    });
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b">
        <div className="max-w-4xl mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <Button variant="ghost" onClick={onBack}>
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back
              </Button>
              <div>
                <h1 className="text-2xl">Share Your Trip</h1>
                <p className="text-gray-600">Let others see your adventure</p>
              </div>
            </div>
            
            <Badge variant="outline" className="flex items-center gap-2">
              {isPrivate ? <EyeOff className="h-3 w-3" /> : <Eye className="h-3 w-3" />}
              {isPrivate ? 'Private' : 'Public'}
            </Badge>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-4 py-6">
        <div className="grid lg:grid-cols-3 gap-6">
          {/* Trip Preview */}
          <div className="lg:col-span-2">
            <Card>
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div>
                    <CardTitle className="flex items-center gap-2 text-2xl">
                      <MapPin className="h-6 w-6 text-indigo-600" />
                      {tripData.destination}
                    </CardTitle>
                    <CardDescription className="flex items-center gap-4 text-base mt-2">
                      <span className="flex items-center gap-1">
                        <Calendar className="h-4 w-4" />
                        {formatDate(tripData.dates.start)} - {formatDate(tripData.dates.end)}
                      </span>
                      <span className="flex items-center gap-1">
                        <Users className="h-4 w-4" />
                        {tripData.partySize} {tripData.partySize === 1 ? 'traveler' : 'travelers'}
                      </span>
                    </CardDescription>
                  </div>
                  
                  <div className="flex items-center gap-2">
                    <Button variant="outline" size="sm">
                      <ThumbsUp className="h-4 w-4 mr-1" />
                      {votes.thumbsUp}
                    </Button>
                    <Button variant="outline" size="sm">
                      <ThumbsDown className="h-4 w-4 mr-1" />
                      {votes.thumbsDown}
                    </Button>
                  </div>
                </div>
              </CardHeader>
              
              <CardContent>
                {/* Trip Highlights */}
                <div className="space-y-4">
                  <div>
                    <h4 className="font-medium mb-2">Trip Highlights</h4>
                    <div className="flex flex-wrap gap-2">
                      {tripData.themes.map((theme) => (
                        <Badge key={theme} variant="outline">
                          {theme}
                        </Badge>
                      ))}
                    </div>
                  </div>

                  {showPrices && (
                    <div className="p-4 bg-blue-50 rounded-lg">
                      <h4 className="font-medium mb-2">Budget Information</h4>
                      <p className="text-2xl font-semibold text-blue-600">
                        ₹{tripData.budget.toLocaleString()}
                      </p>
                      <p className="text-sm text-gray-600">
                        ₹{Math.floor(tripData.budget / tripData.partySize).toLocaleString()} per person
                      </p>
                    </div>
                  )}

                  {/* Sample Itinerary Preview */}
                  <div>
                    <h4 className="font-medium mb-3">Itinerary Preview</h4>
                    <div className="space-y-3">
                      {tripData.itinerary?.days?.slice(0, 2).map((day: any) => (
                        <div key={day.day} className="border rounded-lg p-4">
                          <h5 className="font-medium mb-2">
                            Day {day.day} - {new Date(day.date).toLocaleDateString()}
                          </h5>
                          <div className="text-sm text-gray-600 space-y-1">
                            {day.activities.slice(0, 3).map((activity: any) => (
                              <div key={activity.id} className="flex items-center gap-2">
                                <span className="text-xs bg-gray-200 px-2 py-1 rounded">
                                  {activity.time}
                                </span>
                                <span>{activity.title}</span>
                              </div>
                            ))}
                            {day.activities.length > 3 && (
                              <p className="text-xs text-gray-500 italic">
                                +{day.activities.length - 3} more activities
                              </p>
                            )}
                          </div>
                        </div>
                      ))}
                      {tripData.itinerary?.days?.length > 2 && (
                        <p className="text-sm text-gray-500 text-center italic">
                          ...and {tripData.itinerary.days.length - 2} more days of adventure!
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Comments Section (Future Phase) */}
            <Card className="mt-4">
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <MessageCircle className="h-5 w-5" />
                  Comments & Reactions
                </CardTitle>
                <CardDescription>
                  Coming soon - let friends comment and react to your trip
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-4 opacity-60">
                  {comments.map((comment) => (
                    <div key={comment.id} className="border-l-2 border-gray-200 pl-4">
                      <div className="flex items-start justify-between mb-1">
                        <p className="font-medium text-sm">{comment.author}</p>
                        <span className="text-xs text-gray-500">{comment.timestamp}</span>
                      </div>
                      <p className="text-sm text-gray-700 mb-2">{comment.text}</p>
                      <Button variant="ghost" size="sm" className="h-6 px-2">
                        <ThumbsUp className="h-3 w-3 mr-1" />
                        {comment.likes}
                      </Button>
                    </div>
                  ))}
                </div>
                <div className="mt-4 p-3 bg-gray-50 rounded-lg text-center">
                  <p className="text-sm text-gray-600">
                    Comments and voting will be available in the next update!
                  </p>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Share Options */}
          <div className="lg:col-span-1 space-y-4">
            <Card>
              <CardHeader>
                <CardTitle>Share Settings</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <Label htmlFor="private-toggle">Private Link</Label>
                    <p className="text-sm text-gray-600">Only people with link can view</p>
                  </div>
                  <Switch
                    id="private-toggle"
                    checked={isPrivate}
                    onCheckedChange={setIsPrivate}
                  />
                </div>
                
                <div className="flex items-center justify-between">
                  <div>
                    <Label htmlFor="prices-toggle">Show Prices</Label>
                    <p className="text-sm text-gray-600">Include budget information</p>
                  </div>
                  <Switch
                    id="prices-toggle"
                    checked={showPrices}
                    onCheckedChange={setShowPrices}
                  />
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Share Link</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex gap-2">
                  <Input value={shareLink} readOnly />
                  <Button variant="outline" size="sm" onClick={copyShareLink}>
                    <Copy className="h-4 w-4" />
                  </Button>
                </div>
                
                <div className="grid grid-cols-2 gap-2">
                  <Button variant="outline" size="sm" onClick={shareViaWhatsApp}>
                    <MessageCircle className="h-3 w-3 mr-1" />
                    WhatsApp
                  </Button>
                  <Button variant="outline" size="sm" onClick={shareViaEmail}>
                    <Share2 className="h-3 w-3 mr-1" />
                    Email
                  </Button>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>QR Code</CardTitle>
                <CardDescription>
                  Easy sharing for mobile devices
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="aspect-square bg-gray-100 rounded-lg flex items-center justify-center">
                  <div className="text-center text-gray-500">
                    <QrCode className="h-12 w-12 mx-auto mb-2" />
                    <p className="text-sm">QR Code</p>
                    <p className="text-xs">Scan to view trip</p>
                  </div>
                </div>
                <Button variant="outline" className="w-full mt-3" size="sm">
                  <Copy className="h-3 w-3 mr-1" />
                  Save QR Code
                </Button>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Share Stats</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex justify-between text-sm">
                  <span>Views</span>
                  <span className="font-medium">42</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span>Shares</span>
                  <span className="font-medium">7</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span>Reactions</span>
                  <span className="font-medium">{votes.thumbsUp + votes.thumbsDown}</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}