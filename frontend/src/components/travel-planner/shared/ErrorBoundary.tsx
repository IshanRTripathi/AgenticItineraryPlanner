import React, { Component, ErrorInfo, ReactNode } from 'react';
import { Button } from '../../ui/button';
import { Card } from '../../ui/card';
import { AlertTriangle, RefreshCw } from 'lucide-react';
import { ErrorDisplay } from '../../shared/ErrorDisplay';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface State {
  hasError: boolean;
  error?: Error;
}

class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    
    if (this.props.onError) {
      this.props.onError(error, errorInfo);
    }
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: undefined });
  };

  render(): ReactNode {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      // Use the new ErrorDisplay component for better UX
      return (
        <ErrorDisplay
          error={this.state.error || new Error('An unexpected error occurred')}
          onRetry={this.handleRetry}
          onGoBack={() => window.history.back()}
          className="min-h-[400px]"
        />
      );
    }

    return this.props.children;
  }
}

export { ErrorBoundary };

// Higher-order component for easier usage
export function withErrorBoundary<P extends object>(
  Component: React.ComponentType<P>,
  errorBoundaryProps?: Omit<Props, 'children'>
) {
  const WrappedComponent = (props: P) => (
    <ErrorBoundary {...errorBoundaryProps}>
      <Component {...props} />
    </ErrorBoundary>
  );

  WrappedComponent.displayName = `withErrorBoundary(${Component.displayName || Component.name})`;
  
  return WrappedComponent;
}
