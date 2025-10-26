/**
 * AI Agent Progress Page
 * Shows real-time progress while AI generates itinerary
 */

import { AgentProgress } from '@/components/ai-planner/AgentProgress';

export function AgentProgressPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-primary via-primary-600 to-primary-700 flex items-center justify-center p-4">
      <AgentProgress />
    </div>
  );
}
