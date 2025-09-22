/**
 * Utility functions for handling UTF-8 encoding and special characters
 */

/**
 * Normalize text to handle special characters properly
 * Converts special characters to their closest ASCII equivalents
 */
export function normalizeText(text: string): string {
  if (!text) return text;
  
  // Create a mapping of common special characters to ASCII equivalents
  const charMap: { [key: string]: string } = {
    'ü': 'u',
    'Ü': 'U',
    'ö': 'o',
    'Ö': 'O',
    'ä': 'a',
    'Ä': 'A',
    'ß': 'ss',
    'ñ': 'n',
    'Ñ': 'N',
    'ç': 'c',
    'Ç': 'C',
    'é': 'e',
    'É': 'E',
    'è': 'e',
    'È': 'E',
    'ê': 'e',
    'Ê': 'E',
    'ë': 'e',
    'Ë': 'E',
    'à': 'a',
    'À': 'A',
    'á': 'a',
    'Á': 'A',
    'â': 'a',
    'Â': 'A',
    'ã': 'a',
    'Ã': 'A',
    'í': 'i',
    'Í': 'I',
    'ì': 'i',
    'Ì': 'I',
    'î': 'i',
    'Î': 'I',
    'ï': 'i',
    'Ï': 'I',
    'ó': 'o',
    'Ó': 'O',
    'ò': 'o',
    'Ò': 'O',
    'ô': 'o',
    'Ô': 'O',
    'õ': 'o',
    'Õ': 'O',
    'ú': 'u',
    'Ú': 'U',
    'ù': 'u',
    'Ù': 'U',
    'û': 'u',
    'Û': 'U',
    'ý': 'y',
    'Ý': 'Y',
    'ÿ': 'y',
    'Ÿ': 'Y'
  };
  
  return text.replace(/[^\x00-\x7F]/g, (char) => {
    return charMap[char] || char;
  });
}

/**
 * Safely encode text for JSON transmission
 * Ensures proper UTF-8 encoding
 */
export function safeJsonEncode(obj: any): string {
  try {
    return JSON.stringify(obj);
  } catch (error) {
    console.warn('JSON encoding failed, attempting to normalize text:', error);
    
    // If JSON.stringify fails, try to normalize the object recursively
    const normalizedObj = normalizeObject(obj);
    return JSON.stringify(normalizedObj);
  }
}

/**
 * Recursively normalize all string values in an object
 */
function normalizeObject(obj: any): any {
  if (typeof obj === 'string') {
    return normalizeText(obj);
  } else if (Array.isArray(obj)) {
    return obj.map(normalizeObject);
  } else if (obj && typeof obj === 'object') {
    const normalized: any = {};
    for (const [key, value] of Object.entries(obj)) {
      normalized[key] = normalizeObject(value);
    }
    return normalized;
  }
  return obj;
}

/**
 * Check if a string contains non-ASCII characters
 */
export function hasSpecialCharacters(text: string): boolean {
  return /[^\x00-\x7F]/.test(text);
}

/**
 * Get a display-friendly version of text with special characters
 * Shows both original and normalized versions
 */
export function getDisplayText(text: string): { original: string; normalized: string; hasSpecial: boolean } {
  const normalized = normalizeText(text);
  const hasSpecial = hasSpecialCharacters(text);
  
  return {
    original: text,
    normalized,
    hasSpecial
  };
}
