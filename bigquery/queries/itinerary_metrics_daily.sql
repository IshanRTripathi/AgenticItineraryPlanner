-- Itinerary Metrics Daily - SCHEDULED QUERY
-- Run daily at 2 AM UTC to update yesterday's metrics
-- Updates comprehensive itinerary generation metrics

MERGE `tripaiplanner.analytics.itinerary_metrics_daily` T
USING (
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
    
    -- Timing Metrics
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
    
    -- Budget Tier Distribution
    COUNT(DISTINCT CASE WHEN eventName = 'itinerary_created' AND JSON_EXTRACT_SCALAR(properties, '$.budgetTier') = 'budget' THEN JSON_EXTRACT_SCALAR(properties, '$.itineraryId') END) as budget_tier_count,
    COUNT(DISTINCT CASE WHEN eventName = 'itinerary_created' AND JSON_EXTRACT_SCALAR(properties, '$.budgetTier') = 'moderate' THEN JSON_EXTRACT_SCALAR(properties, '$.itineraryId') END) as moderate_tier_count,
    COUNT(DISTINCT CASE WHEN eventName = 'itinerary_created' AND JSON_EXTRACT_SCALAR(properties, '$.budgetTier') = 'luxury' THEN JSON_EXTRACT_SCALAR(properties, '$.itineraryId') END) as luxury_tier_count,
    
    -- Trip Duration
    COALESCE(AVG(CASE WHEN eventName = 'itinerary_created' THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationDays') AS INT64) END), 0) as avg_trip_duration_days,
    
    -- Party Size
    COALESCE(AVG(CASE WHEN eventName = 'itinerary_created' THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.partySize') AS INT64) END), 0) as avg_party_size,
    
    CURRENT_TIMESTAMP() as last_updated
    
  FROM `tripaiplanner.analytics.raw_events`
  WHERE DATE(TIMESTAMP_MILLIS(timestamp)) = DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY)
    AND eventName IN ('itinerary_created', 'itinerary_completed', 'cost_estimated')
  GROUP BY date
) S
ON T.date = S.date
WHEN MATCHED THEN
  UPDATE SET
    itineraries_created = S.itineraries_created,
    itineraries_completed = S.itineraries_completed,
    completion_rate_percent = S.completion_rate_percent,
    avg_generation_time_ms = S.avg_generation_time_ms,
    p50_generation_time_ms = S.p50_generation_time_ms,
    p95_generation_time_ms = S.p95_generation_time_ms,
    min_generation_time_ms = S.min_generation_time_ms,
    max_generation_time_ms = S.max_generation_time_ms,
    avg_activities_per_itinerary = S.avg_activities_per_itinerary,
    total_activities_generated = S.total_activities_generated,
    avg_trip_cost = S.avg_trip_cost,
    cost_estimates_generated = S.cost_estimates_generated,
    total_validation_warnings = S.total_validation_warnings,
    total_validation_errors = S.total_validation_errors,
    avg_warnings_per_itinerary = S.avg_warnings_per_itinerary,
    budget_tier_count = S.budget_tier_count,
    moderate_tier_count = S.moderate_tier_count,
    luxury_tier_count = S.luxury_tier_count,
    avg_trip_duration_days = S.avg_trip_duration_days,
    avg_party_size = S.avg_party_size,
    last_updated = S.last_updated
WHEN NOT MATCHED THEN
  INSERT (date, itineraries_created, itineraries_completed, completion_rate_percent,
          avg_generation_time_ms, p50_generation_time_ms, p95_generation_time_ms,
          min_generation_time_ms, max_generation_time_ms, avg_activities_per_itinerary,
          total_activities_generated, avg_trip_cost, cost_estimates_generated,
          total_validation_warnings, total_validation_errors, avg_warnings_per_itinerary,
          budget_tier_count, moderate_tier_count, luxury_tier_count,
          avg_trip_duration_days, avg_party_size, last_updated)
  VALUES (S.date, S.itineraries_created, S.itineraries_completed, S.completion_rate_percent,
          S.avg_generation_time_ms, S.p50_generation_time_ms, S.p95_generation_time_ms,
          S.min_generation_time_ms, S.max_generation_time_ms, S.avg_activities_per_itinerary,
          S.total_activities_generated, S.avg_trip_cost, S.cost_estimates_generated,
          S.total_validation_warnings, S.total_validation_errors, S.avg_warnings_per_itinerary,
          S.budget_tier_count, S.moderate_tier_count, S.luxury_tier_count,
          S.avg_trip_duration_days, S.avg_party_size, S.last_updated);
