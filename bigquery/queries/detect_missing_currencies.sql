-- Detect Missing Currencies
-- Query to find currencies in raw_events that don't have exchange rates
-- Run this periodically to identify new currencies that need to be added

SELECT DISTINCT
  JSON_EXTRACT_SCALAR(properties, '$.currency') as missing_currency,
  COUNT(*) as event_count,
  COUNT(DISTINCT JSON_EXTRACT_SCALAR(properties, '$.itineraryId')) as affected_itineraries,
  MIN(DATE(TIMESTAMP_MILLIS(timestamp))) as first_seen,
  MAX(DATE(TIMESTAMP_MILLIS(timestamp))) as last_seen,
  
  -- Sample itinerary IDs for investigation
  ARRAY_AGG(DISTINCT JSON_EXTRACT_SCALAR(properties, '$.itineraryId') LIMIT 5) as sample_itinerary_ids
  
FROM `tripaiplanner.analytics.raw_events`
WHERE eventName = 'cost_estimated'
  AND JSON_EXTRACT_SCALAR(properties, '$.currency') IS NOT NULL
  AND JSON_EXTRACT_SCALAR(properties, '$.currency') NOT IN (
    SELECT currency_code 
    FROM `tripaiplanner.analytics.exchange_rates`
    WHERE is_active = TRUE
  )
  AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
GROUP BY missing_currency
ORDER BY event_count DESC;
