# Currency Normalization Strategy for BigQuery Analytics

## Problem
Cost data is stored in multiple currencies (INR, MYR, USD, EUR, etc.), making it difficult to:
- Compare costs across different destinations
- Aggregate total costs in dashboards
- Track trends over time
- Generate meaningful reports

## Solution: USD Normalization in BigQuery

### Approach
1. **Store original currency data** in `raw_events` and `cost_breakdown_daily`
2. **Create exchange rate lookup table** in BigQuery
3. **Add USD-normalized fields** to aggregation tables
4. **Auto-update exchange rates** periodically
5. **Handle new currencies** automatically

---

## Implementation

### 1. Exchange Rate Table

**Table:** `tripaiplanner.analytics.exchange_rates`

```sql
CREATE TABLE IF NOT EXISTS `tripaiplanner.analytics.exchange_rates`
(
  currency_code STRING NOT NULL,           -- e.g., "INR", "MYR", "EUR"
  usd_rate FLOAT64 NOT NULL,              -- Rate to convert to USD (1 currency = X USD)
  effective_date DATE NOT NULL,            -- When this rate became effective
  source STRING,                           -- Source of rate (e.g., "manual", "api", "backend")
  last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
  
  -- Metadata
  is_active BOOLEAN DEFAULT TRUE,          -- Whether this rate is currently active
  notes STRING                             -- Optional notes about the rate
)
PARTITION BY effective_date
CLUSTER BY currency_code, effective_date;

-- Create unique index to prevent duplicate rates for same currency/date
CREATE UNIQUE INDEX idx_currency_date 
ON `tripaiplanner.analytics.exchange_rates` (currency_code, effective_date);
```

### 2. Initial Exchange Rate Data

**File:** `bigquery/data/initial_exchange_rates.sql`

```sql
-- Insert initial exchange rates from CurrencyConversionService.java
-- These rates are relative to INR, so we need to convert to USD base

-- Base conversion: 1 INR = 0.012 USD (from CurrencyConversionService)
INSERT INTO `tripaiplanner.analytics.exchange_rates` 
(currency_code, usd_rate, effective_date, source, is_active, notes)
VALUES
  -- Base currency
  ('USD', 1.0, CURRENT_DATE(), 'manual', TRUE, 'US Dollar - base currency'),
  
  -- Major currencies (converted from INR base to USD base)
  ('INR', 0.012, CURRENT_DATE(), 'backend', TRUE, 'Indian Rupee'),
  ('EUR', 0.92, CURRENT_DATE(), 'backend', TRUE, 'Euro (0.011 INR rate / 0.012 USD rate)'),
  ('GBP', 0.79, CURRENT_DATE(), 'backend', TRUE, 'British Pound'),
  ('JPY', 154.17, CURRENT_DATE(), 'backend', TRUE, 'Japanese Yen'),
  ('CNY', 7.17, CURRENT_DATE(), 'backend', TRUE, 'Chinese Yuan'),
  ('AUD', 1.50, CURRENT_DATE(), 'backend', TRUE, 'Australian Dollar'),
  ('CAD', 1.42, CURRENT_DATE(), 'backend', TRUE, 'Canadian Dollar'),
  
  -- Southeast Asian currencies
  ('SGD', 1.33, CURRENT_DATE(), 'backend', TRUE, 'Singapore Dollar'),
  ('MYR', 4.67, CURRENT_DATE(), 'backend', TRUE, 'Malaysian Ringgit'),
  ('THB', 35.0, CURRENT_DATE(), 'backend', TRUE, 'Thai Baht'),
  ('IDR', 16000.0, CURRENT_DATE(), 'backend', TRUE, 'Indonesian Rupiah'),
  ('PHP', 58.33, CURRENT_DATE(), 'backend', TRUE, 'Philippine Peso'),
  ('VND', 25417.0, CURRENT_DATE(), 'backend', TRUE, 'Vietnamese Dong'),
  ('KRW', 1375.0, CURRENT_DATE(), 'backend', TRUE, 'South Korean Won'),
  
  -- Middle East currencies
  ('AED', 3.67, CURRENT_DATE(), 'backend', TRUE, 'UAE Dirham'),
  ('SAR', 3.75, CURRENT_DATE(), 'backend', TRUE, 'Saudi Riyal'),
  
  -- European currencies
  ('CHF', 0.92, CURRENT_DATE(), 'backend', TRUE, 'Swiss Franc'),
  ('SEK', 10.83, CURRENT_DATE(), 'backend', TRUE, 'Swedish Krona'),
  ('NOK', 10.83, CURRENT_DATE(), 'backend', TRUE, 'Norwegian Krone'),
  ('DKK', 6.92, CURRENT_DATE(), 'backend', TRUE, 'Danish Krone'),
  ('PLN', 4.0, CURRENT_DATE(), 'backend', TRUE, 'Polish Zloty'),
  
  -- Other major currencies
  ('NZD', 1.67, CURRENT_DATE(), 'backend', TRUE, 'New Zealand Dollar'),
  ('ZAR', 18.33, CURRENT_DATE(), 'backend', TRUE, 'South African Rand'),
  ('BRL', 5.83, CURRENT_DATE(), 'backend', TRUE, 'Brazilian Real'),
  ('MXN', 20.0, CURRENT_DATE(), 'backend', TRUE, 'Mexican Peso'),
  ('RUB', 100.0, CURRENT_DATE(), 'backend', TRUE, 'Russian Ruble'),
  ('TRY', 34.17, CURRENT_DATE(), 'backend', TRUE, 'Turkish Lira'),
  ('HKD', 7.75, CURRENT_DATE(), 'backend', TRUE, 'Hong Kong Dollar'),
  ('TWD', 31.67, CURRENT_DATE(), 'backend', TRUE, 'Taiwan Dollar'),
  
  -- South Asian currencies
  ('EGP', 49.17, CURRENT_DATE(), 'backend', TRUE, 'Egyptian Pound'),
  ('PKR', 279.17, CURRENT_DATE(), 'backend', TRUE, 'Pakistani Rupee'),
  ('BDT', 110.0, CURRENT_DATE(), 'backend', TRUE, 'Bangladeshi Taka'),
  ('LKR', 291.67, CURRENT_DATE(), 'backend', TRUE, 'Sri Lankan Rupee'),
  ('NPR', 133.33, CURRENT_DATE(), 'backend', TRUE, 'Nepalese Rupee'),
  ('MVR', 15.0, CURRENT_DATE(), 'backend', TRUE, 'Maldivian Rufiyaa')
ON CONFLICT (currency_code, effective_date)
DO UPDATE SET
  usd_rate = EXCLUDED.usd_rate,
  last_updated = CURRENT_TIMESTAMP();
```

### 3. Currency Conversion Function

**File:** `bigquery/functions/convert_to_usd.sql`

```sql
-- Create a SQL function for currency conversion
CREATE OR REPLACE FUNCTION `tripaiplanner.analytics.convert_to_usd`(
  amount FLOAT64,
  currency_code STRING,
  event_date DATE
)
RETURNS FLOAT64
AS (
  -- Get the exchange rate for the given currency and date
  -- If no rate exists for that date, use the most recent rate before that date
  (
    SELECT amount / usd_rate
    FROM `tripaiplanner.analytics.exchange_rates`
    WHERE currency_code = currency_code
      AND effective_date <= event_date
      AND is_active = TRUE
    ORDER BY effective_date DESC
    LIMIT 1
  )
);

-- Usage example:
-- SELECT analytics.convert_to_usd(1000, 'INR', CURRENT_DATE()) as usd_amount
-- Returns: 12.0 (1000 INR * 0.012 = 12 USD)
```

### 4. Updated Cost Breakdown Table with USD

**File:** `bigquery/queries/cost_breakdown_daily_v2_create.sql`

```sql
-- Enhanced Cost Breakdown Daily Table with USD normalization
CREATE TABLE IF NOT EXISTS `tripaiplanner.analytics.cost_breakdown_daily_v2`
PARTITION BY date
CLUSTER BY date, node_type, currency
AS
SELECT
  DATE(TIMESTAMP_MILLIS(timestamp)) as date,
  JSON_EXTRACT_SCALAR(properties, '$.nodeType') as node_type,
  JSON_EXTRACT_SCALAR(properties, '$.currency') as currency,
  
  -- Volume
  COUNT(*) as cost_estimates_count,
  COUNT(DISTINCT JSON_EXTRACT_SCALAR(properties, '$.itineraryId')) as unique_itineraries,
  
  -- Cost Metrics (Original Currency)
  COALESCE(SUM(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.costPerPerson') AS FLOAT64)), 0) as total_cost,
  COALESCE(AVG(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.costPerPerson') AS FLOAT64)), 0) as avg_cost_per_activity,
  COALESCE(MIN(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.costPerPerson') AS FLOAT64)), 0) as min_cost,
  COALESCE(MAX(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.costPerPerson') AS FLOAT64)), 0) as max_cost,
  COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.costPerPerson') AS FLOAT64), 100)[OFFSET(50)], 0) as p50_cost,
  COALESCE(APPROX_QUANTILES(SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.costPerPerson') AS FLOAT64), 100)[OFFSET(95)], 0) as p95_cost,
  
  -- Cost Metrics (USD Normalized) - NEW
  COALESCE(
    SUM(
      SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.costPerPerson') AS FLOAT64) / 
      (SELECT usd_rate FROM `tripaiplanner.analytics.exchange_rates` er 
       WHERE er.currency_code = JSON_EXTRACT_SCALAR(properties, '$.currency')
         AND er.effective_date <= DATE(TIMESTAMP_MILLIS(timestamp))
         AND er.is_active = TRUE
       ORDER BY er.effective_date DESC LIMIT 1)
    ), 0
  ) as total_cost_usd,
  
  COALESCE(
    AVG(
      SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.costPerPerson') AS FLOAT64) / 
      (SELECT usd_rate FROM `tripaiplanner.analytics.exchange_rates` er 
       WHERE er.currency_code = JSON_EXTRACT_SCALAR(properties, '$.currency')
         AND er.effective_date <= DATE(TIMESTAMP_MILLIS(timestamp))
         AND er.is_active = TRUE
       ORDER BY er.effective_date DESC LIMIT 1)
    ), 0
  ) as avg_cost_per_activity_usd,
  
  COALESCE(
    MIN(
      SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.costPerPerson') AS FLOAT64) / 
      (SELECT usd_rate FROM `tripaiplanner.analytics.exchange_rates` er 
       WHERE er.currency_code = JSON_EXTRACT_SCALAR(properties, '$.currency')
         AND er.effective_date <= DATE(TIMESTAMP_MILLIS(timestamp))
         AND er.is_active = TRUE
       ORDER BY er.effective_date DESC LIMIT 1)
    ), 0
  ) as min_cost_usd,
  
  COALESCE(
    MAX(
      SAFE_CAST(JSON_EXTRACT_SCALAR(properties, '$.costPerPerson') AS FLOAT64) / 
      (SELECT usd_rate FROM `tripaiplanner.analytics.exchange_rates` er 
       WHERE er.currency_code = JSON_EXTRACT_SCALAR(properties, '$.currency')
         AND er.effective_date <= DATE(TIMESTAMP_MILLIS(timestamp))
         AND er.is_active = TRUE
       ORDER BY er.effective_date DESC LIMIT 1)
    ), 0
  ) as max_cost_usd,
  
  -- Top Activities by Cost
  APPROX_TOP_COUNT(JSON_EXTRACT_SCALAR(properties, '$.activityName'), 10) as top_expensive_activities,
  
  -- Metadata
  CURRENT_TIMESTAMP() as last_updated
  
FROM `tripaiplanner.analytics.raw_events`
WHERE eventName = 'cost_estimated'
  AND DATE(TIMESTAMP_MILLIS(timestamp)) >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
GROUP BY date, node_type, currency;
```

### 5. Simplified Query View for Dashboards

**File:** `bigquery/views/cost_breakdown_usd.sql`

```sql
-- Create a view that always shows USD-normalized costs
CREATE OR REPLACE VIEW `tripaiplanner.analytics.cost_breakdown_usd` AS
SELECT
  date,
  node_type,
  
  -- Aggregated across all currencies in USD
  SUM(total_cost_usd) as total_cost_usd,
  AVG(avg_cost_per_activity_usd) as avg_cost_per_activity_usd,
  SUM(cost_estimates_count) as total_activities,
  SUM(unique_itineraries) as total_itineraries,
  
  -- Currency breakdown (for reference)
  STRING_AGG(DISTINCT currency ORDER BY currency) as currencies_used,
  COUNT(DISTINCT currency) as currency_count,
  
  last_updated
FROM `tripaiplanner.analytics.cost_breakdown_daily_v2`
GROUP BY date, node_type, last_updated
ORDER BY date DESC, total_cost_usd DESC;

-- Usage in Looker Studio:
-- SELECT * FROM `tripaiplanner.analytics.cost_breakdown_usd`
-- WHERE date >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
```

---

## Handling New Currencies

### Automatic Detection and Alerting

**File:** `bigquery/queries/detect_missing_currencies.sql`

```sql
-- Query to detect currencies in raw_events that don't have exchange rates
SELECT DISTINCT
  JSON_EXTRACT_SCALAR(properties, '$.currency') as missing_currency,
  COUNT(*) as event_count,
  MIN(DATE(TIMESTAMP_MILLIS(timestamp))) as first_seen,
  MAX(DATE(TIMESTAMP_MILLIS(timestamp))) as last_seen
FROM `tripaiplanner.analytics.raw_events`
WHERE eventName = 'cost_estimated'
  AND JSON_EXTRACT_SCALAR(properties, '$.currency') NOT IN (
    SELECT currency_code 
    FROM `tripaiplanner.analytics.exchange_rates`
    WHERE is_active = TRUE
  )
GROUP BY missing_currency
ORDER BY event_count DESC;
```

### Manual Currency Addition Process

When a new currency is detected:

1. **Add to Backend** (`CurrencyConversionService.java`):
```java
EXCHANGE_RATES.put("XXX", 0.XXX);  // Add new currency
CURRENCY_SYMBOLS.put("XXX", "X");   // Add symbol
```

2. **Add to BigQuery** (`exchange_rates` table):
```sql
INSERT INTO `tripaiplanner.analytics.exchange_rates`
(currency_code, usd_rate, effective_date, source, is_active, notes)
VALUES ('XXX', 0.XXX, CURRENT_DATE(), 'manual', TRUE, 'New Currency Name');
```

3. **Backfill Historical Data** (if needed):
```sql
-- Update historical records with new exchange rate
UPDATE `tripaiplanner.analytics.cost_breakdown_daily_v2`
SET total_cost_usd = total_cost / 0.XXX,  -- New rate
    avg_cost_per_activity_usd = avg_cost_per_activity / 0.XXX
WHERE currency = 'XXX'
  AND total_cost_usd = 0;  -- Only update records without USD conversion
```

---

## Automated Exchange Rate Updates

### Option 1: Cloud Function (Recommended)

**File:** `cloud-functions/update-exchange-rates/index.js`

```javascript
const { BigQuery } = require('@google-cloud/bigquery');
const axios = require('axios');

// Use free API: https://exchangerate-api.com or https://api.exchangerate.host
const EXCHANGE_RATE_API = 'https://api.exchangerate.host/latest?base=USD';

exports.updateExchangeRates = async (req, res) => {
  const bigquery = new BigQuery();
  
  try {
    // Fetch latest rates
    const response = await axios.get(EXCHANGE_RATE_API);
    const rates = response.data.rates;
    
    // Prepare insert data
    const rows = Object.entries(rates).map(([currency, rate]) => ({
      currency_code: currency,
      usd_rate: rate,
      effective_date: new Date().toISOString().split('T')[0],
      source: 'api',
      is_active: true,
      last_updated: new Date().toISOString()
    }));
    
    // Insert into BigQuery
    await bigquery
      .dataset('analytics')
      .table('exchange_rates')
      .insert(rows);
    
    console.log(`Updated ${rows.length} exchange rates`);
    res.status(200).send({ success: true, count: rows.length });
    
  } catch (error) {
    console.error('Error updating exchange rates:', error);
    res.status(500).send({ error: error.message });
  }
};
```

**Deploy:**
```bash
gcloud functions deploy update-exchange-rates \
  --runtime nodejs18 \
  --trigger-http \
  --allow-unauthenticated \
  --region us-central1
```

**Schedule with Cloud Scheduler:**
```bash
gcloud scheduler jobs create http update-exchange-rates-daily \
  --schedule="0 0 * * *" \
  --uri="https://us-central1-tripaiplanner.cloudfunctions.net/update-exchange-rates" \
  --http-method=GET \
  --time-zone="UTC"
```

### Option 2: Scheduled Query (Simple)

**File:** `bigquery/queries/update_exchange_rates_scheduled.sql`

```sql
-- This would need to be combined with an external data source
-- For now, manually update rates monthly or use Cloud Function approach
```

---

## Dashboard Queries

### 1. Cost Breakdown by Category (USD)

```sql
SELECT
  node_type as category,
  SUM(total_cost_usd) as total_usd,
  AVG(avg_cost_per_activity_usd) as avg_per_activity_usd,
  SUM(cost_estimates_count) as activity_count
FROM `tripaiplanner.analytics.cost_breakdown_daily_v2`
WHERE date >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
GROUP BY node_type
ORDER BY total_usd DESC;
```

### 2. Cost Trends Over Time (USD)

```sql
SELECT
  date,
  SUM(total_cost_usd) as daily_total_usd,
  SUM(cost_estimates_count) as daily_activities
FROM `tripaiplanner.analytics.cost_breakdown_daily_v2`
WHERE date >= DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)
GROUP BY date
ORDER BY date;
```

### 3. Currency Distribution

```sql
SELECT
  currency,
  SUM(total_cost) as total_original,
  SUM(total_cost_usd) as total_usd,
  SUM(cost_estimates_count) as activities,
  COUNT(DISTINCT date) as days_active
FROM `tripaiplanner.analytics.cost_breakdown_daily_v2`
WHERE date >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
GROUP BY currency
ORDER BY total_usd DESC;
```

---

## Migration Plan

### Phase 1: Setup (Week 1)
1. ✅ Create `exchange_rates` table
2. ✅ Insert initial exchange rates from backend
3. ✅ Create conversion function
4. ✅ Test conversions with sample data

### Phase 2: Table Updates (Week 1-2)
1. ✅ Create `cost_breakdown_daily_v2` with USD fields
2. ✅ Backfill historical data
3. ✅ Create `cost_breakdown_usd` view
4. ✅ Validate data accuracy

### Phase 3: Automation (Week 2-3)
1. ✅ Deploy Cloud Function for rate updates
2. ✅ Set up Cloud Scheduler
3. ✅ Create monitoring alerts for missing currencies
4. ✅ Document manual currency addition process

### Phase 4: Dashboard Updates (Week 3-4)
1. ✅ Update Looker Studio dashboards to use USD fields
2. ✅ Add currency breakdown charts
3. ✅ Create cost comparison reports
4. ✅ Train team on new metrics

---

## Benefits

1. **Unified Reporting**: All costs in USD for easy comparison
2. **Flexible Analysis**: Keep original currency data for auditing
3. **Automatic Updates**: Exchange rates update daily
4. **New Currency Support**: Automatic detection and easy addition
5. **Historical Accuracy**: Date-based exchange rates for accurate historical analysis
6. **No Backend Changes**: All conversion happens in BigQuery

---

## Maintenance

### Weekly Tasks
- Review missing currency alerts
- Verify exchange rate updates

### Monthly Tasks
- Audit exchange rate accuracy
- Update rates manually if API fails
- Review currency usage patterns

### Quarterly Tasks
- Backfill historical data with updated rates (if needed)
- Review and optimize query performance
- Update documentation
