/**
 * Email Share Form
 * Form for sharing itinerary via email
 */

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/use-toast';
import { itineraryApi } from '@/services/api';
import { Loader2, Mail, Plus, X } from 'lucide-react';
import { Badge } from '@/components/ui/badge';

interface EmailShareFormProps {
  itineraryId: string;
  onSuccess?: () => void;
}

export function EmailShareForm({ itineraryId, onSuccess }: EmailShareFormProps) {
  const [recipients, setRecipients] = useState<string[]>([]);
  const [currentEmail, setCurrentEmail] = useState('');
  const [message, setMessage] = useState('');
  const [isSending, setIsSending] = useState(false);
  const { toast } = useToast();

  const isValidEmail = (email: string) => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  };

  const addRecipient = () => {
    const email = currentEmail.trim();
    if (!email) return;

    if (!isValidEmail(email)) {
      toast({
        title: 'Invalid email',
        description: 'Please enter a valid email address',
        variant: 'destructive',
      });
      return;
    }

    if (recipients.includes(email)) {
      toast({
        title: 'Duplicate email',
        description: 'This email is already added',
        variant: 'destructive',
      });
      return;
    }

    setRecipients([...recipients, email]);
    setCurrentEmail('');
  };

  const removeRecipient = (email: string) => {
    setRecipients(recipients.filter((r) => r !== email));
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      addRecipient();
    }
  };

  const handleSend = async () => {
    if (recipients.length === 0) {
      toast({
        title: 'No recipients',
        description: 'Please add at least one email address',
        variant: 'destructive',
      });
      return;
    }

    setIsSending(true);
    try {
      await itineraryApi.post('/email/send', {
        itineraryId,
        recipients,
        message: message || undefined,
      });

      toast({
        title: 'Email sent!',
        description: `Itinerary shared with ${recipients.length} ${
          recipients.length === 1 ? 'person' : 'people'
        }`,
      });

      // Reset form
      setRecipients([]);
      setMessage('');

      if (onSuccess) {
        onSuccess();
      }
    } catch (error) {
      console.error('Failed to send email:', error);
      toast({
        title: 'Failed to send',
        description: 'Could not send email. Please try again.',
        variant: 'destructive',
      });
    } finally {
      setIsSending(false);
    }
  };

  return (
    <div className="space-y-4">
      {/* Email Input */}
      <div className="space-y-2">
        <Label htmlFor="email">Recipient Email</Label>
        <div className="flex gap-2">
          <Input
            id="email"
            type="email"
            placeholder="friend@example.com"
            value={currentEmail}
            onChange={(e) => setCurrentEmail(e.target.value)}
            onKeyPress={handleKeyPress}
            disabled={isSending}
          />
          <Button
            type="button"
            variant="outline"
            size="icon"
            onClick={addRecipient}
            disabled={isSending}
          >
            <Plus className="w-4 h-4" />
          </Button>
        </div>
        <p className="text-xs text-muted-foreground">
          Press Enter or click + to add multiple recipients
        </p>
      </div>

      {/* Recipients List */}
      {recipients.length > 0 && (
        <div className="space-y-2">
          <Label>Recipients ({recipients.length})</Label>
          <div className="flex flex-wrap gap-2">
            {recipients.map((email) => (
              <Badge key={email} variant="secondary" className="gap-1">
                {email}
                <button
                  type="button"
                  onClick={() => removeRecipient(email)}
                  className="ml-1 hover:text-destructive"
                  disabled={isSending}
                >
                  <X className="w-3 h-3" />
                </button>
              </Badge>
            ))}
          </div>
        </div>
      )}

      {/* Optional Message */}
      <div className="space-y-2">
        <Label htmlFor="message">Message (Optional)</Label>
        <Textarea
          id="message"
          placeholder="Add a personal message..."
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          rows={4}
          disabled={isSending}
        />
      </div>

      {/* Send Button */}
      <Button
        className="w-full"
        onClick={handleSend}
        disabled={isSending || recipients.length === 0}
      >
        {isSending ? (
          <>
            <Loader2 className="w-4 h-4 mr-2 animate-spin" />
            Sending...
          </>
        ) : (
          <>
            <Mail className="w-4 h-4 mr-2" />
            Send Email
          </>
        )}
      </Button>
    </div>
  );
}
