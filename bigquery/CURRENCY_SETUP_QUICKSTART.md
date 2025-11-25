# Currency Normalization - Quick Setup Guide

## Problem Solved
Cost data is stored in multiple currencies (INR, MYR, USD, EUR, etc.), making it impossible to compare or aggregate costs across destinations. This solution normalizes everything to USD.

## Quick Deploy (5 minutes)

### Option 1: PowerShell (Windows)
```powershell
cd bigquery
.\deploy_currency_normalization.ps1
```

### Option 2: Bash (Linux/Mac)
```bash
cd bigquery
chmod +x deploy_currency_normalization.sh
./deploy_currency_normalization.sh
```

## What Gets Created

1. **`exchange_rates` table** - Stores currency conversion rates
   - 35+ currencies pre-loaded
   - Date-based rates for historical accuracy
   - Easy to add new currencies

2. **`cost_breakdown_daily_usd` table** - Enhanced cost breakdown with USD
   - All original fields (total_cost, avg_cost, etc.)
   - New USD fields (total_cost_usd, avg_cost_per_activity_usd, etc.)
   - Automatic currency conversion using exchange rates

3. **`v_cost_breakdown_usd` view** - Simplified aggregated view
   - All currencies combined into USD
   - Ready for Looker Studio dashboards

## Dashboard Queries

### 1. Cost Breakdown by Category (USD)
```sql
SELECT
  node_type as category,
  SUM(total_cost_usd) as total_usd,
  AVG(avg_cost_per_activity_usd) as avg_per_activity_usd,
  SUM(cost_estimates_count) as activity_count
FROM `tripaiplanner.analytics.cost_breakdown_daily_usd`
WHERE date >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
GROUP BY node_type
ORDER BY total_usd DESC;
```

**Use in Looker Studio:**
- Dimension: `category`
- Metric: `total_usd`
- Chart: Pie chart or Bar chart

### 2. Cost Trends Over Time (USD)
```sql
SELECT
  date,
  SUM(total_cost_usd) as daily_total_usd,
  SUM(cost_estimates_count) as daily_activities
FROM `tripaiplanner.analytics.cost_breakdown_daily_usd`
WHERE date >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
GROUP BY date
ORDER BY date;
```

**Use in Looker Studio:**
- Dimension: `date`
- Metric: `daily_total_usd`
- Chart: Time series line chart

### 3. Simplified View (All Currencies Aggregated)
```sql
SELECT * 
FROM `tripaiplanner.analytics.v_cost_breakdown_usd`
WHERE date >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
ORDER BY date DESC, total_cost_usd DESC;
```

## Adding New Currencies

### When a new currency is detected:

1. **Check for missing currencies:**
```sql
-- Run this query to find currencies without rates
SELECT * FROM `tripaiplanner.analytics.detect_missing_currencies`;
```

2. **Add to backend** (`CurrencyConversionService.java`):
```java
EXCHANGE_RATES.put("XXX", 0.XXX);  // 1 INR = X XXX
CURRENCY_SYMBOLS.put("XXX", "X");
```

3. **Add to BigQuery:**
```sql
INSERT INTO `tripaiplanner.analytics.exchange_rates`
(currency_code, usd_rate, effective_date, source, is_active, notes)
VALUES ('XXX', 0.XXX, CURRENT_DATE(), 'manual', TRUE, 'Currency Name');
```

**Note:** USD rate = (INR_to_currency_rate / 0.012)

Example: If 1 INR = 0.056 MYR, then USD rate = 0.056 / 0.012 = 4.67

## Scheduled Query Setup

Set up daily updates for the cost breakdown table:

```bash
bq query \
  --project_id=tripaiplanner \
  --use_legacy_sql=false \
  --display_name="Cost Breakdown Daily USD" \
  --schedule="every day 02:00" \
  --replace=true \
  < bigquery/queries/cost_breakdown_daily_usd.sql
```

## Verification

### Check exchange rates loaded:
```sql
SELECT currency_code, usd_rate, effective_date
FROM `tripaiplanner.analytics.exchange_rates`
WHERE is_active = TRUE
ORDER BY currency_code;
```

### Check USD conversion working:
```sql
SELECT 
  currency,
  SUM(total_cost) as original_total,
  SUM(total_cost_usd) as usd_total,
  COUNT(*) as records
FROM `tripaiplanner.analytics.cost_breakdown_daily_usd`
WHERE date >= DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY)
GROUP BY currency;
```

### Check for missing currencies:
```sql
SELECT * FROM `tripaiplanner.analytics.detect_missing_currencies`;
```

## Troubleshooting

### Issue: USD fields are 0 or NULL
**Cause:** Missing exchange rate for that currency
**Fix:** Add the currency to `exchange_rates` table (see "Adding New Currencies" above)

### Issue: Conversion seems wrong
**Cause:** Incorrect exchange rate
**Fix:** Update the rate in `exchange_rates` table:
```sql
UPDATE `tripaiplanner.analytics.exchange_rates`
SET usd_rate = 0.XXX, last_updated = CURRENT_TIMESTAMP()
WHERE currency_code = 'XXX' AND effective_date = CURRENT_DATE();
```

### Issue: Historical data not converted
**Cause:** Exchange rate added after data was created
**Fix:** Backfill by re-running the scheduled query for historical dates

## Next Steps

1. ✅ Deploy currency normalization (run script above)
2. ⏭️ Set up scheduled query for daily updates
3. ⏭️ Update Looker Studio dashboards to use USD fields
4. ⏭️ Set up monitoring for missing currencies
5. ⏭️ (Optional) Deploy Cloud Function for automatic rate updates

## Support

- Full documentation: `bigquery/CURRENCY_NORMALIZATION_STRATEGY.md`
- Data flow: `TRIP_COST_BREAKDOWN_DATA_FLOW.md`
- Questions: Check existing exchange rates and conversion logic
