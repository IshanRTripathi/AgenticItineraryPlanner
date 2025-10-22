/**
 * Change History Panel Component
 * Displays chronological list of changes with user attribution and version navigation
 */

import React, { useState, useEffect, useCallback } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { ScrollArea } from '../ui/scroll-area';
import { 
  History, 
  Clock, 
  User, 
  RotateCcw, 
  ChevronRight,
  Calendar,
  GitBranch,
  RefreshCw,
  Filter,
  Search,
  X
} from 'lucide-react';
import { apiClient } from '../../services/apiClient';
import { useUnifiedItinerary } from '../../contexts/UnifiedItineraryContext';

interface Revision {
  id: string;
  version: number;
  timestamp: string;
  user?: string;
  userId?: string;
  changeCount: number;
  description?: string;
  isCurrent: boolean;
  tags?: string[];
}

interface ChangeHistoryPanelProps {
  className?: string;
  maxHeight?: string;
  showFilters?: boolean;
  onRevisionSelect?: (revision: Revision) => void;
  onJumpToVersion?: (version: number) => void;
}

export function ChangeHistoryPanel({
  className = '',
  maxHeight = '600px',
  showFilters = true,
  onRevisionSelect,
  onJumpToVersion,
}: ChangeHistoryPanelProps) {
  const { state } = useUnifiedItinerary();
  const [revisions, setRevisions] = useState<Revision[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterUser, setFilterUser] = useState<string | null>(null);
  const [selectedRevision, setSelectedRevision] = useState<string | null>(null);

  // Fetch revisions
  const fetchRevisions = useCallback(async () => {
    if (!state.itinerary?.id) return;

    setLoading(true);
    setError(null);

    try {
      const response = await apiClient.getRevisions(state.itinerary.id);
      
      // Transform API response to Revision format
      const transformedRevisions: Revision[] = (response.revisions || []).map((rev: any, index: number) => ({
        id: rev.id || `rev-${index}`,
        version: rev.version || index + 1,
        timestamp: rev.timestamp || rev.createdAt || new Date().toISOString(),
        user: rev.user || rev.userName || 'Unknown User',
        userId: rev.userId,
        changeCount: rev.changeCount || rev.changes?.length || 0,
        description: rev.description || rev.message || 'No description',
        isCurrent: rev.isCurrent || false,
        tags: rev.tags || [],
      }));

      setRevisions(transformedRevisions);
    } catch (err) {
      
      setError('Failed to load change history');
    } finally {
      setLoading(false);
    }
  }, [state.itinerary?.id]);

  // Fetch on mount and when itinerary changes
  useEffect(() => {
    fetchRevisions();
  }, [fetchRevisions]);

  // Filter revisions
  const filteredRevisions = revisions.filter(rev => {
    if (searchTerm && !rev.description?.toLowerCase().includes(searchTerm.toLowerCase())) {
      return false;
    }
    if (filterUser && rev.userId !== filterUser) {
      return false;
    }
    return true;
  });

  // Get unique users for filter
  const uniqueUsers = Array.from(new Set(revisions.map(r => r.userId).filter(Boolean)));

  // Handle revision selection
  const handleRevisionClick = (revision: Revision) => {
    setSelectedRevision(revision.id);
    onRevisionSelect?.(revision);
  };

  // Handle jump to version
  const handleJumpToVersion = async (version: number) => {
    if (!state.itinerary?.id) return;

    try {
      // Call API to rollback to specific version
      await apiClient.rollbackToVersion(state.itinerary.id, version);
      onJumpToVersion?.(version);
      
      // Refresh revisions
      await fetchRevisions();
      
      // Reload page to show changes
      setTimeout(() => window.location.reload(), 500);
    } catch (err) {
      
      alert('Failed to restore version. Please try again.');
    }
  };

  // Format timestamp
  const formatTimestamp = (timestamp: string) => {
    try {
      const date = new Date(timestamp);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffMins = Math.floor(diffMs / 60000);
      const diffHours = Math.floor(diffMs / 3600000);
      const diffDays = Math.floor(diffMs / 86400000);

      if (diffMins < 1) return 'just now';
      if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`;
      if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
      if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
      
      return date.toLocaleDateString();
    } catch {
      return 'Unknown time';
    }
  };

  // Get change type badge color
  const getChangeTypeBadge = (changeCount: number) => {
    if (changeCount === 0) return 'secondary';
    if (changeCount < 5) return 'default';
    if (changeCount < 10) return 'outline';
    return 'destructive';
  };

  if (!state.itinerary?.id) {
    return (
      <Card className={className}>
        <CardContent className="p-6 text-center text-gray-500">
          <History className="w-12 h-12 mx-auto mb-2 opacity-50" />
          <p>No itinerary selected</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <History className="w-5 h-5" />
            <CardTitle>Change History</CardTitle>
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={fetchRevisions}
            disabled={loading}
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </Button>
        </div>

        {/* Filters */}
        {showFilters && (
          <div className="mt-4 space-y-2">
            {/* Search */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                placeholder="Search changes..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-10 py-2 border rounded-md text-sm"
              />
              {searchTerm && (
                <button
                  onClick={() => setSearchTerm('')}
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>

            {/* User Filter */}
            {uniqueUsers.length > 1 && (
              <div className="flex items-center gap-2">
                <Filter className="w-4 h-4 text-gray-400" />
                <select
                  value={filterUser || ''}
                  onChange={(e) => setFilterUser(e.target.value || null)}
                  className="flex-1 px-3 py-2 border rounded-md text-sm"
                >
                  <option value="">All Users</option>
                  {uniqueUsers.map(userId => (
                    <option key={userId} value={userId}>
                      {revisions.find(r => r.userId === userId)?.user || userId}
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>
        )}
      </CardHeader>

      <CardContent>
        {error && (
          <div className="p-4 mb-4 bg-red-50 border border-red-200 rounded-md text-red-700 text-sm">
            {error}
          </div>
        )}

        {loading && revisions.length === 0 ? (
          <div className="flex items-center justify-center py-12">
            <RefreshCw className="w-6 h-6 animate-spin text-gray-400" />
          </div>
        ) : filteredRevisions.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <History className="w-12 h-12 mx-auto mb-2 opacity-50" />
            <p className="text-sm">
              {searchTerm || filterUser ? 'No matching changes found' : 'No change history yet'}
            </p>
          </div>
        ) : (
          <ScrollArea style={{ maxHeight }}>
            <div className="space-y-2">
              {filteredRevisions.map((revision, index) => (
                <div
                  key={revision.id}
                  className={`
                    p-4 border rounded-lg cursor-pointer transition-all
                    ${selectedRevision === revision.id ? 'border-blue-500 bg-blue-50' : 'hover:bg-gray-50'}
                    ${revision.isCurrent ? 'border-green-500 bg-green-50' : ''}
                  `}
                  onClick={() => handleRevisionClick(revision)}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      {/* Version and Current Badge */}
                      <div className="flex items-center gap-2 mb-1">
                        <div className="flex items-center gap-1 text-sm font-medium">
                          <GitBranch className="w-4 h-4" />
                          <span>Version {revision.version}</span>
                        </div>
                        {revision.isCurrent && (
                          <Badge variant="default" className="text-xs">
                            Current
                          </Badge>
                        )}
                        {revision.changeCount > 0 && (
                          <Badge variant={getChangeTypeBadge(revision.changeCount)} className="text-xs">
                            {revision.changeCount} {revision.changeCount === 1 ? 'change' : 'changes'}
                          </Badge>
                        )}
                      </div>

                      {/* Description */}
                      <p className="text-sm text-gray-700 mb-2 line-clamp-2">
                        {revision.description}
                      </p>

                      {/* Metadata */}
                      <div className="flex items-center gap-3 text-xs text-gray-500">
                        <div className="flex items-center gap-1">
                          <User className="w-3 h-3" />
                          <span>{revision.user}</span>
                        </div>
                        <div className="flex items-center gap-1">
                          <Clock className="w-3 h-3" />
                          <span>{formatTimestamp(revision.timestamp)}</span>
                        </div>
                      </div>

                      {/* Tags */}
                      {revision.tags && revision.tags.length > 0 && (
                        <div className="flex items-center gap-1 mt-2">
                          {revision.tags.map(tag => (
                            <Badge key={tag} variant="outline" className="text-xs">
                              {tag}
                            </Badge>
                          ))}
                        </div>
                      )}
                    </div>

                    {/* Actions */}
                    {!revision.isCurrent && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleJumpToVersion(revision.version);
                        }}
                        className="flex-shrink-0"
                      >
                        <RotateCcw className="w-4 h-4 mr-1" />
                        Restore
                      </Button>
                    )}
                  </div>

                  {/* Connector Line */}
                  {index < filteredRevisions.length - 1 && (
                    <div className="ml-2 mt-2 mb-2 border-l-2 border-gray-200 h-4" />
                  )}
                </div>
              ))}
            </div>
          </ScrollArea>
        )}
      </CardContent>
    </Card>
  );
}

