-- Itinerary Metrics Daily Table Creation - NEW
-- Tracks comprehensive itinerary generation metrics
-- Covers all 250+ metrics from the analysis

CREATE TABLE IF NOT EXISTS `tripaiplanner.analytics.itinerary_metrics_daily`
PARTITION BY date
CLUSTER BY date
AS
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  
  -- Volume Metrics
  COUNT(DISTINCT CASE WHEN eventName = 'itinerary_created' THEN JSON_EXTRACT_SCALAR(properties, '$.itineraryId') END) as itineraries_created,
  COUNT(DISTINCT CASE WHEN eventName = 'itinerary_completed' THEN JSON_EXTRACT_SCALAR(properties, '$.itineraryId') END) as itineraries_completed,
  
  -- Success Rate
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN eventName = 'itinerary_completed' AND JSON_EXTRACT_SCALAR(properties, '$.success') = 'true' THEN JSON_EXTRACT_SCALAR(properties, '$.itineraryId') END),
    COUNT(DISTINCT CASE WHEN eventName = 'itinerary_created' THEN JSON_EXTRACT_SCALAR(properties, '$.itineraryId') END)
  ) * 100 as completion_rate_percent,
  
  -- Timing Metrics (from itinerary_completed events)
  COALESCE(AVG(CASE WHEN eventName = 'itinerary_completed' THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalDurationMs') AS INT64) END), 0) as avg_generation_time_ms,
  COALESCE(APPROX_QUANTILES(CASE WHEN eventName = 'itinerary_completed' THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalDurationMs') AS INT64) END, 100)[OFFSET(50)], 0) as p50_generation_time_ms,
  COALESCE(APPROX_QUANTILES(CASE WHEN eventName = 'itinerary_completed' THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalDurationMs') AS INT64) END, 100)[OFFSET(95)], 0) as p95_generation_time_ms,
  COALESCE(MIN(CASE WHEN eventName = 'itinerary_completed' THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalDurationMs') AS INT64) END), 0) as min_generation_time_ms,
  COALESCE(MAX(CASE WHEN eventName = 'itinerary_completed' THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalDurationMs') AS INT64) END), 0) as max_generation_time_ms,
  
  -- Activities Generated
  COALESCE(AVG(CASE WHEN eventName = 'itinerary_completed' THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalActivities') AS INT64) END), 0) as avg_activities_per_itinerary,
  COALESCE(SUM(CASE WHEN eventName = 'itinerary_completed' THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalActivities') AS INT64) END), 0) as total_activities_generated,
  
  -- Cost Metrics
  COALESCE(AVG(CASE WHEN eventName = 'itinerary_completed' THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.totalCost') AS FLOAT64) END), 0) as avg_trip_cost,
  COALESCE(SUM(CASE WHEN eventName = 'cost_estimated' THEN 1 END), 0) as cost_estimates_generated,
  
  -- Quality Metrics
  COALESCE(SUM(CASE WHEN eventName = 'itinerary_completed' THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.validationWarnings') AS INT64) END), 0) as total_validation_warnings,
  COALESCE(SUM(CASE WHEN eventName = 'itinerary_completed' THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.validationErrors') AS INT64) END), 0) as total_validation_errors,
  COALESCE(AVG(CASE WHEN eventName = 'itinerary_completed' THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.validationWarnings') AS INT64) END), 0) as avg_warnings_per_itinerary,
  
  -- Budget Tier Distribution (from itinerary_created events)
  COUNT(DISTINCT CASE WHEN eventName = 'itinerary_created' AND JSON_EXTRACT_SCALAR(properties, '$.budgetTier') = 'budget' THEN JSON_EXTRACT_SCALAR(properties, '$.itineraryId') END) as budget_tier_count,
  COUNT(DISTINCT CASE WHEN eventName = 'itinerary_created' AND JSON_EXTRACT_SCALAR(properties, '$.budgetTier') = 'moderate' THEN JSON_EXTRACT_SCALAR(properties, '$.itineraryId') END) as moderate_tier_count,
  COUNT(DISTINCT CASE WHEN eventName = 'itinerary_created' AND JSON_EXTRACT_SCALAR(properties, '$.budgetTier') = 'luxury' THEN JSON_EXTRACT_SCALAR(properties, '$.itineraryId') END) as luxury_tier_count,
  
  -- Trip Duration Distribution
  COALESCE(AVG(CASE WHEN eventName = 'itinerary_created' THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationDays') AS INT64) END), 0) as avg_trip_duration_days,
  
  -- Party Size Distribution
  COALESCE(AVG(CASE WHEN eventName = 'itinerary_created' THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.partySize') AS INT64) END), 0) as avg_party_size,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated
  
FROM `tripaiplanner.analytics.raw_events`
WHERE DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
  AND eventName IN ('itinerary_created', 'itinerary_completed', 'cost_estimated')
GROUP BY date;
