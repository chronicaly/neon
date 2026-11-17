# Neon quality measurements

Three repeated runs per version; 240 warm-up frames then 480 sampled frames per scenario. Percentiles use nearest rank. Frame times include the configured frame limiter. CPU is sampled with ThreadMXBean (coarse Windows thread-clock resolution); scope time is the monotonic wall time inside the same Neon scopes. Allocations and rendering counters cover instrumented Neon work, not the entire client.

| Version | Scenario | Run | Median ms | p95 ms | p99 ms | CPU µs/frame | Scope µs/frame | Bytes/frame | Upload bytes/frame | Copy pixels/frame | Passes/frame |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Before | chat-docked | 1 | 8.334 | 10.490 | 11.742 | 520.833 | 481.347 | 120564.367 | 0.000 | 0.000 | 0.000 |
| Before | chat-docked | 2 | 8.332 | 9.959 | 10.867 | 390.625 | 379.610 | 120004.350 | 0.000 | 0.000 | 0.000 |
| Before | chat-docked | 3 | 8.332 | 10.015 | 11.717 | 260.417 | 403.099 | 119916.667 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 1 | 8.335 | 10.459 | 11.890 | 97.656 | 175.559 | 12771.467 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 2 | 8.330 | 10.126 | 11.101 | 32.552 | 142.317 | 12685.967 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 3 | 8.336 | 10.317 | 11.041 | 97.656 | 131.423 | 12495.967 | 0.000 | 0.000 | 0.000 |
| After | chat-docked | 1 | 8.335 | 10.399 | 11.572 | 358.073 | 452.893 | 121893.867 | 0.000 | 0.000 | 0.000 |
| After | chat-docked | 2 | 8.341 | 10.798 | 13.561 | 260.417 | 422.104 | 121204.633 | 0.000 | 0.000 | 0.000 |
| After | chat-docked | 3 | 8.338 | 10.081 | 10.923 | 227.865 | 365.909 | 120974.167 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 1 | 8.336 | 10.651 | 12.166 | 97.656 | 163.456 | 12785.983 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 2 | 8.337 | 10.065 | 10.905 | 97.656 | 139.818 | 12687.050 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 3 | 8.337 | 10.266 | 11.335 | 130.208 | 118.376 | 12496.217 | 0.000 | 0.000 | 0.000 |

## Comparison

Values below use the median of the three per-run results. A repeated regression means an increase above 5% in at least two paired runs; investigate it before delivery.

| Scenario | Median Δ | p95 Δ | p99 Δ | Allocations before → after | Upload before → after | Scope before → after µs | Repeated >5% frame regression |
|---|---:|---:|---:|---:|---:|---:|---|
| chat-docked | 0.061% | 3.836% | -1.243% | 120004.350 → 121204.633 | 0.000 → 0.000 | 403.099 → 422.104 | None |
| gameplay | 0.020% | -0.492% | 2.102% | 12685.967 → 12687.050 | 0.000 → 0.000 | 142.317 → 139.818 | None |
