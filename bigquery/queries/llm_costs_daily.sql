-- LLM Token Usage and Cost Aggregation
-- Scheduled to run daily at 2 AM UTC
-- Tracks token consumption and estimated costs by provider, model, and agent

CREATE OR REPLACE TABLE `tripaiplanner-4c951.analytics.llm_costs_daily`
PARTITION BY date
CLUSTER BY date, provider
AS
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  
  -- Provider Breakdown
  JSON_EXTRACT_SCALAR(properties, '$.provider') as provider,
  JSON_EXTRACT_SCALAR(properties, '$.model') as model,
  JSON_EXTRACT_SCALAR(properties, '$.agentType') as agent_type,
  
  -- Token Metrics
  SUM(CAST(JSON_EXTRACT_SCALAR(properties, '$.promptTokens') AS INT64)) as total_prompt_tokens,
  SUM(CAST(JSON_EXTRACT_SCALAR(properties, '$.completionTokens') AS INT64)) as total_completion_tokens,
  SUM(CAST(JSON_EXTRACT_SCALAR(properties, '$.totalTokens') AS INT64)) as total_tokens,
  
  -- Cost Metrics
  SUM(CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64)) as total_cost_usd,
  AVG(CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64)) as avg_cost_per_request,
  
  -- Request Metrics
  COUNT(*) as request_count,
  
  -- Efficiency Metrics
  SAFE_DIVIDE(
    SUM(CAST(JSON_EXTRACT_SCALAR(properties, '$.totalTokens') AS INT64)),
    COUNT(*)
  ) as avg_tokens_per_request,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated

FROM `tripaiplanner-4c951.analytics.raw_events`
WHERE eventName = 'llm_token_usage'
  AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAYS)
GROUP BY date, provider, model, agent_type
ORDER BY date DESC, total_cost_usd DESC;

-- Daily summary (all providers combined)
CREATE OR REPLACE TABLE `tripaiplanner-4c951.analytics.llm_costs_daily_summary`
PARTITION BY date
CLUSTER BY date
AS
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  
  -- Total Metrics
  SUM(CAST(JSON_EXTRACT_SCALAR(properties, '$.promptTokens') AS INT64)) as total_prompt_tokens,
  SUM(CAST(JSON_EXTRACT_SCALAR(properties, '$.completionTokens') AS INT64)) as total_completion_tokens,
  SUM(CAST(JSON_EXTRACT_SCALAR(properties, '$.totalTokens') AS INT64)) as total_tokens,
  SUM(CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64)) as total_cost_usd,
  
  -- Provider Breakdown
  SUM(CASE 
    WHEN JSON_EXTRACT_SCALAR(properties, '$.provider') = 'gemini' 
    THEN CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64) 
    ELSE 0 
  END) as gemini_cost_usd,
  
  SUM(CASE 
    WHEN JSON_EXTRACT_SCALAR(properties, '$.provider') = 'openrouter' 
    THEN CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64) 
    ELSE 0 
  END) as openrouter_cost_usd,
  
  -- Request Count
  COUNT(*) as total_requests,
  
  -- Average Cost per Request
  AVG(CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64)) as avg_cost_per_request,
  
  -- Cost per Trip (if itineraryId is present)
  SAFE_DIVIDE(
    SUM(CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64)),
    COUNT(DISTINCT JSON_EXTRACT_SCALAR(properties, '$.itineraryId'))
  ) as avg_cost_per_trip,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated

FROM `tripaiplanner-4c951.analytics.raw_events`
WHERE eventName = 'llm_token_usage'
  AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAYS)
GROUP BY date
ORDER BY date DESC;

-- Monthly cost projection
CREATE OR REPLACE TABLE `tripaiplanner-4c951.analytics.llm_costs_monthly_projection`
AS
WITH daily_costs AS (
  SELECT
    DATE(TIMESTAMP_MILLIS(timestamp)) as date,
    SUM(CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64)) as daily_cost
  FROM `tripaiplanner-4c951.analytics.raw_events`
  WHERE eventName = 'llm_token_usage'
    AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_TRUNC(CURRENT_DATE(), MONTH)
  GROUP BY date
),
current_month_stats AS (
  SELECT
    FORMAT_DATE('%Y-%m', CURRENT_DATE()) as month,
    SUM(daily_cost) as current_cost,
    AVG(daily_cost) as avg_daily_cost,
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
    WHEN avg_daily_cost * days_in_month > 500 THEN 'CRITICAL'
    WHEN avg_daily_cost * days_in_month > 200 THEN 'WARNING'
    ELSE 'NORMAL'
  END as cost_status,
  CURRENT_TIMESTAMP() as last_updated
FROM current_month_stats;
