# Create All BigQuery Analytics Tables (PowerShell)
# This script creates all 7 aggregated tables with historical data (last 90 days)

$ErrorActionPreference = "Stop"

Write-Host "🚀 Creating BigQuery Analytics Tables..." -ForegroundColor Cyan
Write-Host ""

Set-Location queries

# Table 1: Daily Metrics
Write-Host "📊 Creating daily_metrics table..." -ForegroundColor Yellow
Get-Content daily_metrics_create.sql | bq query --use_legacy_sql=false
Write-Host "✅ daily_metrics created" -ForegroundColor Green
Write-Host ""

# Table 2: LLM Costs (Detailed)
Write-Host "💰 Creating llm_costs_daily table..." -ForegroundColor Yellow
Get-Content llm_costs_daily_create.sql | bq query --use_legacy_sql=false
Write-Host "✅ llm_costs_daily created" -ForegroundColor Green
Write-Host ""

# Table 3: LLM Costs (Summary)
Write-Host "💵 Creating llm_costs_daily_summary table..." -ForegroundColor Yellow
Get-Content llm_costs_daily_summary_create.sql | bq query --use_legacy_sql=false
Write-Host "✅ llm_costs_daily_summary created" -ForegroundColor Green
Write-Host ""

# Table 4: Monthly Projection
Write-Host "📈 Creating llm_costs_monthly_projection table..." -ForegroundColor Yellow
Get-Content llm_costs_monthly_projection.sql | bq query --use_legacy_sql=false
Write-Host "✅ llm_costs_monthly_projection created" -ForegroundColor Green
Write-Host ""

# Table 5: Funnel Metrics
Write-Host "🎯 Creating funnel_metrics table..." -ForegroundColor Yellow
Get-Content funnel_metrics_create.sql | bq query --use_legacy_sql=false
Write-Host "✅ funnel_metrics created" -ForegroundColor Green
Write-Host ""

# Table 6: User Engagement
Write-Host "👥 Creating user_engagement_daily table..." -ForegroundColor Yellow
Get-Content user_engagement_daily_create.sql | bq query --use_legacy_sql=false
Write-Host "✅ user_engagement_daily created" -ForegroundColor Green
Write-Host ""

# Table 7: Agent Performance
Write-Host "🤖 Creating agent_performance_daily table..." -ForegroundColor Yellow
Get-Content agent_performance_create.sql | bq query --use_legacy_sql=false
Write-Host "✅ agent_performance_daily created" -ForegroundColor Green
Write-Host ""

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "✅ All tables created successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "📊 Verify tables:" -ForegroundColor Cyan
Write-Host "   bq ls tripaiplanner:analytics"
Write-Host ""
Write-Host "📈 Next steps:" -ForegroundColor Cyan
Write-Host "   1. Check data: bq query --use_legacy_sql=false 'SELECT * FROM ``tripaiplanner.analytics.daily_metrics`` LIMIT 5'"
Write-Host "   2. Schedule daily updates (see EXECUTION_GUIDE.md)"
Write-Host "   3. Create Looker Studio dashboard (see LOOKER_STUDIO_GUIDE.md)"
Write-Host ""
Write-Host "🎉 Ready for Looker Studio!" -ForegroundColor Green
