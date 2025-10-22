import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

interface KeyboardShortcutsProps {
  children: React.ReactNode;
}

export function KeyboardShortcuts({ children }: KeyboardShortcutsProps) {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // Only handle shortcuts when not typing in input fields
      if (
        event.target instanceof HTMLInputElement ||
        event.target instanceof HTMLTextAreaElement ||
        event.target instanceof HTMLSelectElement ||
        (event.target as HTMLElement)?.contentEditable === 'true'
      ) {
        return;
      }

      // Check for Ctrl/Cmd + key combinations
      const isModifierPressed = event.ctrlKey || event.metaKey;
      
      if (isModifierPressed) {
        switch (event.key.toLowerCase()) {
          case 'h':
            event.preventDefault();
            navigate('/');
            break;
          case 'd':
            if (isAuthenticated) {
              event.preventDefault();
              navigate('/dashboard');
            }
            break;
          case 'n':
            if (isAuthenticated) {
              event.preventDefault();
              navigate('/wizard');
            }
            break;
          case 'k':
            event.preventDefault();
            // Focus search or open command palette (future feature)
            
            break;
        }
      }

      // Handle Escape key for common actions
      if (event.key === 'Escape') {
        // Close any open modals or dropdowns
        const activeElement = document.activeElement as HTMLElement;
        if (activeElement && activeElement.blur) {
          activeElement.blur();
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [navigate, isAuthenticated]);

  return <>{children}</>;
}

// Hook for displaying keyboard shortcuts help
export function useKeyboardShortcuts() {
  const shortcuts = [
    { key: 'Ctrl/Cmd + H', description: 'Go to Home' },
    { key: 'Ctrl/Cmd + D', description: 'Go to Dashboard (My Trips)' },
    { key: 'Ctrl/Cmd + N', description: 'Create New Trip' },
    { key: 'Ctrl/Cmd + K', description: 'Open Command Palette' },
    { key: 'Escape', description: 'Close modals/dropdowns' }
  ];

  return shortcuts;
}

