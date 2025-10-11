import React, { useState } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '../ui/dialog';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Textarea } from '../ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';

interface AgentParameter {
  name: string;
  type: 'string' | 'number' | 'boolean' | 'select';
  label: string;
  description?: string;
  required?: boolean;
  options?: string[];
  defaultValue?: any;
}

interface AgentConfigModalProps {
  isOpen: boolean;
  onClose: () => void;
  onExecute: (config: Record<string, any>) => void;
  agentType: string;
  agentName: string;
  parameters: AgentParameter[];
}

export const AgentConfigModal: React.FC<AgentConfigModalProps> = ({
  isOpen,
  onClose,
  onExecute,
  agentType,
  agentName,
  parameters
}) => {
  const [config, setConfig] = useState<Record<string, any>>(() => {
    const initial: Record<string, any> = {};
    parameters.forEach(param => {
      initial[param.name] = param.defaultValue ?? '';
    });
    return initial;
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleChange = (name: string, value: any) => {
    setConfig(prev => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[name];
        return newErrors;
      });
    }
  };

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};
    parameters.forEach(param => {
      if (param.required && !config[param.name]) {
        newErrors[param.name] = `${param.label} is required`;
      }
    });
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleExecute = () => {
    if (validate()) {
      onExecute(config);
      onClose();
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Configure {agentName}</DialogTitle>
          <DialogDescription>
            Set parameters for the {agentType} agent execution
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {parameters.map(param => (
            <div key={param.name} className="space-y-2">
              <Label htmlFor={param.name}>
                {param.label}
                {param.required && <span className="text-red-500 ml-1">*</span>}
              </Label>
              
              {param.description && (
                <p className="text-sm text-gray-500">{param.description}</p>
              )}

              {param.type === 'string' && (
                <Input
                  id={param.name}
                  value={config[param.name] || ''}
                  onChange={(e) => handleChange(param.name, e.target.value)}
                  placeholder={param.label}
                />
              )}

              {param.type === 'number' && (
                <Input
                  id={param.name}
                  type="number"
                  value={config[param.name] || ''}
                  onChange={(e) => handleChange(param.name, parseFloat(e.target.value))}
                  placeholder={param.label}
                />
              )}

              {param.type === 'boolean' && (
                <Select
                  value={config[param.name]?.toString() || 'false'}
                  onValueChange={(value) => handleChange(param.name, value === 'true')}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="true">Yes</SelectItem>
                    <SelectItem value="false">No</SelectItem>
                  </SelectContent>
                </Select>
              )}

              {param.type === 'select' && param.options && (
                <Select
                  value={config[param.name] || ''}
                  onValueChange={(value) => handleChange(param.name, value)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={`Select ${param.label}`} />
                  </SelectTrigger>
                  <SelectContent>
                    {param.options.map(option => (
                      <SelectItem key={option} value={option}>
                        {option}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}

              {errors[param.name] && (
                <p className="text-sm text-red-500">{errors[param.name]}</p>
              )}
            </div>
          ))}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={handleExecute}>
            Execute Agent
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
