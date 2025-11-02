/**
 * Generation Progress Banner
 * Compact, refined progress indicator for trip page during generation
 */

import { useEffect, useState } from 'react';
import { Sparkles, Check, Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';

interface GenerationProgressBannerProps {
  itineraryStatus?: string;
  completedDays?: number;
  totalDays?: number;
  currentPhase?: string;
  onComplete?: () => void;
}

export function GenerationProgressBanner({
  itineraryStatus,
  completedDays = 0,
  totalDays = 4,
  currentPhase = 'skeleton',
  onComplete
}: GenerationProgressBannerProps) {
  const [progress, setProgress] = useState(0);
  const [isVisible, setIsVisible] = useState(true);

  // Calculate progress based on completed days and phase
  useEffect(() => {
    let calculatedProgress = 0;
    
    // Days contribute 40% of progress
    calculatedProgress += (completedDays / totalDays) * 40;
    
    // Phase contributes remaining 60%
    if (currentPhase === 'population') {
      calculatedProgress += 10;
    } else if (currentPhase === 'enrichment') {
      calculatedProgress += 30;
    } else if (currentPhase === 'cost_estimation') {
      calculatedProgress += 50;
    } else if (currentPhase === 'finalization') {
      calculatedProgress += 55;
    }
    
    setProgress(Math.min(calculatedProgress, 99));
  }, [completedDays, totalDays, currentPhase]);

  // Hide banner when complete
  useEffect(() => {
    if (itineraryStatus === 'completed' || itineraryStatus === 'ready') {
      setProgress(100);
      setTimeout(() => {
        setIsVisible(false);
        onComplete?.();
      }, 3000);
    }
  }, [itineraryStatus, onComplete]);

  // Only show during generation
  if (!isVisible || itineraryStatus === 'completed' || itineraryStatus === 'ready') {
    return null;
  }

  // Don't show if no progress yet
  if (completedDays === 0 && currentPhase === 'skeleton') {
    return null;
  }

  const getPhaseMessage = () => {
    if (completedDays < totalDays) {
      return `Creating day ${completedDays + 1} of ${totalDays}`;
    }
    
    switch (currentPhase) {
      case 'population':
        return 'Adding activity details';
      case 'enrichment':
        return 'Enriching with photos & reviews';
      case 'cost_estimation':
        return 'Calculating costs';
      case 'finalization':
        return 'Finalizing itinerary';
      default:
        return 'Generating itinerary';
    }
  };

  return (
    <div className="sticky top-0 z-50 bg-gradient-to-r from-blue-600 via-indigo-600 to-purple-600 shadow-lg">
      <div className="max-w-7xl mx-auto px-4 py-3">
        <div className="flex items-center justify-between gap-4">
          {/* Left: Icon + Message */}
          <div className="flex items-center gap-3 min-w-0 flex-1">
            <div className="relative flex-shrink-0">
              <div className="w-8 h-8 rounded-full bg-white/20 backdrop-blur flex items-center justify-center">
                {progress === 100 ? (
                  <Check className="w-4 h-4 text-white" />
                ) : (
                  <Loader2 className="w-4 h-4 text-white animate-spin" />
                )}
              </div>
              {progress < 100 && (
                <div className="absolute inset-0 rounded-full border-2 border-white/30 animate-ping" />
              )}
            </div>
            
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <span className="text-sm font-semibold text-white">
                  {progress === 100 ? 'Itinerary Complete!' : getPhaseMessage()}
                </span>
                {completedDays > 0 && progress < 100 && (
                  <span className="text-xs text-white/80">
                    ({completedDays}/{totalDays} days ready)
                  </span>
                )}
              </div>
              <div className="text-xs text-white/70 mt-0.5">
                {progress === 100 
                  ? 'All activities are now available'
                  : 'Updates appear automatically as they\'re ready'
                }
              </div>
            </div>
          </div>

          {/* Right: Progress */}
          <div className="flex items-center gap-3 flex-shrink-0">
            <div className="hidden sm:block w-32">
              <div className="h-2 bg-white/20 rounded-full overflow-hidden backdrop-blur">
                <div 
                  className="h-full bg-white rounded-full transition-all duration-500 ease-out"
                  style={{ width: `${progress}%` }}
                >
                  <div className="h-full w-full bg-gradient-to-r from-white/0 via-white/40 to-white/0 animate-shimmer" 
                       style={{ backgroundSize: '200% 100%' }} />
                </div>
              </div>
            </div>
            <span className="text-sm font-bold text-white tabular-nums">
              {Math.round(progress)}%
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
