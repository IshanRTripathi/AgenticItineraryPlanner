/**
 * Translation Cache
 * Multi-level caching for translated content (memory + sessionStorage)
 */

import type { LanguageCode, CacheEntry } from '../types';
import { API_CONFIG, STORAGE_KEYS } from '../types';

export class TranslationCache {
  private memoryCache: Map<string, CacheEntry>;
  private maxMemorySize: number = 1000; // Max entries in memory
  private hits: number = 0;
  private misses: number = 0;

  constructor() {
    this.memoryCache = new Map();
    this.loadFromSessionStorage();
  }

  /**
   * Generate cache key
   */
  private getCacheKey(text: string, targetLang: LanguageCode, sourceLang: LanguageCode = 'en'): string {
    // Use a simple hash-like key
    return `${sourceLang}:${targetLang}:${text.substring(0, 100)}`;
  }

  /**
   * Get cached translation
   */
  get(text: string, targetLang: LanguageCode, sourceLang: LanguageCode = 'en'): string | null {
    const key = this.getCacheKey(text, targetLang, sourceLang);

    // Check memory cache first
    const memoryEntry = this.memoryCache.get(key);
    if (memoryEntry) {
      // Check if entry is still valid (not expired)
      if (Date.now() - memoryEntry.timestamp < API_CONFIG.CACHE_TTL) {
        this.hits++;
        return memoryEntry.value;
      } else {
        // Remove expired entry
        this.memoryCache.delete(key);
      }
    }

    // Check session storage
    try {
      const sessionData = sessionStorage.getItem(`${STORAGE_KEYS.TRANSLATION_CACHE}:${key}`);
      if (sessionData) {
        const entry: CacheEntry = JSON.parse(sessionData);
        
        // Check if entry is still valid
        if (Date.now() - entry.timestamp < API_CONFIG.CACHE_TTL) {
          // Restore to memory cache
          this.memoryCache.set(key, entry);
          this.hits++;
          return entry.value;
        } else {
          // Remove expired entry
          sessionStorage.removeItem(`${STORAGE_KEYS.TRANSLATION_CACHE}:${key}`);
        }
      }
    } catch (error) {
      console.warn('[TranslationCache] Error reading from sessionStorage:', error);
    }

    this.misses++;
    return null;
  }

  /**
   * Set cached translation
   */
  set(text: string, translatedText: string, targetLang: LanguageCode, sourceLang: LanguageCode = 'en'): void {
    const key = this.getCacheKey(text, targetLang, sourceLang);
    const entry: CacheEntry = {
      value: translatedText,
      timestamp: Date.now(),
      language: targetLang
    };

    // Add to memory cache
    this.memoryCache.set(key, entry);

    // Enforce memory cache size limit
    if (this.memoryCache.size > this.maxMemorySize) {
      // Remove oldest entries (first entries in Map)
      const entriesToRemove = this.memoryCache.size - this.maxMemorySize;
      const keys = Array.from(this.memoryCache.keys());
      for (let i = 0; i < entriesToRemove; i++) {
        this.memoryCache.delete(keys[i]);
      }
    }

    // Add to session storage
    try {
      sessionStorage.setItem(
        `${STORAGE_KEYS.TRANSLATION_CACHE}:${key}`,
        JSON.stringify(entry)
      );
    } catch (error) {
      // Session storage might be full or disabled
      console.warn('[TranslationCache] Error writing to sessionStorage:', error);
    }
  }

  /**
   * Check if translation is cached
   */
  has(text: string, targetLang: LanguageCode, sourceLang: LanguageCode = 'en'): boolean {
    return this.get(text, targetLang, sourceLang) !== null;
  }

  /**
   * Clear all caches
   */
  clear(): void {
    this.memoryCache.clear();
    
    try {
      // Clear translation cache entries from sessionStorage
      const keys = Object.keys(sessionStorage);
      keys.forEach(key => {
        if (key.startsWith(STORAGE_KEYS.TRANSLATION_CACHE)) {
          sessionStorage.removeItem(key);
        }
      });
    } catch (error) {
      console.warn('[TranslationCache] Error clearing sessionStorage:', error);
    }

    this.hits = 0;
    this.misses = 0;
  }

  /**
   * Get cache size
   */
  size(): number {
    return this.memoryCache.size;
  }

  /**
   * Get cache statistics
   */
  getStats(): { hits: number; misses: number; size: number; hitRate: number } {
    const total = this.hits + this.misses;
    const hitRate = total > 0 ? (this.hits / total) * 100 : 0;

    return {
      hits: this.hits,
      misses: this.misses,
      size: this.memoryCache.size,
      hitRate: Math.round(hitRate * 100) / 100 // Round to 2 decimal places
    };
  }

  /**
   * Load cache from sessionStorage on initialization
   */
  private loadFromSessionStorage(): void {
    try {
      const keys = Object.keys(sessionStorage);
      const cacheKeys = keys.filter(key => key.startsWith(STORAGE_KEYS.TRANSLATION_CACHE));

      for (const key of cacheKeys) {
        const data = sessionStorage.getItem(key);
        if (data) {
          try {
            const entry: CacheEntry = JSON.parse(data);
            
            // Only load if not expired
            if (Date.now() - entry.timestamp < API_CONFIG.CACHE_TTL) {
              const cacheKey = key.replace(`${STORAGE_KEYS.TRANSLATION_CACHE}:`, '');
              this.memoryCache.set(cacheKey, entry);
            } else {
              // Remove expired entry
              sessionStorage.removeItem(key);
            }
          } catch (error) {
            // Invalid entry, remove it
            sessionStorage.removeItem(key);
          }
        }
      }

      console.log(`[TranslationCache] Loaded ${this.memoryCache.size} entries from sessionStorage`);
    } catch (error) {
      console.warn('[TranslationCache] Error loading from sessionStorage:', error);
    }
  }

  /**
   * Clear expired entries
   */
  clearExpired(): void {
    const now = Date.now();
    const expiredKeys: string[] = [];

    // Check memory cache
    this.memoryCache.forEach((entry, key) => {
      if (now - entry.timestamp >= API_CONFIG.CACHE_TTL) {
        expiredKeys.push(key);
      }
    });

    // Remove expired entries
    expiredKeys.forEach(key => this.memoryCache.delete(key));

    // Check session storage
    try {
      const keys = Object.keys(sessionStorage);
      keys.forEach(key => {
        if (key.startsWith(STORAGE_KEYS.TRANSLATION_CACHE)) {
          const data = sessionStorage.getItem(key);
          if (data) {
            try {
              const entry: CacheEntry = JSON.parse(data);
              if (now - entry.timestamp >= API_CONFIG.CACHE_TTL) {
                sessionStorage.removeItem(key);
              }
            } catch (error) {
              // Invalid entry, remove it
              sessionStorage.removeItem(key);
            }
          }
        }
      });
    } catch (error) {
      console.warn('[TranslationCache] Error clearing expired entries from sessionStorage:', error);
    }

    if (expiredKeys.length > 0) {
      console.log(`[TranslationCache] Cleared ${expiredKeys.length} expired entries`);
    }
  }
}
