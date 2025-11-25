-- Cost Breakdown Daily USD Table Schema
-- Create empty table structure, data will be populated by scheduled query

CREATE TABLE IF NOT EXISTS `tripaiplanner.analytics.cost_breakdown_daily_usd`
(
  date DATE NOT NULL,
  node_type STRING,
  currency STRING,
  
  -- Volume
  cost_estimates_count INT64,
  unique_itineraries INT64,
  
  -- Cost Metrics (Original Currency)
  total_cost FLOAT64,
  avg_cost_per_activity FLOAT64,
  min_cost FLOAT64,
  max_cost FLOAT64,
  p50_cost FLOAT64,
  p95_cost FLOAT64,
  
  -- Cost Metrics (USD Normalized)
  total_cost_usd FLOAT64,
  avg_cost_per_activity_usd FLOAT64,
  min_cost_usd FLOAT64,
  max_cost_usd FLOAT64,
  
  -- Top Activities
  top_expensive_activities ARRAY<STRUCT<value STRING, count INT64>>,
  
  -- Metadata
  last_updated TIMESTAMP
)
PARTITION BY date
CLUSTER BY date, node_type, currency;
