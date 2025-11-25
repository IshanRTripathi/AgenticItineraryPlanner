/**
 * Cloud Function: Analytics Event Ingestion (BULLETPROOF VERSION)
 * 
 * Triggered by Pub/Sub messages from the analytics-events topic.
 * Inserts events into BigQuery with comprehensive error handling and logging.
 * 
 * Deploy with:
 * gcloud functions deploy analytics-ingestion \
 *   --gen2 \
 *   --runtime nodejs20 \
 *   --trigger-topic analytics-events \
 *   --entry-point ingestToBigQuery \
 *   --region us-central1 \
 *   --memory 256MB \
 *   --timeout 60s \
 *   --set-env-vars BIGQUERY_DATASET=analytics,BIGQUERY_TABLE=raw_events
 */

const { BigQuery } = require('@google-cloud/bigquery');
const bigquery = new BigQuery();

const DATASET_ID = process.env.BIGQUERY_DATASET || 'analytics';
const TABLE_ID = process.env.BIGQUERY_TABLE || 'raw_events';
const PROJECT_ID = process.env.GCP_PROJECT || process.env.GCLOUD_PROJECT;

/**
 * Main function triggered by Pub/Sub.
 */
exports.ingestToBigQuery = async (message, context) => {
  const startTime = Date.now();
  let eventName = 'unknown';

  try {
    // Handle both Gen 1 and Gen 2 Cloud Functions format
    const messageData = message.data || message.message?.data;
    const eventId = context?.eventId || message.id || 'unknown';
    const publishTime = context?.timestamp || message.message?.publishTime || new Date().toISOString();

    if (!messageData) {
      console.error('❌ No message data found');
      console.error('Message structure:', JSON.stringify(message, null, 2));
      return;
    }

    // Decode the Pub/Sub message
    const eventData = Buffer.from(messageData, 'base64').toString('utf-8');
    console.log('📨 Received message:', eventData);

    // Parse JSON
    let event;
    try {
      event = JSON.parse(eventData);
    } catch (parseError) {
      console.error('❌ JSON parse error:', parseError.message);
      console.error('Raw data:', eventData);
      return;
    }

    eventName = event.eventName || 'unknown';

    // Validate required fields
    if (!event.eventName || !event.timestamp) {
      console.error('❌ Missing required fields:', event);
      return;
    }

    // Validate timestamp is a number
    const timestamp = typeof event.timestamp === 'number'
      ? event.timestamp
      : parseInt(event.timestamp, 10);

    if (isNaN(timestamp)) {
      console.error('❌ Invalid timestamp:', event.timestamp);
      return;
    }

    // Prepare row for BigQuery
    // CRITICAL: BigQuery expects properties as a JSON string, not an object
    const row = {
      eventName: String(event.eventName),
      timestamp: timestamp,
      userId: event.userId ? String(event.userId) : null,
      sessionId: event.sessionId ? String(event.sessionId) : null,
      platform: event.platform ? String(event.platform) : null,
      properties: event.properties ? JSON.stringify(event.properties) : null,
      _ingested_at: new Date().toISOString(),
      _message_id: String(eventId),
      _publish_time: publishTime
    };

    console.log('📝 Prepared row:', JSON.stringify(row, null, 2));

    // Insert into BigQuery
    const table = bigquery.dataset(DATASET_ID).table(TABLE_ID);
    await table.insert([row], {
      skipInvalidRows: false,
      ignoreUnknownValues: false
    });

    const duration = Date.now() - startTime;
    console.log(`✅ Event ingested: ${eventName} (user: ${event.userId || 'anonymous'}, ${duration}ms)`);

  } catch (error) {
    const duration = Date.now() - startTime;
    console.error(`❌ Failed to ingest: ${eventName} (${duration}ms)`);
    console.error('Error:', error.message);

    if (error.name === 'PartialFailureError') {
      console.error('BigQuery errors:', JSON.stringify(error.errors, null, 2));
      return;
    }

    // Retry on transient errors
    if (error.message && (
      error.message.includes('quota') ||
      error.message.includes('UNAVAILABLE') ||
      error.message.includes('timeout')
    )) {
      throw error;
    }
  }
};

/**
 * HTTP endpoint for testing.
 */
exports.testIngest = async (req, res) => {
  res.set('Access-Control-Allow-Origin', '*');

  if (req.method === 'OPTIONS') {
    res.status(204).send('');
    return;
  }

  try {
    const event = req.body;

    if (!event.eventName || !event.timestamp) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields: eventName, timestamp'
      });
    }

    const timestamp = typeof event.timestamp === 'number'
      ? event.timestamp
      : parseInt(event.timestamp, 10);

    if (isNaN(timestamp)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid timestamp'
      });
    }

    const row = {
      eventName: String(event.eventName),
      timestamp: timestamp,
      userId: event.userId ? String(event.userId) : null,
      sessionId: event.sessionId ? String(event.sessionId) : null,
      platform: event.platform ? String(event.platform) : null,
      properties: event.properties ? JSON.stringify(event.properties) : null,
      _ingested_at: new Date().toISOString(),
      _message_id: 'test-' + Date.now(),
      _publish_time: new Date().toISOString()
    };

    const table = bigquery.dataset(DATASET_ID).table(TABLE_ID);
    await table.insert([row]);

    res.json({
      success: true,
      message: 'Event ingested successfully',
      eventName: event.eventName,
      timestamp: timestamp,
      row: row
    });

  } catch (error) {
    console.error('Test failed:', error);
    res.status(500).json({
      success: false,
      message: error.message,
      details: error.errors || null
    });
  }
};
