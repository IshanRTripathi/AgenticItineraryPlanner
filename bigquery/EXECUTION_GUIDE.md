# BigQuery Tables Execution Guide

## 📊 Overview

We have **14 SQL files** that create **7 aggregated tables** for comprehensive analytics.

## 🎯 Quick Start: Run These 6 Commands

```bash
cd bigquery/queries

# 1. Daily Business Metrics
bq query --use_legacy_sql=false < daily_metrics_create.sql

# 2. LLM Costs (Detailed)
bq query --use_legacy_sql=false < llm_costs_daily_create.sql

# 3. LLM Costs (Summary)
bq query --use_legacy_sql=false < llm_costs_daily_summary_create.sql

# 4. Conversion Funnels
bq query --use_legacy_sql=false < funnel_metrics_create.sql

# 5. User Engagement
bq query --use_legacy_sql=false < user_engagement_daily_create.sql

# 6. Agent Performance
bq query --use_legacy_sql=false < agent_performance_create.sql
```

**Time**: ~2 minutes  
**Result**: 6 new tables with last 90 days of data

---

## 📋 Detailed Breakdown

### Table 1: `daily_metrics` - Core Business Metrics

**Purpose**: Daily KPIs for business tracking

**Metrics**:
- DAU (Daily Active Users)
- New signups
- Daily logins
- Trip creation funnel (starts → completed)
- Booking funnel (initiated → completed)
- PDF exports
- Public links created
- Page views & sessions
- Revenue
- Conversion rates

**Files**:
- `daily_metrics_create.sql` - Run ONCE to create table
- `daily_metrics.sql` - Schedule daily (updates yesterday's data)

**Command**:
```bash
bq query --use_legacy_sql=false < daily_metrics_create.sql
```

**Verify**:
```bash
bq query --use_legacy_sql=false \
  "SELECT * FROM \`tripaiplanner.analytics.daily_metrics\` 
   ORDER BY date DESC LIMIT 7"
```

---

### Table 2: `llm_costs_daily` - Detailed LLM Costs

**Purpose**: Track token usage and costs by provider/model/agent

**Metrics**:
- Total tokens (prompt + completion)
- Cost in USD
- Request count
- Average tokens per request
- Breakdown by:
  - Provider (Gemini, OpenRouter)
  - Model (gemini-2.5-flash, qwen-2.5-72b, etc.)
  - Agent type (SkeletonAgent, PopulationAgent, etc.)

**Files**:
- `llm_costs_daily_create.sql` - Run ONCE
- `llm_costs_daily.sql` - Schedule daily

**Command**:
```bash
bq query --use_legacy_sql=false < llm_costs_daily_create.sql
```

**Verify**:
```bash
bq query --use_legacy_sql=false \
  "SELECT date, provider, model, total_cost_usd, request_count 
   FROM \`tripaiplanner.analytics.llm_costs_daily\` 
   ORDER BY date DESC, total_cost_usd DESC 
   LIMIT 10"
```

---

### Table 3: `llm_costs_daily_summary` - Daily Cost Totals

**Purpose**: Aggregated daily LLM costs (all providers combined)

**Metrics**:
- Total tokens
- Total cost (USD)
- Cost by provider (Gemini vs OpenRouter)
- Average cost per request
- Average cost per trip

**Files**:
- `llm_costs_daily_summary_create.sql` - Run ONCE
- `llm_costs_daily_summary.sql` - Schedule daily

**Command**:
```bash
bq query --use_legacy_sql=false < llm_costs_daily_summary_create.sql
```

**Verify**:
```bash
bq query --use_legacy_sql=false \
  "SELECT date, total_cost_usd, gemini_cost_usd, openrouter_cost_usd, avg_cost_per_trip 
   FROM \`tripaiplanner.analytics.llm_costs_daily_summary\` 
   ORDER BY date DESC 
   LIMIT 7"
```

---

### Table 4: `llm_costs_monthly_projection` - Cost Forecast

**Purpose**: Project end-of-month costs based on current usage

**Metrics**:
- Current month cost
- Average daily cost
- Days elapsed/remaining
- Projected monthly cost
- Cost status (NORMAL/WARNING/CRITICAL)

**Files**:
- `llm_costs_monthly_projection.sql` - Run daily (no _create file needed)

**Command**:
```bash
bq query --use_legacy_sql=false < llm_costs_monthly_projection.sql
```

**Verify**:
```bash
bq query --use_legacy_sql=false \
  "SELECT * FROM \`tripaiplanner.analytics.llm_costs_monthly_projection\`"
```

---

### Table 5: `funnel_metrics` - Conversion Funnels

**Purpose**: Track user journey through key conversion funnels

**Metrics**:
- Trip creation funnel:
  - Wizard started → Initiated → Completed
  - Conversion rates at each step
- Booking funnel:
  - Initiated → Completed
- Payment funnel:
  - Initiated → Completed
- Export funnel:
  - Initiated → Completed

**Files**:
- `funnel_metrics_create.sql` - Run ONCE
- `funnel_metrics.sql` - Schedule daily

**Command**:
```bash
bq query --use_legacy_sql=false < funnel_metrics_create.sql
```

**Verify**:
```bash
bq query --use_legacy_sql=false \
  "SELECT date, 
          funnel_trip_wizard_started,
          funnel_trip_completed,
          trip_overall_conversion_rate,
          booking_conversion_rate
   FROM \`tripaiplanner.analytics.funnel_metrics\` 
   ORDER BY date DESC 
   LIMIT 7"
```

---

### Table 6: `user_engagement_daily` - Engagement Metrics

**Purpose**: Track user activity and feature usage

**Metrics**:
- DAU, total sessions
- Average events per session
- Page views per user
- Feature usage:
  - Users viewing activities
  - Users expanding days
  - Users using chat
  - Users searching
  - Users exporting PDFs
  - Users creating links
- Engagement score (composite metric)

**Files**:
- `user_engagement_daily_create.sql` - Run ONCE
- `user_engagement_daily.sql` - Schedule daily

**Command**:
```bash
bq query --use_legacy_sql=false < user_engagement_daily_create.sql
```

**Verify**:
```bash
bq query --use_legacy_sql=false \
  "SELECT date, dau, total_sessions, avg_page_views_per_user, engagement_score 
   FROM \`tripaiplanner.analytics.user_engagement_daily\` 
   ORDER BY date DESC 
   LIMIT 7"
```

---

### Table 7: `agent_performance_daily` - AI Agent Metrics

**Purpose**: Track AI agent execution performance

**Metrics**:
- Executions (started/completed/failed)
- Success rate (%)
- Latency percentiles (p50, p95, p99)
- Breakdown by agent type

**Files**:
- `agent_performance_create.sql` - Run ONCE
- `agent_performance.sql` - Schedule daily

**Command**:
```bash
bq query --use_legacy_sql=false < agent_performance_create.sql
```

**Verify**:
```bash
bq query --use_legacy_sql=false \
  "SELECT date, agent_type, success_rate, avg_duration_ms, p95_duration_ms 
   FROM \`tripaiplanner.analytics.agent_performance_daily\` 
   ORDER BY date DESC, executions_started DESC 
   LIMIT 10"
```

---

## 🔄 Scheduling Daily Updates

After creating tables, schedule these queries to run daily at 2 AM UTC:

```bash
cd bigquery/queries

# Schedule all daily updates
bq query --use_legacy_sql=false \
  --display_name='Daily Metrics Update' \
  --schedule='every day 02:00' \
  --replace=true \
  < daily_metrics.sql

bq query --use_legacy_sql=false \
  --display_name='LLM Costs Daily Update' \
  --schedule='every day 02:00' \
  --replace=true \
  < llm_costs_daily.sql

bq query --use_legacy_sql=false \
  --display_name='LLM Costs Summary Update' \
  --schedule='every day 02:00' \
  --replace=true \
  < llm_costs_daily_summary.sql

bq query --use_legacy_sql=false \
  --display_name='LLM Costs Monthly Projection' \
  --schedule='every day 02:00' \
  --replace=true \
  < llm_costs_monthly_projection.sql

bq query --use_legacy_sql=false \
  --display_name='Funnel Metrics Update' \
  --schedule='every day 02:00' \
  --replace=true \
  < funnel_metrics.sql

bq query --use_legacy_sql=false \
  --display_name='User Engagement Update' \
  --schedule='every day 02:00' \
  --replace=true \
  < user_engagement_daily.sql

bq query --use_legacy_sql=false \
  --display_name='Agent Performance Update' \
  --schedule='every day 02:00' \
  --replace=true \
  < agent_performance.sql
```

**Verify Scheduled Queries**:
```bash
bq ls --transfer_config --project_id=tripaiplanner
```

---

## 📊 What Each Table Gives You

### For Business Dashboard:
- `daily_metrics` - DAU, signups, trips, bookings, revenue
- `funnel_metrics` - Conversion rates

### For Cost Monitoring:
- `llm_costs_daily` - Detailed breakdown
- `llm_costs_daily_summary` - Daily totals
- `llm_costs_monthly_projection` - Budget tracking

### For Product Analytics:
- `user_engagement_daily` - Feature usage
- `funnel_metrics` - User journey

### For Engineering:
- `agent_performance_daily` - AI performance
- `llm_costs_daily` - Token efficiency

---

## 🎨 Looker Studio Data Sources

Once tables are created, connect them to Looker Studio:

1. Go to: https://lookerstudio.google.com/
2. Create new report
3. Add data source → BigQuery
4. Select project: `tripaiplanner`
5. Select dataset: `analytics`
6. Add these tables:
   - ✅ `daily_metrics`
   - ✅ `llm_costs_daily_summary`
   - ✅ `llm_costs_monthly_projection`
   - ✅ `funnel_metrics`
   - ✅ `user_engagement_daily`
   - ✅ `agent_performance_daily`

---

## ✅ Verification Checklist

After running all create scripts:

```bash
# Check all tables exist
bq ls tripaiplanner:analytics

# Expected tables:
# - raw_events
# - daily_metrics
# - llm_costs_daily
# - llm_costs_daily_summary
# - llm_costs_monthly_projection
# - funnel_metrics
# - user_engagement_daily
# - agent_performance_daily

# Check row counts
bq query --use_legacy_sql=false \
  "SELECT 
    (SELECT COUNT(*) FROM \`tripaiplanner.analytics.daily_metrics\`) as daily_metrics,
    (SELECT COUNT(*) FROM \`tripaiplanner.analytics.llm_costs_daily\`) as llm_costs,
    (SELECT COUNT(*) FROM \`tripaiplanner.analytics.funnel_metrics\`) as funnels,
    (SELECT COUNT(*) FROM \`tripaiplanner.analytics.user_engagement_daily\`) as engagement,
    (SELECT COUNT(*) FROM \`tripaiplanner.analytics.agent_performance_daily\`) as agents"
```

---

## 🚨 Important Notes

### File Naming Convention:
- `*_create.sql` - Run ONCE to create table with historical data (90 days)
- `*.sql` - Schedule DAILY to update with yesterday's data

### Don't Run These Directly:
- `alert_metrics.sql` - Creates views for monitoring (optional)
- `deploy_scheduled_queries.sh` - Automated deployment script (optional)

### Data Availability:
- Tables populate with last 90 days of data from `raw_events`
- If `raw_events` is empty, tables will be empty
- Send test events first to see data

---

## 💡 Quick Test

Want to see if it's working? Run this after creating tables:

```bash
# Check if you have data
bq query --use_legacy_sql=false \
  "SELECT 
    'raw_events' as table_name,
    COUNT(*) as row_count,
    MIN(DATE(TIMESTAMP_MILLIS(timestamp))) as earliest_date,
    MAX(DATE(TIMESTAMP_MILLIS(timestamp))) as latest_date
   FROM \`tripaiplanner.analytics.raw_events\`
   
   UNION ALL
   
   SELECT 
    'daily_metrics' as table_name,
    COUNT(*) as row_count,
    MIN(date) as earliest_date,
    MAX(date) as latest_date
   FROM \`tripaiplanner.analytics.daily_metrics\`"
```

If `raw_events` has data but aggregated tables are empty, the queries need to run (either manually or wait for scheduled time).

---

**Last Updated**: November 19, 2025  
**Status**: Ready to Execute  
**Estimated Time**: 2-3 minutes for all tables
