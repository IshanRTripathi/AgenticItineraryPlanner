/**
 * Packing Templates Component
 * Quick-start templates for common trip types
 */

import { motion } from 'framer-motion';
import { Plane, Briefcase, Mountain, Palmtree, Camera, Heart } from 'lucide-react';
import { cn } from '@/lib/utils';

interface PackingTemplate {
  id: string;
  name: string;
  description: string;
  icon: React.ReactNode;
  color: string;
}

interface PackingTemplatesProps {
  onSelectTemplate: (templateId: string) => void;
}

const templates: PackingTemplate[] = [
  {
    id: 'beach',
    name: 'Beach Vacation',
    description: 'Swimwear, sunscreen, flip-flops',
    icon: <Palmtree className="w-5 h-5" />,
    color: 'from-cyan-500 to-blue-500',
  },
  {
    id: 'business',
    name: 'Business Trip',
    description: 'Suits, laptop, documents',
    icon: <Briefcase className="w-5 h-5" />,
    color: 'from-gray-600 to-gray-800',
  },
  {
    id: 'adventure',
    name: 'Adventure',
    description: 'Hiking gear, outdoor equipment',
    icon: <Mountain className="w-5 h-5" />,
    color: 'from-green-600 to-emerald-700',
  },
  {
    id: 'city',
    name: 'City Break',
    description: 'Casual wear, camera, guidebook',
    icon: <Camera className="w-5 h-5" />,
    color: 'from-purple-500 to-pink-500',
  },
  {
    id: 'romantic',
    name: 'Romantic Getaway',
    description: 'Nice outfits, special items',
    icon: <Heart className="w-5 h-5" />,
    color: 'from-rose-500 to-red-500',
  },
  {
    id: 'general',
    name: 'General Travel',
    description: 'All-purpose essentials',
    icon: <Plane className="w-5 h-5" />,
    color: 'from-blue-500 to-indigo-600',
  },
];

export function PackingTemplates({ onSelectTemplate }: PackingTemplatesProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white rounded-xl border border-gray-200 p-5"
    >
      <div className="mb-4">
        <h3 className="text-sm font-semibold text-gray-900 mb-1">Quick Start Templates</h3>
        <p className="text-xs text-muted-foreground">
          Choose a template to get started quickly
        </p>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
        {templates.map((template) => (
          <motion.button
            key={template.id}
            onClick={() => onSelectTemplate(template.id)}
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            className="relative overflow-hidden rounded-lg border border-gray-200 p-4 text-left hover:shadow-lg transition-all group"
          >
            {/* Gradient Background */}
            <div
              className={cn(
                'absolute inset-0 bg-gradient-to-br opacity-0 group-hover:opacity-10 transition-opacity',
                template.color
              )}
            />

            {/* Content */}
            <div className="relative">
              <div
                className={cn(
                  'w-10 h-10 rounded-lg bg-gradient-to-br flex items-center justify-center text-white mb-3',
                  template.color
                )}
              >
                {template.icon}
              </div>
              <div className="font-medium text-sm text-gray-900 mb-1">
                {template.name}
              </div>
              <div className="text-xs text-muted-foreground line-clamp-2">
                {template.description}
              </div>
            </div>
          </motion.button>
        ))}
      </div>
    </motion.div>
  );
}
