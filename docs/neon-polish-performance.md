# Neon polish performance — 10 November 2026

This compares the previously installed compact B build `0c38f7b5c63d9682c199d6b3adcf8ea6193528da29548a4eadbaa60adcc5923f` with the delivered build `bd6f55a02ae591239b77ad3b4f26c9c0e6885765121caeaa999652dc0a1a2cbf`. Both use the same production harness, world fixture, camera, equipment, 1280×800 resolution, GUI scale 2, mod stack, 120 FPS cap, VSync off, render/simulation distances, and blur strength 5. Existing user clients remain open. Each of seven scenarios has three runs with 240 warm-up and 480 measured frames: 10,080 measured frames per version. No build or second verification client ran during collection.

No median, p95, or p99 metric regressed above 5% in two paired runs. HUD-placement allocation fell from 16,268 to 14,382 bytes/frame (11.6%); measured Neon scope time fell from 109.1 to 57.2 microseconds/frame (47.6%). The changing-label font probe fell from 442.0 to 351.5 microseconds/frame (20.5%), with identical upload volume and pixel output. Menu effects have a measured cost: idle-menu scope time rose by about 0.060 ms/frame, and navigation by about 0.070 ms/frame. Navigation allocations rose with the additional directly exposed controls. These are measured tradeoffs, not a claim that every path became cheaper. Frame measurements include the limiter and are not an uncapped FPS claim.

An intermediate build exposed high allocation in the synthetic font probe (approximately 761 KB/frame). BufferedImage.getRGB can allocate a temporary raster-element array per sample; reading the same packed ARGB buffer directly removed that path. The final probe measured approximately 132 KB/frame. Comparing 336 text runs against the preceding checked renderer confirmed every texel, dimension, and bearing matched. Intermediate data is retained as `runtime/neon-perf-before-raster-fix.csv`; it is superseded by the final run below.

Evidence: `runtime/neon-polish-perf-before.log`, `runtime/neon-polish-perf-after.log`, `runtime/smoke/game/neon-perf-polish-before.csv`, `neon-perf-polish-after.csv`, their adjacent loaded-JAR hash records, and `runtime/neon-raster-equivalence.log`. Reproduce the table with `tools/analyze-quality.ps1`. Instrumentation exists only in the verification mod.

# Neon quality measurements

Three repeated runs per version; 240 warm-up frames then 480 sampled frames per scenario. Percentiles use nearest rank. Frame times include the configured frame limiter. CPU is sampled with ThreadMXBean (coarse Windows thread-clock resolution); scope time is the monotonic wall time inside the same Neon scopes. Allocations and rendering counters cover instrumented Neon work, not the entire client.

| Version | Scenario | Run | Median ms | p95 ms | p99 ms | CPU µs/frame | Scope µs/frame | Bytes/frame | Upload bytes/frame | Copy pixels/frame | Passes/frame |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Before | color-edit | 1 | 8.336 | 9.053 | 9.623 | 292.969 | 328.578 | 68201.117 | 0.000 | 0.000 | 0.000 |
| Before | color-edit | 2 | 8.336 | 9.045 | 9.513 | 97.656 | 295.850 | 64233.033 | 0.000 | 0.000 | 0.000 |
| Before | color-edit | 3 | 8.336 | 8.999 | 9.485 | 195.313 | 295.348 | 63640.850 | 0.000 | 0.000 | 0.000 |
| Before | font-churn | 1 | 8.337 | 9.123 | 9.577 | 488.281 | 522.281 | 128072.350 | 14841.275 | 0.000 | 0.000 |
| Before | font-churn | 2 | 8.335 | 8.992 | 9.529 | 292.969 | 436.507 | 135328.800 | 15289.508 | 0.000 | 0.000 |
| Before | font-churn | 3 | 8.336 | 9.022 | 9.364 | 162.760 | 442.027 | 139421.850 | 15528.358 | 0.000 | 0.000 |
| Before | gameplay | 1 | 8.341 | 10.000 | 10.460 | 65.104 | 51.746 | 8393.817 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 2 | 8.335 | 9.915 | 10.535 | 0.000 | 38.628 | 8304.000 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 3 | 8.336 | 9.749 | 10.158 | 0.000 | 33.842 | 8304.000 | 0.000 | 0.000 | 0.000 |
| Before | hud-placement | 1 | 8.336 | 9.793 | 10.200 | 65.104 | 132.467 | 16893.583 | 0.000 | 0.000 | 0.000 |
| Before | hud-placement | 2 | 8.337 | 9.699 | 10.143 | 32.552 | 108.213 | 16268.350 | 0.000 | 0.000 | 0.000 |
| Before | hud-placement | 3 | 8.334 | 9.772 | 10.146 | 32.552 | 109.102 | 16192.900 | 0.000 | 0.000 | 0.000 |
| Before | idle-menu | 1 | 8.339 | 9.072 | 9.748 | 130.208 | 203.996 | 25319.600 | 0.000 | 0.000 | 0.000 |
| Before | idle-menu | 2 | 8.335 | 9.090 | 9.480 | 97.656 | 156.633 | 24435.133 | 0.000 | 0.000 | 0.000 |
| Before | idle-menu | 3 | 8.335 | 8.983 | 9.613 | 65.104 | 153.400 | 24176.000 | 0.000 | 0.000 | 0.000 |
| Before | motion-blur | 1 | 8.335 | 9.940 | 10.674 | 65.104 | 102.631 | 10008.000 | 0.000 | 1024000.000 | 1.000 |
| Before | motion-blur | 2 | 8.336 | 9.802 | 10.401 | 65.104 | 88.351 | 9920.000 | 0.000 | 1024000.000 | 1.000 |
| Before | motion-blur | 3 | 8.337 | 9.758 | 10.171 | 97.656 | 90.574 | 9920.000 | 0.000 | 1024000.000 | 1.000 |
| Before | navigation-scroll | 1 | 8.341 | 9.373 | 10.542 | 162.760 | 237.428 | 31005.967 | 0.000 | 0.000 | 0.000 |
| Before | navigation-scroll | 2 | 8.335 | 9.037 | 9.541 | 65.104 | 177.602 | 30406.783 | 0.000 | 0.000 | 0.000 |
| Before | navigation-scroll | 3 | 8.335 | 8.985 | 9.292 | 195.313 | 183.836 | 30328.000 | 0.000 | 0.000 | 0.000 |
| After | color-edit | 1 | 8.336 | 9.029 | 9.504 | 227.865 | 322.712 | 62877.683 | 0.000 | 0.000 | 0.000 |
| After | color-edit | 2 | 8.335 | 9.005 | 9.459 | 195.313 | 304.552 | 59342.367 | 0.000 | 0.000 | 0.000 |
| After | color-edit | 3 | 8.337 | 8.998 | 9.512 | 227.865 | 306.077 | 58262.850 | 0.000 | 0.000 | 0.000 |
| After | font-churn | 1 | 8.335 | 9.041 | 9.518 | 325.521 | 396.456 | 124392.233 | 14841.275 | 0.000 | 0.000 |
| After | font-churn | 2 | 8.335 | 9.039 | 9.426 | 130.208 | 334.412 | 131904.800 | 15289.508 | 0.000 | 0.000 |
| After | font-churn | 3 | 8.335 | 9.010 | 9.361 | 227.865 | 351.546 | 135974.250 | 15528.358 | 0.000 | 0.000 |
| After | gameplay | 1 | 8.339 | 9.967 | 10.453 | 32.552 | 46.110 | 8393.667 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 2 | 8.335 | 9.793 | 10.387 | 0.000 | 35.014 | 8304.000 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 3 | 8.337 | 9.583 | 10.213 | 0.000 | 29.208 | 8304.000 | 0.000 | 0.000 | 0.000 |
| After | hud-placement | 1 | 8.337 | 9.748 | 10.275 | 97.656 | 77.056 | 15019.600 | 0.000 | 0.000 | 0.000 |
| After | hud-placement | 2 | 8.334 | 9.567 | 10.016 | 0.000 | 57.213 | 14381.900 | 0.000 | 0.000 | 0.000 |
| After | hud-placement | 3 | 8.336 | 9.599 | 10.089 | 0.000 | 53.767 | 14381.900 | 0.000 | 0.000 | 0.000 |
| After | idle-menu | 1 | 8.336 | 9.039 | 9.597 | 162.760 | 240.678 | 26157.400 | 0.000 | 0.000 | 0.000 |
| After | idle-menu | 2 | 8.338 | 9.114 | 9.524 | 130.208 | 216.575 | 24986.467 | 0.000 | 0.000 | 0.000 |
| After | idle-menu | 3 | 8.335 | 9.036 | 9.461 | 292.969 | 188.434 | 24792.000 | 0.000 | 0.000 | 0.000 |
| After | motion-blur | 1 | 8.339 | 9.748 | 10.285 | 97.656 | 91.255 | 10008.117 | 0.000 | 1024000.000 | 1.000 |
| After | motion-blur | 2 | 8.334 | 9.904 | 10.411 | 0.000 | 84.114 | 9920.000 | 0.000 | 1024000.000 | 1.000 |
| After | motion-blur | 3 | 8.338 | 9.650 | 9.932 | 65.104 | 73.641 | 9920.000 | 0.000 | 1024000.000 | 1.000 |
| After | navigation-scroll | 1 | 8.337 | 9.014 | 9.664 | 162.760 | 265.626 | 38737.117 | 0.000 | 0.000 | 0.000 |
| After | navigation-scroll | 2 | 8.335 | 9.156 | 9.546 | 195.313 | 253.820 | 37973.700 | 0.000 | 0.000 | 0.000 |
| After | navigation-scroll | 3 | 8.336 | 9.022 | 9.452 | 195.313 | 235.599 | 37893.200 | 0.000 | 0.000 | 0.000 |

## Comparison

Values below use the median of the three per-run results. A repeated regression means an increase above 5% in at least two paired runs; investigate it before delivery.

| Scenario | Median Δ | p95 Δ | p99 Δ | Allocations before → after | Upload before → after | Scope before → after µs | Repeated >5% frame regression |
|---|---:|---:|---:|---:|---:|---:|---|
| color-edit | 0.004% | -0.440% | -0.098% | 64233.033 → 59342.367 | 0.000 → 0.000 | 295.850 → 306.077 | None |
| font-churn | -0.011% | 0.187% | -1.081% | 135328.800 → 131904.800 | 15289.508 → 15289.508 | 442.027 → 351.546 | None |
| gameplay | 0.008% | -1.236% | -0.694% | 8304.000 → 8304.000 | 0.000 → 0.000 | 38.628 → 35.014 | None |
| hud-placement | -0.001% | -1.774% | -0.559% | 16268.350 → 14381.900 | 0.000 → 0.000 | 109.102 → 57.213 | None |
| idle-menu | 0.011% | -0.369% | -0.931% | 24435.133 → 24986.467 | 0.000 → 0.000 | 156.633 → 216.575 | None |
| motion-blur | 0.025% | -0.549% | -1.115% | 9920.000 → 9920.000 | 0.000 → 0.000 | 90.574 → 84.114 | None |
| navigation-scroll | 0.002% | -0.167% | 0.045% | 30406.783 → 37973.700 | 0.000 → 0.000 | 183.836 → 253.820 | None |
