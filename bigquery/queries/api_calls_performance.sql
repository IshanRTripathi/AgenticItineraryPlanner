-- API Calls Performance - SCHEDULED QUERY
-- Run daily at 2 AM UTC to update yesterday's API metrics
-- Tracks external API calls (Google Places, etc.)

MERGE `tripaiplanner.analytics.api_calls_performance` T
USING (
  SELECT
    DATE(TIMESTAMP_MILLIS(timestamp)) as date,
    JSON_EXTRACT_SCALAR(properties, '$.apiProvider') as api_provider,
    JSON_EXTRACT_SCALAR(properties, '$.endpoint') as endpoint,
    
    -- Volume
    COUNT(*) as total_calls,
    COUNT(DISTINCT JSON_EXTRACT_SCALAR(properties, '$.itineraryId')) as unique_itineraries,
    
    -- Success Tracking
    COUNT(CASE WHEN JSON_EXTRACT_SCALAR(properties, '$.success') = 'true' THEN 1 END) as successful_calls,
    COUNT(CASE WHEN JSON_EXTRACT_SCALAR(properties, '$.success') = 'false' THEN 1 END) as failed_calls,
    SAFE_DIVIDE(
      COUNT(CASE WHEN JSON_EXTRACT_SCALAR(properties, '$.success') = 'true' THEN 1 END),
      COUNT(*)
    ) * 100 as success_rate_percent,
    
    -- Timing Metrics
    COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64)), 0) as avg_duration_ms,
    COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64), 100)[OFFSET(50)], 0) as p50_duration_ms,
    COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64), 100)[OFFSET(95)], 0) as p95_duration_ms,
    COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64), 100)[OFFSET(99)], 0) as p99_duration_ms,
    
    -- HTTP Status Codes
    APPROX_TOP_COUNT(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.statusCode') AS STRING), 10) as status_code_distribution,
    
    CURRENT_TIMESTAMP() as last_updated
    
  FROM `tripaiplanner.analytics.raw_events`
  WHERE eventName = 'api_call_completed'
    AND DATE(TIMESTAMP_MILLIS(timestamp)) = DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY)
  GROUP BY date, api_provider, endpoint
) S
ON T.date = S.date AND T.api_provider = S.api_provider AND T.endpoint = S.endpoint
WHEN MATCHED THEN
  UPDATE SET
    total_calls = S.total_calls,
    unique_itineraries = S.unique_itineraries,
    successful_calls = S.successful_calls,
    failed_calls = S.failed_calls,
    success_rate_percent = S.success_rate_percent,
    avg_duration_ms = S.avg_duration_ms,
    p50_duration_ms = S.p50_duration_ms,
    p95_duration_ms = S.p95_duration_ms,
    p99_duration_ms = S.p99_duration_ms,
    status_code_distribution = S.status_code_distribution,
    last_updated = S.last_updated
WHEN NOT MATCHED THEN
  INSERT (date, api_provider, endpoint, total_calls, unique_itineraries,
          successful_calls, failed_calls, success_rate_percent,
          avg_duration_ms, p50_duration_ms, p95_duration_ms, p99_duration_ms,
          status_code_distribution, last_updated)
  VALUES (S.date, S.api_provider, S.endpoint, S.total_calls, S.unique_itineraries,
          S.successful_calls, S.failed_calls, S.success_rate_percent,
          S.avg_duration_ms, S.p50_duration_ms, S.p95_duration_ms, S.p99_duration_ms,
          S.status_code_distribution, S.last_updated);
