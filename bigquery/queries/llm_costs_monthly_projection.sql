-- LLM Costs Monthly Projection - SCHEDULED QUERY
-- Run daily at 2 AM UTC to update monthly cost projection
-- Projects end-of-month costs based on current usage

CREATE OR REPLACE TABLE `tripaiplanner.analytics.llm_costs_monthly_projection`
AS
WITH daily_costs AS (
  SELECT
    DATE(TIMESTAMP_MILLIS(timestamp)) as date,
    COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64)), 0) as daily_cost
  FROM `tripaiplanner.analytics.raw_events`
  WHERE eventName = 'llm_token_usage'
    AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_TRUNC(CURRENT_DATE(), MONTH)
  GROUP BY date
),
current_month_stats AS (
  SELECT
    FORMAT_DATE('%Y-%m', CURRENT_DATE()) as month,
    COALESCE(SUM(daily_cost), 0) as current_cost,
    COALESCE(AVG(daily_cost), 0) as avg_daily_cost,
    COUNT(*) as days_elapsed,
    EXTRACT(DAY FROM LAST_DAY(CURRENT_DATE())) as days_in_month
  FROM daily_costs
)
SELECT
  month,
  current_cost,
  avg_daily_cost,
  days_elapsed,
  days_in_month,
  days_in_month - days_elapsed as days_remaining,
  avg_daily_cost * days_in_month as projected_monthly_cost,
  CASE
    WHEN avg_daily_cost * days_in_month > 200 THEN 'CRITICAL'
    WHEN avg_daily_cost * days_in_month > 100 THEN 'WARNING'
    ELSE 'NORMAL'
  END as cost_status,
  CURRENT_TIMESTAMP() as last_updated
FROM current_month_stats;
