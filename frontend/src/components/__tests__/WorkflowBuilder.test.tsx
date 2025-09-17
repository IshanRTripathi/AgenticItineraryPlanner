import React from 'react';
import { render, screen } from '@testing-library/react';
import { WorkflowBuilder } from '../WorkflowBuilder';
import { TripData } from '../../App';

// Mock ReactFlow since it requires DOM elements
jest.mock('reactflow', () => ({
  ReactFlowProvider: ({ children }: { children: React.ReactNode }) => <div data-testid="react-flow-provider">{children}</div>,
  default: () => <div data-testid="react-flow" />,
  useNodesState: () => [[], jest.fn(), jest.fn()],
  useEdgesState: () => [[], jest.fn(), jest.fn()],
  useReactFlow: () => ({
    project: jest.fn(),
    getViewport: jest.fn(() => ({ x: 0, y: 0, zoom: 1 }))
  }),
  addEdge: jest.fn(),
  Controls: () => <div data-testid="controls" />,
  Background: () => <div data-testid="background" />,
  MiniMap: () => <div data-testid="minimap" />,
  Handle: () => <div data-testid="handle" />,
  Position: {
    Left: 'left',
    Right: 'right',
    Top: 'top',
    Bottom: 'bottom'
  }
}));

const mockTripData: TripData = {
  id: '1',
  destination: 'New Delhi',
  dates: { start: '2024-03-15', end: '2024-03-17' },
  budget: 50000,
  partySize: 2,
  themes: ['heritage', 'culture'],
  dietaryRestrictions: ['vegetarian'],
  walkingTolerance: 3,
  pace: 3,
  stayType: 'Hotel',
  transport: 'Flight',
  itinerary: {
    days: [
      {
        day: 1,
        date: '2024-03-15',
        activities: []
      }
    ]
  }
};

describe('WorkflowBuilder', () => {
  const mockOnSave = jest.fn();
  const mockOnCancel = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders without crashing', () => {
    render(
      <WorkflowBuilder
        tripData={mockTripData}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    expect(screen.getByText('Workflow Builder')).toBeInTheDocument();
    expect(screen.getByText('New Delhi')).toBeInTheDocument();
  });

  it('displays day tabs', () => {
    render(
      <WorkflowBuilder
        tripData={mockTripData}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    expect(screen.getByText('Day 1')).toBeInTheDocument();
  });

  it('shows add node buttons in toolbar', () => {
    render(
      <WorkflowBuilder
        tripData={mockTripData}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    expect(screen.getByText('Attraction')).toBeInTheDocument();
    expect(screen.getByText('Meal')).toBeInTheDocument();
    expect(screen.getByText('Transit')).toBeInTheDocument();
    expect(screen.getByText('Hotel')).toBeInTheDocument();
    expect(screen.getByText('FreeTime')).toBeInTheDocument();
  });

  it('has timeline toggle button', () => {
    render(
      <WorkflowBuilder
        tripData={mockTripData}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    expect(screen.getByText('Hide Timeline')).toBeInTheDocument();
  });

  it('has control buttons in header', () => {
    render(
      <WorkflowBuilder
        tripData={mockTripData}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    expect(screen.getByText('Back')).toBeInTheDocument();
    expect(screen.getByText('Auto Arrange')).toBeInTheDocument();
    expect(screen.getByText('Reset Demo')).toBeInTheDocument();
    expect(screen.getByText('Apply to Itinerary')).toBeInTheDocument();
  });
});

describe('WorkflowBuilder integration', () => {
  const mockOnSave = jest.fn();
  const mockOnCancel = jest.fn();

  it('calls onCancel when back button is clicked', () => {
    render(
      <WorkflowBuilder
        tripData={mockTripData}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    const backButton = screen.getByText('Back');
    backButton.click();

    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });
});