import React, { useEffect, useState } from 'react';
import { X, Undo2, CheckCircle, AlertCircle, Info } from 'lucide-react';
import { Button } from '../ui/button';

interface ChangeNotificationProps {
  message: string;
  type?: 'success' | 'error' | 'info' | 'warning';
  showUndo?: boolean;
  onUndo?: () => void;
  onClose?: () => void;
  autoClose?: boolean;
  duration?: number;
  userName?: string;
}

export function ChangeNotification({
  message,
  type = 'info',
  showUndo = false,
  onUndo,
  onClose,
  autoClose = true,
  duration = 5000,
  userName,
}: ChangeNotificationProps) {
  const [isVisible, setIsVisible] = useState(true);

  useEffect(() => {
    if (autoClose) {
      const timer = setTimeout(() => {
        setIsVisible(false);
        onClose?.();
      }, duration);
      return () => clearTimeout(timer);
    }
  }, [autoClose, duration, onClose]);

  if (!isVisible) return null;

  const getIcon = () => {
    switch (type) {
      case 'success': return <CheckCircle className="w-5 h-5 text-green-600" />;
      case 'error': return <AlertCircle className="w-5 h-5 text-red-600" />;
      case 'warning': return <AlertCircle className="w-5 h-5 text-yellow-600" />;
      default: return <Info className="w-5 h-5 text-blue-600" />;
    }
  };

  const getBgColor = () => {
    switch (type) {
      case 'success': return 'bg-green-50 border-green-200';
      case 'error': return 'bg-red-50 border-red-200';
      case 'warning': return 'bg-yellow-50 border-yellow-200';
      default: return 'bg-blue-50 border-blue-200';
    }
  };

  return (
    <div className={`fixed bottom-4 right-4 z-50 max-w-md animate-in slide-in-from-bottom-5`}>
      <div className={`${getBgColor()} border rounded-lg shadow-lg p-4`}>
        <div className="flex items-start gap-3">
          {getIcon()}
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-gray-900">{message}</p>
            {userName && (
              <p className="text-xs text-gray-600 mt-1">by {userName}</p>
            )}
          </div>
          <div className="flex items-center gap-2">
            {showUndo && onUndo && (
              <Button
                variant="ghost"
                size="sm"
                onClick={onUndo}
                className="h-8 px-2"
              >
                <Undo2 className="w-4 h-4 mr-1" />
                Undo
              </Button>
            )}
            <button
              onClick={() => {
                setIsVisible(false);
                onClose?.();
              }}
              className="text-gray-400 hover:text-gray-600"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
