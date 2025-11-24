-- Destination Popularity Daily Table Creation - NEW
-- Tracks which destinations are most popular and their characteristics
-- Aggregates from itinerary_created events

CREATE TABLE IF NOT EXISTS `tripaiplanner.analytics.destination_popularity_daily`
PARTITION BY date
CLUSTER BY date, destination
AS
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  JSON_EXTRACT_SCALAR(properties, '$.destination') as destination,
  
  -- Volume
  COUNT(DISTINCT JSON_EXTRACT_SCALAR(properties, '$.itineraryId')) as itineraries_created,
  COUNT(DISTINCT JSON_EXTRACT_SCALAR(properties, '$.userId')) as unique_users,
  
  -- Trip Characteristics
  COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationDays') AS INT64)), 0) as avg_trip_duration_days,
  COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.partySize') AS INT64)), 0) as avg_party_size,
  
  -- Budget Distribution
  COUNT(CASE WHEN JSON_EXTRACT_SCALAR(properties, '$.budgetTier') = 'budget' THEN 1 END) as budget_tier_count,
  COUNT(CASE WHEN JSON_EXTRACT_SCALAR(properties, '$.budgetTier') = 'moderate' THEN 1 END) as moderate_tier_count,
  COUNT(CASE WHEN JSON_EXTRACT_SCALAR(properties, '$.budgetTier') = 'luxury' THEN 1 END) as luxury_tier_count,
  
  -- Budget Ranges
  COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.budgetMin') AS FLOAT64)), 0) as avg_budget_min,
  COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.budgetMax') AS FLOAT64)), 0) as avg_budget_max,
  
  -- Duration Distribution
  COUNT(CASE WHEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationDays') AS INT64) <= 3 THEN 1 END) as short_trips_1_3_days,
  COUNT(CASE WHEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationDays') AS INT64) BETWEEN 4 AND 7 THEN 1 END) as medium_trips_4_7_days,
  COUNT(CASE WHEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationDays') AS INT64) >= 8 THEN 1 END) as long_trips_8_plus_days,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated
  
FROM `tripaiplanner.analytics.raw_events`
WHERE eventName = 'itinerary_created'
  AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
  AND JSON_EXTRACT_SCALAR(properties, '$.destination') IS NOT NULL
GROUP BY date, destination;
