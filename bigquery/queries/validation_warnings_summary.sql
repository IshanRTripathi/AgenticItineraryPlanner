-- Validation Warnings Summary - SCHEDULED QUERY
-- Run daily at 2 AM UTC to update yesterday's validation warnings
-- Tracks all validation warnings by type and severity

MERGE `tripaiplanner.analytics.validation_warnings_summary` T
USING (
  SELECT
    DATE(TIMESTAMP_MILLIS(timestamp)) as date,
    JSON_EXTRACT_SCALAR(properties, '$.warningType') as warning_type,
    JSON_EXTRACT_SCALAR(properties, '$.severity') as severity,
    
    -- Volume
    COUNT(*) as warning_count,
    COUNT(DISTINCT JSON_EXTRACT_SCALAR(properties, '$.itineraryId')) as affected_itineraries,
    -- Note: nodeId is embedded in message, not a separate field
    0 as affected_nodes,
    
    -- Most common messages (nodeId not available as separate field)
    APPROX_TOP_COUNT(JSON_EXTRACT_SCALAR(properties, '$.message'), 10) as top_affected_nodes,
    
    -- Sample messages
    ARRAY_AGG(JSON_EXTRACT_SCALAR(properties, '$.message') LIMIT 5) as sample_messages,
    
    CURRENT_TIMESTAMP() as last_updated
    
  FROM `tripaiplanner.analytics.raw_events`
  WHERE eventName = 'validation_warning'
    AND DATE(TIMESTAMP_MILLIS(timestamp)) = DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY)
  GROUP BY date, warning_type, severity
) S
ON T.date = S.date AND T.warning_type = S.warning_type AND T.severity = S.severity
WHEN MATCHED THEN
  UPDATE SET
    warning_count = S.warning_count,
    affected_itineraries = S.affected_itineraries,
    affected_nodes = S.affected_nodes,
    top_affected_nodes = S.top_affected_nodes,
    sample_messages = S.sample_messages,
    last_updated = S.last_updated
WHEN NOT MATCHED THEN
  INSERT (date, warning_type, severity, warning_count, affected_itineraries,
          affected_nodes, top_affected_nodes, sample_messages, last_updated)
  VALUES (S.date, S.warning_type, S.severity, S.warning_count, S.affected_itineraries,
          S.affected_nodes, S.top_affected_nodes, S.sample_messages, S.last_updated);
