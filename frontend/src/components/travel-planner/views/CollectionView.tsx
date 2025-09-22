import React from 'react';
import { Button } from '../../ui/button';
import { Card } from '../../ui/card';
import { ViewComponentProps } from '../shared/types';

export function CollectionView({ tripData }: ViewComponentProps) {
  return (
    <div className="p-6 overflow-y-auto h-full">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-semibold">Collection</h2>
        <Button>Add to Collection</Button>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <Card className="p-4">
          <div className="aspect-video bg-gray-200 rounded-lg mb-4"></div>
          <h3 className="font-semibold">Saved Places</h3>
          <p className="text-sm text-gray-600">{tripData.itinerary?.days?.length || 0} destinations planned</p>
        </Card>
        
        <Card className="p-4">
          <div className="aspect-video bg-gray-200 rounded-lg mb-4"></div>
          <h3 className="font-semibold">Photo Spots</h3>
          <p className="text-sm text-gray-600">
            {tripData.itinerary?.days?.reduce((total: number, day: any) => 
              total + (day.components?.filter((c: any) => c.type === 'attraction' || c.type === 'activity')?.length || 0), 0) || 0} attractions
          </p>
        </Card>
        
        <Card className="p-4">
          <div className="aspect-video bg-gray-200 rounded-lg mb-4"></div>
          <h3 className="font-semibold">Must-Try Foods</h3>
          <p className="text-sm text-gray-600">
            {tripData.itinerary?.days?.reduce((total: number, day: any) => 
              total + (day.components?.filter((c: any) => c.type === 'restaurant')?.length || 0), 0) || 0} meals planned
          </p>
        </Card>
      </div>
    </div>
  );
}
