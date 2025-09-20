import React from 'react';
import { Card } from '../../ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../ui/select';
import { CURRENCIES } from '../shared/types';

interface BudgetViewProps {
  tripData: any;
  currency: string;
  onCurrencyChange: (currency: string) => void;
}

export function BudgetView({ tripData, currency, onCurrencyChange }: BudgetViewProps) {
  return (
    <div className="p-6 overflow-y-auto h-full">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-semibold">Budget & Costs</h2>
        <div className="flex items-center space-x-3">
          <Select value={currency} onValueChange={onCurrencyChange}>
            <SelectTrigger className="w-32">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {CURRENCIES.map(curr => (
                <SelectItem key={curr} value={curr}>{curr}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <Card className="p-6">
          <h3 className="font-semibold mb-4">Total Budget</h3>
          <div className="text-3xl font-bold text-green-600">
            {currency} {tripData.budget?.total || 1200}
          </div>
          <p className="text-sm text-gray-600 mt-2">Estimated total cost</p>
        </Card>
        
        <Card className="p-6">
          <h3 className="font-semibold mb-4">Accommodation</h3>
          <div className="text-2xl font-bold">
            {currency} {Math.round((tripData.budget?.total || 1200) * 0.4)}
          </div>
          <p className="text-sm text-gray-600 mt-2">40% of total budget</p>
        </Card>
        
        <Card className="p-6">
          <h3 className="font-semibold mb-4">Activities</h3>
          <div className="text-2xl font-bold">
            {currency} {Math.round((tripData.budget?.total || 1200) * 0.3)}
          </div>
          <p className="text-sm text-gray-600 mt-2">30% of total budget</p>
        </Card>
      </div>
    </div>
  );
}
