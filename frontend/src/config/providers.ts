/**
 * Provider Configuration
 * Defines all booking providers with their URLs and settings
 */

export interface Provider {
  id: string;
  name: string;
  logo: string;
  urlTemplate: string;
  verticals: ('flight' | 'hotel' | 'activity' | 'train' | 'bus')[];
  active: boolean;
  rating?: number;
}

export const providers: Provider[] = [
  // Hotels
  {
    id: 'booking-com',
    name: 'Booking.com',
    logo: '/assets/providers/booking.png',
    urlTemplate: 'https://www.booking.com/searchresults.html?ss={destination}&checkin={checkIn}&checkout={checkOut}&group_adults={adults}&group_children={children}',
    verticals: ['hotel'],
    active: true,
    rating: 4.5,
  },
  {
    id: 'expedia',
    name: 'Expedia',
    logo: '/assets/providers/expedia.png',
    urlTemplate: 'https://www.expedia.com/Hotel-Search?destination={destination}&startDate={checkIn}&endDate={checkOut}&rooms=1&adults={adults}',
    verticals: ['hotel', 'flight'],
    active: true,
    rating: 4.3,
  },
  {
    id: 'airbnb',
    name: 'Airbnb',
    logo: '/assets/providers/airbnb.png',
    urlTemplate: 'https://www.airbnb.com/s/{destination}/homes?checkin={checkIn}&checkout={checkOut}&adults={adults}&children={children}',
    verticals: ['hotel'],
    active: true,
    rating: 4.6,
  },
  {
    id: 'agoda',
    name: 'Agoda',
    logo: '/assets/providers/agoda.png',
    urlTemplate: 'https://www.agoda.com/search?city={destination}&checkIn={checkIn}&checkOut={checkOut}&rooms=1&adults={adults}&children={children}',
    verticals: ['hotel'],
    active: true,
    rating: 4.4,
  },
  {
    id: 'hotels-com',
    name: 'Hotels.com',
    logo: '/assets/providers/hotels.png',
    urlTemplate: 'https://www.hotels.com/search.do?destination={destination}&startDate={checkIn}&endDate={checkOut}&rooms=1&adults={adults}',
    verticals: ['hotel'],
    active: true,
    rating: 4.2,
  },
  {
    id: 'vio-com',
    name: 'Vio.com',
    logo: '/assets/providers/vio.png',
    urlTemplate: 'https://www.vio.com/hotels?location={destination}&checkin={checkIn}&checkout={checkOut}&adults={adults}',
    verticals: ['hotel'],
    active: true,
    rating: 4.1,
  },
  {
    id: 'trip-com',
    name: 'Trip.com',
    logo: '/assets/providers/trip.png',
    urlTemplate: 'https://www.trip.com/hotels?locale=en-US&curr=USD&city={destination}&checkin={checkIn}&checkout={checkOut}&adult={adults}',
    verticals: ['hotel', 'flight'],
    active: true,
    rating: 4.3,
  },
  {
    id: 'hostelworld',
    name: 'Hostelworld',
    logo: '/assets/providers/hostelworld.png',
    urlTemplate: 'https://www.hostelworld.com/findabed.php?search_keywords={destination}&date_from={checkIn}&date_to={checkOut}&number_of_guests={adults}',
    verticals: ['hotel'],
    active: true,
    rating: 4.0,
  },

  // Transportation
  {
    id: 'railyatra',
    name: 'RailYatra',
    logo: '/assets/providers/railyatra.png',
    urlTemplate: 'https://www.railyatra.com/train-ticket/trains-between-stations?from_code={origin}&to_code={destination}&date={date}',
    verticals: ['train'],
    active: true,
    rating: 4.2,
  },
  {
    id: 'redbus',
    name: 'RedBus',
    logo: '/assets/providers/redbus.png',
    urlTemplate: 'https://www.redbus.in/bus-tickets/{origin}-to-{destination}?fromCityName={origin}&toCityName={destination}&onward={date}',
    verticals: ['bus'],
    active: true,
    rating: 4.4,
  },

  // Flights
  {
    id: 'skyscanner',
    name: 'Skyscanner',
    logo: '/assets/providers/skyscanner.png',
    urlTemplate: 'https://www.skyscanner.com/transport/flights/{origin}/{destination}/{date}/?adults={adults}&children={children}',
    verticals: ['flight'],
    active: true,
    rating: 4.5,
  },
  {
    id: 'kayak',
    name: 'Kayak',
    logo: '/assets/providers/kayak.png',
    urlTemplate: 'https://www.kayak.com/flights/{origin}-{destination}/{date}?sort=bestflight_a&adults={adults}',
    verticals: ['flight'],
    active: true,
    rating: 4.4,
  },

  // Activities
  {
    id: 'viator',
    name: 'Viator',
    logo: '/assets/providers/viator.png',
    urlTemplate: 'https://www.viator.com/{destination}/d{destinationId}-ttd',
    verticals: ['activity'],
    active: true,
    rating: 4.6,
  },
  {
    id: 'getyourguide',
    name: 'GetYourGuide',
    logo: '/assets/providers/getyourguide.png',
    urlTemplate: 'https://www.getyourguide.com/s/?q={destination}&date_from={date}',
    verticals: ['activity'],
    active: true,
    rating: 4.5,
  },
];

/**
 * Get providers by vertical
 */
export function getProvidersByVertical(vertical: 'flight' | 'hotel' | 'activity' | 'train' | 'bus'): Provider[] {
  return providers.filter(p => p.active && p.verticals.includes(vertical));
}

/**
 * Get provider by ID
 */
export function getProviderById(id: string): Provider | undefined {
  return providers.find(p => p.id === id);
}

/**
 * Construct provider URL with parameters
 */
export function constructProviderUrl(
  providerId: string,
  params: Record<string, string | number>
): string {
  const provider = getProviderById(providerId);
  if (!provider) {
    throw new Error(`Provider ${providerId} not found`);
  }

  let url = provider.urlTemplate;

  // Replace all placeholders with actual values
  Object.entries(params).forEach(([key, value]) => {
    const placeholder = `{${key}}`;
    url = url.replace(placeholder, encodeURIComponent(String(value)));
  });

  return url;
}

/**
 * Get provider logo URL
 */
export function getProviderLogo(providerId: string): string {
  const provider = getProviderById(providerId);
  return provider?.logo || '/assets/providers/default.png';
}
