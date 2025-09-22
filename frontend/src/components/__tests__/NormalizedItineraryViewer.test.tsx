import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { NormalizedItineraryViewer } from '../NormalizedItineraryViewer';
import { apiClient } from '../../services/apiClient';
import { NormalizedItinerary } from '../../types/NormalizedItinerary';

// Mock the API client
vi.mock('../../services/apiClient');
const mockApiClient = apiClient as vi.Mocked<typeof apiClient>;

describe('NormalizedItineraryViewer', () => {
  const mockItinerary: NormalizedItinerary = {
    itineraryId: 'test-itinerary',
    version: 1,
    summary: '3-day Barcelona adventure for family',
    currency: 'EUR',
    themes: ['culture', 'food', 'architecture'],
    days: [
      {
        dayNumber: 1,
        date: '2025-06-01',
        location: 'Barcelona',
        nodes: [
          {
            id: 'n_attraction_1',
            type: 'attraction',
            title: 'Sagrada Familia',
            location: {
              name: 'Sagrada Familia',
              address: 'Carrer de Mallorca, 401, 08013 Barcelona, Spain',
              coordinates: { lat: 41.4036, lng: 2.1744 }
            },
            timing: {
              startTime: '2025-06-01T09:00:00',
              endTime: '2025-06-01T11:00:00',
              durationMin: 120
            },
            cost: {
              amount: 25,
              currency: 'EUR',
              per: 'person'
            },
            details: {
              rating: 4.8,
              category: 'monument',
              tags: ['iconic', 'architecture', 'gaudi']
            },
            tips: {
              bestTime: ['morning', 'early afternoon'],
              travel: ['metro line 2 or 5 to Sagrada Familia'],
              warnings: ['book tickets in advance']
            },
            locked: false
          },
          {
            id: 'n_meal_1',
            type: 'meal',
            title: 'Lunch at local restaurant',
            timing: {
              startTime: '2025-06-01T13:00:00',
              endTime: '2025-06-01T14:30:00',
              durationMin: 90
            },
            cost: {
              amount: 35,
              currency: 'EUR',
              per: 'person'
            },
            locked: false
          }
        ],
        edges: [
          {
            from: 'n_attraction_1',
            to: 'n_meal_1',
            transitInfo: {
              mode: 'walking',
              durationMin: 15
            }
          }
        ],
        totals: {
          distanceKm: 5.2,
          cost: 60,
          durationHr: 3.5
        }
      },
      {
        dayNumber: 2,
        date: '2025-06-02',
        location: 'Barcelona',
        nodes: [
          {
            id: 'n_attraction_2',
            type: 'attraction',
            title: 'Park Güell',
            locked: false
          }
        ],
        edges: [],
        totals: {
          distanceKm: 3.1,
          cost: 20,
          durationHr: 3
        }
      }
    ],
    settings: {
      autoApply: false,
      defaultScope: 'day'
    },
    agents: {
      planner: { status: 'completed' },
      enrichment: { status: 'completed' }
    }
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  test('should render loading state initially', () => {
    // Arrange
    mockApiClient.getItineraryJson.mockImplementation(() => new Promise(() => {})); // Never resolves

    // Act
    render(<NormalizedItineraryViewer itineraryId="test-itinerary" />);

    // Assert
    expect(screen.getByText('Loading itinerary...')).toBeInTheDocument();
  });

  test('should render itinerary data when loaded successfully', async () => {
    // Arrange
    mockApiClient.getItineraryJson.mockResolvedValue(mockItinerary);

    // Act
    render(<NormalizedItineraryViewer itineraryId="test-itinerary" />);

    // Assert
    await waitFor(() => {
      expect(screen.getByText('3-day Barcelona adventure for family')).toBeInTheDocument();
    });

    expect(screen.getByText('Version: 1 | Currency: EUR | Themes: culture, food, architecture')).toBeInTheDocument();
    expect(screen.getByText('Day 1 - 2025-06-01')).toBeInTheDocument();
    expect(screen.getByText('Sagrada Familia')).toBeInTheDocument();
    expect(screen.getByText('Lunch at local restaurant')).toBeInTheDocument();
  });

  test('should render error state when API call fails', async () => {
    // Arrange
    mockApiClient.getItineraryJson.mockRejectedValue(new Error('API Error'));

    // Act
    render(<NormalizedItineraryViewer itineraryId="test-itinerary" />);

    // Assert
    await waitFor(() => {
      expect(screen.getByText('Error: API Error')).toBeInTheDocument();
    });

    expect(screen.getByText('Retry')).toBeInTheDocument();
  });

  test('should allow day selection', async () => {
    // Arrange
    mockApiClient.getItineraryJson.mockResolvedValue(mockItinerary);

    // Act
    render(<NormalizedItineraryViewer itineraryId="test-itinerary" />);

    await waitFor(() => {
      expect(screen.getByText('Day 1 - 2025-06-01')).toBeInTheDocument();
    });

    // Change day selection
    const daySelector = screen.getByDisplayValue('Day 1 - 2025-06-01');
    fireEvent.change(daySelector, { target: { value: '2' } });

    // Assert
    await waitFor(() => {
      expect(screen.getByText('Day 2 - 2025-06-02')).toBeInTheDocument();
    });
    expect(screen.getByText('Park Güell')).toBeInTheDocument();
  });

  test('should display node details correctly', async () => {
    // Arrange
    mockApiClient.getItineraryJson.mockResolvedValue(mockItinerary);

    // Act
    render(<NormalizedItineraryViewer itineraryId="test-itinerary" />);

    await waitFor(() => {
      expect(screen.getByText('Sagrada Familia')).toBeInTheDocument();
    });

    // Assert
    expect(screen.getByText('attraction')).toBeInTheDocument();
    expect(screen.getAllByText((content, element) => {
      return element?.textContent?.includes('Time:') && 
             element?.textContent?.includes('9:00:00 am') && 
             element?.textContent?.includes('11:00:00 am') &&
             element?.textContent?.includes('120 min');
    })).toHaveLength(1);
    expect(screen.getAllByText((content, element) => {
      return element?.textContent?.includes('Cost:') && 
             element?.textContent?.includes('25') && 
             element?.textContent?.includes('EUR') &&
             element?.textContent?.includes('per person');
    })).toHaveLength(1);
    expect(screen.getAllByText((content, element) => {
      return element?.textContent?.includes('Location:') && 
             element?.textContent?.includes('Sagrada Familia');
    })).toHaveLength(1);
  });

  test('should display locked and booked nodes correctly', async () => {
    // Arrange
    const itineraryWithLockedNode = {
      ...mockItinerary,
      days: [
        {
          ...mockItinerary.days[0],
          nodes: [
            {
              ...mockItinerary.days[0].nodes[0],
              locked: true,
              bookingRef: 'BK123456'
            }
          ]
        }
      ]
    };
    mockApiClient.getItineraryJson.mockResolvedValue(itineraryWithLockedNode);

    // Act
    render(<NormalizedItineraryViewer itineraryId="test-itinerary" />);

    await waitFor(() => {
      expect(screen.getByText('Sagrada Familia')).toBeInTheDocument();
    });

    // Assert
    expect(screen.getByText('Locked')).toBeInTheDocument();
    expect(screen.getByText('Booked: BK123456')).toBeInTheDocument();
  });

  test('should display transportation edges', async () => {
    // Arrange
    mockApiClient.getItineraryJson.mockResolvedValue(mockItinerary);

    // Act
    render(<NormalizedItineraryViewer itineraryId="test-itinerary" />);

    await waitFor(() => {
      expect(screen.getByText('Transportation')).toBeInTheDocument();
    });

    // Assert
    expect(screen.getAllByText((content, element) => {
      return element?.textContent?.includes('From:') && 
             element?.textContent?.includes('n_attraction_1') && 
             element?.textContent?.includes('→') &&
             element?.textContent?.includes('To:') &&
             element?.textContent?.includes('n_meal_1');
    })).toHaveLength(1);
    expect(screen.getAllByText((content, element) => {
      return element?.textContent?.includes('Mode:') && 
             element?.textContent?.includes('walking') && 
             element?.textContent?.includes('15 min');
    })).toHaveLength(1);
  });

  test('should display agent status', async () => {
    // Arrange
    mockApiClient.getItineraryJson.mockResolvedValue(mockItinerary);

    // Act
    render(<NormalizedItineraryViewer itineraryId="test-itinerary" />);

    await waitFor(() => {
      expect(screen.getByText('Agent Status')).toBeInTheDocument();
    });

    // Assert
    expect(screen.getAllByText((content, element) => {
      return element?.textContent?.includes('planner') && element?.textContent?.includes('Agent');
    })).toHaveLength(1);
    expect(screen.getAllByText((content, element) => {
      return element?.textContent?.includes('enrichment') && element?.textContent?.includes('Agent');
    })).toHaveLength(1);
    expect(screen.getAllByText((content, element) => {
      return element?.textContent?.includes('Status:') && element?.textContent?.includes('completed');
    })).toHaveLength(2);
  });

  test('should handle propose change button click', async () => {
    // Arrange
    mockApiClient.getItineraryJson.mockResolvedValue(mockItinerary);
    mockApiClient.proposeChanges.mockResolvedValue({
      proposed: mockItinerary,
      diff: { added: [], removed: [], updated: [] },
      previewVersion: 1
    });

    // Mock window.alert
    window.alert = vi.fn();

    // Act
    render(<NormalizedItineraryViewer itineraryId="test-itinerary" />);

    await waitFor(() => {
      expect(screen.getByText('Propose Change')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Propose Change'));

    // Assert
    await waitFor(() => {
      expect(mockApiClient.proposeChanges).toHaveBeenCalledWith('test-itinerary', expect.any(Object));
    });
    expect(window.alert).toHaveBeenCalledWith('Changes proposed successfully! Check console for details.');
  });

  test('should handle apply change button click', async () => {
    // Arrange
    mockApiClient.getItineraryJson.mockResolvedValue(mockItinerary);
    mockApiClient.applyChanges.mockResolvedValue({
      toVersion: 2,
      diff: { added: [], removed: [], updated: [] }
    });

    // Mock window.alert
    window.alert = vi.fn();

    // Act
    render(<NormalizedItineraryViewer itineraryId="test-itinerary" />);

    await waitFor(() => {
      expect(screen.getByText('Apply Change')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Apply Change'));

    // Assert
    await waitFor(() => {
      expect(mockApiClient.applyChanges).toHaveBeenCalledWith('test-itinerary', expect.any(Object));
    });
    expect(window.alert).toHaveBeenCalledWith('Changes applied successfully! Check console for details.');
    // Should reload itinerary after apply
    expect(mockApiClient.getItineraryJson).toHaveBeenCalledTimes(2); // Once initially, once after apply
  });

  test('should handle undo change button click', async () => {
    // Arrange
    mockApiClient.getItineraryJson.mockResolvedValue(mockItinerary);
    mockApiClient.undoChanges.mockResolvedValue({
      toVersion: 1,
      diff: { added: [], removed: [], updated: [] }
    });

    // Mock window.alert
    window.alert = vi.fn();

    // Act
    render(<NormalizedItineraryViewer itineraryId="test-itinerary" />);

    await waitFor(() => {
      expect(screen.getByText('Undo Change')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Undo Change'));

    // Assert
    await waitFor(() => {
      expect(mockApiClient.undoChanges).toHaveBeenCalledWith('test-itinerary', expect.any(Object));
    });
    expect(window.alert).toHaveBeenCalledWith('Changes undone successfully! Check console for details.');
    // Should reload itinerary after undo
    expect(mockApiClient.getItineraryJson).toHaveBeenCalledTimes(2); // Once initially, once after undo
  });

  test('should handle mock booking button click', async () => {
    // Arrange
    mockApiClient.getItineraryJson.mockResolvedValue(mockItinerary);
    mockApiClient.mockBook.mockResolvedValue({
      itineraryId: 'test-itinerary',
      nodeId: 'n_attraction_1',
      bookingRef: 'BK123456',
      locked: true,
      message: 'Booking completed successfully'
    });

    // Mock window.alert
    window.alert = vi.fn();

    // Act
    render(<NormalizedItineraryViewer itineraryId="test-itinerary" />);

    await waitFor(() => {
      expect(screen.getByText('Mock Booking')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Mock Booking'));

    // Assert
    await waitFor(() => {
      expect(mockApiClient.mockBook).toHaveBeenCalledWith({
        itineraryId: 'test-itinerary',
        nodeId: 'n_attraction_1'
      });
    });
    expect(window.alert).toHaveBeenCalledWith('Booking completed! Reference: BK123456');
    // Should reload itinerary after booking
    expect(mockApiClient.getItineraryJson).toHaveBeenCalledTimes(2); // Once initially, once after booking
  });

  test('should handle API errors gracefully', async () => {
    // Arrange
    mockApiClient.getItineraryJson.mockResolvedValue(mockItinerary);
    mockApiClient.proposeChanges.mockRejectedValue(new Error('Propose failed'));

    // Mock window.alert
    window.alert = vi.fn();

    // Act
    render(<NormalizedItineraryViewer itineraryId="test-itinerary" />);

    await waitFor(() => {
      expect(screen.getByText('Propose Change')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Propose Change'));

    // Assert
    await waitFor(() => {
      expect(window.alert).toHaveBeenCalledWith('Failed to propose changes: Propose failed');
    });
  });

  test('should handle retry button click', async () => {
    // Arrange
    mockApiClient.getItineraryJson
      .mockRejectedValueOnce(new Error('API Error'))
      .mockResolvedValueOnce(mockItinerary);

    // Act
    render(<NormalizedItineraryViewer itineraryId="test-itinerary" />);

    await waitFor(() => {
      expect(screen.getByText('Retry')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Retry'));

    // Assert
    await waitFor(() => {
      expect(screen.getByText('3-day Barcelona adventure for family')).toBeInTheDocument();
    });
    expect(mockApiClient.getItineraryJson).toHaveBeenCalledTimes(2);
  });

  test('should display day totals correctly', async () => {
    // Arrange
    mockApiClient.getItineraryJson.mockResolvedValue(mockItinerary);

    // Act
    render(<NormalizedItineraryViewer itineraryId="test-itinerary" />);

    await waitFor(() => {
      expect(screen.getByText('Day 1 - 2025-06-01')).toBeInTheDocument();
    });

    // Assert
    expect(screen.getByText('60 EUR')).toBeInTheDocument(); // Total cost
    expect(screen.getByText('5.2 km')).toBeInTheDocument(); // Total distance
    expect(screen.getByText('3.5 hours')).toBeInTheDocument(); // Total duration
  });

  test('should display node tips and warnings', async () => {
    // Arrange
    mockApiClient.getItineraryJson.mockResolvedValue(mockItinerary);

    // Act
    render(<NormalizedItineraryViewer itineraryId="test-itinerary" />);

    await waitFor(() => {
      expect(screen.getByText('Sagrada Familia')).toBeInTheDocument();
    });

    // Assert
    expect(screen.getAllByText((content, element) => {
      return element?.textContent?.includes('Best time:') && element?.textContent?.includes('morning, early afternoon');
    })).toHaveLength(1);
    expect(screen.getAllByText((content, element) => {
      return element?.textContent?.includes('Warnings:') && element?.textContent?.includes('book tickets in advance');
    })).toHaveLength(1);
  });
});
