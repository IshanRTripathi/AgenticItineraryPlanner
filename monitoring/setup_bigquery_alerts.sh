#!/bin/bash

# Setup Cloud Monitoring Alerts Based on BigQuery Data
# This approach uses BigQuery scheduled queries + log-based metrics
# No native client libraries needed - works perfectly in Cloud Run

set -e

PROJECT_ID=${GCP_PROJECT_ID:-"tripaiplanner"}

echo "🔔 Setting up BigQuery-based Cloud Monitoring Alerts..."
echo "   Project: $PROJECT_ID"
echo ""

# Step 1: Create log-based metrics from BigQuery query results
echo "📊 Creating log-based metrics..."

# Metric 1: Daily LLM Cost
gcloud logging metrics create llm_daily_cost \
  --project=$PROJECT_ID \
  --description="Daily LLM API cost in USD" \
  --value-extractor='EXTRACT(jsonPayload.cost)' \
  --log-filter='resource.type="bigquery_dataset"
    AND jsonPayload.query_name="llm_costs_daily_summary"
    AND jsonPayload.cost > 0' \
  || echo "Metric llm_daily_cost already exists"

# Metric 2: Agent Failure Rate
gcloud logging metrics create agent_failure_rate \
  --project=$PROJECT_ID \
  --description="Agent failure rate percentage" \
  --value-extractor='EXTRACT(jsonPayload.failure_rate)' \
  --log-filter='resource.type="bigquery_dataset"
    AND jsonPayload.query_name="agent_performance_daily"
    AND jsonPayload.failure_rate > 0' \
  || echo "Metric agent_failure_rate already exists"

echo ""
echo "✅ Log-based metrics created"
echo ""

# Step 2: Create alert policies using gcloud
echo "🚨 Creating alert policies..."

# Alert 1: Daily LLM Cost > $20
gcloud alpha monitoring policies create \
  --project=$PROJECT_ID \
  --notification-channels="" \
  --display-name="Daily LLM Cost Warning" \
  --condition-display-name="Daily cost exceeds $20" \
  --condition-threshold-value=20 \
  --condition-threshold-duration=3600s \
  --condition-threshold-filter='metric.type="logging.googleapis.com/user/llm_daily_cost"
    resource.type="global"' \
  || echo "Alert policy already exists"

# Alert 2: Agent Failure Rate > 5%
gcloud alpha monitoring policies create \
  --project=$PROJECT_ID \
  --notification-channels="" \
  --display-name="Agent Failure Rate High" \
  --condition-display-name="Failure rate exceeds 5%" \
  --condition-threshold-value=5 \
  --condition-threshold-duration=900s \
  --condition-threshold-filter='metric.type="logging.googleapis.com/user/agent_failure_rate"
    resource.type="global"' \
  || echo "Alert policy already exists"

echo ""
echo "✅ Alert policies created"
echo ""

echo "📝 Next steps:"
echo "  1. Configure notification channels in Cloud Console"
echo "  2. Link notification channels to alert policies"
echo "  3. Test alerts by generating high-cost LLM requests"
echo ""
echo "View alerts: https://console.cloud.google.com/monitoring/alerting?project=$PROJECT_ID"
