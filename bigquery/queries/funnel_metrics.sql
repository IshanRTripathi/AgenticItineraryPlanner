-- Conversion Funnel Metrics - SCHEDULED QUERY
-- Run daily at 2 AM UTC to update yesterday's funnel metrics
-- Tracks user journey through key conversion funnels

MERGE `tripaiplanner.analytics.funnel_metrics` T
USING (
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
  WHERE DATE(TIMESTAMP_MILLIS(timestamp)) = DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY)
  GROUP BY date
) S
ON T.date = S.date
WHEN MATCHED THEN
  UPDATE SET
    funnel_trip_wizard_started = S.funnel_trip_wizard_started,
    funnel_trip_initiated = S.funnel_trip_initiated,
    funnel_trip_completed = S.funnel_trip_completed,
    trip_wizard_to_initiation_rate = S.trip_wizard_to_initiation_rate,
    trip_initiation_to_completion_rate = S.trip_initiation_to_completion_rate,
    trip_overall_conversion_rate = S.trip_overall_conversion_rate,
    funnel_booking_initiated = S.funnel_booking_initiated,
    funnel_booking_completed = S.funnel_booking_completed,
    booking_conversion_rate = S.booking_conversion_rate,
    funnel_payment_initiated = S.funnel_payment_initiated,
    funnel_payment_completed = S.funnel_payment_completed,
    payment_conversion_rate = S.payment_conversion_rate,
    funnel_export_initiated = S.funnel_export_initiated,
    funnel_export_completed = S.funnel_export_completed,
    export_success_rate = S.export_success_rate,
    last_updated = S.last_updated
WHEN NOT MATCHED THEN
  INSERT (date, funnel_trip_wizard_started, funnel_trip_initiated, funnel_trip_completed,
          trip_wizard_to_initiation_rate, trip_initiation_to_completion_rate, trip_overall_conversion_rate,
          funnel_booking_initiated, funnel_booking_completed, booking_conversion_rate,
          funnel_payment_initiated, funnel_payment_completed, payment_conversion_rate,
          funnel_export_initiated, funnel_export_completed, export_success_rate, last_updated)
  VALUES (S.date, S.funnel_trip_wizard_started, S.funnel_trip_initiated, S.funnel_trip_completed,
          S.trip_wizard_to_initiation_rate, S.trip_initiation_to_completion_rate, S.trip_overall_conversion_rate,
          S.funnel_booking_initiated, S.funnel_booking_completed, S.booking_conversion_rate,
          S.funnel_payment_initiated, S.funnel_payment_completed, S.payment_conversion_rate,
          S.funnel_export_initiated, S.funnel_export_completed, S.export_success_rate, S.last_updated);
