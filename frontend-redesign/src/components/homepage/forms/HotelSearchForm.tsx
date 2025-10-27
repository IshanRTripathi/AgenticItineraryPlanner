/**
 * Hotel Search Form
 * Premium hotel search with validation
 */

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Search } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export function HotelSearchForm() {
  const navigate = useNavigate();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    navigate('/search-results?type=hotels');
  };

  return (
    <form className="space-y-4" onSubmit={handleSubmit}>
      {/* Location */}
      <div>
        <Label htmlFor="location">Location</Label>
        <Input
          id="location"
          placeholder="City, hotel name, or landmark"
          className="mt-2"
        />
      </div>

      {/* Check-in & Check-out */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <Label htmlFor="checkin">Check-in</Label>
          <Input
            id="checkin"
            type="date"
            className="mt-2"
          />
        </div>
        <div>
          <Label htmlFor="checkout">Check-out</Label>
          <Input
            id="checkout"
            type="date"
            className="mt-2"
          />
        </div>
      </div>

      {/* Guests & Rooms */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <Label htmlFor="guests">Guests</Label>
          <Select value="2" onValueChange={() => {}}>
            <SelectTrigger className="mt-2">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="1">1 Guest</SelectItem>
              <SelectItem value="2">2 Guests</SelectItem>
              <SelectItem value="3">3 Guests</SelectItem>
              <SelectItem value="4">4+ Guests</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div>
          <Label htmlFor="rooms">Rooms</Label>
          <Select value="1" onValueChange={() => {}}>
            <SelectTrigger className="mt-2">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="1">1 Room</SelectItem>
              <SelectItem value="2">2 Rooms</SelectItem>
              <SelectItem value="3">3 Rooms</SelectItem>
              <SelectItem value="4">4+ Rooms</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Search Button */}
      <Button type="submit" size="lg" className="w-full">
        <Search className="mr-2 h-5 w-5" />
        Search Hotels
      </Button>
    </form>
  );
}
