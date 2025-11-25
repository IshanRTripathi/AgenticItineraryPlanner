-- Cost Breakdown Daily - SCHEDULED QUERY
-- Run daily at 2 AM UTC to update yesterday's cost metrics
-- Tracks cost estimates by activity type and currency

MERGE `tripaiplanner.analytics.cost_breakdown_daily` T
USING (
  SELECT
    DATE(TIMESTAMP_MILLIS(timestamp)) as date,
    JSON_EXTRACT_SCALAR(properties, '$.nodeType') as node_type,
    JSON_EXTRACT_SCALAR(properties, '$.currency') as currency,
    
    -- Volume
    COUNT(*) as cost_estimates_count,
    COUNT(DISTINCT JSON_EXTRACT_SCALAR(properties, '$.itineraryId')) as unique_itineraries,
    
    -- Cost Metrics
    COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.costPerPerson') AS FLOAT64)), 0) as total_cost,
    COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.costPerPerson') AS FLOAT64)), 0) as avg_cost_per_activity,
    COALESCE(MIN(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.costPerPerson') AS FLOAT64)), 0) as min_cost,
    COALESCE(MAX(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.costPerPerson') AS FLOAT64)), 0) as max_cost,
    
    -- Cost Percentiles
    COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.costPerPerson') AS FLOAT64), 100)[OFFSET(50)], 0) as p50_cost,
    COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.costPerPerson') AS FLOAT64), 100)[OFFSET(95)], 0) as p95_cost,
    
    -- Top Activities by Cost
    APPROX_TOP_COUNT(JSON_EXTRACT_SCALAR(properties, '$.activityName'), 10) as top_expensive_activities,
    
    CURRENT_TIMESTAMP() as last_updated
    
  FROM `tripaiplanner.analytics.raw_events`
  WHERE eventName = 'cost_estimated'
    AND DATE(TIMESTAMP_MILLIS(timestamp)) = DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY)
  GROUP BY date, node_type, currency
) S
ON T.date = S.date AND T.node_type = S.node_type AND T.currency = S.currency
WHEN MATCHED THEN
  UPDATE SET
    cost_estimates_count = S.cost_estimates_count,
    unique_itineraries = S.unique_itineraries,
    total_cost = S.total_cost,
    avg_cost_per_activity = S.avg_cost_per_activity,
    min_cost = S.min_cost,
    max_cost = S.max_cost,
    p50_cost = S.p50_cost,
    p95_cost = S.p95_cost,
    top_expensive_activities = S.top_expensive_activities,
    last_updated = S.last_updated
WHEN NOT MATCHED THEN
  INSERT (date, node_type, currency, cost_estimates_count, unique_itineraries,
          total_cost, avg_cost_per_activity, min_cost, max_cost, p50_cost, p95_cost,
          top_expensive_activities, last_updated)
  VALUES (S.date, S.node_type, S.currency, S.cost_estimates_count, S.unique_itineraries,
          S.total_cost, S.avg_cost_per_activity, S.min_cost, S.max_cost, S.p50_cost, S.p95_cost,
          S.top_expensive_activities, S.last_updated);
