/**
 * Dynamic Translator
 * Handles translation of dynamic content from backend using Google Translate API
 * Includes caching, batching, and proper noun detection
 */

import { GoogleTranslateService } from './GoogleTranslateService';
import { TranslationCache } from './TranslationCache';
import type { LanguageCode, DynamicTranslateOptions } from '../types';

export class DynamicTranslator {
  private googleTranslate: GoogleTranslateService;
  private cache: TranslationCache;
  private pendingTranslations: Map<string, Promise<string>>;

  constructor(apiKey?: string) {
    this.googleTranslate = new GoogleTranslateService(apiKey);
    this.cache = new TranslationCache();
    this.pendingTranslations = new Map();
  }

  /**
   * Translate a single text
   */
  async translate(text: string, options: DynamicTranslateOptions): Promise<string> {
    // Validate input
    if (!text || text.trim() === '') {
      return text;
    }

    const {
      from = 'en',
      to,
      cache: useCache = true,
      skipIfProperNoun = false
    } = options;

    // If source and target are the same, return original
    if (from === to) {
      return text;
    }

    // Check if it's a proper noun and should be skipped
    if (skipIfProperNoun && this.googleTranslate.isProperNoun(text)) {
      console.log(`[DynamicTranslator] Skipping proper noun: ${text}`);
      return text;
    }

    // Check cache first
    if (useCache) {
      const cached = this.cache.get(text, to, from);
      if (cached) {
        return cached;
      }
    }

    // Check if translation is already pending (deduplication)
    const pendingKey = `${from}:${to}:${text}`;
    if (this.pendingTranslations.has(pendingKey)) {
      return this.pendingTranslations.get(pendingKey)!;
    }

    // Create translation promise
    const translationPromise = this.performTranslation(text, to, from, useCache);
    
    // Store pending promise
    this.pendingTranslations.set(pendingKey, translationPromise);

    try {
      const result = await translationPromise;
      return result;
    } finally {
      // Remove from pending
      this.pendingTranslations.delete(pendingKey);
    }
  }

  /**
   * Perform the actual translation
   */
  private async performTranslation(
    text: string,
    targetLang: LanguageCode,
    sourceLang: LanguageCode,
    useCache: boolean
  ): Promise<string> {
    try {
      // Check if service is configured
      if (!this.googleTranslate.isConfigured()) {
        console.warn('[DynamicTranslator] Google Translate not configured');
        return text;
      }

      // Translate
      const translated = await this.googleTranslate.translate(text, targetLang, sourceLang);

      // Cache the result
      if (useCache && translated !== text) {
        this.cache.set(text, translated, targetLang, sourceLang);
      }

      return translated;
    } catch (error) {
      console.error('[DynamicTranslator] Translation failed:', error);
      return text; // Return original on error
    }
  }

  /**
   * Translate multiple texts in batch
   */
  async translateBatch(texts: string[], options: DynamicTranslateOptions): Promise<string[]> {
    if (texts.length === 0) {
      return [];
    }

    const {
      from = 'en',
      to,
      cache: useCache = true,
      skipIfProperNoun = false
    } = options;

    // If source and target are the same, return originals
    if (from === to) {
      return texts;
    }

    const results: string[] = [];
    const textsToTranslate: string[] = [];
    const indicesToTranslate: number[] = [];

    // Check cache and filter texts
    for (let i = 0; i < texts.length; i++) {
      const text = texts[i];

      // Empty text
      if (!text || text.trim() === '') {
        results[i] = text;
        continue;
      }

      // Proper noun check
      if (skipIfProperNoun && this.googleTranslate.isProperNoun(text)) {
        results[i] = text;
        continue;
      }

      // Check cache
      if (useCache) {
        const cached = this.cache.get(text, to, from);
        if (cached) {
          results[i] = cached;
          continue;
        }
      }

      // Need to translate
      textsToTranslate.push(text);
      indicesToTranslate.push(i);
    }

    // If nothing to translate, return results
    if (textsToTranslate.length === 0) {
      return results;
    }

    try {
      // Check if service is configured
      if (!this.googleTranslate.isConfigured()) {
        console.warn('[DynamicTranslator] Google Translate not configured');
        // Fill remaining with originals
        indicesToTranslate.forEach((index, i) => {
          results[index] = textsToTranslate[i];
        });
        return results;
      }

      // Translate batch
      const translated = await this.googleTranslate.translateBatch(textsToTranslate, to, from);

      // Store results and cache
      translated.forEach((translatedText, i) => {
        const index = indicesToTranslate[i];
        const originalText = textsToTranslate[i];
        
        results[index] = translatedText;

        // Cache if different from original
        if (useCache && translatedText !== originalText) {
          this.cache.set(originalText, translatedText, to, from);
        }
      });

      return results;
    } catch (error) {
      console.error('[DynamicTranslator] Batch translation failed:', error);
      
      // Fill remaining with originals on error
      indicesToTranslate.forEach((index, i) => {
        results[index] = textsToTranslate[i];
      });
      
      return results;
    }
  }

  /**
   * Clear translation cache
   */
  clearCache(): void {
    this.cache.clear();
  }

  /**
   * Get cache statistics
   */
  getCacheStats(): { hits: number; misses: number; size: number; hitRate: number } {
    return this.cache.getStats();
  }

  /**
   * Get rate limit status
   */
  getRateLimitStatus(): { count: number; limit: number; resetIn: number } {
    return this.googleTranslate.getRateLimitStatus();
  }

  /**
   * Check if service is configured
   */
  isConfigured(): boolean {
    return this.googleTranslate.isConfigured();
  }
}
