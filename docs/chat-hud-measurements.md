# Neon 0.5.3 chat and HUD measurements

Three repeated runs per version; 240 warm-up frames then 480 sampled frames per scenario. Percentiles use nearest rank. Frame times include the configured frame limiter. CPU is sampled with ThreadMXBean (coarse Windows thread-clock resolution); scope time is the monotonic wall time inside the same Neon scopes. The scopes include Neon work, native chat extraction and deferred Neon surface/glyph vertices. They exclude other native rendering and GPU time. These scope values are not directly comparable with older reports that omitted native chat extraction.

| Version | Scenario | Run | Median ms | p95 ms | p99 ms | CPU µs/frame | Scope µs/frame | Bytes/frame | Upload bytes/frame | Copy pixels/frame | Passes/frame |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Before | chat-visible | 1 | 8.335 | 9.870 | 11.377 | 97.656 | 193.243 | 85555.100 | 0.000 | 0.000 | 0.000 |
| Before | chat-visible | 2 | 8.334 | 9.758 | 10.849 | 130.208 | 168.781 | 85404.583 | 0.000 | 0.000 | 0.000 |
| Before | chat-visible | 3 | 8.333 | 9.768 | 10.384 | 97.656 | 171.257 | 85383.883 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 1 | 8.338 | 10.096 | 10.889 | 65.104 | 113.023 | 12807.683 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 2 | 8.333 | 9.742 | 10.629 | 65.104 | 88.438 | 12674.967 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 3 | 8.336 | 9.717 | 11.360 | 130.208 | 83.874 | 12577.833 | 0.000 | 0.000 | 0.000 |
| Before | hud-placement | 1 | 8.335 | 9.965 | 11.043 | 716.146 | 687.398 | 68091.900 | 0.000 | 0.000 | 0.000 |
| Before | hud-placement | 2 | 8.334 | 9.819 | 10.609 | 423.177 | 636.126 | 56728.150 | 0.000 | 0.000 | 0.000 |
| Before | hud-placement | 3 | 8.334 | 9.731 | 10.520 | 716.146 | 629.556 | 56632.100 | 0.000 | 0.000 | 0.000 |
| After | chat-visible | 1 | 8.331 | 9.859 | 10.913 | 358.073 | 486.033 | 227281.317 | 0.000 | 0.000 | 0.000 |
| After | chat-visible | 2 | 8.335 | 9.707 | 9.986 | 390.625 | 471.755 | 227034.950 | 0.000 | 0.000 | 0.000 |
| After | chat-visible | 3 | 8.332 | 9.709 | 10.339 | 227.865 | 456.999 | 226892.967 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 1 | 8.333 | 10.090 | 11.331 | 65.104 | 117.972 | 12967.150 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 2 | 8.331 | 9.785 | 10.355 | 130.208 | 88.466 | 12892.917 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 3 | 8.331 | 9.784 | 10.969 | 65.104 | 80.740 | 12799.383 | 0.000 | 0.000 | 0.000 |
| After | hud-placement | 1 | 8.334 | 9.894 | 10.916 | 553.385 | 745.891 | 76503.017 | 0.000 | 0.000 | 0.000 |
| After | hud-placement | 2 | 8.334 | 9.764 | 10.532 | 520.833 | 687.788 | 64727.900 | 0.000 | 0.000 | 0.000 |
| After | hud-placement | 3 | 8.333 | 9.642 | 10.407 | 1041.667 | 678.817 | 64631.950 | 0.000 | 0.000 | 0.000 |

## Comparison

Values below use the median of the three per-run results. A repeated regression means an increase above 5% in at least two paired runs; investigate it before delivery.

| Scenario | Median Δ | p95 Δ | p99 Δ | Allocations before → after | Upload before → after | Scope before → after µs | Repeated >5% frame regression |
|---|---:|---:|---:|---:|---:|---:|---|
| chat-visible | -0.028% | -0.609% | -4.698% | 85404.583 → 227034.950 | 0.000 → 0.000 | 171.257 → 471.755 | None |
| gameplay | -0.052% | 0.444% | 0.734% | 12674.967 → 12892.917 | 0.000 → 0.000 | 88.438 → 88.466 | None |
| hud-placement | -0.006% | -0.563% | -0.723% | 56728.150 → 64727.900 | 0.000 → 0.000 | 636.126 → 687.788 | None |
