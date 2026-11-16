# Neon 0.5.3 — Unity chat and HUD polish

For Minecraft 26.2 / Java 25 / Fabric.

- Restored the HUD editor's animated cyan/violet outline, including its attached color drawer.
- Clicking a canvas widget smoothly opens its settings; dragging preserves the normal placement interaction.
- Widget edges catch and align at six-pixel spacing before overlap. Push farther to overlap deliberately, or hold Alt to bypass snapping.
- Armor stays fixed beneath open chat. Other bottom information groups retain smooth chat clearance.
- Coordinates no longer shows dimension text or its toggle; Nether conversion remains.
- Adapted Unity's chat font layout, clickable styles, native placement and input frame. Better chat's Client font uses Settings → Fonts and can be disabled separately.
- Added the optional Chat HUD with scale, background, outline and shadow controls. Enable its row in the HUD editor to move native messages. The client now has 22 modules and 24 HUD elements.

Original chat history, signatures, links, filtering and scroll state are preserved. Only accepted new player/server chat triggers pings; reflows and local notices stay silent. Existing settings, profiles, palette, alpha and placements are retained.

Download the runnable `neon-0.5.3.jar`; the sources JAR is for development. Keep one Neon JAR in `mods` and restart Minecraft to load the update. The release includes an exact-JAR verification receipt and SHA-256 checksums.

The final build passed **148 unit tests and 20,646 packaged live checks**. The checked JAR was installed into the development Vanilla instance without closing user games.

See [verification and measured costs](chat-hud-results.md), [README](../README.md) and [source attribution](../NOTICE). Test clients are fully muted and isolated. Measurements are not an FPS promise.
