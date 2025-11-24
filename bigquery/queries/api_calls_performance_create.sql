-- API Calls Performance Table Creation - NEW
-- Tracks external API calls (Google Places, etc.)
-- Monitors latency and success rates

CREATE TABLE IF NOT EXISTS `tripaiplanner.analytics.api_calls_performance`
PARTITION BY date  
CLUSTER BY date, api_provider, endpoint
AS
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
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated
  
FROM `tripaiplanner.analytics.raw_events`
WHERE eventName = 'api_call_completed'
  AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
GROUP BY date, api_provider, endpoint;
