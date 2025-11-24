-- Agent Error Patterns Table Creation - NEW
-- Identifies recurring errors and failure patterns across pipeline phases
-- Aggregates from phase_completed events where success = false

CREATE TABLE IF NOT EXISTS `tripaiplanner.analytics.agent_error_patterns`
PARTITION BY date
CLUSTER BY date, phase, error_type
AS
WITH error_classification AS (
  SELECT
    TIMESTAMP_MILLIS(timestamp) as event_time,
    JSON_EXTRACT_SCALAR(properties, '$.itineraryId') as itinerary_id,
    JSON_EXTRACT_SCALAR(properties, '$.phase') as phase,
    JSON_EXTRACT_SCALAR(properties, '$.errorMessage') as error_message,
    SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationMs') AS INT64) as duration_ms,
    
    -- Classify error types based on message patterns
    CASE
      WHEN JSON_EXTRACT_SCALAR(properties, '$.errorMessage') LIKE '%timeout%' THEN 'timeout'
      WHEN JSON_EXTRACT_SCALAR(properties, '$.errorMessage') LIKE '%rate limit%' THEN 'rate_limit'
      WHEN JSON_EXTRACT_SCALAR(properties, '$.errorMessage') LIKE '%quota%' THEN 'quota_exceeded'
      WHEN JSON_EXTRACT_SCALAR(properties, '$.errorMessage') LIKE '%connection%' THEN 'connection_error'
      WHEN JSON_EXTRACT_SCALAR(properties, '$.errorMessage') LIKE '%parse%' OR JSON_EXTRACT_SCALAR(properties, '$.errorMessage') LIKE '%JSON%' THEN 'parsing_error'
      WHEN JSON_EXTRACT_SCALAR(properties, '$.errorMessage') LIKE '%validation%' THEN 'validation_error'
      WHEN JSON_EXTRACT_SCALAR(properties, '$.errorMessage') LIKE '%null%' OR JSON_EXTRACT_SCALAR(properties, '$.errorMessage') LIKE '%NullPointer%' THEN 'null_pointer'
      WHEN JSON_EXTRACT_SCALAR(properties, '$.errorMessage') LIKE '%concurrent%' OR JSON_EXTRACT_SCALAR(properties, '$.errorMessage') LIKE '%lock%' THEN 'concurrency_error'
      ELSE 'other'
    END as error_type
    
  FROM `tripaiplanner.analytics.raw_events`
  WHERE eventName = 'phase_completed'
    AND JSON_EXTRACT_SCALAR(properties, '$.success') = 'false'
    AND JSON_EXTRACT_SCALAR(properties, '$.errorMessage') IS NOT NULL
    AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
)
SELECT
  DATE(event_time) as date,
  phase,
  error_type,
  
  -- Volume
  COUNT(*) as error_count,
  COUNT(DISTINCT itinerary_id) as affected_itineraries,
  
  -- Timing
  AVG(duration_ms) as avg_duration_before_failure_ms,
  MAX(duration_ms) as max_duration_before_failure_ms,
  
  -- Sample Error Messages (for debugging)
  ARRAY_AGG(error_message LIMIT 5) as sample_error_messages,
  
  -- Most Common Error Message
  APPROX_TOP_COUNT(error_message, 1)[OFFSET(0)].value as most_common_error_message,
  APPROX_TOP_COUNT(error_message, 1)[OFFSET(0)].count as most_common_error_count,
  
  -- Error Rate (compared to total phase executions) - removed due to aggregation complexity
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated
  
FROM error_classification
GROUP BY date, phase, error_type;
