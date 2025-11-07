# Frontend Redesign - Configuration Guide

**Purpose**: Complete guide for setting up environment variables  
**Last Updated**: 2025-01-27

---

## 🚀 Quick Setup (5 minutes)

### 1. Copy Environment File
```bash
cd frontend-redesign
cp .env.example .env
```

### 2. Update Critical Variables
Open `.env` and update these **required** variables:

```bash
# CRITICAL - Map functionality won't work without this
VITE_GOOGLE_MAPS_API_KEY=your-actual-api-key-here

# CRITICAL - Authentication won't work without these
VITE_FIREBASE_API_KEY=your-actual-api-key-here
VITE_FIREBASE_MESSAGING_SENDER_ID=your-actual-sender-id-here
VITE_FIREBASE_APP_ID=your-actual-app-id-here
```

### 3. Verify Configuration
```bash
npm run dev
```

Visit http://localhost:5173 and check:
- ✅ Map component loads (not showing "API key not configured")
- ✅ Authentication works
- ✅ API calls succeed

---

## 📋 Environment Variables Reference

### Backend Configuration (Server-side only)

These variables are **NOT** exposed to the client (no `VITE_` prefix):

```bash
# Firestore
FIRESTORE_USE_EMULATOR=false          # Use local emulator?
FIRESTORE_EMULATOR_HOST=localhost:8080
GCP_PROJECT_ID=tripaiplanner
FIRESTORE_PROJECT_ID=tripaiplanner-4c951
FIRESTORE_CREDENTIALS_FILE=firestore_credentials.json

# Database
APP_DATABASE_TYPE=firestore           # Database type

# Google AI (Gemini)
GEMINI_API_KEY=your-key-here          # Backend AI processing
GEMINI_MOCK_MODE=false                # Use mock responses?

# Firebase Auth
FIREBASE_AUTH_ENABLED=true            # Enable authentication?
```

### Frontend Configuration (Client-side)

These variables **ARE** exposed to the client (have `VITE_` prefix):

#### Application URLs
```bash
VITE_FRONTEND_URL=http://localhost:5173    # Frontend dev server
VITE_APP_BASE_URL=http://localhost:8080    # Backend base URL
VITE_API_BASE_URL=http://localhost:8080/api/v1  # API endpoint
VITE_WS_URL=ws://localhost:8080            # WebSocket URL
```

#### Google Maps API (CRITICAL)
```bash
VITE_GOOGLE_MAPS_API_KEY=your-key-here     # For map component
VITE_GOOGLE_PLACES_API_KEY=your-key-here   # For places autocomplete
```

**How to get**:
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Enable "Maps JavaScript API" and "Places API"
3. Create API key
4. Add key to `.env`

**Why critical**: Map component shows "API key not configured" without this

#### Firebase Configuration (CRITICAL)
```bash
VITE_FIREBASE_API_KEY=your-key-here
VITE_FIREBASE_AUTH_DOMAIN=tripaiplanner-4c951.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=tripaiplanner-4c951
VITE_FIREBASE_STORAGE_BUCKET=tripaiplanner-4c951.appspot.com
VITE_FIREBASE_MESSAGING_SENDER_ID=your-sender-id-here
VITE_FIREBASE_APP_ID=your-app-id-here
```

**How to get**:
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select project: `tripaiplanner-4c951`
3. Go to Project Settings → General
4. Scroll to "Your apps" → Web app
5. Copy configuration values
6. Add to `.env`

**Why critical**: Authentication won't work without these

#### Google Analytics (Optional)
```bash
VITE_GA_TRACKING_ID=                       # Google Analytics 4 tracking ID
```

**How to get**:
1. Go to [Google Analytics](https://analytics.google.com/)
2. Create GA4 property
3. Get Measurement ID (G-XXXXXXXXXX)
4. Add to `.env`

**When needed**: For production analytics tracking

#### Weather API (Optional)
```bash
VITE_WEATHER_API_KEY=                      # OpenWeather API key
```

**How to get**:
1. Go to [OpenWeather](https://openweathermap.org/api)
2. Sign up for free account
3. Get API key
4. Add to `.env`

**When needed**: For real weather data (currently uses mock data)

#### Feature Flags
```bash
VITE_ENABLE_ANALYTICS=false                # Enable analytics tracking?
VITE_ENABLE_PWA=false                      # Enable PWA features?
VITE_ENABLE_MOCK_DATA=false                # Use mock data instead of API?
```

#### Development Settings
```bash
VITE_DEV_MODE=true                         # Development mode?
VITE_LOG_LEVEL=debug                       # Logging level (debug, info, warn, error)
```

---

## 🔧 Configuration by Environment

### Development (Local)
```bash
VITE_FRONTEND_URL=http://localhost:5173
VITE_APP_BASE_URL=http://localhost:8080
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_WS_URL=ws://localhost:8080
VITE_DEV_MODE=true
VITE_LOG_LEVEL=debug
VITE_ENABLE_MOCK_DATA=false
```

### Staging
```bash
VITE_FRONTEND_URL=https://staging.tripaiplanner.com
VITE_APP_BASE_URL=https://api-staging.tripaiplanner.com
VITE_API_BASE_URL=https://api-staging.tripaiplanner.com/api/v1
VITE_WS_URL=wss://api-staging.tripaiplanner.com
VITE_DEV_MODE=false
VITE_LOG_LEVEL=info
VITE_ENABLE_ANALYTICS=true
```

### Production
```bash
VITE_FRONTEND_URL=https://tripaiplanner.com
VITE_APP_BASE_URL=https://api.tripaiplanner.com
VITE_API_BASE_URL=https://api.tripaiplanner.com/api/v1
VITE_WS_URL=wss://api.tripaiplanner.com
VITE_DEV_MODE=false
VITE_LOG_LEVEL=error
VITE_ENABLE_ANALYTICS=true
VITE_ENABLE_PWA=true
```

---

## 🐛 Troubleshooting

### Map shows "API key not configured"
**Problem**: `VITE_GOOGLE_MAPS_API_KEY` is missing or invalid

**Solution**:
1. Check `.env` file has `VITE_GOOGLE_MAPS_API_KEY=your-key-here`
2. Verify API key is valid in Google Cloud Console
3. Ensure "Maps JavaScript API" is enabled
4. Restart dev server: `npm run dev`

### Authentication fails
**Problem**: Firebase configuration is missing or invalid

**Solution**:
1. Check all `VITE_FIREBASE_*` variables are set in `.env`
2. Verify values match Firebase Console
3. Ensure Firebase Authentication is enabled in console
4. Restart dev server: `npm run dev`

### API calls fail with CORS error
**Problem**: Backend URL is incorrect or backend is not running

**Solution**:
1. Check `VITE_API_BASE_URL` points to running backend
2. Verify backend is running: `curl http://localhost:8080/api/v1/health`
3. Check CORS configuration in backend allows frontend URL

### WebSocket connection fails
**Problem**: WebSocket URL is incorrect or backend WebSocket is not enabled

**Solution**:
1. Check `VITE_WS_URL` is correct (ws:// for local, wss:// for production)
2. Verify backend WebSocket endpoint is running
3. Check firewall/proxy allows WebSocket connections

### Environment variables not updating
**Problem**: Vite caches environment variables

**Solution**:
1. Stop dev server (Ctrl+C)
2. Delete `.vite` cache folder: `rm -rf node_modules/.vite`
3. Restart dev server: `npm run dev`

---

## 🔒 Security Best Practices

### DO NOT commit `.env` file
- ✅ `.env` is in `.gitignore`
- ✅ Use `.env.example` as template
- ❌ Never commit API keys to git

### Rotate API keys regularly
- Change keys every 90 days
- Use different keys for dev/staging/production
- Revoke old keys after rotation

### Restrict API key usage
**Google Maps API**:
- Restrict to specific domains (e.g., tripaiplanner.com)
- Restrict to specific APIs (Maps JavaScript API, Places API)
- Set usage quotas

**Firebase**:
- Enable App Check
- Set up security rules
- Monitor usage in Firebase Console

### Use environment-specific keys
- Development: Unrestricted keys for localhost
- Staging: Restricted to staging domain
- Production: Restricted to production domain

---

## 📝 Checklist

Before starting development:
- [ ] `.env` file exists in `frontend-redesign/`
- [ ] `VITE_GOOGLE_MAPS_API_KEY` is set
- [ ] `VITE_GOOGLE_PLACES_API_KEY` is set
- [ ] All `VITE_FIREBASE_*` variables are set
- [ ] `VITE_API_BASE_URL` points to running backend
- [ ] `VITE_WS_URL` is correct
- [ ] Dev server starts without errors: `npm run dev`
- [ ] Map component loads correctly
- [ ] Authentication works
- [ ] API calls succeed

Before deploying to production:
- [ ] All production URLs are set
- [ ] API keys are production keys (not dev keys)
- [ ] API keys are restricted to production domain
- [ ] `VITE_DEV_MODE=false`
- [ ] `VITE_LOG_LEVEL=error`
- [ ] `VITE_ENABLE_ANALYTICS=true`
- [ ] Analytics tracking ID is set
- [ ] Build succeeds: `npm run build`
- [ ] Production build tested locally: `npm run preview`

---

## 📞 Getting Help

**Configuration issues?**
1. Check this guide first
2. Check `.env.example` for correct format
3. Check QUICK_START_GUIDE.md for common issues
4. Check MASTER_IMPLEMENTATION_TRACKER.md for known issues

**API key issues?**
- Google Maps: https://console.cloud.google.com/google/maps-apis
- Firebase: https://console.firebase.google.com/
- OpenWeather: https://openweathermap.org/api

**Still stuck?**
- Check browser console for errors
- Check network tab for failed requests
- Check backend logs
- Verify backend is running and accessible

---

**Last Updated**: 2025-01-27  
**Maintained By**: Development Team  
**Related Docs**: QUICK_START_GUIDE.md, MASTER_IMPLEMENTATION_TRACKER.md
