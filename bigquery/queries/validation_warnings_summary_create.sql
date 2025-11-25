-- Validation Warnings Summary Table Creation - NEW
-- Tracks all validation warnings by type and severity
-- Helps identify quality issues in itinerary generation

CREATE TABLE IF NOT EXISTS `tripaiplanner.analytics.validation_warnings_summary`
PARTITION BY date
CLUSTER BY date, warning_type
AS
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  JSON_EXTRACT_SCALAR(properties, '$.warningType') as warning_type,
  JSON_EXTRACT_SCALAR(properties, '$.severity') as severity,
  
  -- Volume
  COUNT(*) as warning_count,
  COUNT(DISTINCT JSON_EXTRACT_SCALAR(properties, '$.itineraryId')) as affected_itineraries,
  COUNT(DISTINCT JSON_EXTRACT_SCALAR(properties, '$.nodeId')) as affected_nodes,
  
  -- Most common nodes causing this warning
  APPROX_TOP_COUNT(JSON_EXTRACT_SCALAR(properties, '$.nodeId'), 10) as top_affected_nodes,
  
  -- Sample messages (first 5)
  ARRAY_AGG(JSON_EXTRACT_SCALAR(properties, '$.message') LIMIT 5) as sample_messages,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated
  
FROM `tripaiplanner.analytics.raw_events`
WHERE eventName = 'validation_warning'
  AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
GROUP BY date, warning_type, severity;
