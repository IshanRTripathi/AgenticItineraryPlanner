/**
 * Export Service
 * Handles PDF export and share functionality for itineraries
 */

import { NormalizedItinerary } from '@/types/dto';

class ExportService {
  /**
   * Generate PDF from itinerary
   * Uses browser print API for now - can be enhanced with jsPDF later
   */
  async exportToPDF(itinerary: NormalizedItinerary): Promise<void> {
    try {
      // Create a printable version of the itinerary
      const printWindow = window.open('', '_blank');
      if (!printWindow) {
        throw new Error('Popup blocked. Please allow popups for this site.');
      }

      const html = this.generatePrintableHTML(itinerary);
      printWindow.document.write(html);
      printWindow.document.close();

      // Wait for content to load
      printWindow.onload = () => {
        printWindow.print();
      };
    } catch (error) {
      console.error('Failed to export PDF:', error);
      throw error;
    }
  }

  /**
   * Generate share link for itinerary
   */
  async generateShareLink(itineraryId: string): Promise<string> {
    try {
      // In production, this would call backend API to generate a shareable link
      const baseUrl = window.location.origin;
      const shareUrl = `${baseUrl}/trip/${itineraryId}`;

      // Copy to clipboard
      await navigator.clipboard.writeText(shareUrl);

      return shareUrl;
    } catch (error) {
      console.error('Failed to generate share link:', error);
      throw error;
    }
  }

  /**
   * Share via Web Share API (mobile-friendly)
   */
  async shareViaWebAPI(itinerary: NormalizedItinerary): Promise<void> {
    if (!navigator.share) {
      throw new Error('Web Share API not supported');
    }

    try {
      const days = itinerary.days || [];
      const destination = days[0]?.location || 'Trip';
      const shareData = {
        title: `${destination} Itinerary`,
        text: `Check out my ${destination} travel itinerary!`,
        url: `${window.location.origin}/trip/${itinerary.itineraryId}`,
      };

      await navigator.share(shareData);
    } catch (error) {
      // User cancelled or error occurred
      if ((error as Error).name !== 'AbortError') {
        console.error('Failed to share:', error);
        throw error;
      }
    }
  }

  /**
   * Generate printable HTML for itinerary
   */
  private generatePrintableHTML(itinerary: NormalizedItinerary): string {
    const days = itinerary.days || [];
    const destination = days[0]?.location || 'Trip';
    const startDate = days[0]?.date || '';
    const endDate = days[days.length - 1]?.date || '';

    const formatDate = (date: string) => {
      return new Date(date).toLocaleDateString('en-US', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      });
    };

    const formatTime = (time: string) => {
      if (!time) return '';
      return new Date(`2000-01-01T${time}`).toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true,
      });
    };

    const getNodeIcon = (type: string) => {
      const icons: Record<string, string> = {
        attraction: '🏛️',
        meal: '🍽️',
        hotel: '🏨',
        accommodation: '🏨',
        transit: '🚗',
        transport: '🚗',
      };
      return icons[type] || '📍';
    };

    const daysHTML = days
      .map(
        (day, index) => `
      <div class="day" style="page-break-inside: avoid; margin-bottom: 30px;">
        <h2 style="color: #002B5B; border-bottom: 2px solid #F5C542; padding-bottom: 10px; margin-bottom: 20px;">
          Day ${day.dayNumber}: ${day.location}
        </h2>
        <p style="color: #666; margin-bottom: 20px;">${formatDate(day.date)}</p>
        
        <div class="activities">
          ${day.nodes
            .map(
              (node) => `
            <div class="activity" style="margin-bottom: 20px; padding: 15px; border-left: 4px solid #002B5B; background: #f9f9f9;">
              <div style="display: flex; align-items: start; gap: 10px;">
                <span style="font-size: 24px;">${getNodeIcon(node.type)}</span>
                <div style="flex: 1;">
                  <h3 style="margin: 0 0 5px 0; color: #002B5B;">${node.title}</h3>
                  ${(node as any).startTime ? `<p style="margin: 5px 0; color: #666;"><strong>Time:</strong> ${formatTime((node as any).startTime)}</p>` : ''}
                  ${node.location?.address ? `<p style="margin: 5px 0; color: #666;"><strong>Location:</strong> ${node.location.address}</p>` : ''}
                  ${(node as any).description ? `<p style="margin: 10px 0 0 0; color: #333;">${(node as any).description}</p>` : ''}
                  ${node.cost?.amount ? `<p style="margin: 5px 0; color: #F5C542; font-weight: bold;">Cost: $${node.cost.amount}</p>` : ''}
                </div>
              </div>
            </div>
          `
            )
            .join('')}
        </div>
      </div>
    `
      )
      .join('');

    return `
      <!DOCTYPE html>
      <html>
        <head>
          <meta charset="UTF-8">
          <title>${destination} Itinerary</title>
          <style>
            @media print {
              body { margin: 0; }
              .no-print { display: none; }
            }
            body {
              font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
              line-height: 1.6;
              color: #333;
              max-width: 800px;
              margin: 0 auto;
              padding: 40px 20px;
            }
            h1 {
              color: #002B5B;
              border-bottom: 3px solid #F5C542;
              padding-bottom: 15px;
              margin-bottom: 10px;
            }
            .subtitle {
              color: #666;
              margin-bottom: 30px;
            }
            .print-button {
              background: #002B5B;
              color: white;
              border: none;
              padding: 12px 24px;
              border-radius: 8px;
              cursor: pointer;
              font-size: 16px;
              margin-bottom: 30px;
            }
            .print-button:hover {
              background: #003d7a;
            }
          </style>
        </head>
        <body>
          <button class="print-button no-print" onclick="window.print()">Print / Save as PDF</button>
          
          <h1>${destination} Travel Itinerary</h1>
          <p class="subtitle">
            ${formatDate(startDate)} - ${formatDate(endDate)}
          </p>
          
          ${daysHTML}
          
          <div style="margin-top: 40px; padding-top: 20px; border-top: 1px solid #ddd; color: #999; font-size: 12px;">
            <p>Generated by EaseMyTrip Planner on ${new Date().toLocaleDateString()}</p>
          </div>
        </body>
      </html>
    `;
  }

  /**
   * Download itinerary as JSON
   */
  async downloadJSON(itinerary: NormalizedItinerary): Promise<void> {
    try {
      const dataStr = JSON.stringify(itinerary, null, 2);
      const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr);
      const exportFileDefaultName = `itinerary-${itinerary.itineraryId}-${Date.now()}.json`;

      const linkElement = document.createElement('a');
      linkElement.setAttribute('href', dataUri);
      linkElement.setAttribute('download', exportFileDefaultName);
      linkElement.click();
    } catch (error) {
      console.error('Failed to download JSON:', error);
      throw error;
    }
  }
}

export const exportService = new ExportService();
