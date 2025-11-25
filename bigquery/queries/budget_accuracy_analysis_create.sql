-- Budget Accuracy Analysis Table Creation - NEW
-- Compares planned budget vs actual costs to improve estimation accuracy
-- Joins itinerary_created with itinerary_completed events

CREATE TABLE IF NOT EXISTS `tripaiplanner.analytics.budget_accuracy_analysis`
PARTITION BY date
CLUSTER BY date, budget_tier
AS
WITH created AS (
  SELECT
    JSON_EXTRACT_SCALAR(properties, '$.itineraryId') as itinerary_id,
    TIMESTAMP_MILLIS(timestamp) as created_at,
    SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.budgetMin') AS FLOAT64) as budget_min,
    SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.budgetMax') AS FLOAT64) as budget_max,
    JSON_EXTRACT_SCALAR(properties, '$.budgetTier') as budget_tier,
    SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationDays') AS INT64) as duration_days,
    JSON_EXTRACT_SCALAR(properties, '$.destination') as destination
  FROM `tripaiplanner.analytics.raw_events`
  WHERE eventName = 'itinerary_created'
    AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
),
completed AS (
  SELECT
    JSON_EXTRACT_SCALAR(properties, '$.itineraryId') as itinerary_id,
    SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalCost') AS FLOAT64) as actual_cost,
    JSON_EXTRACT_SCALAR(properties, '$.currency') as currency
  FROM `tripaiplanner.analytics.raw_events`
  WHERE eventName = 'itinerary_completed'
    AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
)
SELECT
  DATE(cr.created_at) as date,
  cr.budget_tier,
  co.currency,
  
  -- Volume
  COUNT(*) as itineraries_with_budget_data,
  COUNT(DISTINCT cr.destination) as unique_destinations,
  
  -- Budget Metrics
  AVG(cr.budget_min) as avg_budget_min,
  AVG(cr.budget_max) as avg_budget_max,
  AVG((cr.budget_min + cr.budget_max) / 2) as avg_budget_midpoint,
  
  -- Actual Cost Metrics
  AVG(co.actual_cost) as avg_actual_cost,
  MIN(co.actual_cost) as min_actual_cost,
  MAX(co.actual_cost) as max_actual_cost,
  APPROX_QUANTILES(co.actual_cost, 100)[OFFSET(50)] as p50_actual_cost,
  APPROX_QUANTILES(co.actual_cost, 100)[OFFSET(95)] as p95_actual_cost,
  
  -- Accuracy Metrics
  AVG(co.actual_cost - cr.budget_max) as avg_budget_variance,
  AVG(ABS(co.actual_cost - ((cr.budget_min + cr.budget_max) / 2))) as avg_absolute_variance,
  SAFE_DIVIDE(
    AVG(co.actual_cost - ((cr.budget_min + cr.budget_max) / 2)),
    AVG((cr.budget_min + cr.budget_max) / 2)
  ) * 100 as avg_variance_percent,
  
  -- Budget Compliance
  COUNT(CASE WHEN co.actual_cost <= cr.budget_max THEN 1 END) as within_budget_count,
  COUNT(CASE WHEN co.actual_cost > cr.budget_max THEN 1 END) as over_budget_count,
  COUNT(CASE WHEN co.actual_cost < cr.budget_min THEN 1 END) as under_budget_count,
  SAFE_DIVIDE(
    COUNT(CASE WHEN co.actual_cost <= cr.budget_max THEN 1 END),
    COUNT(*)
  ) * 100 as budget_compliance_rate_percent,
  
  -- Per-Day Metrics
  AVG(co.actual_cost / cr.duration_days) as avg_cost_per_day,
  AVG(cr.budget_max / cr.duration_days) as avg_budget_per_day,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated
  
FROM created cr
JOIN completed co ON cr.itinerary_id = co.itinerary_id
WHERE cr.budget_max IS NOT NULL
  AND co.actual_cost IS NOT NULL
  AND co.actual_cost > 0
GROUP BY date, budget_tier, currency;
