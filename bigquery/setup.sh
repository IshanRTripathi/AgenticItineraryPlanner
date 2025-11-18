#!/bin/bash

# Setup BigQuery infrastructure for analytics
# Creates dataset, tables, and scheduled queries

set -e

PROJECT_ID=${GCP_PROJECT_ID:-"tripaiplanner-4c951"}
DATASET_ID="analytics"
LOCATION="US"

echo "🔧 Setting up BigQuery Analytics Infrastructure..."
echo "   Project: $PROJECT_ID"
echo "   Dataset: $DATASET_ID"
echo "   Location: $LOCATION"
echo ""

# Create dataset
echo "📦 Creating BigQuery dataset..."
bq mk --dataset \
  --location=$LOCATION \
  --description="Analytics data warehouse for Agentic Itinerary Planner" \
  $PROJECT_ID:$DATASET_ID || echo "Dataset already exists"

# Create raw_events table with partitioning
echo "📊 Creating raw_events table..."
bq mk --table \
  --time_partitioning_field=timestamp \
  --time_partitioning_type=DAY \
  --clustering_fields=eventName,userId \
  --description="Raw analytics events from Pub/Sub" \
  --schema=schemas/raw_events.json \
  $PROJECT_ID:$DATASET_ID.raw_events || echo "Table already exists"

echo ""
echo "✅ BigQuery infrastructure created successfully!"
echo ""
echo "Next steps:"
echo "  1. Create Pub/Sub topic: gcloud pubsub topics create analytics-events"
echo "  2. Deploy Cloud Function: cd ../cloud-functions/analytics-ingestion && ./deploy.sh"
echo "  3. Create scheduled queries: Run SQL files in queries/ directory"
