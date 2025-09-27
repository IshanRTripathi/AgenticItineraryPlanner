import React from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '../ui/dialog';
import { Button } from '../ui/button';
import { HelpCircle, Keyboard } from 'lucide-react';
import { useKeyboardShortcuts } from './KeyboardShortcuts';

interface KeyboardShortcutsHelpProps {
  trigger?: React.ReactNode;
}

export function KeyboardShortcutsHelp({ trigger }: KeyboardShortcutsHelpProps) {
  const shortcuts = useKeyboardShortcuts();

  const defaultTrigger = (
    <Button variant="ghost" size="sm">
      <HelpCircle className="h-4 w-4 mr-2" />
      Shortcuts
    </Button>
  );

  return (
    <Dialog>
      <DialogTrigger asChild>
        {trigger || defaultTrigger}
      </DialogTrigger>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center space-x-2">
            <Keyboard className="h-5 w-5" />
            <span>Keyboard Shortcuts</span>
          </DialogTitle>
        </DialogHeader>
        
        <div className="space-y-4">
          <p className="text-sm text-gray-600">
            Use these keyboard shortcuts to navigate quickly:
          </p>
          
          <div className="space-y-3">
            {shortcuts.map((shortcut, index) => (
              <div key={index} className="flex items-center justify-between">
                <span className="text-sm text-gray-700">
                  {shortcut.description}
                </span>
                <kbd className="px-2 py-1 text-xs font-mono bg-gray-100 border border-gray-300 rounded">
                  {shortcut.key}
                </kbd>
              </div>
            ))}
          </div>
          
          <div className="pt-4 border-t border-gray-200">
            <p className="text-xs text-gray-500">
              Shortcuts work when you're not typing in input fields.
            </p>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
