-- Cost Breakdown Daily USD - SCHEDULED QUERY (Fixed with JOIN)
-- Run daily at 2 AM UTC to update yesterday's cost metrics with USD normalization

MERGE `tripaiplanner.analytics.cost_breakdown_daily_usd` T
USING (
  WITH latest_rates AS (
    -- Get the most recent exchange rate for each currency
    SELECT 
      currency_code,
      usd_rate,
      effective_date,
      ROW_NUMBER() OVER (PARTITION BY currency_code ORDER BY effective_date DESC) as rn
    FROM `tripaiplanner.analytics.exchange_rates`
    WHERE is_active = TRUE
  ),
  cost_data AS (
    SELECT
      DATE(TIMESTAMP_MILLIS(timestamp)) as date,
      JSON_EXTRACT_SCALAR(properties, '$.nodeType') as node_type,
      JSON_EXTRACT_SCALAR(properties, '$.currency') as currency,
      SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.costPerPerson') AS FLOAT64) as cost_per_person,
      JSON_EXTRACT_SCALAR(properties, '$.itineraryId') as itinerary_id,
      JSON_EXTRACT_SCALAR(properties, '$.activityName') as activity_name
    FROM `tripaiplanner.analytics.raw_events`
    WHERE eventName = 'cost_estimated'
      AND DATE(TIMESTAMP_MILLIS(timestamp)) = DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY)
  )
  SELECT
    cd.date,
    cd.node_type,
    cd.currency,
    
    -- Volume
    COUNT(*) as cost_estimates_count,
    COUNT(DISTINCT cd.itinerary_id) as unique_itineraries,
    
    -- Cost Metrics (Original Currency)
    COALESCE(SUM(cd.cost_per_person), 0) as total_cost,
    COALESCE(AVG(cd.cost_per_person), 0) as avg_cost_per_activity,
    COALESCE(MIN(cd.cost_per_person), 0) as min_cost,
    COALESCE(MAX(cd.cost_per_person), 0) as max_cost,
    COALESCE(APPROX_QUANTILES(cd.cost_per_person, 100)[OFFSET(50)], 0) as p50_cost,
    COALESCE(APPROX_QUANTILES(cd.cost_per_person, 100)[OFFSET(95)], 0) as p95_cost,
    
    -- Cost Metrics (USD Normalized) - Using JOIN instead of correlated subquery
    COALESCE(SUM(cd.cost_per_person * COALESCE(lr.usd_rate, 1.0)), 0) as total_cost_usd,
    COALESCE(AVG(cd.cost_per_person * COALESCE(lr.usd_rate, 1.0)), 0) as avg_cost_per_activity_usd,
    COALESCE(MIN(cd.cost_per_person * COALESCE(lr.usd_rate, 1.0)), 0) as min_cost_usd,
    COALESCE(MAX(cd.cost_per_person * COALESCE(lr.usd_rate, 1.0)), 0) as max_cost_usd,
    
    -- Top Activities by Cost
    APPROX_TOP_COUNT(cd.activity_name, 10) as top_expensive_activities,
    
    CURRENT_TIMESTAMP() as last_updated
    
  FROM cost_data cd
  LEFT JOIN latest_rates lr ON cd.currency = lr.currency_code AND lr.rn = 1
  GROUP BY cd.date, cd.node_type, cd.currency
) S
ON T.date = S.date AND T.node_type = S.node_type AND T.currency = S.currency
WHEN MATCHED THEN
  UPDATE SET
    cost_estimates_count = S.cost_estimates_count,
    unique_itineraries = S.unique_itineraries,
    total_cost = S.total_cost,
    avg_cost_per_activity = S.avg_cost_per_activity,
    min_cost = S.min_cost,
    max_cost = S.max_cost,
    p50_cost = S.p50_cost,
    p95_cost = S.p95_cost,
    total_cost_usd = S.total_cost_usd,
    avg_cost_per_activity_usd = S.avg_cost_per_activity_usd,
    min_cost_usd = S.min_cost_usd,
    max_cost_usd = S.max_cost_usd,
    top_expensive_activities = S.top_expensive_activities,
    last_updated = S.last_updated
WHEN NOT MATCHED THEN
  INSERT (date, node_type, currency, cost_estimates_count, unique_itineraries,
          total_cost, avg_cost_per_activity, min_cost, max_cost, p50_cost, p95_cost,
          total_cost_usd, avg_cost_per_activity_usd, min_cost_usd, max_cost_usd,
          top_expensive_activities, last_updated)
  VALUES (S.date, S.node_type, S.currency, S.cost_estimates_count, S.unique_itineraries,
          S.total_cost, S.avg_cost_per_activity, S.min_cost, S.max_cost, S.p50_cost, S.p95_cost,
          S.total_cost_usd, S.avg_cost_per_activity_usd, S.min_cost_usd, S.max_cost_usd,
          S.top_expensive_activities, S.last_updated);
