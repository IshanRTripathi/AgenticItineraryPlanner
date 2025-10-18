import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { X, Copy, Check, Share2 } from 'lucide-react';
import { apiClient } from '../../services/apiClient';

interface ShareModalProps {
  itineraryId: string;
  isOpen: boolean;
  onClose: () => void;
}

export function ShareModal({ itineraryId, isOpen, onClose }: ShareModalProps) {
  const [shareLink, setShareLink] = useState('');
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  React.useEffect(() => {
    if (isOpen && !shareLink) {
      generateShareLink();
    }
  }, [isOpen]);

  const generateShareLink = async () => {
    setLoading(true);
    try {
      const response = await fetch(
        `${(apiClient as any).baseUrl}/itineraries/${itineraryId}:share`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${(apiClient as any).authToken || ''}`,
          },
          body: JSON.stringify({}),
        }
      );
      const data = await response.json();
      const link = `${window.location.origin}/shared/${data.shareId}`;
      setShareLink(link);
    } catch (error) {
      console.error('Failed to generate share link:', error);
    } finally {
      setLoading(false);
    }
  };

  const copyToClipboard = async () => {
    try {
      await navigator.clipboard.writeText(shareLink);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (error) {
      console.error('Failed to copy:', error);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Share2 className="w-5 h-5" />
              <CardTitle>Share Itinerary</CardTitle>
            </div>
            <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
              <X className="w-5 h-5" />
            </button>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-gray-600">
            Anyone with this link can view your itinerary
          </p>
          <div className="flex gap-2">
            <Input
              value={shareLink}
              readOnly
              placeholder={loading ? 'Generating link...' : 'Share link'}
              className="flex-1"
            />
            <Button
              onClick={copyToClipboard}
              disabled={!shareLink || loading}
              variant="outline"
            >
              {copied ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
            </Button>
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={onClose}>
              Close
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
