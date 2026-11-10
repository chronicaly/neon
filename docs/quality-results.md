# Neon quality pass — 9 November 2026

The release preserves the four categories, compact sharp geometry, native-resolution Inter text, animated cyan/violet outline, P default, header movement, corner resizing, inline colors, and direct armor/totem editor. Scrollbars are removed throughout Neon. Wheel scrolling remains smooth and bounded, with immediate keyboard focus reveal.

Unity's inline color fold now uses its square HSV surface, hue/opacity strips, original/current previews and Copy/Paste. Left or right click opens/closes a row; edits apply live and remain applied when the fold closes. Color editing, navigation, settings changes, and disabling modules are silent. Only enabling a module plays the original quiet cue. Fresh/reset armor sits bottom-aligned immediately left of the offhand slot/hotbar; existing custom positions are retained.

Rendering changes include dirty atlas-region uploads, common-label warm-up, bounded page eviction between frames, cached outline geometry, per-frame armor/totem reuse, and alternating full-resolution motion-blur targets. Normal settings writes use immutable snapshots, one ordered writer, a 250 ms debounce, atomic replacement, failure reporting, retained unsaved state, and close/disconnect/shutdown flushing. Appearance profiles persist Adaptive/Full effects; Adaptive changes only menu blur and decorative glow.

## Verification

- 46 JUnit tests passed, including debounce/coalescing, ordered flushing, save failure/retry, compatibility, adaptive hysteresis, animation retargeting, color formats/opacity, armor defaults/migration, and font raster quality.
- The final packaged JAR passed **4,423 live checks**. Its actual SHA-256 is `27ccb941e01c20084154119b93a06ca393fd8ff6757842f9617e9a1eb4915d03`.
- 156 page captures cover six module/settings pages, basic/advanced states, all seven color folds and their final actions, at default/minimum window sizes and GUI scales 1–3. Geometry checks require complete labels, controls inside their column, the requested window size at capture time, and reachable final rows. GPU readback checks smooth text coverage and a continuous animated border; the density check compares actual rendered text/card pixels across GUI scales.
- Interaction checks cover visible-position hit testing, wheel motion, draft/focus preservation, unchanged widget identity through dragging/resizing, inline numeric validation/commit/cancel, Undo without rewriting unchanged bindings, long profile-name wrapping, live colors/opacity/Copy/Paste/Undo, silent color editing, one sound when enabling and none when disabling, nested keyboard focus reveal, adaptive downgrade/recovery/Full override, resource reload, atlas eviction, and twenty repeated menu openings.
- Gameplay checks cover camera/zoom cancellation and timing, autoclicker intervals and menu behavior, profile operations, native HUD placement/configuration, and GPU motion-blur blending, orientation, reset and cleanup behavior.
- The separate packaged persistent-autoclicker suite passed **49 checks**, including menus, text entry, exact intervals, focus changes, disconnect/rejoin, manual stopping and preserved pause settings. Evidence: `runtime/quality-release-clicker.log` and `runtime/clicker/game/neon-verification.txt`.

The successful final Gradle build installed `neon.jar` into PrismLauncher Vanilla using a staged atomic replacement, without truncating the JAR held by an existing client. The built, tested and installed files all match SHA-256 `27ccb941e01c20084154119b93a06ca393fd8ff6757842f9617e9a1eb4915d03`. Installation log: `runtime/quality-installed-build.log`. Existing Minecraft instances stayed open; Unity's separate checkout and artifacts were not changed. The new code loads on the next game launch.

Evidence: `runtime/quality-delivery-build.log`, `runtime/quality-delivery-live.log`, `runtime/quality-delivery-pass.txt`, and `runtime/smoke/game/screenshots/neon-quality-page-*.png`. Additional HUD, invalid-input, adaptive-tier and reload captures are in the same screenshots directory. Verification classes and instrumentation are excluded from the release JAR. The disposable test client blocks desktop pointer/keyboard callbacks while exercising the normal screen input methods with scripted coordinates/events; it does not establish every physical OS input path.

## Performance method

The pre-change runnable JAR was archived before implementation as `runtime/neon-quality-baseline.jar`, SHA-256 `478646c2d7e0b4a145cf99a5ddf35fcf48d4df532e4397bf255b1310687d1150`. Both versions were then replayed through the same final harness, which verifies the loaded JAR hash. A frozen JAR copy prevents builds from replacing a file used by a running test client.

Both runs use the same isolated world fixture, fixed camera, equipment/positions, noon/clear weather, 1280×800 resolution, GUI scale 2, 120 FPS cap, VSync off, render distance 6 and simulation distance 5, with the same optional mod stack. The host uses an NVIDIA GeForce RTX 2060 with OpenGL/NVIDIA driver 617.42. Existing gameplay clients remain open. Synthetic input refreshes Minecraft's activity timer to prevent AFK throttling.

After world warm-up, each scenario has 240 warm-up frames and 480 sampled frames, repeated three times: gameplay, motion blur, idle menu, navigation/scrolling, color editing, HUD placement, and a synthetic changing-label probe. The latter isolates atlas-upload overhead. There are 10,080 sampled frames per version. No builds or other verification clients run concurrently with performance collection.

The test-only instrumentation records frame times, render duration, client-thread CPU, monotonic scope time, allocations, texture-upload bytes, copied pixels and render passes. Scopes cover Neon menu extraction, outline vertices, HUD, camera, autoclicker ticks and settings gestures; background-writer CPU and unrelated client work are not attributed to Neon. Windows thread CPU clocks have coarse resolution, so aggregate CPU means accompany finer monotonic scope times. Frame times include the limiter; these results are not an uncapped FPS claim.

## Results

Using the median of three per-run measurements:

| Target | Before | After | Change |
|---|---:|---:|---:|
| Idle-menu allocations | 266,979 B/frame | 21,840 B/frame | −91.8% |
| Navigation/scroll allocations | 282,114 B/frame | 37,076 B/frame | −86.9% |
| HUD-placement allocations | 108,303 B/frame | 15,290 B/frame | −85.9% |
| Changing-label atlas uploads | 4,203,042 B/frame | 15,290 B/frame | −99.6% |
| Motion-blur copy work | 2,048,000 pixels/frame | 1,024,000 pixels/frame | −50%, same resolution/blending |
| Idle-menu scope time | 191.9 µs/frame | 129.7 µs/frame | −32.4% |

No scenario has a frame-time increase above 5% in two or more paired runs. Median frame times remain near the configured 120 FPS limit. The richer Unity color picker increases measured scope time from 217.8 to 287.3 µs/frame (about 0.07 ms), while reducing its allocations by 76.4%; its p99 frame-time change is +3.2%, below the regression threshold. This is a measured tradeoff, not a claim that every code path became cheaper.

Raw samples: `runtime/smoke/game/neon-perf-baseline-final.csv` and `neon-perf-release-final.csv`. Logs: `runtime/quality-baseline-final.log` and `runtime/quality-release-perf.log`. The adjacent `*-jar.txt` files record the loaded hashes. Earlier exploratory datasets are superseded by these final runs.

Reproduce analysis with `tools/analyze-quality.ps1 -Before runtime/smoke/game/neon-perf-baseline-final.csv -After runtime/smoke/game/neon-perf-release-final.csv`. The complete per-run median/p95/p99, CPU, allocation and rendering tables follow.

| Version | Scenario | Run | Median ms | p95 ms | p99 ms | CPU µs/frame | Scope µs/frame | Bytes/frame | Upload bytes/frame | Copy pixels/frame | Passes/frame |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Before | color-edit | 1 | 8.337 | 9.052 | 9.454 | 358.073 | 336.391 | 307004.617 | 218453.333 | 0.000 | 0.000 |
| Before | color-edit | 2 | 8.336 | 9.146 | 9.574 | 32.552 | 217.773 | 272621.217 | 0.000 | 0.000 | 0.000 |
| Before | color-edit | 3 | 8.336 | 9.031 | 9.538 | 227.865 | 209.089 | 272592.050 | 0.000 | 0.000 | 0.000 |
| Before | font-churn | 1 | 8.336 | 9.000 | 9.658 | 1106.771 | 1037.483 | 950345.050 | 4203042.133 | 0.000 | 0.000 |
| Before | font-churn | 2 | 8.336 | 9.229 | 9.675 | 1106.771 | 1288.838 | 1000772.767 | 4203042.133 | 0.000 | 0.000 |
| Before | font-churn | 3 | 8.335 | 9.103 | 9.536 | 1009.115 | 1072.088 | 1027856.883 | 4203042.133 | 0.000 | 0.000 |
| Before | gameplay | 1 | 8.335 | 10.223 | 10.727 | 0.000 | 58.549 | 8609.667 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 2 | 8.336 | 10.045 | 10.877 | 0.000 | 40.255 | 8384.000 | 0.000 | 0.000 | 0.000 |
| Before | gameplay | 3 | 8.337 | 10.201 | 11.405 | 0.000 | 40.003 | 8384.000 | 0.000 | 0.000 | 0.000 |
| Before | hud-placement | 1 | 8.334 | 9.880 | 10.728 | 32.552 | 139.109 | 108926.983 | 0.000 | 0.000 | 0.000 |
| Before | hud-placement | 2 | 8.337 | 10.106 | 10.637 | 32.552 | 130.980 | 108303.467 | 0.000 | 0.000 | 0.000 |
| Before | hud-placement | 3 | 8.339 | 9.771 | 10.532 | 0.000 | 106.006 | 108204.117 | 0.000 | 0.000 | 0.000 |
| Before | idle-menu | 1 | 8.337 | 9.187 | 9.862 | 292.969 | 249.050 | 268191.017 | 0.000 | 0.000 | 0.000 |
| Before | idle-menu | 2 | 8.336 | 9.090 | 9.770 | 130.208 | 191.874 | 266978.533 | 0.000 | 0.000 | 0.000 |
| Before | idle-menu | 3 | 8.335 | 9.130 | 9.596 | 65.104 | 191.852 | 266591.867 | 0.000 | 0.000 | 0.000 |
| Before | motion-blur | 1 | 8.335 | 10.213 | 10.682 | 32.552 | 121.807 | 10248.000 | 0.000 | 2048000.000 | 1.000 |
| Before | motion-blur | 2 | 8.334 | 9.706 | 9.979 | 130.208 | 98.786 | 10024.000 | 0.000 | 2048000.000 | 1.000 |
| Before | motion-blur | 3 | 8.338 | 10.011 | 10.562 | 130.208 | 114.097 | 10024.000 | 0.000 | 2048000.000 | 1.000 |
| Before | navigation-scroll | 1 | 8.335 | 9.018 | 9.586 | 227.865 | 252.744 | 282906.033 | 0.000 | 0.000 | 0.000 |
| Before | navigation-scroll | 2 | 8.337 | 9.167 | 9.576 | 162.760 | 235.229 | 282113.600 | 0.000 | 0.000 | 0.000 |
| Before | navigation-scroll | 3 | 8.337 | 9.039 | 9.549 | 130.208 | 220.554 | 282025.700 | 0.000 | 0.000 | 0.000 |
| After | color-edit | 1 | 8.337 | 9.043 | 9.841 | 195.313 | 287.317 | 67883.383 | 0.000 | 0.000 | 0.000 |
| After | color-edit | 2 | 8.337 | 9.213 | 9.984 | 130.208 | 348.693 | 64419.633 | 0.000 | 0.000 | 0.000 |
| After | color-edit | 3 | 8.336 | 8.999 | 9.409 | 162.760 | 264.309 | 63825.783 | 0.000 | 0.000 | 0.000 |
| After | font-churn | 1 | 8.337 | 9.043 | 9.715 | 520.833 | 667.230 | 708266.883 | 14841.275 | 0.000 | 0.000 |
| After | font-churn | 2 | 8.337 | 9.191 | 9.566 | 553.385 | 790.873 | 758028.800 | 15289.508 | 0.000 | 0.000 |
| After | font-churn | 3 | 8.335 | 9.069 | 9.564 | 553.385 | 601.601 | 785051.450 | 15528.358 | 0.000 | 0.000 |
| After | gameplay | 1 | 8.336 | 10.219 | 10.787 | 0.000 | 52.633 | 8458.000 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 2 | 8.338 | 9.722 | 10.086 | 0.000 | 34.408 | 8232.000 | 0.000 | 0.000 | 0.000 |
| After | gameplay | 3 | 8.337 | 9.815 | 10.324 | 0.000 | 34.678 | 8232.000 | 0.000 | 0.000 | 0.000 |
| After | hud-placement | 1 | 8.339 | 9.844 | 10.301 | 32.552 | 115.916 | 15953.550 | 0.000 | 0.000 | 0.000 |
| After | hud-placement | 2 | 8.342 | 10.196 | 11.599 | 32.552 | 129.915 | 15290.150 | 0.000 | 0.000 | 0.000 |
| After | hud-placement | 3 | 8.336 | 9.802 | 10.312 | 97.656 | 94.311 | 15214.900 | 0.000 | 0.000 | 0.000 |
| After | idle-menu | 1 | 8.335 | 9.009 | 9.425 | 130.208 | 148.870 | 23208.717 | 0.000 | 0.000 | 0.000 |
| After | idle-menu | 2 | 8.336 | 9.010 | 9.426 | 0.000 | 129.693 | 21839.933 | 0.000 | 0.000 | 0.000 |
| After | idle-menu | 3 | 8.336 | 8.982 | 9.318 | 130.208 | 115.306 | 21582.000 | 0.000 | 0.000 | 0.000 |
| After | motion-blur | 1 | 8.336 | 9.863 | 10.427 | 130.208 | 91.543 | 10096.000 | 0.000 | 1024000.000 | 1.000 |
| After | motion-blur | 2 | 8.337 | 9.973 | 10.420 | 65.104 | 91.088 | 9872.000 | 0.000 | 1024000.000 | 1.000 |
| After | motion-blur | 3 | 8.335 | 9.707 | 10.364 | 65.104 | 80.309 | 9872.000 | 0.000 | 1024000.000 | 1.000 |
| After | navigation-scroll | 1 | 8.336 | 9.072 | 9.533 | 97.656 | 177.307 | 37826.783 | 0.000 | 0.000 | 0.000 |
| After | navigation-scroll | 2 | 8.335 | 9.132 | 9.486 | 162.760 | 205.969 | 37076.000 | 0.000 | 0.000 | 0.000 |
| After | navigation-scroll | 3 | 8.335 | 9.046 | 9.483 | 97.656 | 164.554 | 36995.333 | 0.000 | 0.000 | 0.000 |

## Comparison

Values below use the median of the three per-run results. A repeated regression means an increase above 5% in at least two paired runs; investigate it before delivery.

| Scenario | Median Δ | p95 Δ | p99 Δ | Allocations before → after | Upload before → after | Scope before → after µs | Repeated >5% frame regression |
|---|---:|---:|---:|---:|---:|---:|---|
| color-edit | 0.012% | -0.099% | 3.168% | 272621.217 → 64419.633 | 0.000 → 0.000 | 217.773 → 287.317 | None |
| font-churn | 0.008% | -0.368% | -0.950% | 1000772.767 → 758028.800 | 4203042.133 → 15289.508 | 1072.088 → 667.230 | None |
| gameplay | 0.008% | -3.776% | -5.083% | 8384.000 → 8232.000 | 0.000 → 0.000 | 40.255 → 34.678 | None |
| hud-placement | 0.014% | -0.361% | -3.052% | 108303.467 → 15290.150 | 0.000 → 0.000 | 130.980 → 115.916 | None |
| idle-menu | 0.000% | -1.328% | -3.536% | 266978.533 → 21839.933 | 0.000 → 0.000 | 191.874 → 129.693 | None |
| motion-blur | 0.007% | -1.473% | -1.348% | 10024.000 → 9872.000 | 0.000 → 0.000 | 114.097 → 91.088 | None |
| navigation-scroll | -0.013% | 0.364% | -0.941% | 282113.600 → 37076.000 | 0.000 → 0.000 | 235.229 → 177.307 | None |
