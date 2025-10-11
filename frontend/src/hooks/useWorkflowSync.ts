import { useCallback, useEffect, useState } from 'react';
import { workflowSyncService } from '../services/workflowSyncService';
import { useUnifiedItinerary } from '../contexts/UnifiedItineraryContext';

interface SyncStatus {
  syncing: boolean;
  lastSync: number | null;
  error: string | null;
}

export const useWorkflowSync = (itineraryId: string) => {
  const { updateWorkflowSettings } = useUnifiedItinerary();
  const [syncStatus, setSyncStatus] = useState<SyncStatus>({
    syncing: false,
    lastSync: null,
    error: null
  });

  // Sync node position to backend
  const syncNodePosition = useCallback(async (nodeId: string, x: number, y: number) => {
    try {
      setSyncStatus(prev => ({ ...prev, syncing: true, error: null }));
      await workflowSyncService.syncNodePosition(itineraryId, nodeId, x, y);
      setSyncStatus({ syncing: false, lastSync: Date.now(), error: null });
    } catch (error) {
      console.error('Failed to sync node position:', error);
      setSyncStatus(prev => ({ 
        ...prev, 
        syncing: false, 
        error: 'Failed to sync position changes' 
      }));
    }
  }, [itineraryId]);

  // Sync node data to backend
  const syncNodeData = useCallback(async (nodeId: string, data: any) => {
    try {
      setSyncStatus(prev => ({ ...prev, syncing: true, error: null }));
      await workflowSyncService.syncNodeData(itineraryId, nodeId, data);
      setSyncStatus({ syncing: false, lastSync: Date.now(), error: null });
    } catch (error) {
      console.error('Failed to sync node data:', error);
      setSyncStatus(prev => ({ 
        ...prev, 
        syncing: false, 
        error: 'Failed to sync data changes' 
      }));
    }
  }, [itineraryId]);

  // Sync workflow settings (positions, layout, etc.)
  const syncWorkflowSettings = useCallback(async (settings: any) => {
    try {
      setSyncStatus(prev => ({ ...prev, syncing: true, error: null }));
      await updateWorkflowSettings(settings);
      setSyncStatus({ syncing: false, lastSync: Date.now(), error: null });
    } catch (error) {
      console.error('Failed to sync workflow settings:', error);
      setSyncStatus(prev => ({ 
        ...prev, 
        syncing: false, 
        error: 'Failed to sync workflow settings' 
      }));
    }
  }, [updateWorkflowSettings]);

  // Retry sync after error
  const retrySync = useCallback(() => {
    setSyncStatus({ syncing: false, lastSync: null, error: null });
  }, []);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      workflowSyncService.clearQueue();
    };
  }, []);

  return {
    syncStatus,
    syncNodePosition,
    syncNodeData,
    syncWorkflowSettings,
    retrySync
  };
};
