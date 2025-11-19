#!/bin/bash

# Setup BigQuery Scheduled Queries for Analytics Aggregation
# This script creates scheduled queries that run daily to aggregate raw events

PROJECT_ID="tripaiplanner"
DATASET="analytics"
LOCATION="us"

echo "Setting up BigQuery Scheduled Queries for project: $PROJECT_ID"
echo "Dataset: $DATASET"
echo ""

# 1. Daily Metrics - Runs daily at 1 AM
echo "Creating scheduled query: daily_metrics..."
bq query \
  --project_id=$PROJECT_ID \
  --use_legacy_sql=false \
  --display_name="Analytics - Daily Metrics" \
  --schedule="every day 01:00" \
  --destination_table="${PROJECT_ID}:${DATASET}.daily_metrics" \
  --replace=true \
  "$(cat queries/daily_metrics.sql)"

# 2. User Engagement Daily - Runs daily at 1:15 AM
echo "Creating scheduled query: user_engagement_daily..."
bq query \
  --project_id=$PROJECT_ID \
  --use_legacy_sql=false \
  --display_name="Analytics - User Engagement Daily" \
  --schedule="every day 01:15" \
  --destination_table="${PROJECT_ID}:${DATASET}.user_engagement_daily" \
  --replace=true \
  "$(cat queries/user_engagement_daily.sql)"

# 3. Funnel Metrics - Runs daily at 1:30 AM
echo "Creating scheduled query: funnel_metrics..."
bq query \
  --project_id=$PROJECT_ID \
  --use_legacy_sql=false \
  --display_name="Analytics - Funnel Metrics" \
  --schedule="every day 01:30" \
  --destination_table="${PROJECT_ID}:${DATASET}.funnel_metrics" \
  --replace=true \
  "$(cat queries/funnel_metrics.sql)"

# 4. LLM Costs Daily - Runs daily at 1:45 AM
echo "Creating scheduled query: llm_costs_daily..."
bq query \
  --project_id=$PROJECT_ID \
  --use_legacy_sql=false \
  --display_name="Analytics - LLM Costs Daily" \
  --schedule="every day 01:45" \
  --destination_table="${PROJECT_ID}:${DATASET}.llm_costs_daily" \
  --replace=true \
  "$(cat queries/llm_costs_daily.sql)"

# 5. Agent Performance - Runs daily at 2:00 AM
echo "Creating scheduled query: agent_performance..."
bq query \
  --project_id=$PROJECT_ID \
  --use_legacy_sql=false \
  --display_name="Analytics - Agent Performance" \
  --schedule="every day 02:00" \
  --destination_table="${PROJECT_ID}:${DATASET}.agent_performance" \
  --replace=true \
  "$(cat queries/agent_performance.sql)"

echo ""
echo "✅ All scheduled queries created successfully!"
echo ""
echo "To verify, run:"
echo "  bq ls --transfer_config --project_id=$PROJECT_ID"
echo ""
echo "To manually trigger a query for testing:"
echo "  bq mk --transfer_run --run_time='2024-01-01T00:00:00Z' projects/$PROJECT_ID/locations/$LOCATION/transferConfigs/[CONFIG_ID]"
