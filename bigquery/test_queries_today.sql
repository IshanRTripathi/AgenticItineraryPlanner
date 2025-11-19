-- TEST QUERIES FOR TODAY'S DATA
-- Use these to test your queries with today's data instead of yesterday's

-- 1. Test daily_metrics with TODAY's data
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  COUNT(DISTINCT userId) as dau,
  COUNT(DISTINCT CASE WHEN eventName = 'user_signup_completed' THEN userId END) as new_signups,
  COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_initiated' THEN userId END) as trip_wizard_starts,
  COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_completed' THEN userId END) as trips_created,
  COUNT(DISTINCT CASE WHEN eventName = 'booking_initiated' THEN userId END) as booking_initiations,
  COUNT(DISTINCT CASE WHEN eventName = 'booking_completed' THEN userId END) as bookings_completed,
  COUNT(CASE WHEN eventName = 'pdf_export_completed' THEN 1 END) as pdf_exports,
  COUNT(CASE WHEN eventName = 'public_link_created' THEN 1 END) as public_links_created
FROM `tripaiplanner.analytics.raw_events`
WHERE DATE(TIMESTAMP_MILLIS(timestamp)) = CURRENT_DATE()
GROUP BY date;

-- 2. Test funnel_metrics with TODAY's data
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  COUNT(DISTINCT CASE WHEN eventName = 'trip_wizard_started' THEN userId END) as funnel_trip_wizard_started,
  COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_initiated' THEN userId END) as funnel_trip_initiated,
  COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_completed' THEN userId END) as funnel_trip_completed,
  COUNT(DISTINCT CASE WHEN eventName = 'booking_initiated' THEN userId END) as funnel_booking_initiated,
  COUNT(DISTINCT CASE WHEN eventName = 'booking_completed' THEN userId END) as funnel_booking_completed,
  COUNT(DISTINCT CASE WHEN eventName = 'payment_initiated' THEN userId END) as funnel_payment_initiated,
  COUNT(DISTINCT CASE WHEN eventName = 'payment_completed' THEN userId END) as funnel_payment_completed
FROM `tripaiplanner.analytics.raw_events`
WHERE DATE(TIMESTAMP_MILLIS(timestamp)) = CURRENT_DATE()
GROUP BY date;

-- 3. Test agent_performance with TODAY's data
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  JSON_EXTRACT_SCALAR(properties, '$.agentType') as agent_type,
  COUNT(CASE WHEN eventName = 'agent_started' THEN 1 END) as executions_started,
  COUNT(CASE WHEN eventName = 'agent_completed' THEN 1 END) as executions_completed,
  COUNT(CASE WHEN eventName = 'agent_failed' THEN 1 END) as executions_failed,
  COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.duration') AS FLOAT64)), 0) as avg_duration_ms
FROM `tripaiplanner.analytics.raw_events`
WHERE eventName IN ('agent_started', 'agent_completed', 'agent_failed')
  AND DATE(TIMESTAMP_MILLIS(timestamp)) = CURRENT_DATE()
GROUP BY date, agent_type;

-- 4. Test llm_costs with TODAY's data
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  JSON_EXTRACT_SCALAR(properties, '$.provider') as provider,
  JSON_EXTRACT_SCALAR(properties, '$.model') as model,
  JSON_EXTRACT_SCALAR(properties, '$.agentType') as agent_type,
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalTokens') AS INT64)), 0) as total_tokens,
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.llmCostUsd') AS FLOAT64)), 0) as total_cost_usd,
  COUNT(*) as request_count
FROM `tripaiplanner.analytics.raw_events`
WHERE eventName = 'llm_token_usage'
  AND DATE(TIMESTAMP_MILLIS(timestamp)) = CURRENT_DATE()
GROUP BY date, provider, model, agent_type;
