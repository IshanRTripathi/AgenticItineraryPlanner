import React from 'react';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Badge } from '../ui/badge';
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';

interface RevisionNavigationProps {
  currentVersion: number;
  totalVersions: number;
  onNavigate: (version: number) => void;
}

export const RevisionNavigation: React.FC<RevisionNavigationProps> = ({
  currentVersion,
  totalVersions,
  onNavigate
}) => {
  const [jumpToVersion, setJumpToVersion] = React.useState('');

  const handleJump = () => {
    const version = parseInt(jumpToVersion);
    if (version >= 1 && version <= totalVersions) {
      onNavigate(version);
      setJumpToVersion('');
    }
  };

  return (
    <div className="flex items-center justify-between p-3 bg-gray-50 border rounded-lg">
      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          onClick={() => onNavigate(1)}
          disabled={currentVersion === 1}
        >
          <ChevronsLeft className="h-4 w-4" />
        </Button>
        <Button
          variant="outline"
          size="sm"
          onClick={() => onNavigate(currentVersion - 1)}
          disabled={currentVersion === 1}
        >
          <ChevronLeft className="h-4 w-4" />
        </Button>
        
        <Badge variant="outline" className="px-3">
          Version {currentVersion} of {totalVersions}
        </Badge>

        <Button
          variant="outline"
          size="sm"
          onClick={() => onNavigate(currentVersion + 1)}
          disabled={currentVersion === totalVersions}
        >
          <ChevronRight className="h-4 w-4" />
        </Button>
        <Button
          variant="outline"
          size="sm"
          onClick={() => onNavigate(totalVersions)}
          disabled={currentVersion === totalVersions}
        >
          <ChevronsRight className="h-4 w-4" />
        </Button>
      </div>

      <div className="flex items-center gap-2">
        <span className="text-sm text-gray-600">Jump to:</span>
        <Input
          type="number"
          min={1}
          max={totalVersions}
          value={jumpToVersion}
          onChange={(e) => setJumpToVersion(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && handleJump()}
          className="w-20"
          placeholder="v#"
        />
        <Button size="sm" onClick={handleJump} disabled={!jumpToVersion}>
          Go
        </Button>
      </div>
    </div>
  );
};
