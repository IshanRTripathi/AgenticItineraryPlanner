import { useState } from 'react';
import { Calendar, Train, ArrowLeftRight } from 'lucide-react';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../ui/select';

export function TrainSearchForm() {
  const [fromStation, setFromStation] = useState('');
  const [toStation, setToStation] = useState('');
  const [journeyDate, setJourneyDate] = useState('');
  const [trainClass, setTrainClass] = useState('');

  const handleSwap = () => {
    const temp = fromStation;
    setFromStation(toStation);
    setToStation(temp);
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    console.log('Searching trains:', { fromStation, toStation, journeyDate, trainClass });
    // TODO: Implement search logic
  };

  return (
    <form onSubmit={handleSearch} className="space-y-6">
      {/* From/To Stations */}
      <div className="grid grid-cols-1 md:grid-cols-[1fr,auto,1fr] gap-4 items-end">
        <div className="space-y-2">
          <Label htmlFor="train-from">From Station</Label>
          <div className="relative">
            <Train className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
            <Input
              id="train-from"
              type="text"
              placeholder="Enter station name"
              value={fromStation}
              onChange={(e) => setFromStation(e.target.value)}
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
          <Label htmlFor="train-to">To Station</Label>
          <div className="relative">
            <Train className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
            <Input
              id="train-to"
              type="text"
              placeholder="Enter station name"
              value={toStation}
              onChange={(e) => setToStation(e.target.value)}
              className="pl-10 h-14"
              required
            />
          </div>
        </div>
      </div>

      {/* Journey Date */}
      <div className="space-y-2">
        <Label htmlFor="train-date">Journey Date</Label>
        <div className="relative">
          <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
          <Input
            id="train-date"
            type="date"
            value={journeyDate}
            onChange={(e) => setJourneyDate(e.target.value)}
            className="pl-10 h-14"
            required
          />
        </div>
      </div>

      {/* Class Selection */}
      <div className="space-y-2">
        <Label htmlFor="train-class">Class</Label>
        <Select value={trainClass} onValueChange={setTrainClass}>
          <SelectTrigger id="train-class" className="h-14">
            <SelectValue placeholder="Select class" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="sleeper">Sleeper (SL)</SelectItem>
            <SelectItem value="3ac">AC 3-Tier (3A)</SelectItem>
            <SelectItem value="2ac">AC 2-Tier (2A)</SelectItem>
            <SelectItem value="1ac">AC 1st Class (1A)</SelectItem>
            <SelectItem value="cc">Chair Car (CC)</SelectItem>
            <SelectItem value="ec">Executive Chair Car (EC)</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Search Button */}
      <Button type="submit" className="w-full h-14 text-lg" size="lg">
        <Train className="mr-2 h-5 w-5" />
        Search Trains
      </Button>
    </form>
  );
}
