import { useState } from 'react';
import { Calendar, Users, MapPin } from 'lucide-react';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../ui/select';

export function HolidaySearchForm() {
  const [destination, setDestination] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [adults, setAdults] = useState(2);
  const [children, setChildren] = useState(0);
  const [packageType, setPackageType] = useState('');

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    console.log('Searching holidays:', { destination, startDate, endDate, adults, children, packageType });
    // TODO: Implement search logic
  };

  return (
    <form onSubmit={handleSearch} className="space-y-6">
      {/* Destination */}
      <div className="space-y-2">
        <Label htmlFor="holiday-destination">Destination</Label>
        <div className="relative">
          <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
          <Input
            id="holiday-destination"
            type="text"
            placeholder="Where do you want to go?"
            value={destination}
            onChange={(e) => setDestination(e.target.value)}
            className="pl-10 h-14"
            required
          />
        </div>
      </div>

      {/* Date Range */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="holiday-start-date">Start Date</Label>
          <div className="relative">
            <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
            <Input
              id="holiday-start-date"
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="pl-10 h-14"
              required
            />
          </div>
        </div>
        <div className="space-y-2">
          <Label htmlFor="holiday-end-date">End Date</Label>
          <div className="relative">
            <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
            <Input
              id="holiday-end-date"
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              min={startDate}
              className="pl-10 h-14"
              required
            />
          </div>
        </div>
      </div>

      {/* Travelers */}
      <div className="space-y-2">
        <Label>Travelers</Label>
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="holiday-adults" className="text-sm text-muted-foreground">Adults</Label>
            <div className="flex items-center gap-3">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setAdults(Math.max(1, adults - 1))}
                className="h-10 w-10 rounded-full"
              >
                -
              </Button>
              <span className="text-lg font-semibold w-8 text-center">{adults}</span>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setAdults(Math.min(9, adults + 1))}
                className="h-10 w-10 rounded-full"
              >
                +
              </Button>
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="holiday-children" className="text-sm text-muted-foreground">Children</Label>
            <div className="flex items-center gap-3">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setChildren(Math.max(0, children - 1))}
                className="h-10 w-10 rounded-full"
              >
                -
              </Button>
              <span className="text-lg font-semibold w-8 text-center">{children}</span>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setChildren(Math.min(8, children + 1))}
                className="h-10 w-10 rounded-full"
              >
                +
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Package Type */}
      <div className="space-y-2">
        <Label htmlFor="package-type">Package Type</Label>
        <Select value={packageType} onValueChange={setPackageType}>
          <SelectTrigger id="package-type" className="h-14">
            <SelectValue placeholder="Select package type" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all-inclusive">All-Inclusive</SelectItem>
            <SelectItem value="adventure">Adventure</SelectItem>
            <SelectItem value="luxury">Luxury</SelectItem>
            <SelectItem value="budget">Budget</SelectItem>
            <SelectItem value="family">Family</SelectItem>
            <SelectItem value="romantic">Romantic</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Search Button */}
      <Button type="submit" className="w-full h-14 text-lg" size="lg">
        <Users className="mr-2 h-5 w-5" />
        Search Holiday Packages
      </Button>
    </form>
  );
}
