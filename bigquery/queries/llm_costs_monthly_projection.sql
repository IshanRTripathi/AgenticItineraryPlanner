-- LLM Costs Monthly Projection - FIXED VERSION
-- Projects end-of-month costs based on current usage
-- FIXED: Now uses llm_requests_detailed with proper token breakdown

CREATE OR REPLACE TABLE `tripaiplanner.analytics.llm_costs_monthly_projection` AS
WITH daily_tokens AS (
  SELECT
    date,
    SUM(total_prompt_tokens) as daily_prompt_tokens,
    SUM(total_response_tokens) as daily_response_tokens,
    SUM(total_thoughts_tokens) as daily_thoughts_tokens,
    SUM(total_tokens) as daily_total_tokens
  FROM `tripaiplanner.analytics.llm_requests_detailed`
  WHERE DATE_TRUNC(date, MONTH) = DATE_TRUNC(CURRENT_DATE(), MONTH)
  GROUP BY date
),
monthly_summary AS (
  SELECT
    DATE_TRUNC(CURRENT_DATE(), MONTH) as month,
    SUM(daily_prompt_tokens) as month_to_date_prompt_tokens,
    SUM(daily_response_tokens) as month_to_date_response_tokens,
    SUM(daily_thoughts_tokens) as month_to_date_thoughts_tokens,
    SUM(daily_total_tokens) as month_to_date_total_tokens,
    COUNT(DISTINCT date) as days_elapsed,
    DATE_DIFF(LAST_DAY(CURRENT_DATE()), CURRENT_DATE(), DAY) + 1 as days_remaining,
    DATE_DIFF(LAST_DAY(CURRENT_DATE()), DATE_TRUNC(CURRENT_DATE(), MONTH), DAY) + 1 as days_in_month
  FROM daily_tokens
)
SELECT
  month,
  
  -- Current Usage
  month_to_date_prompt_tokens as current_prompt_tokens,
  month_to_date_response_tokens as current_response_tokens,
  month_to_date_thoughts_tokens as current_thoughts_tokens,
  month_to_date_total_tokens as current_total_tokens,
  
  -- Time Metrics
  days_elapsed,
  days_remaining,
  days_in_month,
  
  -- Daily Averages
  SAFE_DIVIDE(month_to_date_prompt_tokens, days_elapsed) as avg_daily_prompt_tokens,
  SAFE_DIVIDE(month_to_date_response_tokens, days_elapsed) as avg_daily_response_tokens,
  SAFE_DIVIDE(month_to_date_total_tokens, days_elapsed) as avg_daily_total_tokens,
  
  -- Projected Monthly Totals
  SAFE_DIVIDE(month_to_date_prompt_tokens, days_elapsed) * days_in_month as projected_monthly_prompt_tokens,
  SAFE_DIVIDE(month_to_date_response_tokens, days_elapsed) * days_in_month as projected_monthly_response_tokens,
  SAFE_DIVIDE(month_to_date_total_tokens, days_elapsed) * days_in_month as projected_monthly_total_tokens,
  
  -- Cost Calculation (Gemini 2.5 Flash pricing)
  -- Prompt: $0.10 per 1M tokens, Response: $0.40 per 1M tokens
  (SAFE_DIVIDE(month_to_date_prompt_tokens, days_elapsed) * days_in_month * 0.10 / 1000000) as projected_prompt_cost_usd,
  (SAFE_DIVIDE(month_to_date_response_tokens, days_elapsed) * days_in_month * 0.40 / 1000000) as projected_response_cost_usd,
  
  -- Total Projected Cost
  (SAFE_DIVIDE(month_to_date_prompt_tokens, days_elapsed) * days_in_month * 0.10 / 1000000) +
  (SAFE_DIVIDE(month_to_date_response_tokens, days_elapsed) * days_in_month * 0.40 / 1000000) as projected_monthly_cost,
  
  -- Current Month-to-Date Cost
  (month_to_date_prompt_tokens * 0.10 / 1000000) +
  (month_to_date_response_tokens * 0.40 / 1000000) as current_cost,
  
  -- Cost Status
  CASE
    WHEN (SAFE_DIVIDE(month_to_date_prompt_tokens, days_elapsed) * days_in_month * 0.10 / 1000000) +
         (SAFE_DIVIDE(month_to_date_response_tokens, days_elapsed) * days_in_month * 0.40 / 1000000) > 200 THEN 'CRITICAL'
    WHEN (SAFE_DIVIDE(month_to_date_prompt_tokens, days_elapsed) * days_in_month * 0.10 / 1000000) +
         (SAFE_DIVIDE(month_to_date_response_tokens, days_elapsed) * days_in_month * 0.40 / 1000000) > 100 THEN 'WARNING'
    ELSE 'NORMAL'
  END as cost_status,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated
  
FROM monthly_summary;
