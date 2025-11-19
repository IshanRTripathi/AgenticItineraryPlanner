-- LLM Costs Daily Summary Table Creation - RUN ONCE
-- Creates the summary table with initial data from last 90 days

CREATE OR REPLACE TABLE `tripaiplanner.analytics.llm_costs_daily_summary`
PARTITION BY date
CLUSTER BY date
AS
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.promptTokens') AS INT64)), 0) as total_prompt_tokens,
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.completionTokens') AS INT64)), 0) as total_completion_tokens,
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalTokens') AS INT64)), 0) as total_tokens,
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64)), 0) as total_cost_usd,
  COALESCE(SUM(CASE 
    WHEN JSON_EXTRACT_SCALAR(properties, '$.provider') = 'gemini' 
    THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64) 
    ELSE 0 
  END), 0) as gemini_cost_usd,
  COALESCE(SUM(CASE 
    WHEN JSON_EXTRACT_SCALAR(properties, '$.provider') = 'openrouter' 
    THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64) 
    ELSE 0 
  END), 0) as openrouter_cost_usd,
  COUNT(*) as total_requests,
  COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64)), 0) as avg_cost_per_request,
  SAFE_DIVIDE(
    COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64)), 0),
    COUNT(DISTINCT JSON_EXTRACT_SCALAR(properties, '$.itineraryId'))
  ) as avg_cost_per_trip,
  CURRENT_TIMESTAMP() as last_updated
FROM `tripaiplanner.analytics.raw_events`
WHERE eventName = 'llm_token_usage'
  AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
GROUP BY date;
