/**
 * Translation Validator
 * Validates translation files for completeness and correctness
 */

import type { TranslationFile, LanguageCode, Namespace } from '../types';

interface ValidationResult {
  isValid: boolean;
  errors: string[];
  warnings: string[];
  missingKeys: string[];
}

/**
 * Validate a translation file against a reference (usually English)
 */
export function validateTranslationFile(
  file: TranslationFile,
  reference: TranslationFile,
  path: string = ''
): ValidationResult {
  const result: ValidationResult = {
    isValid: true,
    errors: [],
    warnings: [],
    missingKeys: []
  };

  // Check for missing keys
  for (const key in reference) {
    const currentPath = path ? `${path}.${key}` : key;
    const refValue = reference[key];
    const fileValue = file[key];

    if (fileValue === undefined) {
      result.missingKeys.push(currentPath);
      result.isValid = false;
      continue;
    }

    // If reference value is an object, recurse
    if (typeof refValue === 'object' && refValue !== null && !Array.isArray(refValue)) {
      if (typeof fileValue !== 'object' || fileValue === null || Array.isArray(fileValue)) {
        result.errors.push(`Type mismatch at ${currentPath}: expected object, got ${typeof fileValue}`);
        result.isValid = false;
      } else {
        const nestedResult = validateTranslationFile(
          fileValue as TranslationFile,
          refValue as TranslationFile,
          currentPath
        );
        result.errors.push(...nestedResult.errors);
        result.warnings.push(...nestedResult.warnings);
        result.missingKeys.push(...nestedResult.missingKeys);
        if (!nestedResult.isValid) {
          result.isValid = false;
        }
      }
    }
    // If reference value is a string, check file value is also a string
    else if (typeof refValue === 'string') {
      if (typeof fileValue !== 'string') {
        result.errors.push(`Type mismatch at ${currentPath}: expected string, got ${typeof fileValue}`);
        result.isValid = false;
      } else if (fileValue.trim() === '') {
        result.warnings.push(`Empty translation at ${currentPath}`);
      }
    }
  }

  // Check for extra keys (not in reference)
  for (const key in file) {
    if (reference[key] === undefined) {
      const currentPath = path ? `${path}.${key}` : key;
      result.warnings.push(`Extra key not in reference: ${currentPath}`);
    }
  }

  return result;
}

/**
 * Get all translation keys from a file (flattened)
 */
export function getAllKeys(file: TranslationFile, prefix: string = ''): string[] {
  const keys: string[] = [];

  for (const key in file) {
    const currentPath = prefix ? `${prefix}.${key}` : key;
    const value = file[key];

    if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
      keys.push(...getAllKeys(value as TranslationFile, currentPath));
    } else if (typeof value === 'string') {
      keys.push(currentPath);
    }
  }

  return keys;
}

/**
 * Check if a translation file is empty
 */
export function isEmptyTranslationFile(file: TranslationFile): boolean {
  return Object.keys(file).length === 0;
}

/**
 * Log validation results
 */
export function logValidationResults(
  language: LanguageCode,
  namespace: Namespace,
  result: ValidationResult
): void {
  if (result.isValid && result.warnings.length === 0) {
    console.log(`[Validation] ✓ ${language}/${namespace} is valid`);
    return;
  }

  console.group(`[Validation] ${language}/${namespace}`);

  if (result.errors.length > 0) {
    console.error('Errors:', result.errors);
  }

  if (result.missingKeys.length > 0) {
    console.warn('Missing keys:', result.missingKeys);
  }

  if (result.warnings.length > 0) {
    console.warn('Warnings:', result.warnings);
  }

  console.groupEnd();
}
