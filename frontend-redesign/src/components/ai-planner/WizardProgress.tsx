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
    <div className="relative">
      {/* Progress Line */}
      <div className="absolute top-4 left-0 right-0 h-0.5 bg-muted">
        <div
          className="h-full bg-primary transition-all duration-300"
          style={{ width: `${((currentStep - 1) / (steps.length - 1)) * 100}%` }}
        />
      </div>

      {/* Steps */}
      <div className="relative flex justify-between">
        {steps.map((step) => {
          const isCompleted = step.id < currentStep;
          const isCurrent = step.id === currentStep;
          const isUpcoming = step.id > currentStep;

          return (
            <div key={step.id} className="flex flex-col items-center">
              {/* Step Circle */}
              <div
                className={cn(
                  'w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold transition-all duration-300',
                  isCompleted && 'bg-success text-white',
                  isCurrent && 'bg-primary text-white ring-4 ring-primary/20',
                  isUpcoming && 'bg-muted text-muted-foreground'
                )}
              >
                {isCompleted ? (
                  <Check className="w-4 h-4" />
                ) : (
                  step.id
                )}
              </div>

              {/* Step Label */}
              <span
                className={cn(
                  'mt-2 text-xs font-medium transition-colors',
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
