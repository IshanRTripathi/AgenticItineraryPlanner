import React from 'react';
import { Button } from '../ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Badge } from '../ui/badge';
import { Separator } from '../ui/separator';
import { 
  CheckCircle, 
  Download, 
  Calendar,
  Share2,
  Mail,
  MessageCircle,
  Plane,
  Building,
  MapPin,
  Clock,
  Users,
  Copy,
  Home
} from 'lucide-react';
import { TripData } from '../../types/TripData';

interface BookingConfirmationProps {
  tripData: TripData;
  onShare: () => void;
  onDashboard: () => void;
}

export function BookingConfirmation({ tripData, onShare, onDashboard }: BookingConfirmationProps) {
  const bookingData = tripData.bookingData;
  
  if (!bookingData) {
    return <div>Booking data not found</div>;
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  const formatTime = (dateString: string) => {
    return new Date(dateString).toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const generateCalendarEvent = () => {
    const startDate = new Date(tripData.dates.start);
    const endDate = new Date(tripData.dates.end);
    
    const event = {
      title: `Trip to ${tripData.destination || tripData.endLocation?.name || 'Unknown'}`,
      start: startDate.toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, ''),
      end: endDate.toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, ''),
      description: `Your amazing trip to ${tripData.destination || tripData.endLocation?.name || 'Unknown'}. Booking ref: ${bookingData.bookingReference}`
    };

    const calendarUrl = `data:text/calendar;charset=utf8,BEGIN:VCALENDAR
VERSION:2.0
BEGIN:VEVENT
DTSTART:${event.start}
DTEND:${event.end}
SUMMARY:${event.title}
DESCRIPTION:${event.description}
END:VEVENT
END:VCALENDAR`;

    const link = document.createElement('a');
    link.href = encodeURI(calendarUrl);
    link.download = `${tripData.destination}-trip.ics`;
    link.click();
  };

  const copyBookingRef = () => {
    navigator.clipboard.writeText(bookingData.bookingReference);
    // Could add a toast notification here
  };

  const shareViaWhatsApp = () => {
    const message = `🎉 I just booked an amazing trip to ${tripData.destination}! 
    
✈️ ${formatDate(tripData.dates.start)} - ${formatDate(tripData.dates.end)}
👥 ${tripData.partySize} travelers
📍 Booking ref: ${bookingData.bookingReference}

Can't wait to explore!`;

    const whatsappUrl = `https://wa.me/?text=${encodeURIComponent(message)}`;
    window.open(whatsappUrl, '_blank');
  };

  const checklist = [
    { item: 'Valid passport/ID for travel', required: true },
    { item: 'Travel insurance (recommended)', required: false },
    { item: 'Check flight check-in times', required: true },
    { item: 'Confirm hotel check-in details', required: true },
    { item: 'Download offline maps', required: false },
    { item: 'Inform bank of travel plans', required: false }
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-50 to-emerald-100">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Success Header */}
        <div className="text-center mb-8">
          <div className="w-20 h-20 bg-green-500 rounded-full flex items-center justify-center mx-auto mb-4">
            <CheckCircle className="h-12 w-12 text-white" />
          </div>
          <h1 className="text-3xl mb-2">Booking Confirmed!</h1>
          <p className="text-gray-600 text-lg">
            Your amazing trip to {tripData.destination} is all set
          </p>
        </div>

        <div className="grid lg:grid-cols-3 gap-6">
          {/* Booking Details */}
          <div className="lg:col-span-2 space-y-6">
            {/* Confirmation Numbers */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Plane className="h-5 w-5" />
                  Confirmation Details
                </CardTitle>
                <CardDescription>
                  Save these numbers for your trip
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="p-4 bg-blue-50 rounded-lg">
                    <div className="flex items-center justify-between mb-2">
                      <h4 className="font-medium">Flight PNR</h4>
                      <Button variant="ghost" size="sm" onClick={copyBookingRef}>
                        <Copy className="h-3 w-3" />
                      </Button>
                    </div>
                    <p className="text-2xl font-mono">{bookingData.pnr}</p>
                    <p className="text-sm text-gray-600">Use this for web check-in</p>
                  </div>
                  
                  <div className="p-4 bg-purple-50 rounded-lg">
                    <div className="flex items-center justify-between mb-2">
                      <h4 className="font-medium">Hotel Confirmation</h4>
                      <Button variant="ghost" size="sm" onClick={copyBookingRef}>
                        <Copy className="h-3 w-3" />
                      </Button>
                    </div>
                    <p className="text-2xl font-mono">{bookingData.hotelConfirmation}</p>
                    <p className="text-sm text-gray-600">Show this at check-in</p>
                  </div>
                </div>
                
                <div className="p-4 bg-gray-50 rounded-lg">
                  <div className="flex items-center justify-between">
                    <div>
                      <h4 className="font-medium">Booking Reference</h4>
                      <p className="text-lg font-mono">{bookingData.bookingReference}</p>
                    </div>
                    <Badge variant="outline" className="bg-green-50 text-green-700">
                      Confirmed
                    </Badge>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Trip Summary */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <MapPin className="h-5 w-5" />
                  Trip Summary
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="flex items-center gap-3">
                    <Calendar className="h-4 w-4 text-gray-400" />
                    <div>
                      <p className="font-medium">Departure</p>
                      <p className="text-sm text-gray-600">{formatDate(tripData.dates.start)}</p>
                    </div>
                  </div>
                  
                  <div className="flex items-center gap-3">
                    <Calendar className="h-4 w-4 text-gray-400" />
                    <div>
                      <p className="font-medium">Return</p>
                      <p className="text-sm text-gray-600">{formatDate(tripData.dates.end)}</p>
                    </div>
                  </div>
                  
                  <div className="flex items-center gap-3">
                    <Users className="h-4 w-4 text-gray-400" />
                    <div>
                      <p className="font-medium">Travelers</p>
                      <p className="text-sm text-gray-600">{tripData.partySize} {tripData.partySize === 1 ? 'person' : 'people'}</p>
                    </div>
                  </div>
                  
                  <div className="flex items-center gap-3">
                    <Building className="h-4 w-4 text-gray-400" />
                    <div>
                      <p className="font-medium">Accommodation</p>
                      <p className="text-sm text-gray-600">{tripData.stayType}</p>
                    </div>
                  </div>
                </div>

                <Separator />

                <div>
                  <h4 className="font-medium mb-2">Your Interests</h4>
                  <div className="flex flex-wrap gap-2">
                    {tripData.themes.map((theme) => (
                      <Badge key={theme} variant="outline">
                        {theme}
                      </Badge>
                    ))}
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Pre-Trip Checklist */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Clock className="h-5 w-5" />
                  Pre-Trip Checklist
                </CardTitle>
                <CardDescription>
                  Things to do before your trip
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {checklist.map((item, index) => (
                    <div key={index} className="flex items-center gap-3">
                      <input 
                        type="checkbox" 
                        className="rounded"
                        id={`checklist-${index}`}
                      />
                      <label 
                        htmlFor={`checklist-${index}`}
                        className="text-sm flex-1"
                      >
                        {item.item}
                      </label>
                      {item.required && (
                        <Badge variant="outline" className="text-xs">
                          Required
                        </Badge>
                      )}
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Quick Actions */}
          <div className="lg:col-span-1 space-y-4">
            <Card>
              <CardHeader>
                <CardTitle>Quick Actions</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <Button className="w-full" variant="outline" onClick={() => {}}>
                  <Download className="h-4 w-4 mr-2" />
                  Download Tickets
                </Button>
                
                <Button className="w-full" variant="outline" onClick={() => {}}>
                  <Download className="h-4 w-4 mr-2" />
                  Download Invoice
                </Button>
                
                <Button className="w-full" variant="outline" onClick={generateCalendarEvent}>
                  <Calendar className="h-4 w-4 mr-2" />
                  Add to Calendar
                </Button>
                
                <Button className="w-full" variant="outline" onClick={onShare}>
                  <Share2 className="h-4 w-4 mr-2" />
                  Share Trip
                </Button>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Share Your Excitement</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <Button className="w-full" variant="outline" onClick={shareViaWhatsApp}>
                  <MessageCircle className="h-4 w-4 mr-2" />
                  Share via WhatsApp
                </Button>
                
                <Button className="w-full" variant="outline" onClick={() => {}}>
                  <Mail className="h-4 w-4 mr-2" />
                  Email to Friends
                </Button>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Payment Details</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex justify-between text-sm">
                  <span>Total Paid</span>
                  <span className="font-medium">₹{bookingData.amount.toLocaleString()}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span>Payment Method</span>
                  <span className="capitalize">{bookingData.paymentMethod}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span>Payment ID</span>
                  <span className="font-mono text-xs">{bookingData.paymentId}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span>Date</span>
                  <span>{new Date(bookingData.bookedAt).toLocaleDateString()}</span>
                </div>
              </CardContent>
            </Card>

            <Button className="w-full" onClick={onDashboard}>
              <Home className="h-4 w-4 mr-2" />
              Go to Dashboard
            </Button>
          </div>
        </div>

        {/* Support Notice */}
        <Card className="mt-8">
          <CardContent className="p-4">
            <div className="text-center">
              <h4 className="font-medium mb-2">Need Help?</h4>
              <p className="text-sm text-gray-600 mb-3">
                Our support team is available 24/7 to assist you with your trip
              </p>
              <div className="flex justify-center gap-4">
                <Button variant="outline" size="sm">
                  <MessageCircle className="h-3 w-3 mr-1" />
                  Chat Support
                </Button>
                <Button variant="outline" size="sm">
                  <Mail className="h-3 w-3 mr-1" />
                  Email Support
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}


