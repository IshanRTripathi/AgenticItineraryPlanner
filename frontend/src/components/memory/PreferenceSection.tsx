import React from 'react';
import { Memory } from '../../services/memoryApi';
import { PreferenceItem } from './PreferenceItem';
import './PreferenceSection.css';

interface PreferenceSectionProps {
  title: string;
  icon: React.ReactNode;
  memories: Memory[];
  onUpdate: (memoryId: string, updates: Partial<Memory>) => void;
  onDelete: (memoryId: string) => void;
  isPrivate?: boolean;
}

export const PreferenceSection: React.FC<PreferenceSectionProps> = ({
  title,
  icon,
  memories,
  onUpdate,
  onDelete,
  isPrivate = false
}) => {
  if (memories.length === 0) {
    return null;
  }
  
  return (
    <div className="preference-section">
      <div className="section-header">
        <span className="section-icon">{icon}</span>
        <h3 className="section-title">{title}</h3>
        {isPrivate && <span className="private-icon">🔒</span>}
      </div>
      
      <div className="section-content">
        {memories.map(memory => (
          <PreferenceItem
            key={memory.id}
            memory={memory}
            onUpdate={onUpdate}
            onDelete={onDelete}
          />
        ))}
      </div>
    </div>
  );
};
