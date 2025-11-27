import React, { useState, useEffect } from 'react';
import { Memory, MemoryCategory, memoryApi } from '../../services/memoryApi';
import { PreferenceSection } from './PreferenceSection';
import './PreferencesPanel.css';

interface PreferencesPanelProps {
  itineraryId: string;
  isOpen: boolean;
  onClose: () => void;
}

export const PreferencesPanel: React.FC<PreferencesPanelProps> = ({
  itineraryId,
  isOpen,
  onClose
}) => {
  const [memories, setMemories] = useState<Memory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  useEffect(() => {
    if (isOpen) {
      loadMemories();
    }
  }, [isOpen, itineraryId]);
  
  const loadMemories = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await memoryApi.getAll(itineraryId);
      setMemories(data);
    } catch (err: any) {
      console.error('Failed to load memories:', err);
      setError(err.message || 'Failed to load preferences');
    } finally {
      setLoading(false);
    }
  };
  
  const handleUpdate = async (memoryId: string, updates: Partial<Memory>) => {
    try {
      await memoryApi.update(itineraryId, memoryId, updates);
      await loadMemories(); // Reload
    } catch (err: any) {
      console.error('Failed to update memory:', err);
      alert('Failed to update preference');
    }
  };
  
  const handleDelete = async (memoryId: string) => {
    try {
      await memoryApi.delete(itineraryId, memoryId);
      await loadMemories(); // Reload
    } catch (err: any) {
      console.error('Failed to delete memory:', err);
      alert('Failed to delete preference');
    }
  };
  
  // Group memories by category
  const grouped = memories.reduce((acc, memory) => {
    const category = memory.category;
    if (!acc[category]) {
      acc[category] = [];
    }
    acc[category].push(memory);
    return acc;
  }, {} as Record<MemoryCategory, Memory[]>);
  
  if (!isOpen) return null;
  
  return (
    <div className="preferences-panel-overlay" onClick={onClose}>
      <div className="preferences-panel" onClick={(e) => e.stopPropagation()}>
        <div className="panel-header">
          <h2>Your Travel Preferences</h2>
          <button className="close-button" onClick={onClose}>✕</button>
        </div>
        
        <p className="panel-subtitle">
          We learn from your trips to personalize recommendations
        </p>
        
        <div className="panel-content">
          {loading && (
            <div className="loading-state">Loading preferences...</div>
          )}
          
          {error && (
            <div className="error-state">
              <p>❌ {error}</p>
              <button onClick={loadMemories}>Retry</button>
            </div>
          )}
          
          {!loading && !error && memories.length === 0 && (
            <div className="empty-state">
              <p>No preferences yet. We'll learn as you create trips!</p>
            </div>
          )}
          
          {!loading && !error && memories.length > 0 && (
            <>
              {grouped[MemoryCategory.ACTIVITY] && (
                <PreferenceSection
                  title="Activity Preferences"
                  icon="🎯"
                  memories={grouped[MemoryCategory.ACTIVITY]}
                  onUpdate={handleUpdate}
                  onDelete={handleDelete}
                />
              )}
              
              {grouped[MemoryCategory.BUDGET] && (
                <PreferenceSection
                  title="Budget & Spending"
                  icon="💰"
                  memories={grouped[MemoryCategory.BUDGET]}
                  onUpdate={handleUpdate}
                  onDelete={handleDelete}
                />
              )}
              
              {grouped[MemoryCategory.TIMING] && (
                <PreferenceSection
                  title="Travel Pace"
                  icon="⏱️"
                  memories={grouped[MemoryCategory.TIMING]}
                  onUpdate={handleUpdate}
                  onDelete={handleDelete}
                />
              )}
              
              {grouped[MemoryCategory.MEAL] && (
                <PreferenceSection
                  title="Dining Preferences"
                  icon="🍽️"
                  memories={grouped[MemoryCategory.MEAL]}
                  onUpdate={handleUpdate}
                  onDelete={handleDelete}
                />
              )}
              
              {grouped[MemoryCategory.DIETARY] && (
                <PreferenceSection
                  title="Dietary Restrictions"
                  icon="🥗"
                  memories={grouped[MemoryCategory.DIETARY]}
                  onUpdate={handleUpdate}
                  onDelete={handleDelete}
                  isPrivate={true}
                />
              )}
              
              {grouped[MemoryCategory.VALIDATION] && (
                <PreferenceSection
                  title="Validation Preferences"
                  icon="✅"
                  memories={grouped[MemoryCategory.VALIDATION]}
                  onUpdate={handleUpdate}
                  onDelete={handleDelete}
                />
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};
