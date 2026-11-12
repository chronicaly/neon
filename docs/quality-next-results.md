# Quality follow-up — 12 November 2026

Implementation is in place for the six requested improvements, completed in sequence. Every completed code/configuration change set received an actual Gradle build. Scripted verification and installation passed. The remaining native checks were explicitly skipped at the user's request before publishing to GitHub; they are not reported as passing. Work used the Neon checkout and disposable worlds under `runtime/quality-next/`; Unity and running user Minecraft processes were preserved.

## Behavior

1. Scrollable content gently dissolves its last clipped sliver. The fade affects drawing only; scrolling, focus reveal and fixed hitboxes are unchanged. No scrollbars were added.
2. Rejected color Paste shows **Invalid** in the existing button, with a format explanation on hover/focus/narration. It preserves color, Undo and silence. It survives animated themes until a valid paste or direct color edit.
3. Checker cells share one render-state batch. Text rasterization reuses a bounded 4 MiB scratch image per rendering thread, cleared on resource reset. The output pixels and interaction timings are preserved.
4. Settings and profiles retain a `.bak` copy of the latest successfully saved snapshot. Corrupt originals are archived as `.invalid-*.json`. Recovery keeps healthy profile edits/order, repairs damaged entries individually and does not resurrect intentionally deleted profiles. Valid primary files always win. Recovery notices are local and silent; Saved waits for both primary and backup writes.
5. Installation requires the complete packaged release suite for the exact production JAR and harness. The client reports the hashes it actually loaded; Gradle validates them again against current outputs and staged installation bytes. Focused runs and benchmarks cannot produce an installation receipt.
6. Verification adds native desktop input observation, a cross-process Windows clipboard contention fixture and a rendered menu/color/HUD stability run with resource reloads. Native input mode keeps audio and clipboard interception active while allowing real window input and focus callbacks.

## Measurements

JFR identified checker rectangle/matrix objects and text raster buffers as allocation hotspots. JFR was used separately from timing runs. The initial comparison covered twelve scenarios, three repetitions each, 240 warm-up and 480 sampled frames per scenario. A follow-up repeated five scenarios with native focus/iconification recorded. Both used 1280×800, GUI scale 2, 120 FPS cap and VSync off. The user's other Minecraft client stayed open.

Median of the three follow-up runs; allocation counts cover instrumented Neon work, not the whole game:

| Scenario | Bytes/frame before → after | Reduction | Median frame ms before → after | p95 ms before → after | p99 ms before → after |
|---|---:|---:|---:|---:|---:|
| Color editor | 101,130 → 55,260 | 45.4% | 8.340 → 8.336 | 9.328 → 9.221 | 11.813 → 15.952 |
| Continuous color editing | 212,659 → 69,556 | 67.3% | 8.337 → 8.341 | 8.970 → 10.387 | 9.480 → 20.743 |
| Changing text | 136,113 → 30,355 | 77.7% | 8.340 → 8.339 | 9.103 → 11.188 | 10.279 → 15.614 |
| Gameplay control | 15,188 → 15,176 | 0.1% | 8.341 → 8.340 | 9.616 → 10.063 | 10.807 → 16.544 |
| Idle menu control | 44,330 → 44,314 | <0.1% | 8.338 → 8.338 | 8.911 → 8.994 | 9.207 → 9.722 |

Allocation reductions repeated across all three runs. Median frame times remained near the limiter's 8.34 ms target. Tail times worsened in several scenarios, including unchanged controls. The initial twelve-scenario run also had broadly slower after timings. In the follow-up, all 7,200 before samples had native focus; 3,003 after samples had focus and 4,197 did not. Neither run was iconified. Even the first fully focused after repetition had higher tails, so focus alone does not explain them. Shared-desktop contention prevents a clean attribution: **no FPS improvement or absence of frame-time regression is claimed**. An exclusive-machine timing run remains useful.

Raw samples are retained in [quality-next-samples.zip](quality-next-samples.zip); per-run results in [quality-next-measurements.json](quality-next-measurements.json) and [quality-next-initial-measurements.json](quality-next-initial-measurements.json). Before artifact SHA-256: `a5f0b55fe50ae5c55dde8e141ee13f2215530db6881defdecc4bf6f16bb3a83c`; after allocation-change artifact: `6dd63765e2e1bef710915e9065e706e23e709b71611ecd2f273d0162ab654338`.

## Verification and reproduction

The packaged release run passed **9,150 checks** against the production JAR and the verification harness identified in the [release receipt](quality-next-release.json). Initial installation was denied while Vanilla's JAR was loaded; temporary files were cleaned up and the user client stayed open. A subsequent `build -PskipInstall` succeeded. The checked deliverable is `runtime/quality-next/neon-0.3.0-verified.jar`, SHA-256 `f80c7bbfefebbb98eca5c2c951363bc4715ed656624b852204a5056756b92bc3`. The archive contains Java 25 classes and excludes the verification mod. Logs: `final-build-install.log` and `final-build.log`.

On resumption, the user client had exited. `build -x releaseSmoke` succeeded, running `verifyInstall` against the unchanged passing production/harness hashes before installation. Both build inputs were up to date. Vanilla's installed `mods/neon.jar` now matches the verified deliverable exactly (`f80c7bbfefebbb98eca5c2c951363bc4715ed656624b852204a5056756b92bc3`). No user client was closed by this work. Installation log: `runtime/quality-next/install-resumed.log`.

The stability run passed **2,401 checks**, **200 rendered cycles**, and **three resource reloads** in **371.455 seconds**. [Whole-client heap samples](quality-next-stability.csv) after cycles 50/100/150/200 were 324.9/325.3/326.3/324.2 MB. This short soak does not establish leak-free long-term operation.

Native desktop verification remains incomplete. Two disposable clients crashed in `glfw.dll+0x10fa1` during desktop activation/event polling; the cause remains unproven. A later session received native mouse movement, focus changes and ordinary keyboard input. Test-only key logging showed that the input tool sent Insert as key `-1`/scan `256`, and Right Shift as Left Shift (`340`/scan `42`). Configurable native test bindings were added to allow coverage with supported keys, but that run was stopped when the user requested skipping the remaining tests and publishing. There is no native PASS result. One monitor at 100% scale was reported; mixed-DPI movement remains unverified. Logs are retained locally under `runtime/quality-next/`.

The final native diagnostics and configurable-key harness compiled in successful Gradle builds after the release receipt was created. The production JAR is unchanged. The receipt deliberately retains the original tested harness hash; a future installing build must run the release suite again for the newer harness. No verification record was rewritten to claim an unperformed run. The Windows on-screen keyboard helper was closed by the user; its original click-sound setting was never changed.

- Unit suite: 119 tests, including exact text-raster reference pixels, config/profile recovery, backup durability and real Windows clipboard contention.
- Layout/interaction suites: 8,042 step-one checks; 7,739 step-two checks; 7,751 step-three checks, including twelve exact checker GPU framebuffer comparisons; 102 focused color checks after the animated-theme feedback correction.
- Recovery: two real damaged/repaired startup runs, ten checks each. Custom palette/alpha, independent FPS color and healthy profile edits survived. Original damaged files remained archived and recovery notices did not repeat on restart.
- Installation gate: 8,934 combined packaged checks in the first release run and 9,150 in the later run identified by the receipt. Receipt tests accepted a valid record and rejected missing, incomplete, wrong-production-hash and wrong-harness-hash records.
- Actual module/settings captures were inspected: [default module](images/neon-quality-next-module-default.png), [minimum module](images/neon-quality-next-module-minimum.png), [default settings](images/neon-quality-next-settings-default.png), [minimum settings](images/neon-quality-next-settings-minimum.png), and [small-display rejected Paste](images/neon-quality-next-invalid-paste.png).

Run from JDK 25 with the prepared disposable world:

```powershell
.\gradlew.bat build -PskipInstall --console=plain
.\gradlew.bat releaseSmoke -PneonVerificationDir=runtime/quality-next/game --console=plain
.\gradlew.bat prodSmoke -PstabilityOnly -PsmokeDir=runtime/quality-next/stability-game -PsmokeArtifactDir=runtime/quality-next/stability-artifact --console=plain
.\gradlew.bat prodSmoke -PdesktopOnly -PsmokeDir=runtime/quality-next/native-game -PsmokeArtifactDir=runtime/quality-next/native-artifact --console=plain
```

Native mode identifies its window as **Neon native verification — muted**. Its status/monitor JSON records native callback counts, menu transitions, focus, chat shortcut priority and exact numeric entry. Complete native actions before creating `neon-desktop-finish` inside that disposable directory; incomplete coverage fails. Never target the user's launcher client. Automated screen scenarios isolate text clipboard operations and intercept image copies. The separate contention test holds the real Windows clipboard in a helper process, proves the lock, checks retry exhaustion and unchanged sequence number, then releases it. The first, incorrect null-window fixture unexpectedly allowed a one-pixel test image write; the user was notified and the fixture was corrected before delivery.

Optional `-PdesktopPrimary=80 -PdesktopAlternate=79` selects P/O in the disposable native test client; the status and result report the actual tested key codes. Omitting these properties tests Insert/Right Shift. This does not alter the installed instance's bindings.

The local socket workaround and alternate output directory used here are recorded in `runtime/quality-next/build-output.gradle` and the step logs. No changes to Minecraft's audio settings are required for silent verification.
