import React, { useState } from 'react';
import { Button } from '../../ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '../../ui/dialog';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../ui/select';
import { MapPin, Clock, Car, Plane, Train, Bus, Navigation, Plus, X } from 'lucide-react';

interface TransportPopupProps {
  isOpen: boolean;
  onClose: () => void;
  fromDestination: string;
  toDestination: string;
  onSave: (transports: TransportDetails[]) => void;
  existingTransports?: TransportDetails[];
}

interface TransportDetails {
  id: string;
  mode: 'walk' | 'cab' | 'flight' | 'other';
  distance: string;
  distanceUnit: 'km' | 'mi';
  duration?: string;
  cost?: string;
  notes?: string;
}

const transportModes = [
  { value: 'walk', label: 'Walk', icon: Navigation, color: 'text-green-600' },
  { value: 'cab', label: 'Cab/Taxi', icon: Car, color: 'text-yellow-600' },
  { value: 'flight', label: 'Flight', icon: Plane, color: 'text-blue-600' },
  { value: 'other', label: 'Other', icon: Train, color: 'text-gray-600' },
];

export function TransportPopup({ 
  isOpen, 
  onClose, 
  fromDestination, 
  toDestination, 
  onSave,
  existingTransports = []
}: TransportPopupProps) {
  const [transports, setTransports] = useState<TransportDetails[]>(
    existingTransports.length > 0 ? existingTransports : [{
      id: Date.now().toString(),
      mode: 'walk',
      distance: '',
      distanceUnit: 'km',
      duration: '',
      cost: '',
      notes: '',
    }]
  );

  const addTransport = () => {
    const newTransport: TransportDetails = {
      id: Date.now().toString(),
      mode: 'walk',
      distance: '',
      distanceUnit: 'km',
      duration: '',
      cost: '',
      notes: '',
    };
    setTransports(prev => [...prev, newTransport]);
  };

  const removeTransport = (id: string) => {
    if (transports.length > 1) {
      setTransports(prev => prev.filter(t => t.id !== id));
    }
  };

  const updateTransport = (id: string, updates: Partial<TransportDetails>) => {
    setTransports(prev => prev.map(t => t.id === id ? { ...t, ...updates } : t));
  };

  const handleSave = () => {
    const validTransports = transports.filter(t => t.distance.trim());
    if (validTransports.length > 0) {
      onSave(validTransports);
      onClose();
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="w-[90vw] max-w-2xl mx-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center space-x-2">
            <MapPin className="w-5 h-5" />
            <span>Transport Details</span>
          </DialogTitle>
          <DialogDescription>
            From <strong>{fromDestination}</strong> to <strong>{toDestination}</strong>
          </DialogDescription>
        </DialogHeader>

        <div className="grid grid-cols-2 gap-6">
          {/* Left Side - Transport Details */}
          <div className="space-y-4">
            <h3 className="text-lg font-semibold">Transport Options</h3>
            {transports.map((transport, index) => {
              const selectedMode = transportModes.find(mode => mode.value === transport.mode);
              const IconComponent = selectedMode?.icon || Navigation;
              
              return (
                <div key={transport.id} className="border rounded-lg p-4 space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                      <IconComponent className={`w-5 h-5 ${selectedMode?.color}`} />
                      <span className="font-medium">Transport {index + 1}</span>
                    </div>
                    {transports.length > 1 && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => removeTransport(transport.id)}
                        className="h-8 w-8 p-0 text-red-500 hover:text-red-700"
                      >
                        <X className="w-4 h-4" />
                      </Button>
                    )}
                  </div>

                  {/* Transport Mode */}
                  <div className="space-y-2">
                    <Label className="text-sm font-medium">Mode</Label>
                    <Select 
                      value={transport.mode} 
                      onValueChange={(value: any) => updateTransport(transport.id, { mode: value })}
                    >
                      <SelectTrigger className="h-10">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {transportModes.map((mode) => {
                          const ModeIcon = mode.icon;
                          return (
                            <SelectItem key={mode.value} value={mode.value}>
                              <div className="flex items-center space-x-2">
                                <ModeIcon className={`w-4 h-4 ${mode.color}`} />
                                <span>{mode.label}</span>
                              </div>
                            </SelectItem>
                          );
                        })}
                      </SelectContent>
                    </Select>
                  </div>

                  {/* Distance */}
                  <div className="space-y-2">
                    <Label className="text-sm font-medium">Distance *</Label>
                    <div className="flex space-x-2">
                      <Input
                        placeholder="5"
                        value={transport.distance}
                        onChange={(e) => updateTransport(transport.id, { distance: e.target.value })}
                        className="h-10 flex-1"
                      />
                      <Select 
                        value={transport.distanceUnit} 
                        onValueChange={(value: any) => updateTransport(transport.id, { distanceUnit: value })}
                      >
                        <SelectTrigger className="w-20 h-10">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="km">km</SelectItem>
                          <SelectItem value="mi">mi</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>

                  {/* Duration and Cost */}
                  <div className="grid grid-cols-2 gap-3">
                    <div className="space-y-2">
                      <Label className="text-sm font-medium">Duration</Label>
                      <Input
                        placeholder="30 min"
                        value={transport.duration}
                        onChange={(e) => updateTransport(transport.id, { duration: e.target.value })}
                        className="h-10"
                      />
                    </div>
                    <div className="space-y-2">
                      <Label className="text-sm font-medium">Cost</Label>
                      <Input
                        placeholder="$25"
                        value={transport.cost}
                        onChange={(e) => updateTransport(transport.id, { cost: e.target.value })}
                        className="h-10"
                      />
                    </div>
                  </div>

                  {/* Notes */}
                  <div className="space-y-2">
                    <Label className="text-sm font-medium">Notes</Label>
                    <Input
                      placeholder="Additional details..."
                      value={transport.notes}
                      onChange={(e) => updateTransport(transport.id, { notes: e.target.value })}
                      className="h-10"
                    />
                  </div>
                </div>
              );
            })}
          </div>

          {/* Right Side - Add Transport */}
          <div className="space-y-4">
            <h3 className="text-lg font-semibold">Add Transport</h3>
            <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 flex flex-col items-center justify-center h-full min-h-[300px]">
              <Plus className="w-12 h-12 text-gray-400 mb-4" />
              <p className="text-gray-600 text-center mb-4">
                Add another transport option for this route
              </p>
              <Button onClick={addTransport} className="px-6">
                <Plus className="w-4 h-4 mr-2" />
                Add Transport Mode
              </Button>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex justify-end space-x-3 pt-6 border-t">
          <Button variant="outline" onClick={onClose} className="px-6">
            Cancel
          </Button>
          <Button 
            onClick={handleSave} 
            disabled={transports.every(t => !t.distance.trim())}
            className="px-6"
          >
            Save Transport
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
