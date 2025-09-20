import React from 'react';
import { Button } from '../../ui/button';
import { Card } from '../../ui/card';
import { Progress } from '../../ui/progress';
import { Share2, FileText } from 'lucide-react';
import { AgentStatus } from '../shared/types';

interface TripOverviewViewProps {
  tripData: any;
  agentStatuses: AgentStatus[];
  onShare: () => void;
  onExportPDF: () => void;
}

export function TripOverviewView({ tripData, agentStatuses, onShare, onExportPDF }: TripOverviewViewProps) {
  return (
    <div className="p-6 overflow-y-auto h-full">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-semibold">Trip Overview</h2>
        <div className="flex items-center space-x-3">
          <Button variant="outline" onClick={onShare}>
            <Share2 className="w-4 h-4 mr-2" />
            Share
          </Button>
          <Button variant="outline" onClick={onExportPDF}>
            <FileText className="w-4 h-4 mr-2" />
            Export PDF
          </Button>
        </div>
      </div>
      
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="p-6">
          <h3 className="font-semibold mb-4">Trip Summary</h3>
          <div className="space-y-3">
            <div className="flex justify-between">
              <span className="text-gray-600">Destination:</span>
              <span className="font-medium">{tripData.destination}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Duration:</span>
              <span className="font-medium">{tripData.itinerary?.days?.length || 0} days</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Travelers:</span>
              <span className="font-medium">{tripData.travelers?.length || 1} people</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Budget:</span>
              <span className="font-medium">{tripData.budget?.currency} {tripData.budget?.total}</span>
            </div>
          </div>
        </Card>
        
        <Card className="p-6">
          <h3 className="font-semibold mb-4">Agent Progress</h3>
          <div className="space-y-3">
            {agentStatuses.map((agent) => (
              <div key={agent.id} className="flex items-center justify-between">
                <span className="text-sm capitalize">{agent.kind}</span>
                <div className="flex items-center space-x-2">
                  <Progress value={agent.progress} className="w-20" />
                  <span className="text-xs text-gray-600">{agent.progress}%</span>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
