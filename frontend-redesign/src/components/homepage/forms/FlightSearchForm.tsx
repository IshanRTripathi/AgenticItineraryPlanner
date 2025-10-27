/**
 * Flight Search Form
 * Premium flight search with validation
 */

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { ArrowLeftRight, Search } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export function FlightSearchForm() {
  const navigate = useNavigate();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    navigate('/search-results?type=flights');
  };

  return (
    <form className="space-y-4" onSubmit={handleSubmit}>
      {/* Trip Type */}
      <div className="flex gap-4">
        <Select value="roundtrip" onValueChange={() => {}}>
          <SelectTrigger className="w-40">
            <SelectValue placeholder="Trip type" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="oneway">One Way</SelectItem>
            <SelectItem value="roundtrip">Round Trip</SelectItem>
            <SelectItem value="multicity">Multi-City</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Origin & Destination */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <Label htmlFor="origin">From</Label>
          <Input
            id="origin"
            placeholder="Origin city or airport"
            className="mt-2"
          />
        </div>
        <div className="relative">
          <Label htmlFor="destination">To</Label>
          <Input
            id="destination"
            placeholder="Destination city or airport"
            className="mt-2"
          />
          <button
            type="button"
            className="absolute right-3 top-11 p-2 rounded-full hover:bg-muted transition-colors"
            title="Swap cities"
          >
            <ArrowLeftRight className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Dates & Passengers */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <Label htmlFor="departure">Departure</Label>
          <Input
            id="departure"
            type="date"
            className="mt-2"
          />
        </div>
        <div>
          <Label htmlFor="return">Return</Label>
          <Input
            id="return"
            type="date"
            className="mt-2"
          />
        </div>
        <div>
          <Label htmlFor="passengers">Passengers</Label>
          <Select value="1" onValueChange={() => {}}>
            <SelectTrigger className="mt-2">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="1">1 Passenger</SelectItem>
              <SelectItem value="2">2 Passengers</SelectItem>
              <SelectItem value="3">3 Passengers</SelectItem>
              <SelectItem value="4">4+ Passengers</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Class */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <Label htmlFor="class">Class</Label>
          <Select value="economy" onValueChange={() => {}}>
            <SelectTrigger className="mt-2">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="economy">Economy</SelectItem>
              <SelectItem value="premium">Premium Economy</SelectItem>
              <SelectItem value="business">Business</SelectItem>
              <SelectItem value="first">First Class</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Search Button */}
      <Button type="submit" size="lg" className="w-full">
        <Search className="mr-2 h-5 w-5" />
        Search Flights
      </Button>
    </form>
  );
}
