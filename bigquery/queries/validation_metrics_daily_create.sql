-- Validation Metrics Daily Table Creation - NEW
-- Tracks validation completion metrics for quality monitoring
-- Aggregates from validation_completed events

CREATE TABLE IF NOT EXISTS `tripaiplanner.analytics.validation_metrics_daily`
PARTITION BY date
CLUSTER BY date
AS
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  
  -- Volume
  COUNT(*) as total_validations,
  COUNT(DISTINCT JSON_EXTRACT_SCALAR(properties, '$.itineraryId')) as unique_itineraries,
  
  -- Success Rate
  COUNT(CASE WHEN JSON_EXTRACT_SCALAR(properties, '$.isValid') = 'true' THEN 1 END) as valid_itineraries,
  COUNT(CASE WHEN JSON_EXTRACT_SCALAR(properties, '$.isValid') = 'false' THEN 1 END) as invalid_itineraries,
  SAFE_DIVIDE(
    COUNT(CASE WHEN JSON_EXTRACT_SCALAR(properties, '$.isValid') = 'true' THEN 1 END),
    COUNT(*)
  ) * 100 as validation_pass_rate_percent,
  
  -- Error Metrics
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.errorCount') AS INT64)), 0) as total_errors,
  COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.errorCount') AS INT64)), 0) as avg_errors_per_validation,
  COALESCE(MAX(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.errorCount') AS INT64)), 0) as max_errors_single_validation,
  
  -- Warning Metrics
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.warningCount') AS INT64)), 0) as total_warnings,
  COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.warningCount') AS INT64)), 0) as avg_warnings_per_validation,
  COALESCE(MAX(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.warningCount') AS INT64)), 0) as max_warnings_single_validation,
  
  -- Timing Metrics
  COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64)), 0) as avg_duration_ms,
  COALESCE(MIN(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64)), 0) as min_duration_ms,
  COALESCE(MAX(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64)), 0) as max_duration_ms,
  COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64), 100)[OFFSET(50)], 0) as p50_duration_ms,
  COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64), 100)[OFFSET(95)], 0) as p95_duration_ms,
  
  -- Quality Score (higher is better)
  SAFE_DIVIDE(
    COUNT(CASE WHEN JSON_EXTRACT_SCALAR(properties, '$.isValid') = 'true' 
      AND SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.errorCount') AS INT64) = 0 
      AND SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.warningCount') AS INT64) < 10 THEN 1 END),
    COUNT(*)
  ) * 100 as high_quality_rate_percent,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated
  
FROM `tripaiplanner.analytics.raw_events`
WHERE eventName = 'validation_completed'
  AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
GROUP BY date;
