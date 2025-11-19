#!/bin/bash

# Run all BigQuery analytics queries manually
# This populates the dashboard tables immediately

PROJECT_ID="tripaiplanner"
DATASET="analytics"

echo "=========================================="
echo "Running All Analytics Queries"
echo "Project: $PROJECT_ID"
echo "Dataset: $DATASET"
echo "=========================================="
echo ""

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 1. Daily Metrics
echo "📊 Running: daily_metrics..."
bq query --project_id=$PROJECT_ID --use_legacy_sql=false < "$SCRIPT_DIR/queries/daily_metrics.sql"
if [ $? -eq 0 ]; then
    echo "✅ daily_metrics completed"
else
    echo "❌ daily_metrics failed"
fi
echo ""

# 2. User Engagement Daily
echo "👥 Running: user_engagement_daily..."
bq query --project_id=$PROJECT_ID --use_legacy_sql=false < "$SCRIPT_DIR/queries/user_engagement_daily.sql"
if [ $? -eq 0 ]; then
    echo "✅ user_engagement_daily completed"
else
    echo "❌ user_engagement_daily failed"
fi
echo ""

# 3. Funnel Metrics
echo "🔄 Running: funnel_metrics..."
bq query --project_id=$PROJECT_ID --use_legacy_sql=false < "$SCRIPT_DIR/queries/funnel_metrics.sql"
if [ $? -eq 0 ]; then
    echo "✅ funnel_metrics completed"
else
    echo "❌ funnel_metrics failed"
fi
echo ""

# 4. LLM Costs Daily
echo "💰 Running: llm_costs_daily..."
bq query --project_id=$PROJECT_ID --use_legacy_sql=false < "$SCRIPT_DIR/queries/llm_costs_daily.sql"
if [ $? -eq 0 ]; then
    echo "✅ llm_costs_daily completed"
else
    echo "❌ llm_costs_daily failed"
fi
echo ""

# 5. Agent Performance
echo "🤖 Running: agent_performance..."
bq query --project_id=$PROJECT_ID --use_legacy_sql=false < "$SCRIPT_DIR/queries/agent_performance.sql"
if [ $? -eq 0 ]; then
    echo "✅ agent_performance completed"
else
    echo "❌ agent_performance failed"
fi
echo ""

echo "=========================================="
echo "Verifying Results"
echo "=========================================="
echo ""

# Verify each table has data
echo "Checking row counts..."
echo ""

echo "daily_metrics:"
bq query --project_id=$PROJECT_ID --use_legacy_sql=false --format=pretty \
  "SELECT COUNT(*) as row_count FROM \`${PROJECT_ID}.${DATASET}.daily_metrics\`"
echo ""

echo "user_engagement_daily:"
bq query --project_id=$PROJECT_ID --use_legacy_sql=false --format=pretty \
  "SELECT COUNT(*) as row_count FROM \`${PROJECT_ID}.${DATASET}.user_engagement_daily\`"
echo ""

echo "funnel_metrics:"
bq query --project_id=$PROJECT_ID --use_legacy_sql=false --format=pretty \
  "SELECT COUNT(*) as row_count FROM \`${PROJECT_ID}.${DATASET}.funnel_metrics\`"
echo ""

echo "llm_costs_daily:"
bq query --project_id=$PROJECT_ID --use_legacy_sql=false --format=pretty \
  "SELECT COUNT(*) as row_count FROM \`${PROJECT_ID}.${DATASET}.llm_costs_daily\`"
echo ""

echo "agent_performance:"
bq query --project_id=$PROJECT_ID --use_legacy_sql=false --format=pretty \
  "SELECT COUNT(*) as row_count FROM \`${PROJECT_ID}.${DATASET}.agent_performance\`"
echo ""

echo "=========================================="
echo "✅ All queries completed!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Check Looker Studio dashboard for data"
echo "2. Set up scheduled queries: ./setup_scheduled_queries.sh"
echo "3. Monitor raw_events table for new data"
