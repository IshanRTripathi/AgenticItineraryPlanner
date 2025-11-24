# USD Normalization - Deployment Summary

## ✅ Deployed Successfully

### Tables Created
1. **`tripaiplanner.analytics.exchange_rates`**
   - 165 currencies loaded
   - Partitioned by effective_date
   - Clustered by currency_code, effective_date

2. **`tripaiplanner.analytics.cost_breakdown_daily_usd`**
   - Empty table structure created
   - Ready for scheduled query population
   - Partitioned by date
   - Clustered by date, node_type, currency

3. **`tripaiplanner.analytics.v_cost_breakdown_usd`**
   - View for simplified USD-aggregated queries
   - Combines all currencies into USD totals

---

## 📁 Essential Files

### Data
- `bigquery/data/initial_exchange_rates.sql` - 165 currency rates (already loaded)

### Schema
- `bigquery/schemas/cost_breakdown_daily_usd_schema.sql` - Table structure (already created)

### Queries
- `bigquery/queries/cost_breakdown_daily_usd_v2.sql` - Scheduled query (uses JOIN, not correlated subquery)
- `bigquery/queries/detect_missing_currencies.sql` - Monitor for new currencies
- `bigquery/views/cost_breakdown_usd_view.sql` - Aggregated view (already created)

### Deployment
- `bigquery/deploy_usd.ps1` - Deployment script (already run)

### Documentation
- `bigquery/CURRENCY_NORMALIZATION_STRATEGY.md` - Complete technical guide
- `bigquery/CURRENCY_SETUP_QUICKSTART.md` - Quick reference
- `TRIP_COST_BREAKDOWN_DATA_FLOW.md` - Data flow documentation

---

## 🔄 Next Steps

### 1. Set Up Scheduled Query (Daily Updates)
```bash
bq query \
  --project_id=tripaiplanner \
  --display_name="Cost Breakdown USD Daily" \
  --schedule="every day 02:00" \
  --replace=true \
  --use_legacy_sql=false \
  < bigquery/queries/cost_breakdown_daily_usd_v2.sql
```

### 2. Test the View
```sql
SELECT 
  date,
  node_type,
  total_cost_usd,
  total_activities,
  currencies_used
FROM `tripaiplanner.analytics.v_cost_breakdown_usd`
WHERE date >= DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY)
ORDER BY date DESC, total_cost_usd DESC
LIMIT 10;
```

### 3. Check for Missing Currencies
```bash
bq query --use_legacy_sql=false < bigquery/queries/detect_missing_currencies.sql
```

---

## 📊 Dashboard Queries

### Cost Breakdown by Category (USD)
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

### Cost Trends Over Time (USD)
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

### Simplified View (All Currencies Aggregated)
```sql
SELECT * 
FROM `tripaiplanner.analytics.v_cost_breakdown_usd`
WHERE date >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
ORDER BY date DESC, total_cost_usd DESC;
```

---

## 🔧 Maintenance

### Adding New Currencies
When a new currency is detected:

1. **Add to BigQuery:**
```sql
INSERT INTO `tripaiplanner.analytics.exchange_rates`
(currency_code, usd_rate, effective_date, source, is_active, notes)
VALUES ('XXX', 0.XXX, CURRENT_DATE(), 'manual', TRUE, 'Currency Name');
```

2. **Update Backend** (optional, for consistency):
Edit `src/main/java/com/tripplanner/service/CurrencyConversionService.java`

### Updating Exchange Rates
```sql
UPDATE `tripaiplanner.analytics.exchange_rates`
SET usd_rate = 0.XXX, last_updated = CURRENT_TIMESTAMP()
WHERE currency_code = 'XXX' AND effective_date = CURRENT_DATE();
```

---

## ✅ Verification Commands

### Check exchange rates loaded
```bash
bq query --use_legacy_sql=false "SELECT COUNT(*) FROM \`tripaiplanner.analytics.exchange_rates\`"
```

### Check table structure
```bash
bq show tripaiplanner:analytics.cost_breakdown_daily_usd
```

### Check view exists
```bash
bq show tripaiplanner:analytics.v_cost_breakdown_usd
```

---

## 📝 Notes

- All costs are normalized to USD for easy comparison
- Original currency data is preserved in the `currency` and `total_cost` fields
- Exchange rates are date-based for historical accuracy
- The scheduled query uses JOIN instead of correlated subqueries for BigQuery compatibility
- 0 rows affected is normal if no cost_estimated events exist for yesterday
