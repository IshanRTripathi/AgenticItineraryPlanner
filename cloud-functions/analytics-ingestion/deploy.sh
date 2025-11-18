#!/bin/bash

# Deploy Analytics Ingestion Cloud Function
# This function processes Pub/Sub messages and inserts them into BigQuery

set -e

PROJECT_ID=${GCP_PROJECT_ID:-"tripaiplanner-4c951"}
REGION=${REGION:-"us-central1"}
FUNCTION_NAME="analytics-ingestion"
TOPIC_NAME="analytics-events"
DATASET_ID="analytics"
TABLE_ID="raw_events"

echo "🚀 Deploying Analytics Ingestion Cloud Function..."
echo "   Project: $PROJECT_ID"
echo "   Region: $REGION"
echo "   Topic: $TOPIC_NAME"
echo "   Dataset: $DATASET_ID"
echo "   Table: $TABLE_ID"
echo ""

gcloud functions deploy $FUNCTION_NAME \
  --gen2 \
  --runtime nodejs20 \
  --region $REGION \
  --source . \
  --entry-point ingestToBigQuery \
  --trigger-topic $TOPIC_NAME \
  --memory 256MB \
  --timeout 60s \
  --max-instances 10 \
  --set-env-vars BIGQUERY_DATASET=$DATASET_ID,BIGQUERY_TABLE=$TABLE_ID \
  --project $PROJECT_ID

echo ""
echo "✅ Cloud Function deployed successfully!"
echo ""
echo "Test with:"
echo "  gcloud pubsub topics publish $TOPIC_NAME --message '{\"eventName\":\"test_event\",\"timestamp\":$(date +%s)000}'"
