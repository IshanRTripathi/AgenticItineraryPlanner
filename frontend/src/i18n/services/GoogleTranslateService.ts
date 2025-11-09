/**
 * Google Translate Service
 * Handles translation of dynamic content using Google Cloud Translation API
 */

import type { LanguageCode, GoogleTranslateResponse } from '../types';
import { API_CONFIG } from '../types';

export class GoogleTranslateService {
  private apiKey: string;
  private endpoint: string;
  private requestQueue: Array<{ text: string; resolve: (value: string) => void; reject: (error: Error) => void }> = [];
  private isProcessing: boolean = false;
  private rateLimitCount: number = 0;
  private rateLimitResetTime: number = Date.now() + 60000; // 1 minute from now

  constructor(apiKey?: string) {
    this.apiKey = apiKey || import.meta.env.VITE_GOOGLE_TRANSLATE_API_KEY || '';
    this.endpoint = API_CONFIG.GOOGLE_TRANSLATE_ENDPOINT;

    if (!this.apiKey) {
      console.warn('[GoogleTranslateService] API key not configured. Dynamic translation will not work.');
    }
  }

  /**
   * Check if service is configured
   */
  isConfigured(): boolean {
    return !!this.apiKey;
  }

  /**
   * Translate a single text
   */
  async translate(
    text: string,
    targetLanguage: LanguageCode,
    sourceLanguage: LanguageCode = 'en'
  ): Promise<string> {
    if (!this.isConfigured()) {
      console.warn('[GoogleTranslateService] API key not configured, returning original text');
      return text;
    }

    if (!text || text.trim() === '') {
      return text;
    }

    // If target is same as source, return original
    if (targetLanguage === sourceLanguage) {
      return text;
    }

    try {
      // Check rate limit
      this.checkRateLimit();

      const response = await this.makeRequest([text], targetLanguage, sourceLanguage);
      
      if (response.data?.translations?.[0]?.translatedText) {
        return response.data.translations[0].translatedText;
      }

      console.warn('[GoogleTranslateService] No translation in response');
      return text;
    } catch (error) {
      console.error('[GoogleTranslateService] Translation failed:', error);
      return text; // Return original text on error
    }
  }

  /**
   * Translate multiple texts in a batch
   */
  async translateBatch(
    texts: string[],
    targetLanguage: LanguageCode,
    sourceLanguage: LanguageCode = 'en'
  ): Promise<string[]> {
    if (!this.isConfigured()) {
      console.warn('[GoogleTranslateService] API key not configured, returning original texts');
      return texts;
    }

    if (texts.length === 0) {
      return [];
    }

    // If target is same as source, return originals
    if (targetLanguage === sourceLanguage) {
      return texts;
    }

    try {
      // Check rate limit
      this.checkRateLimit();

      // Split into batches if needed
      const batches: string[][] = [];
      for (let i = 0; i < texts.length; i += API_CONFIG.MAX_BATCH_SIZE) {
        batches.push(texts.slice(i, i + API_CONFIG.MAX_BATCH_SIZE));
      }

      // Process all batches
      const results: string[] = [];
      for (const batch of batches) {
        const response = await this.makeRequest(batch, targetLanguage, sourceLanguage);
        
        if (response.data?.translations) {
          results.push(...response.data.translations.map(t => t.translatedText));
        } else {
          // If batch fails, return original texts
          results.push(...batch);
        }
      }

      return results;
    } catch (error) {
      console.error('[GoogleTranslateService] Batch translation failed:', error);
      return texts; // Return original texts on error
    }
  }

  /**
   * Make API request to Google Translate
   */
  private async makeRequest(
    texts: string[],
    targetLanguage: LanguageCode,
    sourceLanguage: LanguageCode
  ): Promise<GoogleTranslateResponse> {
    const url = `${this.endpoint}?key=${this.apiKey}`;

    const body = {
      q: texts,
      target: targetLanguage,
      source: sourceLanguage,
      format: 'text'
    };

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body)
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Google Translate API error: ${response.status} - ${errorText}`);
    }

    // Increment rate limit counter
    this.rateLimitCount++;

    return await response.json();
  }

  /**
   * Check and enforce rate limiting
   */
  private checkRateLimit(): void {
    const now = Date.now();

    // Reset counter if time window has passed
    if (now >= this.rateLimitResetTime) {
      this.rateLimitCount = 0;
      this.rateLimitResetTime = now + 60000; // Next minute
    }

    // Check if we've exceeded the limit
    if (this.rateLimitCount >= API_CONFIG.RATE_LIMIT_PER_MINUTE) {
      const waitTime = this.rateLimitResetTime - now;
      throw new Error(`Rate limit exceeded. Please wait ${Math.ceil(waitTime / 1000)} seconds.`);
    }
  }

  /**
   * Detect if text is a proper noun (simple heuristic)
   * Proper nouns typically start with capital letter and are short
   */
  isProperNoun(text: string): boolean {
    if (!text || text.length === 0) return false;

    // Check if it's a single word starting with capital
    const words = text.trim().split(/\s+/);
    
    // If it's a single capitalized word, likely a proper noun
    if (words.length === 1 && /^[A-Z]/.test(text)) {
      return true;
    }

    // If all words are capitalized, likely a proper noun (e.g., "New York")
    if (words.every(word => /^[A-Z]/.test(word))) {
      return true;
    }

    return false;
  }

  /**
   * Get current rate limit status
   */
  getRateLimitStatus(): { count: number; limit: number; resetIn: number } {
    const now = Date.now();
    const resetIn = Math.max(0, this.rateLimitResetTime - now);

    return {
      count: this.rateLimitCount,
      limit: API_CONFIG.RATE_LIMIT_PER_MINUTE,
      resetIn: Math.ceil(resetIn / 1000) // seconds
    };
  }
}
