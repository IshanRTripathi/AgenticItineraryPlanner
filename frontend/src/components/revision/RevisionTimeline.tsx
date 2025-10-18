import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { ScrollArea } from '../ui/scroll-area';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Badge } from '../ui/badge';
import { Clock, User, Search, Filter } from 'lucide-react';
import { apiClient } from '../../services/apiClient';

interface Revision {
  id: string;
  version: number;
  timestamp: string;
  user: string;
  changeCount: number;
  changeType: 'major' | 'minor' | 'patch';
  description: string;
}

interface RevisionTimelineProps {
  itineraryId: string;
  onVersionSelect: (version: number) => void;
  selectedVersion?: number;
}

export const RevisionTimeline: React.FC<RevisionTimelineProps> = ({
  itineraryId,
  onVersionSelect,
  selectedVersion
}) => {
  const [revisions, setRevisions] = useState<Revision[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState<string>('all');

  useEffect(() => {
    loadRevisions();
  }, [itineraryId]);

  const loadRevisions = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getRevisions(itineraryId);
      setRevisions(data);
    } catch (error) {
      console.error('Failed to load revisions:', error);
    } finally {
      setLoading(false);
    }
  };

  const filteredRevisions = revisions.filter(rev => {
    const matchesSearch = rev.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         rev.user.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesFilter = filterType === 'all' || rev.changeType === filterType;
    return matchesSearch && matchesFilter;
  });

  const getChangeTypeColor = (type: string) => {
    switch (type) {
      case 'major': return 'bg-red-100 text-red-800';
      case 'minor': return 'bg-yellow-100 text-yellow-800';
      case 'patch': return 'bg-green-100 text-green-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Version History</CardTitle>
        <div className="flex gap-2 mt-2">
          <div className="relative flex-1">
            <Search className="absolute left-2 top-2.5 h-4 w-4 text-gray-400" />
            <Input
              placeholder="Search revisions..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-8"
            />
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setFilterType(filterType === 'all' ? 'major' : 'all')}
          >
            <Filter className="h-4 w-4 mr-1" />
            {filterType === 'all' ? 'All' : 'Major'}
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        <ScrollArea className="h-96">
          {loading ? (
            <div className="text-center py-8 text-gray-500">Loading...</div>
          ) : filteredRevisions.length === 0 ? (
            <div className="text-center py-8 text-gray-500">No revisions found</div>
          ) : (
            <div className="space-y-4">
              {filteredRevisions.map((revision, index) => (
                <div
                  key={revision.id}
                  className={`relative pl-6 pb-4 ${
                    index !== filteredRevisions.length - 1 ? 'border-l-2 border-gray-200' : ''
                  }`}
                >
                  <div
                    className={`absolute left-0 top-0 w-3 h-3 rounded-full -ml-1.5 ${
                      selectedVersion === revision.version
                        ? 'bg-blue-500 ring-4 ring-blue-100'
                        : 'bg-gray-300'
                    }`}
                  />
                  <div
                    className={`p-3 rounded-lg border cursor-pointer transition-colors ${
                      selectedVersion === revision.version
                        ? 'bg-blue-50 border-blue-200'
                        : 'bg-white hover:bg-gray-50'
                    }`}
                    onClick={() => onVersionSelect(revision.version)}
                  >
                    <div className="flex items-start justify-between mb-2">
                      <div>
                        <div className="font-medium">Version {revision.version}</div>
                        <div className="text-sm text-gray-500 flex items-center gap-2 mt-1">
                          <Clock className="h-3 w-3" />
                          {new Date(revision.timestamp).toLocaleString()}
                        </div>
                      </div>
                      <Badge className={getChangeTypeColor(revision.changeType)}>
                        {revision.changeType}
                      </Badge>
                    </div>
                    <p className="text-sm text-gray-700 mb-2">{revision.description}</p>
                    <div className="flex items-center gap-4 text-xs text-gray-500">
                      <div className="flex items-center gap-1">
                        <User className="h-3 w-3" />
                        {revision.user}
                      </div>
                      <div>{revision.changeCount} changes</div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </ScrollArea>
      </CardContent>
    </Card>
  );
};
