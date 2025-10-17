import http from "k6/http";
import { check, sleep } from "k6";
import { Rate, Trend, Counter } from "k6/metrics";
import { SharedArray } from "k6/data";

const payloads = new SharedArray("payloads", function () {
  return JSON.parse(open("./payloads.json"));
});

// Custom metrics with tags for better comparison
const responseTime = new Trend("response_time_by_endpoint", true);
const errorRate = new Rate("error_rate_by_endpoint", true);
const requestCount = new Counter("requests_by_endpoint", true);
const throughput = new Rate("throughput_by_endpoint", true);

export const options = {
  stages: [
    { duration: "30s", target: 25 },
    { duration: "2m", target: 50 },
    { duration: "30s", target: 100 },
    { duration: "2m", target: 150 },
    { duration: "30s", target: 0 },
  ],
};

export default function () {
  const payload = payloads[Math.floor(Math.random() * payloads.length)];
  const { n, l, r } = payload;

  // Test Standard endpoint (direct calculation)
  const resStandard = http.post(
    `http://bitonic-app:8080/bitonic?n=${n}&l=${l}&r=${r}`,
    null,
    { tags: { endpoint: "standard" } }
  );

  const standardOk = check(resStandard, {
    "standard: status 200": (r) => r.status === 200,
    "standard: has response": (r) => r.body && r.body.length > 0,
  });

  // Record metrics with tags
  responseTime.add(resStandard.timings.duration, { endpoint: "standard" });
  errorRate.add(!standardOk ? 1 : 0, { endpoint: "standard" });
  requestCount.add(1, { endpoint: "standard" });
  throughput.add(1, { endpoint: "standard" });

  sleep(0.05);

  // Test Redis endpoint
  const resRedis = http.post(
    `http://bitonic-app:8080/bitonic-redis?n=${n}&l=${l}&r=${r}`,
    null,
    { tags: { endpoint: "redis" } }
  );

  const redisOk = check(resRedis, {
    "redis: status 200": (r) => r.status === 200,
    "redis: has response": (r) => r.body && r.body.length > 0,
  });

  // Record metrics with tags
  responseTime.add(resRedis.timings.duration, { endpoint: "redis" });
  errorRate.add(!redisOk ? 1 : 0, { endpoint: "redis" });
  requestCount.add(1, { endpoint: "redis" });
  throughput.add(1, { endpoint: "redis" });

  sleep(0.05);

  // Test Memcached endpoint
  const resMemcached = http.post(
    `http://bitonic-app:8080/bitonic-memcached?n=${n}&l=${l}&r=${r}`,
    null,
    { tags: { endpoint: "memcached" } }
  );

  const memcachedOk = check(resMemcached, {
    "memcached: status 200": (r) => r.status === 200,
    "memcached: has response": (r) => r.body && r.body.length > 0,
  });

  // Record metrics with tags
  responseTime.add(resMemcached.timings.duration, { endpoint: "memcached" });
  errorRate.add(!memcachedOk ? 1 : 0, { endpoint: "memcached" });
  requestCount.add(1, { endpoint: "memcached" });
  throughput.add(1, { endpoint: "memcached" });

  sleep(0.1);
}
