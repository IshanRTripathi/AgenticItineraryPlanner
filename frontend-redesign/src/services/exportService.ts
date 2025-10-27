/**
 * Export Service
 * Handles PDF export and email sharing
 */

import { apiClient } from './apiClient';

export interface ExportOptions {
  format?: 'pdf' | 'docx';
  includeImages?: boolean;
  includeMap?: boolean;
  includeBookings?: boolean;
}

export interface EmailShareOptions {
  recipients: string[];
  message?: string;
  includeAttachment?: boolean;
}

class ExportService {
  /**
   * Export itinerary as PDF
   */
  async exportPdf(itineraryId: string, options: ExportOptions = {}): Promise<{
    success: boolean;
    url?: string;
    error?: string;
  }> {
    try {
      const response = await apiClient.post(
        `/export/${itineraryId}/pdf`,
        options,
        { responseType: 'blob' }
      );

      // Create download URL
      const blob = new Blob([response.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);

      return { success: true, url };
    } catch (error: any) {
      console.error('[Export] PDF export failed:', error);
      return {
        success: false,
        error: error.response?.data?.message || 'Failed to export PDF',
      };
    }
  }

  /**
   * Download PDF directly
   */
  async downloadPdf(itineraryId: string, filename?: string, options: ExportOptions = {}) {
    const result = await this.exportPdf(itineraryId, options);
    
    if (result.success && result.url) {
      const link = document.createElement('a');
      link.href = result.url;
      link.download = filename || `itinerary-${itineraryId}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(result.url);
      return { success: true };
    }

    return result;
  }

  /**
   * Export itinerary as DOCX
   */
  async exportDocx(itineraryId: string, options: ExportOptions = {}): Promise<{
    success: boolean;
    url?: string;
    error?: string;
  }> {
    try {
      const response = await apiClient.post(
        `/export/${itineraryId}/docx`,
        options,
        { responseType: 'blob' }
      );

      const blob = new Blob([response.data], { 
        type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' 
      });
      const url = window.URL.createObjectURL(blob);

      return { success: true, url };
    } catch (error: any) {
      console.error('[Export] DOCX export failed:', error);
      return {
        success: false,
        error: error.response?.data?.message || 'Failed to export DOCX',
      };
    }
  }

  /**
   * Share itinerary via email
   */
  async shareViaEmail(
    itineraryId: string,
    options: EmailShareOptions
  ): Promise<{
    success: boolean;
    error?: string;
  }> {
    try {
      await apiClient.post(`/export/${itineraryId}/email`, options);
      return { success: true };
    } catch (error: any) {
      console.error('[Export] Email share failed:', error);
      return {
        success: false,
        error: error.response?.data?.message || 'Failed to send email',
      };
    }
  }

  /**
   * Get shareable link
   */
  async getShareableLink(itineraryId: string): Promise<{
    success: boolean;
    link?: string;
    expiresAt?: string;
    error?: string;
  }> {
    try {
      const response = await apiClient.post(`/export/${itineraryId}/share-link`);
      return {
        success: true,
        link: response.data.link,
        expiresAt: response.data.expiresAt,
      };
    } catch (error: any) {
      console.error('[Export] Share link generation failed:', error);
      return {
        success: false,
        error: error.response?.data?.message || 'Failed to generate share link',
      };
    }
  }

  /**
   * Print itinerary (opens print dialog)
   */
  async print(itineraryId: string) {
    const result = await this.exportPdf(itineraryId);
    
    if (result.success && result.url) {
      const printWindow = window.open(result.url, '_blank');
      if (printWindow) {
        printWindow.onload = () => {
          printWindow.print();
        };
      }
      return { success: true };
    }

    return result;
  }
}

export const exportService = new ExportService();
