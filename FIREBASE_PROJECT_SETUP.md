# Firebase Project Setup Guide

This guide helps you set up your Firebase project `tripplanner-bdd3c` for the Agentic Itinerary Planner.

## Project Configuration

Your Firebase project details:
- **Project ID**: `tripplanner-bdd3c`
- **API Key**: `AIzaSyD8v1WcTJ4U5IbttjI4W9hy-aDAGArVQGk`
- **Auth Domain**: `tripplanner-bdd3c.firebaseapp.com`
- **Storage Bucket**: `tripplanner-bdd3c.firebasestorage.app`
- **Messaging Sender ID**: `815103657721`
- **App ID**: `1:815103657721:web:069d08463b62088d72c428`

## 1. Enable Firestore Database

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project `tripplanner-bdd3c`
3. Navigate to **Firestore Database**
4. Click **Create database**
5. Choose **Start in test mode** (for development)
6. Select a location (recommend `us-central1` or `us-east1`)

## 2. Set Up Firestore Security Rules

Replace the default rules with these:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow read/write access to itineraries collection
    match /itineraries/{itineraryId} {
      allow read, write: if true; // For development - restrict in production
    }
    
    // Allow access to revisions subcollection
    match /itineraries/{itineraryId}/revisions/{revisionId} {
      allow read, write: if true; // For development - restrict in production
    }
  }
}
```

## 3. Create Service Account (for Backend)

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select project `tripplanner-bdd3c`
3. Navigate to **IAM & Admin** > **Service Accounts**
4. Click **Create Service Account**
5. Name: `firestore-service-account`
6. Description: `Service account for Firestore access`
7. Click **Create and Continue**
8. Grant roles:
   - **Cloud Datastore User**
   - **Firebase Admin SDK Administrator Service Agent**
9. Click **Done**
10. Click on the created service account
11. Go to **Keys** tab
12. Click **Add Key** > **Create new key**
13. Choose **JSON** format
14. Download the key file

## 4. Environment Variables

### For Local Development

Create a `.env` file in your project root:

```bash
# Database Configuration
APP_DATABASE_TYPE=firestore
FIRESTORE_USE_EMULATOR=true
FIRESTORE_EMULATOR_HOST=localhost:8080
FIRESTORE_ENABLED=true

# For production (when ready)
# APP_DATABASE_TYPE=firestore
# FIRESTORE_USE_EMULATOR=false
# FIRESTORE_CREDENTIALS={"type":"service_account","project_id":"tripplanner-bdd3c",...}
# GCP_PROJECT_ID=tripplanner-bdd3c
```

### For Production Deployment

Set these environment variables in your deployment platform:

```bash
APP_DATABASE_TYPE=firestore
FIRESTORE_USE_EMULATOR=false
FIRESTORE_CREDENTIALS={"type":"service_account","project_id":"tripplanner-bdd3c","private_key_id":"...","private_key":"...","client_email":"...","client_id":"...","auth_uri":"...","token_uri":"...","auth_provider_x509_cert_url":"...","client_x509_cert_url":"..."}
GCP_PROJECT_ID=tripplanner-bdd3c
```

## 5. Local Development Setup

### Install Firebase CLI

```bash
npm install -g firebase-tools
```

### Login to Firebase

```bash
firebase login
```

### Initialize Firebase in Project

```bash
firebase init firestore
```

### Start Firestore Emulator

```bash
firebase emulators:start --only firestore
```

The emulator will be available at:
- **Firestore**: `localhost:8080`
- **Emulator UI**: `http://localhost:4000`

## 6. Frontend Setup

### Install Firebase SDK

```bash
cd frontend
npm install firebase
```

### Firebase Configuration

The Firebase configuration is already set up in `frontend/src/config/firebase.ts` with your project details.

### Usage Example

```typescript
import { firebaseService } from '../services/firebaseService';

// Get an itinerary
const itinerary = await firebaseService.getItinerary('it_barcelona_comprehensive');

// Save an itinerary
const id = await firebaseService.saveItinerary(itinerary);

// Update an itinerary
await firebaseService.updateItinerary(id, updatedItinerary);
```

## 7. Backend Setup

The backend is already configured to use your Firebase project. The configuration is in:

- `src/main/resources/application.yml`
- `cloudbuild.yaml`

## 8. Testing the Setup

### Test Local Emulator

1. Start the emulator: `firebase emulators:start --only firestore`
2. Start the backend: `./gradlew bootRun`
3. Test endpoints:
   ```bash
   # Create sample data
   curl -X POST http://localhost:8080/api/v1/test/create-sample
   
   # Get itinerary
   curl http://localhost:8080/api/v1/itineraries/it_barcelona_comprehensive/json
   ```

### Test Production Firestore

1. Deploy with production environment variables
2. Test the same endpoints against your deployed backend

## 9. Data Structure

Your Firestore will have this structure:

```
itineraries/
├── {itineraryId}/
│   ├── id: string
│   ├── version: number
│   ├── json: string (NormalizedItinerary JSON)
│   ├── updatedAt: timestamp
│   └── revisions/
│       ├── v1: {revision data}
│       ├── v2: {revision data}
│       └── ...
```

## 10. Cost Monitoring

Monitor your Firestore usage in the [Firebase Console](https://console.firebase.google.com/):

1. Go to **Usage** tab
2. Monitor:
   - **Reads**: 50,000/day free
   - **Writes**: 20,000/day free
   - **Deletes**: 10,000/day free
   - **Storage**: 1 GB free

## 11. Security (Production)

For production, update your Firestore rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /itineraries/{itineraryId} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
    }
  }
}
```

## Troubleshooting

### Common Issues

1. **Permission Denied**: Check Firestore security rules
2. **Project Not Found**: Verify project ID in configuration
3. **Authentication Failed**: Check service account credentials
4. **Emulator Not Starting**: Ensure port 8080 is available

### Debug Commands

```bash
# Check Firebase project
firebase projects:list

# Check current project
firebase use

# Test emulator connection
curl http://localhost:8080/v1/projects/tripplanner-bdd3c/databases/(default)/documents
```

## Next Steps

1. ✅ Enable Firestore Database
2. ✅ Set up security rules
3. ✅ Create service account
4. ✅ Configure environment variables
5. ✅ Test local emulator
6. ✅ Deploy to production
7. 🔄 Monitor usage and costs
8. 🔄 Implement authentication (optional)

