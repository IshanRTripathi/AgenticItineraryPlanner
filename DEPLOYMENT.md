# Google Cloud Deployment Guide

This guide explains how to deploy the Agentic Itinerary Planner to Google Cloud Platform using Cloud Build and Cloud Run.

## Prerequisites

1. **Google Cloud Project**: Create a new project or use an existing one
2. **Enable APIs**: Enable the following APIs in your project:
   - Cloud Build API
   - Cloud Run API
   - Container Registry API
   - Artifact Registry API (recommended)

3. **Install Google Cloud SDK**: Install and configure the gcloud CLI
4. **Docker**: Install Docker for local testing (optional)

## Setup Instructions

### 1. Configure Google Cloud Project

```bash
# Set your project ID
export PROJECT_ID="your-project-id"
gcloud config set project $PROJECT_ID

# Enable required APIs
gcloud services enable cloudbuild.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable containerregistry.googleapis.com
```

### 2. Configure Environment Variables

You need to set up the following environment variables in your Cloud Build trigger:

#### Required Variables:
- `_GEMINI_API_KEY`: Your Google Gemini API key
- `_GOOGLE_OAUTH_CLIENT_ID`: Google OAuth client ID
- `_GOOGLE_OAUTH_CLIENT_SECRET`: Google OAuth client secret

#### Optional Variables:
- `_RAZORPAY_KEY_ID`: Razorpay payment gateway key ID
- `_RAZORPAY_KEY_SECRET`: Razorpay payment gateway secret
- `_SMTP_USERNAME`: Email service username
- `_SMTP_PASSWORD`: Email service password
- `_EMAIL_FROM`: From email address
- `_REGION`: Deployment region (default: us-central1)

### 3. Create Cloud Build Trigger

1. Go to Cloud Build > Triggers in the Google Cloud Console
2. Click "Create Trigger"
3. Configure the trigger:
   - **Name**: `agentic-itinerary-planner-deploy`
   - **Event**: Push to a branch
   - **Source**: Connect your repository
   - **Branch**: `main` (or your default branch)
   - **Build Configuration**: Cloud Build configuration file
   - **Location**: Repository
   - **Cloud Build config file**: `cloudbuild.yaml`

4. **Important**: After the first deployment, update the `_BACKEND_URL` and `_FRONTEND_URL` variables in the trigger with the actual Cloud Run URLs.

### 4. First Deployment

1. Push your code to the main branch
2. The Cloud Build trigger will automatically start
3. Wait for the build to complete
4. Note the Cloud Run URLs from the build logs

### 5. Update URLs and Redeploy

After the first deployment, you need to update the URLs in the Cloud Build trigger:

1. Go to Cloud Build > Triggers
2. Edit your trigger
3. Update the substitution variables:
   - `_BACKEND_URL`: `https://agentic-itinerary-planner-backend-xxxxxxxxx-uc.a.run.app`
   - `_FRONTEND_URL`: `https://agentic-itinerary-planner-frontend-xxxxxxxxx-uc.a.run.app`
   - `_PDF_BASE_URL`: Same as backend URL
   - `_APP_BASE_URL`: Same as backend URL

4. Save the trigger
5. Push another commit to trigger a new deployment

## Architecture

The deployment creates two separate Cloud Run services:

### Backend Service
- **Name**: `agentic-itinerary-planner-backend`
- **Port**: 8080
- **Resources**: 2 CPU, 2GB RAM
- **Max Instances**: 10
- **Timeout**: 300 seconds

### Frontend Service
- **Name**: `agentic-itinerary-planner-frontend`
- **Port**: 80
- **Resources**: 1 CPU, 512MB RAM
- **Max Instances**: 10
- **Timeout**: 60 seconds

## Environment Configuration

### Backend Environment Variables
The backend uses the following environment variables (set in Cloud Build trigger):

- `SPRING_PROFILES_ACTIVE=cloud`
- `GEMINI_API_KEY`: Your Gemini API key
- `GOOGLE_OAUTH_CLIENT_ID`: OAuth client ID
- `GOOGLE_OAUTH_CLIENT_SECRET`: OAuth client secret
- `RAZORPAY_KEY_ID`: Payment gateway key
- `RAZORPAY_KEY_SECRET`: Payment gateway secret
- `SMTP_USERNAME`: Email service username
- `SMTP_PASSWORD`: Email service password
- `EMAIL_FROM`: From email address
- `PDF_BASE_URL`: Backend URL for PDF generation
- `APP_BASE_URL`: Backend URL
- `FRONTEND_URL`: Frontend URL

### Frontend Environment Variables
The frontend uses the following environment variable:

- `VITE_API_BASE_URL`: Backend API URL (set during build)

## Local Development

### Backend
```bash
# Run with development profile
./gradlew bootRun

# Run with cloud profile for testing
./gradlew bootRun --args='--spring.profiles.active=cloud'
```

### Frontend
```bash
cd frontend

# Install dependencies
npm install

# Create environment file
cp .env.example .env.local

# Edit .env.local with your local backend URL
# VITE_API_BASE_URL=http://localhost:8080/api/v1

# Start development server
npm run dev
```

## Monitoring and Logs

### View Logs
```bash
# Backend logs
gcloud run services logs read agentic-itinerary-planner-backend --region=us-central1

# Frontend logs
gcloud run services logs read agentic-itinerary-planner-frontend --region=us-central1
```

### Monitor Performance
- Go to Cloud Run in the Google Cloud Console
- Click on your service to view metrics
- Monitor CPU, memory, and request metrics

## Troubleshooting

### Common Issues

1. **Build Fails**: Check the Cloud Build logs for specific error messages
2. **Service Won't Start**: Check the Cloud Run logs for startup errors
3. **API Connection Issues**: Verify the `VITE_API_BASE_URL` is correctly set
4. **CORS Issues**: The nginx configuration includes CORS headers

### Debug Commands

```bash
# Check service status
gcloud run services describe agentic-itinerary-planner-backend --region=us-central1
gcloud run services describe agentic-itinerary-planner-frontend --region=us-central1

# View recent logs
gcloud run services logs tail agentic-itinerary-planner-backend --region=us-central1
```

## Security Considerations

1. **Environment Variables**: Store sensitive data in Cloud Build trigger variables, not in code
2. **HTTPS**: Cloud Run automatically provides HTTPS
3. **CORS**: Configured in nginx for frontend-backend communication
4. **Database**: Currently uses H2 file database (consider Cloud SQL for production)

## Cost Optimization

1. **Min Instances**: Set to 0 to scale to zero when not in use
2. **Max Instances**: Adjust based on expected traffic
3. **CPU Allocation**: Frontend uses 1 CPU, backend uses 2 CPU
4. **Memory**: Frontend uses 512MB, backend uses 2GB

## Next Steps

1. Set up a custom domain
2. Configure Cloud SQL for persistent database
3. Set up monitoring and alerting
4. Configure CI/CD with proper testing
5. Set up backup and disaster recovery
