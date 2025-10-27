import { useState } from 'react';
import { Calendar, Bus, ArrowLeftRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../ui/select';

export function BusSearchForm() {
  const navigate = useNavigate();
  const [fromCity, setFromCity] = useState('');
  const [toCity, setToCity] = useState('');
  const [journeyDate, setJourneyDate] = useState('');
  const [seatType, setSeatType] = useState('');

  const handleSwap = () => {
    const temp = fromCity;
    setFromCity(toCity);
    setToCity(temp);
  };

  const handleSearch = (e: React.FormEvent) => {
    navigate('/search-results?type=buses');
    e.preventDefault();
    console.log('Searching buses:', { fromCity, toCity, journeyDate, seatType });
    // TODO: Implement search logic
  };

  return (
    <form onSubmit={handleSearch} className="space-y-6">
      {/* From/To Cities */}
      <div className="grid grid-cols-1 md:grid-cols-[1fr,auto,1fr] gap-4 items-end">
        <div className="space-y-2">
          <Label htmlFor="bus-from">From City</Label>
          <div className="relative">
            <Bus className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
            <Input
              id="bus-from"
              type="text"
              placeholder="Enter city name"
              value={fromCity}
              onChange={(e) => setFromCity(e.target.value)}
              className="pl-10 h-14"
              required
            />
          </div>
        </div>

        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={handleSwap}
          className="h-10 w-10 rounded-full mb-1 hidden md:flex items-center justify-center"
        >
          <ArrowLeftRight className="h-4 w-4" />
        </Button>

        <div className="space-y-2">
          <Label htmlFor="bus-to">To City</Label>
          <div className="relative">
            <Bus className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
            <Input
              id="bus-to"
              type="text"
              placeholder="Enter city name"
              value={toCity}
              onChange={(e) => setToCity(e.target.value)}
              className="pl-10 h-14"
              required
            />
          </div>
        </div>
      </div>

      {/* Journey Date */}
      <div className="space-y-2">
        <Label htmlFor="bus-date">Journey Date</Label>
        <div className="relative">
          <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
          <Input
            id="bus-date"
            type="date"
            value={journeyDate}
            onChange={(e) => setJourneyDate(e.target.value)}
            className="pl-10 h-14"
            required
          />
        </div>
      </div>

      {/* Seat Type Selection */}
      <div className="space-y-2">
        <Label htmlFor="seat-type">Seat Type</Label>
        <Select value={seatType} onValueChange={setSeatType}>
          <SelectTrigger id="seat-type" className="h-14">
            <SelectValue placeholder="Select seat type" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="seater">Seater</SelectItem>
            <SelectItem value="sleeper">Sleeper</SelectItem>
            <SelectItem value="ac-seater">AC Seater</SelectItem>
            <SelectItem value="ac-sleeper">AC Sleeper</SelectItem>
            <SelectItem value="volvo">Volvo AC</SelectItem>
            <SelectItem value="multi-axle">Multi-Axle</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Search Button */}
      <Button type="submit" className="w-full h-14 text-lg" size="lg">
        <Bus className="mr-2 h-5 w-5" />
        Search Buses
      </Button>
    </form>
  );
}
