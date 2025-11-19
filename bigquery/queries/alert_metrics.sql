-- Alert Metrics View
-- Creates materialized views for Cloud Monitoring alerts
-- These views are queried by scheduled queries that write to Cloud Logging

-- View 1: Current Daily LLM Cost
CREATE OR REPLACE VIEW `tripaiplanner.analytics.alert_llm_daily_cost` AS
SELECT
  date,
  total_cost_usd as cost,
  CASE
    WHEN total_cost_usd > 15 THEN 'CRITICAL'
    WHEN total_cost_usd > 5 THEN 'WARNING'
    ELSE 'NORMAL'
  END as severity,
  CURRENT_TIMESTAMP() as check_time
FROM `tripaiplanner.analytics.llm_costs_daily_summary`
WHERE date = CURRENT_DATE()
ORDER BY date DESC
LIMIT 1;

-- View 2: Current Monthly Projection
CREATE OR REPLACE VIEW `tripaiplanner.analytics.alert_llm_monthly_projection` AS
SELECT
  month,
  projected_monthly_cost as projected_cost,
  current_cost,
  days_elapsed,
  days_remaining,
  CASE
    WHEN projected_monthly_cost > 200 THEN 'CRITICAL'
    WHEN projected_monthly_cost > 100 THEN 'WARNING'
    ELSE 'NORMAL'
  END as severity,
  CURRENT_TIMESTAMP() as check_time
FROM `tripaiplanner.analytics.llm_costs_monthly_projection`
ORDER BY month DESC
LIMIT 1;

-- View 3: Agent Failure Rates (Last Hour)
CREATE OR REPLACE VIEW `tripaiplanner.analytics.alert_agent_failure_rates` AS
SELECT
  agent_type,
  100 - success_rate as failure_rate,
  executions_started,
  executions_failed,
  CASE
    WHEN (100 - success_rate) > 10 THEN 'CRITICAL'
    WHEN (100 - success_rate) > 5 THEN 'WARNING'
    ELSE 'NORMAL'
  END as severity,
  CURRENT_TIMESTAMP() as check_time
FROM `tripaiplanner.analytics.agent_performance_daily`
WHERE date = CURRENT_DATE()
  AND (100 - success_rate) > 0
ORDER BY failure_rate DESC;

-- View 4: Booking Failure Rate (Last Hour)
CREATE OR REPLACE VIEW `tripaiplanner.analytics.alert_booking_failure_rate` AS
WITH recent_bookings AS (
  SELECT
    COUNT(CASE WHEN eventName = 'booking_initiated' THEN 1 END) as initiated,
    COUNT(CASE WHEN eventName = 'booking_failed' THEN 1 END) as failed
  FROM `tripaiplanner.analytics.raw_events`
  WHERE DATE(TIMESTAMP_MILLIS(timestamp)) = CURRENT_DATE()
    AND TIMESTAMP_MILLIS(timestamp) >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 1 HOUR)
    AND eventName IN ('booking_initiated', 'booking_failed')
)
SELECT
  SAFE_DIVIDE(failed, initiated) * 100 as failure_rate_percent,
  initiated,
  failed,
  CASE
    WHEN SAFE_DIVIDE(failed, initiated) * 100 > 20 THEN 'CRITICAL'
    WHEN SAFE_DIVIDE(failed, initiated) * 100 > 10 THEN 'WARNING'
    ELSE 'NORMAL'
  END as severity,
  CURRENT_TIMESTAMP() as check_time
FROM recent_bookings
WHERE initiated > 0;

-- Scheduled Query: Check and Log Alerts
-- This query runs every 15 minutes and writes to Cloud Logging
-- Cloud Monitoring can then alert based on these log entries

-- To deploy as scheduled query:
-- bq query --use_legacy_sql=false --schedule='every 15 minutes' \
--   --display_name='Alert Metrics Check' \
--   --destination_table='analytics.alert_checks' \
--   --replace=true < alert_metrics_check.sql

CREATE OR REPLACE TABLE `tripaiplanner.analytics.alert_checks`
PARTITION BY DATE(check_time)
AS
SELECT
  'llm_daily_cost' as alert_type,
  CAST(cost AS STRING) as value,
  severity,
  check_time,
  STRUCT(
    date,
    cost
  ) as details
FROM `tripaiplanner.analytics.alert_llm_daily_cost`
WHERE severity IN ('WARNING', 'CRITICAL')

UNION ALL

SELECT
  'llm_monthly_projection' as alert_type,
  CAST(projected_cost AS STRING) as value,
  severity,
  check_time,
  STRUCT(
    month,
    projected_cost,
    current_cost,
    days_remaining
  ) as details
FROM `tripaiplanner.analytics.alert_llm_monthly_projection`
WHERE severity IN ('WARNING', 'CRITICAL')

UNION ALL

SELECT
  'agent_failure_rate' as alert_type,
  CONCAT(agent_type, ': ', CAST(failure_rate AS STRING), '%') as value,
  severity,
  check_time,
  STRUCT(
    agent_type,
    failure_rate,
    executions_started,
    executions_failed
  ) as details
FROM `tripaiplanner.analytics.alert_agent_failure_rates`
WHERE severity IN ('WARNING', 'CRITICAL')

UNION ALL

SELECT
  'booking_failure_rate' as alert_type,
  CAST(failure_rate_percent AS STRING) as value,
  severity,
  check_time,
  STRUCT(
    failure_rate_percent,
    initiated,
    failed
  ) as details
FROM `tripaiplanner.analytics.alert_booking_failure_rate`
WHERE severity IN ('WARNING', 'CRITICAL');
