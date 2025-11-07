/**
 * AI Trip Wizard Page
 * Premium multi-step wizard for trip planning with enhanced UI components
 */

import { Header } from '@/components/layout/Header';
import { PremiumTripWizard } from '@/components/ai-planner/PremiumTripWizard';

function TripWizardPage() {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      <PremiumTripWizard />
    </div>
  );
}

export default TripWizardPage;
