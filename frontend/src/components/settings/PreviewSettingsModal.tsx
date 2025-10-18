/**
 * Preview Settings Modal
 * Allows users to configure change preview preferences
 */

import React from 'react';
import { usePreviewSettings } from '../../contexts/PreviewSettingsContext';
import { Button } from '../ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../ui/card';
import { Switch } from '../ui/switch';
import { Label } from '../ui/label';
import { RadioGroup, RadioGroupItem } from '../ui/radio-group';
import { X, Settings, RotateCcw } from 'lucide-react';

interface PreviewSettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function PreviewSettingsModal({ isOpen, onClose }: PreviewSettingsModalProps) {
  const { settings, updateSettings, resetSettings } = usePreviewSettings();

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <Card className="w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Settings className="w-5 h-5" />
              <CardTitle>Change Preview Settings</CardTitle>
            </div>
            <button
              onClick={onClose}
              className="p-1 hover:bg-gray-100 rounded-full transition-colors"
              aria-label="Close"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
          <CardDescription>
            Customize how change previews are displayed throughout the application
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-6">
          {/* Advanced Diff Viewer */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label htmlFor="advanced-diff" className="text-base font-medium">
                  Use Advanced Diff Viewer
                </Label>
                <p className="text-sm text-gray-500">
                  Show detailed side-by-side comparison with search and filtering
                </p>
              </div>
              <Switch
                id="advanced-diff"
                checked={settings.useAdvancedDiff}
                onCheckedChange={(checked) => updateSettings({ useAdvancedDiff: checked })}
              />
            </div>
          </div>

          {/* View Mode */}
          {settings.useAdvancedDiff && (
            <div className="space-y-3">
              <Label className="text-base font-medium">Default View Mode</Label>
              <RadioGroup
                value={settings.defaultViewMode}
                onValueChange={(value: 'side-by-side' | 'unified') =>
                  updateSettings({ defaultViewMode: value })
                }
              >
                <div className="flex items-center space-x-2">
                  <RadioGroupItem value="side-by-side" id="side-by-side" />
                  <Label htmlFor="side-by-side" className="font-normal cursor-pointer">
                    <div>
                      <div className="font-medium">Side-by-Side</div>
                      <div className="text-sm text-gray-500">
                        Show before and after values in columns
                      </div>
                    </div>
                  </Label>
                </div>
                <div className="flex items-center space-x-2">
                  <RadioGroupItem value="unified" id="unified" />
                  <Label htmlFor="unified" className="font-normal cursor-pointer">
                    <div>
                      <div className="font-medium">Unified</div>
                      <div className="text-sm text-gray-500">
                        Show changes inline with +/- markers
                      </div>
                    </div>
                  </Label>
                </div>
              </RadioGroup>
            </div>
          )}

          {/* Show Unchanged */}
          {settings.useAdvancedDiff && (
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label htmlFor="show-unchanged" className="text-base font-medium">
                    Show Unchanged Fields
                  </Label>
                  <p className="text-sm text-gray-500">
                    Display fields that haven't changed for context
                  </p>
                </div>
                <Switch
                  id="show-unchanged"
                  checked={settings.showUnchanged}
                  onCheckedChange={(checked) => updateSettings({ showUnchanged: checked })}
                />
              </div>
            </div>
          )}

          {/* Cache Preferences */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label htmlFor="cache-prefs" className="text-base font-medium">
                  Remember My Preferences
                </Label>
                <p className="text-sm text-gray-500">
                  Save settings to browser storage
                </p>
              </div>
              <Switch
                id="cache-prefs"
                checked={settings.cachePreferences}
                onCheckedChange={(checked) => updateSettings({ cachePreferences: checked })}
              />
            </div>
          </div>

          {/* Preview Example */}
          <div className="border-t pt-4">
            <h4 className="text-sm font-medium mb-2">Preview</h4>
            <div className="bg-gray-50 p-4 rounded-lg text-sm">
              {settings.useAdvancedDiff ? (
                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    <span className="text-green-600">✓</span>
                    <span>Advanced diff viewer with search and filtering</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-green-600">✓</span>
                    <span>
                      {settings.defaultViewMode === 'side-by-side'
                        ? 'Side-by-side comparison'
                        : 'Unified inline view'}
                    </span>
                  </div>
                  {settings.showUnchanged && (
                    <div className="flex items-center gap-2">
                      <span className="text-green-600">✓</span>
                      <span>Unchanged fields visible</span>
                    </div>
                  )}
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <span className="text-blue-600">ℹ</span>
                  <span>Simple change preview with basic diff display</span>
                </div>
              )}
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-between pt-4 border-t">
            <Button
              variant="outline"
              onClick={resetSettings}
              className="flex items-center gap-2"
            >
              <RotateCcw className="w-4 h-4" />
              Reset to Defaults
            </Button>
            <Button onClick={onClose}>Done</Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
