# Deploy New Analytics Tables - PowerShell Script
# Creates 5 new tables and fixes the monthly projection query
# Run this after the core tables are already deployed

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Deploying New Analytics Tables" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$PROJECT_ID = "tripaiplanner"
$DATASET = "analytics"

# Check if bq command exists
if (-not (Get-Command bq -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: 'bq' command not found. Please install Google Cloud SDK." -ForegroundColor Red
    exit 1
}

# Array of new tables to create
$tables = @(
    @{
        Name = "validation_metrics_daily"
        File = "queries/validation_metrics_daily_create.sql"
        Description = "Validation quality metrics"
    },
    @{
        Name = "destination_popularity_daily"
        File = "queries/destination_popularity_daily_create.sql"
        Description = "Destination popularity analysis"
    },
    @{
        Name = "interest_analysis_daily"
        File = "queries/interest_analysis_daily_create.sql"
        Description = "User interest tracking"
    },
    @{
        Name = "budget_accuracy_analysis"
        File = "queries/budget_accuracy_analysis_create.sql"
        Description = "Budget vs actual cost analysis"
    },
    @{
        Name = "agent_error_patterns"
        File = "queries/agent_error_patterns_create.sql"
        Description = "Error pattern identification"
    }
)

$successCount = 0
$failCount = 0

foreach ($table in $tables) {
    Write-Host "Creating table: $($table.Name)" -ForegroundColor Yellow
    Write-Host "  Description: $($table.Description)" -ForegroundColor Gray
    
    try {
        $result = bq query --use_legacy_sql=false --project_id=$PROJECT_ID (Get-Content $table.File -Raw) 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  ✓ SUCCESS" -ForegroundColor Green
            $successCount++
        } else {
            Write-Host "  ✗ FAILED: $result" -ForegroundColor Red
            $failCount++
        }
    } catch {
        Write-Host "  ✗ FAILED: $_" -ForegroundColor Red
        $failCount++
    }
    
    Write-Host ""
}

# Fix the monthly projection query
Write-Host "Updating LLM costs monthly projection..." -ForegroundColor Yellow
try {
    $result = bq query --use_legacy_sql=false --project_id=$PROJECT_ID (Get-Content "queries/llm_costs_monthly_projection.sql" -Raw) 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✓ SUCCESS" -ForegroundColor Green
        $successCount++
    } else {
        Write-Host "  ✗ FAILED: $result" -ForegroundColor Red
        $failCount++
    }
} catch {
    Write-Host "  ✗ FAILED: $_" -ForegroundColor Red
    $failCount++
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Deployment Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Success: $successCount" -ForegroundColor Green
Write-Host "  Failed:  $failCount" -ForegroundColor $(if ($failCount -gt 0) { "Red" } else { "Green" })
Write-Host ""

if ($failCount -eq 0) {
    Write-Host "✓ All tables deployed successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "  1. Verify tables exist: bq ls $PROJECT_ID`:$DATASET" -ForegroundColor Gray
    Write-Host "  2. Check data: bq query --use_legacy_sql=false 'SELECT * FROM ``$PROJECT_ID.$DATASET.validation_metrics_daily`` LIMIT 5'" -ForegroundColor Gray
    Write-Host "  3. Set up scheduled queries (optional)" -ForegroundColor Gray
} else {
    Write-Host "⚠ Some tables failed to deploy. Check errors above." -ForegroundColor Yellow
    exit 1
}
