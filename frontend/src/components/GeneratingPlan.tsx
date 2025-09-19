import React, { useState, useEffect } from 'react';
import { Button } from './ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Progress } from './ui/progress';
import { Badge } from './ui/badge';
import { MapPin, Clock, Route, Star, X } from 'lucide-react';
import { TripData } from '../types/TripData';
import { AgentOrchestrator } from './AgentOrchestrator';

interface GeneratingPlanProps {
  tripData: TripData;
  onComplete: (itinerary: any) => void;
  onCancel: () => void;
}

const planningSteps = [
  { id: 1, title: 'Analyzing destination', description: 'Finding popular attractions and hidden gems', duration: 2000 },
  { id: 2, title: 'Fetching POIs', description: 'Gathering points of interest based on your preferences', duration: 1500 },
  { id: 3, title: 'Estimating travel times', description: 'Calculating optimal routes and distances', duration: 1800 },
  { id: 4, title: 'Checking opening hours', description: 'Ensuring activities fit your schedule', duration: 1200 },
  { id: 5, title: 'Scoring activities', description: 'Ranking based on your interests and budget', duration: 1500 },
  { id: 6, title: 'Building itinerary', description: 'Creating your personalized day-by-day plan', duration: 2000 }
];

export function GeneratingPlan({ tripData, onComplete, onCancel }: GeneratingPlanProps) {
  const [currentStep, setCurrentStep] = useState(0);
  const [progress, setProgress] = useState(0);
  const [showAgentOrchestrator, setShowAgentOrchestrator] = useState(false);

  useEffect(() => {
    const generatePlan = async () => {
      // Show agent orchestrator after a brief delay
      setTimeout(() => {
        setShowAgentOrchestrator(true);
      }, 1000);

      for (let i = 0; i < planningSteps.length; i++) {
        setCurrentStep(i);
        
        // Simulate API call duration
        await new Promise(resolve => setTimeout(resolve, planningSteps[i].duration));
        
        setProgress(((i + 1) / planningSteps.length) * 100);
      }

      // Generate mock itinerary based on trip data
      const mockItinerary = generateMockItinerary(tripData);
      onComplete(mockItinerary);
    };

    generatePlan();
  }, [tripData, onComplete]);

  const generateMockItinerary = (trip: TripData) => {
    const startDate = new Date(trip.dates.start);
    const endDate = new Date(trip.dates.end);
    const days = Math.ceil((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24));
    
    const itinerary = [];
    
    for (let day = 0; day < days; day++) {
      const currentDate = new Date(startDate);
      currentDate.setDate(startDate.getDate() + day);
      
      const activities = [];
      
      // Morning activity
      if (trip.themes.includes('Heritage') || trip.themes.includes('Culture')) {
        activities.push({
          id: `${day}-morning`,
          time: '09:00',
          title: day === 0 ? 'Historical City Tour' : 'Museum Visit',
          type: 'cultural',
          duration: 120,
          description: 'Explore the rich cultural heritage',
          location: { lat: 28.6139 + (Math.random() - 0.5) * 0.1, lng: 77.2090 + (Math.random() - 0.5) * 0.1 },
          openingHours: '09:00 - 18:00',
          dietSuitable: true,
          walkingTime: 15
        });
      } else if (trip.themes.includes('Adventure')) {
        activities.push({
          id: `${day}-morning`,
          time: '08:00',
          title: 'Adventure Sports',
          type: 'adventure',
          duration: 180,
          description: 'Thrilling outdoor activities',
          location: { lat: 28.6139 + (Math.random() - 0.5) * 0.1, lng: 77.2090 + (Math.random() - 0.5) * 0.1 },
          openingHours: '08:00 - 17:00',
          dietSuitable: true,
          walkingTime: 20
        });
      }

      // Lunch
      activities.push({
        id: `${day}-lunch`,
        time: '12:30',
        title: trip.dietaryRestrictions.includes('Vegetarian') ? 'Vegetarian Restaurant' : 'Local Cuisine',
        type: 'food',
        duration: 90,
        description: 'Authentic local flavors',
        location: { lat: 28.6139 + (Math.random() - 0.5) * 0.1, lng: 77.2090 + (Math.random() - 0.5) * 0.1 },
        openingHours: '11:00 - 22:00',
        dietSuitable: true,
        walkingTime: 10
      });

      // Afternoon activity
      if (trip.themes.includes('Nature')) {
        activities.push({
          id: `${day}-afternoon`,
          time: '15:00',
          title: 'Nature Walk',
          type: 'nature',
          duration: 120,
          description: 'Peaceful nature experience',
          location: { lat: 28.6139 + (Math.random() - 0.5) * 0.1, lng: 77.2090 + (Math.random() - 0.5) * 0.1 },
          openingHours: '06:00 - 19:00',
          dietSuitable: true,
          walkingTime: 25
        });
      } else if (trip.themes.includes('Shopping')) {
        activities.push({
          id: `${day}-afternoon`,
          time: '14:00',
          title: 'Local Markets',
          type: 'shopping',
          duration: 150,
          description: 'Browse local crafts and souvenirs',
          location: { lat: 28.6139 + (Math.random() - 0.5) * 0.1, lng: 77.2090 + (Math.random() - 0.5) * 0.1 },
          openingHours: '10:00 - 20:00',
          dietSuitable: true,
          walkingTime: 30
        });
      }

      // Evening
      if (trip.themes.includes('Nightlife') && day < days - 1) {
        activities.push({
          id: `${day}-evening`,
          time: '19:00',
          title: 'Evening Entertainment',
          type: 'nightlife',
          duration: 180,
          description: 'Experience the local nightlife',
          location: { lat: 28.6139 + (Math.random() - 0.5) * 0.1, lng: 77.2090 + (Math.random() - 0.5) * 0.1 },
          openingHours: '18:00 - 02:00',
          dietSuitable: true,
          walkingTime: 15
        });
      }

      itinerary.push({
        day: day + 1,
        date: currentDate.toISOString(),
        activities,
        totalWalkingTime: activities.reduce((sum, activity) => sum + activity.walkingTime, 0),
        totalDuration: activities.reduce((sum, activity) => sum + activity.duration, 0)
      });
    }

    return {
      days: itinerary,
      totalDays: days,
      estimatedCost: trip.budget * 0.8, // Use 80% of budget
      weather: {
        forecast: Array.from({ length: days }, (_, i) => ({
          date: new Date(startDate.getTime() + i * 24 * 60 * 60 * 1000).toISOString(),
          temperature: { high: 25 + Math.random() * 10, low: 15 + Math.random() * 5 },
          condition: ['sunny', 'partly-cloudy', 'cloudy'][Math.floor(Math.random() * 3)]
        }))
      }
    };
  };

  return (
    <div className="min-h-screen bg-gray-50 p-4">
      <div className="max-w-2xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <Button variant="ghost" onClick={onCancel}>
            <X className="h-4 w-4 mr-2" />
            Cancel
          </Button>
          <Badge variant="outline">
            {Math.round(progress)}% Complete
          </Badge>
        </div>

        {/* Main Card */}
        <Card className="text-center">
          <CardHeader className="pb-4">
            <div className="w-16 h-16 bg-indigo-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <span className="text-2xl">🤖</span>
            </div>
            <CardTitle className="text-2xl">Creating Your Perfect Trip</CardTitle>
            <CardDescription>
              We're analyzing your preferences and building a personalized itinerary for {tripData.destination}
            </CardDescription>
          </CardHeader>
          
          <CardContent className="space-y-6">
            {/* Progress Bar */}
            <div className="space-y-2">
              <Progress value={progress} className="w-full" />
              <p className="text-sm text-gray-600">
                {progress < 100 ? `Processing step ${currentStep + 1} of ${planningSteps.length}` : 'Finalizing your itinerary...'}
              </p>
            </div>

            {/* Current Step */}
            {currentStep < planningSteps.length && (
              <div className="text-left bg-blue-50 p-4 rounded-lg">
                <div className="flex items-center gap-3 mb-2">
                  <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">
                    <span className="text-white text-sm">{currentStep + 1}</span>
                  </div>
                  <h4 className="font-medium">{planningSteps[currentStep].title}</h4>
                </div>
                <p className="text-sm text-gray-600 ml-11">
                  {planningSteps[currentStep].description}
                </p>
              </div>
            )}

            {/* Trip Summary */}
            <div className="grid grid-cols-2 gap-4 pt-4 border-t">
              <div className="flex items-center gap-2">
                <MapPin className="h-4 w-4 text-gray-400" />
                <span className="text-sm">{tripData.destination}</span>
              </div>
              <div className="flex items-center gap-2">
                <Clock className="h-4 w-4 text-gray-400" />
                <span className="text-sm">
                  {Math.ceil((new Date(tripData.dates.end).getTime() - new Date(tripData.dates.start).getTime()) / (1000 * 60 * 60 * 24))} days
                </span>
              </div>
              <div className="flex items-center gap-2">
                <Star className="h-4 w-4 text-gray-400" />
                <span className="text-sm">{tripData.themes.slice(0, 2).join(', ')}</span>
              </div>
              <div className="flex items-center gap-2">
                <Route className="h-4 w-4 text-gray-400" />
                <span className="text-sm">₹{tripData.budget.toLocaleString()}</span>
              </div>
            </div>

            {/* What we're doing */}
            <div className="text-left space-y-3">
              <h4 className="font-medium text-gray-800">What we're doing:</h4>
              <ul className="space-y-2 text-sm text-gray-600">
                <li className="flex items-center gap-2">
                  <div className={`w-2 h-2 rounded-full ${currentStep >= 0 ? 'bg-green-500' : 'bg-gray-300'}`} />
                  Finding attractions matching your interests
                </li>
                <li className="flex items-center gap-2">
                  <div className={`w-2 h-2 rounded-full ${currentStep >= 2 ? 'bg-green-500' : 'bg-gray-300'}`} />
                  Optimizing routes to minimize travel time
                </li>
                <li className="flex items-center gap-2">
                  <div className={`w-2 h-2 rounded-full ${currentStep >= 4 ? 'bg-green-500' : 'bg-gray-300'}`} />
                  Checking dietary requirements and accessibility
                </li>
                <li className="flex items-center gap-2">
                  <div className={`w-2 h-2 rounded-full ${progress >= 100 ? 'bg-green-500' : 'bg-gray-300'}`} />
                  Balancing budget across activities
                </li>
              </ul>
            </div>
          </CardContent>
        </Card>

        {/* Fun Fact */}
        <div className="mt-6 p-4 bg-indigo-50 rounded-lg">
          <p className="text-sm text-indigo-700">
            <span className="font-medium">Did you know?</span> We analyze over 10,000 data points 
            to create your personalized itinerary, including real-time opening hours, weather patterns, 
            and local events.
          </p>
        </div>
      </div>

      {/* Agent Orchestrator Modal */}
      <AgentOrchestrator 
        isOpen={showAgentOrchestrator}
        onClose={() => setShowAgentOrchestrator(false)}
        onComplete={() => {
          setShowAgentOrchestrator(false);
          // Continue with normal flow
        }}
      />
    </div>
  );
}