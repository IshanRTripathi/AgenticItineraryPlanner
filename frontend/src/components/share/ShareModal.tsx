/**
 * Share Modal
 * Allows users to share itinerary via link, email, or social media
 * Task 13: Mobile-optimized with ResponsiveModal and touch-friendly buttons
 */

import { useState } from 'react';
import { ResponsiveModal } from '@/components/ui/responsive-modal';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useToast } from '@/components/ui/use-toast';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { exportService } from '@/services/exportService';
import { Copy, Check, Mail, Link as LinkIcon, Share2 } from 'lucide-react';
import { EmailShareForm } from '@/components/share/EmailShareForm';

interface ShareModalProps {
  isOpen: boolean;
  onClose: () => void;
  itineraryId: string;
  itinerary: any;
}

export function ShareModal({
  isOpen,
  onClose,
  itineraryId,
  itinerary,
}: ShareModalProps) {
  const isMobile = useMediaQuery('(max-width: 768px)');
  const [shareLink, setShareLink] = useState('');
  const [isCopied, setIsCopied] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const { toast } = useToast();

  const generateLink = async () => {
    setIsGenerating(true);
    try {
      const link = await exportService.generateShareLink(itineraryId);
      setShareLink(link);
      toast({
        title: 'Link generated',
        description: 'Share link has been copied to clipboard',
      });
    } catch (error) {
      toast({
        title: 'Failed to generate link',
        description: 'Please try again',
        variant: 'destructive',
      });
    } finally {
      setIsGenerating(false);
    }
  };

  const copyToClipboard = async () => {
    try {
      await navigator.clipboard.writeText(shareLink);
      setIsCopied(true);
      setTimeout(() => setIsCopied(false), 2000);
      toast({
        title: 'Copied!',
        description: 'Link copied to clipboard',
      });
    } catch (error) {
      toast({
        title: 'Failed to copy',
        description: 'Please try again',
        variant: 'destructive',
      });
    }
  };

  const shareViaWebAPI = async () => {
    try {
      await exportService.shareViaWebAPI(itinerary);
    } catch (error) {
      // User cancelled or not supported
      if ((error as Error).name !== 'AbortError') {
        toast({
          title: 'Share failed',
          description: 'Could not share itinerary',
          variant: 'destructive',
        });
      }
    }
  };

  return (
    <ResponsiveModal
      open={isOpen}
      onOpenChange={onClose}
      title="Share Itinerary"
      description="Share your travel plans with friends and family"
      className="sm:max-w-[550px]"
    >
      <Tabs value="link" onValueChange={() => {}} className="w-full">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="link" className="min-h-[44px] gap-1 sm:gap-2">
            <LinkIcon className="w-3 h-3 sm:w-4 sm:h-4" />
            <span className="text-xs sm:text-sm">Link</span>
          </TabsTrigger>
          <TabsTrigger value="email" className="min-h-[44px] gap-1 sm:gap-2">
            <Mail className="w-3 h-3 sm:w-4 sm:h-4" />
            <span className="text-xs sm:text-sm">Email</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="link" className="space-y-4 mt-4">
          {/* Generate Link */}
          {!shareLink ? (
            <div className="text-center py-4 sm:py-6">
              <Button
                onClick={generateLink}
                disabled={isGenerating}
                size="lg"
                className="min-h-[48px] touch-manipulation active:scale-95 transition-transform"
              >
                {isGenerating ? 'Generating...' : 'Generate Share Link'}
              </Button>
              <p className="text-xs sm:text-sm text-muted-foreground mt-2">
                Create a shareable link for this itinerary
              </p>
            </div>
          ) : (
            <>
              {/* Copy Link */}
              <div className="space-y-2">
                <Label htmlFor="share-link" className="text-sm sm:text-base">Share Link</Label>
                <div className="flex gap-2">
                  <Input
                    id="share-link"
                    value={shareLink}
                    readOnly
                    className="flex-1 min-h-[44px] text-sm"
                  />
                  <Button
                    variant="outline"
                    size="icon"
                    onClick={copyToClipboard}
                    className="min-w-[48px] min-h-[48px] touch-manipulation active:scale-95 transition-transform"
                  >
                    {isCopied ? (
                      <Check className="w-4 h-4 sm:w-5 sm:h-5 text-green-600" />
                    ) : (
                      <Copy className="w-4 h-4 sm:w-5 sm:h-5" />
                    )}
                  </Button>
                </div>
                <p className="text-xs text-muted-foreground">
                  Anyone with this link can view your itinerary
                </p>
              </div>

              {/* Web Share API (Mobile) */}
              {typeof navigator !== 'undefined' && 'share' in navigator && (
                <Button
                  variant="outline"
                  className="w-full min-h-[48px] touch-manipulation active:scale-95 transition-transform"
                  onClick={shareViaWebAPI}
                >
                  <Share2 className="w-4 h-4 mr-2" />
                  Share via...
                </Button>
              )}

              {/* Generate New Link */}
              <Button
                variant="ghost"
                className="w-full min-h-[44px] touch-manipulation active:scale-95 transition-transform"
                onClick={generateLink}
                disabled={isGenerating}
              >
                Generate New Link
              </Button>
            </>
          )}
        </TabsContent>

        <TabsContent value="email" className="mt-4">
          <EmailShareForm
            itineraryId={itineraryId}
            onSuccess={() => {
              toast({
                title: 'Email sent!',
                description: 'Your itinerary has been shared',
              });
              onClose();
            }}
          />
        </TabsContent>
      </Tabs>
    </ResponsiveModal>
  );
}
