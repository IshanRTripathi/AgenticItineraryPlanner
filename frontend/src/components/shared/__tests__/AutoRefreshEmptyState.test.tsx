import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AutoRefreshEmptyState } from '../AutoRefreshEmptyState';

// Mock the useAutoRefresh hook
jest.mock('../../../hooks/useAutoRefresh', () => ({
  useAutoRefresh: jest.fn(() => ({
    countdown: 3,
    isRefreshing: false,
    startRefresh: jest.fn(),
    stopRefresh: jest.fn()
  }))
}));

describe('AutoRefreshEmptyState', () => {
  const mockOnRefresh = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders with default props', () => {
    render(<AutoRefreshEmptyState onRefresh={mockOnRefresh} />);
    
    expect(screen.getByText('No itinerary data available yet')).toBeInTheDocument();
    expect(screen.getByText('Your personalized itinerary will appear here once planning is complete.')).toBeInTheDocument();
    expect(screen.getByText('Auto-refreshing in')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('seconds')).toBeInTheDocument();
  });

  it('renders with custom title and description', () => {
    render(
      <AutoRefreshEmptyState 
        onRefresh={mockOnRefresh}
        title="Custom Title"
        description="Custom description"
      />
    );
    
    expect(screen.getByText('Custom Title')).toBeInTheDocument();
    expect(screen.getByText('Custom description')).toBeInTheDocument();
  });

  it('calls onRefresh when refresh button is clicked', () => {
    render(<AutoRefreshEmptyState onRefresh={mockOnRefresh} />);
    
    const refreshButton = screen.getByText('Refresh Now');
    fireEvent.click(refreshButton);
    
    expect(mockOnRefresh).toHaveBeenCalledTimes(1);
  });

  it('shows refreshing state when isRefreshing is true', () => {
    const { useAutoRefresh } = require('../../../hooks/useAutoRefresh');
    useAutoRefresh.mockReturnValue({
      countdown: 3,
      isRefreshing: true,
      startRefresh: jest.fn(),
      stopRefresh: jest.fn()
    });

    render(<AutoRefreshEmptyState onRefresh={mockOnRefresh} />);
    
    expect(screen.getByText('Refreshing...')).toBeInTheDocument();
    expect(screen.getByText('Refresh Now')).toBeDisabled();
  });

  it('calls stopRefresh when stop button is clicked', () => {
    const mockStopRefresh = jest.fn();
    const { useAutoRefresh } = require('../../../hooks/useAutoRefresh');
    useAutoRefresh.mockReturnValue({
      countdown: 3,
      isRefreshing: false,
      startRefresh: jest.fn(),
      stopRefresh: mockStopRefresh
    });

    render(<AutoRefreshEmptyState onRefresh={mockOnRefresh} />);
    
    const stopButton = screen.getByText('Stop Auto-refresh');
    fireEvent.click(stopButton);
    
    expect(mockStopRefresh).toHaveBeenCalledTimes(1);
  });

  it('hides refresh button when showRefreshButton is false', () => {
    render(
      <AutoRefreshEmptyState 
        onRefresh={mockOnRefresh}
        showRefreshButton={false}
      />
    );
    
    expect(screen.queryByText('Refresh Now')).not.toBeInTheDocument();
    expect(screen.queryByText('Stop Auto-refresh')).not.toBeInTheDocument();
  });
});
