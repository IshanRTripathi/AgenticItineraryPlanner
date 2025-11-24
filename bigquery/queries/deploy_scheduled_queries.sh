#!/bin/bash

# Deploy BigQuery Scheduled Queries
# Creates scheduled queries to run daily at 2 AM UTC

set -e

PROJECT_ID=${GCP_PROJECT_ID:-"tripaiplanner"}
LOCATION="US"

echo "🚀 Deploying BigQuery Scheduled Queries..."
echo "   Project: $PROJECT_ID"
echo "   Location: $LOCATION"
echo ""

# Function to create scheduled query
create_scheduled_query() {
  local name=$1
  local sql_file=$2
  local description=$3
  
  echo "📅 Creating scheduled query: $name"
  
  bq query \
    --project_id=$PROJECT_ID \
    --use_legacy_sql=false \
    --schedule='every day 02:00' \
    --location=$LOCATION \
    --display_name="$name" \
    --description="$description" \
    --replace=true \
    < "$sql_file"
  
  echo "   ✅ $name scheduled"
  echo ""
}

# Deploy each scheduled query
create_scheduled_query \
  "Daily Metrics Aggregation" \
  "daily_metrics.sql" \
  "Aggregates core business metrics daily"

create_scheduled_query \
  "LLM Costs Daily" \
  "llm_costs_daily.sql" \
  "Tracks LLM token usage and costs by provider/model/agent"

create_scheduled_query \
  "LLM Costs Daily Summary" \
  "llm_costs_daily_summary.sql" \
  "Aggregates all providers into daily totals"

create_scheduled_query \
  "Funnel Metrics" \
  "funnel_metrics.sql" \
  "Analyzes conversion funnels and user journeys"

create_scheduled_query \
  "User Engagement Daily" \
  "user_engagement_daily.sql" \
  "Tracks user engagement, retention, and activity metrics"

create_scheduled_query \
  "Agent Performance Daily" \
  "agent_performance.sql" \
  "Monitors AI agent execution performance and latency"

echo ""
echo "✅ All scheduled queries deployed successfully!"
echo ""
echo "Queries will run daily at 2 AM UTC"
echo ""
echo "To view scheduled queries:"
echo "  bq ls --transfer_config --project_id=$PROJECT_ID"
echo ""
echo "To manually trigger a query:"
echo "  bq mk --transfer_run --run_time='2024-01-01T00:00:00Z' <config_id>"
