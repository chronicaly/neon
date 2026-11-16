# Neon quality measurements

Three repeated runs per version; 240 warm-up frames then 480 sampled frames per scenario. Percentiles use nearest rank. Frame times include the configured frame limiter. CPU is sampled with ThreadMXBean (coarse Windows thread-clock resolution); scope time is the monotonic wall time inside the same Neon scopes. Allocations and rendering counters cover instrumented Neon work, not the entire client.

| Version | Scenario | Run | Median ms | p95 ms | p99 ms | CPU µs/frame | Scope µs/frame | Bytes/frame | Upload bytes/frame | Copy pixels/frame | Passes/frame |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Before | gameplay | 1 | 8.344 | 10.523 | 12.373 | 227.865 | 153.483 | 12602.933 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 2 | 8.340 | 10.036 | 11.759 | 32.552 | 104.488 | 12452.583 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 3 | 8.332 | 10.166 | 11.014 | 32.552 | 96.310 | 12348.050 | 0.000 | 0.000 | 0.000 |
| Before | hud-catalog | 1 | 8.341 | 10.572 | 14.875 | 781.250 | 989.904 | 53463.433 | 0.000 | 0.000 | 0.000 |
| Before | hud-catalog | 2 | 8.341 | 10.229 | 12.508 | 683.594 | 901.961 | 53041.900 | 0.000 | 0.000 | 0.000 |
| Before | hud-catalog | 3 | 8.335 | 9.992 | 12.489 | 455.729 | 880.153 | 52850.567 | 0.000 | 0.000 | 0.000 |
| Before | idle-menu | 1 | 8.337 | 9.207 | 9.798 | 195.313 | 518.735 | 83764.883 | 0.000 | 0.000 | 0.000 |
| Before | idle-menu | 2 | 8.336 | 9.092 | 9.594 | 488.281 | 418.087 | 81997.200 | 0.000 | 0.000 | 0.000 |
| Before | idle-menu | 3 | 8.337 | 9.344 | 12.445 | 227.865 | 400.173 | 81793.483 | 0.000 | 0.000 | 0.000 |
| Before | menu-resize | 1 | 8.336 | 9.213 | 9.752 | 195.313 | 539.660 | 177625.117 | 0.000 | 0.000 | 0.000 |
| Before | menu-resize | 2 | 8.335 | 9.111 | 9.587 | 423.177 | 485.783 | 177069.033 | 0.000 | 0.000 | 0.000 |
| Before | menu-resize | 3 | 8.335 | 9.301 | 9.879 | 358.073 | 463.107 | 176885.083 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 1 | 8.338 | 10.634 | 12.017 | 97.656 | 147.739 | 12598.867 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 2 | 8.334 | 10.174 | 11.282 | 162.760 | 112.848 | 12419.417 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 3 | 8.337 | 10.241 | 12.781 | 0.000 | 100.743 | 12365.683 | 0.000 | 0.000 | 0.000 |
| After | hud-catalog | 1 | 8.344 | 12.015 | 16.602 | 878.906 | 975.521 | 53116.733 | 0.000 | 0.000 | 0.000 |
| After | hud-catalog | 2 | 8.341 | 10.087 | 12.425 | 553.385 | 928.484 | 55163.200 | 0.000 | 0.000 | 0.000 |
| After | hud-catalog | 3 | 8.335 | 10.024 | 11.080 | 520.833 | 890.961 | 57409.267 | 0.000 | 0.000 | 0.000 |
| After | idle-menu | 1 | 8.338 | 9.758 | 13.418 | 781.250 | 861.920 | 87634.383 | 4.250 | 0.000 | 0.000 |
| After | idle-menu | 2 | 8.341 | 9.544 | 12.012 | 585.938 | 760.895 | 86761.300 | 0.000 | 0.000 | 0.000 |
| After | idle-menu | 3 | 8.334 | 9.258 | 9.886 | 781.250 | 767.524 | 86514.083 | 0.000 | 0.000 | 0.000 |
| After | menu-resize | 1 | 8.338 | 9.428 | 11.669 | 651.042 | 766.126 | 178384.283 | 0.000 | 0.000 | 0.000 |
| After | menu-resize | 2 | 8.338 | 9.195 | 12.300 | 423.177 | 725.167 | 177443.483 | 0.000 | 0.000 | 0.000 |
| After | menu-resize | 3 | 8.336 | 9.150 | 9.691 | 553.385 | 694.860 | 177127.517 | 0.000 | 0.000 | 0.000 |

## Comparison

Values below use the median of the three per-run results. A repeated regression means an increase above 5% in at least two paired runs; investigate it before delivery.

| Scenario | Median Δ | p95 Δ | p99 Δ | Allocations before → after | Upload before → after | Scope before → after µs | Repeated >5% frame regression |
|---|---:|---:|---:|---:|---:|---:|---|
| gameplay | -0.035% | 0.740% | 2.190% | 12452.583 → 12419.417 | 0.000 → 0.000 | 104.488 → 112.848 | None |
| hud-catalog | 0.001% | -1.397% | -0.664% | 53041.900 → 55163.200 | 0.000 → 0.000 | 901.961 → 928.484 | None |
| idle-menu | 0.019% | 3.659% | 22.603% | 81997.200 → 86761.300 | 0.000 → 0.000 | 418.087 → 767.524 | p99 |
| menu-resize | 0.028% | -0.199% | 19.659% | 177069.033 → 177443.483 | 0.000 → 0.000 | 485.783 → 725.167 | p99 |
