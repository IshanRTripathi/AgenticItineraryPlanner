-- User Engagement Metrics
-- Scheduled to run daily at 2 AM UTC
-- Tracks user activity, retention, and engagement patterns

CREATE OR REPLACE TABLE `tripaiplanner.analytics.user_engagement_daily`
PARTITION BY date
CLUSTER BY date
AS
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  
  -- Active Users
  COUNT(DISTINCT userId) as dau,
  COUNT(DISTINCT sessionId) as total_sessions,
  
  -- Session Metrics
  SAFE_DIVIDE(COUNT(*), COUNT(DISTINCT sessionId)) as avg_events_per_session,
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN eventName = 'page_view' THEN sessionId END),
    COUNT(DISTINCT sessionId)
  ) as sessions_with_page_views,
  
  -- Page View Metrics
  COUNT(CASE WHEN eventName = 'page_view' THEN 1 END) as total_page_views,
  SAFE_DIVIDE(
    COUNT(CASE WHEN eventName = 'page_view' THEN 1 END),
    COUNT(DISTINCT userId)
  ) as avg_page_views_per_user,
  
  -- Feature Usage
  COUNT(DISTINCT CASE WHEN eventName = 'activity_viewed' THEN userId END) as users_viewing_activities,
  COUNT(DISTINCT CASE WHEN eventName = 'day_expanded' THEN userId END) as users_expanding_days,
  COUNT(DISTINCT CASE WHEN eventName = 'chat_message_sent' THEN userId END) as users_using_chat,
  COUNT(DISTINCT CASE WHEN eventName = 'search_initiated' THEN userId END) as users_searching,
  
  -- Export & Share
  COUNT(DISTINCT CASE WHEN eventName = 'pdf_export_completed' THEN userId END) as users_exporting_pdf,
  COUNT(DISTINCT CASE WHEN eventName = 'public_link_created' THEN userId END) as users_creating_links,
  
  -- Engagement Score (composite metric)
  (
    COUNT(DISTINCT CASE WHEN eventName = 'trip_creation_completed' THEN userId END) * 10 +
    COUNT(DISTINCT CASE WHEN eventName = 'booking_initiated' THEN userId END) * 5 +
    COUNT(DISTINCT CASE WHEN eventName = 'pdf_export_completed' THEN userId END) * 3 +
    COUNT(DISTINCT CASE WHEN eventName = 'page_view' THEN userId END) * 1
  ) as engagement_score,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated

FROM `tripaiplanner.analytics.raw_events`
WHERE DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAYS)
GROUP BY date
ORDER BY date DESC;

-- User Retention Cohorts
CREATE OR REPLACE TABLE `tripaiplanner.analytics.user_retention_cohorts`
AS
WITH user_first_seen AS (
  SELECT
    userId,
    DATE(TIMESTAMP_MILLIS(MIN(timestamp))) as cohort_date
  FROM `tripaiplanner.analytics.raw_events`
  WHERE userId IS NOT NULL
  GROUP BY userId
),
user_activity AS (
  SELECT DISTINCT
    userId,
    DATE(TIMESTAMP_MILLIS(timestamp)) as activity_date
  FROM `tripaiplanner.analytics.raw_events`
  WHERE userId IS NOT NULL
),
retention_data AS (
  SELECT
    ufs.cohort_date,
    ufs.userId,
    ua.activity_date,
    DATE_DIFF(ua.activity_date, ufs.cohort_date, DAY) as days_since_signup
  FROM user_first_seen ufs
  JOIN user_activity ua ON ufs.userId = ua.userId
)
SELECT
  cohort_date,
  COUNT(DISTINCT userId) as cohort_size,
  
  -- Day 0 (signup day)
  COUNT(DISTINCT CASE WHEN days_since_signup = 0 THEN userId END) as day_0_retained,
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN days_since_signup = 0 THEN userId END),
    COUNT(DISTINCT userId)
  ) * 100 as day_0_retention_rate,
  
  -- Day 1
  COUNT(DISTINCT CASE WHEN days_since_signup = 1 THEN userId END) as day_1_retained,
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN days_since_signup = 1 THEN userId END),
    COUNT(DISTINCT userId)
  ) * 100 as day_1_retention_rate,
  
  -- Day 7
  COUNT(DISTINCT CASE WHEN days_since_signup = 7 THEN userId END) as day_7_retained,
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN days_since_signup = 7 THEN userId END),
    COUNT(DISTINCT userId)
  ) * 100 as day_7_retention_rate,
  
  -- Day 30
  COUNT(DISTINCT CASE WHEN days_since_signup = 30 THEN userId END) as day_30_retained,
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN days_since_signup = 30 THEN userId END),
    COUNT(DISTINCT userId)
  ) * 100 as day_30_retention_rate,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated

FROM retention_data
WHERE cohort_date >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAYS)
GROUP BY cohort_date
ORDER BY cohort_date DESC;

-- Weekly Active Users (WAU) and Monthly Active Users (MAU)
CREATE OR REPLACE TABLE `tripaiplanner.analytics.wau_mau_metrics`
AS
WITH date_range AS (
  SELECT date
  FROM UNNEST(GENERATE_DATE_ARRAY(DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAYS), CURRENT_DATE())) as date
),
daily_users AS (
  SELECT
    DATE(TIMESTAMP_MILLIS(timestamp)) as date,
    userId
  FROM `tripaiplanner.analytics.raw_events`
  WHERE userId IS NOT NULL
    AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAYS)
  GROUP BY date, userId
)
SELECT
  dr.date,
  
  -- DAU
  COUNT(DISTINCT CASE WHEN du.date = dr.date THEN du.userId END) as dau,
  
  -- WAU (7-day rolling window)
  COUNT(DISTINCT CASE 
    WHEN du.date BETWEEN DATE_SUB(dr.date, INTERVAL 6 DAY) AND dr.date 
    THEN du.userId 
  END) as wau,
  
  -- MAU (30-day rolling window)
  COUNT(DISTINCT CASE 
    WHEN du.date BETWEEN DATE_SUB(dr.date, INTERVAL 29 DAY) AND dr.date 
    THEN du.userId 
  END) as mau,
  
  -- Stickiness (DAU/MAU ratio)
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN du.date = dr.date THEN du.userId END),
    COUNT(DISTINCT CASE 
      WHEN du.date BETWEEN DATE_SUB(dr.date, INTERVAL 29 DAY) AND dr.date 
      THEN du.userId 
    END)
  ) * 100 as stickiness_ratio,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated

FROM date_range dr
LEFT JOIN daily_users du ON du.date <= dr.date
GROUP BY dr.date
ORDER BY dr.date DESC;
