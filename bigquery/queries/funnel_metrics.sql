-- Conversion Funnel Metrics
-- Scheduled to run daily at 2 AM UTC
-- Tracks user journey through key conversion funnels

CREATE OR REPLACE TABLE `tripaiplanner-4c951.analytics.funnel_metrics`
PARTITION BY date
CLUSTER BY date
AS
WITH daily_events AS (
  SELECT
    DATE(TIMESTAMP_MILLIS(timestamp)) as date,
    userId,
    sessionId,
    eventName,
    timestamp
  FROM `tripaiplanner-4c951.analytics.raw_events`
  WHERE DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAYS)
)
SELECT
  date,
  
  -- Trip Creation Funnel
  COUNT(DISTINCT CASE WHEN eventName = 'trip_wizard_started' THEN userId END) as funnel_trip_wizard_started,
  COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_initiated' THEN userId END) as funnel_trip_initiated,
  COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_completed' THEN userId END) as funnel_trip_completed,
  
  -- Trip Creation Conversion Rates
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_initiated' THEN userId END),
    COUNT(DISTINCT CASE WHEN eventName = 'trip_wizard_started' THEN userId END)
  ) * 100 as trip_wizard_to_initiation_rate,
  
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_completed' THEN userId END),
    COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_initiated' THEN userId END)
  ) * 100 as trip_initiation_to_completion_rate,
  
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_completed' THEN userId END),
    COUNT(DISTINCT CASE WHEN eventName = 'trip_wizard_started' THEN userId END)
  ) * 100 as trip_overall_conversion_rate,
  
  -- Booking Funnel
  COUNT(DISTINCT CASE WHEN eventName = 'booking_initiated' THEN userId END) as funnel_booking_initiated,
  COUNT(DISTINCT CASE WHEN eventName = 'booking_completed' THEN userId END) as funnel_booking_completed,
  
  -- Booking Conversion Rate
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN eventName = 'booking_completed' THEN userId END),
    COUNT(DISTINCT CASE WHEN eventName = 'booking_initiated' THEN userId END)
  ) * 100 as booking_conversion_rate,
  
  -- Payment Funnel (if available)
  COUNT(DISTINCT CASE WHEN eventName = 'payment_initiated' THEN userId END) as funnel_payment_initiated,
  COUNT(DISTINCT CASE WHEN eventName = 'payment_completed' THEN userId END) as funnel_payment_completed,
  
  -- Payment Conversion Rate
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN eventName = 'payment_completed' THEN userId END),
    COUNT(DISTINCT CASE WHEN eventName = 'payment_initiated' THEN userId END)
  ) * 100 as payment_conversion_rate,
  
  -- Export Funnel
  COUNT(DISTINCT CASE WHEN eventName = 'pdf_export_initiated' THEN userId END) as funnel_export_initiated,
  COUNT(DISTINCT CASE WHEN eventName = 'pdf_export_completed' THEN userId END) as funnel_export_completed,
  
  -- Export Success Rate
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN eventName = 'pdf_export_completed' THEN userId END),
    COUNT(DISTINCT CASE WHEN eventName = 'pdf_export_initiated' THEN userId END)
  ) * 100 as export_success_rate,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated

FROM daily_events
GROUP BY date
ORDER BY date DESC;

-- User Journey Analysis (Session-based)
CREATE OR REPLACE TABLE `tripaiplanner-4c951.analytics.user_journey_sessions`
PARTITION BY date
CLUSTER BY date
AS
WITH session_events AS (
  SELECT
    DATE(TIMESTAMP_MILLIS(timestamp)) as date,
    sessionId,
    userId,
    eventName,
    timestamp,
    ROW_NUMBER() OVER (PARTITION BY sessionId ORDER BY timestamp) as event_sequence
  FROM `tripaiplanner-4c951.analytics.raw_events`
  WHERE DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAYS)
),
session_summary AS (
  SELECT
    date,
    sessionId,
    userId,
    MIN(timestamp) as session_start,
    MAX(timestamp) as session_end,
    COUNT(*) as total_events,
    STRING_AGG(eventName, ' -> ' ORDER BY timestamp) as event_path,
    MAX(CASE WHEN eventName = 'trip_creation_completed' THEN 1 ELSE 0 END) as completed_trip,
    MAX(CASE WHEN eventName = 'booking_completed' THEN 1 ELSE 0 END) as completed_booking,
    MAX(CASE WHEN eventName = 'payment_completed' THEN 1 ELSE 0 END) as completed_payment
  FROM session_events
  GROUP BY date, sessionId, userId
)
SELECT
  date,
  sessionId,
  userId,
  session_start,
  session_end,
  TIMESTAMP_DIFF(TIMESTAMP_MILLIS(session_end), TIMESTAMP_MILLIS(session_start), SECOND) as session_duration_seconds,
  total_events,
  event_path,
  completed_trip,
  completed_booking,
  completed_payment,
  CASE
    WHEN completed_payment = 1 THEN 'converted_payment'
    WHEN completed_booking = 1 THEN 'converted_booking'
    WHEN completed_trip = 1 THEN 'converted_trip'
    ELSE 'no_conversion'
  END as conversion_status,
  CURRENT_TIMESTAMP() as last_updated
FROM session_summary
ORDER BY date DESC, session_start DESC;
