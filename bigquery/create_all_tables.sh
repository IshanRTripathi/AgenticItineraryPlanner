#!/bin/bash

# Create All BigQuery Analytics Tables
# This script creates all 7 aggregated tables with historical data (last 90 days)

set -e

echo "🚀 Creating BigQuery Analytics Tables..."
echo ""

cd queries

# Table 1: Daily Metrics
echo "📊 Creating daily_metrics table..."
bq query --use_legacy_sql=false < daily_metrics_create.sql
echo "✅ daily_metrics created"
echo ""

# Table 2: LLM Costs (Detailed)
echo "💰 Creating llm_costs_daily table..."
bq query --use_legacy_sql=false < llm_costs_daily_create.sql
echo "✅ llm_costs_daily created"
echo ""

# Table 3: LLM Costs (Summary)
echo "💵 Creating llm_costs_daily_summary table..."
bq query --use_legacy_sql=false < llm_costs_daily_summary_create.sql
echo "✅ llm_costs_daily_summary created"
echo ""

# Table 4: Monthly Projection
echo "📈 Creating llm_costs_monthly_projection table..."
bq query --use_legacy_sql=false < llm_costs_monthly_projection.sql
echo "✅ llm_costs_monthly_projection created"
echo ""

# Table 5: Funnel Metrics
echo "🎯 Creating funnel_metrics table..."
bq query --use_legacy_sql=false < funnel_metrics_create.sql
echo "✅ funnel_metrics created"
echo ""

# Table 6: User Engagement
echo "👥 Creating user_engagement_daily table..."
bq query --use_legacy_sql=false < user_engagement_daily_create.sql
echo "✅ user_engagement_daily created"
echo ""

# Table 7: Agent Performance
echo "🤖 Creating agent_performance_daily table..."
bq query --use_legacy_sql=false < agent_performance_create.sql
echo "✅ agent_performance_daily created"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ All tables created successfully!"
echo ""
echo "📊 Verify tables:"
echo "   bq ls tripaiplanner:analytics"
echo ""
echo "📈 Next steps:"
echo "   1. Check data: bq query --use_legacy_sql=false 'SELECT * FROM \`tripaiplanner.analytics.daily_metrics\` LIMIT 5'"
echo "   2. Schedule daily updates (see EXECUTION_GUIDE.md)"
echo "   3. Create Looker Studio dashboard (see LOOKER_STUDIO_GUIDE.md)"
echo ""
echo "🎉 Ready for Looker Studio!"
