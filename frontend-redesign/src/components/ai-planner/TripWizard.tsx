/**
 * Trip Wizard Component
 * Premium 4-step wizard for AI trip planning
 */

import { useState } from 'react';
import { Card } from '@/components/ui/card';
import { WizardProgress } from './WizardProgress';
import { DestinationStep } from './steps/DestinationStep';
import { DatesTravelersStep } from './steps/DatesTravelersStep';
import { PreferencesStep } from './steps/PreferencesStep';
import { ReviewStep } from './steps/ReviewStep';
import { Button } from '@/components/ui/button';
import { ArrowLeft, ArrowRight, Sparkles } from 'lucide-react';
import { api, endpoints } from '@/services/api';

const STEPS = [
    { id: 1, title: 'Destination', component: DestinationStep },
    { id: 2, title: 'Dates & Travelers', component: DatesTravelersStep },
    { id: 3, title: 'Preferences', component: PreferencesStep },
    { id: 4, title: 'Review', component: ReviewStep },
];

interface TripFormData {
    destination?: string;
    startDate?: string;
    endDate?: string;
    adults?: number;
    children?: number;
    infants?: number;
    budget?: string;
    pace?: string;
    interests?: string[];
    [key: string]: any;
}

export function TripWizard() {
    const [currentStep, setCurrentStep] = useState(1);
    const [formData, setFormData] = useState<TripFormData>({});

    const CurrentStepComponent = STEPS[currentStep - 1].component;
    const isFirstStep = currentStep === 1;
    const isLastStep = currentStep === STEPS.length;

    const handleNext = () => {
        if (currentStep < STEPS.length) {
            setCurrentStep(currentStep + 1);
        } else {
            // Submit and navigate to progress
            handleSubmit();
        }
    };

    const handleSubmit = async () => {
        try {
            // Call backend API to create itinerary
            const response = await api.post<any>(endpoints.createItinerary, {
                destination: formData.destination,
                startDate: formData.startDate,
                endDate: formData.endDate,
                adults: formData.adults || 2,
                children: formData.children || 0,
                infants: formData.infants || 0,
                budget: formData.budget || 'mid-range',
                pace: formData.pace || 'moderate',
                interests: formData.interests || [],
            });

            if (response.success && response.executionId) {
                // Navigate to progress page with executionId
                window.location.href = `/ai-progress?executionId=${response.executionId}&itineraryId=${response.itineraryId}`;
            } else {
                console.error('Failed to create itinerary:', response.error);
                alert('Failed to create itinerary. Please try again.');
            }
        } catch (error) {
            console.error('Error creating itinerary:', error);
            alert('An error occurred. Please try again.');
        }
    };

    const handleBack = () => {
        if (currentStep > 1) {
            setCurrentStep(currentStep - 1);
        }
    };

    const handleStepData = (data: any) => {
        setFormData({ ...formData, ...data });
    };

    return (
        <Card className="max-w-4xl mx-auto p-8 shadow-elevation-3">
            {/* Header */}
            <div className="mb-8 text-center">
                <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gradient-to-br from-primary to-primary-600 mb-4">
                    <Sparkles className="w-8 h-8 text-white" />
                </div>
                <h1 className="text-3xl font-bold text-foreground mb-2">
                    Plan Your Perfect Trip
                </h1>
                <p className="text-muted-foreground">
                    Let our AI create a personalized itinerary just for you
                </p>
            </div>

            {/* Progress Indicator */}
            <WizardProgress currentStep={currentStep} steps={STEPS} />

            {/* Step Content */}
            <div className="my-8">
                <CurrentStepComponent
                    data={formData}
                    onDataChange={handleStepData}
                />
            </div>

            {/* Navigation */}
            <div className="flex justify-between gap-4">
                <Button
                    variant="outline"
                    onClick={handleBack}
                    disabled={isFirstStep}
                    className="min-w-32"
                >
                    <ArrowLeft className="w-4 h-4 mr-2" />
                    Back
                </Button>

                <Button
                    onClick={handleNext}
                    className="min-w-32"
                >
                    {isLastStep ? 'Create Itinerary' : 'Next'}
                    {!isLastStep && <ArrowRight className="w-4 h-4 ml-2" />}
                </Button>
            </div>
        </Card>
    );
}
