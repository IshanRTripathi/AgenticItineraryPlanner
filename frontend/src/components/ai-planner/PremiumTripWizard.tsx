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
import { cn } from '@/lib/utils';
import { useTranslation } from '@/i18n';

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
    const { t } = useTranslation();
    const [currentStep, setCurrentStep] = useState(1);
    const [formData, setFormData] = useState<TripFormData>({
        origin: 'Bengaluru, Karnataka, India',
    });
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [direction, setDirection] = useState<'forward' | 'backward'>('forward');

    const STEPS = [
        { id: 1, title: t('pages.planner.steps.destination'), component: PremiumDestinationStep },
        { id: 2, title: t('pages.planner.steps.datesTravelers'), component: PremiumDatesTravelersStep },
        { id: 3, title: t('pages.planner.steps.preferences'), component: PremiumPreferencesStep },
        { id: 4, title: t('pages.planner.steps.review'), component: ReviewStep },
    ];

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
            const budgetMin = formData.budgetRange?.[0] || 500;
            const budgetMax = formData.budgetRange?.[1] || 2000;
            
            // Determine budget tier based on range
            let budgetTier = 'moderate';
            if (budgetMax <= 1000) {
                budgetTier = 'budget';
            } else if (budgetMax >= 2500) {
                budgetTier = 'luxury';
            }
            
            const response = await api.post<any>(endpoints.createItinerary, {
                destination: formData.destination,
                origin: formData.origin, // Store for future use
                startDate: formData.startDate,
                endDate: formData.endDate,
                adults: formData.adults || 2,
                children: formData.children || 0,
                infants: formData.infants || 0,
                budgetTier: budgetTier,
                budgetMin: budgetMin,
                budgetMax: budgetMax,
                pace: formData.pace || 'moderate',
                interests: formData.interests || [],
                constraints: formData.customInstructions ? [formData.customInstructions] : [],
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
        <div className="min-h-screen bg-gradient-to-br from-gray-50 via-white to-primary/5 py-4 sm:py-8 px-3 sm:px-4">
            <motion.div
                className="max-w-5xl mx-auto"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
            >
                <Card className="p-4 sm:p-6 md:p-8 shadow-2xl border-0 bg-white/80 backdrop-blur-sm">
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
                    <div className="my-6 sm:my-8">
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
                        className="flex flex-col sm:flex-row justify-between gap-3 sm:gap-4 pt-6 sm:pt-8 border-t border-border"
                        variants={fadeInUp}
                        initial="initial"
                        animate="animate"
                        transition={{ delay: 0.2 }}
                    >
                        <Button
                            variant="outline"
                            onClick={handleBack}
                            disabled={isFirstStep || isSubmitting}
                            className="w-full sm:w-auto sm:min-w-32 h-11 sm:h-12 touch-manipulation active:scale-95"
                            size="lg"
                        >
                            <ArrowLeft className="w-4 h-4 mr-2" />
                            {t('pages.planner.navigation.back')}
                        </Button>

                        <Button
                            onClick={handleNext}
                            disabled={!isStepValid() || isSubmitting}
                            className={cn(
                                "w-full sm:w-auto sm:min-w-32 h-12 sm:h-14 shadow-lg hover:shadow-xl transition-all touch-manipulation active:scale-95 font-semibold text-base sm:text-lg",
                                isLastStep 
                                    ? "bg-gradient-to-r from-green-600 via-green-500 to-emerald-600 hover:from-green-700 hover:via-green-600 hover:to-emerald-700 text-white relative overflow-hidden group"
                                    : "bg-gradient-to-r from-primary to-primary-600 hover:from-primary-600 hover:to-primary-700"
                            )}
                            size="lg"
                        >
                            {isSubmitting ? (
                                <>
                                    <Loader2 className="w-5 h-5 mr-2 animate-spin" />
                                    <span>{t('pages.planner.navigation.creatingTrip')}</span>
                                </>
                            ) : isLastStep ? (
                                <>
                                    {/* Shimmer effect for Create button */}
                                    <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent group-hover:animate-shimmer-fast" />
                                    <span className="relative z-10">{t('pages.planner.navigation.createItinerary')}</span>
                                </>
                            ) : (
                                <>
                                    <span>{t('pages.planner.navigation.nextStep')}</span>
                                    <ArrowRight className="w-5 h-5 ml-2" />
                                </>
                            )}
                        </Button>
                    </motion.div>

                    {/* Step Indicator */}
                    <motion.div
                        className="text-center mt-4 sm:mt-6 text-xs sm:text-sm text-muted-foreground"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        transition={{ delay: 0.3 }}
                    >
                        {t('pages.planner.navigation.stepOf', { current: currentStep, total: STEPS.length })}
                    </motion.div>
                </Card>
            </motion.div>

            {/* Add shimmer animation for Create button */}
            <style>{`
                @keyframes shimmer-fast {
                    0% { transform: translateX(-100%); }
                    100% { transform: translateX(100%); }
                }
                .group:hover .group-hover\\:animate-shimmer-fast {
                    animation: shimmer-fast 1.5s infinite;
                }
            `}</style>
        </div>
    );
}
