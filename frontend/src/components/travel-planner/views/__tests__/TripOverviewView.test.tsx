import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { TripOverviewView } from '../TripOverviewView';

// Mock the weather service
jest.mock('../../../services/weatherService', () => ({
  weatherService: {
    getWeatherForCities: jest.fn(),
  },
}));

// Mock the itinerary utils
jest.mock('../../../utils/itineraryUtils', () => ({
  extractCitiesFromItinerary: jest.fn(),
  getTotalActivities: jest.fn(() => 5),
  getTransportModes: jest.fn(() => ({ car: 2, plane: 1 })),
}));

const mockWeatherService = require('../../../services/weatherService').weatherService;
const mockItineraryUtils = require('../../../utils/itineraryUtils');

describe('TripOverviewView Weather API Logic', () => {
  const mockTripData = {
    id: 'test-trip',
    destination: 'Barcelona',
    itinerary: {
      days: [
        { location: 'Barcelona', nodes: [] },
        { location: 'Madrid', nodes: [] }
      ]
    }
  };

  const mockAgentStatuses = [];
  const mockOnShare = jest.fn();
  const mockOnExportPDF = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockWeatherService.getWeatherForCities.mockResolvedValue([
      { city: 'Barcelona', temperature: 22, condition: 'sunny' },
      { city: 'Madrid', temperature: 20, condition: 'cloudy' }
    ]);
  });

  it('should use destinations directly when available', async () => {
    const destinations = [
      { id: '1', name: 'Barcelona', nights: 3, sleeping: true, notes: '' },
      { id: '2', name: 'Madrid', nights: 2, sleeping: true, notes: '' }
    ];

    render(
      <TripOverviewView
        tripData={mockTripData}
        agentStatuses={mockAgentStatuses}
        onShare={mockOnShare}
        onExportPDF={mockOnExportPDF}
        destinations={destinations}
      />
    );

    // Wait for the weather API call
    await waitFor(() => {
      expect(mockWeatherService.getWeatherForCities).toHaveBeenCalledTimes(1);
      const callArgs = mockWeatherService.getWeatherForCities.mock.calls[0][0];
      expect(callArgs).toHaveLength(2);
      expect(callArgs).toContain('Barcelona');
      expect(callArgs).toContain('Madrid');
    });

    // Should NOT call extractCitiesFromItinerary when destinations are available
    expect(mockItineraryUtils.extractCitiesFromItinerary).not.toHaveBeenCalled();
  });

  it('should fallback to itinerary extraction when no destinations available', async () => {
    mockItineraryUtils.extractCitiesFromItinerary.mockReturnValue(['Barcelona', 'Madrid']);

    render(
      <TripOverviewView
        tripData={mockTripData}
        agentStatuses={mockAgentStatuses}
        onShare={mockOnShare}
        onExportPDF={mockOnExportPDF}
        destinations={[]}
      />
    );

    // Wait for the weather API call
    await waitFor(() => {
      expect(mockItineraryUtils.extractCitiesFromItinerary).toHaveBeenCalledWith(mockTripData);
    });

    expect(mockWeatherService.getWeatherForCities).toHaveBeenCalledWith(['Barcelona', 'Madrid']);
  });

  it('should handle empty destinations array', async () => {
    mockItineraryUtils.extractCitiesFromItinerary.mockReturnValue(['Barcelona']);

    render(
      <TripOverviewView
        tripData={mockTripData}
        agentStatuses={mockAgentStatuses}
        onShare={mockOnShare}
        onExportPDF={mockOnExportPDF}
        destinations={[]}
      />
    );

    await waitFor(() => {
      expect(mockItineraryUtils.extractCitiesFromItinerary).toHaveBeenCalledWith(mockTripData);
    });
  });

  it('should handle undefined destinations', async () => {
    mockItineraryUtils.extractCitiesFromItinerary.mockReturnValue(['Barcelona']);

    render(
      <TripOverviewView
        tripData={mockTripData}
        agentStatuses={mockAgentStatuses}
        onShare={mockOnShare}
        onExportPDF={mockOnExportPDF}
        // destinations prop not provided (undefined)
      />
    );

    await waitFor(() => {
      expect(mockItineraryUtils.extractCitiesFromItinerary).toHaveBeenCalledWith(mockTripData);
    });
  });

  it('should extract unique city names from destinations and avoid duplicate API calls', async () => {
    const destinations = [
      { id: '1', name: 'Barcelona', nights: 3, sleeping: true, notes: '' },
      { id: '2', name: 'Madrid', nights: 2, sleeping: true, notes: '' },
      { id: '3', name: 'Barcelona', nights: 1, sleeping: false, notes: '' }, // Duplicate
      { id: '4', name: 'Seville', nights: 2, sleeping: true, notes: '' }
    ];

    render(
      <TripOverviewView
        tripData={mockTripData}
        agentStatuses={mockAgentStatuses}
        onShare={mockOnShare}
        onExportPDF={mockOnExportPDF}
        destinations={destinations}
      />
    );

    // Should extract unique city names: ['Barcelona', 'Madrid', 'Seville']
    // Note: Set doesn't guarantee order, so we check that it's called with 3 unique cities
    await waitFor(() => {
      expect(mockWeatherService.getWeatherForCities).toHaveBeenCalledTimes(1);
      const callArgs = mockWeatherService.getWeatherForCities.mock.calls[0][0];
      expect(callArgs).toHaveLength(3);
      expect(callArgs).toContain('Barcelona');
      expect(callArgs).toContain('Madrid');
      expect(callArgs).toContain('Seville');
    });
  });
});
