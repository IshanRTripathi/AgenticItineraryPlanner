-- Phase Performance Daily Table Creation - NEW
-- Tracks performance of each pipeline phase (6 phases)
-- Provides timing metrics, success rates, and percentiles

CREATE TABLE IF NOT EXISTS `tripaiplanner.analytics.phase_performance_daily`
PARTITION BY date
CLUSTER BY date, phase_name
AS
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  JSON_EXTRACT_SCALAR(properties, '$.phase') as phase_name,
  
  -- Volume
  COUNT(*) as executions,
  COUNT(DISTINCT JSON_EXTRACT_SCALAR(properties, '$.itineraryId')) as unique_itineraries,
  
  -- Success Rate
  COUNT(CASE WHEN JSON_EXTRACT_SCALAR(properties, '$.success') = 'true' THEN 1 END) as successful_executions,
  COUNT(CASE WHEN JSON_EXTRACT_SCALAR(properties, '$.success') = 'false' THEN 1 END) as failed_executions,
  SAFE_DIVIDE(
    COUNT(CASE WHEN JSON_EXTRACT_SCALAR(properties, '$.success') = 'true' THEN 1 END),
    COUNT(*)
  ) * 100 as success_rate_percent,
  
  -- Timing Metrics
  COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64)), 0) as avg_duration_ms,
  COALESCE(MIN(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64)), 0) as min_duration_ms,
  COALESCE(MAX(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64)), 0) as max_duration_ms,
  COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64), 100)[OFFSET(50)], 0) as p50_duration_ms,
  COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64), 100)[OFFSET(75)], 0) as p75_duration_ms,
  COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64), 100)[OFFSET(95)], 0) as p95_duration_ms,
  COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64), 100)[OFFSET(99)], 0) as p99_duration_ms,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated
  
FROM `tripaiplanner.analytics.raw_events`
WHERE eventName = 'phase_completed'
  AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
GROUP BY date, phase_name;
