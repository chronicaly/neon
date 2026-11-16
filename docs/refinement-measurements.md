# Neon quality measurements

Three repeated runs per version; 240 warm-up frames then 480 sampled frames per scenario. Percentiles use nearest rank. Frame times include the configured frame limiter. CPU is sampled with ThreadMXBean (coarse Windows thread-clock resolution); scope time is the monotonic wall time inside the same Neon scopes. Allocations and rendering counters cover instrumented Neon work, not the entire client.

| Version | Scenario | Run | Median ms | p95 ms | p99 ms | CPU µs/frame | Scope µs/frame | Bytes/frame | Upload bytes/frame | Copy pixels/frame | Passes/frame |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Before | gameplay | 1 | 8.336 | 10.114 | 11.369 | 130.208 | 136.227 | 9649.400 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 2 | 8.334 | 10.140 | 11.958 | 65.104 | 121.995 | 9403.583 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 3 | 8.333 | 9.862 | 10.924 | 97.656 | 113.949 | 9348.000 | 0.000 | 0.000 | 0.000 |
| Before | hud-catalog | 1 | 8.334 | 9.933 | 11.088 | 748.698 | 956.180 | 48479.533 | 0.000 | 0.000 | 0.000 |
| Before | hud-catalog | 2 | 8.335 | 9.896 | 10.546 | 683.594 | 945.790 | 47101.350 | 0.000 | 0.000 | 0.000 |
| Before | hud-catalog | 3 | 8.334 | 9.831 | 10.581 | 585.938 | 924.112 | 47217.150 | 0.000 | 0.000 | 0.000 |
| Before | idle-menu | 1 | 8.334 | 8.970 | 9.543 | 358.073 | 418.396 | 80558.333 | 0.000 | 0.000 | 0.000 |
| Before | idle-menu | 2 | 8.333 | 9.120 | 9.647 | 292.969 | 392.754 | 79047.600 | 0.000 | 0.000 | 0.000 |
| Before | idle-menu | 3 | 8.335 | 9.102 | 9.635 | 325.521 | 379.375 | 78884.000 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 1 | 8.336 | 10.037 | 10.987 | 65.104 | 86.161 | 12475.117 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 2 | 8.337 | 10.016 | 11.263 | 0.000 | 74.143 | 12434.717 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 3 | 8.332 | 9.776 | 10.551 | 0.000 | 58.699 | 12284.000 | 0.000 | 0.000 | 0.000 |
| After | hud-catalog | 1 | 8.341 | 9.884 | 11.434 | 195.313 | 273.255 | 53297.333 | 0.000 | 0.000 | 0.000 |
| After | hud-catalog | 2 | 8.331 | 9.751 | 10.486 | 195.313 | 242.248 | 55682.650 | 0.000 | 0.000 | 0.000 |
| After | hud-catalog | 3 | 8.336 | 9.802 | 10.657 | 0.000 | 212.176 | 52563.483 | 0.000 | 0.000 | 0.000 |
| After | idle-menu | 1 | 8.335 | 8.973 | 9.294 | 195.313 | 401.672 | 83554.133 | 0.000 | 0.000 | 0.000 |
| After | idle-menu | 2 | 8.335 | 8.984 | 9.470 | 227.865 | 345.294 | 82158.600 | 0.000 | 0.000 | 0.000 |
| After | idle-menu | 3 | 8.335 | 8.907 | 9.570 | 325.521 | 323.797 | 81900.000 | 0.000 | 0.000 | 0.000 |

## Comparison

Values below use the median of the three per-run results. A repeated regression means an increase above 5% in at least two paired runs; investigate it before delivery.

| Scenario | Median Δ | p95 Δ | p99 Δ | Allocations before → after | Upload before → after | Scope before → after µs | Repeated >5% frame regression |
|---|---:|---:|---:|---:|---:|---:|---|
| gameplay | 0.026% | -0.970% | -3.355% | 9403.583 → 12434.717 | 0.000 → 0.000 | 121.995 → 74.143 | None |
| hud-catalog | 0.019% | -0.942% | 0.722% | 47217.150 → 53297.333 | 0.000 → 0.000 | 945.790 → 242.248 | None |
| idle-menu | 0.005% | -1.415% | -1.706% | 79047.600 → 82158.600 | 0.000 → 0.000 | 392.754 → 345.294 | None |
