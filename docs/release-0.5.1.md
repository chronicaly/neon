# Neon 0.5.1 — HUD and visual refinements

For Minecraft **26.2**, Java **25** and Fabric. Install the runnable `neon-0.5.1.jar` alongside Fabric API for 26.2, replacing the older Neon JAR.

- **Joined glass HUD:** tight padding, subtle rounded corners and a shared outside contour. Nearby enabled panels merge; separated panels split again. No doubled fill opacity or internal outline at overlaps. All widget outlines sample one synchronized cyan/violet animation.
- **Consistent typography:** FPS shares the same row height, padding and label/value styling while keeping its four independent colors, Sync and reserved four-digit width. Coordinates adapts Unity's comma-separated format and inline conversion. Potion effects, held items, inventory and counters fit their content.
- **Master visibility:** General → HUD surfaces turns backgrounds and outlines off together. Backgrounds and Outlines can also be controlled separately. Local widget choices, profiles, Undo and placements are preserved.
- **Unity-derived block outlines:** Outline, Both, Fill, Glow and Wire styles, width, fill opacity, Neon gradient, bloom and existing color/Sync. The silhouette shader uses native target eligibility and actual voxel shapes with depth testing. Disabling restores native selection.
- **Low fire fixed:** transform both final native fire quads, using Unity's percentage-to-height mapping. The default leaves visible flames along the bottom while clearing the center; fire state and textures remain native.

Existing keys, palette, alpha, module preferences and custom HUD placements are retained. The menu keeps its current grid, drawers and premium interaction contract. Unity's checkout and installed artifacts are unchanged.

See [verification and measurements](refinement-results.md), [source credits](../NOTICE) and the [updated README](../README.md). The verification mod remains separate from the runnable release and suppresses all test audio.

Verified with **139 unit tests** and **18,519 packaged-client checks**. The exact checked JAR is installed in the development Vanilla instance. Frame-time medians remain around 8.33 ms under the 120 FPS cap; HUD-catalog allocations increased from about 47.2 to 53.3 KB/frame. Full measurements and their scope are documented; no FPS increase is claimed.
