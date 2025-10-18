/**
 * Centralized logging utility for the unified itinerary system
 * Provides structured logging with different levels and contexts
 */

export enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3
}

export interface LogContext {
  component?: string;
  action?: string;
  userId?: string;
  itineraryId?: string;
  sessionId?: string;
  timestamp?: number;
  [key: string]: any;
}

class Logger {
  private logLevel: LogLevel;
  private sessionId: string;

  constructor() {
    this.logLevel = import.meta.env.DEV ? LogLevel.DEBUG : LogLevel.INFO;
    this.sessionId = this.generateSessionId();
  }

  private generateSessionId(): string {
    return `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  private formatMessage(level: string, message: string, context?: LogContext, data?: any): void {
    const timestamp = new Date().toISOString();
    const logContext: LogContext = {
      sessionId: this.sessionId,
      timestamp: Date.now(),
      ...context
    };

    const logEntry = {
      level,
      timestamp,
      message,
      context: logContext,
      ...(data && { data })
    };

    // Console output with color coding
    const colors = {
      DEBUG: '#6B7280', // Gray
      INFO: '#3B82F6',  // Blue
      WARN: '#F59E0B',  // Yellow
      ERROR: '#EF4444'  // Red
    };

    const color = colors[level as keyof typeof colors] || colors.INFO;
    
    console.groupCollapsed(
      `%c[${level}] ${context?.component || 'APP'} - ${message}`,
      `color: ${color}; font-weight: bold;`
    );
    
    if (context && Object.keys(context).length > 0) {
      console.log('Context:', logContext);
    }
    
    if (data) {
      console.log('Data:', data);
    }
    
    console.trace('Stack trace');
    console.groupEnd();

    // Send to external logging service in production
    if (import.meta.env.PROD && level !== 'DEBUG') {
      this.sendToExternalLogger(logEntry);
    }
  }

  private sendToExternalLogger(logEntry: any): void {
    // TODO: Implement external logging service integration
    // Examples: Sentry, LogRocket, DataDog, etc.
    try {
      // For now, just store in localStorage for debugging
      const logs = JSON.parse(localStorage.getItem('app_logs') || '[]');
      logs.push(logEntry);
      
      // Keep only last 100 logs to prevent storage overflow
      if (logs.length > 100) {
        logs.splice(0, logs.length - 100);
      }
      
      localStorage.setItem('app_logs', JSON.stringify(logs));
    } catch (error) {
      console.error('Failed to store log entry:', error);
    }
  }

  debug(message: string, context?: LogContext, data?: any): void {
    if (this.logLevel <= LogLevel.DEBUG) {
      this.formatMessage('DEBUG', message, context, data);
    }
  }

  info(message: string, context?: LogContext, data?: any): void {
    if (this.logLevel <= LogLevel.INFO) {
      this.formatMessage('INFO', message, context, data);
    }
  }

  warn(message: string, context?: LogContext, data?: any): void {
    if (this.logLevel <= LogLevel.WARN) {
      this.formatMessage('WARN', message, context, data);
    }
  }

  error(message: string, context?: LogContext, data?: any): void {
    if (this.logLevel <= LogLevel.ERROR) {
      this.formatMessage('ERROR', message, context, data);
    }
  }

  // Performance logging
  startTimer(label: string, context?: LogContext): () => void {
    const startTime = performance.now();
    this.debug(`Timer started: ${label}`, { ...context, action: 'timer_start' });
    
    return () => {
      const endTime = performance.now();
      const duration = endTime - startTime;
      this.info(`Timer completed: ${label} (${duration.toFixed(2)}ms)`, {
        ...context,
        action: 'timer_end',
        duration
      });
    };
  }

  // API call logging
  logApiCall(method: string, url: string, context?: LogContext): {
    success: (response: any, duration: number) => void;
    error: (error: any, duration: number) => void;
  } {
    const startTime = performance.now();
    this.info(`API call started: ${method} ${url}`, {
      ...context,
      action: 'api_call_start',
      method,
      url
    });

    return {
      success: (response: any, duration: number) => {
        this.info(`API call succeeded: ${method} ${url}`, {
          ...context,
          action: 'api_call_success',
          method,
          url,
          duration,
          status: response?.status
        }, { responseSize: JSON.stringify(response).length });
      },
      error: (error: any, duration: number) => {
        this.error(`API call failed: ${method} ${url}`, {
          ...context,
          action: 'api_call_error',
          method,
          url,
          duration,
          errorType: error?.constructor?.name,
          errorMessage: error?.message
        }, error);
      }
    };
  }

  // User interaction logging
  logUserAction(action: string, context?: LogContext, data?: any): void {
    this.info(`User action: ${action}`, {
      ...context,
      action: 'user_interaction',
      userAction: action
    }, data);
  }

  // State change logging
  logStateChange(component: string, oldState: any, newState: any, context?: LogContext): void {
    this.debug(`State change in ${component}`, {
      ...context,
      component,
      action: 'state_change'
    }, {
      oldState,
      newState,
      diff: this.getStateDiff(oldState, newState)
    });
  }

  private getStateDiff(oldState: any, newState: any): any {
    if (typeof oldState !== 'object' || typeof newState !== 'object') {
      return { old: oldState, new: newState };
    }

    const diff: any = {};
    const allKeys = new Set([...Object.keys(oldState || {}), ...Object.keys(newState || {})]);
    
    for (const key of allKeys) {
      if (oldState?.[key] !== newState?.[key]) {
        diff[key] = { old: oldState?.[key], new: newState?.[key] };
      }
    }
    
    return diff;
  }

  // Error boundary logging
  logError(error: Error, errorInfo?: any, context?: LogContext): void {
    this.error(`Unhandled error: ${error.message}`, {
      ...context,
      action: 'error_boundary',
      errorName: error.name,
      errorStack: error.stack
    }, { error, errorInfo });
  }

  // WebSocket logging
  logWebSocketEvent(event: string, context?: LogContext, data?: any): void {
    this.info(`WebSocket event: ${event}`, {
      ...context,
      action: 'websocket_event',
      wsEvent: event
    }, data);
  }

  // Component lifecycle logging
  logComponentLifecycle(component: string, lifecycle: string, context?: LogContext, data?: any): void {
    this.debug(`Component lifecycle: ${component} - ${lifecycle}`, {
      ...context,
      component,
      action: 'component_lifecycle',
      lifecycle
    }, data);
  }
}

export const logger = new Logger();

// Export convenience functions
export const logDebug = logger.debug.bind(logger);
export const logInfo = logger.info.bind(logger);
export const logWarn = logger.warn.bind(logger);
export const logError = logger.error.bind(logger);
export const logUserAction = logger.logUserAction.bind(logger);
export const logStateChange = logger.logStateChange.bind(logger);
export const logApiCall = logger.logApiCall.bind(logger);
export const startTimer = logger.startTimer.bind(logger);
export const logWebSocketEvent = logger.logWebSocketEvent.bind(logger);
export const logComponentLifecycle = logger.logComponentLifecycle.bind(logger);