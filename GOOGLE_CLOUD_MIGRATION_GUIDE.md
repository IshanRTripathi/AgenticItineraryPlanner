# 🚀 Complete Google Cloud Migration Guide
## Agentic Itinerary Planner - New Account Setup

This comprehensive guide will help you migrate your Agentic Itinerary Planner project to a new Google Cloud account with all services, configurations, and deployments properly set up.

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [New Google Cloud Account Setup](#new-google-cloud-account-setup)
3. [Firebase & Firestore Configuration](#firebase--firestore-configuration)
4. [Google Cloud Services Setup](#google-cloud-services-setup)
5. [API Keys & External Services](#api-keys--external-services)
6. [Secret Manager Configuration](#secret-manager-configuration)
7. [Cloud Build & Deployment](#cloud-build--deployment)
8. [Environment Variables & Configuration](#environment-variables--configuration)
9. [Domain & SSL Setup](#domain--ssl-setup)
10. [Testing & Verification](#testing--verification)
11. [Troubleshooting](#troubleshooting)

---

## 🔧 Prerequisites

### Required Accounts & Services
- [ ] New Google Cloud Account with billing enabled
- [ ] Google AI Studio account (for Gemini API)
- [ ] OpenRouter account (for AI models)
- [ ] Razorpay account (for payments)
- [ ] Email service (Gmail/SMTP)
- [ ] Domain name (optional, for custom domain)

### Required Tools
- [ ] Google Cloud CLI (`gcloud`) installed
- [ ] Docker installed
- [ ] Git access to your repository
- [ ] Text editor for configuration files

---

## 🆕 New Google Cloud Account Setup

### 1. Create New Project

```bash
# Set your new project ID (choose a unique name)
export NEW_PROJECT_ID="your-new-project-id"

# Create the project
gcloud projects create $NEW_PROJECT_ID --name="Agentic Itinerary Planner"

# Set the project as active
gcloud config set project $NEW_PROJECT_ID

# Enable billing (you'll need to do this in the console)
echo "Go to: https://console.cloud.google.com/billing/linkedaccount?project=$NEW_PROJECT_ID"
```

### 2. Enable Required APIs

```bash
# Enable all required Google Cloud APIs
gcloud services enable \
  cloudbuild.googleapis.com \
  run.googleapis.com \
  firestore.googleapis.com \
  firebase.googleapis.com \
  secretmanager.googleapis.com \
  cloudresourcemanager.googleapis.com \
  iam.googleapis.com \
  containerregistry.googleapis.com \
  artifactregistry.googleapis.com \
  maps-backend.googleapis.com \
  places-backend.googleapis.com \
  geocoding-backend.googleapis.com \
  aiplatform.googleapis.com
```

### 3. Set Up Authentication

```bash
# Create a service account for the application
gcloud iam service-accounts create agentic-planner-sa \
  --display-name="Agentic Itinerary Planner Service Account" \
  --description="Service account for Agentic Itinerary Planner application"

# Grant necessary roles to the service account
gcloud projects add-iam-policy-binding $NEW_PROJECT_ID \
  --member="serviceAccount:agentic-planner-sa@$NEW_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/firestore.user"

gcloud projects add-iam-policy-binding $NEW_PROJECT_ID \
  --member="serviceAccount:agentic-planner-sa@$NEW_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud projects add-iam-policy-binding $NEW_PROJECT_ID \
  --member="serviceAccount:agentic-planner-sa@$NEW_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/run.developer"

# Create and download service account key
gcloud iam service-accounts keys create ./service-account-key.json \
  --iam-account=agentic-planner-sa@$NEW_PROJECT_ID.iam.gserviceaccount.com
```

---

## 🔥 Firebase & Firestore Configuration

### 1. Initialize Firebase Project

```bash
# Install Firebase CLI if not already installed
npm install -g firebase-tools

# Login to Firebase
firebase login

# Initialize Firebase in your project
firebase init firestore

# Select your new project when prompted
# Choose to use existing firestore.rules and firestore.indexes.json
```

### 2. Configure Firestore

Update your `firestore.rules`:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow read/write access to itineraries for authenticated users
    match /itineraries/{itineraryId} {
      allow read, write: if request.auth != null;
      allow read: if resource.data.isPublic == true;
    }
    
    // Allow read/write access to agent_tasks for authenticated users
    match /agent_tasks/{taskId} {
      allow read, write: if request.auth != null;
    }
    
    // Allow read/write access to users collection
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

Deploy Firestore rules and indexes:

```bash
# Deploy Firestore configuration
firebase deploy --only firestore:rules,firestore:indexes
```

### 3. Get Firebase Configuration

```bash
# Get your Firebase config
firebase apps:sdkconfig web

# This will give you the config object to update in your frontend
```

Update `frontend/src/config/firebase.ts` with your new Firebase config:

```typescript
const firebaseConfig = {
  apiKey: "YOUR_NEW_API_KEY",
  authDomain: "your-new-project-id.firebaseapp.com",
  projectId: "your-new-project-id",
  storageBucket: "your-new-project-id.firebasestorage.app",
  messagingSenderId: "YOUR_MESSAGING_SENDER_ID",
  appId: "YOUR_APP_ID"
};
```

---

## ☁️ Google Cloud Services Setup

### 1. Configure Cloud Build

```bash
# Grant Cloud Build service account necessary permissions
gcloud projects add-iam-policy-binding $NEW_PROJECT_ID \
  --member="serviceAccount:$NEW_PROJECT_ID@cloudbuild.gserviceaccount.com" \
  --role="roles/run.admin"

gcloud projects add-iam-policy-binding $NEW_PROJECT_ID \
  --member="serviceAccount:$NEW_PROJECT_ID@cloudbuild.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"

gcloud projects add-iam-policy-binding $NEW_PROJECT_ID \
  --member="serviceAccount:$NEW_PROJECT_ID@cloudbuild.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

### 2. Set Up Container Registry

```bash
# Configure Docker to use gcloud as a credential helper
gcloud auth configure-docker

# Enable Container Registry API
gcloud services enable containerregistry.googleapis.com
```

---

## 🔑 API Keys & External Services

### 1. Google AI Studio (Gemini API)

1. Go to [Google AI Studio](https://aistudio.google.com/)
2. Create a new API key
3. Note down the API key for Secret Manager

### 2. OpenRouter API

1. Go to [OpenRouter](https://openrouter.ai/)
2. Create an account and get your API key
3. Note down the API key for Secret Manager

### 3. Google Maps API

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Navigate to "APIs & Services" > "Credentials"
3. Create a new API key
4. Restrict the key to:
   - Maps JavaScript API
   - Places API
   - Geocoding API
5. Note down the API key for Secret Manager

### 4. OpenWeather API (Optional)

1. Go to [OpenWeatherMap](https://openweathermap.org/api)
2. Sign up for a free account
3. Get your API key
4. Note down the API key for Secret Manager

### 5. Razorpay (Optional)

1. Go to [Razorpay Dashboard](https://dashboard.razorpay.com/)
2. Get your Key ID and Key Secret
3. Note down both for Secret Manager

---

## 🔐 Secret Manager Configuration

### 1. Create All Required Secrets

```bash
# Create secrets in Google Secret Manager
gcloud secrets create _GEMINI_API_KEY --data-file=- <<< "your-gemini-api-key"
gcloud secrets create _OPENROUTER_API_KEY --data-file=- <<< "your-openrouter-api-key"
gcloud secrets create _GOOGLE_MAPS_BROWSER_KEY --data-file=- <<< "your-google-maps-api-key"
gcloud secrets create _OPENWEATHER_API_KEY --data-file=- <<< "your-openweather-api-key"
gcloud secrets create _RAZORPAY_KEY_ID --data-file=- <<< "your-razorpay-key-id"
gcloud secrets create _RAZORPAY_KEY_SECRET --data-file=- <<< "your-razorpay-key-secret"
gcloud secrets create _SMTP_USERNAME --data-file=- <<< "your-smtp-username"
gcloud secrets create _SMTP_PASSWORD --data-file=- <<< "your-smtp-password"
gcloud secrets create _AI_MODEL --data-file=- <<< "qwen/qwen-2.5-72b-instruct:free"
gcloud secrets create _FIRESTORE_CREDENTIALS --data-file=./service-account-key.json
```

### 2. Grant Access to Secrets

```bash
# Grant Cloud Build access to secrets
gcloud secrets add-iam-policy-binding _GEMINI_API_KEY \
  --member="serviceAccount:$NEW_PROJECT_ID@cloudbuild.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding _OPENROUTER_API_KEY \
  --member="serviceAccount:$NEW_PROJECT_ID@cloudbuild.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding _GOOGLE_MAPS_BROWSER_KEY \
  --member="serviceAccount:$NEW_PROJECT_ID@cloudbuild.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding _OPENWEATHER_API_KEY \
  --member="serviceAccount:$NEW_PROJECT_ID@cloudbuild.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding _RAZORPAY_KEY_ID \
  --member="serviceAccount:$NEW_PROJECT_ID@cloudbuild.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding _RAZORPAY_KEY_SECRET \
  --member="serviceAccount:$NEW_PROJECT_ID@cloudbuild.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding _SMTP_USERNAME \
  --member="serviceAccount:$NEW_PROJECT_ID@cloudbuild.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding _SMTP_PASSWORD \
  --member="serviceAccount:$NEW_PROJECT_ID@cloudbuild.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding _AI_MODEL \
  --member="serviceAccount:$NEW_PROJECT_ID@cloudbuild.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding _FIRESTORE_CREDENTIALS \
  --member="serviceAccount:$NEW_PROJECT_ID@cloudbuild.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

---

## 🏗️ Cloud Build & Deployment

### 1. Update cloudbuild.yaml

Update your `cloudbuild.yaml` file with the new project ID:

```yaml
# Update the substitutions section
substitutions:
  _REGION: 'us-central1'  # or your preferred region
  _BACKEND_URL: 'https://agentic-itinerary-planner-backend-xxxxx-uc.a.run.app'  # Will be updated after first deployment
  _FRONTEND_URL: 'https://agentic-itinerary-planner-frontend-xxxxx-uc.a.run.app'  # Will be updated after first deployment
  _GCP_PROJECT_ID: 'your-new-project-id'
```

### 2. Update Application Configuration

Update `src/main/resources/application-cloud.yml`:

```yaml
spring:
  cloud:
    gcp:
      project-id: your-new-project-id
      firestore:
        project-id: your-new-project-id
```

### 3. First Deployment

```bash
# Submit the build
gcloud builds submit --config cloudbuild.yaml .

# This will:
# 1. Build both frontend and backend Docker images
# 2. Push them to Container Registry
# 3. Deploy to Cloud Run
# 4. Set up all environment variables and secrets
```

### 4. Get Deployment URLs

After the first deployment, get your service URLs:

```bash
# Get backend URL
BACKEND_URL=$(gcloud run services describe agentic-itinerary-planner-backend \
  --region=us-central1 \
  --format="value(status.url)")

# Get frontend URL
FRONTEND_URL=$(gcloud run services describe agentic-itinerary-planner-frontend \
  --region=us-central1 \
  --format="value(status.url)")

echo "Backend URL: $BACKEND_URL"
echo "Frontend URL: $FRONTEND_URL"
```

### 5. Update cloudbuild.yaml with Actual URLs

Update the `substitutions` section in `cloudbuild.yaml` with the actual URLs:

```yaml
substitutions:
  _REGION: 'us-central1'
  _BACKEND_URL: 'https://agentic-itinerary-planner-backend-xxxxx-uc.a.run.app'
  _FRONTEND_URL: 'https://agentic-itinerary-planner-frontend-xxxxx-uc.a.run.app'
  _GCP_PROJECT_ID: 'your-new-project-id'
```

---

## ⚙️ Environment Variables & Configuration

### 1. Backend Environment Variables

The following environment variables are automatically set by Cloud Build:

| Variable | Source | Description |
|----------|--------|-------------|
| `SPRING_PROFILES_ACTIVE` | Cloud Build | Set to `cloud` |
| `GEMINI_API_KEY` | Secret Manager | Gemini AI API key |
| `OPENROUTER_API_KEY` | Secret Manager | OpenRouter API key |
| `AI_MODEL` | Secret Manager | AI model name |
| `GEMINI_MOCK_MODE` | Cloud Build | Set to `false` |
| `AI_PROVIDER` | Cloud Build | Set to `openrouter` |
| `FIRESTORE_CREDENTIALS` | Secret Manager | Service account JSON |
| `GCP_PROJECT_ID` | Cloud Build | Your project ID |
| `FRONTEND_URL` | Cloud Build | Frontend service URL |

### 2. Frontend Environment Variables

The following are set during the Docker build:

| Variable | Source | Description |
|----------|--------|-------------|
| `VITE_API_BASE_URL` | Build arg | Backend API URL |
| `VITE_GOOGLE_MAPS_BROWSER_KEY` | Secret Manager | Google Maps API key |
| `VITE_OPENWEATHER_API_KEY` | Secret Manager | OpenWeather API key |

### 3. Local Development Setup

Create `.env` file in the project root:

```bash
# Backend environment variables
GEMINI_API_KEY=your-gemini-api-key
OPENROUTER_API_KEY=your-openrouter-api-key
AI_PROVIDER=openrouter
AI_MODEL=qwen/qwen-2.5-72b-instruct:free
GEMINI_MOCK_MODE=false
FIRESTORE_PROJECT_ID=your-new-project-id
FIRESTORE_CREDENTIALS_FILE=./service-account-key.json
GOOGLE_PLACES_API_KEY=your-google-maps-api-key
RAZORPAY_KEY_ID=your-razorpay-key-id
RAZORPAY_KEY_SECRET=your-razorpay-key-secret
SMTP_USERNAME=your-smtp-username
SMTP_PASSWORD=your-smtp-password
EMAIL_FROM=noreply@yourdomain.com
```

Create `frontend/.env.local`:

```bash
# Frontend environment variables
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_GOOGLE_MAPS_BROWSER_KEY=your-google-maps-api-key
VITE_GOOGLE_CLIENT_ID=your-google-oauth-client-id
VITE_OPENWEATHER_API_KEY=your-openweather-api-key
```

---

## 🌐 Domain & SSL Setup

### 1. Custom Domain (Optional)

If you want to use a custom domain:

```bash
# Map your domain to Cloud Run services
gcloud run domain-mappings create \
  --service=agentic-itinerary-planner-frontend \
  --domain=yourdomain.com \
  --region=us-central1

gcloud run domain-mappings create \
  --service=agentic-itinerary-planner-backend \
  --domain=api.yourdomain.com \
  --region=us-central1
```

### 2. Update DNS Records

Add the following DNS records to your domain:

```
# For frontend
CNAME www yourdomain.com
A @ [Cloud Run IP]

# For backend API
CNAME api api.yourdomain.com
```

---

## 🧪 Testing & Verification

### 1. Test Backend API

```bash
# Test health endpoint
curl https://your-backend-url.run.app/api/v1/health

# Test itinerary creation
curl -X POST https://your-backend-url.run.app/api/v1/itineraries \
  -H "Content-Type: application/json" \
  -d '{"destination": "Paris", "duration": 3, "travelers": 2}'
```

### 2. Test Frontend

1. Open your frontend URL in a browser
2. Verify the application loads correctly
3. Test creating a new itinerary
4. Verify Firebase authentication works
5. Test the chat functionality

### 3. Test Firebase Integration

```bash
# Test Firestore connection
firebase firestore:get /itineraries --project=your-new-project-id
```

### 4. Monitor Logs

```bash
# View backend logs
gcloud logs tail --service=agentic-itinerary-planner-backend

# View frontend logs
gcloud logs tail --service=agentic-itinerary-planner-frontend

# View build logs
gcloud builds log --stream
```

---

## 🔧 Troubleshooting

### Common Issues & Solutions

#### 1. Build Failures

**Issue**: Docker build fails
**Solution**: 
```bash
# Check Docker is running
docker --version

# Clear Docker cache
docker system prune -a

# Rebuild with verbose output
gcloud builds submit --config cloudbuild.yaml . --verbosity=debug
```

#### 2. Secret Manager Access Denied

**Issue**: Cloud Build can't access secrets
**Solution**:
```bash
# Grant Cloud Build access to Secret Manager
gcloud projects add-iam-policy-binding $NEW_PROJECT_ID \
  --member="serviceAccount:$NEW_PROJECT_ID@cloudbuild.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

#### 3. Firestore Connection Issues

**Issue**: Backend can't connect to Firestore
**Solution**:
```bash
# Verify Firestore is enabled
gcloud services list --enabled | grep firestore

# Check service account permissions
gcloud projects get-iam-policy $NEW_PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:agentic-planner-sa@$NEW_PROJECT_ID.iam.gserviceaccount.com"
```

#### 4. CORS Issues

**Issue**: Frontend can't connect to backend
**Solution**: Update CORS configuration in your Spring Boot application to include your frontend domain.

#### 5. API Key Issues

**Issue**: Google Maps or other APIs not working
**Solution**:
```bash
# Verify API keys are correctly set in Secret Manager
gcloud secrets versions access latest --secret="_GOOGLE_MAPS_BROWSER_KEY"

# Check API quotas in Google Cloud Console
```

### Debug Commands

```bash
# Check service status
gcloud run services list

# Check service details
gcloud run services describe agentic-itinerary-planner-backend --region=us-central1

# Check build history
gcloud builds list --limit=10

# Check secret versions
gcloud secrets versions list _GEMINI_API_KEY
```

---

## 📊 Cost Optimization

### 1. Set Up Budget Alerts

```bash
# Create a budget alert
gcloud billing budgets create \
  --billing-account=YOUR_BILLING_ACCOUNT_ID \
  --display-name="Agentic Planner Budget" \
  --budget-amount=100USD \
  --threshold-rule=percent=80 \
  --threshold-rule=percent=100
```

### 2. Optimize Cloud Run Settings

- Set appropriate memory limits (512Mi for frontend, 2Gi for backend)
- Use appropriate CPU allocation
- Set min instances to 0 to save costs
- Configure appropriate concurrency limits

### 3. Monitor Usage

```bash
# Check current usage
gcloud billing accounts list
gcloud billing budgets list --billing-account=YOUR_BILLING_ACCOUNT_ID
```

---

## 🚀 Final Checklist

Before going live, ensure:

- [ ] All APIs are enabled
- [ ] All secrets are created and accessible
- [ ] Firestore rules and indexes are deployed
- [ ] Both services are deployed and accessible
- [ ] Environment variables are correctly set
- [ ] CORS is properly configured
- [ ] Firebase authentication is working
- [ ] All external API keys are valid
- [ ] Domain mapping is complete (if using custom domain)
- [ ] SSL certificates are active
- [ ] Monitoring and logging are set up
- [ ] Budget alerts are configured

---

## 📞 Support & Resources

### Useful Commands Reference

```bash
# Quick project setup
export PROJECT_ID="your-project-id"
gcloud config set project $PROJECT_ID

# Deploy everything
gcloud builds submit --config cloudbuild.yaml .

# Check service status
gcloud run services list

# View logs
gcloud logs tail --service=agentic-itinerary-planner-backend

# Update secrets
gcloud secrets versions add _GEMINI_API_KEY --data-file=- <<< "new-api-key"
```

### Important URLs

- [Google Cloud Console](https://console.cloud.google.com/)
- [Firebase Console](https://console.firebase.google.com/)
- [Cloud Run Console](https://console.cloud.google.com/run)
- [Secret Manager Console](https://console.cloud.google.com/security/secret-manager)
- [Cloud Build Console](https://console.cloud.google.com/cloud-build)

---

## 🎉 Congratulations!

Your Agentic Itinerary Planner should now be fully deployed on your new Google Cloud account! 

The application includes:
- ✅ **Backend API** running on Cloud Run
- ✅ **Frontend** running on Cloud Run with Nginx
- ✅ **Firestore** database with proper rules and indexes
- ✅ **Firebase Authentication** configured
- ✅ **AI Integration** with Gemini and OpenRouter
- ✅ **Google Maps** integration
- ✅ **Secret Management** for all API keys
- ✅ **Automated CI/CD** with Cloud Build
- ✅ **Monitoring and Logging** set up

Your application is now ready for production use! 🚀
