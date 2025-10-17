#!/usr/bin/env python3
import requests
import time
import csv

# -----------------------------
# CONFIGURATION
# -----------------------------
BASE_URL = "http://localhost:8080"
ENDPOINTS = ["bitonic", "bitonic-memcached"]

# Different sizes of N to test — from small to very large
SIZES = [10, 100, 1000, 10_000, 50_000, 100_000, 200_000, 500_000]

L = 1
R = 1_000_000
OUT_FILE = "benchmark_results.csv"

# -----------------------------
# HELPER FUNCTIONS
# -----------------------------
def measure_time(url: str):
    """Send a POST request and measure the response time."""
    start = time.perf_counter()
    try:
        response = requests.post(url, timeout=120)
        duration = time.perf_counter() - start
        size = len(response.content)
        status = response.status_code
        return duration, size, status
    except Exception as e:
        duration = time.perf_counter() - start
        return duration, 0, f"error: {e}"

# -----------------------------
# MAIN BENCHMARK
# -----------------------------
def main():
    results = []
    print(f"Benchmarking endpoints at {BASE_URL}")
    print(f"Testing sizes: {SIZES}\n")

    for n in SIZES:
        for endpoint in ENDPOINTS:
            url = f"{BASE_URL}/{endpoint}?n={n}&l={L}&r={R}"
            print(f"→ Testing {endpoint:20s} | n={n:>8} | ", end="", flush=True)

            duration, size, status = measure_time(url)
            print(f"{duration:.3f}s | size={size} bytes | status={status}")

            results.append({
                "endpoint": endpoint,
                "n": n,
                "l": L,
                "r": R,
                "duration_s": round(duration, 6),
                "size_bytes": size,
                "status": status,
            })

    # -----------------------------
    # SAVE CSV RESULTS
    # -----------------------------
    with open(OUT_FILE, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=results[0].keys())
        writer.writeheader()
        writer.writerows(results)

    print(f"\n✅ Results saved to {OUT_FILE}")
    print("\nSample summary (short):")
    for row in results:
        print(f"{row['endpoint']:20s} | n={row['n']:>8} | {row['duration_s']:.3f}s")

# -----------------------------
# ENTRY POINT
# -----------------------------
if __name__ == "__main__":
    main()
