# Neon 0.5.6 improvement results — 17 November 2026

The four follow-ups are complete: chat allocation work, compact search, expanded feasible native/endurance verification, and consolidated design/delivery documentation. **Neon 0.5.6 is installed in PrismLauncher Vanilla**, with its installed bytes matching the exact packaged JAR that passed the release gate. Physical default-key and mixed-DPI coverage limits are explicit below.

## Chat work and measurements

Consecutive duplicate grouping now retains native wrapped lines for unchanged groups instead of rewrapping the whole history on each received message. Reflow still invalidates the cache for width/font/filter/settings/deletion changes. Original history, source, signatures and tags remain untouched; the display uses the latest message's metadata. Scrolled history retains its native-line anchor where available. Clickable style spans reuse immutable prepared layouts at unchanged origins. Deferred glyph rendering uses packed append-only buffers, preserving order across atlas/style changes without allocating an object per glyph.

The same verification harness compared the original 0.5.5 JAR with the frozen chat-optimized intermediate JAR before search was added. Three runs per scenario used 240 warm-up and 480 sampled frames at a 120 FPS limit, on OpenGL/NVIDIA RTX 2060 with the existing Sodium/Iris/ImmediatelyFast compatibility set. All 5,760 samples per version were focused; none was iconified. These were sequential comparisons before the later simultaneous endurance/release runs.

Medians of the three per-run results:

| Scenario | Instrumented bytes/frame, before → after | Scoped µs/frame, before → after | Frame median ms, before → after | p95 ms, before → after | p99 ms, before → after |
| --- | ---: | ---: | ---: | ---: | ---: |
| Visible chat | 226,292.817 → 157,767.150 | 499.079 → 473.871 | 8.333 → 8.340 | 9.834 → 9.809 | 10.703 → 10.943 |
| Docked Chat/Coordinates | 121,102.517 → 87,070.500 | 314.895 → 308.267 | 8.333 → 8.338 | 9.829 → 9.806 | 11.216 → 10.282 |
| Receiving chat | 453,761.867 → 132,379.717 | 682.209 → 465.338 | 8.332 → 8.337 | 9.779 → 9.707 | 11.316 → 10.276 |
| Gameplay control | 12,429.217 → 12,515.967 | 90.030 → 91.154 | 8.333 → 8.337 | 10.025 → 9.870 | 10.798 → 10.593 |

Instrumented allocations fell about **30% for visible chat, 28% for docked chat and 71% while receiving messages**. No median/p95/p99 increased over 5% in two paired runs. The frame limiter dominates medians, and visible-chat p99 rose in the median-of-runs summary. This is short, shared-machine evidence for allocation/scoped-work reductions, **not an FPS increase** or a whole-client/GPU benchmark. Windows CPU thread-clock readings are coarse. The optimization measurements apply to the frozen intermediate artifact, not an unmeasured claim about every final-build scenario.

See [all per-run measurements](improvements-measurements.md), [statistics](improvements-measurements.json), [raw before samples](evidence/improvements/chat-before.csv), [raw after samples](evidence/improvements/chat-after.csv) and [artifact identities](evidence/improvements/artifacts.json). Profiling remains verification-only.

The focused chat suite passed **18,666 checks**, including 125 accepted signed/linked messages, native 100-message/line limits, unchanged-line reuse, duplicate counts, filter/width rebuilds, scroll anchors, unchanged/moved clickable layouts and silent redraw. The Unity chat/HUD polish suite passed **439 checks**, including rendered mixed styles and link behavior.

## Search and interaction coverage

Search filters the current category's module/Settings grid or the HUD column, including disabled widgets. It is temporary view state, never configuration/profile/Undo state. Following the user's final refinement, search starts hidden with no reserved row or hidden input target. Ctrl+F reveals and focuses it, Enter/Down reveals a result without activation, and Escape clears then hides the empty field, reclaiming its space. Clearing restores scrolling. Visibility and queries survive drawer/category rebuilds and returning from HUD editing. Canvas selection clears a filter hiding the selected widget and reveals its fold; dragging does not.

- **486 focused search checks** passed at default/minimum menu sizes and 640×360, across GUI scales 1–3. They exercise actual screen character/key/pointer routes, hidden defaults, Ctrl+F reveal, Escape dismissal and restored list geometry, per-category visibility/query retention, silence, no configuration mutation, empty results, scroll restoration, text priority, result navigation, attached FPS colors, disabled-widget search, canvas selection, arrow nudging/Undo and parent return. [Focused result](evidence/improvements/hidden-search-result.txt).
- Before the final search-visibility refinement, **266 premium interaction checks** and **19,435 grid/border/color/page-layout checks** passed. Existing checks now identify the Profile name field explicitly and reveal offscreen targets before clicking, accommodating the new search field without dropping their assertions.
- Actual [default grid](images/neon-grid-default-collapsed.png), [default Settings](images/neon-grid-settings-default.png), [minimum grid with search hidden](images/neon-search-module-hidden-1.png), [small HUD with search hidden](images/neon-search-hud-hidden-2.png), [filtered module](images/neon-search-module-0.png), [minimum module](images/neon-search-module-1.png), [minimum Settings](images/neon-search-settings-1.png), [small-display module](images/neon-search-module-2.png) and [small HUD/color drawer](images/neon-search-hud-color-2.png) were inspected after the refinement. Controls fit their panes and remain reachable.

These are scripted in-client checks, distinct from the native desktop coverage below. Counts overlap the full release suite and should not be added as unique assertions.

The combined release run exposed a fragile existing Low fire screenshot fixture: its baseline showed flames, but the lowered capture had none to measure. The same production code passed focused reruns. Holding server/client burn state removed one possible race but did not resolve the combined-run failure. A module-then-refinement sequence then passed **997 checks**, recording strongly variable orange coverage across native animation frames (864–130,831 pixels in its lowered captures). [Frame observations](evidence/improvements/fire-frame-samples.txt).

The visual fixture now samples eight native frames at 20% lowering and asserts sufficient combined flame pixels plus a lower weighted centroid. Each sample must retain the burning state. Existing vertex checks still validate exact default 40% and zero transforms. This avoids assuming identical orange coverage in every animated sprite, while retaining a real framebuffer check that fails if flames disappear throughout the sample or do not move down. Temporary server state is restored afterward. The focused option runs Modules immediately before Refinement to cover their interaction without the full prelude. No production Low fire code changed. In the initial passing full release run before the search-visibility refinement, the weighted centroid moved from **588.07 to 667.44 display pixels**; [final frame observations](evidence/improvements/final-fire-frame-samples.txt) and actual baseline/lowered captures were inspected.

## Native desktop coverage and limits

The isolated desktop client retained Minecraft/Neon audio and clipboard interception. Optional voice chat was disabled, muted, set to zero volume and prevented from starting its local server. No user's game was closed or used for test input.

The desktop automation sent **Insert as GLFW key −1 / scan 256** and **Right Shift as Left Shift 340 / scan 42**. It therefore could not verify those physical default bindings. The [recorded default-key events](evidence/improvements/native-default-key-events.txt) and [status](evidence/improvements/native-default-status.json) show the limitation; the incomplete run failed rather than claiming coverage.

A separate native run with temporary **F6/F7 (295/296)** bindings passed the harness: 31 pointer events, 40 key/character events, two menu opens/closes, one focus loss/return, two chat-shortcut checks and exact **4.125×** numeric entry. Native observations included right-click settings, header dragging, text-entry priority and no deferred opening after leaving chat. [Final native status](evidence/improvements/native-fkeys-status.json). These bindings applied only to the disposable client. The native test did not establish wheel-scroll movement from the automation helper; scripted wheel/scroll coverage remains separate.

The [native PASS marker](evidence/improvements/native-result.txt) and [focused refinement result](evidence/improvements/refinement-result.txt) are retained. [Entry-by-entry JAR comparison](evidence/improvements/version-content-comparison.json) compares the native/endurance-tested artifact with the initial 0.5.6 build before the later Ctrl+F-only search refinement: only `fabric.mod.json` version metadata differed (310 entries each). The final search visibility change has separate focused and full release verification; the native/endurance runs were not repeated for it. The [follow-up JAR comparison](evidence/improvements/search-visibility-content-comparison.json) records the changed classes.

The [GLFW monitor inventory](evidence/improvements/native-monitors.json) reported one Generic PnP monitor at **1.0× / 100%**. Physical mixed-DPI monitor transitions remain unverified. GUI-scale/viewport checks are not substitutes. Physical Insert/Right Shift and a real mixed-DPI setup remain the specific manual/hardware coverage gaps; no product change is justified by the helper's incorrect key codes.

## Endurance

The isolated run passed **24,901 checks over 1,000 rendered menu/color/HUD/search/chat cycles and 19 resource reloads in 1,864.29 seconds (31.07 minutes)**. It varied viewport and GUI scale, performed live color edits plus exact Undo, filtered Settings, opened/returned from HUD editing, received grouped chat and closed immediately to gameplay. Font/chat cache bounds and absence of Neon audio clips passed throughout. Harness sound-request history was cleared each cycle so the observer itself did not accumulate requests.

The twenty post-requested-GC heap samples ranged from **438.42 to 443.58 MiB**; the first was **438.42 MiB**, the last **438.87 MiB**. This did not show sustained growth across this fixture, but a 31-minute run cannot establish leak freedom or all-day/multiplayer stability. Native and release checks shared the machine during portions of the run; this was endurance coverage, not a timing benchmark. [PASS result](evidence/improvements/stability-result.txt), [raw samples](evidence/improvements/stability.csv), [summary](evidence/improvements/stability-summary.json).

## Documentation and delivery

[The design contract](design-language.md), [repository instructions](../AGENTS.md) and [actual-client HTML reference](neon-design-reference.html) now express the current design once. Superseded floating-picker, static-column-border, three-widget-only, sharp-corner and centered-sidebar directions are retained only in [historical snapshots](history/README.md). The guide includes Fonts/search and the animated HUD outline. HTML nesting/assets and Markdown encoding were checked; the HTML was rendered and visually inspected.

The old 0.5.5 installation note is resolved: its guarded installer completed at 2026-11-17T13:32:25.3700542Z, and the installed hash matched at the start of this work. See [that historical result](chat-dock-results.md).

Actual Gradle builds passed after each completed code/configuration set. The final build passed **153 JUnit tests** (zero failures/errors/skips) and **23,497 packaged release checks**, then installed the checked runnable JAR through the guarded Gradle task. No artifact was installed after either failed release attempt. The passing full build took 8m 29s.

The built JAR, release-tested JAR and Vanilla's installed `neon.jar` all match SHA-256 **`ff8353a5a3ef239fec1962fb261a73e18aa693359a98335db8db8814894e6e41`**. The final verification harness is **`a38b26fbc19bda76b5513b696aa74eba49344d0341932bb01966d4ff89942f29`**. The release receipt completed at **2026-11-17T16:44:40.896301200Z**; the installed file was independently rechecked at **2026-11-17T16:45:03.9906939Z**. [Release receipt](evidence/improvements/release-verified.json), [installation identity](evidence/improvements/installed.json), [build result](evidence/improvements/final-build-result.txt), [unit results](evidence/improvements/unit-tests.json).

The final runnable JAR contains no verification/profiling/interception classes. Unity's separate checkout/artifacts and existing Minecraft clients were preserved. The installed code loads on the next Vanilla launch. The subsequent requested GitHub upload is available as [v0.5.6](https://github.com/chronicaly/neon/releases/tag/v0.5.6), with the same checked JAR, sources, checksums and receipt. Local raw logs and frozen comparison artifacts remain under `runtime/improvements/`.
