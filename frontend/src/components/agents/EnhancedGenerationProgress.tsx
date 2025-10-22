import React from 'react';
import { Card } from '../ui/card';
import { Button } from '../ui/button';
import { X, Loader2 } from 'lucide-react';
import { useProgressWithStages } from '../../hooks/useSmoothProgress';

interface EnhancedGenerationProgressProps {
  tripName: string;
  actualProgress?: number; // Optional real progress from backend (0-100)
  onCancel?: () => void;
  onComplete?: () => void;
}

export function EnhancedGenerationProgress({
  tripName,
  actualProgress,
  onCancel,
  onComplete
}: EnhancedGenerationProgressProps) {
  const { progress, stage } = useProgressWithStages(true, actualProgress);

  // Auto-complete when progress reaches 100
  React.useEffect(() => {
    if (progress >= 100 && onComplete) {
      console.log('[EnhancedGenerationProgress] Progress reached 100%, triggering completion');
      const timer = setTimeout(() => {
        console.log('[EnhancedGenerationProgress] Calling onComplete callback');
        onComplete();
      }, 1000); // Give user 1 second to see 100%
      return () => clearTimeout(timer);
    }
  }, [progress, onComplete]);

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <Card className="w-full max-w-2xl p-8 space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-300">
        {/* Header */}
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <h2 className="text-2xl font-bold text-gray-900">
              Creating Your Itinerary
            </h2>
            <p className="text-gray-600">{tripName}</p>
          </div>
          {onCancel && (
            <Button
              variant="ghost"
              size="sm"
              onClick={onCancel}
              className="text-gray-400 hover:text-gray-600"
            >
              <X className="w-5 h-5" />
            </Button>
          )}
        </div>

        {/* Progress Bar */}
        <div className="space-y-3">
          <div className="flex items-center justify-between text-sm">
            <div className="flex items-center gap-2">
              <span className={`text-2xl ${stage.color}`}>{stage.icon}</span>
              <span className="font-medium text-gray-700">{stage.message}</span>
            </div>
            <span className="font-semibold text-gray-900">{Math.round(progress)}%</span>
          </div>

          {/* Progress bar container */}
          <div className="relative h-3 bg-gray-200 rounded-full overflow-hidden">
            {/* Animated background gradient */}
            <div
              className="absolute inset-0 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500 opacity-20 animate-pulse"
              style={{
                width: `${progress}%`,
                transition: 'width 0.3s ease-out'
              }}
            />
            
            {/* Main progress bar */}
            <div
              className="absolute inset-y-0 left-0 bg-gradient-to-r from-blue-600 via-purple-600 to-pink-600 rounded-full transition-all duration-300 ease-out"
              style={{ width: `${progress}%` }}
            >
              {/* Shimmer effect */}
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent animate-shimmer" />
            </div>
          </div>
        </div>

        {/* Status indicators */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-4">
          <StatusIndicator
            label="Destinations"
            isActive={progress > 10}
            isComplete={progress > 30}
          />
          <StatusIndicator
            label="Activities"
            isActive={progress > 30}
            isComplete={progress > 60}
          />
          <StatusIndicator
            label="Routes"
            isActive={progress > 60}
            isComplete={progress > 85}
          />
          <StatusIndicator
            label="Details"
            isActive={progress > 85}
            isComplete={progress >= 100}
          />
        </div>

        {/* Info message */}
        <div className="pt-4 border-t border-gray-200">
          <p className="text-sm text-gray-500 text-center">
            This usually takes about a minute. Feel free to grab a coffee! ☕
          </p>
        </div>
      </Card>

      {/* Add shimmer animation styles */}
      <style>{`
        @keyframes shimmer {
          0% {
            transform: translateX(-100%);
          }
          100% {
            transform: translateX(100%);
          }
        }
        .animate-shimmer {
          animation: shimmer 2s infinite;
        }
      `}</style>
    </div>
  );
}

interface StatusIndicatorProps {
  label: string;
  isActive: boolean;
  isComplete: boolean;
}

function StatusIndicator({ label, isActive, isComplete }: StatusIndicatorProps) {
  return (
    <div className="flex flex-col items-center gap-2">
      <div
        className={`w-10 h-10 rounded-full flex items-center justify-center transition-all duration-300 ${
          isComplete
            ? 'bg-green-500 text-white'
            : isActive
            ? 'bg-blue-500 text-white animate-pulse'
            : 'bg-gray-200 text-gray-400'
        }`}
      >
        {isComplete ? (
          <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
        ) : isActive ? (
          <Loader2 className="w-5 h-5 animate-spin" />
        ) : (
          <div className="w-2 h-2 rounded-full bg-current" />
        )}
      </div>
      <span
        className={`text-xs font-medium transition-colors duration-300 ${
          isComplete ? 'text-green-600' : isActive ? 'text-blue-600' : 'text-gray-400'
        }`}
      >
        {label}
      </span>
    </div>
  );
}
