import React, { useState } from 'react';
import { Card, CardContent } from '../../ui/card';
import { MapPin, Globe, MessageSquare } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { TopNavigation } from '../layout/TopNavigation';

interface MobilePlanViewProps {
  onCardSelect: (cardType: 'plan' | 'map' | 'chat') => void;
  onBack: () => void;
}

export function MobilePlanView({ onCardSelect, onBack }: MobilePlanViewProps) {
  const { t } = useTranslation();
  
  

  const cards = [
    {
      id: 'plan' as const,
      title: 'Plan',
      description: 'Destinations & Day-by-Day',
      icon: MapPin,
      color: 'bg-blue-500',
      hoverColor: 'hover:bg-blue-600',
    },
    {
      id: 'map' as const,
      title: 'Map',
      description: 'Interactive Map & Workflow',
      icon: Globe,
      color: 'bg-green-500',
      hoverColor: 'hover:bg-green-600',
    },
    {
      id: 'chat' as const,
      title: 'AI Assistant',
      description: 'Chat with AI',
      icon: MessageSquare,
      color: 'bg-purple-500',
      hoverColor: 'hover:bg-purple-600',
    },
  ];

  return (
    <div className="h-full w-full flex flex-col" style={{ 
      minHeight: '100vh', 
      backgroundColor: '#f9fafb',
      position: 'relative'
    }}>
      {/* Top Navigation */}
      <TopNavigation
        tripData={null as any} // We don't need trip data for this view
        onShare={() => {}} // Not needed for this view
        onExportPDF={() => {}} // Not needed for this view
        onBack={onBack}
      />

      {/* Cards Grid */}
      <div className="flex-1 p-4" style={{ backgroundColor: '#f9fafb' }}>
        <div className="grid grid-cols-1 gap-4 max-w-md mx-auto">
          {cards.map((card) => {
            const Icon = card.icon;
            return (
              <div
                key={card.id}
                className="cursor-pointer transition-all duration-200 hover:shadow-lg hover:scale-105 active:scale-95 bg-white border border-gray-200 rounded-xl p-6"
                onClick={() => {
                  
                  onCardSelect(card.id);
                }}
                style={{ 
                  backgroundColor: 'white',
                  border: '1px solid #e5e7eb',
                  borderRadius: '12px',
                  padding: '24px'
                }}
              >
                <div className="flex items-center space-x-4">
                  <div 
                    className={`w-12 h-12 ${card.color} ${card.hoverColor} rounded-lg flex items-center justify-center transition-colors`}
                    style={{ 
                      width: '48px', 
                      height: '48px', 
                      borderRadius: '8px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}
                  >
                    <Icon className="w-6 h-6 text-white" />
                  </div>
                  <div className="flex-1">
                    <h3 className="text-lg font-semibold text-gray-900">{card.title}</h3>
                    <p className="text-sm text-gray-600">{card.description}</p>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

