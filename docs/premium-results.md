# Premium interaction measurements — 12 November 2026

The six interaction changes were built and checked sequentially. Measurements use packaged production JARs, with instrumentation confined to the verification mod. The starting JAR and the JAR after step 5 were frozen before optimizing presentation allocations.

## Method

Each version runs the same 12 scenarios three times: gameplay, motion blur, idle menu, navigation/scrolling, color editing, HUD placement, font churn, first openings with cleared font caches, repeated drawer reversals, category changes, continuous HSV dragging and menu resizing. Each scenario gets 240 warm-up frames and 480 sampled frames: 17,280 measured frames per version. The fixture uses the same world/camera, 1280×800 window, GUI scale 2, 120 FPS limit and disabled VSync. No concurrent builds or additional verification clients run during measurement. Existing user clients remain open.

Frame times include the limiter. Neon scope time is monotonic wall time within instrumented code; Windows thread CPU sampling is coarse. Allocation and upload counters cover instrumented Neon work, not the whole client. First-opening event samples are reported separately because their cost can disappear inside the overall percentile. Twelve cold-cache opening events per version are a small sample, not a startup-performance guarantee.

The starting-to-step-5 comparison also includes the separately developed Screenshot module in the shared checkout. The step-5-to-final comparison isolates this pass's measured optimization and final input cleanup. No camera or FPS smoothing was changed.

## Demonstrated cost and optimization

Step 5 increased menu allocations because every frame created recording buffers, copied both recorded command lists and intersected already-contained clip rectangles. The optimization reuses the recording buffers, makes immutable copies only when a transition begins or the menu closes, and reuses contained clipping rectangles. It retains a fresh native graphics extractor each frame so pointer and text-input state remain current. Outgoing content remains render-only.

## Results

Median-of-three allocation results (bytes per instrumented frame):

| Scenario | After step 5 | Final | Reduction |
|---|---:|---:|---:|
| Category changes | 54,898 | 37,146 | 32.3% |
| Drawer reversals | 116,751 | 79,141 | 32.2% |
| Color edits | 146,062 | 112,102 | 23.3% |
| Continuous color dragging | 245,920 | 211,960 | 13.8% |
| Idle menu | 51,522 | 43,909 | 14.8% |
| Menu resizing | 87,478 | 79,988 | 8.6% |

All 12 scenarios had no frame-median, p95 or p99 increase above 5% in two paired runs, against either starting or step-5 builds. Most frame medians stay around the 8.33 ms limiter interval. These results do **not** demonstrate an FPS increase. Texture-upload counts are unchanged for the matched scenarios. Instrumented drawer-reversal time decreased from 376.7 to 355.6 µs/frame versus step 5.

The extra presentation work still costs more than the starting client. Median scope time rose from 204.8 to 243.4 µs for category changes, 260.1 to 355.6 µs for drawer reversals, and 568.9 to 690.4 µs for discrete color edits. Repeated >5% per-run scope increases were investigated in category changes, both color scenarios, reversals, font churn, idle menu, resizing and scrolling. Command capture, clipping and outgoing replay explain new menu work; the recording/intersection allocation hotspot was reduced. Remaining median differences range from roughly 3 to 121 µs. Run-to-run variability also affects the unchanged font-churn control, so scope timings do not support a general execution-time improvement claim. Frame tails remained within the stated threshold. No gameplay optimization or speculative FPS claim was made.

Cold-font-cache opening events, 12 per version (nearest-rank p95 is the maximum for this small sample):

| Version | Event duration median / p95 ms | Neon scope median / p95 ms |
|---|---:|---:|
| Starting | 9.936 / 15.957 | 9.331 / 15.254 |
| After step 5 | 10.429 / 14.786 | 9.823 / 13.744 |
| Final | 10.819 / 14.636 | 10.147 / 13.418 |

The final cold-opening median is higher and its tail lower. Paired per-run event medians increased above 5% only in the third run versus the start (12.807→12.398, 8.378→8.365, 8.710→10.819 ms), so this sample does not establish a repeatable cold-opening regression or improvement.

Full median/p95/p99, CPU/scope, allocation, upload and render-counter tables: [step 5 versus final](premium-measurements.md), [starting versus final](premium-start-comparison.md). The associated JSON files contain per-run aggregates; [raw CSV samples](premium-samples.zip) retain all 51,840 sampled frames across those three versions.

| Artifact | SHA-256 |
|---|---|
| Starting | `c1bddc1cd5388641b3632ebb169a33a4b930b7907db4eb038a55eabc8c07b124` |
| After step 5 | `6a6387e09bb7ec7f5fd3114930fc3c7e8c29a443bfb0b04edb2585a05c89e1d6` |
| Final tested and installed | `bc3a768a7b5709d98eb1c5981bfbb77fa1bf78f60649bc1d3d38bf94bf35156a` |

## Reproduction

Build with `gradlew.bat build -PskipInstall --console=plain`. Run one frozen artifact at a time with `prodSmoke -PperfOnly -PperfLabel=LABEL -PperfBaselineJar=PATH -PperfExpectedHash=SHA256 -PsmokeDir=runtime/premium/perf-game -PsmokeArtifactDir=runtime/premium/perf-LABEL --console=plain`. The benchmark refuses an unexpected artifact hash. Compare its CSV files with `tools/analyze-quality.ps1 -Before BEFORE.csv -After AFTER.csv -Output REPORT.md`.

The isolated local build uses `-I runtime/premium/build-output.gradle` to avoid overwriting other work in the shared checkout. Logs, frozen JARs, raw CSVs and PASS markers are retained under `runtime/premium/`. Minecraft and Neon audio are intercepted; the final functional harness also uses an in-memory text clipboard and an intercepted image clipboard.
