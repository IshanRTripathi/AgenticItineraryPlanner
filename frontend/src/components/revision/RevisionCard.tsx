import React, { useState } from 'react';
import { Card, CardContent } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Clock, User, FileText, RotateCcw, Eye } from 'lucide-react';

interface RevisionCardProps {
  version: number;
  timestamp: string;
  user: string;
  changeCount: number;
  changeType: 'major' | 'minor' | 'patch';
  description: string;
  onRestore: () => void;
  onPreview: () => void;
  isSelected?: boolean;
}

export const RevisionCard: React.FC<RevisionCardProps> = ({
  version,
  timestamp,
  user,
  changeCount,
  changeType,
  description,
  onRestore,
  onPreview,
  isSelected = false
}) => {
  const [showPreview, setShowPreview] = useState(false);

  const getChangeTypeColor = () => {
    switch (changeType) {
      case 'major': return 'bg-red-100 text-red-800 border-red-200';
      case 'minor': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'patch': return 'bg-green-100 text-green-800 border-green-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  return (
    <Card className={`transition-all ${isSelected ? 'ring-2 ring-blue-500' : ''}`}>
      <CardContent className="p-4">
        <div className="flex items-start justify-between mb-3">
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-1">
              <h3 className="font-semibold">Version {version}</h3>
              <Badge className={getChangeTypeColor()}>
                {changeType}
              </Badge>
            </div>
            <div className="flex items-center gap-4 text-sm text-gray-500">
              <div className="flex items-center gap-1">
                <Clock className="h-3 w-3" />
                {new Date(timestamp).toLocaleString()}
              </div>
              <div className="flex items-center gap-1">
                <User className="h-3 w-3" />
                {user}
              </div>
            </div>
          </div>
        </div>

        <p className="text-sm text-gray-700 mb-3">{description}</p>

        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1 text-xs text-gray-500">
            <FileText className="h-3 w-3" />
            {changeCount} changes
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setShowPreview(!showPreview);
                onPreview();
              }}
            >
              <Eye className="h-3 w-3 mr-1" />
              Preview
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={onRestore}
            >
              <RotateCcw className="h-3 w-3 mr-1" />
              Restore
            </Button>
          </div>
        </div>

        {showPreview && (
          <div className="mt-3 pt-3 border-t">
            <div className="text-xs text-gray-500">
              Preview will show changes in this version
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
};
