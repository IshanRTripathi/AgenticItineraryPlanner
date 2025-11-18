-- Agent Performance Metrics
-- Tracks AI agent execution performance, success rates, and latency

CREATE OR REPLACE TABLE `tripaiplanner-4c951.analytics.agent_performance_daily`
PARTITION BY date
CLUSTER BY date, agent_type
AS
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  JSON_EXTRACT_SCALAR(properties, '$.agentType') as agent_type,
  
  -- Execution Metrics
  COUNT(CASE WHEN eventName = 'agent_started' THEN 1 END) as executions_started,
  COUNT(CASE WHEN eventName = 'agent_completed' THEN 1 END) as executions_completed,
  COUNT(CASE WHEN eventName = 'agent_failed' THEN 1 END) as executions_failed,
  
  -- Success Rate
  SAFE_DIVIDE(
    COUNT(CASE WHEN eventName = 'agent_completed' THEN 1 END),
    COUNT(CASE WHEN eventName = 'agent_started' THEN 1 END)
  ) * 100 as success_rate,
  
  -- Latency Metrics (from duration property)
  AVG(CAST(JSON_EXTRACT_SCALAR(properties, '$.duration') AS FLOAT64)) as avg_duration_ms,
  APPROX_QUANTILES(CAST(JSON_EXTRACT_SCALAR(properties, '$.duration') AS FLOAT64), 100)[OFFSET(50)] as p50_duration_ms,
  APPROX_QUANTILES(CAST(JSON_EXTRACT_SCALAR(properties, '$.duration') AS FLOAT64), 100)[OFFSET(95)] as p95_duration_ms,
  APPROX_QUANTILES(CAST(JSON_EXTRACT_SCALAR(properties, '$.duration') AS FLOAT64), 100)[OFFSET(99)] as p99_duration_ms,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated

FROM `tripaiplanner-4c951.analytics.raw_events`
WHERE eventName IN ('agent_started', 'agent_completed', 'agent_failed')
  AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAYS)
GROUP BY date, agent_type
ORDER BY date DESC, executions_started DESC;
