import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { ProgressiveItineraryGeneration } from '../../components/ProgressiveItineraryGeneration';
import { useSseConnection } from '../../hooks/useSseConnection';

// Mock the SSE hook
vi.mock('../../hooks/useSseConnection', () => ({
  useSseConnection: vi.fn(),
}));

// Mock the SSE manager
vi.mock('../../services/sseManager', () => ({
  SseManager: vi.fn().mockImplementation(() => ({
    connect: vi.fn(),
    disconnect: vi.fn(),
  })),
}));

describe('Progressive Itinerary Generation Component', () => {
  const mockItineraryId = 'it_paris_4day_123';
  const mockExecutionId = 'exec_paris_4day_456';
  const mockOnComplete = vi.fn();
  const mockOnError = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  describe('Day 1 Generation Flow', () => {
    it('should show skeleton generation progress for Day 1', async () => {
      // Given
      const mockSseConnection = {
        isConnected: true,
        connect: vi.fn(),
        disconnect: vi.fn(),
      };

      (useSseConnection as any).mockReturnValue(mockSseConnection);

      // When
      render(
        <ProgressiveItineraryGeneration
          itineraryId={mockItineraryId}
          executionId={mockExecutionId}
          onComplete={mockOnComplete}
          onError={mockOnError}
        />
      );

      // Then
      expect(screen.getByText('Generating your 4-day Paris itinerary...')).toBeInTheDocument();
      expect(screen.getByText('Day 1: Creating structure...')).toBeInTheDocument();
    });

    it('should show activity generation progress for Day 1', async () => {
      // Given
      const mockSseConnection = {
        isConnected: true,
        connect: vi.fn(),
        disconnect: vi.fn(),
      };

      (useSseConnection as any).mockReturnValue(mockSseConnection);

      // When
      render(
        <ProgressiveItineraryGeneration
          itineraryId={mockItineraryId}
          executionId={mockExecutionId}
          onComplete={mockOnComplete}
          onError={mockOnError}
        />
      );

      // Simulate Day 1 skeleton completion and activity start
      await waitFor(() => {
        expect(screen.getByText('Day 1: Adding activities...')).toBeInTheDocument();
      });
    });

    it('should show meal generation progress for Day 1', async () => {
      // Given
      const mockSseConnection = {
        isConnected: true,
        connect: vi.fn(),
        disconnect: vi.fn(),
      };

      (useSseConnection as any).mockReturnValue(mockSseConnection);

      // When
      render(
        <ProgressiveItineraryGeneration
          itineraryId={mockItineraryId}
          executionId={mockExecutionId}
          onComplete={mockOnComplete}
          onError={mockOnError}
        />
      );

      // Simulate Day 1 activity completion and meal start
      await waitFor(() => {
        expect(screen.getByText('Day 1: Adding meals...')).toBeInTheDocument();
      });
    });

    it('should show transport generation progress for Day 1', async () => {
      // Given
      const mockSseConnection = {
        isConnected: true,
        connect: vi.fn(),
        disconnect: vi.fn(),
      };

      (useSseConnection as any).mockReturnValue(mockSseConnection);

      // When
      render(
        <ProgressiveItineraryGeneration
          itineraryId={mockItineraryId}
          executionId={mockExecutionId}
          onComplete={mockOnComplete}
          onError={mockOnError}
        />
      );

      // Simulate Day 1 meal completion and transport start
      await waitFor(() => {
        expect(screen.getByText('Day 1: Adding transport...')).toBeInTheDocument();
      });
    });

    it('should show enrichment progress for Day 1', async () => {
      // Given
      const mockSseConnection = {
        isConnected: true,
        connect: vi.fn(),
        disconnect: vi.fn(),
      };

      (useSseConnection as any).mockReturnValue(mockSseConnection);

      // When
      render(
        <ProgressiveItineraryGeneration
          itineraryId={mockItineraryId}
          executionId={mockExecutionId}
          onComplete={mockOnComplete}
          onError={mockOnError}
        />
      );

      // Simulate Day 1 transport completion and enrichment start
      await waitFor(() => {
        expect(screen.getByText('Day 1: Adding photos and tips...')).toBeInTheDocument();
      });
    });

    it('should show Day 1 completion and Day 2 start', async () => {
      // Given
      const mockSseConnection = {
        isConnected: true,
        connect: vi.fn(),
        disconnect: vi.fn(),
      };

      (useSseConnection as any).mockReturnValue(mockSseConnection);

      // When
      render(
        <ProgressiveItineraryGeneration
          itineraryId={mockItineraryId}
          executionId={mockExecutionId}
          onComplete={mockOnComplete}
          onError={mockOnError}
        />
      );

      // Simulate Day 1 completion and Day 2 start
      await waitFor(() => {
        expect(screen.getByText('Day 1 completed! ✅')).toBeInTheDocument();
        expect(screen.getByText('Day 2: Creating structure...')).toBeInTheDocument();
      });
    });
  });

  describe('Complete 4-Day Generation Flow', () => {
    it('should show progress for all 4 days', async () => {
      // Given
      const mockSseConnection = {
        isConnected: true,
        connect: vi.fn(),
        disconnect: vi.fn(),
      };

      (useSseConnection as any).mockReturnValue(mockSseConnection);

      // When
      render(
        <ProgressiveItineraryGeneration
          itineraryId={mockItineraryId}
          executionId={mockExecutionId}
          onComplete={mockOnComplete}
          onError={mockOnError}
        />
      );

      // Simulate complete 4-day generation
      await waitFor(() => {
        expect(screen.getByText('Day 1: Creating structure...')).toBeInTheDocument();
      });

      await waitFor(() => {
        expect(screen.getByText('Day 2: Creating structure...')).toBeInTheDocument();
      });

      await waitFor(() => {
        expect(screen.getByText('Day 3: Creating structure...')).toBeInTheDocument();
      });

      await waitFor(() => {
        expect(screen.getByText('Day 4: Creating structure...')).toBeInTheDocument();
      });
    });

    it('should show final completion message', async () => {
      // Given
      const mockSseConnection = {
        isConnected: true,
        connect: vi.fn(),
        disconnect: vi.fn(),
      };

      (useSseConnection as any).mockReturnValue(mockSseConnection);

      // When
      render(
        <ProgressiveItineraryGeneration
          itineraryId={mockItineraryId}
          executionId={mockExecutionId}
          onComplete={mockOnComplete}
          onError={mockOnError}
        />
      );

      // Simulate final completion
      await waitFor(() => {
        expect(screen.getByText('🎉 Your 4-day Paris itinerary is ready!')).toBeInTheDocument();
      });

      expect(mockOnComplete).toHaveBeenCalled();
    });
  });

  describe('Error Handling', () => {
    it('should show error message when generation fails', async () => {
      // Given
      const mockSseConnection = {
        isConnected: true,
        connect: vi.fn(),
        disconnect: vi.fn(),
      };

      (useSseConnection as any).mockReturnValue(mockSseConnection);

      // When
      render(
        <ProgressiveItineraryGeneration
          itineraryId={mockItineraryId}
          executionId={mockExecutionId}
          onComplete={mockOnComplete}
          onError={mockOnError}
        />
      );

      // Simulate error
      await waitFor(() => {
        expect(screen.getByText('❌ Error generating itinerary')).toBeInTheDocument();
      });

      expect(mockOnError).toHaveBeenCalled();
    });

    it('should show retry message when generation recovers', async () => {
      // Given
      const mockSseConnection = {
        isConnected: true,
        connect: vi.fn(),
        disconnect: vi.fn(),
      };

      (useSseConnection as any).mockReturnValue(mockSseConnection);

      // When
      render(
        <ProgressiveItineraryGeneration
          itineraryId={mockItineraryId}
          executionId={mockExecutionId}
          onComplete={mockOnComplete}
          onError={mockOnError}
        />
      );

      // Simulate error and recovery
      await waitFor(() => {
        expect(screen.getByText('🔄 Retrying generation...')).toBeInTheDocument();
      });
    });
  });

  describe('SSE Connection Status', () => {
    it('should show connection status', async () => {
      // Given
      const mockSseConnection = {
        isConnected: true,
        connect: vi.fn(),
        disconnect: vi.fn(),
      };

      (useSseConnection as any).mockReturnValue(mockSseConnection);

      // When
      render(
        <ProgressiveItineraryGeneration
          itineraryId={mockItineraryId}
          executionId={mockExecutionId}
          onComplete={mockOnComplete}
          onError={mockOnError}
        />
      );

      // Then
      expect(screen.getByText('🟢 Connected')).toBeInTheDocument();
    });

    it('should show disconnected status', async () => {
      // Given
      const mockSseConnection = {
        isConnected: false,
        connect: vi.fn(),
        disconnect: vi.fn(),
      };

      (useSseConnection as any).mockReturnValue(mockSseConnection);

      // When
      render(
        <ProgressiveItineraryGeneration
          itineraryId={mockItineraryId}
          executionId={mockExecutionId}
          onComplete={mockOnComplete}
          onError={mockOnError}
        />
      );

      // Then
      expect(screen.getByText('🔴 Disconnected')).toBeInTheDocument();
    });
  });

  describe('Progress Indicators', () => {
    it('should show progress bar for each day', async () => {
      // Given
      const mockSseConnection = {
        isConnected: true,
        connect: vi.fn(),
        disconnect: vi.fn(),
      };

      (useSseConnection as any).mockReturnValue(mockSseConnection);

      // When
      render(
        <ProgressiveItineraryGeneration
          itineraryId={mockItineraryId}
          executionId={mockExecutionId}
          onComplete={mockOnComplete}
          onError={mockOnError}
        />
      );

      // Then
      expect(screen.getByRole('progressbar')).toBeInTheDocument();
    });

    it('should show percentage progress', async () => {
      // Given
      const mockSseConnection = {
        isConnected: true,
        connect: vi.fn(),
        disconnect: vi.fn(),
      };

      (useSseConnection as any).mockReturnValue(mockSseConnection);

      // When
      render(
        <ProgressiveItineraryGeneration
          itineraryId={mockItineraryId}
          executionId={mockExecutionId}
          onComplete={mockOnComplete}
          onError={mockOnError}
        />
      );

      // Then
      expect(screen.getByText('25%')).toBeInTheDocument();
    });
  });

  describe('Real-time Updates', () => {
    it('should update progress in real-time', async () => {
      // Given
      const mockSseConnection = {
        isConnected: true,
        connect: vi.fn(),
        disconnect: vi.fn(),
      };

      (useSseConnection as any).mockReturnValue(mockSseConnection);

      // When
      render(
        <ProgressiveItineraryGeneration
          itineraryId={mockItineraryId}
          executionId={mockExecutionId}
          onComplete={mockOnComplete}
          onError={mockOnError}
        />
      );

      // Simulate real-time progress updates
      await waitFor(() => {
        expect(screen.getByText('25%')).toBeInTheDocument();
      });

      await waitFor(() => {
        expect(screen.getByText('50%')).toBeInTheDocument();
      });

      await waitFor(() => {
        expect(screen.getByText('75%')).toBeInTheDocument();
      });

      await waitFor(() => {
        expect(screen.getByText('100%')).toBeInTheDocument();
      });
    });

    it('should show current phase description', async () => {
      // Given
      const mockSseConnection = {
        isConnected: true,
        connect: vi.fn(),
        disconnect: vi.fn(),
      };

      (useSseConnection as any).mockReturnValue(mockSseConnection);

      // When
      render(
        <ProgressiveItineraryGeneration
          itineraryId={mockItineraryId}
          executionId={mockExecutionId}
          onComplete={mockOnComplete}
          onError={mockOnError}
        />
      );

      // Then
      expect(screen.getByText('Creating day structure...')).toBeInTheDocument();
    });
  });
});


