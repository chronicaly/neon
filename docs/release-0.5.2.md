# Neon 0.5.2 — UI polish, fonts and smooth HUD motion

For Minecraft **26.2**, Java **25** and Fabric. Install `neon-0.5.2.jar` alongside Fabric API for 26.2, replacing the older Neon JAR.

- **Centered HUD text:** Inter rows use their visible glyph bounds instead of a fixed menu baseline. Colored label/value runs share a baseline, including coordinates, potion timers and scoreboard rows.
- **Balanced FPS:** the label stays fixed, the number aligns to the right of its reserved four-digit field, and both use a stable vertically centered baseline.
- **Consistent soft corners:** the combined menu, attached drawers and quiet HUD column use six-display-pixel corners; module, Settings/action buttons and text fields use three-pixel corners. Fill and outline follow the same antialiased contour.
- **Themed HUD editor and Settings:** the editor uses Neon's dark panel, matching headings, outlined glass rows, cyan switches and right-aligned values. Settings color rows, profile actions and text inputs share the same treatment. The compact draggable column and direct canvas controls remain.
- **Installed fonts:** Settings → Fonts provides a searchable list of local families and a live preview. Papyrus works when installed. Choices apply to menus and Client-mode HUD text, persist in settings/profiles, and support Undo. Missing fonts fall back to bundled Inter without discarding the saved family name.
- **Smooth chat clearance:** bottom HUD elements ease above the native chat input over 220 ms and return on close. Joined widgets keep their spacing, quick reversals preserve motion, and saved placement stays unchanged. The attached offhand count remains on its real slot.
- **Aligned, quieter sidebar:** Combat, Visuals, Settings and HUD editor share one left-aligned icon/text column, with faint selected tint and no surrounding box. Header dragging and invisible resizing take priority over focused text fields.
- **Subtle transparency:** menu and attached pane surfaces use 84% of the saved background alpha/opacity, with clear text and controls. The HUD editor uses the same surface.
- **Preserved interaction:** one traveling outline surrounds the full menu during expansion/reversal. Fixed hitboxes, invisible-corner resizing, press feedback, scrolling, profiles, Undo and immediate gameplay return remain. Functional switches, slider tracks and swatches keep their rectangular presentation.

Saved palette, alpha, keys, widget placements and local FPS colors are retained. See [verification and measured costs](corners-results.md) and the [README](../README.md). Validation passed 143 unit tests and 19,911 packaged-client assertions. Verification clients are muted; the test harness and profiling code are excluded from the runnable JAR.

Measured limitation: the updated menu adds CPU/allocation work. The reverse-order comparison still measured resize p99 about 8.4% above 0.5.1; see the linked report for both runs and scope limits. This release makes no FPS-increase claim.
