# Neon 0.5.1 verification

## Changes and scope

This release responds to the user's report that Low fire did not work and the HUD and block outlines were poorly executed. Unity revision `5bbd639` supplies the selected-block silhouette mask, outline/fill/bloom shaders, final fire-quad transform and compact coordinate formatting. Neon adapts them to native target eligibility, voxel shapes, depth testing, settings, profiles and GPU cleanup. Source credits are in [NOTICE](../NOTICE). Unity's checkout and installed artifacts are preserved.

The shared glass HUD is implemented in Neon: small-radius surfaces merge across nearby gaps, a tessellated union avoids doubled alpha and internal strokes, and one framebuffer-space light field synchronizes all contours. Content-sized layouts use consistent padding and Inter typography. Color regions preserve local FPS overrides inside a joined surface. General adds master and independent background/outline visibility without rewriting local preferences.

## Checks

The Gradle build passes 139 unit tests. New tests cover nearby/separate unions, non-overlapping fill, interior holes, global setting migration/reset/copies, and Unity coordinate flooring/conversion. Existing tests cover input, configuration, Undo and persistence.

The focused packaged-client suite exercises actual framebuffer output at normal and small display sizes: visible joined contours, filled bridges, removed internal edges, separation, disabled-widget exclusion, each global visibility switch, independent FPS colors, and real HUD-row input/profile/Undo behavior. It renders all five block styles against an actual selected world block and verifies native fallback. Low fire is checked on both deferred textured quads and actual visible flame pixels; caller transforms and zero-lowering native geometry are preserved.

The complete release suite also covers premium interaction timings, color/clipboard behavior, resource-pack font fallback, FPS overrides, Better chat, Better screenshot, every module/Settings/color page, and the full HUD catalog. Verification runs in an isolated prepared world, suppresses Minecraft and Neon audio, and intercepts clipboard writes. No profiling, audit mixins or test harness enters the runnable JAR.

An initial release run was stopped when review found that a custom FPS color could interpolate into a neighboring merged panel. The geometry now partitions color regions while retaining one outside contour; framebuffer checks cover this boundary. No JAR from the interrupted run was installed.

Actual potion, held-item, inventory and FPS pages were visually inspected at 1280×800 and 640×360. The block-outline drawer and attached color controls were inspected at default/minimum menu sizes. Compact text, native item decorations and editor controls fit. The saved palette, alpha, placements, module bindings and menu interaction contract are retained. Representative evidence: [joined HUD](images/neon-glass-hud.png), [independent joined FPS colors](images/neon-glass-fps-colors.png), [block drawer](images/neon-glass-block-default.png), [minimum drawer](images/neon-glass-block-minimum.png).

The final build passed **18,519 packaged release checks** in **7m29s** on 16 November 2026 and installed its exact runnable JAR into PrismLauncher Vanilla. Actual fire pixels moved down from a 625.81-pixel to 691.96-pixel vertical centroid in the fixed fixture, while remaining visible. Both deferred fire quads also passed vertex-by-vertex checks. The [receipt](refinement-receipt.json) binds the full suite to the production and harness hashes.

Final artifact identities:

- Runnable and installed `mods/neon.jar`: `cd2f788d2f5223fe49539db03432aaa08190a223e1973f2baac3a6408b031f6d`.
- Sources: `064ac02f0ef135be477eccb9ba84266c8739c68e35c997fceb975fdb73557987`.
- Verification harness: `532802edd3f201754c5e9b08dddbe3cb7bf832171452bfce4bbbba472dde96b5`.

The runnable archive contains version 0.5.1, both Inter font assets, their license, LICENSE, NOTICE and the block shaders, with no test/profiling classes. The Unity checkout remains clean. Existing user Minecraft processes were not closed.

## Reproduce

```powershell
./tools/build.ps1 -SkipInstall
./gradlew.bat prodSmoke -PrefinementOnly --console=plain
./tools/build.ps1
```

The installing build runs the full `releaseSmoke` suite and checks production/harness SHA-256 identities before, during and after execution. It installs only identical checked bytes into PrismLauncher's Vanilla instance. User Minecraft processes remain open and load the update on their next restart.

## Measured frame times and allocations

Three runs per build/scenario, each with 240 warm-up frames and 480 sampled frames. Values are medians of the three per-run results. Baseline is the frozen published 0.5.0 JAR (`fc49369cc30a4e343547cd4b11c1e1e0e492f133eb9f5245d884d93078117ea1`). The measured 0.5.1 JAR is `cd2f788d2f5223fe49539db03432aaa08190a223e1973f2baac3a6408b031f6d`.

| Build/scenario | Frame median ms | p95 ms | p99 ms | Instrumented scope µs/frame | Instrumented bytes/frame |
| --- | ---: | ---: | ---: | ---: | ---: |
| 0.5.0 gameplay | 8.334 | 10.114 | 11.369 | 121.995 | 9,403.583 |
| 0.5.1 gameplay | 8.336 | 10.016 | 10.987 | 74.143 | 12,434.717 |
| 0.5.0 idle menu | 8.334 | 9.102 | 9.635 | 392.754 | 79,047.600 |
| 0.5.1 idle menu | 8.335 | 8.973 | 9.470 | 345.294 | 82,158.600 |
| 0.5.0 HUD catalog | 8.334 | 9.896 | 10.581 | 945.790 | 47,217.150 |
| 0.5.1 HUD catalog | 8.336 | 9.802 | 10.657 | 242.248 | 53,297.333 |

Measured allocations increase by about 3 KB/frame in gameplay/menu and 6 KB/frame with the HUD catalog. Scoped time decreases, but shared mesh vertex construction happens in deferred GUI rendering outside these scopes; this does not prove lower total HUD CPU cost. The frame limiter dominates medians. The comparison does not trigger the repeated >5% frame-percentile threshold, and no FPS increase is claimed.

The HUD catalog enables all twenty additions, FPS background/border and the equipment HUD. Empty potion/scoreboard data correctly draws nothing. New visual modules remain disabled during these measurements, so this is not a per-module GPU-cost benchmark. Block shader and fire behavior are covered by functional framebuffer checks.

Environment: Windows, Ryzen 9 5950X, RTX 2060 (driver 32.0.16.1742), Java 25, 1280×800, GUI scale 2, 120 FPS cap, VSync off, render/simulation distances 6/5 and 1536 MiB heap. Both versions use the same isolated world and compatibility-mod set, including Sodium, Iris and ImmediatelyFast. All 8,640 samples report a focused, non-iconified verification window. User games stayed open; shared CPU/GPU contention limits tail comparisons. Windows thread CPU-clock resolution is coarse. Zero scoped upload/pass counters do not mean zero total native/GPU work.

Evidence: [per-run comparison](refinement-measurements.md), [JSON](refinement-measurements.json), [raw samples and environment manifest](refinement-samples.zip).
