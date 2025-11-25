-- LLM Requests Detailed Table Creation - NEW
-- Tracks every LLM request with full token breakdown
-- Replaces the old llm_costs_daily with more granular data

CREATE TABLE IF NOT EXISTS `tripaiplanner.analytics.llm_requests_detailed`
PARTITION BY date
CLUSTER BY date, agent_name, model_name
AS
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  JSON_EXTRACT_SCALAR(properties, '$.agent') as agent_name,
  JSON_EXTRACT_SCALAR(properties, '$.model') as model_name,
  JSON_EXTRACT_SCALAR(properties, '$.provider') as provider,
  
  -- Volume
  COUNT(*) as request_count,
  COUNT(DISTINCT JSON_EXTRACT_SCALAR(properties, '$.itineraryId')) as unique_itineraries,
  
  -- Token Metrics (Full Breakdown)
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.promptTokens') AS INT64)), 0) as total_prompt_tokens,
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.responseTokens') AS INT64)), 0) as total_response_tokens,
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.thoughtsTokens') AS INT64)), 0) as total_thoughts_tokens,
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.cachedTokens') AS INT64)), 0) as total_cached_tokens,
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalTokens') AS INT64)), 0) as total_tokens,
  
  -- Average Tokens
  COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalTokens') AS INT64)), 0) as avg_tokens_per_request,
  COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.promptTokens') AS INT64)), 0) as avg_prompt_tokens,
  COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.responseTokens') AS INT64)), 0) as avg_response_tokens,
  
  -- Timing Metrics
  COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64)), 0) as avg_duration_ms,
  COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64), 100)[OFFSET(50)], 0) as p50_duration_ms,
  COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64), 100)[OFFSET(95)], 0) as p95_duration_ms,
  COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64), 100)[OFFSET(99)], 0) as p99_duration_ms,
  
  -- Success Rate
  COUNT(CASE WHEN JSON_EXTRACT_SCALAR(properties, '$.success') = 'true' THEN 1 END) as successful_requests,
  COUNT(CASE WHEN JSON_EXTRACT_SCALAR(properties, '$.success') = 'false' THEN 1 END) as failed_requests,
  SAFE_DIVIDE(
    COUNT(CASE WHEN JSON_EXTRACT_SCALAR(properties, '$.success') = 'true' THEN 1 END),
    COUNT(*)
  ) * 100 as success_rate_percent,
  
  -- Efficiency Ratios
  SAFE_DIVIDE(
    COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.thoughtsTokens') AS INT64)), 0),
    COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.responseTokens') AS INT64)), 1)
  ) as thoughts_to_response_ratio,
  
  SAFE_DIVIDE(
    COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.cachedTokens') AS INT64)), 0),
    COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalTokens') AS INT64)), 1)
  ) * 100 as cache_hit_rate_percent,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated
  
FROM `tripaiplanner.analytics.raw_events`
WHERE eventName = 'llm_request_completed'
  AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
GROUP BY date, agent_name, model_name, provider;
