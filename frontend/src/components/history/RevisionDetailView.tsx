/**
 * Revision Detail View Component
 * Displays detailed information about a specific revision with diff viewer
 */

import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Separator } from '../ui/separator';
import { 
  X, 
  RotateCcw, 
  Clock, 
  User, 
  GitBranch,
  FileText,
  AlertCircle,
  CheckCircle,
  Download,
  Share2
} from 'lucide-react';
import { DiffViewer } from '../diff/DiffViewer';
import { createObjectDiff } from '../../utils/diffUtils';
import { apiClient } from '../../services/apiClient';

interface RevisionDetailViewProps {
  revisionId: string;
  itineraryId: string;
  onClose?: () => void;
  onRestore?: () => void;
  className?: string;
}

interface RevisionDetail {
  id: string;
  version: number;
  timestamp: string;
  user?: string;
  userId?: string;
  description?: string;
  changeCount: number;
  changes?: any[];
  beforeState?: any;
  afterState?: any;
  isCurrent: boolean;
  tags?: string[];
  metadata?: Record<string, any>;
}

export function RevisionDetailView({
  revisionId,
  itineraryId,
  onClose,
  onRestore,
  className = '',
}: RevisionDetailViewProps) {
  const [revision, setRevision] = useState<RevisionDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [restoring, setRestoring] = useState(false);

  // Fetch revision details
  useEffect(() => {
    const fetchRevisionDetail = async () => {
      setLoading(true);
      setError(null);

      try {
        const response = await apiClient.getRevisionDetail(itineraryId, revisionId);
        
        // Transform API response
        const detail: RevisionDetail = {
          id: response.id || revisionId,
          version: response.version || 1,
          timestamp: response.timestamp || response.createdAt || new Date().toISOString(),
          user: response.user || response.userName || 'Unknown User',
          userId: response.userId,
          description: response.description || response.message || 'No description',
          changeCount: response.changeCount || response.changes?.length || 0,
          changes: response.changes || [],
          beforeState: response.beforeState || response.before,
          afterState: response.afterState || response.after,
          isCurrent: response.isCurrent || false,
          tags: response.tags || [],
          metadata: response.metadata || {},
        };

        setRevision(detail);
      } catch (err) {
        console.error('Failed to fetch revision detail:', err);
        setError('Failed to load revision details');
      } finally {
        setLoading(false);
      }
    };

    fetchRevisionDetail();
  }, [itineraryId, revisionId]);

  // Handle restore
  const handleRestore = async () => {
    if (!revision || revision.isCurrent) return;

    const confirmed = window.confirm(
      `Are you sure you want to restore to version ${revision.version}? This will create a new version with the restored state.`
    );

    if (!confirmed) return;

    setRestoring(true);
    try {
      await apiClient.rollbackToVersion(itineraryId, revision.version);
      onRestore?.();
      
      // Reload page to show changes
      setTimeout(() => window.location.reload(), 500);
    } catch (err) {
      console.error('Failed to restore revision:', err);
      alert('Failed to restore version. Please try again.');
    } finally {
      setRestoring(false);
    }
  };

  // Format timestamp
  const formatTimestamp = (timestamp: string) => {
    try {
      const date = new Date(timestamp);
      return date.toLocaleString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return 'Unknown time';
    }
  };

  // Create diff sections from changes
  const getDiffSections = () => {
    if (!revision) return [];

    // If we have before/after states, create object diff
    if (revision.beforeState && revision.afterState) {
      return [createObjectDiff('Changes', revision.beforeState, revision.afterState)];
    }

    // If we have changes array, transform to diff sections
    if (revision.changes && revision.changes.length > 0) {
      return [{
        title: 'Changes',
        changes: revision.changes.map((change: any, index: number) => ({
          type: change.type || 'modified',
          path: change.path || `change-${index}`,
          label: change.label || change.description || change.path,
          oldValue: change.oldValue || change.before,
          newValue: change.newValue || change.after || change.value,
        })),
      }];
    }

    return [];
  };

  if (loading) {
    return (
      <Card className={className}>
        <CardContent className="p-12 text-center">
          <div className="animate-spin w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full mx-auto mb-4" />
          <p className="text-gray-500">Loading revision details...</p>
        </CardContent>
      </Card>
    );
  }

  if (error || !revision) {
    return (
      <Card className={className}>
        <CardContent className="p-12 text-center">
          <AlertCircle className="w-12 h-12 text-red-500 mx-auto mb-4" />
          <p className="text-red-600 mb-4">{error || 'Revision not found'}</p>
          {onClose && (
            <Button onClick={onClose} variant="outline">
              Close
            </Button>
          )}
        </CardContent>
      </Card>
    );
  }

  const diffSections = getDiffSections();

  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-2">
              <GitBranch className="w-5 h-5" />
              <CardTitle>Version {revision.version}</CardTitle>
              {revision.isCurrent && (
                <Badge variant="default">Current</Badge>
              )}
            </div>
            <CardDescription>{revision.description}</CardDescription>
          </div>
          {onClose && (
            <Button variant="ghost" size="sm" onClick={onClose}>
              <X className="w-4 h-4" />
            </Button>
          )}
        </div>

        {/* Metadata */}
        <div className="flex flex-wrap items-center gap-4 mt-4 text-sm text-gray-600">
          <div className="flex items-center gap-1">
            <User className="w-4 h-4" />
            <span>{revision.user}</span>
          </div>
          <div className="flex items-center gap-1">
            <Clock className="w-4 h-4" />
            <span>{formatTimestamp(revision.timestamp)}</span>
          </div>
          <div className="flex items-center gap-1">
            <FileText className="w-4 h-4" />
            <span>{revision.changeCount} {revision.changeCount === 1 ? 'change' : 'changes'}</span>
          </div>
        </div>

        {/* Tags */}
        {revision.tags && revision.tags.length > 0 && (
          <div className="flex items-center gap-2 mt-3">
            {revision.tags.map(tag => (
              <Badge key={tag} variant="outline">
                {tag}
              </Badge>
            ))}
          </div>
        )}

        <Separator className="mt-4" />
      </CardHeader>

      <CardContent className="space-y-4">
        {/* Status Message */}
        {revision.isCurrent ? (
          <div className="flex items-center gap-2 p-3 bg-green-50 border border-green-200 rounded-lg text-green-700">
            <CheckCircle className="w-5 h-5" />
            <span className="text-sm font-medium">This is the current version</span>
          </div>
        ) : (
          <div className="flex items-center gap-2 p-3 bg-blue-50 border border-blue-200 rounded-lg text-blue-700">
            <AlertCircle className="w-5 h-5" />
            <span className="text-sm">This is a previous version. You can restore it to make it current.</span>
          </div>
        )}

        {/* Diff Viewer */}
        {diffSections.length > 0 ? (
          <div className="border rounded-lg overflow-hidden">
            <DiffViewer
              sections={diffSections}
              viewMode="side-by-side"
              showUnchanged={false}
            />
          </div>
        ) : (
          <div className="p-8 text-center text-gray-500 border rounded-lg">
            <FileText className="w-12 h-12 mx-auto mb-2 opacity-50" />
            <p>No detailed change information available</p>
          </div>
        )}

        {/* Additional Metadata */}
        {revision.metadata && Object.keys(revision.metadata).length > 0 && (
          <div className="border rounded-lg p-4">
            <h4 className="text-sm font-medium mb-2">Additional Information</h4>
            <dl className="space-y-1 text-sm">
              {Object.entries(revision.metadata).map(([key, value]) => (
                <div key={key} className="flex gap-2">
                  <dt className="text-gray-500 min-w-[120px]">{key}:</dt>
                  <dd className="text-gray-900">{String(value)}</dd>
                </div>
              ))}
            </dl>
          </div>
        )}

        {/* Actions */}
        <div className="flex items-center justify-between pt-4 border-t">
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled>
              <Download className="w-4 h-4 mr-1" />
              Export
            </Button>
            <Button variant="outline" size="sm" disabled>
              <Share2 className="w-4 h-4 mr-1" />
              Share
            </Button>
          </div>

          {!revision.isCurrent && (
            <Button
              onClick={handleRestore}
              disabled={restoring}
              className="flex items-center gap-1"
            >
              <RotateCcw className="w-4 h-4" />
              {restoring ? 'Restoring...' : 'Restore This Version'}
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
