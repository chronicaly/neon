# Neon quality measurements

Three repeated runs per version; 240 warm-up frames then 480 sampled frames per scenario. Percentiles use nearest rank. Frame times include the configured frame limiter. CPU is sampled with ThreadMXBean (coarse Windows thread-clock resolution); scope time is the monotonic wall time inside the same Neon scopes. Allocations and rendering counters cover instrumented Neon work, not the entire client.

| Version | Scenario | Run | Median ms | p95 ms | p99 ms | CPU µs/frame | Scope µs/frame | Bytes/frame | Upload bytes/frame | Copy pixels/frame | Passes/frame |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Before | idle-menu | 1 | 8.336 | 9.278 | 9.864 | 683.594 | 556.871 | 83658.683 | 0.000 | 0.000 | 0.000 |
| Before | idle-menu | 2 | 8.336 | 9.135 | 9.881 | 195.313 | 444.926 | 82204.100 | 0.000 | 0.000 | 0.000 |
| Before | idle-menu | 3 | 8.335 | 9.223 | 9.869 | 455.729 | 449.052 | 82042.617 | 0.000 | 0.000 | 0.000 |
| Before | menu-resize | 1 | 8.336 | 9.258 | 9.963 | 520.833 | 550.215 | 177600.033 | 0.000 | 0.000 | 0.000 |
| Before | menu-resize | 2 | 8.334 | 9.164 | 9.632 | 358.073 | 515.030 | 177303.000 | 2.533 | 0.000 | 0.000 |
| Before | menu-resize | 3 | 8.336 | 9.041 | 9.771 | 358.073 | 480.327 | 177163.817 | 0.000 | 0.000 | 0.000 |
| After | idle-menu | 1 | 8.342 | 9.466 | 11.530 | 455.729 | 901.847 | 87644.000 | 4.250 | 0.000 | 0.000 |
| After | idle-menu | 2 | 8.338 | 9.160 | 9.876 | 520.833 | 776.373 | 86690.550 | 0.000 | 0.000 | 0.000 |
| After | idle-menu | 3 | 8.336 | 9.133 | 9.493 | 390.625 | 772.371 | 86436.733 | 0.000 | 0.000 | 0.000 |
| After | menu-resize | 1 | 8.336 | 9.254 | 10.594 | 488.281 | 781.409 | 177963.267 | 0.000 | 0.000 | 0.000 |
| After | menu-resize | 2 | 8.339 | 9.124 | 9.829 | 455.729 | 739.520 | 177560.217 | 0.000 | 0.000 | 0.000 |
| After | menu-resize | 3 | 8.338 | 9.960 | 14.792 | 325.521 | 688.664 | 177148.017 | 0.000 | 0.000 | 0.000 |

## Comparison

Values below use the median of the three per-run results. A repeated regression means an increase above 5% in at least two paired runs; investigate it before delivery.

| Scenario | Median Δ | p95 Δ | p99 Δ | Allocations before → after | Upload before → after | Scope before → after µs | Repeated >5% frame regression |
|---|---:|---:|---:|---:|---:|---:|---|
| idle-menu | 0.022% | -0.679% | 0.066% | 82204.100 → 86690.550 | 0.000 → 0.000 | 449.052 → 776.373 | None |
| menu-resize | 0.025% | 0.974% | 8.420% | 177303.000 → 177560.217 | 0.000 → 0.000 | 515.030 → 739.520 | p99 |
