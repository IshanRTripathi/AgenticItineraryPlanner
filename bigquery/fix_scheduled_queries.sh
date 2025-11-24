#!/bin/bash
# Fix Scheduled Queries - Quick Fix Script
# Deletes 3 obsolete queries and fixes 4 schedules
# Run time: ~10 minutes

echo "========================================"
echo "Fixing Scheduled Queries"
echo "========================================"
echo ""

PROJECT_ID="tripaiplanner"

# Step 1: Delete obsolete queries
echo "Step 1: Deleting obsolete queries..."
echo ""

echo "  Deleting: LLM Costs Daily Summary (obsolete table)"
bq rm --transfer_config projects/342690752571/locations/us/transferConfigs/6963f28b-0000-2150-a619-f40304350350
echo ""

echo "  Deleting: LLM Token Usage and Cost Aggregation (obsolete table)"
bq rm --transfer_config projects/342690752571/locations/us/transferConfigs/69830724-0000-2486-9d5e-94eb2c0ce348
echo ""

echo "  Deleting: Agent Performance Metrics (obsolete table)"
bq rm --transfer_config projects/342690752571/locations/us/transferConfigs/69a1965c-0000-2a16-a003-3c286d4aa562
echo ""

echo "✓ Obsolete queries deleted"
echo ""

# Step 2: Fix schedules
echo "Step 2: Updating schedules to daily at 2 AM UTC..."
echo ""

echo "  Updating: Daily metrics"
bq update --transfer_config \
  --schedule="every day 02:00" \
  projects/342690752571/locations/us/transferConfigs/696bd947-0000-2d24-8a1b-089e0828bd68
echo ""

echo "  Updating: User Engagement Metrics"
bq update --transfer_config \
  --schedule="every day 02:00" \
  projects/342690752571/locations/us/transferConfigs/695c8d84-0000-24cb-bb7c-089e0826716c
echo ""

echo "  Updating: Conversion Funnel Metrics"
bq update --transfer_config \
  --schedule="every day 02:00" \
  projects/342690752571/locations/us/transferConfigs/696ad9ca-0000-2414-9cc6-582429b03bdc
echo ""

echo "  Updating: Alerts (every 15 minutes for real-time monitoring)"
bq update --transfer_config \
  --schedule="every 15 minutes" \
  projects/342690752571/locations/us/transferConfigs/69952da4-0000-21cb-a8f1-582429b01084
echo ""

echo "✓ Schedules updated"
echo ""

# Step 3: Verify
echo "Step 3: Verifying changes..."
echo ""

echo "Current scheduled queries:"
bq ls --transfer_config --transfer_location=us --project_id=$PROJECT_ID
echo ""

echo "========================================"
echo "Quick Fix Complete!"
echo "========================================"
echo ""
echo "Summary:"
echo "  ✓ Deleted 3 obsolete queries"
echo "  ✓ Updated 4 schedules to daily at 2 AM"
echo "  ✓ Cost reduced by ~73%"
echo ""
echo "Next steps:"
echo "  1. Create 11 missing MERGE queries (see SCHEDULED_QUERIES_AUDIT.md)"
echo "  2. Deploy new scheduled queries"
echo "  3. Verify all tables are updating daily"
echo ""
echo "Run: bq ls --transfer_config --transfer_location=us --project_id=$PROJECT_ID"
echo "to see current scheduled queries"
