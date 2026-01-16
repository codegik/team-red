import sse from "k6/x/sse";
import { check, sleep } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";

// Custom metrics
const sseConnectionErrors = new Counter("sse_connection_errors");
const sseEventsReceived = new Counter("sse_events_received");
const sseConnectionSuccess = new Rate("sse_connection_success_rate");
const sseTimeToFirstEvent = new Trend("sse_time_to_first_event");
const sseEventRate = new Trend("sse_event_rate");

export const options = {
  scenarios: {
    // Ramp up to 10,000 users
    stress_test: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "2m", target: 1000 },   // Ramp up to 1k users
        { duration: "2m", target: 5000 },   // Ramp up to 5k users
        { duration: "2m", target: 10000 },  // Ramp up to 10k users
        { duration: "5m", target: 50000 },  // Stay at 10k for 5 minutes
        { duration: "2m", target: 0 },      // Ramp down
      ],
      gracefulRampDown: "30s",
    },
  },
  thresholds: {
    // Define success criteria
    "sse_connection_success_rate": ["rate > 0.95"], // 95% successful connections
    "sse_connection_errors": ["count < 500"],       // Less than 500 errors
    "http_req_duration": ["p(95) < 5000"],          // 95% of requests under 5s
    "sse_time_to_first_event": ["p(95) < 3000"],    // 95% receive first event within 3s
  },
};

export default function () {
  const url = "http://localhost:3000/events";
  const params = {
    method: "GET",
    headers: {
      Accept: "text/event-stream",
    },
    tags: {
      scenario: "sse_stress_test",
    },
  };

  const startTime = Date.now();
  let firstEventTime = null;
  let eventCount = 0;
  let hasError = false;

  const response = sse.open(url, params, function (client) {
    client.on("open", function () {
      console.log(`[VU ${__VU}] Connected to SSE endpoint`);
    });

    client.on("event", function (event) {
      // Skip empty events or retry messages
      if (!event.data || event.data.trim() === "") {
        return;
      }

      eventCount++;
      sseEventsReceived.add(1);

      // Track time to first event
      if (firstEventTime === null) {
        firstEventTime = Date.now();
        const timeToFirst = firstEventTime - startTime;
        sseTimeToFirstEvent.add(timeToFirst);
        console.log(`[VU ${__VU}] First event received in ${timeToFirst}ms`);
      }

      try {
        const data = JSON.parse(event.data);
        
        // Track event rate every 10 events
        if (eventCount % 10 === 0) {
          const totalTime = Date.now() - startTime;
          const eventsPerSecond = (eventCount / totalTime) * 1000;
          sseEventRate.add(eventsPerSecond);
          console.log(
            `[VU ${__VU}] Event #${eventCount}: ${eventsPerSecond.toFixed(2)} events/sec`
          );
        }
      } catch (e) {
        console.error(`[VU ${__VU}] Failed to parse event data: "${event.data}"`, e);
        hasError = true;
        sseConnectionErrors.add(1);
        client.close();
      }
    });

    client.on("error", function (error) {
      console.error(`[VU ${__VU}] SSE Error:`, error);
      hasError = true;
      sseConnectionErrors.add(1);
    });
  });

  // Check response status
  const connectionSuccess = check(response, {
    "connection established": (r) => r && r.status === 200,
    "no errors occurred": () => !hasError,
    "received events": () => eventCount > 0,
  });

  sseConnectionSuccess.add(connectionSuccess);

  // Small pause before next iteration
  sleep(1);
}
