-- Cost Breakdown Daily Table Creation - NEW
-- Tracks cost estimates by activity type and currency
-- Aggregates from cost_estimated events

CREATE TABLE IF NOT EXISTS `tripaiplanner.analytics.cost_breakdown_daily`
PARTITION BY date
CLUSTER BY date, node_type, currency
AS
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
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated
  
FROM `tripaiplanner.analytics.raw_events`
WHERE eventName = 'cost_estimated'
  AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
GROUP BY date, node_type, currency;
