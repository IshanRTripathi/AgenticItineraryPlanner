/**
 * Agent Error Display
 * Shows detailed error information with retry options
 */

import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { AlertTriangle, RefreshCw, Copy, ChevronDown, ChevronUp } from 'lucide-react';

interface AgentError {
  agentId: string;
  agentName: string;
  errorCode: string;
  message: string;
  context?: Record<string, any>;
  suggestions?: string[];
  timestamp: Date;
  stackTrace?: string;
}

interface AgentErrorDisplayProps {
  error: AgentError;
  onRetry?: () => void;
  onDismiss?: () => void;
  className?: string;
}

export function AgentErrorDisplay({
  error,
  onRetry,
  onDismiss,
  className = '',
}: AgentErrorDisplayProps) {
  const [showDetails, setShowDetails] = useState(false);
  const [retryCount, setRetryCount] = useState(0);
  const [isRetrying, setIsRetrying] = useState(false);

  const handleRetry = async () => {
    setIsRetrying(true);
    setRetryCount(prev => prev + 1);
    
    try {
      await onRetry?.();
    } finally {
      setIsRetrying(false);
    }
  };

  const copyError = () => {
    const errorText = `
Agent: ${error.agentName}
Error Code: ${error.errorCode}
Message: ${error.message}
Timestamp: ${error.timestamp.toISOString()}
${error.stackTrace ? `\nStack Trace:\n${error.stackTrace}` : ''}
${error.context ? `\nContext:\n${JSON.stringify(error.context, null, 2)}` : ''}
    `.trim();

    navigator.clipboard.writeText(errorText);
    alert('Error details copied to clipboard');
  };

  const getRetryDelay = () => {
    // Exponential backoff: 1s, 2s, 4s, 8s, max 30s
    return Math.min(Math.pow(2, retryCount) * 1000, 30000);
  };

  return (
    <Card className={`${className} border-red-200 border-2`}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <AlertTriangle className="w-5 h-5 text-red-500" />
            <div>
              <CardTitle className="text-red-700">Agent Execution Failed</CardTitle>
              <p className="text-sm text-gray-500 mt-1">{error.agentName}</p>
            </div>
          </div>
          <Badge className="bg-red-100 text-red-700">{error.errorCode}</Badge>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {/* Error Message */}
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
          <p className="text-sm text-red-800">{error.message}</p>
          <p className="text-xs text-gray-500 mt-2">
            Occurred at {error.timestamp.toLocaleString()}
          </p>
        </div>

        {/* Suggestions */}
        {error.suggestions && error.suggestions.length > 0 && (
          <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <h4 className="font-semibold text-sm mb-2 text-blue-900">Suggestions:</h4>
            <ul className="list-disc list-inside space-y-1">
              {error.suggestions.map((suggestion, index) => (
                <li key={index} className="text-sm text-blue-800">
                  {suggestion}
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* Details Toggle */}
        {(error.context || error.stackTrace) && (
          <div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setShowDetails(!showDetails)}
              className="w-full justify-between"
            >
              <span>Technical Details</span>
              {showDetails ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
            </Button>

            {showDetails && (
              <div className="mt-2 p-4 bg-gray-50 rounded-lg space-y-3">
                {error.context && (
                  <div>
                    <h5 className="text-xs font-semibold text-gray-700 mb-1">Context:</h5>
                    <pre className="text-xs font-mono text-gray-600 overflow-x-auto">
                      {JSON.stringify(error.context, null, 2)}
                    </pre>
                  </div>
                )}

                {error.stackTrace && (
                  <div>
                    <h5 className="text-xs font-semibold text-gray-700 mb-1">Stack Trace:</h5>
                    <pre className="text-xs font-mono text-gray-600 overflow-x-auto whitespace-pre-wrap">
                      {error.stackTrace}
                    </pre>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* Retry Info */}
        {retryCount > 0 && (
          <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg">
            <p className="text-sm text-amber-800">
              Retry attempt {retryCount}. Next retry in {getRetryDelay() / 1000}s
            </p>
          </div>
        )}

        {/* Actions */}
        <div className="flex justify-between gap-2 pt-4 border-t">
          <Button variant="outline" size="sm" onClick={copyError}>
            <Copy className="w-4 h-4 mr-2" />
            Copy Error
          </Button>

          <div className="flex gap-2">
            {onDismiss && (
              <Button variant="outline" onClick={onDismiss}>
                Dismiss
              </Button>
            )}
            {onRetry && (
              <Button
                onClick={handleRetry}
                disabled={isRetrying}
                className="bg-blue-600 hover:bg-blue-700"
              >
                <RefreshCw className={`w-4 h-4 mr-2 ${isRetrying ? 'animate-spin' : ''}`} />
                {isRetrying ? 'Retrying...' : 'Retry'}
              </Button>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
