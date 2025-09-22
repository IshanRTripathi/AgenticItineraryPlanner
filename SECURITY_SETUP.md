# Security Setup Guide

## Firebase Service Account Credentials

### For Local Development

1. **Download the service account key** from Firebase Console:
   - Go to Project Settings → Service Accounts
   - Click "Generate new private key"
   - Save as `tripplanner-bdd3c-firebase-adminsdk-fbsvc-633254ef65.json` in the project root

2. **Set environment variables** in `.env.local`:
   ```bash
   FIRESTORE_USE_EMULATOR=false
   GCP_PROJECT_ID=tripplanner-bdd3c
   FIRESTORE_CREDENTIALS_FILE=./tripplanner-bdd3c-firebase-adminsdk-fbsvc-633254ef65.json
   ```

### For Production Deployment

1. **Set up Google Cloud Build environment variables**:
   ```bash
   gcloud builds submit --config cloudbuild.yaml \
     --substitutions=_FIRESTORE_CREDENTIALS='{"type":"service_account",...}'
   ```

2. **Or use Google Secret Manager** (recommended):
   ```bash
   # Store credentials in Secret Manager
   gcloud secrets create firestore-credentials --data-file=credentials.json
   
   # Update cloudbuild.yaml to use Secret Manager
   ```

## Security Best Practices

- ✅ **Never commit credentials to git**
- ✅ **Use environment variables for sensitive data**
- ✅ **Use Google Secret Manager for production**
- ✅ **Rotate credentials regularly**
- ✅ **Use least privilege access**

## Files to Keep Secure

- `tripplanner-bdd3c-firebase-adminsdk-fbsvc-633254ef65.json` - Service account key
- `.env.local` - Local environment variables
- Any files containing API keys or secrets

## Troubleshooting

If you see "Push cannot contain secrets" error:
1. Remove the credential file from git history
2. Add it to `.gitignore`
3. Use environment variables instead
4. Force push to clean the history (if safe to do so)
