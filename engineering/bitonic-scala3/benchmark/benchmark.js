import http from "k6/http";
import { check, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";
import { SharedArray } from "k6/data";

const payloads = new SharedArray("payloads", function () {
  return JSON.parse(open("./payloads.json"));
});

const errorRateStandard = new Rate("errors_standard");
const errorRateRedis = new Rate("errors_redis");
const errorRateMemcached = new Rate("errors_memcached");
const durationStandard = new Trend("duration_standard");
const durationRedis = new Trend("duration_redis");
const durationMemcached = new Trend("duration_memcached");

export const options = {
  stages: [{ duration: "5m", target: 500 }],
};

export default function () {
  const payload = payloads[Math.floor(Math.random() * payloads.length)];
  const { n, l, r } = payload;

  const resStandard = http.post(
    `http://bitonic-app:8080/bitonic?n=${n}&l=${l}&r=${r}`
  );

  const standardOk = check(resStandard, {
    "standard: status 200": (r) => r.status === 200,
    "standard: has response": (r) => r.body && r.body.length > 0,
  });

  if (!standardOk) {
    errorRateStandard.add(1);
  }

  durationStandard.add(resStandard.timings.duration);

  sleep(0.05);

  const resRedis = http.post(
    `http://bitonic-app:8080/bitonic-redis?n=${n}&l=${l}&r=${r}`
  );

  const redisOk = check(resRedis, {
    "cached: status 200": (r) => r.status === 200,
    "cached: has response": (r) => r.body && r.body.length > 0,
  });

  if (!redisOk) {
    errorRateRedis.add(1);
  }

  durationRedis.add(resRedis.timings.duration);

  sleep(0.05);

  const resMemcached = http.post(
    `http://bitonic-app:8080/bitonic-memcached?n=${n}&l=${l}&r=${r}`
  );

  const memcachedOk = check(resMemcached, {
    "cached: status 200": (r) => r.status === 200,
    "cached: has response": (r) => r.body && r.body.length > 0,
  });

  if (!memcachedOk) {
    errorRateMemcached.add(1);
  }

  durationMemcached.add(resMemcached.timings.duration);

  sleep(0.1);
}
