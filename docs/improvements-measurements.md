# Neon quality measurements

Three repeated runs per version; 240 warm-up frames then 480 sampled frames per scenario. Percentiles use nearest rank. Frame times include the configured frame limiter. CPU is sampled with ThreadMXBean (coarse Windows thread-clock resolution); scope time is the monotonic wall time inside the same Neon scopes. Allocations and rendering counters cover instrumented Neon work, not the entire client.

| Version | Scenario | Run | Median ms | p95 ms | p99 ms | CPU µs/frame | Scope µs/frame | Bytes/frame | Upload bytes/frame | Copy pixels/frame | Passes/frame |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Before | chat-docked | 1 | 8.332 | 9.975 | 11.448 | 195.313 | 346.658 | 121903.850 | 0.000 | 0.000 | 0.000 |
| Before | chat-docked | 2 | 8.333 | 9.762 | 10.577 | 227.865 | 314.895 | 121102.517 | 0.000 | 0.000 | 0.000 |
| Before | chat-docked | 3 | 8.333 | 9.829 | 11.216 | 195.313 | 297.013 | 120741.400 | 0.000 | 0.000 | 0.000 |
| Before | chat-receive | 1 | 8.332 | 9.782 | 11.814 | 716.146 | 742.309 | 455364.367 | 0.000 | 0.000 | 0.000 |
| Before | chat-receive | 2 | 8.331 | 9.779 | 11.316 | 423.177 | 682.209 | 453761.867 | 0.000 | 0.000 | 0.000 |
| Before | chat-receive | 3 | 8.332 | 9.727 | 10.472 | 651.042 | 675.062 | 453418.067 | 0.000 | 0.000 | 0.000 |
| Before | chat-visible | 1 | 8.332 | 9.962 | 10.757 | 358.073 | 529.216 | 226847.250 | 0.000 | 0.000 | 0.000 |
| Before | chat-visible | 2 | 8.333 | 9.834 | 10.538 | 423.177 | 499.079 | 226292.817 | 0.000 | 0.000 | 0.000 |
| Before | chat-visible | 3 | 8.333 | 9.713 | 10.703 | 260.417 | 460.949 | 226273.500 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 1 | 8.334 | 10.173 | 10.798 | 32.552 | 123.048 | 12702.667 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 2 | 8.331 | 10.025 | 10.751 | 130.208 | 90.030 | 12427.083 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 3 | 8.333 | 9.908 | 10.949 | 32.552 | 88.216 | 12429.217 | 0.000 | 0.000 | 0.000 |
| After | chat-docked | 1 | 8.340 | 9.917 | 11.629 | 130.208 | 339.704 | 87726.883 | 0.000 | 0.000 | 0.000 |
| After | chat-docked | 2 | 8.338 | 9.806 | 10.282 | 162.760 | 308.267 | 87070.500 | 0.000 | 0.000 | 0.000 |
| After | chat-docked | 3 | 8.336 | 9.659 | 10.221 | 195.313 | 274.930 | 86692.750 | 0.000 | 0.000 | 0.000 |
| After | chat-receive | 1 | 8.337 | 9.830 | 10.314 | 130.208 | 500.800 | 132457.383 | 0.000 | 0.000 | 0.000 |
| After | chat-receive | 2 | 8.337 | 9.707 | 10.276 | 390.625 | 465.338 | 132379.717 | 0.000 | 0.000 | 0.000 |
| After | chat-receive | 3 | 8.334 | 9.665 | 10.110 | 358.073 | 436.315 | 132091.833 | 0.000 | 0.000 | 0.000 |
| After | chat-visible | 1 | 8.340 | 9.951 | 10.827 | 358.073 | 504.761 | 157839.833 | 0.000 | 0.000 | 0.000 |
| After | chat-visible | 2 | 8.341 | 9.809 | 11.137 | 227.865 | 473.871 | 157767.150 | 0.000 | 0.000 | 0.000 |
| After | chat-visible | 3 | 8.336 | 9.703 | 10.943 | 358.073 | 453.283 | 157702.883 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 1 | 8.337 | 10.151 | 10.593 | 32.552 | 125.994 | 12742.333 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 2 | 8.336 | 9.867 | 10.571 | 32.552 | 91.154 | 12515.967 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 3 | 8.337 | 9.870 | 11.152 | 0.000 | 88.435 | 12515.833 | 0.000 | 0.000 | 0.000 |

## Comparison

Values below use the median of the three per-run results. A repeated regression means an increase above 5% in at least two paired runs; investigate it before delivery.

| Scenario | Median Δ | p95 Δ | p99 Δ | Allocations before → after | Upload before → after | Scope before → after µs | Repeated >5% frame regression |
|---|---:|---:|---:|---:|---:|---:|---|
| chat-docked | 0.064% | -0.227% | -8.324% | 121102.517 → 87070.500 | 0.000 → 0.000 | 314.895 → 308.267 | None |
| chat-receive | 0.056% | -0.742% | -9.191% | 453761.867 → 132379.717 | 0.000 → 0.000 | 682.209 → 465.338 | None |
| chat-visible | 0.089% | -0.254% | 2.249% | 226292.817 → 157767.150 | 0.000 → 0.000 | 499.079 → 473.871 | None |
| gameplay | 0.046% | -1.540% | -1.901% | 12429.217 → 12515.967 | 0.000 → 0.000 | 90.030 → 91.154 | None |
