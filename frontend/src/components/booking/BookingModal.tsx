import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { X, Calendar, DollarSign, Users, CreditCard } from 'lucide-react';
import { apiClient } from '../../services/apiClient';
import { formatCurrency } from '../../utils/formatters';

interface BookingModalProps {
  nodeId: string;
  itineraryId: string;
  nodeDetails: {
    title: string;
    type: string;
    cost?: { amount: number; currency: string };
    location?: { name: string };
  };
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: (bookingRef: string) => void;
}

export function BookingModal({
  nodeId,
  itineraryId,
  nodeDetails,
  isOpen,
  onClose,
  onSuccess,
}: BookingModalProps) {
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    guests: 1,
    specialRequests: '',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await fetch(`${(apiClient as any).baseUrl}/book`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${(apiClient as any).authToken || ''}`,
        },
        body: JSON.stringify({
          itineraryId,
          nodeId,
          ...formData,
        }),
      });

      const data = await response.json();
      onSuccess?.(data.bookingReference);
      onClose();
    } catch (error) {
      
      alert('Booking failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <Card className="w-full max-w-lg">
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Book {nodeDetails.title}</CardTitle>
            <button onClick={onClose}>
              <X className="w-5 h-5" />
            </button>
          </div>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="bg-gray-50 p-4 rounded-lg space-y-2">
              <div className="flex items-center gap-2">
                <Calendar className="w-4 h-4" />
                <span className="font-medium">{nodeDetails.type}</span>
              </div>
              {nodeDetails.location && (
                <p className="text-sm text-gray-600">{nodeDetails.location.name}</p>
              )}
              {nodeDetails.cost && (
                <div className="flex items-center gap-2">
                  <DollarSign className="w-4 h-4" />
                  <span className="font-semibold">
                    {formatCurrency(nodeDetails.cost.amount, nodeDetails.cost.currency)}
                  </span>
                </div>
              )}
            </div>

            <div>
              <Label htmlFor="name">Full Name</Label>
              <Input
                id="name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                required
              />
            </div>

            <div>
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                required
              />
            </div>

            <div>
              <Label htmlFor="phone">Phone</Label>
              <Input
                id="phone"
                type="tel"
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                required
              />
            </div>

            <div>
              <Label htmlFor="guests">Number of Guests</Label>
              <Input
                id="guests"
                type="number"
                min="1"
                value={formData.guests}
                onChange={(e) => setFormData({ ...formData, guests: parseInt(e.target.value) })}
                required
              />
            </div>

            <div>
              <Label htmlFor="requests">Special Requests (Optional)</Label>
              <textarea
                id="requests"
                value={formData.specialRequests}
                onChange={(e) => setFormData({ ...formData, specialRequests: e.target.value })}
                className="w-full p-2 border rounded-md"
                rows={3}
              />
            </div>

            <div className="flex justify-end gap-2 pt-4">
              <Button type="button" variant="outline" onClick={onClose}>
                Cancel
              </Button>
              <Button type="submit" disabled={loading}>
                <CreditCard className="w-4 h-4 mr-2" />
                {loading ? 'Booking...' : 'Confirm Booking'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

