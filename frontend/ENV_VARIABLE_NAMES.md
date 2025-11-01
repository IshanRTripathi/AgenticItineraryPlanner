# Correct Environment Variable Names

**IMPORTANT**: The code uses specific variable names. Use these exact names in `.env`:

## ✅ CORRECT Variable Names (What the code expects)

### Google Maps
```bash
VITE_GOOGLE_MAPS_BROWSER_KEY=your-key-here
```
**Used in**: `src/hooks/useGoogleMaps.ts`

### Weather API
```bash
VITE_OPENWEATHER_API_KEY=your-key-here
```
**Used in**: `src/services/weatherService.ts`, `src/components/weather/WeatherWidget.tsx`

### API Base URL
```bash
VITE_API_BASE_URL=http://localhost:8080/api/v1
```
**Used in**: `src/services/apiClient.ts`, `src/services/api.ts`, `src/services/analytics.ts`

### WebSocket Base URL
```bash
VITE_WS_BASE_URL=http://localhost:8080/ws
```
**Used in**: `src/services/websocket.ts`, `src/services/api.ts`, `src/hooks/useStompWebSocket.ts`

### Firebase Configuration
```bash
VITE_FIREBASE_API_KEY=your-key-here
VITE_FIREBASE_AUTH_DOMAIN=tripaiplanner-4c951.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=tripaiplanner-4c951
VITE_FIREBASE_STORAGE_BUCKET=tripaiplanner-4c951.firebasestorage.app
VITE_FIREBASE_MESSAGING_SENDER_ID=your-sender-id-here
VITE_FIREBASE_APP_ID=your-app-id-here
```
**Used in**: `src/config/firebase.ts`

### Google Analytics
```bash
VITE_GA_TRACKING_ID=your-tracking-id-here
```
**Used in**: `src/services/analytics.ts`

### Development Settings
```bash
VITE_LOG_LEVEL=debug
```
**Used in**: `src/utils/logger.ts`

---

## ❌ WRONG Variable Names (Don't use these)

- ❌ `VITE_GOOGLE_MAPS_API_KEY` (code doesn't look for this)
- ❌ `VITE_GOOGLE_PLACES_API_KEY` (not used in code)
- ❌ `VITE_WEATHER_API_KEY` (code looks for OPENWEATHER)
- ❌ `VITE_WS_URL` (code looks for WS_BASE_URL)

---

## 🔍 How to Find Variable Names

If you're unsure what variable name to use:

```bash
# Search for environment variable usage in code
grep -r "import.meta.env.VITE_" frontend-redesign/src/
```

Or use the IDE search to find `import.meta.env.VITE_`

---

**Last Updated**: 2025-01-31  
**Status**: Variable names corrected in `.env` file
