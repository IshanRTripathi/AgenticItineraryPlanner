#!/bin/bash
# Deploy New Analytics Tables - Bash Script
# Creates 5 new tables and fixes the monthly projection query
# Run this after the core tables are already deployed

echo "========================================"
echo "Deploying New Analytics Tables"
echo "========================================"
echo ""

PROJECT_ID="tripaiplanner"
DATASET="analytics"

# Check if bq command exists
if ! command -v bq &> /dev/null; then
    echo "ERROR: 'bq' command not found. Please install Google Cloud SDK."
    exit 1
fi

# Array of new tables to create
declare -a tables=(
    "validation_metrics_daily:queries/validation_metrics_daily_create.sql:Validation quality metrics"
    "destination_popularity_daily:queries/destination_popularity_daily_create.sql:Destination popularity analysis"
    "interest_analysis_daily:queries/interest_analysis_daily_create.sql:User interest tracking"
    "budget_accuracy_analysis:queries/budget_accuracy_analysis_create.sql:Budget vs actual cost analysis"
    "agent_error_patterns:queries/agent_error_patterns_create.sql:Error pattern identification"
)

success_count=0
fail_count=0

for table_info in "${tables[@]}"; do
    IFS=':' read -r table_name file_path description <<< "$table_info"
    
    echo "Creating table: $table_name"
    echo "  Description: $description"
    
    if bq query --use_legacy_sql=false --project_id=$PROJECT_ID < "$file_path" 2>&1; then
        echo "  ✓ SUCCESS"
        ((success_count++))
    else
        echo "  ✗ FAILED"
        ((fail_count++))
    fi
    
    echo ""
done

# Fix the monthly projection query
echo "Updating LLM costs monthly projection..."
if bq query --use_legacy_sql=false --project_id=$PROJECT_ID < "queries/llm_costs_monthly_projection.sql" 2>&1; then
    echo "  ✓ SUCCESS"
    ((success_count++))
else
    echo "  ✗ FAILED"
    ((fail_count++))
fi

echo ""
echo "========================================"
echo "Deployment Summary"
echo "========================================"
echo "  Success: $success_count"
echo "  Failed:  $fail_count"
echo ""

if [ $fail_count -eq 0 ]; then
    echo "✓ All tables deployed successfully!"
    echo ""
    echo "Next steps:"
    echo "  1. Verify tables exist: bq ls $PROJECT_ID:$DATASET"
    echo "  2. Check data: bq query --use_legacy_sql=false 'SELECT * FROM \`$PROJECT_ID.$DATASET.validation_metrics_daily\` LIMIT 5'"
    echo "  3. Set up scheduled queries (optional)"
else
    echo "⚠ Some tables failed to deploy. Check errors above."
    exit 1
fi
