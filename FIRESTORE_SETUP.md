# Firestore Setup Guide

This guide will help you set up Firestore for the Agentic Itinerary Planner, both for local development and production deployment.

## Prerequisites

1. **Firebase CLI**: Install Firebase CLI globally
   ```bash
   npm install -g firebase-tools
   ```

2. **Firebase Project**: The project is already configured to use `tripplanner-bdd3c`

3. **Service Account**: The service account credentials are already configured

## Local Development Setup

### 1. Start Firestore Emulator

Run the setup script:
```bash
chmod +x setup-firebase.sh
./setup-firebase.sh
```

Or manually:
```bash
# Login to Firebase
firebase login --reauth

# Set the project
firebase use tripplanner-bdd3c

# Start the emulator
firebase emulators:start --only firestore
```

The Firestore emulator will be available at:
- **Emulator**: http://localhost:8080
- **Emulator UI**: http://localhost:4000

### 2. Run the Application

Set environment variables for local development:
```bash
export FIRESTORE_USE_EMULATOR=true
export FIRESTORE_EMULATOR_HOST=localhost:8080
export GCP_PROJECT_ID=tripplanner-bdd3c
```

Or create a `.env.local` file:
```bash
FIRESTORE_USE_EMULATOR=true
FIRESTORE_EMULATOR_HOST=localhost:8080
GCP_PROJECT_ID=tripplanner-bdd3c
FIRESTORE_CREDENTIALS_FILE=tripplanner-bdd3c-firebase-adminsdk-fbsvc-633254ef65.json
```

Then run the application:
```bash
./gradlew bootRun
```

## Production Deployment

### 1. Environment Variables

The production deployment is already configured in `cloudbuild.yaml` with:
- `FIRESTORE_USE_EMULATOR=false`
- `FIRESTORE_CREDENTIALS` (service account JSON)
- `GCP_PROJECT_ID=tripplanner-bdd3c`

### 2. Deploy

The application will automatically use production Firestore when deployed via Google Cloud Build.

## Data Structure

### Collections

1. **`itineraries`** - Main itinerary documents
   - Document ID: `{itineraryId}`
   - Fields: `version`, `json`, `updatedAt`

2. **`revisions`** - Version history for undo/redo
   - Document ID: `{itineraryId}_v{version}`
   - Fields: `itineraryId`, `version`, `json`, `createdAt`

### Example Document Structure

```json
{
  "id": "it_barcelona_comprehensive",
  "version": 1,
  "json": "{\"itineraryId\":\"it_barcelona_comprehensive\",\"destination\":\"Barcelona, Spain\",...}",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

## Configuration Files

- **`firebase.json`** - Firebase project configuration
- **`firestore.rules`** - Security rules (permissive for development)
- **`firestore.indexes.json`** - Database indexes
- **`.firebaserc`** - Default project configuration

## Testing

### 1. Test Endpoints

The application provides test endpoints for Firestore integration:

```bash
# Create sample data
curl -X POST http://localhost:8080/api/v1/test/create-sample

# Get itinerary
curl http://localhost:8080/api/v1/test/itinerary/it_barcelona_sample

# List all itineraries
curl http://localhost:8080/api/v1/test/itineraries
```

### 2. Emulator UI

Access the Firestore emulator UI at http://localhost:4000 to:
- View collections and documents
- Run queries
- Monitor real-time updates

## Troubleshooting

### Common Issues

1. **Emulator not starting**
   - Check if port 8080 is available
   - Ensure Firebase CLI is installed and logged in

2. **Connection errors**
   - Verify `FIRESTORE_USE_EMULATOR=true` is set
   - Check `FIRESTORE_EMULATOR_HOST=localhost:8080`

3. **Authentication errors**
   - Ensure service account credentials are valid
   - Check project ID matches `tripplanner-bdd3c`

### Logs

Check application logs for Firestore connection status:
```bash
./gradlew bootRun --info
```

## Cost Optimization

### Free Tier Limits
- **Reads**: 50,000/day
- **Writes**: 20,000/day
- **Deletes**: 10,000/day
- **Storage**: 1GB

### Usage Monitoring
Monitor usage in the Firebase Console:
https://console.firebase.google.com/project/tripplanner-bdd3c/usage

## Security

### Development
- Uses permissive rules for easy development
- Emulator runs locally without authentication

### Production
- Service account authentication
- Consider implementing proper security rules
- Monitor access patterns

## Next Steps

1. **Start the emulator**: `./setup-firebase.sh`
2. **Run the application**: `./gradlew bootRun`
3. **Test the endpoints**: Use the test endpoints to verify integration
4. **Deploy to production**: Push to trigger Cloud Build deployment

The application is now fully configured to use Firestore with both local development and production support!