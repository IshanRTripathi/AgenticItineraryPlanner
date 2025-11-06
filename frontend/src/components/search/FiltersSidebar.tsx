/**
 * Filters Sidebar Component
 * Task 9: Mobile-optimized filters with bottom sheet on mobile
 * Task 35.2: Search filters with price range, ratings, amenities
 */

import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import { Slider } from '@/components/ui/slider';
import { Badge } from '@/components/ui/badge';
import { ResponsiveModal } from '@/components/ui/responsive-modal';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { Star, X, SlidersHorizontal } from 'lucide-react';

interface FiltersSidebarProps {
  onFilterChange: (filters: any) => void;
}

export function FiltersSidebar({ onFilterChange }: FiltersSidebarProps) {
  const isMobile = useMediaQuery('(max-width: 768px)');
  const [isOpen, setIsOpen] = useState(false);
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
    if (isMobile) {
      setIsOpen(false);
    }
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

  // Filter content component (shared between mobile and desktop)
  const FiltersContent = () => (
    <div className="space-y-4">
      {/* Header - Desktop only (mobile has it in modal) */}
      {!isMobile && (
        <>
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-semibold">Filters</h3>
            {activeFiltersCount > 0 && (
              <Button
                variant="ghost"
                size="sm"
                onClick={handleClearFilters}
                className="min-h-[44px] px-2 touch-manipulation"
              >
                <X className="w-4 h-4 mr-1" />
                Clear
              </Button>
            )}
          </div>

          {activeFiltersCount > 0 && (
            <Badge variant="secondary">{activeFiltersCount} active</Badge>
          )}
        </>
      )}

      {/* Price Range */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-sm sm:text-base">Price Range</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <Slider
            value={priceRange}
            onValueChange={setPriceRange}
            min={0}
            max={2000}
            step={50}
            className="w-full touch-manipulation"
          />
          <div className="flex items-center justify-between text-sm">
            <span className="text-muted-foreground font-medium">${priceRange[0]}</span>
            <span className="text-muted-foreground font-medium">${priceRange[1]}</span>
          </div>
        </CardContent>
      </Card>

      {/* Stops */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-sm sm:text-base">Stops</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {['Non-stop', '1 stop', '2+ stops'].map((stop) => (
            <div key={stop} className="flex items-center space-x-3 min-h-[44px]">
              <Checkbox
                id={stop}
                checked={selectedStops.includes(stop)}
                onCheckedChange={() => toggleStop(stop)}
                className="touch-manipulation"
              />
              <Label 
                htmlFor={stop} 
                className="text-sm sm:text-base cursor-pointer flex-1 touch-manipulation"
              >
                {stop}
              </Label>
            </div>
          ))}
        </CardContent>
      </Card>

      {/* Rating */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-sm sm:text-base">Minimum Rating</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {[4.5, 4.0, 3.5, 3.0].map((rating) => (
            <button
              key={rating}
              onClick={() => setMinRating(rating)}
              className={`w-full flex items-center gap-2 px-3 py-3 min-h-[48px] rounded-lg text-sm sm:text-base transition-all touch-manipulation active:scale-95 ${
                minRating === rating
                  ? 'bg-primary text-primary-foreground shadow-md'
                  : 'hover:bg-muted'
              }`}
            >
              <Star className="w-4 h-4 sm:w-5 sm:h-5 fill-current" />
              <span>{rating}+</span>
            </button>
          ))}
        </CardContent>
      </Card>

      {/* Amenities */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-sm sm:text-base">Amenities</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {[
            { id: 'wifi', label: 'WiFi' },
            { id: 'meals', label: 'Meals Included' },
            { id: 'entertainment', label: 'Entertainment' },
            { id: 'extra-legroom', label: 'Extra Legroom' },
          ].map((amenity) => (
            <div key={amenity.id} className="flex items-center space-x-3 min-h-[44px]">
              <Checkbox
                id={amenity.id}
                checked={selectedAmenities.includes(amenity.id)}
                onCheckedChange={() => toggleAmenity(amenity.id)}
                className="touch-manipulation"
              />
              <Label 
                htmlFor={amenity.id} 
                className="text-sm sm:text-base cursor-pointer flex-1 touch-manipulation"
              >
                {amenity.label}
              </Label>
            </div>
          ))}
        </CardContent>
      </Card>

      {/* Apply Button */}
      <Button 
        onClick={handleApplyFilters} 
        className="w-full min-h-[48px] touch-manipulation active:scale-95 transition-transform"
      >
        Apply Filters
        {activeFiltersCount > 0 && ` (${activeFiltersCount})`}
      </Button>
    </div>
  );

  // Mobile: Render trigger button + bottom sheet
  if (isMobile) {
    return (
      <>
        <Button
          onClick={() => setIsOpen(true)}
          variant="outline"
          className="w-full min-h-[48px] touch-manipulation active:scale-95 transition-transform"
        >
          <SlidersHorizontal className="w-4 h-4 mr-2" />
          Filters
          {activeFiltersCount > 0 && (
            <Badge variant="secondary" className="ml-2">
              {activeFiltersCount}
            </Badge>
          )}
        </Button>

        <ResponsiveModal
          open={isOpen}
          onOpenChange={setIsOpen}
          title="Filters"
          footer={
            activeFiltersCount > 0 ? (
              <Button
                variant="ghost"
                onClick={handleClearFilters}
                className="w-full min-h-[48px] touch-manipulation"
              >
                <X className="w-4 h-4 mr-2" />
                Clear All Filters
              </Button>
            ) : undefined
          }
        >
          <FiltersContent />
        </ResponsiveModal>
      </>
    );
  }

  // Desktop: Render sidebar
  return (
    <div className="w-[280px] sticky top-4">
      <FiltersContent />
    </div>
  );
}
