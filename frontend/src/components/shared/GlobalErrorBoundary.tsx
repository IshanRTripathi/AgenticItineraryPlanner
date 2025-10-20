import React, { Component, ErrorInfo, ReactNode } from 'react';
import { ErrorDisplay } from './ErrorDisplay';
import { logger } from '../../utils/logger';
import { ErrorHandler } from '../../utils/errorHandler';

interface Props {
  children: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface State {
  hasError: boolean;
  error?: Error;
  errorInfo?: ErrorInfo;
}

export class GlobalErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    // Handle and log error using centralized error handler
    const appError = ErrorHandler.handle(error, {
      component: 'GlobalErrorBoundary',
      action: 'component_error',
      componentStack: errorInfo.componentStack
    });
    
    // Log additional error info
    logger.error('React component error caught', {
      component: 'GlobalErrorBoundary',
      action: 'component_did_catch',
      errorType: appError.type,
      componentStack: errorInfo.componentStack?.split('\n').slice(0, 5).join('\n') // First 5 lines
    }, error);
    
    // Call custom error handler if provided
    if (this.props.onError) {
      this.props.onError(error, errorInfo);
    }
    
    this.setState({ errorInfo });
  }

  handleRetry = () => {
    logger.info('User retrying after error', {
      component: 'GlobalErrorBoundary',
      action: 'retry'
    });
    this.setState({ hasError: false, error: undefined, errorInfo: undefined });
  };

  handleReload = () => {
    logger.info('User reloading page after error', {
      component: 'GlobalErrorBoundary',
      action: 'reload'
    });
    window.location.reload();
  };

  render(): ReactNode {
    if (this.state.hasError) {
      return (
        <ErrorDisplay
          error={this.state.error || new Error('An unexpected application error occurred')}
          onRetry={this.handleRetry}
          onGoBack={() => window.location.href = '/'}
          className="min-h-screen"
        />
      );
    }

    return this.props.children;
  }
}

// Higher-order component for wrapping components with global error boundary
export function withGlobalErrorBoundary<P extends object>(
  Component: React.ComponentType<P>,
  errorBoundaryProps?: Omit<Props, 'children'>
) {
  const WrappedComponent = (props: P) => (
    <GlobalErrorBoundary {...errorBoundaryProps}>
      <Component {...props} />
    </GlobalErrorBoundary>
  );

  WrappedComponent.displayName = `withGlobalErrorBoundary(${Component.displayName || Component.name})`;
  
  return WrappedComponent;
}
