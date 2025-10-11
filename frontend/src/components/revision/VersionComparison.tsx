import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { Badge } from '../ui/badge';
import { ArrowRight, Download } from 'lucide-react';
import { RevisionDiffViewer } from './RevisionDiffViewer';

interface VersionComparisonProps {
  itineraryId: string;
  versions: number[];
}

export const VersionComparison: React.FC<VersionComparisonProps> = ({
  itineraryId,
  versions
}) => {
  const [fromVersion, setFromVersion] = useState<number>(versions[0] || 1);
  const [toVersion, setToVersion] = useState<number>(versions[versions.length - 1] || 1);
  const [showDiff, setShowDiff] = useState(false);

  const handleCompare = () => {
    if (fromVersion !== toVersion) {
      setShowDiff(true);
    }
  };

  const handleExport = () => {
    const data = {
      itineraryId,
      fromVersion,
      toVersion,
      exportedAt: new Date().toISOString()
    };
    
    const blob = new Blob([JSON.stringify(data, null, 2)], {
      type: 'application/json'
    });
    
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `comparison-v${fromVersion}-v${toVersion}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Compare Versions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-4">
            <div className="flex-1">
              <label className="text-sm font-medium mb-2 block">From Version</label>
              <Select
                value={fromVersion.toString()}
                onValueChange={(value) => setFromVersion(parseInt(value))}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {versions.map(v => (
                    <SelectItem key={v} value={v.toString()}>
                      Version {v}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="pt-6">
              <ArrowRight className="h-5 w-5 text-gray-400" />
            </div>

            <div className="flex-1">
              <label className="text-sm font-medium mb-2 block">To Version</label>
              <Select
                value={toVersion.toString()}
                onValueChange={(value) => setToVersion(parseInt(value))}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {versions.map(v => (
                    <SelectItem key={v} value={v.toString()}>
                      Version {v}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="pt-6 flex gap-2">
              <Button onClick={handleCompare} disabled={fromVersion === toVersion}>
                Compare
              </Button>
              {showDiff && (
                <Button variant="outline" onClick={handleExport}>
                  <Download className="h-4 w-4 mr-1" />
                  Export
                </Button>
              )}
            </div>
          </div>

          {fromVersion === toVersion && (
            <div className="mt-4 p-3 bg-yellow-50 border border-yellow-200 rounded text-sm text-yellow-800">
              Please select different versions to compare
            </div>
          )}
        </CardContent>
      </Card>

      {showDiff && fromVersion !== toVersion && (
        <RevisionDiffViewer
          itineraryId={itineraryId}
          fromVersion={fromVersion}
          toVersion={toVersion}
        />
      )}
    </div>
  );
};
