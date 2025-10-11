import React, { useState } from 'react';
import { Button } from '../ui/button';
import { Download, Loader2 } from 'lucide-react';
import { apiClient } from '../../services/apiClient';

interface PdfExportButtonProps {
  itineraryId: string;
  variant?: 'default' | 'outline' | 'ghost';
  size?: 'sm' | 'default' | 'lg';
  className?: string;
  onSuccess?: () => void;
  onError?: (error: Error) => void;
}

export function PdfExportButton({
  itineraryId,
  variant = 'default',
  size = 'default',
  className = '',
  onSuccess,
  onError,
}: PdfExportButtonProps) {
  const [loading, setLoading] = useState(false);

  const handleExport = async () => {
    setLoading(true);
    try {
      const response = await fetch(
        `${apiClient['baseUrl']}/export/itineraries/${itineraryId}/pdf`,
        {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${(apiClient as any).authToken || ''}`,
          },
        }
      );

      if (!response.ok) throw new Error('Export failed');

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `itinerary-${itineraryId}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      onSuccess?.();
    } catch (error) {
      console.error('PDF export failed:', error);
      onError?.(error as Error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Button
      variant={variant}
      size={size}
      onClick={handleExport}
      disabled={loading}
      className={className}
    >
      {loading ? (
        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
      ) : (
        <Download className="w-4 h-4 mr-2" />
      )}
      {loading ? 'Exporting...' : 'Export PDF'}
    </Button>
  );
}
