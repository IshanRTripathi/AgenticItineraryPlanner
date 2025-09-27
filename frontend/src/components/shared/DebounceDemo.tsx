import React, { useState } from 'react';
import { Button } from '../ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { useFormSubmission } from '../../hooks/useFormSubmission';

/**
 * Demo component to showcase the debouncing functionality
 * This can be used for testing and demonstration purposes
 */
export const DebounceDemo: React.FC = () => {
  const [submissionCount, setSubmissionCount] = useState(0);
  const [lastSubmissionTime, setLastSubmissionTime] = useState<Date | null>(null);

  const { isSubmitting, submit: submitForm, error, reset } = useFormSubmission({
    debounceMs: 2000, // 2 second debounce
    onSuccess: () => {
      setSubmissionCount(prev => prev + 1);
      setLastSubmissionTime(new Date());
    },
    onError: (error) => {
      console.error('Demo submission error:', error);
    }
  });

  const handleSubmit = async () => {
    await submitForm(async () => {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Simulate random failure (10% chance)
      if (Math.random() < 0.1) {
        throw new Error('Random failure for demo purposes');
      }
      
      return { success: true, timestamp: new Date().toISOString() };
    });
  };

  const handleReset = () => {
    reset();
    setSubmissionCount(0);
    setLastSubmissionTime(null);
  };

  return (
    <Card className="w-full max-w-md mx-auto">
      <CardHeader>
        <CardTitle>Debounce Demo</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="text-sm text-gray-600">
          <p>Try clicking the button multiple times quickly to see debouncing in action.</p>
          <p className="mt-2">
            <strong>Debounce period:</strong> 2 seconds
          </p>
        </div>

        <div className="space-y-2">
          <Button 
            onClick={handleSubmit}
            disabled={isSubmitting}
            className="w-full"
          >
            {isSubmitting ? (
              <>
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                Processing...
              </>
            ) : (
              'Submit (Demo)'
            )}
          </Button>

          <Button 
            onClick={handleReset}
            variant="outline"
            className="w-full"
            disabled={isSubmitting}
          >
            Reset Demo
          </Button>
        </div>

        <div className="text-sm space-y-1">
          <p><strong>Successful submissions:</strong> {submissionCount}</p>
          {lastSubmissionTime && (
            <p><strong>Last submission:</strong> {lastSubmissionTime.toLocaleTimeString()}</p>
          )}
        </div>

        {error && (
          <div className="p-3 bg-red-50 border border-red-200 rounded-md">
            <p className="text-sm text-red-600">
              <strong>Error:</strong> {error.message}
            </p>
          </div>
        )}

        <div className="text-xs text-gray-500">
          <p>💡 <strong>Tip:</strong> Rapid clicks will be ignored during the debounce period.</p>
        </div>
      </CardContent>
    </Card>
  );
};
