# Neon 0.5.0 expansion measurements

Three repeated runs per version; 240 warm-up frames then 480 sampled frames per scenario. Percentiles use nearest rank. Frame times include the configured frame limiter. CPU is sampled with ThreadMXBean (coarse Windows thread-clock resolution); scope time is the monotonic wall time inside the same Neon scopes. Allocations and rendering counters cover instrumented Neon work, not the entire client.

See [environment, artifact hashes, interpretation and limits](expansion-results.md#measured-costs). Raw samples are retained in [expansion-samples.zip](expansion-samples.zip). The HUD-catalog workload exists only in 0.5.0 and is listed separately below.

| Version | Scenario | Run | Median ms | p95 ms | p99 ms | CPU µs/frame | Scope µs/frame | Bytes/frame | Upload bytes/frame | Copy pixels/frame | Passes/frame |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Before | gameplay | 1 | 8.339 | 10.124 | 10.909 | 32.552 | 68.736 | 13457.917 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 2 | 8.335 | 9.935 | 10.896 | 0.000 | 51.474 | 13098.667 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 3 | 8.337 | 9.824 | 10.765 | 0.000 | 49.998 | 13068.000 | 0.000 | 0.000 | 0.000 |
| Before | idle-menu | 1 | 8.332 | 9.119 | 9.709 | 292.969 | 346.278 | 46145.267 | 0.000 | 0.000 | 0.000 |
| Before | idle-menu | 2 | 8.334 | 9.076 | 9.674 | 130.208 | 289.560 | 44312.067 | 0.000 | 0.000 | 0.000 |
| Before | idle-menu | 3 | 8.335 | 9.013 | 9.404 | 162.760 | 283.545 | 44236.050 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 1 | 8.338 | 10.128 | 11.126 | 97.656 | 134.597 | 9599.017 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 2 | 8.333 | 9.885 | 11.049 | 97.656 | 122.959 | 9435.767 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 3 | 8.333 | 9.832 | 10.691 | 97.656 | 114.215 | 9348.000 | 0.000 | 0.000 | 0.000 |
| After | idle-menu | 1 | 8.334 | 9.106 | 10.016 | 260.417 | 411.000 | 76114.800 | 0.000 | 0.000 | 0.000 |
| After | idle-menu | 2 | 8.334 | 9.095 | 9.626 | 390.625 | 384.369 | 74601.733 | 0.000 | 0.000 | 0.000 |
| After | idle-menu | 3 | 8.334 | 8.975 | 9.760 | 423.177 | 364.961 | 74492.050 | 0.000 | 0.000 | 0.000 |

## Comparison

Values below use the median of the three per-run results. A repeated regression means an increase above 5% in at least two paired runs; investigate it before delivery.

| Scenario | Median Δ | p95 Δ | p99 Δ | Allocations before → after | Upload before → after | Scope before → after µs | Repeated >5% frame regression |
|---|---:|---:|---:|---:|---:|---:|---|
| gameplay | -0.043% | -0.497% | 1.406% | 13098.667 → 9435.767 | 0.000 → 0.000 | 51.474 → 122.959 | None |
| idle-menu | 0.000% | 0.209% | 0.885% | 44312.067 → 74601.733 | 0.000 → 0.000 | 289.560 → 384.369 | None |

## Expanded HUD workload

All twenty additions enabled, with FPS background/border and existing equipment HUD. Empty potion/scoreboard data draws nothing. Same warm-up, sample count and environment as above; no equivalent baseline scenario exists.

| Run | Frames | Median ms | p95 ms | p99 ms | Scope µs/frame | Bytes/frame |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 480 | 8.335 | 9.896 | 11.738 | 953.581 | 48,684.883 |
| 2 | 480 | 8.334 | 9.867 | 10.463 | 961.451 | 47,809.450 |
| 3 | 480 | 8.336 | 9.832 | 10.507 | 934.146 | 47,195.417 |
