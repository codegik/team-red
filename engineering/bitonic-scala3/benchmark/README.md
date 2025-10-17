# Bitonic Benchmark Runner

This script benchmarks the two endpoints:

- `POST /bitonic?n=...&l=...&r=...`
- `POST /bitonic-memcached?n=...&l=...&r=...`

It measures latency statistics across different `n` sizes, with configurable concurrency and request counts, and writes a CSV report.

## Requirements

- Python 3.9+
- `aiohttp`

Install dependencies:

1. **Install Python 3.11.13 using pyenv:**
   ```bash
   pyenv install 3.11.13
   pyenv local 3.11.13  # Set for current project
   ```

2. **Install uv package manager:**
   ```bash
   pip install uv==0.7.11
   ```

3. **Create and activate virtual environment:**
   ```bash
   uv venv
   source .venv/bin/activate
    ```

    ```bash
    pip install aiohttp
    ```

## Usage

```bash
python benchmark_bitonic.py --base-url http://localhost:8080   --endpoints bitonic bitonic-memcached   --sizes 16 32 64 128 256 512 1024   --l 1 --r 100000   --requests 200 --concurrency 50   --warmup 20   --out results.csv
```

## Output

- A `results.csv` with columns:
  - `endpoint, n, l, r, requests, concurrency, success_rate, latency_mean_s, latency_stdev_s, latency_p50_s, latency_p90_s, latency_p95_s, latency_p99_s, resp_size_mean_bytes`

