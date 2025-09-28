import React from 'react';
import { Button } from '../ui/button';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { Separator } from '../ui/separator';
import { Alert, AlertDescription } from '../ui/alert';
import { Trash2, AlertTriangle } from 'lucide-react';
import { WorkflowNodeData } from '../WorkflowBuilder';

interface NodeInspectorModalProps {
  isOpen: boolean;
  onClose: () => void;
  selectedNode: { id: string; data: WorkflowNodeData } | null;
  onUpdateNode: (updates: Partial<WorkflowNodeData>) => void;
  onDeleteNode: (nodeId: string) => void;
}

export function NodeInspectorModal({
  isOpen,
  onClose,
  selectedNode,
  onUpdateNode,
  onDeleteNode,
}: NodeInspectorModalProps) {
  if (!selectedNode) return null;

  const handleDelete = () => {
    onDeleteNode(selectedNode.id);
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="w-screen max-h-[80vh] my-[10vh] rounded-none sm:w-[95vw] sm:max-w-sm sm:max-h-[80vh] sm:my-[10vh] sm:rounded-lg sm:mx-auto flex flex-col p-0 overflow-hidden">
        <DialogHeader className="flex-shrink-0 p-4 pb-3 border-b border-gray-200">
          <DialogTitle className="text-lg font-semibold text-gray-900">
            Node Inspector
          </DialogTitle>
        </DialogHeader>

        {/* Scrollable Content */}
        <div className="flex-1 min-h-0 overflow-y-auto overscroll-contain">
          <div className="space-y-4 px-4 py-2">
              <div>
                <Label htmlFor="node-title" className="text-sm font-medium text-gray-700">
                  Title
                </Label>
                <Input
                  id="node-title"
                  value={selectedNode.data.title}
                  onChange={(e) => onUpdateNode({ title: e.target.value })}
                  className="mt-1 min-h-[44px]"
                  placeholder="Enter node title"
                />
              </div>

              <div>
                <Label htmlFor="node-type" className="text-sm font-medium text-gray-700">
                  Category
                </Label>
                <Select
                  value={selectedNode.data.type}
                  onValueChange={(value: WorkflowNodeData['type']) => onUpdateNode({ type: value })}
                >
                  <SelectTrigger className="mt-1 min-h-[44px]">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="Attraction">Attraction</SelectItem>
                    <SelectItem value="Meal">Meal</SelectItem>
                    <SelectItem value="Transit">Transit</SelectItem>
                    <SelectItem value="Hotel">Hotel</SelectItem>
                    <SelectItem value="FreeTime">Free Time</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <Label htmlFor="node-start" className="text-sm font-medium text-gray-700">
                    Start Time
                  </Label>
                  <Input
                    id="node-start"
                    type="time"
                    value={selectedNode.data.start}
                    onChange={(e) => onUpdateNode({ start: e.target.value })}
                    className="mt-1 min-h-[44px]"
                  />
                </div>
                <div>
                  <Label htmlFor="node-duration" className="text-sm font-medium text-gray-700">
                    Duration (min)
                  </Label>
                  <Input
                    id="node-duration"
                    type="number"
                    value={selectedNode.data.durationMin}
                    onChange={(e) => onUpdateNode({ durationMin: parseInt(e.target.value) || 0 })}
                    className="mt-1 min-h-[44px]"
                    placeholder="60"
                  />
                </div>
              </div>

              <div>
                <Label htmlFor="node-cost" className="text-sm font-medium text-gray-700">
                  Cost (₹)
                </Label>
                <Input
                  id="node-cost"
                  type="number"
                  value={selectedNode.data.costINR}
                  onChange={(e) => onUpdateNode({ costINR: parseInt(e.target.value) || 0 })}
                  className="mt-1 min-h-[44px]"
                  placeholder="0"
                />
              </div>

              <div>
                <Label htmlFor="node-tags" className="text-sm font-medium text-gray-700">
                  Tags
                </Label>
                <Input
                  id="node-tags"
                  value={selectedNode.data.tags.join(', ')}
                  onChange={(e) => onUpdateNode({ tags: e.target.value.split(', ').filter(Boolean) })}
                  placeholder="veg, family, heritage"
                  className="mt-1 min-h-[44px]"
                />
              </div>

              <Separator className="my-4" />

              <div>
                <h4 className="font-medium mb-3 text-gray-900">Other Information</h4>
                <div className="space-y-3 text-sm">
                  <div className="flex justify-between items-center py-2 px-3 bg-gray-50 rounded-lg">
                    <span className="text-gray-600">Rating:</span>
                    <span className="font-medium">{selectedNode.data.meta.rating}/5</span>
                  </div>
                  <div className="flex justify-between items-center py-2 px-3 bg-gray-50 rounded-lg">
                    <span className="text-gray-600">Hours:</span>
                    <span className="font-medium">{selectedNode.data.meta.open} - {selectedNode.data.meta.close}</span>
                  </div>
                  <div className="py-2 px-3 bg-gray-50 rounded-lg">
                    <span className="text-gray-600 block mb-1">Address:</span>
                    <p className="text-gray-900 font-medium">{selectedNode.data.meta.address}</p>
                  </div>
                  <div className="flex justify-between items-center py-2 px-3 bg-gray-50 rounded-lg">
                    <span className="text-gray-600 block mb-1">Distance:</span>
                    <span className="font-medium">{selectedNode.data.meta.distanceKm} km</span>
                  </div>
                </div>
              </div>

              {selectedNode.data.validation && selectedNode.data.validation.status !== 'valid' && (
                <Alert className={`${selectedNode.data.validation.status === 'error' ? 'border-red-200 bg-red-50' : 'border-amber-200 bg-amber-50'}`}>
                  <AlertTriangle className="h-4 w-4" />
                  <AlertDescription>
                    {selectedNode.data.validation.message}
                  </AlertDescription>
                </Alert>
              )}
          </div>
        </div>

        {/* Actions */}
        <div className="flex-shrink-0 flex flex-row gap-3 p-4 border-t border-gray-200 bg-white">
          <Button
            variant="outline"
            onClick={handleDelete}
            className="min-h-[48px] flex-1 text-red-600 border-red-200 hover:bg-red-50 hover:border-red-300"
          >
            <Trash2 className="h-4 w-4 mr-2" />
            Delete
          </Button>
          <Button
            onClick={onClose}
            className="min-h-[48px] flex-1"
          >
            Save and Close
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
