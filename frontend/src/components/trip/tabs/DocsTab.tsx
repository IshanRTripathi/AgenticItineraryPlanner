/**
 * Docs Tab Component
 * Travel documents, requirements, and emergency information
 */

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { FileText, Download, ExternalLink, AlertTriangle, Phone, MapPin } from 'lucide-react';

interface DocsTabProps {
  tripId?: string;
  destination?: string;
}

export function DocsTab({ tripId, destination = 'Paris, France' }: DocsTabProps) {
  return (
    <div className="space-y-6 p-6">
      {/* Passport & Visa Requirements */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5" />
            Passport & Visa Requirements
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-start gap-3 p-4 bg-muted/50 rounded-lg">
            <AlertTriangle className="h-5 w-5 text-warning flex-shrink-0 mt-0.5" />
            <div className="flex-1">
              <p className="font-medium">Passport Validity</p>
              <p className="text-sm text-muted-foreground mt-1">
                Your passport must be valid for at least 6 months beyond your planned departure date from {destination}.
              </p>
            </div>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between p-3 border rounded-lg">
              <div>
                <p className="font-medium">Visa Required</p>
                <p className="text-sm text-muted-foreground">For US citizens</p>
              </div>
              <Badge variant="success">Not Required</Badge>
            </div>

            <div className="flex items-center justify-between p-3 border rounded-lg">
              <div>
                <p className="font-medium">Tourist Stay Duration</p>
                <p className="text-sm text-muted-foreground">Maximum allowed</p>
              </div>
              <Badge variant="outline">90 days</Badge>
            </div>

            <div className="flex items-center justify-between p-3 border rounded-lg">
              <div>
                <p className="font-medium">Entry Requirements</p>
                <p className="text-sm text-muted-foreground">COVID-19 related</p>
              </div>
              <Badge variant="secondary">Check Latest</Badge>
            </div>
          </div>

          <Button variant="outline" className="w-full">
            <ExternalLink className="h-4 w-4 mr-2" />
            Check Official Travel Advisory
          </Button>
        </CardContent>
      </Card>

      {/* Booking Confirmations */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5" />
            Booking Confirmations
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="flex items-center justify-between p-3 border rounded-lg hover:bg-muted/50 transition-colors">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                <FileText className="h-5 w-5 text-primary" />
              </div>
              <div>
                <p className="font-medium">Flight Confirmation</p>
                <p className="text-sm text-muted-foreground">EMT123456789</p>
              </div>
            </div>
            <Button variant="ghost" size="icon">
              <Download className="h-4 w-4" />
            </Button>
          </div>

          <div className="flex items-center justify-between p-3 border rounded-lg hover:bg-muted/50 transition-colors">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-full bg-secondary/10 flex items-center justify-center">
                <FileText className="h-5 w-5 text-secondary" />
              </div>
              <div>
                <p className="font-medium">Hotel Reservation</p>
                <p className="text-sm text-muted-foreground">Grand Hotel Paris</p>
              </div>
            </div>
            <Button variant="ghost" size="icon">
              <Download className="h-4 w-4" />
            </Button>
          </div>

          <div className="flex items-center justify-between p-3 border rounded-lg hover:bg-muted/50 transition-colors">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-full bg-success/10 flex items-center justify-center">
                <FileText className="h-5 w-5 text-success" />
              </div>
              <div>
                <p className="font-medium">Travel Insurance</p>
                <p className="text-sm text-muted-foreground">Policy #INS789012</p>
              </div>
            </div>
            <Button variant="ghost" size="icon">
              <Download className="h-4 w-4" />
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Travel Insurance */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5" />
            Travel Insurance
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="text-sm text-muted-foreground">Provider</p>
              <p className="font-medium">TravelGuard Insurance</p>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Policy Number</p>
              <p className="font-medium">INS789012345</p>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Coverage Amount</p>
              <p className="font-medium">$100,000</p>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Valid Until</p>
              <p className="font-medium">Dec 31, 2025</p>
            </div>
          </div>

          <div className="p-3 bg-muted/50 rounded-lg">
            <p className="text-sm font-medium mb-2">Coverage Includes:</p>
            <ul className="text-sm text-muted-foreground space-y-1">
              <li>• Medical emergencies</li>
              <li>• Trip cancellation/interruption</li>
              <li>• Lost/delayed baggage</li>
              <li>• Emergency evacuation</li>
            </ul>
          </div>

          <Button variant="outline" className="w-full">
            <Download className="h-4 w-4 mr-2" />
            Download Policy Document
          </Button>
        </CardContent>
      </Card>

      {/* Emergency Contacts */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Phone className="h-5 w-5" />
            Emergency Contacts
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="p-4 border rounded-lg space-y-2">
            <div className="flex items-center gap-2">
              <Phone className="h-4 w-4 text-error" />
              <p className="font-medium">Local Emergency Services</p>
            </div>
            <p className="text-2xl font-bold text-error">112</p>
            <p className="text-sm text-muted-foreground">Police, Fire, Ambulance</p>
          </div>

          <div className="p-4 border rounded-lg space-y-2">
            <div className="flex items-center gap-2">
              <MapPin className="h-4 w-4 text-primary" />
              <p className="font-medium">US Embassy in Paris</p>
            </div>
            <p className="text-sm">2 Avenue Gabriel, 75008 Paris</p>
            <p className="text-sm font-medium">+33 1 43 12 22 22</p>
            <Button variant="link" className="h-auto p-0 text-sm">
              <ExternalLink className="h-3 w-3 mr-1" />
              View on Map
            </Button>
          </div>

          <div className="p-4 border rounded-lg space-y-2">
            <div className="flex items-center gap-2">
              <Phone className="h-4 w-4 text-secondary" />
              <p className="font-medium">Travel Insurance 24/7 Hotline</p>
            </div>
            <p className="text-sm font-medium">+1 800 123 4567</p>
            <p className="text-sm text-muted-foreground">Available 24/7 for emergencies</p>
          </div>

          <div className="p-4 border rounded-lg space-y-2">
            <div className="flex items-center gap-2">
              <Phone className="h-4 w-4 text-success" />
              <p className="font-medium">Personal Emergency Contact</p>
            </div>
            <p className="text-sm">John Doe (Family)</p>
            <p className="text-sm font-medium">+1 555 123 4567</p>
          </div>
        </CardContent>
      </Card>

      {/* Important Notes */}
      <Card className="border-warning bg-warning/5">
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-warning" />
            Important Reminders
          </CardTitle>
        </CardHeader>
        <CardContent>
          <ul className="space-y-2 text-sm text-muted-foreground">
            <li>• Make copies of all important documents and store them separately</li>
            <li>• Register your trip with your embassy's travel registration program</li>
            <li>• Keep digital copies of documents in cloud storage</li>
            <li>• Share your itinerary with family or friends</li>
            <li>• Check travel advisories before departure</li>
          </ul>
        </CardContent>
      </Card>
    </div>
  );
}
