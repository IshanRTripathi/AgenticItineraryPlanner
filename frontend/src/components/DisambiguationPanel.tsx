import React, { useState } from 'react';
import { NodeCandidate } from '../types/ChatTypes';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { Input } from './ui/input';
import { MapPin, Calendar, Star, Search, X } from 'lucide-react';
import { DisambiguationContext } from './chat/DisambiguationContext';
import './DisambiguationPanel.css';

interface DisambiguationPanelProps {
  candidates: NodeCandidate[];
  onSelect: (candidate: NodeCandidate) => void;
  onCancel: () => void;
  context?: string;
  disambiguationReason?: string;
  ambiguousTerm?: string;
  currentContext?: {
    day?: number;
    location?: string;
    previousActivity?: string;
  };
}

export const DisambiguationPanel: React.FC<DisambiguationPanelProps> = ({
  candidates,
  onSelect,
  onCancel,
  context,
  disambiguationReason,
  ambiguousTerm,
  currentContext
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [hoveredId, setHoveredId] = useState<string | null>(null);

  const filteredCandidates = candidates.filter(c =>
    c.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
    c.location?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const getTypeIcon = (type: string) => {
    const iconMap: Record<string, React.ReactNode> = {
      'attraction': <MapPin className="h-5 w-5" />,
      'meal': <span className="text-xl">🍽️</span>,
      'hotel': <span className="text-xl">🏨</span>,
      'accommodation': <span className="text-xl">🏨</span>,
      'transit': <span className="text-xl">🚗</span>,
      'transport': <span className="text-xl">🚗</span>,
      'activity': <span className="text-xl">🎯</span>,
    };
    return iconMap[type] || <MapPin className="h-5 w-5" />;
  };

  const getConfidenceBadge = (confidence?: number) => {
    if (!confidence) return null;
    const percent = Math.round(confidence * 100);
    if (confidence >= 0.8) return <Badge className="bg-green-100 text-green-800">{percent}% match</Badge>;
    if (confidence >= 0.6) return <Badge className="bg-yellow-100 text-yellow-800">{percent}% match</Badge>;
    return <Badge className="bg-gray-100 text-gray-800">{percent}% match</Badge>;
  };

  return (
    <Card className="w-full max-w-2xl mx-auto">
      <CardHeader>
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <CardTitle className="flex items-center gap-2">
              <span className="text-2xl">🤔</span>
              Which one did you mean?
            </CardTitle>
            {context && (
              <p className="text-sm text-gray-600 mt-2">
                Context: {context}
              </p>
            )}
          </div>
          <Button variant="ghost" size="sm" onClick={onCancel}>
            <X className="h-4 w-4" />
          </Button>
        </div>
        
        {candidates.length > 3 && (
          <div className="relative mt-4">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-gray-400" />
            <Input
              placeholder="Search options..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-9"
            />
          </div>
        )}
      </CardHeader>
      
      {/* Disambiguation Context */}
      {disambiguationReason && ambiguousTerm && (
        <div className="px-6">
          <DisambiguationContext
            reason={disambiguationReason}
            ambiguousTerm={ambiguousTerm}
            currentContext={currentContext}
          />
        </div>
      )}
      
      <CardContent>
        <div className="space-y-2">
          {filteredCandidates.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              No matches found
            </div>
          ) : (
            filteredCandidates.map((candidate) => (
              <div
                key={candidate.id}
                className={`p-4 border rounded-lg cursor-pointer transition-all ${
                  hoveredId === candidate.id
                    ? 'border-blue-500 bg-blue-50 shadow-md'
                    : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                }`}
                onClick={() => onSelect(candidate)}
                onMouseEnter={() => setHoveredId(candidate.id)}
                onMouseLeave={() => setHoveredId(null)}
              >
                <div className="flex items-start gap-3">
                  <div className="flex-shrink-0 mt-1">
                    {getTypeIcon(candidate.type)}
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <h4 className="font-medium text-gray-900">{candidate.title}</h4>
                      {getConfidenceBadge(candidate.confidence)}
                    </div>
                    
                    <div className="flex flex-wrap items-center gap-2 mt-2 text-sm text-gray-600">
                      <Badge variant="outline">{candidate.type}</Badge>
                      <div className="flex items-center gap-1">
                        <Calendar className="h-3 w-3" />
                        Day {candidate.day}
                      </div>
                      {candidate.location && (
                        <div className="flex items-center gap-1">
                          <MapPin className="h-3 w-3" />
                          {candidate.location}
                        </div>
                      )}
                    </div>
                    
                    {candidate.preview && (
                      <p className="text-sm text-gray-500 mt-2 line-clamp-2">
                        {candidate.preview}
                      </p>
                    )}
                  </div>
                  
                  <div className="flex-shrink-0">
                    <span className="text-gray-400">→</span>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
        
        <div className="mt-4 pt-4 border-t">
          <Button variant="outline" onClick={onCancel} className="w-full">
            None of these
          </Button>
        </div>
      </CardContent>
    </Card>
  );
};
