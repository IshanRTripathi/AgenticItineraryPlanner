-- TEST QUERY: Run this after using the app to verify all events are tracked correctly
-- This shows which events exist vs which are still missing

WITH expected_events AS (
  SELECT event_name, category FROM UNNEST([
    STRUCT('page_view' AS event_name, 'Core' AS category),
    STRUCT('user_signup_completed', 'Auth'),
    STRUCT('user_login_started', 'Auth'),
    STRUCT('user_login_completed', 'Auth'),
    STRUCT('trip_wizard_started', 'Trip Creation'),
    STRUCT('trip_creation_initiated', 'Trip Creation'),
    STRUCT('trip_creation_completed', 'Trip Creation'),
    STRUCT('trip_creation_failed', 'Trip Creation'),
    STRUCT('booking_initiated', 'Booking'),
    STRUCT('booking_completed', 'Booking'),
    STRUCT('booking_failed', 'Booking'),
    STRUCT('payment_initiated', 'Payment'),
    STRUCT('payment_completed', 'Payment'),
    STRUCT('payment_failed', 'Payment'),
    STRUCT('pdf_export_initiated', 'Export'),
    STRUCT('pdf_export_completed', 'Export'),
    STRUCT('pdf_export_failed', 'Export'),
    STRUCT('public_link_created', 'Sharing'),
    STRUCT('activity_viewed', 'Engagement'),
    STRUCT('day_expanded', 'Engagement'),
    STRUCT('chat_message_sent', 'Engagement'),
    STRUCT('search_initiated', 'Engagement'),
    STRUCT('llm_token_usage', 'Backend'),
    STRUCT('agent_started', 'Backend'),
    STRUCT('agent_completed', 'Backend'),
    STRUCT('agent_failed', 'Backend')
  ])
),
actual_events AS (
  SELECT 
    eventName,
    COUNT(*) as count,
    MIN(TIMESTAMP_MILLIS(timestamp)) as first_seen,
    MAX(TIMESTAMP_MILLIS(timestamp)) as last_seen
  FROM `tripaiplanner.analytics.raw_events`
  GROUP BY eventName
)
SELECT 
  e.category,
  e.event_name,
  CASE 
    WHEN a.eventName IS NOT NULL THEN '✅ TRACKED'
    ELSE '❌ MISSING'
  END as status,
  COALESCE(a.count, 0) as event_count,
  a.first_seen,
  a.last_seen
FROM expected_events e
LEFT JOIN actual_events a ON e.event_name = a.eventName
ORDER BY 
  e.category,
  CASE WHEN a.eventName IS NOT NULL THEN 0 ELSE 1 END,
  e.event_name;
