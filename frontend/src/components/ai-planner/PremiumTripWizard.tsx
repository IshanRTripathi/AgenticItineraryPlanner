/**
 * Premium Trip Wizard Component
 * Enhanced 4-step wizard using premium UI components
 */

import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Card } from '@/components/ui/card';
import { WizardProgress } from './WizardProgress';
import { PremiumDestinationStep } from './steps/PremiumDestinationStep';
import { PremiumDatesTravelersStep } from './steps/PremiumDatesTravelersStep';
import { PremiumPreferencesStep } from './steps/PremiumPreferencesStep';
import { ReviewStep } from './steps/ReviewStep';
import { Button } from '@/components/ui/button';
import { ArrowLeft, ArrowRight, Sparkles, Loader2 } from 'lucide-react';
import { api, endpoints } from '@/services/api';
import { fadeInUp, slideInRight, slideInLeft } from '@/lib/animations/variants';

const STEPS = [
    { id: 1, title: 'Destination', component: PremiumDestinationStep },
    { id: 2, title: 'Dates & Travelers', component: PremiumDatesTravelersStep },
    { id: 3, title: 'Preferences', component: PremiumPreferencesStep },
    { id: 4, title: 'Review', component: ReviewStep },
];

interface TripFormData {
    origin?: string;
    destination?: string;
    startDate?: string;
    endDate?: string;
    adults?: number;
    children?: number;
    infants?: number;
    budgetRange?: [number, number];
    pace?: string;
    interests?: string[];
    [key: string]: any;
}

export function PremiumTripWizard() {
    const [currentStep, setCurrentStep] = useState(1);
    const [formData, setFormData] = useState<TripFormData>({
        origin: 'Bengaluru, Karnataka, India',
    });
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [direction, setDirection] = useState<'forward' | 'backward'>('forward');

    const CurrentStepComponent = STEPS[currentStep - 1].component;
    const isFirstStep = currentStep === 1;
    const isLastStep = currentStep === STEPS.length;

    const handleNext = async () => {
        if (currentStep < STEPS.length) {
            setDirection('forward');
            setCurrentStep(currentStep + 1);
        } else {
            // Submit and navigate to progress
            await handleSubmit();
        }
    };

    const handleSubmit = async () => {
        setIsSubmitting(true);
        try {
            // Call backend API to create itinerary
            // Note: Backend currently only uses destination, origin is stored for future use
            const response = await api.post<any>(endpoints.createItinerary, {
                destination: formData.destination,
                origin: formData.origin, // Store for future use
                startDate: formData.startDate,
                endDate: formData.endDate,
                adults: formData.adults || 2,
                children: formData.children || 0,
                infants: formData.infants || 0,
                budgetMin: formData.budgetRange?.[0] || 500,
                budgetMax: formData.budgetRange?.[1] || 2000,
                pace: formData.pace || 'moderate',
                interests: formData.interests || [],
            });

            console.log('[PremiumTripWizard] Create itinerary response:', response);

            // Response from backend: { itinerary, executionId, status, stages }
            // CRITICAL: Only itineraryId is used for WebSocket - executionId is just for tracking
            const { itinerary } = response;
            const itineraryId = itinerary?.id;

            if (itineraryId) {
                console.log('[PremiumTripWizard] Navigating to planner progress:', { itineraryId });
                // Only pass itineraryId - it's the only identifier needed for WebSocket
                window.location.href = `/planner-progress?itineraryId=${itineraryId}`;
            } else {
                console.error('Missing itineraryId in response:', response);
                alert('Failed to create itinerary. Missing required data.');
            }
        } catch (error) {
            console.error('Error creating itinerary:', error);
            const errorMessage = error instanceof Error ? error.message : 'An error occurred. Please try again.';
            alert(errorMessage);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleBack = () => {
        if (currentStep > 1) {
            setDirection('backward');
            setCurrentStep(currentStep - 1);
        }
    };

    const handleStepData = (data: any) => {
        setFormData({ ...formData, ...data });
    };

    // Determine if current step is valid
    const isStepValid = () => {
        switch (currentStep) {
            case 1:
                return !!formData.destination;
            case 2:
                return !!formData.startDate && !!formData.endDate;
            case 3:
                return true; // Preferences are optional
            case 4:
                return true; // Review step
            default:
                return false;
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-gray-50 via-white to-primary/5 py-8 px-4">
            <motion.div
                className="max-w-5xl mx-auto"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
            >
                <Card className="p-6 md:p-8 shadow-2xl border-0 bg-white/80 backdrop-blur-sm">
                    {/* Header - More Compact */}
                    <motion.div
                        className="mb-6 text-center"
                        variants={fadeInUp}
                        initial="initial"
                        animate="animate"
                    >
                        <motion.div
                            className="inline-flex items-center justify-center w-14 h-14 rounded-full bg-gradient-to-br from-primary to-primary-600 mb-4 shadow-lg"
                            whileHover={{ scale: 1.05, rotate: 5 }}
                            transition={{ type: 'spring', stiffness: 300 }}
                        >
                            <Sparkles className="w-7 h-7 text-white" />
                        </motion.div>
                        <h1 className="text-3xl md:text-4xl font-bold text-foreground mb-2 bg-gradient-to-r from-primary to-primary-600 bg-clip-text text-transparent">
                            Plan Your Perfect Trip
                        </h1>
                        <p className="text-sm text-muted-foreground max-w-2xl mx-auto">
                            Let our AI create a personalized itinerary with premium search experience
                        </p>
                    </motion.div>

                    {/* Progress Indicator */}
                    <motion.div
                        variants={fadeInUp}
                        initial="initial"
                        animate="animate"
                        transition={{ delay: 0.1 }}
                    >
                        <WizardProgress currentStep={currentStep} steps={STEPS} />
                    </motion.div>

                    {/* Step Content with Animations */}
                    <div className="my-8">
                        <AnimatePresence mode="wait" custom={direction}>
                            <motion.div
                                key={currentStep}
                                custom={direction}
                                variants={direction === 'forward' ? slideInRight : slideInLeft}
                                initial="initial"
                                animate="animate"
                                exit="exit"
                                transition={{ duration: 0.3, ease: 'easeInOut' }}
                            >
                                <CurrentStepComponent
                                    data={formData}
                                    onDataChange={handleStepData}
                                />
                            </motion.div>
                        </AnimatePresence>
                    </div>

                    {/* Navigation */}
                    <motion.div
                        className="flex justify-between gap-4 pt-8 border-t border-border"
                        variants={fadeInUp}
                        initial="initial"
                        animate="animate"
                        transition={{ delay: 0.2 }}
                    >
                        <Button
                            variant="outline"
                            onClick={handleBack}
                            disabled={isFirstStep || isSubmitting}
                            className="min-w-32 h-12"
                            size="lg"
                        >
                            <ArrowLeft className="w-4 h-4 mr-2" />
                            Back
                        </Button>

                        <Button
                            onClick={handleNext}
                            disabled={!isStepValid() || isSubmitting}
                            className="min-w-32 h-12 bg-gradient-to-r from-primary to-primary-600 hover:from-primary-600 hover:to-primary-700 shadow-lg hover:shadow-xl transition-all"
                            size="lg"
                        >
                            {isSubmitting ? (
                                <>
                                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                                    Creating...
                                </>
                            ) : isLastStep ? (
                                <>
                                    <Sparkles className="w-4 h-4 mr-2" />
                                    Create Itinerary
                                </>
                            ) : (
                                <>
                                    Next
                                    <ArrowRight className="w-4 h-4 ml-2" />
                                </>
                            )}
                        </Button>
                    </motion.div>

                    {/* Step Indicator */}
                    <motion.div
                        className="text-center mt-6 text-sm text-muted-foreground"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        transition={{ delay: 0.3 }}
                    >
                        Step {currentStep} of {STEPS.length}
                    </motion.div>
                </Card>
            </motion.div>
        </div>
    );
}
