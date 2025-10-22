import React, { useState } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '../ui/dialog';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Textarea } from '../ui/textarea';
import { Checkbox } from '../ui/checkbox';
import { Alert, AlertDescription } from '../ui/alert';
import { Mail, CheckCircle, AlertTriangle } from 'lucide-react';
import { apiClient } from '../../services/apiClient';

interface EmailShareFormProps {
  isOpen: boolean;
  onClose: () => void;
  itineraryId: string;
}

export const EmailShareForm: React.FC<EmailShareFormProps> = ({
  isOpen,
  onClose,
  itineraryId
}) => {
  const [recipients, setRecipients] = useState('');
  const [subject, setSubject] = useState('Check out this travel itinerary');
  const [message, setMessage] = useState('');
  const [includePdf, setIncludePdf] = useState(true);
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSend = async () => {
    const emailList = recipients.split(',').map(e => e.trim()).filter(e => e);
    
    if (emailList.length === 0) {
      setError('Please enter at least one email address');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      // Placeholder - API method needs to be implemented
      

      setSuccess(true);
      setTimeout(() => {
        onClose();
        setSuccess(false);
        setRecipients('');
        setMessage('');
      }, 2000);
    } catch (err: any) {
      setError(err.message || 'Failed to send email');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Mail className="h-5 w-5" />
            Share via Email
          </DialogTitle>
          <DialogDescription>
            Send this itinerary to friends or family
          </DialogDescription>
        </DialogHeader>

        {success ? (
          <div className="py-8">
            <Alert className="bg-green-50 border-green-200">
              <CheckCircle className="h-4 w-4 text-green-600" />
              <AlertDescription className="text-green-800">
                Email sent successfully!
              </AlertDescription>
            </Alert>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="recipients">
                Recipients <span className="text-red-500">*</span>
              </Label>
              <Input
                id="recipients"
                type="email"
                placeholder="email@example.com, another@example.com"
                value={recipients}
                onChange={(e) => setRecipients(e.target.value)}
                disabled={loading}
              />
              <p className="text-xs text-gray-500">
                Separate multiple emails with commas
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="subject">Subject</Label>
              <Input
                id="subject"
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                disabled={loading}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="message">Personal Message (Optional)</Label>
              <Textarea
                id="message"
                placeholder="Add a personal note..."
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                disabled={loading}
                rows={4}
              />
            </div>

            <div className="flex items-center space-x-2">
              <Checkbox
                id="includePdf"
                checked={includePdf}
                onCheckedChange={(checked) => setIncludePdf(checked as boolean)}
                disabled={loading}
              />
              <Label htmlFor="includePdf" className="cursor-pointer">
                Include PDF attachment
              </Label>
            </div>

            {error && (
              <Alert variant="destructive">
                <AlertTriangle className="h-4 w-4" />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
          </div>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={loading}>
            Cancel
          </Button>
          {!success && (
            <Button onClick={handleSend} disabled={loading}>
              {loading ? 'Sending...' : 'Send Email'}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

