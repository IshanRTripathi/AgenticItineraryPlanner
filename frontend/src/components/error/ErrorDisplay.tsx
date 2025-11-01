/**
 * Error Display Component
 * Shows user-friendly error messages with retry functionality
 */

import { Header } from '@/components/layout/Header';
import { Footer } from '@/components/layout/Footer';
import { Button } from '@/components/ui/button';
import { AlertCircle } from 'lucide-react';

interface ErrorDisplayProps {
  error: Error | null;
  onRetry?: () => void;
  onGoBack?: () => void;
}

export function ErrorDisplay({ error, onRetry, onGoBack }: ErrorDisplayProps) {
  const errorMessage = error?.message || 'An unexpected error occurred';
  
  // Determine error type for better UX
  const is404 = errorMessage.includes('404') || errorMessage.includes('not found');
  const is401 = errorMessage.includes('401') || errorMessage.includes('unauthorized');
  const isNetwork = errorMessage.includes('network') || errorMessage.includes('fetch');

  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      <main className="flex-1 flex items-center justify-center p-4">
        <div className="text-center max-w-md">
          <div className="mb-6">
            <AlertCircle className="w-16 h-16 text-destructive mx-auto mb-4" />
            
            <h2 className="text-2xl font-bold mb-2">
              {is404 && 'Trip Not Found'}
              {is401 && 'Access Denied'}
              {isNetwork && 'Connection Error'}
              {!is404 && !is401 && !isNetwork && 'Something Went Wrong'}
            </h2>
            
            <p className="text-muted-foreground mb-6">
              {is404 && 'The trip you\'re looking for doesn\'t exist or has been deleted.'}
              {is401 && 'You don\'t have permission to view this trip.'}
              {isNetwork && 'Unable to connect to the server. Please check your internet connection.'}
              {!is404 && !is401 && !isNetwork && errorMessage}
            </p>
          </div>

          <div className="space-y-3">
            {onRetry && !is404 && !is401 && (
              <Button onClick={onRetry} className="w-full">
                Try Again
              </Button>
            )}
            
            {onGoBack && (
              <Button onClick={onGoBack} variant="outline" className="w-full">
                Go Back
              </Button>
            )}
            
            <Button
              onClick={() => window.location.href = '/dashboard'}
              variant="ghost"
              className="w-full"
            >
              Go to Dashboard
            </Button>
          </div>

          {/* Debug info (only in development) */}
          {import.meta.env.DEV && (
            <details className="mt-6 text-left">
              <summary className="cursor-pointer text-sm text-muted-foreground hover:text-foreground">
                Technical Details
              </summary>
              <pre className="mt-2 p-3 bg-muted rounded text-xs overflow-auto max-h-40">
                {error?.stack || errorMessage}
              </pre>
            </details>
          )}
        </div>
      </main>

      <Footer />
    </div>
  );
}
