/**
 * Trip Navigation Modal
 * Compact popover attached to navbar with icon-only buttons
 * Matches navbar styling exactly
 */

import { useTranslation } from '@/i18n';
import { DollarSign, Package, FileText } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

interface TripNavModalProps {
  isOpen: boolean;
  onClose: () => void;
  onNavigate: (tab: string) => void;
  activeTab: string;
}

export function TripNavModal({ isOpen, onClose, onNavigate, activeTab }: TripNavModalProps) {
  const { t } = useTranslation();

  const otherTabs = [
    { id: 'budget', label: t('components.tripNav.budget'), icon: DollarSign },
    { id: 'packing', label: t('components.tripNav.packing'), icon: Package },
    { id: 'docs', label: t('components.tripNav.docs'), icon: FileText },
  ];

  const handleTabClick = (tabId: string) => {
    onNavigate(tabId);
    onClose();
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          {/* Backdrop - transparent, just for closing */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 z-40"
          />

          {/* Compact Popover - Matches navbar styling */}
          <motion.div
            initial={{ opacity: 0, y: 10, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 10, scale: 0.95 }}
            transition={{ type: 'spring', damping: 25, stiffness: 400 }}
            className="fixed bottom-20 right-4 z-50"
            style={{
              background: 'rgba(255, 255, 255, 0.95)',
              backdropFilter: 'blur(20px)',
              borderRadius: '16px',
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.12)',
              width: '64px',
            }}
          >
            {/* Vertically stacked buttons matching navbar style */}
            <div className="flex flex-col p-2 gap-1">
              {otherTabs.map((tab) => {
                const Icon = tab.icon;
                const isActive = activeTab === tab.id;

                return (
                  <button
                    key={tab.id}
                    onClick={() => handleTabClick(tab.id)}
                    className="menu__item relative flex flex-col items-center gap-0.5 p-1.5 border-none bg-transparent cursor-pointer transition-all"
                    style={{
                      touchAction: 'manipulation',
                    }}
                    title={tab.label}
                  >
                    <div 
                      className="menu__icon flex items-center justify-center"
                      style={{
                        width: '1.25rem',
                        height: '1.25rem',
                        color: isActive ? 'var(--component-active-color, hsl(var(--primary)))' : 'var(--component-inactive-color, hsl(var(--muted-foreground)))',
                      }}
                    >
                      <Icon className="icon" style={{ width: '100%', height: '100%', strokeWidth: 2 }} />
                    </div>
                    <span 
                      className="menu__text"
                      style={{
                        fontSize: '0.5625rem',
                        fontWeight: 500,
                        color: isActive ? 'var(--component-active-color, hsl(var(--primary)))' : 'var(--component-inactive-color, hsl(var(--muted-foreground)))',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {tab.label}
                    </span>
                  </button>
                );
              })}
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}
