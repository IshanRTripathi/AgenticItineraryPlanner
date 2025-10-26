/**
 * Mock Popular Flight Routes Data
 * Used for popular routes carousel on homepage
 */

export interface FlightRoute {
  id: string;
  origin: string;
  originCode: string;
  destination: string;
  destinationCode: string;
  airline: string;
  airlineLogoUrl: string;
  price: number;
  currency: string;
  duration: string;
  stops: number;
}

export const mockRoutes: FlightRoute[] = [
  {
    id: 'del-bom',
    origin: 'Delhi',
    originCode: 'DEL',
    destination: 'Mumbai',
    destinationCode: 'BOM',
    airline: 'Air India',
    airlineLogoUrl: 'https://via.placeholder.com/32x32?text=AI',
    price: 89,
    currency: 'USD',
    duration: '2h 15m',
    stops: 0
  },
  {
    id: 'nyc-lax',
    origin: 'New York',
    originCode: 'JFK',
    destination: 'Los Angeles',
    destinationCode: 'LAX',
    airline: 'Delta',
    airlineLogoUrl: 'https://via.placeholder.com/32x32?text=DL',
    price: 199,
    currency: 'USD',
    duration: '5h 30m',
    stops: 0
  },
  {
    id: 'lon-par',
    origin: 'London',
    originCode: 'LHR',
    destination: 'Paris',
    destinationCode: 'CDG',
    airline: 'British Airways',
    airlineLogoUrl: 'https://via.placeholder.com/32x32?text=BA',
    price: 79,
    currency: 'USD',
    duration: '1h 15m',
    stops: 0
  },
  {
    id: 'dxb-bkk',
    origin: 'Dubai',
    originCode: 'DXB',
    destination: 'Bangkok',
    destinationCode: 'BKK',
    airline: 'Emirates',
    airlineLogoUrl: 'https://via.placeholder.com/32x32?text=EK',
    price: 299,
    currency: 'USD',
    duration: '6h 20m',
    stops: 0
  },
  {
    id: 'sin-syd',
    origin: 'Singapore',
    originCode: 'SIN',
    destination: 'Sydney',
    destinationCode: 'SYD',
    airline: 'Singapore Airlines',
    airlineLogoUrl: 'https://via.placeholder.com/32x32?text=SQ',
    price: 349,
    currency: 'USD',
    duration: '8h 10m',
    stops: 0
  },
  {
    id: 'hkg-tok',
    origin: 'Hong Kong',
    originCode: 'HKG',
    destination: 'Tokyo',
    destinationCode: 'NRT',
    airline: 'Cathay Pacific',
    airlineLogoUrl: 'https://via.placeholder.com/32x32?text=CX',
    price: 259,
    currency: 'USD',
    duration: '4h 30m',
    stops: 0
  },
  {
    id: 'fra-ist',
    origin: 'Frankfurt',
    originCode: 'FRA',
    destination: 'Istanbul',
    destinationCode: 'IST',
    airline: 'Lufthansa',
    airlineLogoUrl: 'https://via.placeholder.com/32x32?text=LH',
    price: 149,
    currency: 'USD',
    duration: '3h 20m',
    stops: 0
  },
  {
    id: 'sfo-sea',
    origin: 'San Francisco',
    originCode: 'SFO',
    destination: 'Seattle',
    destinationCode: 'SEA',
    airline: 'Alaska Airlines',
    airlineLogoUrl: 'https://via.placeholder.com/32x32?text=AS',
    price: 129,
    currency: 'USD',
    duration: '2h 15m',
    stops: 0
  },
  {
    id: 'mel-akl',
    origin: 'Melbourne',
    originCode: 'MEL',
    destination: 'Auckland',
    destinationCode: 'AKL',
    airline: 'Qantas',
    airlineLogoUrl: 'https://via.placeholder.com/32x32?text=QF',
    price: 279,
    currency: 'USD',
    duration: '3h 45m',
    stops: 0
  },
  {
    id: 'mad-bcn',
    origin: 'Madrid',
    originCode: 'MAD',
    destination: 'Barcelona',
    destinationCode: 'BCN',
    airline: 'Iberia',
    airlineLogoUrl: 'https://via.placeholder.com/32x32?text=IB',
    price: 69,
    currency: 'USD',
    duration: '1h 20m',
    stops: 0
  }
];
