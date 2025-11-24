-- Cost Breakdown USD View
-- Simplified view that aggregates all currencies into USD for easy dashboard consumption
-- Use this view in Looker Studio for unified cost reporting

CREATE OR REPLACE VIEW `tripaiplanner.analytics.v_cost_breakdown_usd` AS
SELECT
  date,
  node_type,
  
  -- Aggregated across all currencies in USD
  SUM(total_cost_usd) as total_cost_usd,
  AVG(avg_cost_per_activity_usd) as avg_cost_per_activity_usd,
  MIN(min_cost_usd) as min_cost_usd,
  MAX(max_cost_usd) as max_cost_usd,
  
  -- Volume metrics
  SUM(cost_estimates_count) as total_activities,
  SUM(unique_itineraries) as total_itineraries,
  
  -- Currency breakdown (for reference)
  STRING_AGG(DISTINCT currency ORDER BY currency) as currencies_used,
  COUNT(DISTINCT currency) as currency_count,
  
  -- Metadata
  MAX(last_updated) as last_updated
  
FROM `tripaiplanner.analytics.cost_breakdown_daily_usd`
GROUP BY date, node_type
ORDER BY date DESC, total_cost_usd DESC;

-- Usage example:
-- SELECT * FROM `tripaiplanner.analytics.v_cost_breakdown_usd`
-- WHERE date >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
-- ORDER BY date DESC, total_cost_usd DESC;
