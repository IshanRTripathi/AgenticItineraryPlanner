import React, { useState } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '../ui/dialog';
import { Button } from '../ui/button';
import { Alert, AlertDescription } from '../ui/alert';
import { Badge } from '../ui/badge';
import { AlertTriangle, CheckCircle, Info } from 'lucide-react';
import { apiClient } from '../../services/apiClient';

interface RevisionRestoreProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  itineraryId: string;
  version: number;
  versionInfo: {
    timestamp: string;
    user: string;
    changeCount: number;
    description: string;
  };
}

export const RevisionRestore: React.FC<RevisionRestoreProps> = ({
  isOpen,
  onClose,
  onSuccess,
  itineraryId,
  version,
  versionInfo
}) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [previewChanges, setPreviewChanges] = useState<any>(null);

  const handlePreview = async () => {
    try {
      setLoading(true);
      const changes = await apiClient.getRevision(itineraryId, version);
      setPreviewChanges(changes);
    } catch (err) {
      setError('Failed to load preview');
    } finally {
      setLoading(false);
    }
  };

  const handleRestore = async () => {
    try {
      setLoading(true);
      setError(null);
      
      await apiClient.rollbackToRevision(itineraryId, version);
      
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to restore version');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-orange-500" />
            Restore Version {version}
          </DialogTitle>
          <DialogDescription>
            This will restore your itinerary to a previous version
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <Alert>
            <Info className="h-4 w-4" />
            <AlertDescription>
              Your current version will be saved before restoring. You can always undo this action.
            </AlertDescription>
          </Alert>

          <div className="space-y-2">
            <h4 className="font-medium">Version Details</h4>
            <div className="grid grid-cols-2 gap-2 text-sm">
              <div className="text-gray-600">Created:</div>
              <div>{new Date(versionInfo.timestamp).toLocaleString()}</div>
              
              <div className="text-gray-600">By:</div>
              <div>{versionInfo.user}</div>
              
              <div className="text-gray-600">Changes:</div>
              <div>{versionInfo.changeCount}</div>
            </div>
            <p className="text-sm text-gray-700 mt-2">{versionInfo.description}</p>
          </div>

          {previewChanges && (
            <div className="space-y-2">
              <h4 className="font-medium">Preview</h4>
              <div className="bg-gray-50 p-3 rounded text-xs max-h-40 overflow-y-auto">
                <pre>{JSON.stringify(previewChanges, null, 2)}</pre>
              </div>
            </div>
          )}

          {error && (
            <Alert variant="destructive">
              <AlertTriangle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={loading}>
            Cancel
          </Button>
          {!previewChanges && (
            <Button variant="outline" onClick={handlePreview} disabled={loading}>
              Preview Changes
            </Button>
          )}
          <Button onClick={handleRestore} disabled={loading}>
            {loading ? 'Restoring...' : 'Restore Version'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
