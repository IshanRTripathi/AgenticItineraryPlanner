import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ChangePreview } from '../ChangePreview';
import type { ChangeSet, ChangeOperation } from '../../types/ChatTypes';

describe('ChangePreview', () => {
  const mockChangeSet: ChangeSet = {
    operations: [
      {
        type: 'move',
        nodeId: 'museum-node',
        fromDay: 1,
        toDay: 2,
        fromTime: '10:00',
        toTime: '14:00'
      },
      {
        type: 'add',
        nodeId: 'new-restaurant',
        day: 2,
        time: '19:00',
        node: {
          id: 'new-restaurant',
          title: 'New Restaurant',
          type: 'restaurant',
          location: {
            name: 'Restaurant Location',
            address: '123 Main St',
            coordinates: { lat: 41.3851, lng: 2.1734 }
          },
          timing: {
            startTime: new Date('2024-01-02T19:00:00Z'),
            endTime: new Date('2024-01-02T21:00:00Z'),
            durationMin: 120
          },
          cost: {
            amount: 50.0,
            currency: 'EUR',
            per: 'person'
          },
          details: {
            category: 'restaurant',
            description: 'A great restaurant'
          },
          labels: ['recommended'],
          tips: {
            travel: ['Take the metro'],
            warnings: ['Book in advance']
          },
          links: {
            booking: 'https://example.com/book',
            info: 'https://example.com/info'
          },
          transit: {
            fromPrevious: {
              mode: 'walking',
              durationMin: 15,
              distanceKm: 1.2
            },
            toNext: {
              mode: 'taxi',
              durationMin: 10,
              distanceKm: 2.5
            }
          },
          locked: false,
          bookingRef: null,
          status: 'planned',
          updatedBy: 'user',
          updatedAt: new Date().toISOString()
        }
      }
    ],
    preferences: {
      preserveTiming: true,
      maintainPacing: true,
      respectConstraints: true
    }
  };

  const mockOnApply = vi.fn();
  const mockOnReject = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render change preview with operations', () => {
    render(<ChangePreview changeSet={mockChangeSet} onApply={mockOnApply} onReject={mockOnReject} />);
    
    expect(screen.getByTestId('change-preview')).toBeInTheDocument();
    expect(screen.getByText('Proposed Changes')).toBeInTheDocument();
  });

  it('should display move operation details', () => {
    render(<ChangePreview changeSet={mockChangeSet} onApply={mockOnApply} onReject={mockOnReject} />);
    
    expect(screen.getByText('Move museum-node from Day 1 (10:00) to Day 2 (14:00)')).toBeInTheDocument();
  });

  it('should display add operation details', () => {
    render(<ChangePreview changeSet={mockChangeSet} onApply={mockOnApply} onReject={mockOnReject} />);
    
    expect(screen.getByText('Add New Restaurant to Day 2 at 19:00')).toBeInTheDocument();
  });

  it('should call onApply when apply button is clicked', () => {
    render(<ChangePreview changeSet={mockChangeSet} onApply={mockOnApply} onReject={mockOnReject} />);
    
    const applyButton = screen.getByTestId('apply-button');
    fireEvent.click(applyButton);
    
    expect(mockOnApply).toHaveBeenCalledWith(mockChangeSet);
  });

  it('should call onReject when reject button is clicked', () => {
    render(<ChangePreview changeSet={mockChangeSet} onApply={mockOnApply} onReject={mockOnReject} />);
    
    const rejectButton = screen.getByTestId('reject-button');
    fireEvent.click(rejectButton);
    
    expect(mockOnReject).toHaveBeenCalled();
  });

  it('should handle delete operation', () => {
    const deleteChangeSet: ChangeSet = {
      operations: [
        {
          type: 'delete',
          nodeId: 'old-node',
          day: 1,
          time: '10:00'
        }
      ],
      preferences: {
        preserveTiming: true,
        maintainPacing: true,
        respectConstraints: true
      }
    };

    render(<ChangePreview changeSet={deleteChangeSet} onApply={mockOnApply} onReject={mockOnReject} />);
    
    expect(screen.getByText('Delete old-node from Day 1 at 10:00')).toBeInTheDocument();
  });

  it('should handle update operation', () => {
    const updateChangeSet: ChangeSet = {
      operations: [
        {
          type: 'update',
          nodeId: 'existing-node',
          day: 1,
          time: '10:00',
          updates: {
            title: 'Updated Title',
            cost: {
              amount: 25.0,
              currency: 'EUR',
              per: 'person'
            }
          }
        }
      ],
      preferences: {
        preserveTiming: true,
        maintainPacing: true,
        respectConstraints: true
      }
    };

    render(<ChangePreview changeSet={updateChangeSet} onApply={mockOnApply} onReject={mockOnReject} />);
    
    expect(screen.getByText('Update existing-node on Day 1 at 10:00')).toBeInTheDocument();
  });

  it('should handle empty operations list', () => {
    const emptyChangeSet: ChangeSet = {
      operations: [],
      preferences: {
        preserveTiming: true,
        maintainPacing: true,
        respectConstraints: true
      }
    };

    render(<ChangePreview changeSet={emptyChangeSet} onApply={mockOnApply} onReject={mockOnReject} />);
    
    expect(screen.getByTestId('change-preview')).toBeInTheDocument();
    expect(screen.getByText('No changes to apply')).toBeInTheDocument();
  });

  it('should display operation count', () => {
    render(<ChangePreview changeSet={mockChangeSet} onApply={mockOnApply} onReject={mockOnReject} />);
    
    expect(screen.getByText('2 operations')).toBeInTheDocument();
  });

  it('should display single operation count', () => {
    const singleOperationChangeSet: ChangeSet = {
      operations: [
        {
          type: 'move',
          nodeId: 'single-node',
          fromDay: 1,
          toDay: 2
        }
      ],
      preferences: {
        preserveTiming: true,
        maintainPacing: true,
        respectConstraints: true
      }
    };

    render(<ChangePreview changeSet={singleOperationChangeSet} onApply={mockOnApply} onReject={mockOnReject} />);
    
    expect(screen.getByText('1 operation')).toBeInTheDocument();
  });

  it('should handle operations without time information', () => {
    const changeSetWithoutTime: ChangeSet = {
      operations: [
        {
          type: 'move',
          nodeId: 'node-without-time',
          fromDay: 1,
          toDay: 2
        }
      ],
      preferences: {
        preserveTiming: true,
        maintainPacing: true,
        respectConstraints: true
      }
    };

    render(<ChangePreview changeSet={changeSetWithoutTime} onApply={mockOnApply} onReject={mockOnReject} />);
    
    expect(screen.getByText('Move node-without-time from Day 1 to Day 2')).toBeInTheDocument();
  });

  it('should handle operations without day information', () => {
    const changeSetWithoutDay: ChangeSet = {
      operations: [
        {
          type: 'add',
          nodeId: 'node-without-day',
          time: '10:00',
          node: {
            id: 'node-without-day',
            title: 'Node Without Day',
            type: 'activity',
            location: {
              name: 'Location',
              address: 'Address',
              coordinates: { lat: 41.3851, lng: 2.1734 }
            },
            timing: {
              startTime: new Date('2024-01-01T10:00:00Z'),
              endTime: new Date('2024-01-01T12:00:00Z'),
              durationMin: 120
            },
            cost: {
              amount: 10.0,
              currency: 'EUR',
              per: 'person'
            },
            details: {
              category: 'activity',
              description: 'An activity'
            },
            labels: ['recommended'],
            tips: {
              travel: [],
              warnings: []
            },
            links: {
              booking: null,
              info: null
            },
            transit: {
              fromPrevious: null,
              toNext: null
            },
            locked: false,
            bookingRef: null,
            status: 'planned',
            updatedBy: 'user',
            updatedAt: new Date().toISOString()
          }
        }
      ],
      preferences: {
        preserveTiming: true,
        maintainPacing: true,
        respectConstraints: true
      }
    };

    render(<ChangePreview changeSet={changeSetWithoutDay} onApply={mockOnApply} onReject={mockOnReject} />);
    
    expect(screen.getByText('Add Node Without Day at 10:00')).toBeInTheDocument();
  });
});
