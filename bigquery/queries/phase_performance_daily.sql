-- Phase Performance Daily - SCHEDULED QUERY
-- Run daily at 2 AM UTC to update yesterday's phase metrics
-- Tracks performance of each pipeline phase

MERGE `tripaiplanner.analytics.phase_performance_daily` T
USING (
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
    
    CURRENT_TIMESTAMP() as last_updated
    
  FROM `tripaiplanner.analytics.raw_events`
  WHERE eventName = 'phase_completed'
    AND DATE(TIMESTAMP_MILLIS(timestamp)) <= CURRENT_DATE()  -- Changed for testing: use CURRENT_DATE() instead of yesterday
  GROUP BY date, phase_name
) S
ON T.date = S.date AND T.phase_name = S.phase_name
WHEN MATCHED THEN
  UPDATE SET
    executions = S.executions,
    unique_itineraries = S.unique_itineraries,
    successful_executions = S.successful_executions,
    failed_executions = S.failed_executions,
    success_rate_percent = S.success_rate_percent,
    avg_duration_ms = S.avg_duration_ms,
    min_duration_ms = S.min_duration_ms,
    max_duration_ms = S.max_duration_ms,
    p50_duration_ms = S.p50_duration_ms,
    p75_duration_ms = S.p75_duration_ms,
    p95_duration_ms = S.p95_duration_ms,
    p99_duration_ms = S.p99_duration_ms,
    last_updated = S.last_updated
WHEN NOT MATCHED THEN
  INSERT (date, phase_name, executions, unique_itineraries, successful_executions,
          failed_executions, success_rate_percent, avg_duration_ms, min_duration_ms,
          max_duration_ms, p50_duration_ms, p75_duration_ms, p95_duration_ms,
          p99_duration_ms, last_updated)
  VALUES (S.date, S.phase_name, S.executions, S.unique_itineraries, S.successful_executions,
          S.failed_executions, S.success_rate_percent, S.avg_duration_ms, S.min_duration_ms,
          S.max_duration_ms, S.p50_duration_ms, S.p75_duration_ms, S.p95_duration_ms,
          S.p99_duration_ms, S.last_updated);
