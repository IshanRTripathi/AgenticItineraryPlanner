/**
 * Preview Cache
 * Caches change previews for performance optimization
 */

interface CacheEntry {
  changeSet: any;
  diff: any;
  timestamp: number;
}

const CACHE_TTL = 5 * 60 * 1000; // 5 minutes
const MAX_CACHE_SIZE = 50;

class PreviewCacheManager {
  private cache: Map<string, CacheEntry> = new Map();

  /**
   * Generate cache key from change set
   */
  private generateKey(changeSet: any): string {
    return JSON.stringify(changeSet);
  }

  /**
   * Check if cache entry is still valid
   */
  private isValid(entry: CacheEntry): boolean {
    return Date.now() - entry.timestamp < CACHE_TTL;
  }

  /**
   * Clean up expired entries
   */
  private cleanup(): void {
    const now = Date.now();
    for (const [key, entry] of this.cache.entries()) {
      if (now - entry.timestamp >= CACHE_TTL) {
        this.cache.delete(key);
      }
    }
  }

  /**
   * Enforce cache size limit
   */
  private enforceSizeLimit(): void {
    if (this.cache.size > MAX_CACHE_SIZE) {
      // Remove oldest entries
      const entries = Array.from(this.cache.entries());
      entries.sort((a, b) => a[1].timestamp - b[1].timestamp);
      
      const toRemove = entries.slice(0, this.cache.size - MAX_CACHE_SIZE);
      toRemove.forEach(([key]) => this.cache.delete(key));
    }
  }

  /**
   * Get cached preview
   */
  get(changeSet: any): { changeSet: any; diff: any } | null {
    this.cleanup();
    
    const key = this.generateKey(changeSet);
    const entry = this.cache.get(key);
    
    if (entry && this.isValid(entry)) {
      return {
        changeSet: entry.changeSet,
        diff: entry.diff,
      };
    }
    
    return null;
  }

  /**
   * Set cached preview
   */
  set(changeSet: any, diff: any): void {
    const key = this.generateKey(changeSet);
    
    this.cache.set(key, {
      changeSet,
      diff,
      timestamp: Date.now(),
    });
    
    this.enforceSizeLimit();
  }

  /**
   * Clear all cached previews
   */
  clear(): void {
    this.cache.clear();
  }

  /**
   * Get cache statistics
   */
  getStats(): { size: number; maxSize: number; ttl: number } {
    this.cleanup();
    return {
      size: this.cache.size,
      maxSize: MAX_CACHE_SIZE,
      ttl: CACHE_TTL,
    };
  }
}

export const PreviewCache = new PreviewCacheManager();
