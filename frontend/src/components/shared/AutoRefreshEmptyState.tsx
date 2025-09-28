import React from 'react';
import { Card } from '../ui/card';
import { Button } from '../ui/button';
import { Calendar, RefreshCw } from 'lucide-react';
import { useAutoRefresh } from '../../hooks/useAutoRefresh';

interface AutoRefreshEmptyStateProps {
  title?: string;
  description?: string;
  onRefresh: () => void;
  showRefreshButton?: boolean;
  icon?: React.ReactNode;
  className?: string;
}

export const AutoRefreshEmptyState: React.FC<AutoRefreshEmptyStateProps> = ({
  title = "No itinerary data available yet",
  description = "Your personalized itinerary will appear here once planning is complete.",
  onRefresh,
  showRefreshButton = true,
  icon = <Calendar className="w-16 h-16 mx-auto mb-4 text-gray-300" />,
  className = ""
}) => {
  const { countdown, isRefreshing, stopRefresh } = useAutoRefresh({
    interval: 3,
    onRefresh,
    enabled: false // Disable auto-refresh by default to prevent repeated requests
  });

  const handleManualRefresh = () => {
    onRefresh();
  };

  return (
    <Card className={`p-8 text-center ${className}`}>
      <div className="text-gray-500">
        {icon}
        <p className="text-xl font-medium mb-2">{title}</p>
        <p className="text-sm mb-4">{description}</p>
        
        {/* Auto-refresh countdown */}
        <div className="mb-4">
          {isRefreshing ? (
            <div className="flex items-center justify-center space-x-2 text-blue-600">
              <RefreshCw className="w-4 h-4 animate-spin" />
              <span className="text-sm">Refreshing...</span>
            </div>
          ) : (
            <div className="text-sm text-gray-500">
              <span>Auto-refreshing in </span>
              <span className="font-mono font-bold text-blue-600">{countdown}</span>
              <span> seconds</span>
            </div>
          )}
        </div>

        {/* Manual refresh button */}
        {showRefreshButton && (
          <div className="space-y-2">
            <Button 
              onClick={handleManualRefresh}
              variant="outline"
              size="sm"
              disabled={isRefreshing}
              className="mr-2"
            >
              {isRefreshing ? (
                <>
                  <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                  Refreshing...
                </>
              ) : (
                <>
                  <RefreshCw className="w-4 h-4 mr-2" />
                  Refresh Now
                </>
              )}
            </Button>
            <Button 
              onClick={stopRefresh}
              variant="ghost"
              size="sm"
              className="text-gray-500 hover:text-gray-700"
            >
              Stop Auto-refresh
            </Button>
          </div>
        )}
      </div>
    </Card>
  );
};
