// Error message utilities for user-friendly error handling

export interface ErrorInfo {
  title: string;
  message: string;
  action?: string;
  suggestions?: string[];
  isRetryable?: boolean;
}

export function getErrorMessage(error: Error | string): ErrorInfo {
  const errorString = typeof error === 'string' ? error : error.message;
  
  // Network/Connection errors
  if (errorString.includes('Failed to fetch') || errorString.includes('NetworkError')) {
    return {
      title: 'Connection Problem',
      message: 'Unable to connect to the server. Please check your internet connection.',
      action: 'Try Again',
      suggestions: [
        'Check your internet connection',
        'Try refreshing the page',
        'Wait a moment and try again'
      ],
      isRetryable: true
    };
  }
  
  // Authentication errors
  if (errorString.includes('401') || errorString.includes('Unauthorized')) {
    return {
      title: 'Authentication Required',
      message: 'You need to sign in to access this feature.',
      action: 'Sign In',
      suggestions: [
        'Please sign in with your Google account',
        'If you\'re already signed in, try refreshing the page'
      ],
      isRetryable: false
    };
  }
  
  // Authorization errors
  if (errorString.includes('403') || errorString.includes('Forbidden')) {
    return {
      title: 'Access Denied',
      message: 'You don\'t have permission to perform this action.',
      action: 'Go Back',
      suggestions: [
        'Contact support if you believe this is an error',
        'Try signing out and signing back in'
      ],
      isRetryable: false
    };
  }
  
  // Not found errors
  if (errorString.includes('404') || errorString.includes('Not Found')) {
    return {
      title: 'Not Found',
      message: 'The requested resource could not be found.',
      action: 'Go Back',
      suggestions: [
        'The trip or data you\'re looking for may have been deleted',
        'Try refreshing the page or going back to the dashboard'
      ],
      isRetryable: false
    };
  }
  
  // Server errors
  if (errorString.includes('500') || errorString.includes('Internal Server Error')) {
    return {
      title: 'Server Error',
      message: 'Something went wrong on our end. We\'re working to fix it.',
      action: 'Try Again',
      suggestions: [
        'Wait a moment and try again',
        'If the problem persists, contact support'
      ],
      isRetryable: true
    };
  }
  
  // Timeout errors
  if (errorString.includes('408') || errorString.includes('timeout') || errorString.includes('Timeout')) {
    return {
      title: 'Request Timeout',
      message: 'The request took too long to complete.',
      action: 'Try Again',
      suggestions: [
        'The server might be busy, try again in a moment',
        'Check your internet connection'
      ],
      isRetryable: true
    };
  }
  
  // Rate limiting
  if (errorString.includes('429') || errorString.includes('Too Many Requests')) {
    return {
      title: 'Too Many Requests',
      message: 'You\'re making requests too quickly. Please slow down.',
      action: 'Wait and Try Again',
      suggestions: [
        'Wait a few moments before trying again',
        'Avoid rapid clicking or refreshing'
      ],
      isRetryable: true
    };
  }
  
  // Trip generation specific errors
  if (errorString.includes('Trip generation failed') || errorString.includes('Agent failed')) {
    return {
      title: 'Trip Generation Failed',
      message: 'We couldn\'t create your itinerary. This might be due to server issues or invalid parameters.',
      action: 'Try Again',
      suggestions: [
        'Check that your trip details are valid',
        'Try with different dates or destinations',
        'Contact support if the problem persists'
      ],
      isRetryable: true
    };
  }
  
  // API key errors
  if (errorString.includes('InvalidKey') || errorString.includes('API key')) {
    return {
      title: 'Configuration Error',
      message: 'There\'s a problem with the application configuration.',
      action: 'Contact Support',
      suggestions: [
        'This is a technical issue that needs to be resolved by our team',
        'Please contact support with details about what you were trying to do'
      ],
      isRetryable: false
    };
  }
  
  // Generic fallback
  return {
    title: 'Something Went Wrong',
    message: 'An unexpected error occurred. Please try again.',
    action: 'Try Again',
    suggestions: [
      'Refresh the page and try again',
      'Check your internet connection',
      'Contact support if the problem persists'
    ],
    isRetryable: true
  };
}

export function getErrorIcon(error: Error | string): string {
  const errorString = typeof error === 'string' ? error : error.message;
  
  if (errorString.includes('Failed to fetch') || errorString.includes('NetworkError')) {
    return 'wifi-off'; // Network icon
  }
  
  if (errorString.includes('401') || errorString.includes('403')) {
    return 'lock'; // Auth icon
  }
  
  if (errorString.includes('404')) {
    return 'search'; // Not found icon
  }
  
  if (errorString.includes('500') || errorString.includes('timeout')) {
    return 'server'; // Server icon
  }
  
  return 'alert-circle'; // Default error icon
}

export function getErrorColor(error: Error | string): string {
  const errorString = typeof error === 'string' ? error : error.message;
  
  if (errorString.includes('401') || errorString.includes('403')) {
    return 'text-yellow-600'; // Warning color for auth issues
  }
  
  if (errorString.includes('404')) {
    return 'text-blue-600'; // Info color for not found
  }
  
  return 'text-red-600'; // Error color for everything else
}
