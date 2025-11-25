# Deploy USD Normalization
$PROJECT_ID = "tripaiplanner"
$DATASET = "analytics"

Write-Host "USD Normalization Deployment" -ForegroundColor Cyan
Write-Host ""

# Step 1: Create exchange_rates table
Write-Host "[1/5] Creating exchange_rates table..." -ForegroundColor Yellow
$sql1 = "CREATE TABLE IF NOT EXISTS ``$PROJECT_ID.$DATASET.exchange_rates`` (currency_code STRING NOT NULL, usd_rate FLOAT64 NOT NULL, effective_date DATE NOT NULL, source STRING, last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP(), is_active BOOLEAN DEFAULT TRUE, notes STRING) PARTITION BY effective_date CLUSTER BY currency_code, effective_date;"
bq query --project_id=$PROJECT_ID --use_legacy_sql=false --format=none $sql1
Write-Host "✓ Table created" -ForegroundColor Green

# Step 2: Load exchange rates
Write-Host "[2/5] Loading exchange rates..." -ForegroundColor Yellow
$content = Get-Content "data/initial_exchange_rates.sql" -Raw
bq query --project_id=$PROJECT_ID --use_legacy_sql=false --format=none $content
Write-Host "✓ Rates loaded" -ForegroundColor Green

# Step 3: Verify
Write-Host "[3/5] Verifying..." -ForegroundColor Yellow
$count = bq query --project_id=$PROJECT_ID --use_legacy_sql=false --format=csv "SELECT COUNT(*) FROM ``$PROJECT_ID.$DATASET.exchange_rates``" | Select-Object -Skip 1
Write-Host "✓ Loaded $count currencies" -ForegroundColor Green

# Step 4: Create cost table
Write-Host "[4/5] Creating cost_breakdown_daily_usd table..." -ForegroundColor Yellow
$sql2 = Get-Content "queries/cost_breakdown_daily_usd_create.sql" -Raw
bq query --project_id=$PROJECT_ID --use_legacy_sql=false --format=none $sql2
Write-Host "✓ Table created" -ForegroundColor Green

# Step 5: Create view
Write-Host "[5/5] Creating view..." -ForegroundColor Yellow
$sql3 = Get-Content "views/cost_breakdown_usd_view.sql" -Raw
bq query --project_id=$PROJECT_ID --use_legacy_sql=false --format=none $sql3
Write-Host "✓ View created" -ForegroundColor Green

Write-Host ""
Write-Host "Deployment Complete!" -ForegroundColor Green
Write-Host "Tables created successfully" -ForegroundColor White
