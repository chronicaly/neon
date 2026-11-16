# Neon quality measurements

Three repeated runs per version; 240 warm-up frames then 480 sampled frames per scenario. Percentiles use nearest rank. Frame times include the configured frame limiter. CPU is sampled with ThreadMXBean (coarse Windows thread-clock resolution); scope time is the monotonic wall time inside the same Neon scopes. Allocations and rendering counters cover instrumented Neon work, not the entire client.

| Version | Scenario | Run | Median ms | p95 ms | p99 ms | CPU µs/frame | Scope µs/frame | Bytes/frame | Upload bytes/frame | Copy pixels/frame | Passes/frame |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Before | gameplay | 1 | 8.339 | 10.442 | 11.518 | 97.656 | 161.002 | 13010.633 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 2 | 8.346 | 11.170 | 19.072 | 97.656 | 170.816 | 12984.300 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 3 | 8.339 | 10.033 | 11.313 | 32.552 | 130.765 | 12828.833 | 0.000 | 0.000 | 0.000 |
| Before | hud-contact | 1 | 8.340 | 10.266 | 13.642 | 520.833 | 468.844 | 107111.500 | 0.000 | 0.000 | 0.000 |
| Before | hud-contact | 2 | 8.336 | 9.888 | 11.679 | 390.625 | 387.332 | 107088.883 | 0.000 | 0.000 | 0.000 |
| Before | hud-contact | 3 | 8.336 | 9.874 | 11.853 | 162.760 | 407.320 | 106996.133 | 0.000 | 0.000 | 0.000 |
| Before | hud-joined | 1 | 8.343 | 10.266 | 12.770 | 32.552 | 132.999 | 11210.967 | 0.000 | 0.000 | 0.000 |
| Before | hud-joined | 2 | 8.335 | 9.967 | 10.903 | 65.104 | 115.244 | 11140.533 | 0.000 | 0.000 | 0.000 |
| Before | hud-joined | 3 | 8.335 | 9.787 | 10.444 | 32.552 | 99.339 | 11094.817 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 1 | 8.339 | 10.236 | 11.510 | 65.104 | 132.022 | 12747.450 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 2 | 8.336 | 10.093 | 11.486 | 32.552 | 122.862 | 12685.517 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 3 | 8.336 | 9.963 | 10.859 | 32.552 | 110.572 | 12524.833 | 0.000 | 0.000 | 0.000 |
| After | hud-contact | 1 | 8.338 | 9.964 | 11.243 | 97.656 | 179.311 | 39166.767 | 0.000 | 0.000 | 0.000 |
| After | hud-contact | 2 | 8.337 | 10.103 | 10.892 | 162.760 | 162.508 | 39124.217 | 0.000 | 0.000 | 0.000 |
| After | hud-contact | 3 | 8.334 | 9.833 | 11.134 | 32.552 | 138.578 | 38962.683 | 0.000 | 0.000 | 0.000 |
| After | hud-joined | 1 | 8.337 | 10.018 | 11.895 | 97.656 | 103.265 | 11058.317 | 0.000 | 0.000 | 0.000 |
| After | hud-joined | 2 | 8.338 | 9.838 | 10.471 | 0.000 | 85.898 | 10962.667 | 0.000 | 0.000 | 0.000 |
| After | hud-joined | 3 | 8.338 | 9.982 | 10.588 | 65.104 | 89.307 | 10892.317 | 0.000 | 0.000 | 0.000 |

## Comparison

Values below use the median of the three per-run results. A repeated regression means an increase above 5% in at least two paired runs; investigate it before delivery.

| Scenario | Median Δ | p95 Δ | p99 Δ | Allocations before → after | Upload before → after | Scope before → after µs | Repeated >5% frame regression |
|---|---:|---:|---:|---:|---:|---:|---|
| gameplay | -0.037% | -3.350% | -0.276% | 12984.300 → 12685.517 | 0.000 → 0.000 | 161.002 → 122.862 | None |
| hud-contact | 0.007% | 0.771% | -6.070% | 107088.883 → 39124.217 | 0.000 → 0.000 | 407.320 → 162.508 | None |
| hud-joined | 0.029% | 0.146% | -2.895% | 11140.533 → 10962.667 | 0.000 → 0.000 | 115.244 → 89.307 | None |
