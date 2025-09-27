import React, { useState } from 'react';
import { Button } from '../../ui/button';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '../../ui/dialog';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../ui/select';
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '../../ui/collapsible';
import {
  MapPin,
  Car,
  Plane,
  Train,
  Navigation,
  Plus,
  X,
  ChevronDown,
  ChevronRight,
} from 'lucide-react';

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
  existingTransports = [],
}: TransportPopupProps) {
  const [transports, setTransports] = useState<TransportDetails[]>(
    existingTransports.length > 0
      ? existingTransports
      : [
          {
            id: Date.now().toString(),
            mode: 'walk',
            distance: '',
            distanceUnit: 'km',
            duration: '',
            cost: '',
            notes: '',
          },
        ],
  );
  const [expandedTransport, setExpandedTransport] = useState<string | null>(
    existingTransports.length > 0
      ? existingTransports[existingTransports.length - 1].id
      : null,
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
    setTransports((prev) => [...prev, newTransport]);
    setExpandedTransport(newTransport.id);
  };

  const removeTransport = (id: string) => {
    if (transports.length > 1) {
      setTransports((prev) => prev.filter((t) => t.id !== id));
      if (expandedTransport === id) {
        const remaining = transports.filter((t) => t.id !== id);
        setExpandedTransport(remaining.at(-1)?.id || null);
      }
    }
  };

  const updateTransport = (id: string, updates: Partial<TransportDetails>) => {
    setTransports((prev) =>
      prev.map((t) => (t.id === id ? { ...t, ...updates } : t)),
    );
  };

  const toggleTransport = (id: string) => {
    setExpandedTransport(expandedTransport === id ? null : id);
  };

  const handleSave = () => {
    const valid = transports.filter((t) => t.distance.trim());
    if (valid.length > 0) {
      onSave(valid);
      onClose();
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="w-screen max-h-[80vh] my-[10vh] rounded-none sm:w-[95vw] sm:max-w-md sm:max-h-[80vh] sm:my-[10vh] sm:rounded-lg sm:mx-auto flex flex-col p-0 overflow-hidden">
        {/* Header */}
        <DialogHeader className="flex-shrink-0 p-4 sm:p-6 pb-3 sm:pb-4 border-b border-gray-200">
          <DialogTitle className="flex items-center space-x-2 text-lg">
            <MapPin className="w-5 h-5" />
            <span>Transport Details</span>
          </DialogTitle>
          <DialogDescription className="text-sm">
            From <strong>{fromDestination}</strong> to{' '}
            <strong>{toDestination}</strong>
          </DialogDescription>
        </DialogHeader>

        {/* Scrollable content */}
        <div className="flex-1 min-h-0 overflow-y-auto overscroll-contain">
          <div className="space-y-3 px-4 sm:px-6 py-2">
            {transports.map((transport, index) => {
              const selectedMode = transportModes.find(
                (m) => m.value === transport.mode,
              );
              const IconComponent = selectedMode?.icon || Navigation;
              const isExpanded = expandedTransport === transport.id;

              return (
                <Collapsible
                  key={transport.id}
                  open={isExpanded}
                  onOpenChange={() => toggleTransport(transport.id)}
                >
                  <div className="border rounded-lg bg-white shadow-sm">
                    {/* Header */}
                    <CollapsibleTrigger asChild>
                      <div className="flex items-center justify-between p-3 sm:p-4 cursor-pointer hover:bg-gray-50 transition-colors min-h-[60px]">
                        <div className="flex items-center space-x-3 flex-1 min-w-0">
                          <div className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center flex-shrink-0">
                            <IconComponent
                              className={`w-5 h-5 ${selectedMode?.color}`}
                            />
                          </div>
                          <div className="flex-1 min-w-0">
                            <h3 className="font-semibold text-base truncate">
                              {selectedMode?.label || 'Transport'} {index + 1}
                            </h3>
                            <p className="text-sm text-gray-600 truncate">
                              {transport.distance
                                ? `${transport.distance} ${transport.distanceUnit}`
                                : 'No distance set'}
                              {transport.duration && ` • ${transport.duration}`}
                            </p>
                          </div>
                        </div>
                        <div className="flex items-center space-x-2 flex-shrink-0">
                          {transports.length > 1 && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={(e) => {
                                e.stopPropagation();
                                removeTransport(transport.id);
                              }}
                              className="h-8 w-8 p-0 text-red-500 hover:text-red-700 hover:bg-red-50"
                            >
                              <X className="w-4 h-4" />
                            </Button>
                          )}
                          {isExpanded ? (
                            <ChevronDown className="w-5 h-5 text-gray-500" />
                          ) : (
                            <ChevronRight className="w-5 h-5 text-gray-500" />
                          )}
                        </div>
                      </div>
                    </CollapsibleTrigger>

                    {/* Details */}
                    <CollapsibleContent>
                      <div className="px-3 sm:px-4 pb-3 sm:pb-4 space-y-3 sm:space-y-4 border-t border-gray-100">
                        {/* Mode */}
                        <div className="space-y-2">
                          <Label className="text-sm font-medium text-gray-700">
                            Transport Mode
                          </Label>
                          <Select
                            value={transport.mode}
                            onValueChange={(v: any) =>
                              updateTransport(transport.id, { mode: v })
                            }
                          >
                            <SelectTrigger className="min-h-[44px]">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              {transportModes.map((mode) => {
                                const ModeIcon = mode.icon;
                                return (
                                  <SelectItem key={mode.value} value={mode.value}>
                                    <div className="flex items-center space-x-2">
                                      <ModeIcon
                                        className={`w-4 h-4 ${mode.color}`}
                                      />
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
                          <Label className="text-sm font-medium text-gray-700">
                            Distance *
                          </Label>
                          <div className="flex space-x-2">
                            <Input
                              placeholder="5"
                              value={transport.distance}
                              onChange={(e) =>
                                updateTransport(transport.id, {
                                  distance: e.target.value,
                                })
                              }
                              className="min-h-[44px] flex-1"
                            />
                            <Select
                              value={transport.distanceUnit}
                              onValueChange={(v: any) =>
                                updateTransport(transport.id, {
                                  distanceUnit: v,
                                })
                              }
                            >
                              <SelectTrigger className="w-20 min-h-[44px]">
                                <SelectValue />
                              </SelectTrigger>
                              <SelectContent>
                                <SelectItem value="km">km</SelectItem>
                                <SelectItem value="mi">mi</SelectItem>
                              </SelectContent>
                            </Select>
                          </div>
                        </div>

                        {/* Duration + Cost */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                          <div className="space-y-2">
                            <Label className="text-sm font-medium text-gray-700">
                              Duration
                            </Label>
                            <Input
                              placeholder="30 min"
                              value={transport.duration}
                              onChange={(e) =>
                                updateTransport(transport.id, {
                                  duration: e.target.value,
                                })
                              }
                              className="min-h-[44px]"
                            />
                          </div>
                          <div className="space-y-2">
                            <Label className="text-sm font-medium text-gray-700">
                              Cost
                            </Label>
                            <Input
                              placeholder="$25"
                              value={transport.cost}
                              onChange={(e) =>
                                updateTransport(transport.id, {
                                  cost: e.target.value,
                                })
                              }
                              className="min-h-[44px]"
                            />
                          </div>
                        </div>

                        {/* Notes */}
                        <div className="space-y-2">
                          <Label className="text-sm font-medium text-gray-700">
                            Notes
                          </Label>
                          <Input
                            placeholder="Additional details..."
                            value={transport.notes}
                            onChange={(e) =>
                              updateTransport(transport.id, {
                                notes: e.target.value,
                              })
                            }
                            className="min-h-[44px]"
                          />
                        </div>
                      </div>
                    </CollapsibleContent>
                  </div>
                </Collapsible>
              );
            })}
          </div>
        </div>

        {/* Add transport */}
        <div className="flex-shrink-0 p-4 sm:p-6 pt-3 border-t border-gray-200 bg-white">
          <Button
            onClick={addTransport}
            variant="outline"
            className="w-full min-h-[48px] border-dashed border-gray-300 hover:border-gray-400 hover:bg-gray-50"
          >
            <Plus className="w-5 h-5 mr-2" />
            Add Another Transport
          </Button>
        </div>

        {/* Actions */}
        <div className="flex-shrink-0 flex flex-col sm:flex-row gap-3 p-4 sm:p-6 border-t border-gray-200 bg-white">
          <Button
            variant="outline"
            onClick={onClose}
            className="min-h-[48px] flex-1 sm:flex-none sm:px-6"
          >
            Cancel
          </Button>
          <Button
            onClick={handleSave}
            disabled={transports.every((t) => !t.distance.trim())}
            className="min-h-[48px] flex-1 sm:flex-none sm:px-6"
          >
            Save Transport
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
