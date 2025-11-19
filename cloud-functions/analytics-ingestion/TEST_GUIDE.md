# Analytics Ingestion Testing Guide

## Problem Diagnosis

If events are sent to Pub/Sub but not appearing in BigQuery, the issue is likely:

1. **Schema mismatch** - Properties field not being converted to JSON string
2. **Cloud Function not deployed** - Function doesn't exist or isn't triggered
3. **Permissions issue** - Function can't write to BigQuery
4. **Dead letter queue** - Messages are failing and being moved to DLQ

## Step-by-Step Testing

### 1. Check if Cloud Function Exists

```bash
# List all Cloud Functions
gcloud functions list --project=tripaiplanner

# Check if analytics-ingestion exists
gcloud functions describe analytics-ingestion \
  --region=us-central1 \
  --project=tripaiplanner
```

**Expected**: Function should exist and show status "ACTIVE"

### 2. Check Cloud Function Logs

```bash
# View recent logs
gcloud functions logs read analytics-ingestion \
  --region=us-central1 \
  --project=tripaiplanner \
  --limit=50

# Follow logs in real-time
gcloud functions logs read analytics-ingestion \
  --region=us-central1 \
  --project=tripaiplanner \
  --limit=50 \
  --follow
```

**Look for**:
- ✅ "Event ingested successfully" messages
- ❌ "Failed to ingest event" errors
- ❌ "BigQuery PartialFailureError" errors

### 3. Test with HTTP Endpoint (Recommended)

Deploy the test HTTP function:

```bash
cd cloud-functions/analytics-ingestion

gcloud functions deploy analytics-ingestion-test \
  --gen2 \
  --runtime nodejs20 \
  --trigger-http \
  --allow-unauthenticated \
  --entry-point testIngest \
  --region us-central1 \
  --memory 256MB \
  --timeout 60s \
  --set-env-vars BIGQUERY_DATASET=analytics,BIGQUERY_TABLE=raw_events \
  --project=tripaiplanner
```

Then test it:

```bash
# Get the function URL
FUNCTION_URL=$(gcloud functions describe analytics-ingestion-test \
  --region=us-central1 \
  --project=tripaiplanner \
  --format='value(serviceConfig.uri)')

echo "Function URL: $FUNCTION_URL"

# Send test event
curl -X POST "$FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -d @test-event.json

# Or with inline JSON
curl -X POST "$FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventName": "test_event",
    "timestamp": '$(date +%s)000',
    "userId": "test_user_123",
    "sessionId": "test_session_456",
    "platform": "web",
    "properties": {
      "test": true,
      "timestamp_readable": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'"
    }
  }'
```

**Expected Response**:
```json
{
  "success": true,
  "message": "Event ingested successfully",
  "eventName": "test_event",
  "timestamp": 1700000000000,
  "row": { ... }
}
```

### 4. Check BigQuery Table

```bash
# Check if table exists
bq show tripaiplanner:analytics.raw_events

# Count rows
bq query --use_legacy_sql=false \
  'SELECT COUNT(*) as count FROM `tripaiplanner.analytics.raw_events`'

# View recent events
bq query --use_legacy_sql=false \
  'SELECT 
    eventName,
    timestamp,
    userId,
    sessionId,
    platform,
    properties,
    _ingested_at
  FROM `tripaiplanner.analytics.raw_events`
  ORDER BY _ingested_at DESC
  LIMIT 10'

# Check for test events
bq query --use_legacy_sql=false \
  "SELECT * FROM \`tripaiplanner.analytics.raw_events\`
  WHERE eventName = 'test_event'
  ORDER BY _ingested_at DESC
  LIMIT 5"
```

### 5. Test Pub/Sub → Cloud Function Flow

```bash
# Publish a test message to Pub/Sub
gcloud pubsub topics publish analytics-events \
  --project=tripaiplanner \
  --message='{
    "eventName": "pubsub_test",
    "timestamp": '$(date +%s)000',
    "userId": "pubsub_test_user",
    "sessionId": "pubsub_test_session",
    "platform": "test",
    "properties": {
      "source": "gcloud_pubsub",
      "test": true
    }
  }'

# Wait 5 seconds
sleep 5

# Check if it appeared in BigQuery
bq query --use_legacy_sql=false \
  "SELECT * FROM \`tripaiplanner.analytics.raw_events\`
  WHERE eventName = 'pubsub_test'
  ORDER BY _ingested_at DESC
  LIMIT 1"
```

### 6. Check Pub/Sub Subscription

```bash
# List subscriptions
gcloud pubsub subscriptions list --project=tripaiplanner

# Check for dead letter messages
gcloud pubsub subscriptions pull gcf-analytics-ingestion-us-central1-analytics-events \
  --project=tripaiplanner \
  --limit=10 \
  --auto-ack
```

### 7. Test from Backend API

```bash
# Test the backend analytics endpoint
curl -X POST http://localhost:8080/api/v1/analytics/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventName": "backend_test",
    "timestamp": '$(date +%s)000',
    "userId": "backend_test_user",
    "sessionId": "backend_test_session",
    "platform": "web",
    "properties": {
      "source": "backend_api_test",
      "test": true
    }
  }'

# Check backend logs
# Look for "Published analytics event" message

# Wait 10 seconds for processing
sleep 10

# Check BigQuery
bq query --use_legacy_sql=false \
  "SELECT * FROM \`tripaiplanner.analytics.raw_events\`
  WHERE eventName = 'backend_test'
  ORDER BY _ingested_at DESC
  LIMIT 1"
```

## Common Issues & Solutions

### Issue 1: "Table not found"

**Cause**: BigQuery table doesn't exist

**Solution**:
```bash
cd bigquery
./setup.sh
```

### Issue 2: "Permission denied"

**Cause**: Cloud Function service account doesn't have BigQuery permissions

**Solution**:
```bash
# Get the service account
PROJECT_NUMBER=$(gcloud projects describe tripaiplanner --format='value(projectNumber)')
SERVICE_ACCOUNT="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

# Grant BigQuery Data Editor role
gcloud projects add-iam-policy-binding tripaiplanner \
  --member="serviceAccount:${SERVICE_ACCOUNT}" \
  --role="roles/bigquery.dataEditor"

# Grant BigQuery Job User role
gcloud projects add-iam-policy-binding tripaiplanner \
  --member="serviceAccount:${SERVICE_ACCOUNT}" \
  --role="roles/bigquery.jobUser"
```

### Issue 3: "PartialFailureError: invalid"

**Cause**: Schema mismatch - properties field is an object instead of JSON string

**Solution**: This is fixed in the new Cloud Function code. Redeploy:
```bash
cd cloud-functions/analytics-ingestion
./deploy.sh
```

### Issue 4: Events in Pub/Sub but not in BigQuery

**Cause**: Cloud Function not triggered or failing silently

**Solution**:
1. Check Cloud Function logs (see step 2 above)
2. Verify Pub/Sub subscription exists and is connected to function
3. Check for dead letter messages

```bash
# Check if subscription exists
gcloud pubsub subscriptions describe \
  gcf-analytics-ingestion-us-central1-analytics-events \
  --project=tripaiplanner

# If not, the function isn't properly connected to Pub/Sub
# Redeploy the function
```

### Issue 5: "properties" field is NULL in BigQuery

**Cause**: Properties not being converted to JSON string

**Solution**: The new Cloud Function code fixes this:
```javascript
properties: event.properties ? JSON.stringify(event.properties) : null
```

Redeploy the function to apply the fix.

## Verification Checklist

- [ ] Cloud Function `analytics-ingestion` exists and is ACTIVE
- [ ] Cloud Function has correct environment variables (BIGQUERY_DATASET, BIGQUERY_TABLE)
- [ ] Cloud Function service account has BigQuery permissions
- [ ] Pub/Sub topic `analytics-events` exists
- [ ] Pub/Sub subscription for Cloud Function exists
- [ ] BigQuery table `analytics.raw_events` exists
- [ ] Test HTTP endpoint returns success
- [ ] Test Pub/Sub message appears in BigQuery
- [ ] Backend API test appears in BigQuery
- [ ] Cloud Function logs show "Event ingested successfully"
- [ ] No errors in Cloud Function logs

## Debugging Commands

```bash
# Full diagnostic
echo "=== Cloud Function Status ==="
gcloud functions describe analytics-ingestion --region=us-central1 --project=tripaiplanner

echo -e "\n=== Recent Logs ==="
gcloud functions logs read analytics-ingestion --region=us-central1 --project=tripaiplanner --limit=20

echo -e "\n=== Pub/Sub Topic ==="
gcloud pubsub topics describe analytics-events --project=tripaiplanner

echo -e "\n=== BigQuery Table ==="
bq show tripaiplanner:analytics.raw_events

echo -e "\n=== Row Count ==="
bq query --use_legacy_sql=false 'SELECT COUNT(*) FROM `tripaiplanner.analytics.raw_events`'

echo -e "\n=== Recent Events ==="
bq query --use_legacy_sql=false 'SELECT eventName, timestamp, userId, _ingested_at FROM `tripaiplanner.analytics.raw_events` ORDER BY _ingested_at DESC LIMIT 5'
```

## Success Criteria

✅ Test HTTP endpoint returns `{"success": true}`  
✅ Event appears in BigQuery within 5 seconds  
✅ `properties` field contains JSON string (not NULL)  
✅ Cloud Function logs show "✅ Event ingested successfully"  
✅ No errors in Cloud Function logs  
✅ Backend API test event appears in BigQuery  

If all checks pass, the pipeline is working correctly! 🎉
