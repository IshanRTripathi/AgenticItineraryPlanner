-- LLM Costs Daily Table Creation - RUN ONCE
-- Creates the llm_costs_daily table with initial data from last 90 days
-- After running this, use llm_costs_daily.sql as a scheduled query

CREATE OR REPLACE TABLE `tripaiplanner.analytics.llm_costs_daily`
PARTITION BY date
CLUSTER BY date, provider, model
AS
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  
  -- Provider Breakdown
  JSON_EXTRACT_SCALAR(properties, '$.provider') as provider,
  JSON_EXTRACT_SCALAR(properties, '$.model') as model,
  JSON_EXTRACT_SCALAR(properties, '$.agentType') as agent_type,
  
  -- Token Metrics with SAFE_CAST to handle invalid JSON
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.promptTokens') AS INT64)), 0) as total_prompt_tokens,
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.completionTokens') AS INT64)), 0) as total_completion_tokens,
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalTokens') AS INT64)), 0) as total_tokens,
  
  -- Cost Metrics with SAFE_CAST
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64)), 0) as total_cost_usd,
  COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64)), 0) as avg_cost_per_request,
  
  -- Request Metrics
  COUNT(*) as request_count,
  
  -- Efficiency Metrics
  SAFE_DIVIDE(
    COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalTokens') AS INT64)), 0),
    COUNT(*)
  ) as avg_tokens_per_request,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated

FROM `tripaiplanner.analytics.raw_events`
WHERE eventName = 'llm_token_usage'
  AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
GROUP BY date, provider, model, agent_type;
