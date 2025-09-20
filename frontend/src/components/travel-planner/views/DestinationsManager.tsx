import React, { useState } from 'react';
import { Button } from '../../ui/button';
import { Badge } from '../../ui/badge';
import { Input } from '../../ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../ui/select';
import { Menu, Plus, Minus, Search, FileText } from 'lucide-react';
import { DestinationManagerProps, CURRENCIES, ErrorBoundary } from '../shared/types';
import { TransportPlanner } from './TransportPlanner';

export function DestinationsManager({ 
  destinations, 
  currency, 
  showNotes, 
  onUpdate, 
  onAdd, 
  onRemove, 
  onCurrencyChange, 
  onToggleNotes 
}: DestinationManagerProps) {
  const [newDestination, setNewDestination] = useState('');
  const [tripData, setTripData] = useState<any>({}); // This should be passed as prop

  const totalNights = destinations.reduce((sum, dest) => sum + dest.nights, 0);
  const maxNights = 10;

  const addDestination = () => {
    if (newDestination.trim()) {
      const newDest = {
        id: Date.now().toString(),
        name: newDestination,
        nights: 1,
        sleeping: false,
        discover: false,
        notes: ''
      };
      onAdd(newDest);
      setNewDestination('');
    }
  };

  return (
    <ErrorBoundary>
      <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-4">
          <Select value={currency} onValueChange={onCurrencyChange}>
            <SelectTrigger className="w-48">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {CURRENCIES.map(curr => (
                <SelectItem key={curr} value={curr}>
                  Cost in {curr} ({curr === 'EUR' ? '€' : curr === 'USD' ? '$' : '£'})
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Badge variant="secondary">
            {totalNights}/{maxNights} Nights planned
          </Badge>
          <Button variant="ghost" size="sm" onClick={onToggleNotes}>
            <Menu className="w-4 h-4 mr-2" />
            Show notes
          </Button>
        </div>
      </div>

      <div className="border rounded-lg">
        <div className={`grid ${showNotes ? 'grid-cols-7' : 'grid-cols-6'} gap-4 p-4 bg-gray-50 border-b font-medium text-sm`}>
          <div>Destination</div>
          <div>Nights</div>
          <div>Sleeping</div>
          <div>Discover</div>
          <div>Transport</div>
          {showNotes && <div>Notes</div>}
        </div>
        
        {destinations.map((destination) => (
          <div key={destination.id} className={`grid ${showNotes ? 'grid-cols-7' : 'grid-cols-6'} gap-4 p-4 border-b items-center`}>
            <div className="font-medium">{destination.name}</div>
            <div className="flex items-center space-x-2">
              <Button 
                variant="outline" 
                size="sm" 
                className="w-8 h-8 p-0"
                onClick={() => onUpdate(destination.id, { nights: Math.max(0, destination.nights - 1) })}
              >
                <Minus className="w-3 h-3" />
              </Button>
              <span className="w-6 text-center">{destination.nights}</span>
              <Button 
                variant="outline" 
                size="sm" 
                className="w-8 h-8 p-0"
                onClick={() => onUpdate(destination.id, { nights: destination.nights + 1 })}
              >
                <Plus className="w-3 h-3" />
              </Button>
            </div>
            <div>
              <Button 
                variant={destination.sleeping ? "default" : "outline"} 
                size="sm" 
                className="w-8 h-8 p-0"
                onClick={() => onUpdate(destination.id, { sleeping: !destination.sleeping })}
              >
                <Plus className="w-3 h-3" />
              </Button>
            </div>
            <div>
              <Button 
                variant={destination.discover ? "default" : "outline"} 
                size="sm" 
                className="w-8 h-8 p-0 bg-amber-100 border-amber-300 text-amber-700 hover:bg-amber-200"
                onClick={() => onUpdate(destination.id, { discover: !destination.discover })}
              >
                <Plus className="w-3 h-3" />
              </Button>
            </div>
            <div>
              {destination.transport && (
                <TransportPlanner 
                  destination={destination}
                  tripData={tripData}
                  onUpdate={(destinationId, transport) => {
                    onUpdate(destinationId, { transport });
                  }}
                />
              )}
            </div>
            {showNotes && (
              <div>
                <Input 
                  placeholder="Add notes.." 
                  value={destination.notes}
                  onChange={(e) => onUpdate(destination.id, { notes: e.target.value })}
                  className="text-sm"
                />
              </div>
            )}
          </div>
        ))}
        
        <div className="p-4">
          <div className="flex items-center space-x-2">
            <Input 
              placeholder="Add new destination…" 
              value={newDestination}
              onChange={(e) => setNewDestination(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && addDestination()}
              className="flex-1"
            />
            <Button onClick={addDestination}>Add</Button>
          </div>
        </div>
      </div>

      <div className="flex justify-center space-x-4 mt-6">
        <Button variant="outline">
          <Search className="w-4 h-4 mr-2" />
          Discover
        </Button>
        <Button variant="outline">
          <FileText className="w-4 h-4 mr-2" />
          Collection
        </Button>
      </div>
      </div>
    </ErrorBoundary>
  );
}
