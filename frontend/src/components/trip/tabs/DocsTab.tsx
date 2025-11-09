/**
 * Docs Tab Component
 * Travel documents, requirements, and emergency information
 */

import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { FileText, Download, ExternalLink, AlertTriangle, Phone, MapPin } from 'lucide-react';
import { BookingModal } from '@/components/booking/BookingModal';
import { buildVisaUrl } from '@/utils/easemytripUrlBuilder';

interface DocsTabProps {
  tripId?: string;
  destination?: string;
}

export function DocsTab({ tripId, destination = 'Paris, France' }: DocsTabProps) {
  const [isVisaModalOpen, setIsVisaModalOpen] = useState(false);

  return (
    <div className="space-y-3 sm:space-y-4 md:space-y-6">
      {/* Passport & Visa Requirements */}
      <Card>
        <CardHeader className="p-4 sm:p-6">
          <CardTitle className="flex items-center gap-2 text-base sm:text-lg">
            <FileText className="h-4 w-4 sm:h-5 sm:w-5" />
            Passport & Visa Requirements
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 sm:space-y-4 p-4 sm:p-6 pt-0">
          <div className="flex items-start gap-2 sm:gap-3 p-3 sm:p-4 bg-muted/50 rounded-lg">
            <AlertTriangle className="h-4 w-4 sm:h-5 sm:w-5 text-warning flex-shrink-0 mt-0.5" />
            <div className="flex-1 min-w-0">
              <p className="font-medium text-sm sm:text-base">Passport Validity</p>
              <p className="text-xs sm:text-sm text-muted-foreground mt-1">
                Your passport must be valid for at least 6 months beyond your planned departure date from {destination}.
              </p>
            </div>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between p-2.5 sm:p-3 border rounded-lg">
              <div className="min-w-0 flex-1">
                <p className="font-medium text-sm sm:text-base">Visa Required</p>
                <p className="text-xs sm:text-sm text-muted-foreground">For US citizens</p>
              </div>
              <Badge variant="success" className="text-xs flex-shrink-0 ml-2">Not Required</Badge>
            </div>

            <div className="flex items-center justify-between p-2.5 sm:p-3 border rounded-lg">
              <div className="min-w-0 flex-1">
                <p className="font-medium text-sm sm:text-base">Tourist Stay Duration</p>
                <p className="text-xs sm:text-sm text-muted-foreground">Maximum allowed</p>
              </div>
              <Badge variant="outline" className="text-xs flex-shrink-0 ml-2">90 days</Badge>
            </div>

            <div className="flex items-center justify-between p-2.5 sm:p-3 border rounded-lg">
              <div className="min-w-0 flex-1">
                <p className="font-medium text-sm sm:text-base">Entry Requirements</p>
                <p className="text-xs sm:text-sm text-muted-foreground">COVID-19 related</p>
              </div>
              <Badge variant="secondary" className="text-xs flex-shrink-0 ml-2">Check Latest</Badge>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row gap-2">
            <Button variant="outline" className="flex-1 min-h-[44px] text-sm">
              <ExternalLink className="h-3 w-3 sm:h-4 sm:w-4 mr-2" />
              Check Official Travel Advisory
            </Button>
            <Button 
              onClick={() => setIsVisaModalOpen(true)}
              className="flex-1 min-h-[44px] text-sm"
            >
              <FileText className="h-3 w-3 sm:h-4 sm:w-4 mr-2" />
              Book Visa Assistance
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Booking Confirmations */}
      <Card>
        <CardHeader className="p-4 sm:p-6">
          <CardTitle className="flex items-center gap-2 text-base sm:text-lg">
            <FileText className="h-4 w-4 sm:h-5 sm:w-5" />
            Booking Confirmations
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 sm:space-y-3 p-4 sm:p-6 pt-0">
          <div className="flex items-center justify-between p-2.5 sm:p-3 border rounded-lg hover:bg-muted/50 transition-colors">
            <div className="flex items-center gap-2 sm:gap-3 min-w-0 flex-1">
              <div className="h-8 w-8 sm:h-10 sm:w-10 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                <FileText className="h-4 w-4 sm:h-5 sm:w-5 text-primary" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="font-medium text-sm sm:text-base truncate">Flight Confirmation</p>
                <p className="text-xs sm:text-sm text-muted-foreground truncate">EMT123456789</p>
              </div>
            </div>
            <Button variant="ghost" size="icon" className="h-8 w-8 sm:h-10 sm:w-10 flex-shrink-0">
              <Download className="h-3 w-3 sm:h-4 sm:w-4" />
            </Button>
          </div>

          <div className="flex items-center justify-between p-2.5 sm:p-3 border rounded-lg hover:bg-muted/50 transition-colors">
            <div className="flex items-center gap-2 sm:gap-3 min-w-0 flex-1">
              <div className="h-8 w-8 sm:h-10 sm:w-10 rounded-full bg-secondary/10 flex items-center justify-center flex-shrink-0">
                <FileText className="h-4 w-4 sm:h-5 sm:w-5 text-secondary" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="font-medium text-sm sm:text-base truncate">Hotel Reservation</p>
                <p className="text-xs sm:text-sm text-muted-foreground truncate">Grand Hotel Paris</p>
              </div>
            </div>
            <Button variant="ghost" size="icon" className="h-8 w-8 sm:h-10 sm:w-10 flex-shrink-0">
              <Download className="h-3 w-3 sm:h-4 sm:w-4" />
            </Button>
          </div>

          <div className="flex items-center justify-between p-2.5 sm:p-3 border rounded-lg hover:bg-muted/50 transition-colors">
            <div className="flex items-center gap-2 sm:gap-3 min-w-0 flex-1">
              <div className="h-8 w-8 sm:h-10 sm:w-10 rounded-full bg-success/10 flex items-center justify-center flex-shrink-0">
                <FileText className="h-4 w-4 sm:h-5 sm:w-5 text-success" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="font-medium text-sm sm:text-base truncate">Travel Insurance</p>
                <p className="text-xs sm:text-sm text-muted-foreground truncate">Policy #INS789012</p>
              </div>
            </div>
            <Button variant="ghost" size="icon" className="h-8 w-8 sm:h-10 sm:w-10 flex-shrink-0">
              <Download className="h-3 w-3 sm:h-4 sm:w-4" />
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Travel Insurance */}
      <Card>
        <CardHeader className="p-4 sm:p-6">
          <CardTitle className="flex items-center gap-2 text-base sm:text-lg">
            <FileText className="h-4 w-4 sm:h-5 sm:w-5" />
            Travel Insurance
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 sm:space-y-4 p-4 sm:p-6 pt-0">
          <div className="grid grid-cols-2 gap-3 sm:gap-4">
            <div>
              <p className="text-xs sm:text-sm text-muted-foreground">Provider</p>
              <p className="font-medium text-sm sm:text-base truncate">TravelGuard Insurance</p>
            </div>
            <div>
              <p className="text-xs sm:text-sm text-muted-foreground">Policy Number</p>
              <p className="font-medium text-sm sm:text-base truncate">INS789012345</p>
            </div>
            <div>
              <p className="text-xs sm:text-sm text-muted-foreground">Coverage Amount</p>
              <p className="font-medium text-sm sm:text-base">$100,000</p>
            </div>
            <div>
              <p className="text-xs sm:text-sm text-muted-foreground">Valid Until</p>
              <p className="font-medium text-sm sm:text-base">Dec 31, 2025</p>
            </div>
          </div>

          <div className="p-3 sm:p-4 bg-muted/50 rounded-lg">
            <p className="text-xs sm:text-sm font-medium mb-2">Coverage Includes:</p>
            <ul className="text-xs sm:text-sm text-muted-foreground space-y-1">
              <li>• Medical emergencies</li>
              <li>• Trip cancellation/interruption</li>
              <li>• Lost/delayed baggage</li>
              <li>• Emergency evacuation</li>
            </ul>
          </div>

          <Button variant="outline" className="w-full min-h-[44px] text-sm">
            <Download className="h-3 w-3 sm:h-4 sm:w-4 mr-2" />
            Download Policy Document
          </Button>
        </CardContent>
      </Card>

      {/* Emergency Contacts */}
      <Card>
        <CardHeader className="p-4 sm:p-6">
          <CardTitle className="flex items-center gap-2 text-base sm:text-lg">
            <Phone className="h-4 w-4 sm:h-5 sm:w-5" />
            Emergency Contacts
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 sm:space-y-3 p-4 sm:p-6 pt-0">
          <div className="p-3 sm:p-4 border rounded-lg space-y-1.5 sm:space-y-2">
            <div className="flex items-center gap-2">
              <Phone className="h-3 w-3 sm:h-4 sm:w-4 text-error" />
              <p className="font-medium text-sm sm:text-base">Local Emergency Services</p>
            </div>
            <p className="text-xl sm:text-2xl font-bold text-error">112</p>
            <p className="text-xs sm:text-sm text-muted-foreground">Police, Fire, Ambulance</p>
          </div>

          <div className="p-3 sm:p-4 border rounded-lg space-y-1.5 sm:space-y-2">
            <div className="flex items-center gap-2">
              <MapPin className="h-3 w-3 sm:h-4 sm:w-4 text-primary" />
              <p className="font-medium text-sm sm:text-base">US Embassy in Paris</p>
            </div>
            <p className="text-xs sm:text-sm">2 Avenue Gabriel, 75008 Paris</p>
            <p className="text-xs sm:text-sm font-medium">+33 1 43 12 22 22</p>
            <Button variant="link" className="h-auto p-0 text-xs sm:text-sm">
              <ExternalLink className="h-3 w-3 mr-1" />
              View on Map
            </Button>
          </div>

          <div className="p-3 sm:p-4 border rounded-lg space-y-1.5 sm:space-y-2">
            <div className="flex items-center gap-2">
              <Phone className="h-3 w-3 sm:h-4 sm:w-4 text-secondary" />
              <p className="font-medium text-sm sm:text-base">Travel Insurance 24/7 Hotline</p>
            </div>
            <p className="text-xs sm:text-sm font-medium">+1 800 123 4567</p>
            <p className="text-xs sm:text-sm text-muted-foreground">Available 24/7 for emergencies</p>
          </div>

          <div className="p-3 sm:p-4 border rounded-lg space-y-1.5 sm:space-y-2">
            <div className="flex items-center gap-2">
              <Phone className="h-3 w-3 sm:h-4 sm:w-4 text-success" />
              <p className="font-medium text-sm sm:text-base">Personal Emergency Contact</p>
            </div>
            <p className="text-xs sm:text-sm">John Doe (Family)</p>
            <p className="text-xs sm:text-sm font-medium">+1 555 123 4567</p>
          </div>
        </CardContent>
      </Card>

      {/* Important Notes */}
      <Card className="border-warning bg-warning/5">
        <CardHeader className="p-4 sm:p-6">
          <CardTitle className="text-sm sm:text-base flex items-center gap-2">
            <AlertTriangle className="h-4 w-4 sm:h-5 sm:w-5 text-warning" />
            Important Reminders
          </CardTitle>
        </CardHeader>
        <CardContent className="p-4 sm:p-6 pt-0">
          <ul className="space-y-1.5 sm:space-y-2 text-xs sm:text-sm text-muted-foreground">
            <li>• Make copies of all important documents and store them separately</li>
            <li>• Register your trip with your embassy's travel registration program</li>
            <li>• Keep digital copies of documents in cloud storage</li>
            <li>• Share your itinerary with family or friends</li>
            <li>• Check travel advisories before departure</li>
          </ul>
        </CardContent>
      </Card>

      {/* Visa Booking Modal */}
      <BookingModal
        isOpen={isVisaModalOpen}
        onClose={() => setIsVisaModalOpen(false)}
        bookingType="activity"
        itemName="Visa Assistance"
        providerUrl={buildVisaUrl()}
      />
    </div>
  );
}
