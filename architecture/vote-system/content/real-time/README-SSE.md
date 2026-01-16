# Server-Sent Events (SSE) Implementation

This document explains the SSE-based real-time communication solution in this project, why SSE was chosen over WebSockets and other asynchronous communication methods, and how it's implemented.

## What is SSE?

Server-Sent Events (SSE) is a server push technology enabling a server to send automatic updates to a client via HTTP. Unlike WebSockets, SSE is a **unidirectional** communication channel from server to client, making it ideal for scenarios where the server needs to push updates but doesn't need to receive messages back from the client.

## Why SSE Over WebSockets?

### Advantages of SSE

1. **Simpler Protocol**: SSE uses standard HTTP, making it easier to implement and debug
2. **Automatic Reconnection**: Built-in automatic reconnection with configurable retry intervals
3. **Event IDs**: Native support for event IDs enables clients to resume from the last received event after reconnection
4. **Better Firewall/Proxy Compatibility**: Works over standard HTTP, avoiding issues with proxy servers that block WebSocket upgrades
5. **Lower Overhead**: No need for the WebSocket handshake protocol
6. **Resource Efficiency**: For unidirectional communication, SSE uses fewer resources than maintaining bidirectional WebSocket connections
7. **Browser Native Support**: Built-in `EventSource` API in all modern browsers

### When to Use SSE vs WebSockets

**Use SSE when:**
- You only need server-to-client communication
- Broadcasting updates to many clients (notifications, live feeds, dashboards)
- Real-time monitoring and metrics
- Event streams and activity feeds
- Stock tickers, sports scores, news updates

**Use WebSockets when:**
- You need bidirectional communication
- Building chat applications
- Real-time collaborative editing
- Gaming applications
- Interactive tools requiring client-to-server messages

## Implementation Architecture

### SSE Server Implementation

The SSE endpoint is implemented in [app.js](app.js) using the `princejs` framework:

```javascript
// Event emitter for broadcasting to all connected clients
const sseEvents = new EventEmitter();
sseEvents.setMaxListeners(250000);

// Broadcast function to send data to all connected clients
export const broadcast = async (data) => {
  const message = `id: ${randomUUID()}\ndata: ${JSON.stringify(data)}\n\n`;
  sseEvents.emit("sse", message);
  await kafkaProducer.sendEvent(data);
};

// SSE endpoint handler
app.get("/events", (req) => {
  return app
    .response()
    .status(200)
    .header("Content-Type", "text/event-stream;charset=utf-8")
    .header("Cache-Control", "no-cache, no-transform")
    .header("Connection", "keep-alive")
    .header("X-Accel-Buffering", "no")
    .stream((push, close) => {
      const handler = (message) => {
        push(message);
      };

      sseEvents.on("sse", handler);

      // Clean up on connection close
      req.signal?.addEventListener("abort", () => {
        sseEvents.off("sse", handler);
        close();
      });
    });
});
```

### Key Implementation Details

1. **Event Emitter Pattern**: Uses Node.js `EventEmitter` to manage broadcasts to all connected clients
2. **High Concurrency Support**: Configured to support up to 250,000 simultaneous connections
3. **Proper SSE Message Format**: Each message includes a unique ID and JSON data in SSE format
4. **Essential HTTP Headers**:
   - `Content-Type: text/event-stream` - Identifies the response as SSE
   - `Cache-Control: no-cache` - Prevents caching
   - `Connection: keep-alive` - Maintains the connection
   - `X-Accel-Buffering: no` - Disables nginx buffering (important for production)
5. **Clean Resource Management**: Automatically removes event listeners when clients disconnect
6. **Kafka Integration**: Events are also published to Kafka for persistence and further processing

### SSE Message Format

SSE messages follow a specific format:

```
id: <unique-id>
data: <json-payload>

```

Example:
```
id: 550e8400-e29b-41d4-a716-446655440000
data: {"payload":{"date":1704672000000,"times":42}}

```

Each message ends with two newlines (`\n\n`) to signal the end of an event.

## Client-Side Usage

### JavaScript Browser Client

```javascript
const eventSource = new EventSource('http://localhost:3000/events');

eventSource.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('Received:', data);
};

eventSource.onerror = (error) => {
  console.error('SSE error:', error);
};

// Close connection when done
eventSource.close();
```

### Curl Command

```bash
curl -N -H "Accept: text/event-stream" http://localhost:3000/events
```

The `-N` flag disables buffering, allowing you to see events as they arrive.

## Load Testing

The solution includes comprehensive load testing using k6 with the SSE extension. See [README-LOAD-TEST.md](README-LOAD-TEST.md) for details on running load tests.

### Load Test Capabilities

The [sse-load-test.js](sse-load-test.js) script can:
- Simulate up to 50,000 concurrent SSE connections
- Measure connection success rates
- Track time to first event
- Monitor event throughput
- Detect connection errors and failures

### Performance Thresholds

The load test enforces these thresholds:
- **Connection Success Rate**: > 95%
- **Connection Errors**: < 500 total
- **95th Percentile Response Time**: < 5 seconds
- **Time to First Event (95th percentile)**: < 3 seconds

## Kafka Integration

Events broadcasted via SSE are also published to Kafka for:
- **Persistence**: Events are stored for replay and audit
- **Stream Processing**: Kafka consumers can aggregate and analyze events
- **Scalability**: Kafka enables horizontal scaling across multiple server instances
- **Decoupling**: Other services can consume events without connecting to SSE

See [README-KAFKA.md](README-KAFKA.md) for detailed Kafka configuration and usage.

## Scaling Considerations

### Horizontal Scaling

To scale SSE across multiple server instances:

1. **Use Kafka as Message Backbone**: Each server instance subscribes to Kafka topics and broadcasts events to its connected clients
2. **Sticky Sessions**: Configure load balancer for sticky sessions to keep clients connected to the same server
3. **Redis Pub/Sub**: Alternatively, use Redis pub/sub for broadcasting across instances (lighter weight than Kafka for simple scenarios)

### Vertical Scaling

Single instance limits:
- **Memory**: Each connection consumes ~4-8KB of memory
- **File Descriptors**: Default Linux limit is 1024; increase with `ulimit -n`
- **Event Emitter Listeners**: Configured to 250,000 (see `setMaxListeners`)

### Production Recommendations

1. **Reverse Proxy Configuration**: Ensure nginx/Apache doesn't buffer SSE responses
   ```nginx
   proxy_buffering off;
   proxy_cache off;
   proxy_set_header Connection '';
   proxy_http_version 1.1;
   chunked_transfer_encoding off;
   ```

2. **Connection Limits**: Monitor and adjust system limits for open connections

3. **Heartbeat/Keep-Alive**: Send periodic heartbeat messages to keep connections alive through proxies

4. **Graceful Degradation**: Implement backoff strategies for reconnection on the client side

5. **Monitoring**: Track metrics like active connections, event throughput, and memory usage

## API Endpoints

### GET /events
Server-Sent Events stream endpoint. Clients connect to receive real-time updates.

**Response Headers:**
- `Content-Type: text/event-stream;charset=utf-8`
- `Cache-Control: no-cache, no-transform`
- `Connection: keep-alive`

**Response Format:** Continuous stream of SSE messages

### GET /kafka/status
Check Kafka producer/consumer status and view latest event aggregation.

**Response:**
```json
{
  "producer": {
    "connected": true
  },
  "consumer": {
    "connected": true,
    "currentAggregation": { /* aggregated data */ }
  }
}
```

### GET /
Health check endpoint that lists all available endpoints.

## Running the Application

1. **Install dependencies:**
   ```bash
   bun install
   ```

2. **Start Kafka (if using):**
   ```bash
   docker-compose up -d
   ```

3. **Run the server:**
   ```bash
   bun run app.js
   ```

4. **Connect to SSE stream:**
   ```bash
   curl -N -H "Accept: text/event-stream" http://localhost:3000/events
   ```

## Comparison: SSE vs Other Technologies

| Feature | SSE | WebSockets | Long Polling | Short Polling |
|---------|-----|------------|--------------|---------------|
| **Protocol** | HTTP | WebSocket (TCP) | HTTP | HTTP |
| **Communication** | Unidirectional | Bidirectional | Bidirectional | Bidirectional |
| **Connection** | Persistent | Persistent | Pseudo-persistent | Request/Response |
| **Reconnection** | Automatic | Manual | Manual | N/A |
| **Browser Support** | Native API | Native API | Manual | Manual |
| **Complexity** | Low | Medium | Medium | Low |
| **Overhead** | Low | Low | Medium | High |
| **Firewall/Proxy** | Excellent | Poor | Good | Excellent |
| **Use Case** | Server Push | Real-time Chat | Fallback Option | Simple Updates |

## Conclusion

SSE provides an elegant, simple, and efficient solution for server-to-client real-time communication. Its native browser support, automatic reconnection, and HTTP compatibility make it an excellent choice for applications that need to push updates to clients without the complexity of bidirectional WebSocket connections.

For this project, SSE is ideal because:
- The primary use case is broadcasting events to multiple clients
- Clients don't need to send messages back (unidirectional)
- Simplicity and ease of debugging are priorities
- Integration with Kafka provides persistence and scalability
- High connection counts (50,000+) need to be supported efficiently
