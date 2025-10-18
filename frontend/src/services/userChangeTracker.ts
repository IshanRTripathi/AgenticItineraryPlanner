/**
 * Service to track user changes in workflow nodes
 * Integrates with chat changes to highlight modified nodes
 */

export interface UserChange {
  nodeId: string;
  changeType: 'added' | 'modified' | 'moved' | 'deleted';
  timestamp: string;
  originalData?: any;
  newData?: any;
  source: 'chat' | 'workflow' | 'manual';
}

class UserChangeTracker {
  private changes: Map<string, UserChange[]> = new Map();
  private listeners: Set<(changes: UserChange[]) => void> = new Set();

  /**
   * Track a user change to a node
   */
  trackChange(change: UserChange): void {
    const nodeChanges = this.changes.get(change.nodeId) || [];
    nodeChanges.push(change);
    this.changes.set(change.nodeId, nodeChanges);
    
    // Notify listeners
    this.notifyListeners();
    
    console.log(`[UserChangeTracker] Tracked ${change.changeType} for node ${change.nodeId}:`, change);
  }

  /**
   * Get all changes for a specific node
   */
  getNodeChanges(nodeId: string): UserChange[] {
    return this.changes.get(nodeId) || [];
  }

  /**
   * Get the latest change for a node
   */
  getLatestChange(nodeId: string): UserChange | null {
    const changes = this.getNodeChanges(nodeId);
    return changes.length > 0 ? changes[changes.length - 1] : null;
  }

  /**
   * Check if a node has been modified by the user
   */
  isNodeModified(nodeId: string): boolean {
    return this.changes.has(nodeId) && this.changes.get(nodeId)!.length > 0;
  }

  /**
   * Get all modified node IDs
   */
  getModifiedNodeIds(): string[] {
    return Array.from(this.changes.keys());
  }

  /**
   * Clear changes for a specific node
   */
  clearNodeChanges(nodeId: string): void {
    this.changes.delete(nodeId);
    this.notifyListeners();
  }

  /**
   * Clear all changes
   */
  clearAllChanges(): void {
    this.changes.clear();
    this.notifyListeners();
  }

  /**
   * Subscribe to change notifications
   */
  subscribe(listener: (changes: UserChange[]) => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  /**
   * Notify all listeners of changes
   */
  private notifyListeners(): void {
    const allChanges = Array.from(this.changes.values()).flat();
    this.listeners.forEach(listener => listener(allChanges));
  }

  /**
   * Process chat changes and track them as user modifications
   */
  processChatChanges(chatResponse: any): void {
    if (!chatResponse.diff || !chatResponse.applied) {
      return;
    }

    const diff = chatResponse.diff;
    
    // Process added items
    if (diff.added && Array.isArray(diff.added)) {
      diff.added.forEach((item: any) => {
        if (item.id) {
          this.trackChange({
            nodeId: item.id,
            changeType: 'added',
            timestamp: new Date().toISOString(),
            newData: item,
            source: 'chat'
          });
        }
      });
    }

    // Process updated items
    if (diff.updated && Array.isArray(diff.updated)) {
      diff.updated.forEach((item: any) => {
        if (item.id) {
          this.trackChange({
            nodeId: item.id,
            changeType: 'modified',
            timestamp: new Date().toISOString(),
            newData: item,
            source: 'chat'
          });
        }
      });
    }

    // Process removed items
    if (diff.removed && Array.isArray(diff.removed)) {
      diff.removed.forEach((item: any) => {
        if (item.id) {
          this.trackChange({
            nodeId: item.id,
            changeType: 'deleted',
            timestamp: new Date().toISOString(),
            originalData: item,
            source: 'chat'
          });
        }
      });
    }
  }

  /**
   * Process workflow changes (when user modifies nodes directly in workflow)
   */
  processWorkflowChange(nodeId: string, changeType: 'added' | 'modified' | 'moved' | 'deleted', originalData?: any, newData?: any): void {
    this.trackChange({
      nodeId,
      changeType,
      timestamp: new Date().toISOString(),
      originalData,
      newData,
      source: 'workflow'
    });
  }
}

// Export singleton instance
export const userChangeTracker = new UserChangeTracker();

// Export types
export type { UserChange };

