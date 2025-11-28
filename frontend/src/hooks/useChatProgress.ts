import { useState, useEffect, useRef } from 'react';

export interface ChatProgressEvent {
  type: 'progress' | 'complete' | 'error';
  stage: string;
  message: string;
  progress: number;
  timestamp: number;
}

const API_BASE_URL = (import.meta as any).env?.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

/**
 * Hook for receiving real-time chat progress updates via Server-Sent Events (SSE).
 * 
 * Usage:
 * ```tsx
 * const progress = useChatProgress(sessionId);
 * 
 * {progress && (
 *   <div>
 *     {progress.message} ({progress.progress}%)
 *   </div>
 * )}
 * ```
 */
export function useChatProgress(sessionId: string | null) {
  const [progress, setProgress] = useState<ChatProgressEvent | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    if (!sessionId) {
      return;
    }

    console.log('[useChatProgress] Connecting to SSE for session:', sessionId);

    // Create EventSource connection
    const eventSource = new EventSource(`${API_BASE_URL}/chat/progress/${sessionId}`);
    eventSourceRef.current = eventSource;

    // Handle connection open
    eventSource.onopen = () => {
      console.log('[useChatProgress] SSE connection opened');
      setIsConnected(true);
      setError(null);
    };

    // Handle connection event
    eventSource.addEventListener('connected', (e) => {
      console.log('[useChatProgress] Connected:', e.data);
    });

    // Handle progress events
    eventSource.addEventListener('progress', (e) => {
      try {
        const event: ChatProgressEvent = JSON.parse(e.data);
        console.log('[useChatProgress] Progress:', event);
        setProgress(event);

        // Auto-close on complete or error
        if (event.type === 'complete' || event.type === 'error') {
          setTimeout(() => {
            eventSource.close();
          }, 1000);
        }
      } catch (err) {
        console.error('[useChatProgress] Failed to parse progress event:', err);
      }
    });

    // Handle errors
    eventSource.onerror = (err) => {
      console.error('[useChatProgress] SSE error:', err);
      setIsConnected(false);
      setError('Connection lost');
      eventSource.close();
    };

    // Cleanup on unmount
    return () => {
      console.log('[useChatProgress] Closing SSE connection');
      eventSource.close();
      eventSourceRef.current = null;
    };
  }, [sessionId]);

  return {
    progress,
    isConnected,
    error,
    reset: () => setProgress(null),
  };
}
