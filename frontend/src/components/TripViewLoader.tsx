import React, { useState } from 'react';
import { useItinerary } from '../state/query/hooks';
import { TripData } from '../types/TripData';
import { LoadingState } from './shared/LoadingState';
import { DayCardSkeleton } from './loading/SkeletonLoader';
import { ErrorDisplay } from './shared/ErrorDisplay';
import { TravelPlanner } from './TravelPlanner';
import { useAuth } from '../contexts/AuthContext';

interface TripViewLoaderProps {
  itineraryId: string;
  onSave: (updatedTrip: TripData) => void;
  onBack: () => void;
  onShare: () => void;
  onExportPDF: () => void;
}

export function TripViewLoader({ 
  itineraryId, 
  onSave, 
  onBack, 
  onShare, 
  onExportPDF 
}: TripViewLoaderProps) {
  const { data: freshTripData, isLoading, error, refetch } = useItinerary(itineraryId, {
    maxRetries: 1,
    retryDelay: 1000
  });
  const [isRegenerating, setIsRegenerating] = useState(false);
  const { getIdToken } = useAuth();

  const handleRegenerateItinerary = async () => {
    setIsRegenerating(true);
    try {
      // Try to trigger regeneration by calling the agents endpoint
      const response = await fetch(`http://localhost:8080/api/v1/agents/run`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${await getAuthToken()}`
        },
        body: JSON.stringify({
          destination: (freshTripData as TripData)?.endLocation?.name || (freshTripData as TripData)?.destination || 'Unknown',
          startLocation: (freshTripData as TripData)?.startLocation?.name || 'Unknown',
          startDate: (freshTripData as TripData)?.dates?.start || new Date().toISOString().split('T')[0],
          endDate: (freshTripData as TripData)?.dates?.end || new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
          party: (freshTripData as TripData)?.travelers || [{ name: 'Traveler', age: 30 }],
          budgetTier: 'mid', // Default budget tier
          interests: [], // Default empty interests array
          constraints: [], // Default empty constraints array
          language: 'en'
        })
      });
      
      if (response.ok) {
        // Wait a moment then refetch the data
        setTimeout(() => {
          refetch();
          setIsRegenerating(false);
        }, 2000);
      } else {
        console.error('Failed to regenerate itinerary:', response.statusText);
        setIsRegenerating(false);
      }
    } catch (error) {
      console.error('Error regenerating itinerary:', error);
      setIsRegenerating(false);
    }
  };

  const getAuthToken = async () => {
    // Get auth token from auth context
    return await getIdToken();
  };

  // Show loading state while fetching data
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 p-4">
        <LoadingState 
          variant="fullPage" 
          message="Loading trip data... Preparing your itinerary..." 
          size="lg"
        />
      </div>
    );
  }

  // Show error state if data fetch failed
  if (error) {
    return (
      <ErrorDisplay
        error={error}
        onRetry={() => window.location.reload()}
        onGoBack={onBack}
      />
    );
  }

  // Use fresh data from API
  const currentTripData = freshTripData as TripData;

  // Debug logging
  console.log('=== TRIP VIEW LOADER DEBUG ===');
  console.log('Itinerary ID:', itineraryId);
  console.log('Fresh Trip Data:', freshTripData);
  console.log('Current Trip Data:', currentTripData);
  console.log('Trip Status:', currentTripData?.status);
  console.log('Has Itinerary:', !!currentTripData?.itinerary);
  console.log('Days Count:', currentTripData?.itinerary?.days?.length || 0);
  console.log('Days Data:', currentTripData?.itinerary?.days);
  console.log('Full Itinerary Object:', currentTripData?.itinerary);
  console.log('Is Loading:', isLoading);
  console.log('Error:', error);
  console.log('================================');
  
  // Additional error logging
  if (error) {
    console.error('=== TRIP VIEW LOADER ERROR DETAILS ===');
    console.error('Error object:', error);
    console.error('Error message:', (error as Error).message);
    console.error('Error stack:', (error as Error).stack);
    console.error('Error name:', (error as Error).name);
    console.error('=====================================');
  }

  // Check if we have valid itinerary data
  if (!currentTripData?.itinerary?.days || currentTripData.itinerary.days.length === 0) {
    const tripStatus = currentTripData?.status || 'unknown';
    const isGenerating = tripStatus === 'planning' || tripStatus === 'draft';
    
    // If we have a trip but no days, and it's not generating, it might be a failed generation
    const isFailedGeneration = !isGenerating && currentTripData && (!currentTripData.itinerary?.days || currentTripData.itinerary.days.length === 0);
    
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center max-w-md">
          <div className="mb-4">
            <div className={`w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4 ${
              isGenerating ? 'bg-blue-100' : 'bg-yellow-100'
            }`}>
              {isGenerating ? (
                <svg className="w-8 h-8 text-blue-600 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
              ) : (
                <svg className="w-8 h-8 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.732-.833-2.5 0L4.268 19.5c-.77.833.192 2.5 1.732 2.5z" />
                </svg>
              )}
            </div>
            <h2 className="text-xl font-semibold text-gray-900 mb-2">
              {isGenerating ? 'Itinerary Still Generating' : 
               isFailedGeneration ? 'Generation Failed' : 'Itinerary Data Missing'}
            </h2>
            <p className="text-gray-600 mb-6">
              {isGenerating 
                ? 'Your trip itinerary is still being generated. This usually takes a few minutes. Please wait or check back later.'
                : isFailedGeneration
                ? 'The itinerary generation process failed or was interrupted. You can try regenerating the itinerary or contact support if the issue persists.'
                : 'This trip was created but the itinerary data is missing. This can happen if the generation process was interrupted or there was an issue saving the data.'
              }
            </p>
            {isGenerating && (
              <div className="mb-6">
                <div className="flex items-center justify-center space-x-2 text-sm text-gray-500">
                  <div className="w-2 h-2 bg-blue-600 rounded-full animate-pulse"></div>
                  <span>Status: {tripStatus}</span>
                </div>
              </div>
            )}
          </div>
          <div className="space-y-3">
            {!isGenerating && (
              <button
                onClick={handleRegenerateItinerary}
                disabled={isRegenerating}
                className="w-full bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isRegenerating ? 'Regenerating...' : 'Regenerate Itinerary'}
              </button>
            )}
            <details className="w-full">
              <summary className="cursor-pointer text-sm text-gray-500 hover:text-gray-700">
                Debug Information
              </summary>
              <div className="mt-2 p-3 bg-gray-100 rounded text-xs text-gray-600 max-h-40 overflow-y-auto">
                <div><strong>Itinerary ID:</strong> {itineraryId}</div>
                <div><strong>Status:</strong> {currentTripData?.status || 'unknown'}</div>
                <div><strong>Has Itinerary:</strong> {!!currentTripData?.itinerary ? 'Yes' : 'No'}</div>
                <div><strong>Days Count:</strong> {currentTripData?.itinerary?.days?.length || 0}</div>
                <div><strong>Created:</strong> {new Date(currentTripData?.createdAt || 0).toLocaleString()}</div>
                <div><strong>Updated:</strong> {new Date(currentTripData?.updatedAt || 0).toLocaleString()}</div>
                <div><strong>Raw Data:</strong></div>
                <pre className="text-xs mt-1 whitespace-pre-wrap">
                  {JSON.stringify(currentTripData, null, 2)}
                </pre>
              </div>
            </details>
            <button
              onClick={() => {
                console.log('Force refreshing data...');
                refetch();
              }}
              className="w-full bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
            >
              {isGenerating ? 'Check Status' : 'Force Refresh Data'}
            </button>
            <button
              onClick={onBack}
              className="w-full bg-gray-200 text-gray-800 px-4 py-2 rounded-lg hover:bg-gray-300 transition-colors"
            >
              Back to Dashboard
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Data is loaded and valid, render the TravelPlanner
  return (
    <TravelPlanner
      tripData={currentTripData}
      onSave={onSave}
      onBack={onBack}
      onShare={onShare}
      onExportPDF={onExportPDF}
    />
  );
}
