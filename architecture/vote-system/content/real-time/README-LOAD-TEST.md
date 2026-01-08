# SSE Load Testing with k6 and xk6-sse

This directory contains performance tests for the SSE (Server-Sent Events) endpoint using k6 with the xk6-sse extension.

## Prerequisites

### Install k6 with SSE support

Since k6 now automatically resolves community extensions, you can simply run:

```bash
# Download latest k6
# For Linux:
wget https://github.com/grafana/k6/releases/download/v0.54.0/k6-v0.54.0-linux-amd64.tar.gz
tar -xzf k6-v0.54.0-linux-amd64.tar.gz
sudo mv k6-v0.54.0-linux-amd64/k6 /usr/local/bin/

# Or install via package manager:
# Ubuntu/Debian
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# macOS
brew install k6
```

**Note:** As of k6's recent updates, the xk6-sse extension is resolved automatically when you `import sse from "k6/x/sse"`. No custom build required!

If automatic resolution doesn't work, you can build k6 with xk6:

```bash
# Install xk6
go install go.k6.io/xk6/cmd/xk6@latest

# Build k6 with xk6-sse extension
xk6 build --with github.com/phymbert/xk6-sse@latest

# This creates a ./k6 binary in the current directory
```

## Test Scripts

### 1. `sse-load-test.js` - Comprehensive Load Test

This script gradually ramps up to 10,000 concurrent users with detailed metrics:

**Features:**
- Ramps up from 0 → 1k → 5k → 10k users over 6 minutes
- Maintains 10k users for 5 minutes
- Tracks custom metrics:
  - Time to first event
  - Event rate per connection
  - Connection success rate
  - Total events received
- Each user receives 5 events (~10 seconds) before closing

**Run:**
```bash
k6 run sse-load-test.js
```

### 2. `sse-load-test-simple.js` - Simple Constant Load

This script immediately starts 10,000 users for a constant load test:

**Features:**
- 10,000 concurrent users for 5 minutes
- Simpler metrics (connection success, events received)
- Each user receives 3 events (~6 seconds) before closing
- Good for baseline performance testing

**Run:**
```bash
k6 run sse-load-test-simple.js
```

## Running the Tests

### 1. Start your application
```bash
bun run app.js
```

### 2. Run the load test

**Comprehensive test:**
```bash
k6 run sse-load-test.js
```

**Simple constant load:**
```bash
k6 run sse-load-test-simple.js
```

**With custom output (InfluxDB, Prometheus, etc.):**
```bash
k6 run --out influxdb=http://localhost:8086/k6 sse-load-test.js
```

**Run with custom VU count:**
```bash
k6 run --vus 5000 --duration 3m sse-load-test-simple.js
```

## Expected Results

### Success Criteria (Thresholds)

**Comprehensive test:**
- ✅ 95% connection success rate
- ✅ Less than 500 connection errors
- ✅ 95% of requests complete under 5 seconds
- ✅ 95% receive first event within 3 seconds

**Simple test:**
- ✅ 90% connection success rate
- ✅ Less than 1000 connection errors
- ✅ 95% of requests complete under 5 seconds

### Metrics to Monitor

- `sse_events_received` - Total SSE events received
- `sse_connection_errors` - Failed connections
- `sse_connection_success_rate` - Percentage of successful connections
- `sse_time_to_first_event` - Latency until first event (comprehensive test)
- `sse_event_rate` - Events per second per connection (comprehensive test)
- `http_req_duration` - Request duration distribution
- `vus` - Current virtual users

## Performance Optimization Tips

If you encounter issues with 10,000 concurrent connections:

### 1. System Limits
```bash
# Check current limits
ulimit -n

# Increase file descriptor limit
ulimit -n 65536

# For permanent change, edit /etc/security/limits.conf:
* soft nofile 65536
* hard nofile 65536
```

### 2. Node.js/Bun EventEmitter Limits
The app already sets `sseEvents.setMaxListeners(10000)`, but you may need to increase this further or optimize the event broadcasting mechanism.

### 3. Network Tuning (Linux)
```bash
# Increase network buffer sizes
sudo sysctl -w net.core.rmem_max=26214400
sudo sysctl -w net.core.wmem_max=26214400
```

### 4. Application Optimizations
- Consider using Redis pub/sub for event broadcasting at scale
- Implement connection pooling
- Add rate limiting
- Use a reverse proxy (nginx) with SSE optimizations

## Monitoring During Tests

Watch real-time metrics:
```bash
# Monitor connections
watch -n 1 'ss -tan | grep :3000 | wc -l'

# Monitor memory usage
watch -n 1 'ps aux | grep bun'

# Monitor system load
htop
```

## Cloud Load Testing

For realistic 10k user tests, consider running k6 from:
- [k6 Cloud](https://k6.io/cloud)
- AWS EC2 instances
- GCP Compute Engine
- Azure VMs

## References

- [xk6-sse GitHub](https://github.com/phymbert/xk6-sse)
- [k6 Documentation](https://k6.io/docs/)
- [SSE Specification](https://html.spec.whatwg.org/multipage/server-sent-events.html)
- [PrinceJS Documentation](https://github.com/princejs-org/princejs)
