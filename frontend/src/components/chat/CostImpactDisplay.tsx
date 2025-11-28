import { DollarSign, AlertCircle, TrendingUp, TrendingDown } from 'lucide-react';

export interface CostImpact {
  currentCost: number;
  newCost: number;
  difference: number;
  currency: string;
  exceedsBudget: boolean;
  budgetLimit?: number;
  breakdown?: Record<string, number>;
}

interface CostImpactDisplayProps {
  costImpact: CostImpact;
  compact?: boolean;
}

/**
 * Component to display cost impact of proposed changes.
 * Shows current vs new cost with visual indicators.
 */
export function CostImpactDisplay({ costImpact, compact = false }: CostImpactDisplayProps) {
  const isIncrease = costImpact.difference > 0;
  const exceedsBudget = costImpact.exceedsBudget;
  
  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: costImpact.currency || 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  if (compact) {
    return (
      <div className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-sm ${
        exceedsBudget 
          ? 'bg-red-50 text-red-700 border border-red-200' 
          : isIncrease 
            ? 'bg-orange-50 text-orange-700 border border-orange-200'
            : 'bg-green-50 text-green-700 border border-green-200'
      }`}>
        <DollarSign className="h-3 w-3" />
        <span className="font-medium">
          {isIncrease ? '+' : ''}{formatCurrency(costImpact.difference)}
        </span>
        {exceedsBudget && <AlertCircle className="h-3 w-3" />}
      </div>
    );
  }

  return (
    <div className={`border rounded-lg p-3 sm:p-4 ${
      exceedsBudget 
        ? 'border-red-300 bg-red-50' 
        : 'border-gray-200 bg-white'
    }`}>
      {/* Header */}
      <div className="flex items-center gap-2 mb-3">
        <DollarSign className={`h-4 w-4 ${exceedsBudget ? 'text-red-600' : 'text-gray-600'}`} />
        <span className="font-semibold text-sm">Cost Impact</span>
      </div>
      
      {/* Cost Comparison */}
      <div className="space-y-2 text-sm">
        {/* Current Cost */}
        <div className="flex justify-between items-center">
          <span className="text-gray-600">Current:</span>
          <span className="font-medium">{formatCurrency(costImpact.currentCost)}</span>
        </div>
        
        {/* New Cost */}
        <div className="flex justify-between items-center">
          <span className="text-gray-600">After changes:</span>
          <div className="flex items-center gap-2">
            <span className={`font-semibold ${
              isIncrease ? 'text-red-600' : 'text-green-600'
            }`}>
              {formatCurrency(costImpact.newCost)}
            </span>
            {isIncrease ? (
              <TrendingUp className="h-3 w-3 text-red-600" />
            ) : (
              <TrendingDown className="h-3 w-3 text-green-600" />
            )}
          </div>
        </div>
        
        {/* Difference */}
        <div className="flex justify-between items-center pt-2 border-t border-gray-200">
          <span className="text-gray-600">Difference:</span>
          <span className={`font-bold ${
            isIncrease ? 'text-red-600' : 'text-green-600'
          }`}>
            {isIncrease ? '+' : ''}{formatCurrency(costImpact.difference)}
          </span>
        </div>
      </div>
      
      {/* Budget Warning */}
      {exceedsBudget && (
        <div className="flex items-start gap-2 mt-3 p-2 bg-red-100 rounded text-xs text-red-800">
          <AlertCircle className="h-3 w-3 mt-0.5 flex-shrink-0" />
          <div>
            <div className="font-semibold">Exceeds Budget</div>
            {costImpact.budgetLimit && (
              <div className="mt-0.5">
                Budget limit: {formatCurrency(costImpact.budgetLimit)}
                <br />
                Over by: {formatCurrency(costImpact.newCost - costImpact.budgetLimit)}
              </div>
            )}
          </div>
        </div>
      )}
      
      {/* Breakdown (if available) */}
      {costImpact.breakdown && Object.keys(costImpact.breakdown).length > 0 && (
        <div className="mt-3 pt-3 border-t border-gray-200">
          <div className="text-xs font-medium text-gray-600 mb-2">Breakdown:</div>
          <div className="space-y-1">
            {Object.entries(costImpact.breakdown).map(([category, change]) => (
              change !== 0 && (
                <div key={category} className="flex justify-between text-xs">
                  <span className="text-gray-600 capitalize">{category}:</span>
                  <span className={change > 0 ? 'text-red-600' : 'text-green-600'}>
                    {change > 0 ? '+' : ''}{formatCurrency(change)}
                  </span>
                </div>
              )
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
