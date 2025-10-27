/**
 * SSE Manager - STUB IMPLEMENTATION
 * This service was removed as part of Phase 3 Real-time System consolidation
 * Keeping stub to prevent import errors in tests
 * Real-time functionality now uses WebSocket only
 */

export interface ChangeEvent {
  type: string;
  data: any;
}

export interface SseManagerOptions {
  onConnect?: () => void;
  onDisconnect?: () => void;
  onChangeEvent?: (event: ChangeEvent) => void;
  onAgentEvent?: (event: any) => void;
  onPatchEvent?: (event: any) => void;
  onError?: (error: Error) => void;
  executionId?: string;
}

export class SseManager {
  private options: SseManagerOptions;

  constructor(options: SseManagerOptions = {}) {
    this.options = options;
    console.warn('[SseManager STUB] SSE functionality has been removed. Use WebSocket instead.');
  }

  connect(itineraryId: string): void {
    console.warn('[SseManager STUB] connect() called - SSE removed, use WebSocket');
    this.options.onConnect?.();
  }

  disconnect(): void {
    console.warn('[SseManager STUB] disconnect() called - SSE removed, use WebSocket');
    this.options.onDisconnect?.();
  }

  reconnect(): void {
    console.warn('[SseManager STUB] reconnect() called - SSE removed, use WebSocket');
  }
}

// Export singleton instance for backward compatibility
export const sseManager = new SseManager();
