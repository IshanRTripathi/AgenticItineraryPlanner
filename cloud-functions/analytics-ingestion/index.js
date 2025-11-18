/**
 * Cloud Function: Analytics Event Ingestion
 * 
 * Triggered by Pub/Sub messages from the analytics-events topic.
 * Inserts events into BigQuery for analysis.
 * 
 * Deploy with:
 * gcloud functions deploy analytics-ingestion \
 *   --runtime nodejs20 \
 *   --trigger-topic analytics-events \
 *   --entry-point ingestToBigQuery \
 *   --region us-central1 \
 *   --memory 256MB \
 *   --timeout 60s
 */

const {BigQuery} = require('@google-cloud/bigquery');
const bigquery = new BigQuery();

const DATASET_ID = process.env.BIGQUERY_DATASET || 'analytics';
const TABLE_ID = process.env.BIGQUERY_TABLE || 'raw_events';

/**
 * Main function triggered by Pub/Sub.
 * 
 * @param {object} message - Pub/Sub message
 * @param {object} context - Event context
 */
exports.ingestToBigQuery = async (message, context) => {
  try {
    // Decode the Pub/Sub message
    const eventData = message.data
      ? Buffer.from(message.data, 'base64').toString()
      : '{}';
    
    const event = JSON.parse(eventData);
    
    // Validate event has required fields
    if (!event.eventName || !event.timestamp) {
      console.error('Invalid event - missing required fields:', event);
      return; // Don't retry invalid events
    }
    
    // Add ingestion metadata
    const enrichedEvent = {
      ...event,
      _ingested_at: new Date().toISOString(),
      _message_id: context.eventId,
      _publish_time: context.timestamp
    };
    
    // Insert into BigQuery
    await bigquery
      .dataset(DATASET_ID)
      .table(TABLE_ID)
      .insert([enrichedEvent]);
    
    console.log(`✅ Event ingested: ${event.eventName} (user: ${event.userId || 'anonymous'})`);
    
  } catch (error) {
    // Log error details
    console.error('❌ Failed to ingest event:', error);
    console.error('Message data:', message.data);
    console.error('Context:', context);
    
    // Check if it's a BigQuery schema error
    if (error.name === 'PartialFailureError') {
      console.error('BigQuery insertion errors:', JSON.stringify(error.errors, null, 2));
    }
    
    // Throw error to trigger Pub/Sub retry
    throw error;
  }
};

/**
 * HTTP endpoint for testing (optional).
 * Can be used to test the function without Pub/Sub.
 */
exports.testIngest = async (req, res) => {
  try {
    const event = req.body;
    
    if (!event.eventName || !event.timestamp) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields: eventName, timestamp'
      });
    }
    
    const enrichedEvent = {
      ...event,
      _ingested_at: new Date().toISOString(),
      _message_id: 'test-' + Date.now(),
      _publish_time: new Date().toISOString()
    };
    
    await bigquery
      .dataset(DATASET_ID)
      .table(TABLE_ID)
      .insert([enrichedEvent]);
    
    res.json({
      success: true,
      message: 'Event ingested successfully',
      eventName: event.eventName
    });
    
  } catch (error) {
    console.error('Test ingestion failed:', error);
    res.status(500).json({
      success: false,
      message: error.message
    });
  }
};
