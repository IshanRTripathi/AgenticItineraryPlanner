import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { DisambiguationPanel } from '../DisambiguationPanel';
import type { NodeCandidate } from '../../types/ChatTypes';

describe('DisambiguationPanel', () => {
  const mockCandidates: NodeCandidate[] = [
    {
      id: 'restaurant-1',
      title: 'La Boqueria Restaurant',
      type: 'restaurant',
      day: 1,
      time: '12:00',
      location: 'La Rambla',
      description: 'Famous food market'
    },
    {
      id: 'restaurant-2',
      title: 'El Nacional',
      type: 'restaurant',
      day: 2,
      time: '19:00',
      location: 'Passeig de Gràcia',
      description: 'Historic food hall'
    },
    {
      id: 'museum-1',
      title: 'Picasso Museum',
      type: 'museum',
      day: 1,
      time: '14:00',
      location: 'El Born',
      description: 'Art museum'
    }
  ];

  const mockOnSelect = vi.fn();
  const mockOnCancel = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render all candidates', () => {
    render(<DisambiguationPanel candidates={mockCandidates} onSelect={mockOnSelect} onCancel={mockOnCancel} />);
    
    expect(screen.getByText('🤔 Which one did you mean?')).toBeInTheDocument();
    expect(screen.getByText('La Boqueria Restaurant')).toBeInTheDocument();
    expect(screen.getByText('El Nacional')).toBeInTheDocument();
    expect(screen.getByText('Picasso Museum')).toBeInTheDocument();
  });

  it('should display candidate details', () => {
    render(<DisambiguationPanel candidates={mockCandidates} onSelect={mockOnSelect} onCancel={mockOnCancel} />);
    
    // Check that all candidate details are displayed
    expect(screen.getAllByText('Day 1')).toHaveLength(2); // Two candidates on Day 1
    expect(screen.getByText('Day 2')).toBeInTheDocument();
    
    // Check that candidate types are displayed
    expect(screen.getAllByText('restaurant')).toHaveLength(2); // Two restaurants
    expect(screen.getByText('museum')).toBeInTheDocument();
  });

  it('should call onSelect when a candidate is clicked', () => {
    render(<DisambiguationPanel candidates={mockCandidates} onSelect={mockOnSelect} onCancel={mockOnCancel} />);
    
    const firstCandidate = screen.getByText('La Boqueria Restaurant').closest('.candidate-item');
    fireEvent.click(firstCandidate!);
    
    expect(mockOnSelect).toHaveBeenCalledWith(mockCandidates[0]);
  });

  it('should call onSelect with correct ID for each candidate', () => {
    render(<DisambiguationPanel candidates={mockCandidates} onSelect={mockOnSelect} onCancel={mockOnCancel} />);
    
    const secondCandidate = screen.getByText('El Nacional').closest('.candidate-item');
    fireEvent.click(secondCandidate!);
    
    expect(mockOnSelect).toHaveBeenCalledWith(mockCandidates[1]);
    
    const thirdCandidate = screen.getByText('Picasso Museum').closest('.candidate-item');
    fireEvent.click(thirdCandidate!);
    
    expect(mockOnSelect).toHaveBeenCalledWith(mockCandidates[2]);
  });

  it('should handle empty candidates list', () => {
    render(<DisambiguationPanel candidates={[]} onSelect={mockOnSelect} onCancel={mockOnCancel} />);
    
    expect(screen.getByText('🤔 Which one did you mean?')).toBeInTheDocument();
    expect(screen.getByText('🤔 Which one did you mean?')).toBeInTheDocument();
    expect(screen.getByText('I found multiple options. Please select the one you\'re referring to:')).toBeInTheDocument();
  });

  it('should display type icons correctly', () => {
    render(<DisambiguationPanel candidates={mockCandidates} onSelect={mockOnSelect} onCancel={mockOnCancel} />);
    
    // Check that type icons are displayed (the component uses 📍 as default for restaurant and museum)
    expect(screen.getAllByText('📍')).toHaveLength(3); // default icon for all types
  });

  it('should handle candidates with missing optional fields', () => {
    const candidatesWithMissingFields: NodeCandidate[] = [
      {
        id: 'node-1',
        title: 'Test Node',
        type: 'activity',
        day: 1,
        time: '10:00'
        // Missing location and description
      }
    ];

    render(<DisambiguationPanel candidates={candidatesWithMissingFields} onSelect={mockOnSelect} />);
    
    expect(screen.getByText('Test Node')).toBeInTheDocument();
    expect(screen.getByText('Day 1')).toBeInTheDocument();
  });

  it('should handle candidates with null optional fields', () => {
    const candidatesWithNullFields: NodeCandidate[] = [
      {
        id: 'node-1',
        title: 'Test Node',
        type: 'activity',
        day: 1,
        time: '10:00',
        location: null,
        description: null
      }
    ];

    render(<DisambiguationPanel candidates={candidatesWithNullFields} onSelect={mockOnSelect} />);
    
    expect(screen.getByText('Test Node')).toBeInTheDocument();
    expect(screen.getByText('Day 1')).toBeInTheDocument();
  });

  it('should handle candidates with empty string optional fields', () => {
    const candidatesWithEmptyFields: NodeCandidate[] = [
      {
        id: 'node-1',
        title: 'Test Node',
        type: 'activity',
        day: 1,
        time: '10:00',
        location: '',
        description: ''
      }
    ];

    render(<DisambiguationPanel candidates={candidatesWithEmptyFields} onSelect={mockOnSelect} />);
    
    expect(screen.getByText('Test Node')).toBeInTheDocument();
    expect(screen.getByText('Day 1')).toBeInTheDocument();
  });

  it('should display correct type icons for different node types', () => {
    const candidatesWithDifferentTypes: NodeCandidate[] = [
      {
        id: 'restaurant-1',
        title: 'Restaurant',
        type: 'restaurant',
        day: 1,
        time: '12:00'
      },
      {
        id: 'museum-1',
        title: 'Museum',
        type: 'museum',
        day: 1,
        time: '14:00'
      },
      {
        id: 'hotel-1',
        title: 'Hotel',
        type: 'hotel',
        day: 1,
        time: '20:00'
      },
      {
        id: 'activity-1',
        title: 'Activity',
        type: 'activity',
        day: 1,
        time: '16:00'
      }
    ];

    render(<DisambiguationPanel candidates={candidatesWithDifferentTypes} onSelect={mockOnSelect} onCancel={mockOnCancel} />);
    
    // The component uses different icons based on type mapping
    expect(screen.getByText('🏨')).toBeInTheDocument(); // hotel
    expect(screen.getByText('🎯')).toBeInTheDocument(); // activity
    // restaurant and museum use default 📍 icon
  });
});
