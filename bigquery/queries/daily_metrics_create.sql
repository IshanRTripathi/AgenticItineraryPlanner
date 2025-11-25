-- Daily Metrics Table Creation - FIXED VERSION
-- Creates the daily_metrics table with initial data from last 90 days
-- FIXES: Added itinerary metrics, chat metrics, correct event names
-- After running this, use daily_metrics.sql as a scheduled query

CREATE TABLE IF NOT EXISTS `tripaiplanner.analytics.daily_metrics`
PARTITION BY date
CLUSTER BY date
AS
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  
  -- User Metrics
  COUNT(DISTINCT userId) as dau,
  COUNT(DISTINCT CASE WHEN eventName = 'user_signup_completed' THEN userId END) as new_signups,
  COUNT(DISTINCT CASE WHEN eventName = 'user_login_completed' THEN userId END) as daily_logins,
  
  -- Trip Creation Metrics (FIXED: Using correct event names)
  COUNT(DISTINCT CASE WHEN eventName = 'trip_wizard_started' THEN userId END) as trip_wizard_starts,
  COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_initiated' THEN userId END) as trips_initiated,
  COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_completed' THEN userId END) as trips_created,
  COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_failed' THEN userId END) as trip_creation_failures,
  
  -- NEW: Itinerary Generation Metrics
  COUNT(DISTINCT CASE WHEN eventName = 'itinerary_created' THEN JSON_EXTRACT_SCALAR(properties, '$.itineraryId') END) as itineraries_created,
  COUNT(DISTINCT CASE WHEN eventName = 'itinerary_completed' THEN JSON_EXTRACT_SCALAR(properties, '$.itineraryId') END) as itineraries_completed,
  
  -- Booking Metrics
  COUNT(DISTINCT CASE WHEN eventName = 'booking_initiated' THEN userId END) as booking_initiations,
  COUNT(DISTINCT CASE WHEN eventName = 'booking_completed' THEN userId END) as bookings_completed,
  
  -- Export Metrics
  COUNT(CASE WHEN eventName = 'pdf_export_completed' THEN 1 END) as pdf_exports,
  COUNT(CASE WHEN eventName = 'public_link_created' THEN 1 END) as public_links_created,
  
  -- Engagement Metrics
  COUNT(CASE WHEN eventName = 'page_view' THEN 1 END) as total_page_views,
  COUNT(DISTINCT CASE WHEN eventName = 'page_view' THEN sessionId END) as unique_sessions,
  
  -- NEW: Chat Metrics
  COUNT(CASE WHEN eventName = 'chat_message_sent' THEN 1 END) as chat_messages_sent,
  COUNT(CASE WHEN eventName = 'chat_response_received' THEN 1 END) as chat_responses_received,
  COUNT(CASE WHEN eventName = 'chat_response_failed' THEN 1 END) as chat_failures,
  
  -- Payment Metrics with SAFE_CAST to handle invalid JSON
  COALESCE(SUM(CASE 
    WHEN eventName = 'payment_completed' 
    THEN SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.amount') AS FLOAT64) 
    ELSE 0 
  END), 0) as revenue,
  
  -- Conversion Rates (calculated)
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_completed' THEN userId END),
    COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_initiated' THEN userId END)
  ) * 100 as trip_completion_rate,
  
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN eventName = 'booking_completed' THEN userId END),
    COUNT(DISTINCT CASE WHEN eventName = 'booking_initiated' THEN userId END)
  ) * 100 as booking_conversion_rate,
  
  -- NEW: Itinerary Success Rate
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN eventName = 'itinerary_completed' AND JSON_EXTRACT_SCALAR(properties, '$.success') = 'true' THEN JSON_EXTRACT_SCALAR(properties, '$.itineraryId') END),
    COUNT(DISTINCT CASE WHEN eventName = 'itinerary_created' THEN JSON_EXTRACT_SCALAR(properties, '$.itineraryId') END)
  ) * 100 as itinerary_success_rate,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated

FROM `tripaiplanner.analytics.raw_events`
WHERE DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
GROUP BY date;
