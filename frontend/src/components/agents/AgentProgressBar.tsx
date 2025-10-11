import React from 'react';
import { Progress } from '../ui/progress';
import { Badge } from '../ui/badge';
import { Loader2, CheckCircle, XCircle, Clock } from 'lucide-react';

interface AgentProgressBarProps {
  status: 'queued' | 'running' | 'completed' | 'failed';
  progress: number;
  currentStep?: string;
  estimatedTime?: number;
}

export const AgentProgressBar: React.FC<AgentProgressBarProps> = ({
  status,
  progress,
  currentStep,
  estimatedTime
}) => {
  const getStatusIcon = () => {
    switch (status) {
      case 'queued':
        return <Clock className="h-4 w-4" />;
      case 'running':
        return <Loader2 className="h-4 w-4 animate-spin" />;
      case 'completed':
        return <CheckCircle className="h-4 w-4" />;
      case 'failed':
        return <XCircle className="h-4 w-4" />;
    }
  };

  const getStatusColor = () => {
    switch (status) {
      case 'queued':
        return 'bg-gray-500';
      case 'running':
        return 'bg-blue-500';
      case 'completed':
        return 'bg-green-500';
      case 'failed':
        return 'bg-red-500';
    }
  };

  const getStatusText = () => {
    switch (status) {
      case 'queued':
        return 'Queued';
      case 'running':
        return 'Running';
      case 'completed':
        return 'Completed';
      case 'failed':
        return 'Failed';
    }
  };

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className={`${getStatusColor()} rounded-full p-1 text-white`}>
            {getStatusIcon()}
          </div>
          <Badge variant={status === 'failed' ? 'destructive' : 'default'}>
            {getStatusText()}
          </Badge>
        </div>
        <span className="text-sm text-gray-600">
          {Math.round(progress)}%
        </span>
      </div>

      <Progress value={progress} className="h-2" />

      {currentStep && (
        <p className="text-sm text-gray-600">
          {currentStep}
        </p>
      )}

      {estimatedTime && status === 'running' && (
        <p className="text-xs text-gray-500">
          Estimated time remaining: {Math.ceil(estimatedTime / 1000)}s
        </p>
      )}
    </div>
  );
};
