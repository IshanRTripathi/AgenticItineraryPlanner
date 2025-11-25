-- LLM Requests Detailed - SCHEDULED QUERY
-- Run daily at 2 AM UTC to update yesterday's LLM metrics
-- Tracks token usage and costs with full breakdown

MERGE `tripaiplanner.analytics.llm_requests_detailed` T
USING (
  SELECT
    DATE(TIMESTAMP_MILLIS(timestamp)) as date,
    COALESCE(JSON_EXTRACT_SCALAR(properties, '$.agent'), 'unknown') as agent_name,
    JSON_EXTRACT_SCALAR(properties, '$.model') as model_name,
    JSON_EXTRACT_SCALAR(properties, '$.provider') as provider,
    
    -- Volume
    COUNT(*) as request_count,
    COUNT(DISTINCT JSON_EXTRACT_SCALAR(properties, '$.itineraryId')) as unique_itineraries,
    
    -- Token Metrics (using actual field names from events)
    COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.promptTokens') AS INT64)), 0) as total_prompt_tokens,
    COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.completionTokens') AS INT64)), 0) as total_response_tokens,
    COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.thoughtsTokens') AS INT64)), 0) as total_thoughts_tokens,
    COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.cachedTokens') AS INT64)), 0) as total_cached_tokens,
    COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalTokens') AS INT64)), 0) as total_tokens,
    
    -- Average Tokens
    COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalTokens') AS INT64)), 0) as avg_tokens_per_request,
    COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.promptTokens') AS INT64)), 0) as avg_prompt_tokens,
    COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.completionTokens') AS INT64)), 0) as avg_response_tokens,
    
    -- Timing Metrics (using latencyMs instead of durationMs)
    COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.latencyMs') AS INT64)), 0) as avg_duration_ms,
    COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.latencyMs') AS INT64), 100)[OFFSET(50)], 0) as p50_duration_ms,
    COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.latencyMs') AS INT64), 100)[OFFSET(95)], 0) as p95_duration_ms,
    COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.latencyMs') AS INT64), 100)[OFFSET(99)], 0) as p99_duration_ms,
    
    -- Success Rate (all events are successful if they exist, no success field in events)
    COUNT(*) as successful_requests,
    0 as failed_requests,
    100.0 as success_rate_percent,
    
    -- Efficiency Ratios
    SAFE_DIVIDE(
      COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.thoughtsTokens') AS INT64)), 0),
      COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.completionTokens') AS INT64)), 1)
    ) as thoughts_to_response_ratio,
    
    SAFE_DIVIDE(
      COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.cachedTokens') AS INT64)), 0),
      COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalTokens') AS INT64)), 1)
    ) * 100 as cache_hit_rate_percent,
    
    CURRENT_TIMESTAMP() as last_updated
    
  FROM `tripaiplanner.analytics.raw_events`
  WHERE eventName = 'llm_token_usage'
    AND DATE(TIMESTAMP_MILLIS(timestamp)) = DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY)
  GROUP BY date, agent_name, model_name, provider
) S
ON T.date = S.date 
  AND T.agent_name = S.agent_name 
  AND T.model_name = S.model_name 
  AND T.provider = S.provider
WHEN MATCHED THEN
  UPDATE SET
    request_count = S.request_count,
    unique_itineraries = S.unique_itineraries,
    total_prompt_tokens = S.total_prompt_tokens,
    total_response_tokens = S.total_response_tokens,
    total_thoughts_tokens = S.total_thoughts_tokens,
    total_cached_tokens = S.total_cached_tokens,
    total_tokens = S.total_tokens,
    avg_tokens_per_request = S.avg_tokens_per_request,
    avg_prompt_tokens = S.avg_prompt_tokens,
    avg_response_tokens = S.avg_response_tokens,
    avg_duration_ms = S.avg_duration_ms,
    p50_duration_ms = S.p50_duration_ms,
    p95_duration_ms = S.p95_duration_ms,
    p99_duration_ms = S.p99_duration_ms,
    successful_requests = S.successful_requests,
    failed_requests = S.failed_requests,
    success_rate_percent = S.success_rate_percent,
    thoughts_to_response_ratio = S.thoughts_to_response_ratio,
    cache_hit_rate_percent = S.cache_hit_rate_percent,
    last_updated = S.last_updated
WHEN NOT MATCHED THEN
  INSERT (date, agent_name, model_name, provider, request_count, unique_itineraries,
          total_prompt_tokens, total_response_tokens, total_thoughts_tokens, total_cached_tokens,
          total_tokens, avg_tokens_per_request, avg_prompt_tokens, avg_response_tokens,
          avg_duration_ms, p50_duration_ms, p95_duration_ms, p99_duration_ms,
          successful_requests, failed_requests, success_rate_percent,
          thoughts_to_response_ratio, cache_hit_rate_percent, last_updated)
  VALUES (S.date, S.agent_name, S.model_name, S.provider, S.request_count, S.unique_itineraries,
          S.total_prompt_tokens, S.total_response_tokens, S.total_thoughts_tokens, S.total_cached_tokens,
          S.total_tokens, S.avg_tokens_per_request, S.avg_prompt_tokens, S.avg_response_tokens,
          S.avg_duration_ms, S.p50_duration_ms, S.p95_duration_ms, S.p99_duration_ms,
          S.successful_requests, S.failed_requests, S.success_rate_percent,
          S.thoughts_to_response_ratio, S.cache_hit_rate_percent, S.last_updated);
