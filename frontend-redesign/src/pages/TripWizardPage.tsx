/**
 * AI Trip Wizard Page
 * Premium multi-step wizard for trip planning
 */

import { TripWizard } from '@/components/ai-planner/TripWizard';

export function TripWizardPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 to-primary-100 py-4 md:py-12 px-4">
      <TripWizard />
    </div>
  );
}
