# Client expansion work log

Engineering record for Neon 0.5.0, 16 November 2026. Final evidence and artifact identity are in [expansion results](expansion-results.md); product behavior is in the [README](../README.md).

## Delivery sequence

1. Surveyed official Lunar, Badlion, Feather and Laby documentation and inspected public Meteor HUD source. The [research record](client-research.md) identifies sources and boundaries. No proprietary or GPL implementation was incorporated.
2. Improved the original HUD with theme-linked plates, optional animated FPS borders, configurable outline animation and bounded geometry reuse. Preserved saved colors, alpha and placements.
3. Added twenty information widgets individually, each followed by an actual Gradle build and expanding packaged-client checks. The catalog now has 23 elements including Armor, Totems and FPS.
4. Added fifteen modules individually, with a real build after each change set: Crosshair, Toggle sprint, Toggle sneak, Dynamic FOV, Hurt camera, View bobbing, Low fire, Viewmodel, Enchant glint, Hit color, Time changer, Weather, Fullbright, Clean view and Block outline. There are 22 modules total. Additions start disabled and use the existing grid, drawers, profiles, Undo and header-only bindings.
5. Extended runtime checks to real input/render hooks, native-state restoration, all binding modes, profile persistence, gesture Undo, disabled hit testing, data accuracy and default/minimum layouts. Inspected all fifteen new module drawers and the final compact HUD captures.
6. Installed an interim build at the user's explicit request to skip tests. Its hash was `0999dc656d70a582a9a071affb36ef3b30d719f923eccf8a333ae8d2ef740841` with 0.4.0 metadata. This was a test handoff, not a verified release.
7. Addressed the user's crosshair-centering and HUD-quality feedback using actual Unity source. Adapted `CrosshairRaster` and `MinecraftFontLayout` from revision `5bbd639`, physical font sizing and shadow ordering. Added Inter/Minecraft choice, content-sized panels, readable idle keystrokes and native resource-font/obfuscation fallback. Unity's separate checkout and installed artifacts were preserved.
8. Verified crosshair coverage in 32 actual framebuffer configurations: odd/even dimensions, GUI scales 1–4, and one-/two-pixel strokes. The scale-4 fixture uses a 960/961-pixel-high display to avoid Minecraft capping the requested scale.
9. Completed 134 unit tests and the final **17,905-check** packaged release gate. The build installed the exact final 0.5.0 artifact into Vanilla; its receipt is retained in [expansion-receipt.json](expansion-receipt.json). User games remained open.
10. Rewrote the README, retained the detailed user guide, documented source credits, and measured the frozen 0.4.0 baseline against the exact final JAR. See the results page for data, limits and publication artifacts.

## Corrections made during verification

- The HUD column is capped at 300 logical units and remembers scroll through hide/show. Keyboard focus reveals offscreen settings without scrollbars.
- The old four-row FPS fixture now accounts for the full HUD catalog. The integrated release timeout increased from 6,000 to 18,000 ticks; assertions were retained.
- Camera fixtures explicitly supply nonzero native effect strength; potion fixtures clear their disposable client effect map. These corrected test setup rather than changing gameplay to accommodate tests.
- Crosshair rendering uses cached antialiased coverage at physical framebuffer center, including fractional thickness and outline. Native eligibility and attack indication remain.
- Legacy text formatting does not appear as literal section-sign glyphs. Scoreboard colors, emphasis, number formats and custom resource fonts retain their appropriate renderer.

## Reproduction and artifacts

Run `tools/build.ps1 -SkipInstall` for a non-installing build. Focused packaged suites use `gradlew.bat prodSmoke -PexpansionOnly` or `-PmodulesOnly`, with the Windows process-local Java socket fallback in the build script. `tools/build.ps1` performs the exact-JAR release gate and installation.

Local development logs are under ignored `runtime/expansion-*.log`, screenshots under `runtime/smoke/game/screenshots`, and the frozen baseline at `runtime/neon-expansion-baseline.jar`. All profiling and audio/clipboard interception remain in `src/smoke`; none enter the runnable JAR. The initial checkout was clean at `e372a34` (Neon 0.4.0).
