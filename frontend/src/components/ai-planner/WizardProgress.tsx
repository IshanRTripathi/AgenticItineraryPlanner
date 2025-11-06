/**
 * Wizard Progress Indicator
 * Shows current step with visual progress
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
  return (
    <div className="relative overflow-x-auto pb-2 -mx-4 px-4 md:mx-0 md:px-0">
      {/* Progress Line */}
      <div className="absolute top-4 left-4 right-4 md:left-0 md:right-0 h-0.5 bg-muted">
        <div
          className="h-full bg-primary transition-all duration-300"
          style={{ width: `${((currentStep - 1) / (steps.length - 1)) * 100}%` }}
        />
      </div>

      {/* Steps */}
      <div className="relative flex justify-between min-w-max md:min-w-0">
        {steps.map((step) => {
          const isCompleted = step.id < currentStep;
          const isCurrent = step.id === currentStep;
          const isUpcoming = step.id > currentStep;

          return (
            <div key={step.id} className="flex flex-col items-center px-2 md:px-0">
              {/* Step Circle */}
              <div
                className={cn(
                  'w-10 h-10 md:w-8 md:h-8 rounded-full flex items-center justify-center text-sm font-semibold transition-all duration-300',
                  isCompleted && 'bg-success text-white',
                  isCurrent && 'bg-primary text-white ring-4 ring-primary/20',
                  isUpcoming && 'bg-muted text-muted-foreground'
                )}
              >
                {isCompleted ? (
                  <Check className="w-5 h-5 md:w-4 md:h-4" />
                ) : (
                  step.id
                )}
              </div>

              {/* Step Label */}
              <span
                className={cn(
                  'mt-2 text-xs font-medium transition-colors whitespace-nowrap',
                  (isCurrent || isCompleted) && 'text-foreground',
                  isUpcoming && 'text-muted-foreground'
                )}
              >
                {step.title}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
