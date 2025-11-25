#!/bin/bash

# Deploy Analytics Ingestion Cloud Function
# This script deploys the Pub/Sub-triggered function for ingesting analytics events

set -e

PROJECT_ID="tripaiplanner"
REGION="us-south1"
FUNCTION_NAME="analytics-ingestion"
TOPIC_NAME="analytics-events"
DATASET_ID="analytics"
TABLE_ID="raw_events"

echo "🚀 Deploying Analytics Ingestion Cloud Function..."
echo "   Project: $PROJECT_ID"
echo "   Region: $REGION"
echo "   Function: $FUNCTION_NAME"
echo "   Topic: $TOPIC_NAME"
echo ""

# Deploy the Pub/Sub-triggered function
gcloud functions deploy $FUNCTION_NAME \
  --gen2 \
  --runtime nodejs20 \
  --trigger-topic $TOPIC_NAME \
  --entry-point ingestToBigQuery \
  --region $REGION \
  --memory 256MB \
  --timeout 60s \
  --update-env-vars="BIGQUERY_DATASET=$DATASET_ID,BIGQUERY_TABLE=$TABLE_ID" \
  --project $PROJECT_ID \
  --quiet

echo ""
echo "✅ Pub/Sub-triggered function deployed successfully!"
echo ""

# Also deploy the HTTP test function
echo "🚀 Deploying HTTP Test Function..."
gcloud functions deploy ${FUNCTION_NAME}-test \
  --gen2 \
  --runtime nodejs20 \
  --trigger-http \
  --allow-unauthenticated \
  --entry-point testIngest \
  --region $REGION \
  --memory 256MB \
  --timeout 60s \
  --update-env-vars="BIGQUERY_DATASET=$DATASET_ID,BIGQUERY_TABLE=$TABLE_ID" \
  --project $PROJECT_ID \
  --quiet

echo ""
echo "✅ HTTP test function deployed successfully!"
echo ""

# Get the HTTP function URL
FUNCTION_URL=$(gcloud functions describe ${FUNCTION_NAME}-test \
  --region=$REGION \
  --project=$PROJECT_ID \
  --gen2 \
  --format='value(serviceConfig.uri)')

echo "📝 Test the HTTP endpoint:"
echo "   curl -X POST \"$FUNCTION_URL\" \\"
echo "     -H \"Content-Type: application/json\" \\"
echo "     -d '{\"eventName\":\"test\",\"timestamp\":'$(date +%s)000',\"userId\":\"test\",\"sessionId\":\"test\",\"platform\":\"web\",\"properties\":{\"test\":true}}'"
echo ""

echo "📝 View logs:"
echo "   gcloud functions logs read $FUNCTION_NAME --region=$REGION --project=$PROJECT_ID --limit=50"
echo ""

echo "📝 Test Pub/Sub flow:"
echo "   gcloud pubsub topics publish $TOPIC_NAME --project=$PROJECT_ID --message='{\"eventName\":\"test\",\"timestamp\":'$(date +%s)000',\"userId\":\"test\",\"sessionId\":\"test\",\"platform\":\"test\",\"properties\":{\"test\":true}}'"
echo ""

echo "✅ Deployment complete!"
