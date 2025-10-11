/**
 * Keyboard Shortcuts Help Modal
 * Displays all available keyboard shortcuts
 */

import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { X, Keyboard } from 'lucide-react';

interface Shortcut {
  keys: string[];
  description: string;
  category: string;
}

const SHORTCUTS: Shortcut[] = [
  // Navigation
  { keys: ['Ctrl', 'H'], description: 'View history', category: 'Navigation' },
  { keys: ['Ctrl', 'K'], description: 'Open command palette', category: 'Navigation' },
  { keys: ['Esc'], description: 'Close modal/dialog', category: 'Navigation' },
  
  // Editing
  { keys: ['Ctrl', 'Z'], description: 'Undo', category: 'Editing' },
  { keys: ['Ctrl', 'Y'], description: 'Redo', category: 'Editing' },
  { keys: ['Ctrl', 'S'], description: 'Save changes', category: 'Editing' },
  { keys: ['Enter'], description: 'Apply changes', category: 'Editing' },
  
  // Preview
  { keys: ['Ctrl', 'P'], description: 'Toggle preview mode', category: 'Preview' },
  { keys: ['Enter'], description: 'Approve changes', category: 'Preview' },
  { keys: ['Esc'], description: 'Cancel changes', category: 'Preview' },
  
  // Search
  { keys: ['Ctrl', 'F'], description: 'Search', category: 'Search' },
  { keys: ['/'], description: 'Focus search', category: 'Search' },
  
  // Help
  { keys: ['?'], description: 'Show keyboard shortcuts', category: 'Help' },
  { keys: ['Ctrl', '?'], description: 'Show help', category: 'Help' },
];

interface KeyboardShortcutsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function KeyboardShortcutsModal({ isOpen, onClose }: KeyboardShortcutsModalProps) {
  if (!isOpen) return null;

  const categories = Array.from(new Set(SHORTCUTS.map(s => s.category)));

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <Card className="w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Keyboard className="w-5 h-5" />
              <CardTitle>Keyboard Shortcuts</CardTitle>
            </div>
            <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-full">
              <X className="w-5 h-5" />
            </button>
          </div>
        </CardHeader>

        <CardContent className="space-y-6">
          {categories.map(category => (
            <div key={category}>
              <h3 className="text-sm font-semibold text-gray-700 mb-3">{category}</h3>
              <div className="space-y-2">
                {SHORTCUTS.filter(s => s.category === category).map((shortcut, index) => (
                  <div
                    key={index}
                    className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                  >
                    <span className="text-sm text-gray-700">{shortcut.description}</span>
                    <div className="flex gap-1">
                      {shortcut.keys.map((key, keyIndex) => (
                        <React.Fragment key={keyIndex}>
                          <Badge variant="outline" className="font-mono text-xs px-2 py-1">
                            {key}
                          </Badge>
                          {keyIndex < shortcut.keys.length - 1 && (
                            <span className="text-gray-400 mx-1">+</span>
                          )}
                        </React.Fragment>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}

          <div className="pt-4 border-t">
            <p className="text-xs text-gray-500 text-center">
              Press <Badge variant="outline" className="font-mono text-xs">?</Badge> anytime to show this dialog
            </p>
          </div>

          <Button onClick={onClose} className="w-full">
            Got it
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
