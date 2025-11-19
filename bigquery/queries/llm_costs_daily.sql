-- LLM Token Usage and Cost Aggregation - SCHEDULED QUERY
-- Run daily at 2 AM UTC to update yesterday's LLM costs
-- Tracks token consumption and estimated costs by provider, model, and agent

MERGE `tripaiplanner.analytics.llm_costs_daily` T
USING (
  SELECT
    DATE(TIMESTAMP_MILLIS(timestamp)) as date,
    JSON_EXTRACT_SCALAR(properties, '$.provider') as provider,
    JSON_EXTRACT_SCALAR(properties, '$.model') as model,
    JSON_EXTRACT_SCALAR(properties, '$.agentType') as agent_type,
    COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.promptTokens') AS INT64)), 0) as total_prompt_tokens,
    COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.completionTokens') AS INT64)), 0) as total_completion_tokens,
    COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalTokens') AS INT64)), 0) as total_tokens,
    COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64)), 0) as total_cost_usd,
    COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64)), 0) as avg_cost_per_request,
    COUNT(*) as request_count,
    SAFE_DIVIDE(
      COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalTokens') AS INT64)), 0),
      COUNT(*)
    ) as avg_tokens_per_request,
    CURRENT_TIMESTAMP() as last_updated
  FROM `tripaiplanner.analytics.raw_events`
  WHERE eventName = 'llm_token_usage'
    AND DATE(TIMESTAMP_MILLIS(timestamp)) = DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY)
  GROUP BY date, provider, model, agent_type
) S
ON T.date = S.date 
  AND T.provider = S.provider 
  AND T.model = S.model 
  AND COALESCE(T.agent_type, '') = COALESCE(S.agent_type, '')
WHEN MATCHED THEN
  UPDATE SET
    total_prompt_tokens = S.total_prompt_tokens,
    total_completion_tokens = S.total_completion_tokens,
    total_tokens = S.total_tokens,
    total_cost_usd = S.total_cost_usd,
    avg_cost_per_request = S.avg_cost_per_request,
    request_count = S.request_count,
    avg_tokens_per_request = S.avg_tokens_per_request,
    last_updated = S.last_updated
WHEN NOT MATCHED THEN
  INSERT (date, provider, model, agent_type, total_prompt_tokens, total_completion_tokens,
          total_tokens, total_cost_usd, avg_cost_per_request, request_count,
          avg_tokens_per_request, last_updated)
  VALUES (S.date, S.provider, S.model, S.agent_type, S.total_prompt_tokens, S.total_completion_tokens,
          S.total_tokens, S.total_cost_usd, S.avg_cost_per_request, S.request_count,
          S.avg_tokens_per_request, S.last_updated);
