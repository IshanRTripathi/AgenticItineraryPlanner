-- Funnel Metrics Table Creation - RUN ONCE
-- Creates the funnel_metrics table with initial data from last 90 days

CREATE TABLE IF NOT EXISTS `tripaiplanner.analytics.funnel_metrics`
PARTITION BY date
CLUSTER BY date
AS
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  COUNT(DISTINCT CASE WHEN eventName = 'trip_wizard_started' THEN userId END) as funnel_trip_wizard_started,
  COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_initiated' THEN userId END) as funnel_trip_initiated,
  COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_completed' THEN userId END) as funnel_trip_completed,
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
  COUNT(DISTINCT CASE WHEN eventName = 'booking_initiated' THEN userId END) as funnel_booking_initiated,
  COUNT(DISTINCT CASE WHEN eventName = 'booking_completed' THEN userId END) as funnel_booking_completed,
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN eventName = 'booking_completed' THEN userId END),
    COUNT(DISTINCT CASE WHEN eventName = 'booking_initiated' THEN userId END)
  ) * 100 as booking_conversion_rate,
  COUNT(DISTINCT CASE WHEN eventName = 'payment_initiated' THEN userId END) as funnel_payment_initiated,
  COUNT(DISTINCT CASE WHEN eventName = 'payment_completed' THEN userId END) as funnel_payment_completed,
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN eventName = 'payment_completed' THEN userId END),
    COUNT(DISTINCT CASE WHEN eventName = 'payment_initiated' THEN userId END)
  ) * 100 as payment_conversion_rate,
  COUNT(DISTINCT CASE WHEN eventName = 'pdf_export_initiated' THEN userId END) as funnel_export_initiated,
  COUNT(DISTINCT CASE WHEN eventName = 'pdf_export_completed' THEN userId END) as funnel_export_completed,
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN eventName = 'pdf_export_completed' THEN userId END),
    COUNT(DISTINCT CASE WHEN eventName = 'pdf_export_initiated' THEN userId END)
  ) * 100 as export_success_rate,
  CURRENT_TIMESTAMP() as last_updated
FROM `tripaiplanner.analytics.raw_events`
WHERE DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
GROUP BY date;
