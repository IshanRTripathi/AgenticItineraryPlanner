/**
 * Centralized Error Handler Service
 * Provides error classification, user-friendly messages, and recovery actions
 */

import { logger } from './logger';
import { getErrorInfo, ErrorInfo } from './errorMessages';

export enum ErrorType {
  NETWORK = 'NETWORK_ERROR',
  AUTH = 'AUTH_FAILED',
  VALIDATION = 'VALIDATION_ERROR',
  NOT_FOUND = 'NOT_FOUND',
  SERVER = 'SERVER_ERROR',
  TIMEOUT = 'TIMEOUT',
  CONFLICT = 'SYNC_CONFLICT',
  LOCKED = 'NODE_LOCKED',
  AGENT = 'AGENT_EXECUTION_FAILED',
  BOOKING = 'BOOKING_FAILED',
  UNKNOWN = 'UNKNOWN_ERROR'
}

export interface RecoveryAction {
  label: string;
  action: () => void | Promise<void>;
  primary?: boolean;
  variant?: 'default' | 'destructive' | 'outline' | 'secondary';
}

export interface ErrorContext {
  component?: string;
  action?: string;
  userId?: string;
  itineraryId?: string;
  [key: string]: any;
}

export class AppError extends Error {
  public readonly type: ErrorType;
  public readonly timestamp: Date;
  public readonly context?: ErrorContext;

  constructor(
    message: string,
    public code?: string,
    public statusCode?: number,
    public details?: any,
    context?: ErrorContext
  ) {
    super(message);
    this.name = 'AppError';
    this.type = ErrorHandler.classify({ message, code, statusCode });
    this.timestamp = new Date();
    this.context = context;
  }
}

class ErrorHandlerService {
  private errorCounts = new Map<string, number>();
  private maxRetries = 3;
  private retryDelay = 1000; // ms

  /**
   * Classify an error based on its properties
   */
  classify(error: Error | any): ErrorType {
    if (!error) return ErrorType.UNKNOWN;

    // Check error code first
    if (error.code) {
      const code = error.code.toUpperCase();
      if (code.includes('NETWORK') || code === 'ECONNREFUSED' || code === 'ENOTFOUND') {
        return ErrorType.NETWORK;
      }
      if (code.includes('AUTH') || code === 'UNAUTHORIZED') {
        return ErrorType.AUTH;
      }
      if (code.includes('VALIDATION')) {
        return ErrorType.VALIDATION;
      }
      if (code.includes('LOCKED')) {
        return ErrorType.LOCKED;
      }
      if (code.includes('CONFLICT')) {
        return ErrorType.CONFLICT;
      }
      if (code.includes('AGENT')) {
        return ErrorType.AGENT;
      }
      if (code.includes('BOOKING')) {
        return ErrorType.BOOKING;
      }
    }

    // Check HTTP status code
    if (error.statusCode || error.status) {
      const status = error.statusCode || error.status;
      if (status === 401 || status === 403) return ErrorType.AUTH;
      if (status === 404) return ErrorType.NOT_FOUND;
      if (status === 408 || status === 504) return ErrorType.TIMEOUT;
      if (status === 409) return ErrorType.CONFLICT;
      if (status === 422) return ErrorType.VALIDATION;
      if (status >= 500) return ErrorType.SERVER;
    }

    // Check error message
    const message = error.message?.toLowerCase() || '';
    if (message.includes('network') || message.includes('fetch')) {
      return ErrorType.NETWORK;
    }
    if (message.includes('timeout')) {
      return ErrorType.TIMEOUT;
    }
    if (message.includes('unauthorized') || message.includes('authentication')) {
      return ErrorType.AUTH;
    }
    if (message.includes('locked')) {
      return ErrorType.LOCKED;
    }

    return ErrorType.UNKNOWN;
  }

  /**
   * Get user-friendly error message
   */
  getUserMessage(error: Error | any): string {
    const type = this.classify(error);
    const errorInfo = getErrorInfo(type);
    
    // Use custom message if available, otherwise use default
    if (error.message && error.message !== 'Unknown error occurred') {
      return error.message;
    }
    
    return errorInfo.message;
  }

  /**
   * Get recovery actions for an error
   */
  getRecoveryActions(
    error: Error | any,
    context?: {
      onRetry?: () => void | Promise<void>;
      onGoBack?: () => void;
      onSignIn?: () => void;
      onRefresh?: () => void;
    }
  ): RecoveryAction[] {
    const type = this.classify(error);
    const actions: RecoveryAction[] = [];

    switch (type) {
      case ErrorType.NETWORK:
      case ErrorType.TIMEOUT:
        if (context?.onRetry) {
          actions.push({
            label: 'Try Again',
            action: context.onRetry,
            primary: true,
            variant: 'default'
          });
        }
        if (context?.onRefresh) {
          actions.push({
            label: 'Refresh Page',
            action: context.onRefresh,
            variant: 'outline'
          });
        }
        break;

      case ErrorType.AUTH:
        if (context?.onSignIn) {
          actions.push({
            label: 'Sign In',
            action: context.onSignIn,
            primary: true,
            variant: 'default'
          });
        }
        break;

      case ErrorType.CONFLICT:
        if (context?.onRefresh) {
          actions.push({
            label: 'Refresh to See Latest',
            action: context.onRefresh,
            primary: true,
            variant: 'default'
          });
        }
        break;

      case ErrorType.VALIDATION:
      case ErrorType.LOCKED:
        if (context?.onGoBack) {
          actions.push({
            label: 'Go Back',
            action: context.onGoBack,
            primary: true,
            variant: 'outline'
          });
        }
        break;

      default:
        if (context?.onRetry) {
          actions.push({
            label: 'Try Again',
            action: context.onRetry,
            primary: true,
            variant: 'default'
          });
        }
        if (context?.onGoBack) {
          actions.push({
            label: 'Go Back',
            action: context.onGoBack,
            variant: 'outline'
          });
        }
    }

    return actions;
  }

  /**
   * Check if error is retryable
   */
  isRetryable(error: Error | any): boolean {
    const type = this.classify(error);
    return [
      ErrorType.NETWORK,
      ErrorType.TIMEOUT,
      ErrorType.SERVER
    ].includes(type);
  }

  /**
   * Get retry count for an error
   */
  getRetryCount(errorKey: string): number {
    return this.errorCounts.get(errorKey) || 0;
  }

  /**
   * Increment retry count
   */
  incrementRetryCount(errorKey: string): number {
    const count = this.getRetryCount(errorKey) + 1;
    this.errorCounts.set(errorKey, count);
    return count;
  }

  /**
   * Reset retry count
   */
  resetRetryCount(errorKey: string): void {
    this.errorCounts.delete(errorKey);
  }

  /**
   * Check if should retry
   */
  shouldRetry(error: Error | any, errorKey: string): boolean {
    if (!this.isRetryable(error)) return false;
    return this.getRetryCount(errorKey) < this.maxRetries;
  }

  /**
   * Get retry delay with exponential backoff
   */
  getRetryDelay(retryCount: number): number {
    return this.retryDelay * Math.pow(2, retryCount);
  }

  /**
   * Handle and log error
   */
  handle(error: Error | any, context?: ErrorContext): AppError {
    const appError = this.toAppError(error, context);
    
    // Log error with context
    logger.error('Error handled', {
      component: context?.component || 'ErrorHandler',
      action: context?.action || 'handle_error',
      errorType: appError.type,
      errorCode: appError.code,
      statusCode: appError.statusCode,
      ...context
    }, appError);

    return appError;
  }

  /**
   * Convert any error to AppError
   */
  toAppError(error: any, context?: ErrorContext): AppError {
    if (error instanceof AppError) return error;

    if (error.response) {
      return new AppError(
        error.response.data?.message || 'API request failed',
        error.response.data?.code,
        error.response.status,
        error.response.data,
        context
      );
    }

    if (error.request) {
      return new AppError(
        'No response from server',
        ErrorType.NETWORK,
        undefined,
        undefined,
        context
      );
    }

    if (error instanceof Error) {
      return new AppError(
        error.message,
        undefined,
        undefined,
        undefined,
        context
      );
    }

    return new AppError(
      typeof error === 'string' ? error : 'Unknown error occurred',
      ErrorType.UNKNOWN,
      undefined,
      undefined,
      context
    );
  }
}

// Export singleton instance
export const ErrorHandler = new ErrorHandlerService();

// Legacy exports for backward compatibility
export function handleApiError(error: any): AppError {
  return ErrorHandler.toAppError(error);
}

export function getErrorMessage(error: any): string {
  return ErrorHandler.getUserMessage(error);
}
