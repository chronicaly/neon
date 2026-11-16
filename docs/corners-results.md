# Neon 0.5.2 verification

The user supplied off-center coordinate and FPS captures, then requested the same smooth corners across the menu. The HUD now centers Client-font ink within each line box and shares that baseline across colored runs. FPS keeps a fixed label and four-digit field, with the number right-aligned. Cached metrics avoid rebuilding text layouts each frame. Bundled Inter keeps its menu metrics; other families use their own measured widths and a centered reference baseline.

The combined shell, attached drawers and quiet HUD column use six-display-pixel corners; module/Settings/action buttons and text fields use three-pixel corners. The HUD geometry tessellator supplies the shared fill/outline contour. Only the localized moving highlight emits the outer bloom. Existing `OutlineTravel` state preserves the physical edge position through expansion and reversal. Hitboxes and the interaction contract are unchanged.

The user also rejected the HUD editor and Settings styling. The editor now uses the saved menu panel alpha, native headings in the selected font, inset outlined light-glass root rows, matching cyan switches and right-aligned choice/numeric values. Its old extra transparency multiplier and stacked cyan fills are removed. A compact numeric track matches pointer mapping while retaining whole-row dragging and exact entry. Color rows, profile actions, navigation highlights and text fields use consistent control corners. The 188-unit column, quiet static border, direct placement, focus, Undo and attached color behavior remain.

Settings → Fonts enumerates locally installed families through the platform font API. Search and a live preview stay above the smoothly scrolling results in the existing drawer. Menu text and Client-mode HUD text share the selected family, with settings/profile persistence and Undo. Missing families use bundled Inter without replacing the saved name. Font changes invalidate recorded menu presentations before retiring old text atlases. The native Minecraft HUD renderer remains selectable. All four sidebar actions share left-aligned icon/text columns. Sidebar selection has a faint tint without a box; header/corner gestures clear text focus before pointer capture.

Chat clearance uses native input-field geometry and four physical pixels of padding. A 220 ms velocity-preserving motion lifts intersecting bottom widgets and their joined neighbors while their panels/content remain aligned. It reverses smoothly, retains all saved placement, leaves top widgets and attached offhand decorations stationary, and resets for editor previews/world changes.

The final transparency request adds an 84% surface-alpha factor to the combined menu/HUD-editor panel. Saved RGBA and Colors opacity still multiply that value; text and functional controls retain their contrast. Attached panes use one fill, so their alpha does not stack. The rounded-fill framebuffer fixture checks the expected composited opacity as well as antialiasing.

## Evidence

The release gate passed 143 unit tests and 19,911 packaged-client assertions; the non-installing Gradle build also completed successfully. Its [receipt](corners-receipt.json) binds this evidence to the exact production and verification JAR hashes. The automatic replacement encountered the running Vanilla client's Windows file lock. The verified JAR is staged for atomic replacement when that lock releases; the game was kept open.

- 143 unit tests, including actual Inter raster coverage centered at 12, 16, 20, 24 and 32 pixels, local-font enumeration/rasterization and family persistence/fallback.
- 30 focused installed-font checks with Papyrus: real mouse selection, silent search/unchanged selection, live family resolution, query retention, Undo, settings/profile serialization, minimum resizing with search focus, nested wheel scrolling and missing-font fallback. Actual default/minimum and 640×360 pages were inspected.
- 84 focused chat-lift checks across GUI scales 1–3 and a small display: intermediate opening/closing positions, rapid reversals, native input clearance, joined-panel spacing, stationary top FPS, exact restoration, unchanged settings and editor preview bounds.
- 577 focused FPS/HUD checks after the editor styling changes, retaining independent colors, picker behavior and persistence.
- 76 focused framebuffer checks: coordinate/FPS alignment at HUD scales 0.75, 1, 1.25 and 1.5 across GUI scales 1–3, plus clipped corner fill, multiple antialias coverage levels and continuity into straight outline edges.
- The module-grid portion checked 164 animation frames and 385,598 contour samples, following the new quarter-circles rather than the removed square corners. Pointer behavior, binds, drawer reversal, invisible resizing and HUD return/scroll state passed. Its duplicate page-audit tail was stopped; the final installation gate runs that complete audit independently.
- Actual default and minimum module/settings pages, the attached color drawer and HUD were inspected. See [rounded menu](images/neon-corners-menu.png), [minimum menu](images/neon-corners-minimum.png) and [centered HUD](images/neon-centered-hud.png).
- The expanded scope is shown in [Papyrus selected](images/neon-system-fonts.png), [minimum font drawer](images/neon-system-fonts-minimum.png), [themed HUD editor](images/neon-themed-editor.png) and [chat clearance](images/neon-chat-clearance.png).

All live verification uses the isolated prepared world. Minecraft and Neon test audio are intercepted without playback; clipboard writes remain intercepted. User games stay open. Profiling and audit mixins are excluded from the runnable JAR.

## Reproduce

```powershell
./tools/build.ps1 -SkipInstall
./gradlew.bat prodSmoke -PalignmentOnly --console=plain
./gradlew.bat prodSmoke -PfontOnly --console=plain
./gradlew.bat prodSmoke -PchatLiftOnly --console=plain
./tools/build.ps1
```

The installing build requires the full packaged release suite, verifies production/harness hashes and installs only those exact runnable bytes into PrismLauncher Vanilla. The new code loads on the user's next restart.

## Measured frame times and allocations

The comparison uses the frozen published 0.5.1 JAR (`cd2f788d2f5223fe49539db03432aaa08190a223e1973f2baac3a6408b031f6d`) and the 0.5.2 JAR (`77c904ba95376ea31cb4d80c1e478a583e406244568515286ec4c25828f36dab`). Each scenario has three runs per build, with 240 warm-up frames followed by 480 sampled frames. Values below are medians of the three per-run results.

| Build/scenario | Frame median ms | p95 ms | p99 ms | Instrumented scope µs/frame | Instrumented bytes/frame |
| --- | ---: | ---: | ---: | ---: | ---: |
| 0.5.1 gameplay | 8.340 | 10.166 | 11.759 | 104.488 | 12,452.583 |
| 0.5.2 gameplay | 8.337 | 10.241 | 12.017 | 112.848 | 12,419.417 |
| 0.5.1 idle menu | 8.336 | 9.207 | 9.798 | 418.087 | 81,997.200 |
| 0.5.2 idle menu | 8.338 | 9.544 | 12.012 | 767.524 | 86,761.300 |
| 0.5.1 hud catalog | 8.341 | 10.229 | 12.508 | 901.961 | 53,041.900 |
| 0.5.2 hud catalog | 8.341 | 10.086 | 12.425 | 928.484 | 55,163.200 |
| 0.5.1 menu resize | 8.335 | 9.213 | 9.752 | 485.783 | 177,069.033 |
| 0.5.2 menu resize | 8.338 | 9.195 | 11.669 | 725.167 | 177,443.483 |

The updated UI adds measured work: about 0.349 ms and 4.76 KB/frame in the idle menu, and 0.239 ms during resizing. HUD-catalog allocation rises about 2.12 KB/frame. In this first comparison, idle-menu p99 rises 22.6% and resizing p99 rises 19.7%, both crossing the repeated >5% threshold in two of three paired runs. The third updated run returns to roughly 9.7–9.9 ms p99 in both scenarios. These costs and tail changes are recorded rather than presented as an optimization. The 120 FPS limiter dominates frame medians; this is not evidence of an FPS increase.

A second comparison ran the same checked JARs in reverse order (0.5.2 first), with three runs each of idle-menu and menu-resize. Idle-menu median-of-run p99 was 9.876 versus 9.869 ms (+0.066%), with no repeated threshold breach. Resizing was 10.594 versus 9.771 ms (+8.420%) and still breached p99 in two paired runs. Its added instrumented work was 0.224 ms/frame with about 257 extra bytes/frame. The largest resize frame was 30.664 ms, with 7.133 ms in the reported render interval and 1.430 ms in instrumented Neon scopes; other 14–15 ms frames had only 0.56–0.75 ms of scoped Neon work. This shows substantial time outside the measured scopes, but does not identify its cause or exclude GPU costs. Resize tail regression remains a measured limitation; both comparisons are retained. See the [reverse-order report](corners-recheck-measurements.md) and [JSON](corners-recheck-measurements.json).

The same verification harness profiles both versions and now includes deferred HUD surface and menu fill/outline vertex construction. This fixes a scope gap in the older refinement report, so its scope values should not be compared directly with this report. Profiling remains outside the runnable JAR. Zero scoped upload/pass counters do not mean zero native or GPU work.

Environment: Windows, Ryzen 9 5950X, RTX 2060 (driver 32.0.16.1742), Java 25, 1280×800, GUI scale 2, VSync off, render/simulation distances 6/5 and 1536 MiB heap. Both versions use the same isolated world and compatibility-mod set. All 17,280 primary and reverse-order samples report a focused, non-iconified verification window. User games remained open, so shared CPU/GPU contention limits tail comparisons. Windows thread CPU-clock resolution is coarse. The HUD catalog enables the information and equipment widgets; empty potion/scoreboard data draws nothing. New visual modules remain disabled during profiling. Font selection and chat-animation transients have functional coverage but are not isolated by these four benchmark scenarios.

Evidence: [per-run comparison](corners-measurements.md), [JSON](corners-measurements.json), [raw samples and environment manifest](corners-samples.zip).
