-- Interest Analysis Daily Table Creation - NEW
-- Tracks which interests are most popular and their combinations
-- Aggregates from itinerary_created events, unnesting interests array

CREATE TABLE IF NOT EXISTS `tripaiplanner.analytics.interest_analysis_daily`
PARTITION BY date
CLUSTER BY date, interest
AS
WITH interest_data AS (
  SELECT
    DATE(TIMESTAMP_MILLIS(timestamp)) as date,
    JSON_EXTRACT_SCALAR(properties, '$.itineraryId') as itinerary_id,
    JSON_EXTRACT_SCALAR(properties, '$.destination') as destination,
    SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.durationDays') AS INT64) as duration_days,
    SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.partySize') AS INT64) as party_size,
    JSON_EXTRACT_SCALAR(properties, '$.budgetTier') as budget_tier,
    TRIM(JSON_EXTRACT_SCALAR(interest)) as interest
  FROM `tripaiplanner.analytics.raw_events`,
  UNNEST(JSON_EXTRACT_ARRAY(properties, '$.interests')) as interest
  WHERE eventName = 'itinerary_created'
    AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
    AND JSON_EXTRACT_ARRAY(properties, '$.interests') IS NOT NULL
)
SELECT
  date,
  interest,
  
  -- Volume
  COUNT(DISTINCT itinerary_id) as itineraries_with_interest,
  COUNT(DISTINCT destination) as unique_destinations,
  
  -- Trip Characteristics
  AVG(duration_days) as avg_trip_duration_days,
  AVG(party_size) as avg_party_size,
  
  -- Budget Distribution
  COUNT(CASE WHEN budget_tier = 'budget' THEN 1 END) as budget_tier_count,
  COUNT(CASE WHEN budget_tier = 'moderate' THEN 1 END) as moderate_tier_count,
  COUNT(CASE WHEN budget_tier = 'luxury' THEN 1 END) as luxury_tier_count,
  
  -- Popularity Rank (within the day)
  RANK() OVER (PARTITION BY date ORDER BY COUNT(DISTINCT itinerary_id) DESC) as popularity_rank,
  
  -- Top Destinations for this Interest
  APPROX_TOP_COUNT(destination, 5) as top_destinations,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated
  
FROM interest_data
WHERE interest IS NOT NULL
  AND interest != ''
GROUP BY date, interest;
