# Neon 0.5.4 — HUD performance and visual fixes

For Minecraft 26.2 / Java 25 / Fabric.

- Replaced the expensive curve operations used when HUD panels meet with bounded polygon joins. Joined panels keep one translucent fill and animated outside outline, with no internal border or doubled opacity.
- Moving a joined cluster now reuses its geometry. Fill and outline share cached geometry when their enabled panels match.
- Fixed dark dots at module and Settings button-glow corners by matching the bloom to the rounded surface at native display pixels.
- Added **Visuals → Better chat → Message bar**. Turn it off to hide the white stripe beside chat, both while playing and with chat open. Native warning icons, hover details, signatures, original history and links remain intact.
- Preserved six-pixel edge catching, deliberate overlap, Alt bypass, FPS color overrides, chat lift and saved placements. The new preference supports profiles and Undo and defaults on for existing configurations.

Download the runnable `neon-0.5.4.jar`; the sources JAR is for development. Keep one Neon JAR in `mods` and restart Minecraft to load the update. The release includes an exact-JAR verification receipt and SHA-256 checksums.

The final build passed **150 unit tests and 21,326 packaged live checks** and was installed into the development Vanilla instance. Repeated HUD contact used approximately **60% less instrumented time and 63% fewer allocations** than 0.5.3 in the recorded benchmark; frame medians remained near the 120 FPS limit. The report includes raw samples and focus/scheduling limitations.

See [verification and measured costs](contact-results.md), [README](../README.md) and [source attribution](../NOTICE). Test clients are fully muted and isolated. Measurements are not an FPS promise.
