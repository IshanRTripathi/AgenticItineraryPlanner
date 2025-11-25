# Analytics Ingestion Cloud Function

## ⚠️ IMPORTANT: This is a Cloud Function, NOT a Cloud Run service

This code is designed to be deployed as a **Google Cloud Function**, which is event-driven and doesn't require an HTTP server.

## Deployment Options

### Option 1: Cloud Function (Recommended) ✅

Deploy as a Cloud Function triggered by Pub/Sub:

```bash
cd cloud-functions/analytics-ingestion

# Install dependencies
npm install

# Deploy using gcloud CLI
gcloud functions deploy analytics-ingestion \
  --gen2 \
  --runtime nodejs20 \
  --trigger-topic analytics-events \
  --entry-point ingestToBigQuery \
  --region us-south1 \
  --memory 256MB \
  --timeout 60s \
  --update-env-vars="BIGQUERY_DATASET=analytics,BIGQUERY_TABLE=raw_events" \
  --project tripaiplanner
```

**OR** use the deployment script:

```bash
./deploy.sh
```

### Option 2: Cloud Run (If you really need it) ⚠️

If you need to deploy to Cloud Run instead, you'll need a different implementation with an HTTP server. Here's why:

- **Cloud Functions**: Event-driven, no HTTP server needed, triggered by Pub/Sub
- **Cloud Run**: Requires HTTP server listening on PORT 8080

## Why the Error Occurred

The error message:
```
The user-provided container failed to start and listen on the port defined provided by the PORT=8080
```

This means you tried to deploy Cloud Function code to Cloud Run, which expects:
1. An HTTP server (Express, Fastify, etc.)
2. Listening on PORT 8080
3. Responding to health checks

## How to Fix

### If you want Cloud Functions (Recommended):

Use the `gcloud functions deploy` command above, NOT `gcloud run deploy`.

### If you need Cloud Run:

I can create a Cloud Run version with Express server. Let me know if you need this.

## Verification

After deploying as Cloud Function:

```bash
# Check function exists
gcloud functions list --project tripaiplanner

# View logs
gcloud logging read \
  "resource.type=cloud_run_revision AND resource.labels.service_name=analytics-ingestion" \
  --project tripaiplanner \
  --limit=10

# Test with Pub/Sub message
gcloud pubsub topics publish analytics-events \
  --project tripaiplanner \
  --message '{"eventName":"test","timestamp":'$(date +%s)000',"userId":"test","sessionId":"test","platform":"test","properties":{"test":true}}'
```

## Architecture

```
Frontend/Backend
    ↓
POST /api/v1/analytics/events
    ↓
Pub/Sub Topic: analytics-events
    ↓
Cloud Function: analytics-ingestion  ← YOU ARE HERE
    ↓
BigQuery: analytics.raw_events
```

The Cloud Function is triggered automatically when messages arrive in Pub/Sub. No HTTP server needed!

## Common Mistakes

❌ **WRONG**: Deploying to Cloud Run
```bash
gcloud run deploy analytics-ingestion ...  # ❌ This won't work
```

✅ **CORRECT**: Deploying as Cloud Function
```bash
gcloud functions deploy analytics-ingestion ...  # ✅ This is correct
```

## Need Cloud Run Instead?

If you specifically need Cloud Run (e.g., for custom scaling, VPC access, etc.), I can create a Cloud Run version with:
- Express HTTP server
- Health check endpoint
- Pub/Sub push subscription handler

Just let me know!
