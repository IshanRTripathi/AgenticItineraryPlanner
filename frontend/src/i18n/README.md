# i18n Translation Module

## Overview

This module provides internationalization (i18n) support for the EasyTrip application with minimal component changes required.

## Architecture

```
i18n/
├── index.ts                    # Central export point
├── types.ts                    # TypeScript types and constants
├── TranslationProvider.tsx     # Root provider component
├── context/
│   └── TranslationContext.tsx  # React context
├── locales/
│   ├── en/                     # English translations
│   ├── hi/                     # Hindi translations
│   └── es/                     # Spanish translations
└── utils/
    ├── languageDetector.ts     # Browser language detection
    └── translationValidator.ts # Translation file validation
```

## Usage

### 1. Provider Setup (Already Done)

The `TranslationProvider` is already wrapped around the app in `main.tsx`:

```typescript
<TranslationProvider>
  <AuthProvider>
    <App />
  </AuthProvider>
</TranslationProvider>
```

### 2. Using Translations in Components

#### Basic Usage

```typescript
import { useTranslation } from '@/i18n/hooks/useTranslation';

function MyComponent() {
  const { t } = useTranslation('common');
  
  return (
    <button>{t('actions.save')}</button>
  );
}
```

#### With Parameters (Interpolation)

```typescript
const { t } = useTranslation('pages');

<p>{t('dashboard.welcome', { name: user.name })}</p>
// Output: "Welcome, John!"
```

#### With Pluralization

```typescript
const { t } = useTranslation('components');

<span>{t('tripCard.travelers', { count: travelers }, { count: travelers })}</span>
// Output: "1 traveler" or "2 travelers"
```

#### With Default Value

```typescript
const { t } = useTranslation('errors');

<p>{t('unknown.error', {}, { defaultValue: 'An error occurred' })}</p>
```

### 3. Language Switching

```typescript
import { useTranslation } from '@/i18n/hooks/useTranslation';

function LanguageSwitcher() {
  const { language, setLanguage } = useTranslation();
  
  return (
    <select value={language} onChange={(e) => setLanguage(e.target.value)}>
      <option value="en">English</option>
      <option value="hi">हिन्दी</option>
      <option value="es">Español</option>
    </select>
  );
}
```

### 4. Dynamic Translation (Coming in Phase 3)

```typescript
const { td } = useTranslation();

// Translate dynamic content from backend
const translatedText = await td(backendText, {
  to: language,
  cache: true
});
```

## Translation File Structure

### Namespace Organization

- **common.json** - Shared UI elements (buttons, labels, status)
- **pages.json** - Page-specific text
- **components.json** - Component-specific text
- **errors.json** - Error messages
- **validation.json** - Form validation messages

### Key Naming Convention

Use dot notation for nested keys:

```json
{
  "dashboard": {
    "title": "My Trips",
    "filters": {
      "all": "All Trips",
      "upcoming": "Upcoming"
    }
  }
}
```

Access as: `t('pages.dashboard.title')`

### Interpolation

Use `{{paramName}}` for dynamic values:

```json
{
  "welcome": "Welcome, {{name}}!"
}
```

Usage: `t('welcome', { name: 'John' })`

### Pluralization

Add `_plural` suffix for plural forms:

```json
{
  "travelers": "{{count}} traveler",
  "travelers_plural": "{{count}} travelers"
}
```

Usage: `t('travelers', { count: 2 }, { count: 2 })`

## Adding New Translations

### 1. Add to English (en) First

```json
// locales/en/common.json
{
  "actions": {
    "newAction": "New Action"
  }
}
```

### 2. Add to Other Languages

```json
// locales/hi/common.json
{
  "actions": {
    "newAction": "नई कार्रवाई"
  }
}
```

### 3. Use in Component

```typescript
const { t } = useTranslation('common');
<button>{t('actions.newAction')}</button>
```

## Best Practices

### ✅ DO

- Use descriptive key names: `dashboard.filters.upcoming` not `df.u`
- Group related translations together
- Keep translations in appropriate namespaces
- Use interpolation for dynamic values
- Provide context in comments for translators
- Test with all supported languages

### ❌ DON'T

- Hardcode text in components
- Use translation keys as user-facing text
- Nest too deeply (max 3-4 levels)
- Duplicate translations across files
- Leave empty translation values
- Assume English word order works for all languages

## Language Detection

The system automatically detects language in this order:

1. **Saved preference** (localStorage)
2. **Browser language** (navigator.languages)
3. **Default language** (English)

## Performance

- Translations are loaded once on app start
- All namespaces are preloaded (5 small JSON files)
- Translations are cached in memory
- Language switching is instant (no reload)
- Bundle size impact: ~50KB per language

## Debugging

### Development Mode

In development, the system logs:
- Missing translation keys
- Namespace loading
- Language changes
- Translation errors

### Check Current Language

```typescript
const { language } = useTranslation();
console.log('Current language:', language);
```

### Check Loaded Translations

```typescript
const { translations } = useTranslation();
console.log('Loaded translations:', translations);
```

## Troubleshooting

### Translation Not Showing

1. Check the key exists in the JSON file
2. Verify the namespace is correct
3. Check browser console for warnings
4. Ensure TranslationProvider is wrapping your component

### Language Not Changing

1. Check localStorage for saved language
2. Verify language code is supported ('en', 'hi', 'es')
3. Check browser console for errors
4. Clear localStorage and refresh

### Missing Translations

1. Check if the key exists in English
2. Verify the translation file is valid JSON
3. Check for typos in the key path
4. Ensure the file is in the correct directory

## Future Enhancements

- [ ] Lazy loading of namespaces
- [ ] Translation management UI
- [ ] Automatic missing key detection
- [ ] Translation coverage reports
- [ ] RTL language support
- [ ] Context-aware translations
- [ ] Translation memory

## Support

For issues or questions:
1. Check this README
2. Review the [Design Document](../../.kiro/specs/i18n-integration/design.md)
3. Check browser console for errors
4. Review translation JSON files for syntax errors
