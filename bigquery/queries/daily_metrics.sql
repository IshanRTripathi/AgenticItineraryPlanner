-- Daily Metrics Aggregation - SCHEDULED QUERY
-- Run daily at 2 AM UTC to update yesterday's metrics
-- This query uses MERGE to update existing records or insert new ones

MERGE `tripaiplanner.analytics.daily_metrics` T
USING (
  SELECT
    DATE(TIMESTAMP_MILLIS(timestamp)) as date,
    COUNT(DISTINCT userId) as dau,
    COUNT(DISTINCT CASE WHEN eventName = 'user_signup_completed' THEN userId END) as new_signups,
    COUNT(DISTINCT CASE WHEN eventName = 'user_login_completed' THEN userId END) as daily_logins,
    COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_initiated' THEN userId END) as trip_wizard_starts,
    COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_completed' THEN userId END) as trips_created,
    COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_failed' THEN userId END) as trip_creation_failures,
    COUNT(DISTINCT CASE WHEN eventName = 'booking_initiated' THEN userId END) as booking_initiations,
    COUNT(DISTINCT CASE WHEN eventName = 'booking_completed' THEN userId END) as bookings_completed,
    COUNT(CASE WHEN eventName = 'pdf_export_completed' THEN 1 END) as pdf_exports,
    COUNT(CASE WHEN eventName = 'public_link_created' THEN 1 END) as public_links_created,
    COUNT(CASE WHEN eventName = 'page_view' THEN 1 END) as total_page_views,
    COUNT(DISTINCT CASE WHEN eventName = 'page_view' THEN sessionId END) as unique_sessions,
    
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
    
    -- Metadata
    CURRENT_TIMESTAMP() as last_updated
  FROM `tripaiplanner.analytics.raw_events`
  WHERE DATE(TIMESTAMP_MILLIS(timestamp)) = DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY)
  GROUP BY date
) S
ON T.date = S.date
WHEN MATCHED THEN
  UPDATE SET
    dau = S.dau,
    new_signups = S.new_signups,
    daily_logins = S.daily_logins,
    trip_wizard_starts = S.trip_wizard_starts,
    trips_created = S.trips_created,
    trip_creation_failures = S.trip_creation_failures,
    booking_initiations = S.booking_initiations,
    bookings_completed = S.bookings_completed,
    pdf_exports = S.pdf_exports,
    public_links_created = S.public_links_created,
    total_page_views = S.total_page_views,
    unique_sessions = S.unique_sessions,
    revenue = S.revenue,
    trip_completion_rate = S.trip_completion_rate,
    booking_conversion_rate = S.booking_conversion_rate,
    last_updated = S.last_updated
WHEN NOT MATCHED THEN
  INSERT (date, dau, new_signups, daily_logins, trip_wizard_starts, trips_created, 
          trip_creation_failures, booking_initiations, bookings_completed, pdf_exports,
          public_links_created, total_page_views, unique_sessions, revenue,
          trip_completion_rate, booking_conversion_rate, last_updated)
  VALUES (S.date, S.dau, S.new_signups, S.daily_logins, S.trip_wizard_starts, S.trips_created,
          S.trip_creation_failures, S.booking_initiations, S.bookings_completed, S.pdf_exports,
          S.public_links_created, S.total_page_views, S.unique_sessions, S.revenue,
          S.trip_completion_rate, S.booking_conversion_rate, S.last_updated);
