/**
 * Example usage of UndoRedoControls component
 */

import React from 'react';
import { UndoRedoControls } from './UndoRedoControls';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../ui/card';

export function UndoRedoControlsExample() {
  return (
    <Card className="w-full max-w-2xl">
      <CardHeader>
        <CardTitle>Undo/Redo Controls Example</CardTitle>
        <CardDescription>
          Test the undo/redo functionality with keyboard shortcuts
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Basic Usage */}
        <div>
          <h3 className="text-sm font-medium mb-2">Basic Controls</h3>
          <UndoRedoControls />
        </div>

        {/* With Version Info */}
        <div>
          <h3 className="text-sm font-medium mb-2">With Version Info</h3>
          <UndoRedoControls showVersionInfo={true} />
        </div>

        {/* With Stack Depth */}
        <div>
          <h3 className="text-sm font-medium mb-2">With Stack Depth Info</h3>
          <UndoRedoControls showVersionInfo={true} showStackDepth={true} />
        </div>

        {/* With Callbacks */}
        <div>
          <h3 className="text-sm font-medium mb-2">With Event Callbacks</h3>
          <UndoRedoControls
            showVersionInfo={true}
            showStackDepth={true}
            onUndoSuccess={() => console.log('Undo successful!')}
            onRedoSuccess={() => console.log('Redo successful!')}
            onError={(error) => console.error('Operation failed:', error)}
          />
        </div>

        {/* Keyboard Shortcuts Info */}
        <div className="bg-gray-50 p-4 rounded-lg">
          <h3 className="text-sm font-medium mb-2">Keyboard Shortcuts</h3>
          <ul className="text-sm space-y-1 text-gray-600">
            <li><kbd className="px-2 py-1 bg-white border rounded">Ctrl+Z</kbd> or <kbd className="px-2 py-1 bg-white border rounded">Cmd+Z</kbd> - Undo</li>
            <li><kbd className="px-2 py-1 bg-white border rounded">Ctrl+Y</kbd> or <kbd className="px-2 py-1 bg-white border rounded">Cmd+Shift+Z</kbd> - Redo</li>
          </ul>
        </div>
      </CardContent>
    </Card>
  );
}
