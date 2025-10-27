/**
 * Filters Sidebar Component
 * Task 35.2: Search filters with price range, ratings, amenities
 */

import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import { Slider } from '@/components/ui/slider';
import { Badge } from '@/components/ui/badge';
import { Star, X } from 'lucide-react';

interface FiltersSidebarProps {
  onFilterChange: (filters: any) => void;
}

export function FiltersSidebar({ onFilterChange }: FiltersSidebarProps) {
  const [priceRange, setPriceRange] = useState([0, 2000]);
  const [selectedStops, setSelectedStops] = useState<string[]>([]);
  const [selectedAmenities, setSelectedAmenities] = useState<string[]>([]);
  const [minRating, setMinRating] = useState(0);

  const handleApplyFilters = () => {
    onFilterChange({
      priceRange,
      stops: selectedStops,
      amenities: selectedAmenities,
      minRating,
    });
  };

  const handleClearFilters = () => {
    setPriceRange([0, 2000]);
    setSelectedStops([]);
    setSelectedAmenities([]);
    setMinRating(0);
    onFilterChange({});
  };

  const toggleStop = (stop: string) => {
    setSelectedStops(prev =>
      prev.includes(stop) ? prev.filter(s => s !== stop) : [...prev, stop]
    );
  };

  const toggleAmenity = (amenity: string) => {
    setSelectedAmenities(prev =>
      prev.includes(amenity) ? prev.filter(a => a !== amenity) : [...prev, amenity]
    );
  };

  const activeFiltersCount = 
    (priceRange[0] > 0 || priceRange[1] < 2000 ? 1 : 0) +
    selectedStops.length +
    selectedAmenities.length +
    (minRating > 0 ? 1 : 0);

  return (
    <div className="w-[280px] space-y-4 sticky top-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">Filters</h3>
        {activeFiltersCount > 0 && (
          <Button
            variant="ghost"
            size="sm"
            onClick={handleClearFilters}
            className="h-8 px-2"
          >
            <X className="w-4 h-4 mr-1" />
            Clear
          </Button>
        )}
      </div>

      {activeFiltersCount > 0 && (
        <Badge variant="secondary">{activeFiltersCount} active</Badge>
      )}

      {/* Price Range */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-sm">Price Range</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <Slider
            value={priceRange}
            onValueChange={setPriceRange}
            min={0}
            max={2000}
            step={50}
            className="w-full"
          />
          <div className="flex items-center justify-between text-sm">
            <span className="text-muted-foreground">${priceRange[0]}</span>
            <span className="text-muted-foreground">${priceRange[1]}</span>
          </div>
        </CardContent>
      </Card>

      {/* Stops */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-sm">Stops</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {['Non-stop', '1 stop', '2+ stops'].map((stop) => (
            <div key={stop} className="flex items-center space-x-2">
              <Checkbox
                id={stop}
                checked={selectedStops.includes(stop)}
                onCheckedChange={() => toggleStop(stop)}
              />
              <Label htmlFor={stop} className="text-sm cursor-pointer">
                {stop}
              </Label>
            </div>
          ))}
        </CardContent>
      </Card>

      {/* Rating */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-sm">Minimum Rating</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {[4.5, 4.0, 3.5, 3.0].map((rating) => (
            <button
              key={rating}
              onClick={() => setMinRating(rating)}
              className={`w-full flex items-center gap-2 px-3 py-2 rounded-lg text-sm transition-colors ${
                minRating === rating
                  ? 'bg-primary text-primary-foreground'
                  : 'hover:bg-muted'
              }`}
            >
              <Star className="w-4 h-4 fill-current" />
              <span>{rating}+</span>
            </button>
          ))}
        </CardContent>
      </Card>

      {/* Amenities */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-sm">Amenities</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {[
            { id: 'wifi', label: 'WiFi' },
            { id: 'meals', label: 'Meals Included' },
            { id: 'entertainment', label: 'Entertainment' },
            { id: 'extra-legroom', label: 'Extra Legroom' },
          ].map((amenity) => (
            <div key={amenity.id} className="flex items-center space-x-2">
              <Checkbox
                id={amenity.id}
                checked={selectedAmenities.includes(amenity.id)}
                onCheckedChange={() => toggleAmenity(amenity.id)}
              />
              <Label htmlFor={amenity.id} className="text-sm cursor-pointer">
                {amenity.label}
              </Label>
            </div>
          ))}
        </CardContent>
      </Card>

      {/* Apply Button */}
      <Button onClick={handleApplyFilters} className="w-full">
        Apply Filters
      </Button>
    </div>
  );
}
