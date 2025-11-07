/**
 * EaseMyTrip URL Builder
 * Constructs booking URLs for EaseMyTrip based on activity type and location
 */

import { CategorizedBooking } from './categorizeBookings';

interface EaseMyTripUrlParams {
  destination?: string;
  origin?: string;
  checkIn?: string;
  checkOut?: string;
  date?: string;
  adults?: number;
  children?: number;
  rooms?: number;
}

/**
 * Format date to YYYY-MM-DD for EaseMyTrip
 */
function formatDate(dateString: string): string {
  if (!dateString) return '';
  try {
    const date = new Date(dateString);
    return date.toISOString().split('T')[0];
  } catch {
    return '';
  }
}

/**
 * Extract city name from location string
 * e.g., "Paris, France" -> "Paris"
 */
function extractCityName(location: string): string {
  if (!location) return '';
  return location.split(',')[0].trim();
}

/**
 * Format date to DD/MM/YYYY for EaseMyTrip
 */
function formatDateEMT(dateString: string): string {
  if (!dateString) return '';
  try {
    const date = new Date(dateString);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    return `${day}/${month}/${year}`;
  } catch {
    return '';
  }
}

/**
 * Build EaseMyTrip hotel booking URL
 */
export function buildHotelUrl(params: EaseMyTripUrlParams): string {
  const {
    destination = '',
    checkIn = '',
    checkOut = '',
    adults = 2,
    children = 0,
    rooms = 1,
  } = params;

  // Use full destination with city and country for better search results
  const checkInDate = formatDateEMT(checkIn);
  const checkOutDate = formatDateEMT(checkOut);

  // Generate a simple timestamp-based ID (EaseMyTrip uses 'e' parameter)
  const timestamp = Date.now().toString().slice(-12);

  // EaseMyTrip actual hotel search URL format
  // The 'city' parameter accepts full location strings like "Paris, France"
  const baseUrl = 'https://www.easemytrip.com/hotels';
  const searchParams = new URLSearchParams({
    city: destination, // Full destination with country
    checkin: checkInDate,
    checkout: checkOutDate,
    rooms: rooms.toString(),
    adults: adults.toString(),
    children: children.toString(),
  });

  return `${baseUrl}?${searchParams.toString()}`;
}

/**
 * Build EaseMyTrip flight booking URL
 */
export function buildFlightUrl(params: EaseMyTripUrlParams): string {
  const {
    origin = '',
    destination = '',
    date = '',
    adults = 1,
    children = 0,
  } = params;

  const originCity = extractCityName(origin);
  const destCity = extractCityName(destination);
  const travelDate = formatDate(date);

  // EaseMyTrip flight search URL format
  const baseUrl = 'https://www.easemytrip.com/flights';
  const searchParams = new URLSearchParams({
    from: originCity,
    to: destCity,
    depart: travelDate,
    adults: adults.toString(),
    children: children.toString(),
    class: 'Economy',
  });

  return `${baseUrl}?${searchParams.toString()}`;
}

/**
 * Build EaseMyTrip train booking URL
 */
export function buildTrainUrl(params: EaseMyTripUrlParams): string {
  const {
    origin = '',
    destination = '',
    date = '',
  } = params;

  const originCity = extractCityName(origin);
  const destCity = extractCityName(destination);
  const travelDate = formatDate(date);

  // EaseMyTrip train search URL format
  const baseUrl = 'https://www.easemytrip.com/railways';
  const searchParams = new URLSearchParams({
    from: originCity,
    to: destCity,
    date: travelDate,
  });

  return `${baseUrl}?${searchParams.toString()}`;
}

/**
 * Build EaseMyTrip bus booking URL
 */
export function buildBusUrl(params: EaseMyTripUrlParams): string {
  const {
    origin = '',
    destination = '',
    date = '',
  } = params;

  const originCity = extractCityName(origin);
  const destCity = extractCityName(destination);
  const travelDate = formatDate(date);

  // EaseMyTrip bus search URL format
  const baseUrl = 'https://www.easemytrip.com/bus';
  const searchParams = new URLSearchParams({
    from: originCity,
    to: destCity,
    date: travelDate,
  });

  return `${baseUrl}?${searchParams.toString()}`;
}

/**
 * Build EaseMyTrip activities/things to do URL
 */
export function buildActivityUrl(params: EaseMyTripUrlParams): string {
  const { destination = '', date = '' } = params;

  const city = extractCityName(destination);
  const travelDate = formatDate(date);

  // EaseMyTrip activities URL format
  const baseUrl = 'https://www.easemytrip.com/activities';
  const searchParams = new URLSearchParams({
    city: city,
    date: travelDate,
  });

  return `${baseUrl}?${searchParams.toString()}`;
}

/**
 * Main function to build EaseMyTrip URL based on booking type
 */
export function buildEaseMyTripUrl(
  booking: CategorizedBooking,
  itinerary?: any
): string {
  // Extract destination from booking
  const destination = booking.dayLocation || booking.location?.address || '';

  // Get days array from nested structure
  const days = itinerary?.itinerary?.days || itinerary?.days || [];
  const day = days.find((d: any) => d.dayNumber === booking.dayNumber);
  const date = day?.date || '';

  // Extract trip-level data from various possible locations
  const party = itinerary?.party || itinerary?.travelers || {};
  const adults = party.adults || 2;
  const children = party.children || 0;
  const infants = party.infants || 0;
  const rooms = party.rooms || 1;

  // Get trip start and end dates
  const startDate = days[0]?.date || itinerary?.startDate || '';
  const endDate = days[days.length - 1]?.date || itinerary?.endDate || '';

  // Determine booking type and build appropriate URL
  // Check node type for more specific categorization
  const nodeType = booking.type.toLowerCase();

  // Transport types
  if (nodeType.includes('flight') || nodeType.includes('plane')) {
    const origin = getPreviousLocation(booking, itinerary) || destination;
    return buildFlightUrl({
      origin,
      destination,
      date,
      adults,
      children,
    });
  }

  if (nodeType.includes('train') || nodeType.includes('railway')) {
    const trainOrigin = getPreviousLocation(booking, itinerary) || destination;
    return buildTrainUrl({
      origin: trainOrigin,
      destination,
      date,
    });
  }

  if (nodeType.includes('bus')) {
    const busOrigin = getPreviousLocation(booking, itinerary) || destination;
    return buildBusUrl({
      origin: busOrigin,
      destination,
      date,
    });
  }

  // Category-based routing
  switch (booking.category) {
    case 'accommodation':
      // For hotels, we need check-in and check-out dates
      const checkIn = date;
      const checkOut = calculateCheckOutDate(date, itinerary);
      return buildHotelUrl({
        destination,
        checkIn,
        checkOut,
        adults,
        children,
        rooms,
      });

    case 'transport':
      // Generic transport - default to flight
      const origin = getPreviousLocation(booking, itinerary) || destination;
      return buildFlightUrl({
        origin,
        destination,
        date,
        adults,
        children,
      });

    case 'dining':
    case 'attractions':
    default:
      // For activities, dining, attractions - search in the destination city
      return buildActivityUrl({
        destination,
        date,
      });
  }
}

/**
 * Calculate check-out date (next day or end of trip)
 */
function calculateCheckOutDate(checkInDate: string, itinerary?: any): string {
  if (!checkInDate) return '';

  try {
    const checkIn = new Date(checkInDate);

    // Get days array from nested structure
    const days = itinerary?.itinerary?.days || itinerary?.days || [];

    // If we have itinerary end date, use that
    const endDate = days[days.length - 1]?.date || itinerary?.endDate;
    if (endDate) {
      return formatDate(endDate);
    }

    // Otherwise, default to next day
    const checkOut = new Date(checkIn);
    checkOut.setDate(checkOut.getDate() + 1);
    return formatDate(checkOut.toISOString());
  } catch {
    return '';
  }
}

/**
 * Get previous location for transport bookings
 * (origin city for flights/trains/buses)
 */
function getPreviousLocation(booking: CategorizedBooking, itinerary?: any): string {
  // Get days array from nested structure
  const days = itinerary?.itinerary?.days || itinerary?.days || [];
  if (!days || days.length === 0) return '';

  // Find the day before this booking
  const currentDayNumber = booking.dayNumber;
  if (currentDayNumber <= 1) {
    // First day - use home city or first destination
    return itinerary?.origin || itinerary?.destination || days[0]?.location || '';
  }

  // Get previous day's location
  const previousDay = days.find((d: any) => d.dayNumber === currentDayNumber - 1);
  return previousDay?.location || '';
}

/**
 * Get EaseMyTrip provider info
 */
export function getEaseMyTripProvider() {
  return {
    id: 'easemytrip',
    name: 'EaseMyTrip',
    logo: 'https://www.easemytrip.com/images/emt-logo.svg',
    rating: 4.3,
  };
}
