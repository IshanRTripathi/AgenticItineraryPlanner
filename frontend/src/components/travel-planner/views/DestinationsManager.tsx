import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../../ui/button';
import { Badge } from '../../ui/badge';
import { Input } from '../../ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../ui/select';
import { Menu, Plus, Minus, Search, FileText, Trash2, MapPin } from 'lucide-react';
import { DestinationManagerProps, CURRENCIES, ErrorBoundary, TransportDetails } from '../shared/types';
import { TransportConnector } from '../shared/TransportConnector';

export function DestinationsManager({ 
  destinations, 
  currency, 
  showNotes, 
  onUpdate, 
  onAdd, 
  onRemove, 
  onCurrencyChange, 
  onToggleNotes,
  onUpdateTransport
}: DestinationManagerProps) {
  const { t } = useTranslation();
  const [newDestination, setNewDestination] = useState('');
  const [transportConnections, setTransportConnections] = useState<Record<string, TransportDetails[]>>({});

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

  const handleTransportUpdate = (fromId: string, toId: string, transports: TransportDetails[]) => {
    const connectionKey = `${fromId}-${toId}`;
    setTransportConnections(prev => ({
      ...prev,
      [connectionKey]: transports
    }));
    onUpdateTransport(fromId, toId, transports);
  };

  const getTransportConnections = (fromId: string, toId: string) => {
    const connectionKey = `${fromId}-${toId}`;
    return transportConnections[connectionKey] || [];
  };

  // Show empty state if no destinations
  if (destinations.length === 0) {
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
                      {t('destinations.costIn', { currency: curr, symbol: curr === 'EUR' ? '€' : curr === 'USD' ? '$' : '£' })}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Badge variant="secondary">
                {t('destinations.nightsPlanned', { current: totalNights, max: maxNights })}
              </Badge>
            </div>
          </div>

          {/* Empty State */}
          <div className="text-center py-12">
            <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <MapPin className="w-8 h-8 text-gray-400" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-2">No destinations added yet</h3>
            <p className="text-gray-600 mb-6">Start building your trip by adding destinations below.</p>
          </div>
        </div>
      </ErrorBoundary>
    );
  }

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
                    {t('destinations.costIn', { currency: curr, symbol: curr === 'EUR' ? '€' : curr === 'USD' ? '$' : '£' })}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Badge variant="secondary">
              {t('destinations.nightsPlanned', { current: totalNights, max: maxNights })}
            </Badge>
            <Button variant="ghost" size="sm" onClick={onToggleNotes}>
              <Menu className="w-4 h-4 mr-2" />
              {t('destinations.showNotes')}
            </Button>
          </div>
        </div>

        {/* Destinations List */}
        <div className="space-y-4">
          {destinations.map((destination, index) => (
            <div key={destination.id}>
              {/* Destination Card */}
              <div className="bg-white border rounded-lg p-4 shadow-sm">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center space-x-3">
                    <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center text-blue-600 font-semibold text-sm">
                      {index + 1}
                    </div>
                    <div>
                      <h3 className="font-semibold text-lg">{destination.name}</h3>
                      <div className="flex items-center space-x-4 text-sm text-gray-600">
                        <div className="flex items-center space-x-1">
                          <span>{destination.nights} night{destination.nights !== 1 ? 's' : ''}</span>
                          <div className="flex space-x-1">
                            <Button 
                              variant="outline" 
                              size="sm" 
                              className="w-6 h-6 p-0"
                              onClick={() => onUpdate(destination.id, { nights: Math.max(0, destination.nights - 1) })}
                            >
                              <Minus className="w-3 h-3" />
                            </Button>
                            <Button 
                              variant="outline" 
                              size="sm" 
                              className="w-6 h-6 p-0"
                              onClick={() => onUpdate(destination.id, { nights: destination.nights + 1 })}
                            >
                              <Plus className="w-3 h-3" />
                            </Button>
                          </div>
                        </div>
                        <Button 
                          variant={destination.sleeping ? "default" : "outline"} 
                          size="sm" 
                          className="h-6 px-2 text-xs"
                          onClick={() => onUpdate(destination.id, { sleeping: !destination.sleeping })}
                        >
                          {destination.sleeping ? 'Sleeping' : 'No Sleep'}
                        </Button>
                        <Button 
                          variant={destination.discover ? "default" : "outline"} 
                          size="sm" 
                          className="h-6 px-2 text-xs bg-amber-100 border-amber-300 text-amber-700 hover:bg-amber-200"
                          onClick={() => onUpdate(destination.id, { discover: !destination.discover })}
                        >
                          Discover
                        </Button>
                      </div>
                    </div>
                  </div>
                  <Button 
                    variant="ghost" 
                    size="sm" 
                    onClick={() => onRemove(destination.id)}
                    className="text-red-500 hover:text-red-700"
                  >
                    <Trash2 className="w-4 h-4" />
                  </Button>
                </div>
                
                {showNotes && (
                  <div className="mt-3">
                    <Input 
                      placeholder="Add notes for this destination..." 
                      value={destination.notes}
                      onChange={(e) => onUpdate(destination.id, { notes: e.target.value })}
                      className="text-sm"
                    />
                  </div>
                )}
              </div>

              {/* Transport Connector */}
              {index < destinations.length - 1 && (
                <TransportConnector
                  fromDestination={destination.name}
                  toDestination={destinations[index + 1].name}
                  transports={getTransportConnections(destination.id, destinations[index + 1].id)}
                  onUpdate={(transports) => handleTransportUpdate(destination.id, destinations[index + 1].id, transports)}
                />
              )}
            </div>
          ))}
        </div>

        {/* Add New Destination */}
        <div className="mt-6 p-4 border-2 border-dashed border-gray-300 rounded-lg">
          <div className="flex items-center space-x-2">
            <Input 
              placeholder="Add new destination…" 
              value={newDestination}
              onChange={(e) => setNewDestination(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && addDestination()}
              className="flex-1"
            />
            <Button onClick={addDestination}>
              <Plus className="w-4 h-4 mr-2" />
              Add Destination
            </Button>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex justify-center space-x-4 mt-6">
          <Button variant="outline">
            <FileText className="w-4 h-4 mr-2" />
            Collection
          </Button>
        </div>
      </div>
    </ErrorBoundary>
  );
}
