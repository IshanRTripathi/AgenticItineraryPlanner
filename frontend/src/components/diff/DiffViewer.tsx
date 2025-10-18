/**
 * Advanced Diff Viewer Component
 * Displays side-by-side comparison of changes with collapsible sections and search
 */

import React, { useState, useMemo } from 'react';
import './DiffViewer.css';

export interface DiffChange {
  type: 'added' | 'removed' | 'modified' | 'unchanged';
  path: string;
  oldValue?: any;
  newValue?: any;
  label?: string;
}

export interface DiffSection {
  title: string;
  changes: DiffChange[];
  collapsed?: boolean;
}

interface DiffViewerProps {
  sections: DiffSection[];
  viewMode?: 'side-by-side' | 'unified';
  showUnchanged?: boolean;
  onClose?: () => void;
}

export const DiffViewer: React.FC<DiffViewerProps> = ({
  sections: initialSections,
  viewMode = 'side-by-side',
  showUnchanged = false,
  onClose,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [collapsedSections, setCollapsedSections] = useState<Set<string>>(new Set());
  const [currentViewMode, setCurrentViewMode] = useState(viewMode);

  // Filter changes based on search term
  const filteredSections = useMemo(() => {
    if (!searchTerm) return initialSections;

    return initialSections
      .map(section => ({
        ...section,
        changes: section.changes.filter(change =>
          change.path.toLowerCase().includes(searchTerm.toLowerCase()) ||
          change.label?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          JSON.stringify(change.oldValue).toLowerCase().includes(searchTerm.toLowerCase()) ||
          JSON.stringify(change.newValue).toLowerCase().includes(searchTerm.toLowerCase())
        ),
      }))
      .filter(section => section.changes.length > 0);
  }, [initialSections, searchTerm]);

  // Calculate statistics
  const stats = useMemo(() => {
    let added = 0, removed = 0, modified = 0;
    initialSections.forEach(section => {
      section.changes.forEach(change => {
        if (change.type === 'added') added++;
        else if (change.type === 'removed') removed++;
        else if (change.type === 'modified') modified++;
      });
    });
    return { added, removed, modified, total: added + removed + modified };
  }, [initialSections]);

  const toggleSection = (title: string) => {
    setCollapsedSections(prev => {
      const next = new Set(prev);
      if (next.has(title)) {
        next.delete(title);
      } else {
        next.add(title);
      }
      return next;
    });
  };

  const renderValue = (value: any): string => {
    if (value === null || value === undefined) return '(empty)';
    if (typeof value === 'object') return JSON.stringify(value, null, 2);
    return String(value);
  };

  const renderChange = (change: DiffChange, index: number) => {
    if (!showUnchanged && change.type === 'unchanged') return null;

    const typeIcons = {
      added: '➕',
      removed: '➖',
      modified: '🔄',
      unchanged: '—',
    };

    return (
      <div key={index} className={`diff-change diff-change-${change.type}`}>
        <div className="diff-change-header">
          <span className="diff-change-icon">{typeIcons[change.type]}</span>
          <span className="diff-change-path">{change.label || change.path}</span>
        </div>

        {currentViewMode === 'side-by-side' ? (
          <div className="diff-change-content side-by-side">
            <div className="diff-column diff-column-old">
              <div className="diff-column-label">Before</div>
              <pre className="diff-value">{renderValue(change.oldValue)}</pre>
            </div>
            <div className="diff-column diff-column-new">
              <div className="diff-column-label">After</div>
              <pre className="diff-value">{renderValue(change.newValue)}</pre>
            </div>
          </div>
        ) : (
          <div className="diff-change-content unified">
            {change.oldValue !== undefined && (
              <div className="diff-line diff-line-removed">
                <span className="diff-line-marker">-</span>
                <pre className="diff-value">{renderValue(change.oldValue)}</pre>
              </div>
            )}
            {change.newValue !== undefined && (
              <div className="diff-line diff-line-added">
                <span className="diff-line-marker">+</span>
                <pre className="diff-value">{renderValue(change.newValue)}</pre>
              </div>
            )}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="diff-viewer">
      <div className="diff-viewer-header">
        <div className="diff-viewer-title">
          <h3>Change Comparison</h3>
          <div className="diff-stats">
            <span className="diff-stat diff-stat-added">+{stats.added}</span>
            <span className="diff-stat diff-stat-removed">-{stats.removed}</span>
            <span className="diff-stat diff-stat-modified">~{stats.modified}</span>
            <span className="diff-stat-total">{stats.total} total changes</span>
          </div>
        </div>

        <div className="diff-viewer-controls">
          <div className="diff-search">
            <input
              type="text"
              placeholder="Search changes..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="diff-search-input"
            />
            {searchTerm && (
              <button
                className="diff-search-clear"
                onClick={() => setSearchTerm('')}
                aria-label="Clear search"
              >
                ✕
              </button>
            )}
          </div>

          <div className="diff-view-mode-toggle">
            <button
              className={`view-mode-btn ${currentViewMode === 'side-by-side' ? 'active' : ''}`}
              onClick={() => setCurrentViewMode('side-by-side')}
              title="Side-by-side view"
            >
              ⬌
            </button>
            <button
              className={`view-mode-btn ${currentViewMode === 'unified' ? 'active' : ''}`}
              onClick={() => setCurrentViewMode('unified')}
              title="Unified view"
            >
              ☰
            </button>
          </div>

          {onClose && (
            <button className="diff-close-btn" onClick={onClose} aria-label="Close">
              ✕
            </button>
          )}
        </div>
      </div>

      <div className="diff-viewer-content">
        {filteredSections.length === 0 ? (
          <div className="diff-empty-state">
            <span className="diff-empty-icon">🔍</span>
            <p>No changes found{searchTerm ? ` matching "${searchTerm}"` : ''}</p>
          </div>
        ) : (
          filteredSections.map((section, sectionIndex) => {
            const isCollapsed = collapsedSections.has(section.title);
            const sectionStats = {
              added: section.changes.filter(c => c.type === 'added').length,
              removed: section.changes.filter(c => c.type === 'removed').length,
              modified: section.changes.filter(c => c.type === 'modified').length,
            };

            return (
              <div key={sectionIndex} className="diff-section">
                <div
                  className="diff-section-header"
                  onClick={() => toggleSection(section.title)}
                >
                  <span className={`diff-section-toggle ${isCollapsed ? 'collapsed' : ''}`}>
                    ▶
                  </span>
                  <h4 className="diff-section-title">{section.title}</h4>
                  <div className="diff-section-stats">
                    {sectionStats.added > 0 && (
                      <span className="diff-section-stat added">+{sectionStats.added}</span>
                    )}
                    {sectionStats.removed > 0 && (
                      <span className="diff-section-stat removed">-{sectionStats.removed}</span>
                    )}
                    {sectionStats.modified > 0 && (
                      <span className="diff-section-stat modified">~{sectionStats.modified}</span>
                    )}
                  </div>
                </div>

                {!isCollapsed && (
                  <div className="diff-section-content">
                    {section.changes.map((change, changeIndex) =>
                      renderChange(change, changeIndex)
                    )}
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
