-- Agent Performance Daily Table Creation - RUN ONCE
-- Creates the agent_performance_daily table with initial data from last 90 days

CREATE OR REPLACE TABLE `tripaiplanner.analytics.agent_performance_daily`
PARTITION BY date
CLUSTER BY date, agent_type
AS
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  JSON_EXTRACT_SCALAR(properties, '$.agentType') as agent_type,
  COUNT(CASE WHEN eventName = 'agent_started' THEN 1 END) as executions_started,
  COUNT(CASE WHEN eventName = 'agent_completed' THEN 1 END) as executions_completed,
  COUNT(CASE WHEN eventName = 'agent_failed' THEN 1 END) as executions_failed,
  SAFE_DIVIDE(
    COUNT(CASE WHEN eventName = 'agent_completed' THEN 1 END),
    COUNT(CASE WHEN eventName = 'agent_started' THEN 1 END)
  ) * 100 as success_rate,
  COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.duration') AS FLOAT64)), 0) as avg_duration_ms,
  COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.duration') AS FLOAT64), 100)[OFFSET(50)], 0) as p50_duration_ms,
  COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.duration') AS FLOAT64), 100)[OFFSET(95)], 0) as p95_duration_ms,
  COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.duration') AS FLOAT64), 100)[OFFSET(99)], 0) as p99_duration_ms,
  CURRENT_TIMESTAMP() as last_updated
FROM `tripaiplanner.analytics.raw_events`
WHERE eventName IN ('agent_started', 'agent_completed', 'agent_failed')
  AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
GROUP BY date, agent_type;
