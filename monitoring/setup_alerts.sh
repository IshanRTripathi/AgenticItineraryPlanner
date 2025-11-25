#!/bin/bash

# Setup Cloud Monitoring Alerts
# Creates alert policies for analytics monitoring

set -e

PROJECT_ID=${GCP_PROJECT_ID:-"tripaiplanner"}

echo "🔔 Setting up Cloud Monitoring Alerts..."
echo "   Project: $PROJECT_ID"
echo ""

# Create notification channel (email)
echo "📧 Creating notification channel..."
NOTIFICATION_CHANNEL=$(gcloud alpha monitoring channels create \
  --display-name="Analytics Alerts" \
  --type=email \
  --channel-labels=email_address=alerts@tripaiplanner.com \
  --project=$PROJECT_ID \
  --format="value(name)")

echo "   Created: $NOTIFICATION_CHANNEL"
echo ""

# Alert 1: Daily LLM Cost > $20
echo "💰 Creating Daily LLM Cost Warning alert..."
gcloud alpha monitoring policies create \
  --notification-channels=$NOTIFICATION_CHANNEL \
  --display-name="Daily LLM Cost Warning" \
  --condition-display-name="Daily cost > \$20" \
  --condition-threshold-value=20 \
  --condition-threshold-duration=300s \
  --project=$PROJECT_ID

# Alert 2: Monthly Projection > $300
echo "📊 Creating Monthly Cost Projection alert..."
gcloud alpha monitoring policies create \
  --notification-channels=$NOTIFICATION_CHANNEL \
  --display-name="Monthly LLM Cost Projection Warning" \
  --condition-display-name="Projected monthly cost > \$300" \
  --condition-threshold-value=300 \
  --condition-threshold-duration=3600s \
  --project=$PROJECT_ID

# Alert 3: Agent Failure Rate > 5%
echo "🤖 Creating Agent Failure Rate alert..."
gcloud alpha monitoring policies create \
  --notification-channels=$NOTIFICATION_CHANNEL \
  --display-name="Agent Failure Rate High" \
  --condition-display-name="Agent failure rate > 5%" \
  --condition-threshold-value=0.05 \
  --condition-threshold-duration=300s \
  --project=$PROJECT_ID

# Alert 4: Pub/Sub Backlog > 5 minutes
echo "📨 Creating Pub/Sub Backlog alert..."
gcloud alpha monitoring policies create \
  --notification-channels=$NOTIFICATION_CHANNEL \
  --display-name="Analytics Pub/Sub Backlog" \
  --condition-display-name="Message age > 5 minutes" \
  --condition-threshold-value=300 \
  --condition-threshold-duration=300s \
  --project=$PROJECT_ID

echo ""
echo "✅ All alerts created successfully!"
echo ""
echo "To view alerts:"
echo "  gcloud alpha monitoring policies list --project=$PROJECT_ID"
echo ""
echo "To test alerts:"
echo "  # Generate high-cost LLM requests"
echo "  # Check Cloud Console > Monitoring > Alerting"
