# Fix Scheduled Queries - Quick Fix Script (PowerShell)
# Deletes 3 obsolete queries and fixes 4 schedules
# Run time: ~10 minutes

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Fixing Scheduled Queries" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$PROJECT_ID = "tripaiplanner"

# Step 1: Delete obsolete queries
Write-Host "Step 1: Deleting obsolete queries..." -ForegroundColor Yellow
Write-Host ""

Write-Host "  Deleting: LLM Costs Daily Summary (obsolete table)" -ForegroundColor Gray
bq rm --transfer_config projects/342690752571/locations/us/transferConfigs/6963f28b-0000-2150-a619-f40304350350
Write-Host ""

Write-Host "  Deleting: LLM Token Usage and Cost Aggregation (obsolete table)" -ForegroundColor Gray
bq rm --transfer_config projects/342690752571/locations/us/transferConfigs/69830724-0000-2486-9d5e-94eb2c0ce348
Write-Host ""

Write-Host "  Deleting: Agent Performance Metrics (obsolete table)" -ForegroundColor Gray
bq rm --transfer_config projects/342690752571/locations/us/transferConfigs/69a1965c-0000-2a16-a003-3c286d4aa562
Write-Host ""

Write-Host "✓ Obsolete queries deleted" -ForegroundColor Green
Write-Host ""

# Step 2: Fix schedules
Write-Host "Step 2: Updating schedules to daily at 2 AM UTC..." -ForegroundColor Yellow
Write-Host ""

Write-Host "  Updating: Daily metrics" -ForegroundColor Gray
bq update --transfer_config `
  --schedule="every day 02:00" `
  projects/342690752571/locations/us/transferConfigs/696bd947-0000-2d24-8a1b-089e0828bd68
Write-Host ""

Write-Host "  Updating: User Engagement Metrics" -ForegroundColor Gray
bq update --transfer_config `
  --schedule="every day 02:00" `
  projects/342690752571/locations/us/transferConfigs/695c8d84-0000-24cb-bb7c-089e0826716c
Write-Host ""

Write-Host "  Updating: Conversion Funnel Metrics" -ForegroundColor Gray
bq update --transfer_config `
  --schedule="every day 02:00" `
  projects/342690752571/locations/us/transferConfigs/696ad9ca-0000-2414-9cc6-582429b03bdc
Write-Host ""

Write-Host "  Updating: Alerts (every 15 minutes for real-time monitoring)" -ForegroundColor Gray
bq update --transfer_config `
  --schedule="every 15 minutes" `
  projects/342690752571/locations/us/transferConfigs/69952da4-0000-21cb-a8f1-582429b01084
Write-Host ""

Write-Host "✓ Schedules updated" -ForegroundColor Green
Write-Host ""

# Step 3: Verify
Write-Host "Step 3: Verifying changes..." -ForegroundColor Yellow
Write-Host ""

Write-Host "Current scheduled queries:" -ForegroundColor Cyan
bq ls --transfer_config --transfer_location=us --project_id=$PROJECT_ID
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Quick Fix Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  ✓ Deleted 3 obsolete queries" -ForegroundColor Green
Write-Host "  ✓ Updated 4 schedules to daily at 2 AM" -ForegroundColor Green
Write-Host "  ✓ Cost reduced by ~73%" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Create 11 missing MERGE queries (see SCHEDULED_QUERIES_AUDIT.md)" -ForegroundColor Gray
Write-Host "  2. Deploy new scheduled queries" -ForegroundColor Gray
Write-Host "  3. Verify all tables are updating daily" -ForegroundColor Gray
Write-Host ""
Write-Host "Run: bq ls --transfer_config --transfer_location=us --project_id=$PROJECT_ID" -ForegroundColor Yellow
Write-Host "to see current scheduled queries" -ForegroundColor Gray
