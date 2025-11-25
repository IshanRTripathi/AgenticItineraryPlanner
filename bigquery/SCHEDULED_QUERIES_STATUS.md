# BigQuery Scheduled Queries Status
**Date:** November 20, 2025  
**Project:** tripaiplanner  
**Location:** US

## 📊 Current Status

### ✅ Deployed Scheduled Queries: 1
| Display Name | Schedule | Status | Last Run | Query |
|--------------|----------|--------|----------|-------|
| Daily metrics | Every 1 hour | ✅ SUCCEEDED | 2025-11-20 18:01 UTC | daily_metrics.sql (old version) |

### ⚠️ Issues Found:
1. **Wrong Schedule:** Currently running every 1 hour instead of daily at 2 AM UTC
2. **Old Query:** Using an outdated version that includes CREATE OR REPLACE instead of just MERGE
3. **Missing Queries:** 5 other scheduled queries are not deployed

---

## 📋 Required Scheduled Queries

According to your deployment scripts, you should have **6 scheduled queries** running daily:

### 1. ✅ Daily Metrics Aggregation
- **File:** `daily_metrics.sql`
- **Status:** ⚠️ DEPLOYED (but needs update)
- **Schedule:** Should be `every day 02:00` (currently `every 1 hour`)
- **Description:** Aggregates core business metrics daily
- **Action Required:** Update schedule and query content

### 2. ❌ LLM Costs Daily
- **File:** `llm_costs_daily.sql`
- **Status:** ❌ NOT DEPLOYED
- **Schedule:** Should be `every day 02:00`
- **Description:** Tracks LLM token usage and costs by provider/model/agent
- **Action Required:** Deploy scheduled query

### 3. ❌ LLM Costs Daily Summary
- **File:** `llm_costs_daily_summary.sql`
- **Status:** ❌ NOT DEPLOYED
- **Schedule:** Should be `every day 02:00`
- **Description:** Aggregates all providers into daily totals
- **Action Required:** Deploy scheduled query

### 4. ❌ Funnel Metrics
- **File:** `funnel_metrics.sql`
- **Status:** ❌ NOT DEPLOYED
- **Schedule:** Should be `every day 02:00`
- **Description:** Analyzes conversion funnels and user journeys
- **Action Required:** Deploy scheduled query

### 5. ❌ User Engagement Daily
- **File:** `user_engagement_daily.sql`
- **Status:** ❌ NOT DEPLOYED
- **Schedule:** Should be `every day 02:00`
- **Description:** Tracks user engagement, retention, and activity metrics
- **Action Required:** Deploy scheduled query

### 6. ❌ Agent Performance Daily
- **File:** `agent_performance.sql`
- **Status:** ❌ NOT DEPLOYED
- **Schedule:** Should be `every day 02:00`
- **Description:** Monitors AI agent execution performance and latency
- **Action Required:** Deploy scheduled query

---

## 🔧 Recommended Actions

### Option 1: Deploy All at Once (Recommended)
Run the deployment script to create all scheduled queries:

```bash
cd bigquery/queries
bash deploy_scheduled_queries.sh
```

This will:
- Create/update all 6 scheduled queries
- Set them to run daily at 2 AM UTC
- Replace the existing "Daily metrics" query with the correct version

### Option 2: Manual Deployment
Deploy each query individually:

```bash
# 1. Daily Metrics
bq query --use_legacy_sql=false --schedule='every day 02:00' \
  --display_name='Daily Metrics Aggregation' \
  --replace=true < bigquery/queries/daily_metrics.sql

# 2. LLM Costs Daily
bq query --use_legacy_sql=false --schedule='every day 02:00' \
  --display_name='LLM Costs Daily' \
  --replace=true < bigquery/queries/llm_costs_daily.sql

# 3. LLM Costs Daily Summary
bq query --use_legacy_sql=false --schedule='every day 02:00' \
  --display_name='LLM Costs Daily Summary' \
  --replace=true < bigquery/queries/llm_costs_daily_summary.sql

# 4. Funnel Metrics
bq query --use_legacy_sql=false --schedule='every day 02:00' \
  --display_name='Funnel Metrics' \
  --replace=true < bigquery/queries/funnel_metrics.sql

# 5. User Engagement Daily
bq query --use_legacy_sql=false --schedule='every day 02:00' \
  --display_name='User Engagement Daily' \
  --replace=true < bigquery/queries/user_engagement_daily.sql

# 6. Agent Performance Daily
bq query --use_legacy_sql=false --schedule='every day 02:00' \
  --display_name='Agent Performance Daily' \
  --replace=true < bigquery/queries/agent_performance.sql
```

---

## 📅 Recommended Schedule

All queries should run **daily at 2 AM UTC** to process the previous day's data:

| Time (UTC) | Query | Purpose |
|------------|-------|---------|
| 02:00 | Daily Metrics | Core business metrics |
| 02:00 | LLM Costs Daily | Detailed LLM costs by provider/model |
| 02:00 | LLM Costs Daily Summary | Aggregated daily LLM costs |
| 02:00 | Funnel Metrics | Conversion funnel analysis |
| 02:00 | User Engagement | User activity and retention |
| 02:00 | Agent Performance | AI agent performance metrics |

**Note:** All queries can run at the same time (02:00) because they:
- Read from the same source (`raw_events`)
- Write to different destination tables
- Use MERGE operations (idempotent)
- Process the same date range (yesterday's data)

---

## 🔍 Verification Commands

### List all scheduled queries:
```bash
bq ls --transfer_config --transfer_location=us --project_id=tripaiplanner
```

### Get details of a specific scheduled query:
```bash
bq show --transfer_config --location=us <config_id>
```

### Manually trigger a scheduled query (for testing):
```bash
bq mk --transfer_run --run_time='2025-11-19T00:00:00Z' \
  projects/342690752571/locations/us/transferConfigs/<config_id>
```

### Check run history:
```bash
bq ls --transfer_run --transfer_location=us --max_results=10 <config_id>
```

---

## 📊 Expected Results After Deployment

Once all scheduled queries are deployed, you should see:
- ✅ 6 scheduled queries in BigQuery
- ✅ All running daily at 2 AM UTC
- ✅ All tables updated with yesterday's data each morning
- ✅ Consistent data across all analytics tables

---

## 🚨 Important Notes

1. **Idempotency:** All queries use MERGE operations, so they can be re-run safely
2. **Data Freshness:** Queries process yesterday's data (DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY))
3. **Cost:** Each query processes ~20-40KB of data (very efficient due to partitioning)
4. **Dependencies:** No dependencies between queries - they can all run in parallel
5. **Monitoring:** Check the transfer config status regularly to ensure queries are succeeding

---

**Next Step:** Run `bash bigquery/queries/deploy_scheduled_queries.sh` to deploy all scheduled queries.
