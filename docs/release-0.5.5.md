# Neon 0.5.5 — Chat stacking and effect icon controls

For Minecraft 26.2 / Java 25 / Fabric.

- Added **HUD editor → Chat → Message bar**. Disable it to remove the gray/white server-message line and its reserved left space. It shares Better chat's existing preference and works even when that module or custom Chat placement is off. Native history, signatures, warning icons and clickable links remain intact.
- Bottom-left Chat and Coordinates now keep a six-pixel gap and move together smoothly when chat opens, closes or reopens mid-animation. Small screens shorten the chat area while preserving scrolling. Saved positions stay unchanged; armor remains fixed.
- Added **HUD editor → Potion effects → Hide vanilla icons**, active while that HUD is enabled, and the independent **Visuals → Clean view → Hide effect icons** switch. Both hide only the top-right icons and preserve actual effects. They start off and support profiles and Undo.
- Retained the neon theme, existing compact controls, animated outlines, font choices and deliberate overlap for other widgets.

Download the runnable `neon-0.5.5.jar`; the sources JAR is for development. Keep one Neon JAR in `mods` and restart Minecraft to load the update. The release includes the exact-JAR verification receipt and SHA-256 checksums.

Passed **153 unit tests and 21,973 packaged live checks**. See [verification and measured costs](chat-dock-results.md), [README](../README.md) and [source attribution](../NOTICE). Verification clients are isolated and fully muted.
