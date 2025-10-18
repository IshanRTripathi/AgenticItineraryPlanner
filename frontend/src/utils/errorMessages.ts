/**
 * Error Message Utilities
 * Provides user-friendly error messages with suggestions
 */

export interface ErrorInfo {
  code: string;
  message: string;
  suggestions: string[];
  docsLink?: string;
}

const ERROR_MESSAGES: Record<string, ErrorInfo> = {
  // Network Errors
  NETWORK_ERROR: {
    code: 'NETWORK_ERROR',
    message: 'Unable to connect to the server. Please check your internet connection.',
    suggestions: [
      'Check your internet connection',
      'Try refreshing the page',
      'Contact support if the problem persists',
    ],
    docsLink: '/docs/troubleshooting/network-errors',
  },
  
  // Authentication Errors
  AUTH_FAILED: {
    code: 'AUTH_FAILED',
    message: 'Authentication failed. Please sign in again.',
    suggestions: [
      'Sign out and sign in again',
      'Clear your browser cache',
      'Check if your session has expired',
    ],
    docsLink: '/docs/troubleshooting/auth-errors',
  },
  
  // Validation Errors
  VALIDATION_ERROR: {
    code: 'VALIDATION_ERROR',
    message: 'The data you entered is invalid.',
    suggestions: [
      'Check all required fields are filled',
      'Ensure dates are in the correct format',
      'Verify all values are within acceptable ranges',
    ],
    docsLink: '/docs/troubleshooting/validation-errors',
  },
  
  // Lock Errors
  NODE_LOCKED: {
    code: 'NODE_LOCKED',
    message: 'This item is locked and cannot be modified.',
    suggestions: [
      'Unlock the item first',
      'Ask the owner to unlock it',
      'Create a copy if you need to make changes',
    ],
    docsLink: '/docs/features/node-locking',
  },
  
  // Sync Errors
  SYNC_CONFLICT: {
    code: 'SYNC_CONFLICT',
    message: 'Changes conflict with recent updates.',
    suggestions: [
      'Refresh to see the latest changes',
      'Resolve conflicts manually',
      'Discard your changes and start over',
    ],
    docsLink: '/docs/features/sync-conflicts',
  },
  
  // Agent Errors
  AGENT_EXECUTION_FAILED: {
    code: 'AGENT_EXECUTION_FAILED',
    message: 'The AI agent failed to complete the task.',
    suggestions: [
      'Try again with different parameters',
      'Check if any nodes are locked',
      'Simplify your request',
    ],
    docsLink: '/docs/features/ai-agents',
  },
  
  // Booking Errors
  BOOKING_FAILED: {
    code: 'BOOKING_FAILED',
    message: 'Unable to complete the booking.',
    suggestions: [
      'Check availability for the selected date',
      'Verify payment information',
      'Try an alternative option',
    ],
    docsLink: '/docs/features/bookings',
  },
  
  // Generic Errors
  UNKNOWN_ERROR: {
    code: 'UNKNOWN_ERROR',
    message: 'An unexpected error occurred.',
    suggestions: [
      'Try refreshing the page',
      'Clear your browser cache',
      'Contact support with error details',
    ],
    docsLink: '/docs/troubleshooting',
  },
};

export function getErrorInfo(errorCode: string): ErrorInfo {
  return ERROR_MESSAGES[errorCode] || ERROR_MESSAGES.UNKNOWN_ERROR;
}

export function formatErrorMessage(error: Error | string, code?: string): ErrorInfo {
  const errorCode = code || 'UNKNOWN_ERROR';
  const baseInfo = getErrorInfo(errorCode);
  
  const message = typeof error === 'string' ? error : error.message;
  
  return {
    ...baseInfo,
    message: message || baseInfo.message,
  };
}

export function getErrorSuggestions(errorCode: string): string[] {
  const info = getErrorInfo(errorCode);
  return info.suggestions;
}

export function getErrorDocsLink(errorCode: string): string | undefined {
  const info = getErrorInfo(errorCode);
  return info.docsLink;
}
