/**
 * Wizard Progress Indicator
 * Modern unified progress display for all screen sizes
 * Features: Progress bar, percentage, step indicators with animations
 */

import { Check } from 'lucide-react';
import { cn } from '@/lib/utils';

interface Step {
  id: number;
  title: string;
}

interface WizardProgressProps {
  currentStep: number;
  steps: Step[];
}

export function WizardProgress({ currentStep, steps }: WizardProgressProps) {
  const progressPercentage = ((currentStep - 1) / (steps.length - 1)) * 100;

  return (
    <div className="space-y-3 sm:space-y-4">
      {/* Current Step Info & Percentage */}
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs sm:text-sm font-medium text-muted-foreground">
            Step {currentStep} of {steps.length}
          </p>
          <p className="text-sm sm:text-base font-bold text-foreground mt-0.5">
            {steps[currentStep - 1].title}
          </p>
        </div>
        <div className="text-right">
          <p className="text-2xl sm:text-3xl font-bold text-primary">
            {Math.round(progressPercentage)}%
          </p>
        </div>
      </div>

      {/* Progress Bar */}
      <div className="relative h-2 sm:h-2.5 bg-muted rounded-full overflow-hidden">
        <div
          className="absolute inset-y-0 left-0 bg-gradient-to-r from-primary to-primary/80 rounded-full transition-all duration-500 ease-out"
          style={{ width: `${progressPercentage}%` }}
        >
          {/* Shimmer effect */}
          <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent animate-shimmer" />
        </div>
      </div>

      {/* Step Indicators */}
      <div className="flex justify-between items-center px-0.5 sm:px-1">
        {steps.map((step) => {
          const isCompleted = step.id < currentStep;
          const isCurrent = step.id === currentStep;

          return (
            <div key={step.id} className="flex flex-col items-center gap-1 sm:gap-1.5">
              {/* Step Circle */}
              <div
                className={cn(
                  'w-7 h-7 sm:w-9 sm:h-9 md:w-10 md:h-10 rounded-full flex items-center justify-center text-xs sm:text-sm font-bold transition-all duration-300',
                  isCompleted && 'bg-green-500 text-white shadow-md',
                  isCurrent && 'bg-primary text-white shadow-lg ring-2 sm:ring-4 ring-primary/30 scale-105 sm:scale-110',
                  !isCompleted && !isCurrent && 'bg-muted text-muted-foreground'
                )}
              >
                {isCompleted ? (
                  <Check className="w-4 h-4 sm:w-5 sm:h-5" />
                ) : (
                  step.id
                )}
              </div>

              {/* Step Label */}
              <span
                className={cn(
                  'text-[9px] sm:text-xs font-medium max-w-[60px] sm:max-w-[80px] md:max-w-none text-center leading-tight transition-colors',
                  isCurrent && 'text-primary font-semibold',
                  isCompleted && 'text-foreground',
                  !isCompleted && !isCurrent && 'text-muted-foreground'
                )}
              >
                {step.title}
              </span>
            </div>
          );
        })}
      </div>

      <style>{`
        @keyframes shimmer {
          0% { transform: translateX(-100%); }
          100% { transform: translateX(100%); }
        }
        .animate-shimmer {
          animation: shimmer 2s infinite;
        }
      `}</style>
    </div>
  );
}
