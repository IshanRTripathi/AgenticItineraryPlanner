-- LLM Costs Daily Summary - SCHEDULED QUERY
-- Run daily at 2 AM UTC to update yesterday's summary
-- Aggregates all providers into daily totals

MERGE `tripaiplanner.analytics.llm_costs_daily_summary` T
USING (
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
    AND DATE(TIMESTAMP_MILLIS(timestamp)) = DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY)
  GROUP BY date
) S
ON T.date = S.date
WHEN MATCHED THEN
  UPDATE SET
    total_prompt_tokens = S.total_prompt_tokens,
    total_completion_tokens = S.total_completion_tokens,
    total_tokens = S.total_tokens,
    total_cost_usd = S.total_cost_usd,
    gemini_cost_usd = S.gemini_cost_usd,
    openrouter_cost_usd = S.openrouter_cost_usd,
    total_requests = S.total_requests,
    avg_cost_per_request = S.avg_cost_per_request,
    avg_cost_per_trip = S.avg_cost_per_trip,
    last_updated = S.last_updated
WHEN NOT MATCHED THEN
  INSERT (date, total_prompt_tokens, total_completion_tokens, total_tokens, total_cost_usd,
          gemini_cost_usd, openrouter_cost_usd, total_requests, avg_cost_per_request,
          avg_cost_per_trip, last_updated)
  VALUES (S.date, S.total_prompt_tokens, S.total_completion_tokens, S.total_tokens, S.total_cost_usd,
          S.gemini_cost_usd, S.openrouter_cost_usd, S.total_requests, S.avg_cost_per_request,
          S.avg_cost_per_trip, S.last_updated);
