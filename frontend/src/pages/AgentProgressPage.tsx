/**
 * AI Agent Progress Page
 * Shows real-time progress while AI generates itinerary
 */

import { AgentProgress } from '@/components/ai-planner/AgentProgress';
import { InteractiveGlobe } from '@/components/homepage/InteractiveGlobe';

export function AgentProgressPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-indigo-50 flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background Globe - Same as homepage */}
      <div className="absolute inset-0 z-0">
        <InteractiveGlobe />
      </div>
      
      {/* Progress Card - Above globe */}
      <div className="relative z-10">
        <AgentProgress />
      </div>
    </div>
  );
}


export default AgentProgressPage;
