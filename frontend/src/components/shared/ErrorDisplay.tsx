import React from 'react';
import { Button } from '../ui/button';
import { Card } from '../ui/card';
import { getErrorMessage, getErrorIcon, getErrorColor } from '../../utils/errorMessages';
import { 
  AlertCircle, 
  WifiOff, 
  Lock, 
  Search, 
  Server, 
  RefreshCw,
  ArrowLeft,
  LogIn
} from 'lucide-react';

interface ErrorDisplayProps {
  error: Error | string;
  onRetry?: () => void;
  onGoBack?: () => void;
  onSignIn?: () => void;
  className?: string;
  showSuggestions?: boolean;
}

const iconMap = {
  'wifi-off': WifiOff,
  'lock': Lock,
  'search': Search,
  'server': Server,
  'alert-circle': AlertCircle,
};

export function ErrorDisplay({ 
  error, 
  onRetry, 
  onGoBack, 
  onSignIn, 
  className = '',
  showSuggestions = true 
}: ErrorDisplayProps) {
  const errorInfo = getErrorMessage(error);
  const iconName = getErrorIcon(error);
  const iconColor = getErrorColor(error);
  const IconComponent = iconMap[iconName] || AlertCircle;

  const handleAction = () => {
    if (errorInfo.action === 'Sign In' && onSignIn) {
      onSignIn();
    } else if (errorInfo.action === 'Go Back' && onGoBack) {
      onGoBack();
    } else if (errorInfo.isRetryable && onRetry) {
      onRetry();
    } else if (onGoBack) {
      onGoBack();
    }
  };

  const getActionIcon = () => {
    if (errorInfo.action === 'Sign In') return <LogIn className="w-4 h-4" />;
    if (errorInfo.action === 'Go Back') return <ArrowLeft className="w-4 h-4" />;
    if (errorInfo.action?.includes('Try Again') || errorInfo.action?.includes('Wait')) {
      return <RefreshCw className="w-4 h-4" />;
    }
    return <RefreshCw className="w-4 h-4" />;
  };

  return (
    <div className={`min-h-screen bg-gray-50 flex items-center justify-center ${className}`}>
      <Card className="p-8 max-w-lg mx-auto">
        <div className="text-center">
          <div className="mb-6">
            <div className={`mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-red-100 mb-4`}>
              <IconComponent className={`h-6 w-6 ${iconColor}`} />
            </div>
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              {errorInfo.title}
            </h3>
            <p className="text-sm text-gray-600 mb-4">
              {errorInfo.message}
            </p>
          </div>
          
          {showSuggestions && errorInfo.suggestions && errorInfo.suggestions.length > 0 && (
            <div className="mb-6 p-4 bg-gray-50 border border-gray-200 rounded-lg text-left">
              <h4 className="font-medium text-gray-800 mb-2">What you can do:</h4>
              <ul className="text-sm text-gray-700 space-y-1">
                {errorInfo.suggestions.map((suggestion, index) => (
                  <li key={index} className="flex items-start">
                    <span className="text-gray-400 mr-2">•</span>
                    <span>{suggestion}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
          
          <div className="space-y-3">
            <Button 
              onClick={handleAction}
              className="w-full flex items-center justify-center gap-2"
            >
              {getActionIcon()}
              {errorInfo.action}
            </Button>
            
            {errorInfo.isRetryable && onRetry && onGoBack && (
              <Button 
                onClick={onGoBack}
                variant="outline" 
                className="w-full flex items-center justify-center gap-2"
              >
                <ArrowLeft className="w-4 h-4" />
                Go Back
              </Button>
            )}
          </div>
          
          <div className="mt-4 text-xs text-gray-500">
            <p>If this problem persists, please contact support.</p>
          </div>
        </div>
      </Card>
    </div>
  );
}

// Compact version for inline errors
export function InlineErrorDisplay({ 
  error, 
  onRetry, 
  className = '' 
}: Pick<ErrorDisplayProps, 'error' | 'onRetry' | 'className'>) {
  const errorInfo = getErrorMessage(error);
  const iconName = getErrorIcon(error);
  const iconColor = getErrorColor(error);
  const IconComponent = iconMap[iconName] || AlertCircle;

  return (
    <div className={`p-4 bg-red-50 border border-red-200 rounded-lg ${className}`}>
      <div className="flex items-start gap-3">
        <IconComponent className={`h-5 w-5 ${iconColor} flex-shrink-0 mt-0.5`} />
        <div className="flex-1">
          <h4 className="font-medium text-red-800 mb-1">
            {errorInfo.title}
          </h4>
          <p className="text-sm text-red-700 mb-3">
            {errorInfo.message}
          </p>
          {errorInfo.isRetryable && onRetry && (
            <Button 
              onClick={onRetry}
              size="sm"
              variant="outline"
              className="text-red-700 border-red-300 hover:bg-red-100"
            >
              <RefreshCw className="w-3 h-3 mr-1" />
              Try Again
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
