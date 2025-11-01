/**
 * Booking Service
 * Handles real booking data from backend API
 */

import { apiClient } from './apiClient';

export interface Booking {
  id: string;
  itineraryId: string;
  nodeId: string;
  type: 'hotel' | 'flight' | 'activity' | 'transport';
  provider: string;
  status: 'pending' | 'confirmed' | 'cancelled' | 'failed';
  confirmationNumber?: string;
  bookingDetails: any;
  cost: {
    amount: number;
    currency: string;
  };
  createdAt: string;
  updatedAt: string;
}

export interface BookingSearchRequest {
  type: 'hotel' | 'flight' | 'activity';
  location?: string;
  checkIn?: string;
  checkOut?: string;
  guests?: number;
  filters?: Record<string, any>;
}

export interface BookingSearchResult {
  id: string;
  provider: string;
  name: string;
  description?: string;
  price: {
    amount: number;
    currency: string;
  };
  rating?: number;
  images?: string[];
  availability: boolean;
  details: any;
}

class BookingService {
  /**
   * Get all bookings for an itinerary
   */
  async getBookings(itineraryId: string): Promise<Booking[]> {
    try {
      const response = await apiClient.get(`/bookings/itinerary/${itineraryId}`);
      return response.data || [];
    } catch (error) {
      console.error('[Booking] Failed to fetch bookings:', error);
      return [];
    }
  }

  /**
   * Get a specific booking
   */
  async getBooking(bookingId: string): Promise<Booking | null> {
    try {
      const response = await apiClient.get(`/bookings/${bookingId}`);
      return response.data;
    } catch (error) {
      console.error('[Booking] Failed to fetch booking:', error);
      return null;
    }
  }

  /**
   * Search for available bookings
   */
  async searchBookings(request: BookingSearchRequest): Promise<BookingSearchResult[]> {
    try {
      const endpoint = `/booking/${request.type}/search`;
      const response = await apiClient.post(endpoint, request);
      return response.data?.results || [];
    } catch (error) {
      console.error('[Booking] Search failed:', error);
      return [];
    }
  }

  /**
   * Create a new booking
   */
  async createBooking(data: {
    itineraryId: string;
    nodeId: string;
    provider: string;
    type: string;
    bookingDetails: any;
  }): Promise<{ success: boolean; booking?: Booking; error?: string }> {
    try {
      const response = await apiClient.post('/bookings', data);
      return { success: true, booking: response.data };
    } catch (error: any) {
      console.error('[Booking] Create failed:', error);
      return { 
        success: false, 
        error: error.response?.data?.message || 'Failed to create booking' 
      };
    }
  }

  /**
   * Confirm a booking with payment
   */
  async confirmBooking(bookingId: string, paymentDetails: any): Promise<{
    success: boolean;
    confirmationNumber?: string;
    error?: string;
  }> {
    try {
      const response = await apiClient.post(`/bookings/${bookingId}/confirm`, paymentDetails);
      return {
        success: true,
        confirmationNumber: response.data.confirmationNumber,
      };
    } catch (error: any) {
      console.error('[Booking] Confirm failed:', error);
      return {
        success: false,
        error: error.response?.data?.message || 'Failed to confirm booking',
      };
    }
  }

  /**
   * Cancel a booking
   */
  async cancelBooking(bookingId: string, reason?: string): Promise<{
    success: boolean;
    refundAmount?: number;
    error?: string;
  }> {
    try {
      const response = await apiClient.post(`/bookings/${bookingId}/cancel`, { reason });
      return {
        success: true,
        refundAmount: response.data.refundAmount,
      };
    } catch (error: any) {
      console.error('[Booking] Cancel failed:', error);
      return {
        success: false,
        error: error.response?.data?.message || 'Failed to cancel booking',
      };
    }
  }

  /**
   * Get booking status
   */
  async getBookingStatus(bookingId: string): Promise<{
    status: string;
    details?: any;
  }> {
    try {
      const response = await apiClient.get(`/bookings/${bookingId}/status`);
      return response.data;
    } catch (error) {
      console.error('[Booking] Status check failed:', error);
      return { status: 'unknown' };
    }
  }

  /**
   * Get bookings by provider
   */
  async getBookingsByProvider(itineraryId: string, provider: string): Promise<Booking[]> {
    try {
      const response = await apiClient.get(
        `/bookings/itinerary/${itineraryId}/provider/${provider}`
      );
      return response.data || [];
    } catch (error) {
      console.error('[Booking] Failed to fetch provider bookings:', error);
      return [];
    }
  }

  /**
   * Get bookings by type
   */
  async getBookingsByType(itineraryId: string, type: string): Promise<Booking[]> {
    try {
      const response = await apiClient.get(
        `/bookings/itinerary/${itineraryId}/type/${type}`
      );
      return response.data || [];
    } catch (error) {
      console.error('[Booking] Failed to fetch type bookings:', error);
      return [];
    }
  }
}

export const bookingService = new BookingService();
