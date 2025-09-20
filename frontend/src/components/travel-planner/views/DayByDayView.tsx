import React from 'react';
import { Card } from '../../ui/card';
import { Badge } from '../../ui/badge';
import { ViewComponentProps } from '../shared/types';

export function DayByDayView({ tripData }: ViewComponentProps) {
  return (
    <div className="m-0 h-full overflow-y-auto p-6">
      <div className="space-y-4">
        {tripData.itinerary?.days?.map((day: any, index: number) => (
          <Card key={day.id || index} className="p-4">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold">Day {day.dayNumber || index + 1} - {day.location || 'Unknown Location'}</h3>
              <div className="flex gap-2">
                <Badge variant="outline">{day.theme || 'Explore'}</Badge>
                <Badge variant="secondary">{day.components?.length || 0} activities</Badge>
              </div>
            </div>
            
            {day.components && day.components.length > 0 ? (
              <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
                {day.components.map((component: any, compIndex: number) => (
                  <div key={compIndex} className="space-y-2 p-3 border rounded-lg">
                    <div className="flex items-center gap-2">
                      <Badge variant="outline" className="text-xs">
                        {component.type || 'activity'}
                      </Badge>
                      <span className="text-sm font-medium">{component.name || 'Unnamed Activity'}</span>
                    </div>
                    <p className="text-sm text-gray-600 line-clamp-2">
                      {component.description || 'No description available'}
                    </p>
                    <div className="flex items-center justify-between text-xs text-gray-500">
                      <span>
                        {component.timing?.startTime || 'TBD'} - {component.timing?.endTime || 'TBD'}
                      </span>
                      <span>
                        {component.cost?.currency || 'USD'} {component.cost?.pricePerPerson || '0'}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="grid md:grid-cols-3 gap-4">
                <div className="space-y-2">
                  <h4 className="text-sm font-medium text-gray-600">Morning</h4>
                  <p className="text-sm text-gray-500">Arrive and check-in</p>
                </div>
                <div className="space-y-2">
                  <h4 className="text-sm font-medium text-gray-600">Afternoon</h4>
                  <p className="text-sm text-gray-500">Explore local attractions</p>
                </div>
                <div className="space-y-2">
                  <h4 className="text-sm font-medium text-gray-600">Evening</h4>
                  <p className="text-sm text-gray-500">Dinner and rest</p>
                </div>
              </div>
            )}
            
            {day.notes && (
              <div className="mt-4 p-3 bg-blue-50 rounded-lg">
                <p className="text-sm text-blue-800">{day.notes}</p>
              </div>
            )}
          </Card>
        )) || (
          <div className="text-center py-8 text-gray-500">
            <p>No itinerary data available yet.</p>
          </div>
        )}
      </div>
    </div>
  );
}
