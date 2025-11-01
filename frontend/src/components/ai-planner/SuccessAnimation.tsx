/**
 * Success Animation Component
 * Premium celebration animation with confetti
 */

import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { CheckCircle2, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface SuccessAnimationProps {
  onContinue: () => void;
  message?: string;
}

export function SuccessAnimation({ 
  onContinue, 
  message = 'Your itinerary is ready!' 
}: SuccessAnimationProps) {
  const [showConfetti, setShowConfetti] = useState(true);

  useEffect(() => {
    // Hide confetti after 3 seconds
    const timer = setTimeout(() => {
      setShowConfetti(false);
    }, 3000);

    return () => clearTimeout(timer);
  }, []);

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.5, ease: 'easeOut' }}
      className="text-center space-y-6"
    >
      {/* Confetti */}
      {showConfetti && (
        <div className="fixed inset-0 pointer-events-none overflow-hidden">
          {[...Array(50)].map((_, i) => (
            <motion.div
              key={i}
              className="absolute w-2 h-2 rounded-full"
              style={{
                left: `${Math.random() * 100}%`,
                top: '-10%',
                backgroundColor: [
                  '#002B5B',
                  '#F5C542',
                  '#10B981',
                  '#F59E0B',
                  '#EF4444',
                ][Math.floor(Math.random() * 5)],
              }}
              animate={{
                y: ['0vh', '110vh'],
                x: [0, (Math.random() - 0.5) * 200],
                rotate: [0, Math.random() * 720],
                opacity: [1, 0],
              }}
              transition={{
                duration: 2 + Math.random() * 2,
                ease: 'easeIn',
                delay: Math.random() * 0.5,
              }}
            />
          ))}
        </div>
      )}

      {/* Success Icon */}
      <motion.div
        initial={{ scale: 0 }}
        animate={{ scale: 1 }}
        transition={{ 
          delay: 0.2, 
          type: 'spring', 
          stiffness: 200, 
          damping: 10 
        }}
        className="relative inline-block"
      >
        <div className="w-24 h-24 rounded-full bg-gradient-to-br from-success to-success-600 flex items-center justify-center mx-auto relative">
          <CheckCircle2 className="w-12 h-12 text-white" />
          
          {/* Glow effect */}
          <div className="absolute inset-0 rounded-full bg-success/30 animate-ping" />
        </div>
        
        {/* Sparkles */}
        <motion.div
          animate={{ 
            rotate: 360,
            scale: [1, 1.2, 1],
          }}
          transition={{ 
            duration: 2, 
            repeat: Infinity,
            ease: 'linear'
          }}
          className="absolute -top-2 -right-2"
        >
          <Sparkles className="w-6 h-6 text-secondary" />
        </motion.div>
      </motion.div>

      {/* Success Message */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.4 }}
        className="space-y-2"
      >
        <h2 className="text-3xl font-bold bg-gradient-to-r from-primary to-primary-600 bg-clip-text text-transparent">
          {message}
        </h2>
        <p className="text-muted-foreground">
          Let's explore your personalized travel plan
        </p>
      </motion.div>

      {/* Continue Button */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.6 }}
      >
        <Button 
          size="lg" 
          onClick={onContinue}
          className="px-8 py-6 text-lg"
        >
          View My Itinerary
        </Button>
      </motion.div>
    </motion.div>
  );
}
