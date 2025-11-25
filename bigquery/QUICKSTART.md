# Analytics BigQuery Setup - Quick Start

## Problem
Your Cloud Function is writing raw events to `raw_events` table, but the dashboard tables (daily_metrics, funnel_metrics, etc.) are empty.

## Solution
Set up BigQuery Scheduled Queries to aggregate raw events daily.

## Steps

### 1. Make the setup script executable
```bash
chmod +x bigquery/setup_scheduled_queries.sh
```

### 2. Run the setup script
```bash
cd bigquery
./setup_scheduled_queries.sh
```

This will create 5 scheduled queries that run daily:
- **01:00 AM** - Daily Metrics
- **01:15 AM** - User Engagement
- **01:30 AM** - Funnel Metrics
- **01:45 AM** - LLM Costs
- **02:00 AM** - Agent Performance

### 3. Manually trigger for immediate results (optional)
```bash
# List scheduled queries to get CONFIG_ID
bq ls --transfer_config --project_id=tripaiplanner

# Trigger a specific query
bq mk --transfer_run --run_time='2024-01-01T00:00:00Z' \
  projects/tripaiplanner/locations/us/transferConfigs/[CONFIG_ID]
```

### 4. Or run queries manually for immediate results
```bash
# Run each query manually
bq query --use_legacy_sql=false < queries/daily_metrics.sql
bq query --use_legacy_sql=false < queries/user_engagement_daily.sql
bq query --use_legacy_sql=false < queries/funnel_metrics.sql
bq query --use_legacy_sql=false < queries/llm_costs_daily.sql
bq query --use_legacy_sql=false < queries/agent_performance.sql
```

### 5. Verify data
```bash
# Check if tables have data
bq query --use_legacy_sql=false "SELECT COUNT(*) as count FROM \`tripaiplanner.analytics.daily_metrics\`"
bq query --use_legacy_sql=false "SELECT COUNT(*) as count FROM \`tripaiplanner.analytics.funnel_metrics\`"
```

## Architecture

```
Frontend/Backend Events
        ↓
    Pub/Sub Topic (analytics-events)
        ↓
    Cloud Function (analytics-ingestion)
        ↓
    BigQuery raw_events table ✅ WORKING
        ↓
    Scheduled Queries (daily at 1-2 AM) ⚠️ NEEDS SETUP
        ↓
    Dashboard Tables (daily_metrics, funnel_metrics, etc.)
        ↓
    Looker Studio Dashboard
```

## Why Scheduled Queries?

1. **Cost-effective**: Runs once daily, not on every event
2. **Efficient**: Batch processing is faster than real-time aggregation
3. **Maintainable**: SQL queries are easy to update
4. **Scalable**: BigQuery handles large data volumes
5. **No additional infrastructure**: Uses built-in BigQuery features

## Alternative: Real-time Aggregation

If you need real-time dashboard updates, you can:
1. Modify the Cloud Function to also write to aggregated tables
2. Use BigQuery Streaming Inserts with MERGE statements
3. Use Dataflow for real-time aggregation

But for most analytics use cases, daily aggregation is sufficient and much cheaper.
