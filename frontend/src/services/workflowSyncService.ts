import { apiClient } from './apiClient';

interface NodePosition {
  nodeId: string;
  x: number;
  y: number;
}

interface SyncQueueItem {
  type: 'position' | 'data';
  nodeId: string;
  data: any;
  timestamp: number;
}

export class WorkflowSyncService {
  private syncQueue: SyncQueueItem[] = [];
  private debounceTimer: NodeJS.Timeout | null = null;
  private readonly debounceDelay = 300;
  private isSyncing = false;

  async syncNodePosition(itineraryId: string, nodeId: string, x: number, y: number): Promise<void> {
    this.addToQueue({ type: 'position', nodeId, data: { x, y }, timestamp: Date.now() });
    this.debouncedSync(itineraryId);
  }

  async syncNodeData(itineraryId: string, nodeId: string, data: any): Promise<void> {
    this.addToQueue({ type: 'data', nodeId, data, timestamp: Date.now() });
    this.debouncedSync(itineraryId);
  }

  private addToQueue(item: SyncQueueItem): void {
    const existingIndex = this.syncQueue.findIndex(
      q => q.nodeId === item.nodeId && q.type === item.type
    );
    if (existingIndex >= 0) {
      this.syncQueue[existingIndex] = item;
    } else {
      this.syncQueue.push(item);
    }
  }

  private debouncedSync(itineraryId: string): void {
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
    this.debounceTimer = setTimeout(() => this.executeSync(itineraryId), this.debounceDelay);
  }

  private async executeSync(itineraryId: string): Promise<void> {
    if (this.isSyncing || this.syncQueue.length === 0) return;

    this.isSyncing = true;
    const itemsToSync = [...this.syncQueue];
    this.syncQueue = [];

    try {
      const positions: NodePosition[] = itemsToSync
        .filter(item => item.type === 'position')
        .map(item => ({ nodeId: item.nodeId, ...item.data }));

      if (positions.length > 0) {
        await this.updateWorkflowPositionsWithRetry(itineraryId, positions);
      }

      for (const item of itemsToSync.filter(i => i.type === 'data')) {
        await this.updateNodeDataWithRetry(itineraryId, item.nodeId, item.data);
      }
    } catch (error) {
      console.error('Sync failed:', error);
      this.syncQueue.push(...itemsToSync);
    } finally {
      this.isSyncing = false;
    }
  }

  private async updateWorkflowPositionsWithRetry(itineraryId: string, positions: NodePosition[], retryCount = 0): Promise<void> {
    try {
      await apiClient.updateWorkflowPositions(itineraryId, positions);
    } catch (error: any) {
      if (error.message?.includes('401') || error.message?.includes('Unauthorized')) {
        console.warn('Auth token expired, attempting to refresh...');
        // Try to refresh token and retry once
        if (retryCount === 0) {
          try {
            // Import auth service dynamically to avoid circular deps
            const { authService } = await import('../services/authService');
            await authService.getIdTokenForceRefresh();
            await this.updateWorkflowPositionsWithRetry(itineraryId, positions, retryCount + 1);
            return;
          } catch (refreshError) {
            console.error('Token refresh failed:', refreshError);
          }
        }
      }
      throw error;
    }
  }

  private async updateNodeDataWithRetry(itineraryId: string, nodeId: string, data: any, retryCount = 0): Promise<void> {
    try {
      await apiClient.updateNodeData(itineraryId, nodeId, data);
    } catch (error: any) {
      if (error.message?.includes('401') || error.message?.includes('Unauthorized')) {
        console.warn('Auth token expired, attempting to refresh...');
        // Try to refresh token and retry once
        if (retryCount === 0) {
          try {
            // Import auth service dynamically to avoid circular deps
            const { authService } = await import('../services/authService');
            await authService.getIdTokenForceRefresh();
            await this.updateNodeDataWithRetry(itineraryId, nodeId, data, retryCount + 1);
            return;
          } catch (refreshError) {
            console.error('Token refresh failed:', refreshError);
          }
        }
      }
      throw error;
    }
  }

  detectConflict(localData: any, remoteData: any): boolean {
    return JSON.stringify(localData) !== JSON.stringify(remoteData);
  }

  clearQueue(): void {
    this.syncQueue = [];
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
      this.debounceTimer = null;
    }
  }
}

export const workflowSyncService = new WorkflowSyncService();
