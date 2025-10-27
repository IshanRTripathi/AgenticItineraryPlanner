import { useState, useEffect, useRef } from 'react';

interface UseSmoothProgressOptions {
  duration?: number; // Total duration in ms (default: 60000 = 1 minute)
  fastPhaseEnd?: number; // Progress % where fast phase ends (default: 70)
  onComplete?: () => void;
}

/**
 * Hook for smooth progress animation
 * Progresses quickly from 0-70% in first 30 seconds, then slowly to 100% in remaining time
 */
export function useSmoothProgress(
  isActive: boolean,
  options: UseSmoothProgressOptions = {}
) {
  const {
    duration = 60000, // 1 minute default
    fastPhaseEnd = 70,
    onComplete
  } = options;

  const [progress, setProgress] = useState(0);
  const startTimeRef = useRef<number | null>(null);
  const animationFrameRef = useRef<number | null>(null);

  useEffect(() => {
    if (!isActive) {
      // Reset when not active
      setProgress(0);
      startTimeRef.current = null;
      if (animationFrameRef.current) {
        cancelAnimationFrame(animationFrameRef.current);
        animationFrameRef.current = null;
      }
      return;
    }

    // Start the animation
    startTimeRef.current = Date.now();

    const animate = () => {
      if (!startTimeRef.current) return;

      const elapsed = Date.now() - startTimeRef.current;
      const progressRatio = Math.min(elapsed / duration, 1);

      let currentProgress: number;

      if (progressRatio < 0.5) {
        // Fast phase: 0-70% in first 50% of time
        // Use easeOutQuad for smooth deceleration
        const fastProgress = progressRatio * 2; // 0 to 1 over first half
        const eased = 1 - (1 - fastProgress) * (1 - fastProgress);
        currentProgress = eased * fastPhaseEnd;
      } else {
        // Slow phase: 70-100% in last 50% of time
        // Use easeInQuad for smooth acceleration
        const slowProgress = (progressRatio - 0.5) * 2; // 0 to 1 over second half
        const eased = slowProgress * slowProgress;
        currentProgress = fastPhaseEnd + eased * (100 - fastPhaseEnd);
      }

      setProgress(Math.min(currentProgress, 100));

      if (progressRatio < 1) {
        animationFrameRef.current = requestAnimationFrame(animate);
      } else {
        onComplete?.();
      }
    };

    animationFrameRef.current = requestAnimationFrame(animate);

    return () => {
      if (animationFrameRef.current) {
        cancelAnimationFrame(animationFrameRef.current);
      }
    };
  }, [isActive, duration, fastPhaseEnd, onComplete]);

  return progress;
}

/**
 * Hook for progress with stages
 * Shows different messages at different progress points
 */
export function useProgressWithStages(
  isActive: boolean,
  actualProgress?: number // Optional real progress from backend
) {
  const smoothProgress = useSmoothProgress(isActive, {
    duration: 120000, // 2 minutes to match backend generation time
    fastPhaseEnd: 70
  });

  // Use actual progress if available and higher than smooth progress
  const displayProgress = actualProgress !== undefined && actualProgress > smoothProgress
    ? actualProgress
    : smoothProgress;

  // Determine current stage based on progress
  const getStage = (progress: number) => {
    if (progress < 15) {
      return {
        message: 'Analyzing your travel preferences...',
        icon: '🔍',
        color: 'text-blue-600'
      };
    } else if (progress < 30) {
      return {
        message: 'Researching destinations and attractions...',
        icon: '🌍',
        color: 'text-green-600'
      };
    } else if (progress < 50) {
      return {
        message: 'Planning your daily itinerary...',
        icon: '📅',
        color: 'text-purple-600'
      };
    } else if (progress < 70) {
      return {
        message: 'Finding the best places to visit...',
        icon: '⭐',
        color: 'text-yellow-600'
      };
    } else if (progress < 85) {
      return {
        message: 'Optimizing travel routes...',
        icon: '🗺️',
        color: 'text-indigo-600'
      };
    } else if (progress < 95) {
      return {
        message: 'Adding final touches...',
        icon: '✨',
        color: 'text-pink-600'
      };
    } else {
      return {
        message: 'Almost ready!',
        icon: '🎉',
        color: 'text-green-600'
      };
    }
  };

  return {
    progress: displayProgress,
    stage: getStage(displayProgress)
  };
}
