# Kafka Integration Guide

This project includes Kafka integration for event streaming and aggregation. Events from the SSE endpoint are sent to Kafka, aggregated, and summarized back to SSE clients.

## Architecture

```
SSE Events → Kafka Producer → events-topic
                                    ↓
                              Kafka Consumer
                                    ↓
                            Aggregation Logic
                                    ↓
                    Kafka Producer → events-summary-table
                                    ↓
                              SSE Broadcast
```

## Setup

### 1. Install Dependencies

Make sure KafkaJS is installed:

```bash
npm install kafkajs
# or
bun add kafkajs
```

### 2. Start Kafka with Docker

```bash
docker-compose up -d
```

This starts:
- **Zookeeper** on port 2181
- **Kafka** on port 9092
- **Kafka UI** on port 8080 (http://localhost:8080)

### 3. Run the Application

```bash
bun run app.js
# or
node app.js
```

## Kafka Topics

### events-topic
Main event stream where all SSE events are published.

### events-summary-table
Summary/aggregation topic that contains:
- Total event count
- Sum of values
- Average, min, max values
- Sample of recent events
- Aggregation period metadata

## Configuration

Edit `kafka/config.js` to customize:

```javascript
export const aggregationConfig = {
  // Trigger summary after N events
  batchSize: 10,
  // Or trigger summary every N milliseconds (30 seconds)
  intervalMs: 30000
};
```

### Aggregation Triggers

Summaries are published when **either** condition is met:
1. **Batch trigger**: After receiving N events (default: 10)
2. **Time trigger**: Every N milliseconds (default: 30 seconds)

## Endpoints

### GET /events
SSE endpoint that streams:
- Regular events (every 2 seconds)
- Aggregated summaries (based on triggers)

```bash
curl -N http://localhost:3000/events
```

### GET /kafka/status
Check Kafka connection and current aggregation state:

```bash
curl http://localhost:3000/kafka/status
```

Response:
```json
{
  "producer": {
    "connected": true
  },
  "consumer": {
    "connected": true,
    "currentAggregation": {
      "currentBuffer": 5,
      "stats": {
        "totalEvents": 5,
        "sumOfValues": 150,
        "minValue": 10,
        "maxValue": 50
      }
    }
  }
}
```

## Event Flow Example

### 1. Regular Event
```json
{
  "payload": {
    "date": 1701523456789,
    "times": 42
  }
}
```

### 2. Aggregated Summary (sent via SSE)
```json
{
  "type": "summary",
  "data": {
    "id": "summary-1701523456789",
    "timestamp": 1701523456789,
    "aggregationPeriod": {
      "start": 1701523426789,
      "end": 1701523456789,
      "durationMs": 30000
    },
    "statistics": {
      "totalEvents": 10,
      "sumOfValues": 450,
      "averageValue": 45,
      "minValue": 40,
      "maxValue": 50
    },
    "eventSample": [
      // Last 5 events
    ]
  }
}
```

## Monitoring

### Kafka UI
Visit http://localhost:8080 to:
- View topics
- Browse messages
- Monitor consumer groups
- Check topic configurations

### Application Logs
The application logs show:
- 📨 Received events
- 📦 Batch triggers
- ⏰ Cron triggers
- 📊 Published summaries

## Testing

### Send Test Events

The application automatically generates events every 2 seconds. You can also modify the broadcast function to send custom events:

```javascript
broadcast({ 
  payload: { 
    date: Date.now(), 
    times: Math.floor(Math.random() * 100) 
  } 
});
```

### View Summaries

Connect to the SSE endpoint to see both regular events and summaries:

```bash
curl -N http://localhost:3000/events
```

Look for events with `"type": "summary"` to see aggregations.

## Cleanup

Stop Kafka services:

```bash
docker-compose down
```

Remove volumes (deletes all Kafka data):

```bash
docker-compose down -v
```

## Environment Variables

You can customize the Kafka broker address:

```bash
export KAFKA_BROKER=localhost:9092
node app.js
```

Default is `localhost:9092` if not specified.
