-- Alert Metrics View - FIXED VERSION
-- Creates materialized views for Cloud Monitoring alerts
-- FIXED: Updated to use new table names (llm_requests_detailed, phase_performance_daily)

-- View 1: Current Daily LLM Cost
CREATE OR REPLACE VIEW `tripaiplanner.analytics.alert_llm_daily_cost` AS
WITH daily_cost AS (
  SELECT
    date,
    -- Calculate cost from tokens (Gemini 2.5 Flash pricing)
    (SUM(total_prompt_tokens) * 0.10 / 1000000) +
    (SUM(total_response_tokens) * 0.40 / 1000000) as total_cost_usd
  FROM `tripaiplanner.analytics.llm_requests_detailed`
  WHERE date = CURRENT_DATE()
  GROUP BY date
)
SELECT
  date,
  total_cost_usd as cost,
  CASE
    WHEN total_cost_usd > 15 THEN 'CRITICAL'
    WHEN total_cost_usd > 5 THEN 'WARNING'
    ELSE 'NORMAL'
  END as severity,
  CURRENT_TIMESTAMP() as check_time
FROM daily_cost
ORDER BY date DESC
LIMIT 1;

-- View 2: Current Monthly Projection
CREATE OR REPLACE VIEW `tripaiplanner.analytics.alert_llm_monthly_projection` AS
SELECT
  month,
  projected_monthly_cost as projected_cost,
  current_cost,
  avg_daily_cost,
  days_elapsed,
  days_remaining,
  CASE
    WHEN projected_monthly_cost > 200 THEN 'CRITICAL'
    WHEN projected_monthly_cost > 100 THEN 'WARNING'
    ELSE 'NORMAL'
  END as severity,
  CURRENT_TIMESTAMP() as check_time
FROM `tripaiplanner.analytics.llm_costs_monthly_projection`
LIMIT 1;

-- View 3: Phase Failure Rates (Replaces agent_performance)
CREATE OR REPLACE VIEW `tripaiplanner.analytics.alert_phase_failure_rates` AS
SELECT
  phase_name,
  100 - success_rate_percent as failure_rate,
  executions,
  failed_executions,
  CASE
    WHEN (100 - success_rate_percent) > 10 THEN 'CRITICAL'
    WHEN (100 - success_rate_percent) > 5 THEN 'WARNING'
    ELSE 'NORMAL'
  END as severity,
  CURRENT_TIMESTAMP() as check_time
FROM `tripaiplanner.analytics.phase_performance_daily`
WHERE date = CURRENT_DATE()
  AND (100 - success_rate_percent) > 0
ORDER BY failure_rate DESC;

-- View 4: Itinerary Failure Rate (NEW)
CREATE OR REPLACE VIEW `tripaiplanner.analytics.alert_itinerary_failure_rate` AS
SELECT
  date,
  100 - completion_rate_percent as failure_rate,
  itineraries_created,
  itineraries_completed,
  CASE
    WHEN (100 - completion_rate_percent) > 20 THEN 'CRITICAL'
    WHEN (100 - completion_rate_percent) > 10 THEN 'WARNING'
    ELSE 'NORMAL'
  END as severity,
  CURRENT_TIMESTAMP() as check_time
FROM `tripaiplanner.analytics.itinerary_metrics_daily`
WHERE date = CURRENT_DATE()
  AND itineraries_created > 0
ORDER BY date DESC
LIMIT 1;

-- View 5: Excessive Validation Warnings (NEW)
CREATE OR REPLACE VIEW `tripaiplanner.analytics.alert_validation_warnings` AS
SELECT
  date,
  total_validation_warnings,
  avg_warnings_per_itinerary,
  CASE
    WHEN avg_warnings_per_itinerary > 50 THEN 'CRITICAL'
    WHEN avg_warnings_per_itinerary > 20 THEN 'WARNING'
    ELSE 'NORMAL'
  END as severity,
  CURRENT_TIMESTAMP() as check_time
FROM `tripaiplanner.analytics.itinerary_metrics_daily`
WHERE date = CURRENT_DATE()
  AND itineraries_completed > 0
ORDER BY date DESC
LIMIT 1;

-- Scheduled Query: Check and Log Alerts
-- This query runs every 15 minutes and writes alerts to a table

CREATE TABLE IF NOT EXISTS `tripaiplanner.analytics.alert_checks`
PARTITION BY DATE(check_time)
AS
SELECT
  'llm_daily_cost' as alert_type,
  CAST(cost AS STRING) as value,
  severity,
  check_time,
  TO_JSON_STRING(STRUCT(
    date,
    cost
  )) as details
FROM `tripaiplanner.analytics.alert_llm_daily_cost`
WHERE severity IN ('WARNING', 'CRITICAL')

UNION ALL

SELECT
  'llm_monthly_projection' as alert_type,
  CAST(projected_cost AS STRING) as value,
  severity,
  check_time,
  TO_JSON_STRING(STRUCT(
    month,
    projected_cost,
    current_cost,
    days_remaining
  )) as details
FROM `tripaiplanner.analytics.alert_llm_monthly_projection`
WHERE severity IN ('WARNING', 'CRITICAL')

UNION ALL

SELECT
  'phase_failure_rate' as alert_type,
  CONCAT(phase_name, ': ', CAST(failure_rate AS STRING), '%') as value,
  severity,
  check_time,
  TO_JSON_STRING(STRUCT(
    phase_name,
    failure_rate,
    executions,
    failed_executions
  )) as details
FROM `tripaiplanner.analytics.alert_phase_failure_rates`
WHERE severity IN ('WARNING', 'CRITICAL')

UNION ALL

SELECT
  'itinerary_failure_rate' as alert_type,
  CAST(failure_rate AS STRING) as value,
  severity,
  check_time,
  TO_JSON_STRING(STRUCT(
    date,
    failure_rate,
    itineraries_created,
    itineraries_completed
  )) as details
FROM `tripaiplanner.analytics.alert_itinerary_failure_rate`
WHERE severity IN ('WARNING', 'CRITICAL')

UNION ALL

SELECT
  'validation_warnings' as alert_type,
  CAST(avg_warnings_per_itinerary AS STRING) as value,
  severity,
  check_time,
  TO_JSON_STRING(STRUCT(
    date,
    total_validation_warnings,
    avg_warnings_per_itinerary
  )) as details
FROM `tripaiplanner.analytics.alert_validation_warnings`
WHERE severity IN ('WARNING', 'CRITICAL');
