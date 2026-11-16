# Neon 0.5.0 verification

## Scope

The expansion adds fifteen modules and twenty information widgets after improving the original Armor/Totems/FPS presentation. There are 22 modules and 23 HUD elements. Each feature received an actual Gradle build and focused packaged-client checks before the next addition. [Research and source boundaries](client-research.md) document the representative client survey.

The user's first test identified crosshair alignment and HUD typography as unacceptable. The follow-up adapts Unity revision `5bbd639`'s crosshair raster and bitmap origin snapping, plus its physical text sizing and shadow ordering. HUD information now defaults to native-resolution Inter, with a Minecraft-font option, compact measured panels and readable idle keystrokes. Scoreboard styles and custom font/obfuscated runs retain their appropriate renderer. Unity's checkout and installed artifacts were not changed.

## Functional and visual checks

- 134 unit tests: existing input/config/persistence coverage plus new module migration, settings bounds, trailing-second physical clicks, session duration, negative coordinate conversion, compass wrap, speed discontinuities, world time and crosshair coverage centroids.
- Actual crosshair framebuffer centroids on all four odd/even width/height combinations, GUI scales 1–4, and one-/two-pixel strokes. Coverage is symmetric within 0.05 display pixels. The scale-4 fixture uses a 960/961-pixel-high window so Minecraft actually selects scale 4.
- Every added module exercises its real input/render hook, all three bind modes, enable/bind Undo, profiles, visible right-click access, and reachable settings at default/minimum menu sizes. Assertions cover original poses/options/state restoration, overlay pixels, time/weather isolation, darkness/blindness and native block targets.
- Every new HUD exercises real data, toggling, profile persistence, appearance/font Undo, direct drag/gesture Undo, enabled-only hit testing, small displays, keyboard reveal and hide/show scroll retention. Scoreboard checks preserve native fallback, hidden entries, team formatting and custom number formats.
- The packaged release gate also includes existing premium interactions, color/clipboard behavior, FPS/resource-pack changes, Better chat, Better screenshot and all module/Settings/color pages. Screenshots are actual client output, including GPU-readback assertions; they are not mockups.

All 15 new module drawers were visually inspected at default and minimum sizes. New HUD captures were inspected at 1280×800 and 640×360. Inter views were reinspected after the user's typography correction. Native item models/decorations remain unchanged. Theme palette/alpha, FPS overrides, saved placements, three categories and the direct HUD action remain.

The initial release gate exposed an obsolete four-row HUD assertion; it now checks the complete compact scrolling catalog. A subsequent combined run exceeded the old 6,000-tick global timeout after reaching the added suites. The gate now allows 18,000 ticks without removing assertions. The final build, including native resource-font/obfuscation fallback and legacy formatting, passed **17,905 packaged checks** and installed exact verified bytes into Vanilla on 16 November 2026. The full run took 6m54s. See the [machine-readable receipt](expansion-receipt.json).

## Measurements and final artifact

- Runnable `neon-0.5.0.jar`: SHA-256 `fc49369cc30a4e343547cd4b11c1e1e0e492f133eb9f5245d884d93078117ea1`.
- Sources `neon-0.5.0-sources.jar`: SHA-256 `ca66d736e011bf5a45e61e56330004c977f9ddbc346bb3fb777848cfbc1b0739`.
- Verification harness: SHA-256 `deed9df601deec35d23366cea4703c35926d3a8d75cceb12638c3aa89c43a99f`.
- Installed Vanilla `mods/neon.jar` matches the runnable hash. Running user clients were left open; the new code loads on their next restart.

The runnable archive contains version 0.5.0, Inter font assets, their OFL license, LICENSE and NOTICE. It contains no smoke harness, profiling classes or audit mixins. All sound/clipboard interception and profiling remain in `src/smoke`, outside the runnable JAR. Tests are muted, preserve user clipboard contents, and use an isolated world. No FPS increase is claimed.

### Measured costs

Three runs per scenario, 240 warm-up frames and 480 measured frames per run. The table uses the median of the three per-run results. Baseline is the frozen 0.4.0 JAR (`19f5c063efb46fe5bdb849530fb1fb54ccce58a732e1d0b6e834a86be142abc2`); after is the exact release hash above.

| Build/scenario | Frame median ms | p95 ms | p99 ms | Instrumented scope µs/frame | Instrumented bytes/frame |
| --- | ---: | ---: | ---: | ---: | ---: |
| 0.4.0 gameplay | 8.337 | 9.935 | 10.896 | 51.474 | 13,098.667 |
| 0.5.0 gameplay | 8.333 | 9.885 | 11.049 | 122.959 | 9,435.767 |
| 0.4.0 idle menu | 8.334 | 9.076 | 9.674 | 289.560 | 44,312.067 |
| 0.5.0 idle menu | 8.334 | 9.095 | 9.760 | 384.369 | 74,601.733 |
| 0.5.0 HUD catalog enabled | 8.335 | 9.867 | 10.507 | 953.581 | 47,809.450 |

The expanded presentation costs more measured scope time, and the menu allocates about 30 KB more per frame in this workload. Default gameplay allocations decreased in the instrumented scopes. None of these observations establish an FPS gain. The frame limiter dominates the median; the three short comparisons did not trigger the report's repeated >5% frame-percentile threshold, but this is not an uncapped benchmark or a general performance guarantee.

The HUD-catalog scenario enables all twenty additions, FPS background/border and the existing equipment HUD. Empty potion/scoreboard data correctly draws nothing, so it is not a claim that all 23 elements are simultaneously populated. New modules remain disabled in these scenarios. Module behavior is covered by the functional suite rather than a per-module performance comparison.

Environment: Windows, Ryzen 9 5950X, RTX 2060 (driver 32.0.16.1742), Java 25, 1280×800, GUI scale 2, 120 FPS cap, VSync off, render/simulation distances 6/5, 1536 MiB heap. Both versions used the same isolated fixed-world workload and installed compatibility-mod set, including Sodium, Iris and ImmediatelyFast. All samples report a focused, non-iconified verification window. Other user games remained open, so shared-machine scheduling and GPU contention limit tail comparisons.

Allocation/scope counters cover instrumented Neon extraction, UI, camera and input work—not the whole client or every added hook. Deferred font uploads and native GPU work can occur outside these scopes; zero scoped upload counters do not mean zero total uploads or GPU cost. Windows thread CPU-clock quantization produces zero/coarse samples; monotonic scope time is more informative for these short sections.

Retained evidence: [per-run comparison](expansion-measurements.md), [comparison JSON](expansion-measurements.json), [HUD JSON](expansion-hud-measurements.json), and [all 7,200 raw frame samples plus environment manifest](expansion-samples.zip).

## Reproduce

Use JDK 25 and the process-local socket fallback from `tools/build.ps1` when needed on Windows. The isolated world must exist as `runtime/smoke/game/saves/Neon Verification`; never point verification at a user's Prism instance.

```powershell
./tools/build.ps1 -SkipInstall
./gradlew.bat prodSmoke -PmodulesOnly --console=plain
./gradlew.bat prodSmoke -PexpansionOnly --console=plain
./gradlew.bat prodSmoke -PfpsOnly -PfpsFocused --console=plain
./tools/build.ps1
```

The installing build runs `releaseSmoke`, checks hashes of both the runnable JAR and harness at start/end, validates its receipt, stages a byte-identical copy and replaces Vanilla's stable `mods/neon.jar` entry. Failed verification never installs an older artifact. The user's explicit earlier quick-install request skipped that gate for their interim test build; it is not represented as a verified release.
