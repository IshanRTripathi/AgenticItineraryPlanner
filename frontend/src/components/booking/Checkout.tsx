import React, { useState } from 'react';
import { Button } from '../ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { RadioGroup, RadioGroupItem } from '../ui/radio-group';
import { Checkbox } from '../ui/checkbox';
import { Badge } from '../ui/badge';
import { Separator } from '../ui/separator';
import { Alert, AlertDescription } from '../ui/alert';
import { Progress } from '../ui/progress';
import { 
  ArrowLeft, 
  CreditCard, 
  Smartphone,
  Shield,
  Lock,
  CheckCircle,
  Clock,
  AlertTriangle
} from 'lucide-react';
import { TripData } from '../../types/TripData';

interface CheckoutProps {
  tripData: TripData;
  onSuccess: (bookingData: any) => void;
  onBack: () => void;
}

interface PaymentStep {
  id: string;
  title: string;
  status: 'pending' | 'processing' | 'completed' | 'failed';
}

export function Checkout({ tripData, onSuccess, onBack }: CheckoutProps) {
  const [paymentMethod, setPaymentMethod] = useState('upi');
  const [processing, setProcessing] = useState(false);
  const [currentStep, setCurrentStep] = useState(0);
  const [formData, setFormData] = useState({
    email: '',
    phone: '',
    upiId: '',
    cardNumber: '',
    expiryDate: '',
    cvv: '',
    cardholderName: '',
    agreeTerms: false,
    saveDetails: false
  });

  const paymentSteps: PaymentStep[] = [
    { id: 'validate', title: 'Validating payment details', status: 'pending' },
    { id: 'process', title: 'Processing payment', status: 'pending' },
    { id: 'confirm', title: 'Confirming booking', status: 'pending' },
    { id: 'tickets', title: 'Generating tickets', status: 'pending' }
  ];

  const [steps, setSteps] = useState(paymentSteps);

  const calculateTotal = () => {
    return Math.floor((tripData.budget?.total || tripData.budget || 0) * 0.8); // Mock total
  };

  const updateFormData = (updates: Partial<typeof formData>) => {
    setFormData(prev => ({ ...prev, ...updates }));
  };

  const validateForm = () => {
    if (!formData.email || !formData.phone || !formData.agreeTerms) {
      return false;
    }

    if (paymentMethod === 'upi' && !formData.upiId) {
      return false;
    }

    if (paymentMethod === 'card' && (!formData.cardNumber || !formData.expiryDate || !formData.cvv || !formData.cardholderName)) {
      return false;
    }

    return true;
  };

  const processPayment = async () => {
    if (!validateForm()) {
      alert('Please fill in all required fields');
      return;
    }

    setProcessing(true);
    
    // Simulate payment processing steps
    for (let i = 0; i < steps.length; i++) {
      setCurrentStep(i);
      setSteps(prev => prev.map((step, index) => ({
        ...step,
        status: index === i ? 'processing' : index < i ? 'completed' : 'pending'
      })));
      
      // Simulate processing time
      await new Promise(resolve => setTimeout(resolve, 1500 + Math.random() * 1000));
      
      setSteps(prev => prev.map((step, index) => ({
        ...step,
        status: index <= i ? 'completed' : 'pending'
      })));
    }

    // Generate mock booking data
    const bookingData = {
      paymentId: `PAY_${Date.now()}`,
      bookingReference: `BK${Date.now().toString().slice(-8)}`,
      pnr: `PNR${Math.random().toString(36).substr(2, 6).toUpperCase()}`,
      hotelConfirmation: `HTL${Math.random().toString(36).substr(2, 8).toUpperCase()}`,
      amount: calculateTotal(),
      paymentMethod,
      status: 'confirmed',
      bookedAt: new Date().toISOString()
    };

    onSuccess(bookingData);
  };

  if (processing) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <Card className="w-full max-w-md mx-4">
          <CardHeader className="text-center">
            <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <Lock className="h-8 w-8 text-blue-600" />
            </div>
            <CardTitle>Processing Your Payment</CardTitle>
            <CardDescription>
              Please don't close this window while we process your booking
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <Progress value={((currentStep + 1) / steps.length) * 100} />
            
            <div className="space-y-3">
              {steps.map((step, index) => (
                <div key={step.id} className="flex items-center gap-3">
                  <div className={`w-6 h-6 rounded-full flex items-center justify-center ${
                    step.status === 'completed' ? 'bg-green-500' :
                    step.status === 'processing' ? 'bg-blue-500' :
                    'bg-gray-300'
                  }`}>
                    {step.status === 'completed' ? (
                      <CheckCircle className="h-4 w-4 text-white" />
                    ) : step.status === 'processing' ? (
                      <Clock className="h-4 w-4 text-white animate-spin" />
                    ) : (
                      <span className="text-xs text-gray-600">{index + 1}</span>
                    )}
                  </div>
                  <span className={`text-sm ${
                    step.status === 'completed' ? 'text-green-600' :
                    step.status === 'processing' ? 'text-blue-600' :
                    'text-gray-500'
                  }`}>
                    {step.title}
                  </span>
                </div>
              ))}
            </div>

            <Alert>
              <Shield className="h-4 w-4" />
              <AlertDescription>
                Your payment is secured with 256-bit SSL encryption
              </AlertDescription>
            </Alert>
          </CardContent>
        </Card>
      </div>
    );
  }

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
                <h1 className="text-2xl">Secure Checkout</h1>
                <p className="text-gray-600">Complete your booking for {tripData.destination}</p>
              </div>
            </div>
            
            <Badge variant="outline" className="flex items-center gap-2">
              <Shield className="h-3 w-3" />
              SSL Secured
            </Badge>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-4 py-6">
        <div className="grid lg:grid-cols-3 gap-6">
          {/* Payment Form */}
          <div className="lg:col-span-2 space-y-6">
            {/* Contact Information */}
            <Card>
              <CardHeader>
                <CardTitle>Contact Information</CardTitle>
                <CardDescription>
                  We'll send booking confirmations to these details
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label htmlFor="email">Email Address *</Label>
                    <Input
                      id="email"
                      type="email"
                      placeholder="your@email.com"
                      value={formData.email}
                      onChange={(e) => updateFormData({ email: e.target.value })}
                    />
                  </div>
                  <div>
                    <Label htmlFor="phone">Phone Number *</Label>
                    <Input
                      id="phone"
                      type="tel"
                      placeholder="+91 9876543210"
                      value={formData.phone}
                      onChange={(e) => updateFormData({ phone: e.target.value })}
                    />
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Payment Method */}
            <Card>
              <CardHeader>
                <CardTitle>Payment Method</CardTitle>
                <CardDescription>
                  Choose your preferred payment option
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <RadioGroup value={paymentMethod} onValueChange={setPaymentMethod}>
                  <div className="flex items-center space-x-2 p-4 border rounded-lg">
                    <RadioGroupItem value="upi" id="upi" />
                    <div className="flex items-center gap-3 flex-1">
                      <Smartphone className="h-5 w-5 text-blue-600" />
                      <div>
                        <Label htmlFor="upi" className="font-medium">UPI Payment</Label>
                        <p className="text-sm text-gray-600">Pay using any UPI app</p>
                      </div>
                    </div>
                    <Badge variant="outline" className="bg-green-50 text-green-700">
                      Instant
                    </Badge>
                  </div>
                  
                  <div className="flex items-center space-x-2 p-4 border rounded-lg">
                    <RadioGroupItem value="card" id="card" />
                    <div className="flex items-center gap-3 flex-1">
                      <CreditCard className="h-5 w-5 text-purple-600" />
                      <div>
                        <Label htmlFor="card" className="font-medium">Credit/Debit Card</Label>
                        <p className="text-sm text-gray-600">Visa, Mastercard, RuPay accepted</p>
                      </div>
                    </div>
                  </div>
                </RadioGroup>

                {/* UPI Details */}
                {paymentMethod === 'upi' && (
                  <div className="space-y-3 pt-4 border-t">
                    <Label htmlFor="upi-id">UPI ID *</Label>
                    <Input
                      id="upi-id"
                      placeholder="yourname@paytm"
                      value={formData.upiId}
                      onChange={(e) => updateFormData({ upiId: e.target.value })}
                    />
                    <p className="text-sm text-gray-600">
                      Enter your UPI ID or we'll show a QR code to scan
                    </p>
                  </div>
                )}

                {/* Card Details */}
                {paymentMethod === 'card' && (
                  <div className="space-y-4 pt-4 border-t">
                    <div>
                      <Label htmlFor="cardholder">Cardholder Name *</Label>
                      <Input
                        id="cardholder"
                        placeholder="Name as on card"
                        value={formData.cardholderName}
                        onChange={(e) => updateFormData({ cardholderName: e.target.value })}
                      />
                    </div>
                    
                    <div>
                      <Label htmlFor="card-number">Card Number *</Label>
                      <Input
                        id="card-number"
                        placeholder="1234 5678 9012 3456"
                        value={formData.cardNumber}
                        onChange={(e) => updateFormData({ cardNumber: e.target.value })}
                      />
                    </div>
                    
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <Label htmlFor="expiry">Expiry Date *</Label>
                        <Input
                          id="expiry"
                          placeholder="MM/YY"
                          value={formData.expiryDate}
                          onChange={(e) => updateFormData({ expiryDate: e.target.value })}
                        />
                      </div>
                      <div>
                        <Label htmlFor="cvv">CVV *</Label>
                        <Input
                          id="cvv"
                          placeholder="123"
                          value={formData.cvv}
                          onChange={(e) => updateFormData({ cvv: e.target.value })}
                        />
                      </div>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Terms and Save Details */}
            <Card>
              <CardContent className="p-4 space-y-4">
                <div className="flex items-start space-x-2">
                  <Checkbox
                    id="save-details"
                    checked={formData.saveDetails}
                    onCheckedChange={(checked) => updateFormData({ saveDetails: checked as boolean })}
                  />
                  <div>
                    <Label htmlFor="save-details" className="text-sm font-medium">
                      Save payment details for future bookings
                    </Label>
                    <p className="text-xs text-gray-600">
                      Securely store for faster checkout next time
                    </p>
                  </div>
                </div>
                
                <div className="flex items-start space-x-2">
                  <Checkbox
                    id="agree-terms"
                    checked={formData.agreeTerms}
                    onCheckedChange={(checked) => updateFormData({ agreeTerms: checked as boolean })}
                  />
                  <div>
                    <Label htmlFor="agree-terms" className="text-sm font-medium">
                      I agree to the Terms & Conditions and Cancellation Policy *
                    </Label>
                    <p className="text-xs text-gray-600">
                      Free cancellation up to 24 hours before travel. Reprice protection included.
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Order Summary */}
          <div className="lg:col-span-1">
            <Card className="sticky top-4">
              <CardHeader>
                <CardTitle>Order Summary</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-3">
                  <div className="flex justify-between text-sm">
                    <span>Trip Cost</span>
                    <span>₹{Math.floor(calculateTotal() * 0.85).toLocaleString()}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span>Taxes & Fees</span>
                    <span>₹{Math.floor(calculateTotal() * 0.15).toLocaleString()}</span>
                  </div>
                  <Separator />
                  <div className="flex justify-between font-medium">
                    <span>Total Amount</span>
                    <span>₹{calculateTotal().toLocaleString()}</span>
                  </div>
                </div>

                <Alert>
                  <AlertTriangle className="h-4 w-4" />
                  <AlertDescription className="text-xs">
                    Prices may change after payment due to availability. 
                    You'll be notified of any changes before final confirmation.
                  </AlertDescription>
                </Alert>

                <Button 
                  className="w-full" 
                  size="lg"
                  onClick={processPayment}
                  disabled={!validateForm() || processing}
                >
                  <Lock className="h-4 w-4 mr-2" />
                  Pay ₹{calculateTotal().toLocaleString()}
                </Button>

                <div className="space-y-2 text-xs text-gray-500">
                  <div className="flex items-center gap-2">
                    <Shield className="h-3 w-3" />
                    <span>256-bit SSL encryption</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <CheckCircle className="h-3 w-3" />
                    <span>PCI DSS compliant</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Lock className="h-3 w-3" />
                    <span>Your data is secure</span>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}



