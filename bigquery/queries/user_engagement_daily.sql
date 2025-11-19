-- User Engagement Metrics - SCHEDULED QUERY
-- Run daily at 2 AM UTC to update yesterday's engagement metrics
-- Tracks user activity and engagement patterns

MERGE `tripaiplanner.analytics.user_engagement_daily` T
USING (
  SELECT
    DATE(TIMESTAMP_MILLIS(timestamp)) as date,
    COUNT(DISTINCT userId) as dau,
    COUNT(DISTINCT sessionId) as total_sessions,
    SAFE_DIVIDE(COUNT(*), COUNT(DISTINCT sessionId)) as avg_events_per_session,
    SAFE_DIVIDE(
      COUNT(DISTINCT CASE WHEN eventName = 'page_view' THEN sessionId END),
      COUNT(DISTINCT sessionId)
    ) as sessions_with_page_views,
    COUNT(CASE WHEN eventName = 'page_view' THEN 1 END) as total_page_views,
    SAFE_DIVIDE(
      COUNT(CASE WHEN eventName = 'page_view' THEN 1 END),
      COUNT(DISTINCT userId)
    ) as avg_page_views_per_user,
    COUNT(DISTINCT CASE WHEN eventName = 'activity_viewed' THEN userId END) as users_viewing_activities,
    COUNT(DISTINCT CASE WHEN eventName = 'day_expanded' THEN userId END) as users_expanding_days,
    COUNT(DISTINCT CASE WHEN eventName = 'chat_message_sent' THEN userId END) as users_using_chat,
    COUNT(DISTINCT CASE WHEN eventName = 'search_initiated' THEN userId END) as users_searching,
    COUNT(DISTINCT CASE WHEN eventName = 'pdf_export_completed' THEN userId END) as users_exporting_pdf,
    COUNT(DISTINCT CASE WHEN eventName = 'public_link_created' THEN userId END) as users_creating_links,
    (
      COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_completed' THEN userId END) * 10 +
      COUNT(DISTINCT CASE WHEN eventName = 'booking_initiated' THEN userId END) * 5 +
      COUNT(DISTINCT CASE WHEN eventName = 'pdf_export_completed' THEN userId END) * 3 +
      COUNT(DISTINCT CASE WHEN eventName = 'page_view' THEN userId END) * 1
    ) as engagement_score,
    CURRENT_TIMESTAMP() as last_updated
  FROM `tripaiplanner.analytics.raw_events`
  WHERE DATE(TIMESTAMP_MILLIS(timestamp)) = DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY)
  GROUP BY date
) S
ON T.date = S.date
WHEN MATCHED THEN
  UPDATE SET
    dau = S.dau,
    total_sessions = S.total_sessions,
    avg_events_per_session = S.avg_events_per_session,
    sessions_with_page_views = S.sessions_with_page_views,
    total_page_views = S.total_page_views,
    avg_page_views_per_user = S.avg_page_views_per_user,
    users_viewing_activities = S.users_viewing_activities,
    users_expanding_days = S.users_expanding_days,
    users_using_chat = S.users_using_chat,
    users_searching = S.users_searching,
    users_exporting_pdf = S.users_exporting_pdf,
    users_creating_links = S.users_creating_links,
    engagement_score = S.engagement_score,
    last_updated = S.last_updated
WHEN NOT MATCHED THEN
  INSERT (date, dau, total_sessions, avg_events_per_session, sessions_with_page_views,
          total_page_views, avg_page_views_per_user, users_viewing_activities, users_expanding_days,
          users_using_chat, users_searching, users_exporting_pdf, users_creating_links,
          engagement_score, last_updated)
  VALUES (S.date, S.dau, S.total_sessions, S.avg_events_per_session, S.sessions_with_page_views,
          S.total_page_views, S.avg_page_views_per_user, S.users_viewing_activities, S.users_expanding_days,
          S.users_using_chat, S.users_searching, S.users_exporting_pdf, S.users_creating_links,
          S.engagement_score, S.last_updated);
