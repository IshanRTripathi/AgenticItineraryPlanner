-- Agent Performance Metrics - SCHEDULED QUERY
-- Run daily at 2 AM UTC to update yesterday's agent performance
-- Tracks AI agent execution performance, success rates, and latency

MERGE `tripaiplanner.analytics.agent_performance_daily` T
USING (
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
    AND DATE(TIMESTAMP_MILLIS(timestamp)) = DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY)
  GROUP BY date, agent_type
) S
ON T.date = S.date AND T.agent_type = S.agent_type
WHEN MATCHED THEN
  UPDATE SET
    executions_started = S.executions_started,
    executions_completed = S.executions_completed,
    executions_failed = S.executions_failed,
    success_rate = S.success_rate,
    avg_duration_ms = S.avg_duration_ms,
    p50_duration_ms = S.p50_duration_ms,
    p95_duration_ms = S.p95_duration_ms,
    p99_duration_ms = S.p99_duration_ms,
    last_updated = S.last_updated
WHEN NOT MATCHED THEN
  INSERT (date, agent_type, executions_started, executions_completed, executions_failed,
          success_rate, avg_duration_ms, p50_duration_ms, p95_duration_ms, p99_duration_ms, last_updated)
  VALUES (S.date, S.agent_type, S.executions_started, S.executions_completed, S.executions_failed,
          S.success_rate, S.avg_duration_ms, S.p50_duration_ms, S.p95_duration_ms, S.p99_duration_ms, S.last_updated);
