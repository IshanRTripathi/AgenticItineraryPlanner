import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs';
import { DiffViewer } from '../diff/DiffViewer';
import { Badge } from '../ui/badge';
import { apiClient } from '../../services/apiClient';

interface RevisionDiffViewerProps {
  itineraryId: string;
  fromVersion: number;
  toVersion: number;
}

interface DiffSection {
  title: string;
  changes: Array<{
    type: 'added' | 'removed' | 'modified';
    content: string;
    oldContent?: string;
  }>;
}

export const RevisionDiffViewer: React.FC<RevisionDiffViewerProps> = ({
  itineraryId,
  fromVersion,
  toVersion
}) => {
  const [loading, setLoading] = useState(true);
  const [diffSections, setDiffSections] = useState<DiffSection[]>([]);
  const [stats, setStats] = useState({ added: 0, removed: 0, modified: 0 });

  useEffect(() => {
    loadDiff();
  }, [itineraryId, fromVersion, toVersion]);

  const loadDiff = async () => {
    try {
      setLoading(true);
      const response = await apiClient.getRevisions(itineraryId);
      const revisions = response.revisions || [];
      const fromData = revisions.find((r: any) => r.version === fromVersion);
      const toData = revisions.find((r: any) => r.version === toVersion);
      
      const sections = computeDiff(fromData, toData);
      setDiffSections(sections);
      
      const added = sections.reduce((sum, s) => sum + s.changes.filter(c => c.type === 'added').length, 0);
      const removed = sections.reduce((sum, s) => sum + s.changes.filter(c => c.type === 'removed').length, 0);
      const modified = sections.reduce((sum, s) => sum + s.changes.filter(c => c.type === 'modified').length, 0);
      setStats({ added, removed, modified });
    } catch (error) {
      console.error('Failed to load diff:', error);
    } finally {
      setLoading(false);
    }
  };

  const computeDiff = (from: any, to: any): DiffSection[] => {
    const sections: DiffSection[] = [];
    
    if (from.itinerary?.days && to.itinerary?.days) {
      to.itinerary.days.forEach((toDay: any, index: number) => {
        const fromDay = from.itinerary.days[index];
        if (!fromDay) {
          sections.push({
            title: `Day ${index + 1} (Added)`,
            changes: [{ type: 'added', content: JSON.stringify(toDay, null, 2) }]
          });
        } else {
          const dayChanges: any[] = [];
          
          if (JSON.stringify(fromDay) !== JSON.stringify(toDay)) {
            dayChanges.push({
              type: 'modified',
              content: JSON.stringify(toDay, null, 2),
              oldContent: JSON.stringify(fromDay, null, 2)
            });
          }
          
          if (dayChanges.length > 0) {
            sections.push({
              title: `Day ${index + 1}`,
              changes: dayChanges
            });
          }
        }
      });
    }
    
    return sections;
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle>
            Changes: Version {fromVersion} → {toVersion}
          </CardTitle>
          <div className="flex gap-2">
            {stats.added > 0 && (
              <Badge className="bg-green-100 text-green-800">
                +{stats.added}
              </Badge>
            )}
            {stats.modified > 0 && (
              <Badge className="bg-yellow-100 text-yellow-800">
                ~{stats.modified}
              </Badge>
            )}
            {stats.removed > 0 && (
              <Badge className="bg-red-100 text-red-800">
                -{stats.removed}
              </Badge>
            )}
          </div>
        </div>
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="text-center py-8 text-gray-500">Loading diff...</div>
        ) : diffSections.length === 0 ? (
          <div className="text-center py-8 text-gray-500">No changes found</div>
        ) : (
          <Tabs defaultValue="0">
            <TabsList>
              {diffSections.map((section, index) => (
                <TabsTrigger key={index} value={index.toString()}>
                  {section.title}
                </TabsTrigger>
              ))}
            </TabsList>
            {diffSections.map((section, index) => (
              <TabsContent key={index} value={index.toString()}>
                <div className="space-y-2">
                  {section.changes.map((change, changeIndex) => (
                    <DiffViewer
                      key={changeIndex}
                      oldValue={change.oldContent || ''}
                      newValue={change.content}
                      splitView={true}
                    />
                  ))}
                </div>
              </TabsContent>
            ))}
          </Tabs>
        )}
      </CardContent>
    </Card>
  );
};
