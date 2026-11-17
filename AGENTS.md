# Neon development

## Authoritative interface contract

Follow [docs/design-language.md](docs/design-language.md) and [docs/neon-design-reference.html](docs/neon-design-reference.html) for every client UI change. The consolidated contract contains the latest accepted design; files under `docs/history` and dated reports describe earlier versions, not competing requirements. Preserve existing features and saved settings unless the user explicitly changes them.

- Keep fixed-hitbox 50/100 ms press feedback, velocity-preserving motion, 120 ms content crossfades, immediate gameplay release with an 80 ms render-only fade, native gesture cursors and durable Copied/Saved acknowledgements.
- Keep Combat, Visuals and Settings, compact equal-width module/Settings grids, attached reversible drawers and one animated cyan/violet outline. HUD editor is a direct action beneath Settings. All sidebar icons/text share left columns. Use native-resolution selected-family text (Inter fallback), independent 132.25% UI scale, rounded shared surfaces and light-glass controls. Never show scrollbars, footer controls or decorative copy.
- Left click modules to toggle; right click to configure. Bracketed binds belong only at the button's right edge: click assigns, right click cycles, hover replaces the key with the full mode. No grid +/−, floating mode tooltip or drawer bind controls. HUD/color rows retain +/−. Preserve cyan boolean switches, usable numeric controls with narrow-layout fallback, exact entry and Undo. No Reset.
- Preserve the saved primary binding (Insert in Vanilla), separate Right Shift alternate, capture/text priority and discarded chat shortcuts. Search stays hidden until Ctrl+F; Escape clears then hides it. It is a temporary view filter and never changes enabled states, profiles or Undo.
- Use the compact 168-unit attached color drawer with live HSV/opacity, tiny original/current previews, hex and Unity Copy/Paste/Sync. Narrow layouts replace/restore the settings pane. Dismissal keeps live colors; the shared header moves the whole shell. Preserve Vanilla palette and alpha.
- Retain all existing HUD elements in the compact draggable expandable column. Canvas clicks reveal folds; drags do not. No selection outlines, handles or floating labels. Render/hit-test/snap only enabled widgets; keep disabled rows searchable. Preserve invisible resize, gesture Undo, six-pixel catches, deliberate overlap and Alt bypass. Armor stays fixed beneath chat; attached totems stay on the real offhand slot; Coordinates has no dimension text/toggle.
- FPS has four independent theme-linked color rows in its fold. Overrides affect only FPS and persist with settings/profiles/Undo; Sync restores each link. HUD column/color drawer share one animated outline. Preserve native items, centered HUD typography, joined glass surfaces and Client/Minecraft text.
- Better chat preserves original history, signatures, tags, links and scrolling. Emit one message/mention ping only on accepted new player/server chat, at independent volume. Reflows, local notices and action bars stay silent. Preserve bottom-left Chat/Coordinates stacking and the shared Message bar preference.
- UI audio uses original soft console cues at independent Neon volume, one cue per accepted input. Continuous adjustments, typing and unchanged/rejected actions stay silent. Colors owns Opacity; Appearance directly owns UI sounds/Effects. Freelook Distance is 1–16/default 4; Motion blur defaults to 10%, preserving custom values.

## Builds and verification

- Target Minecraft 26.2, Java 25 and unobfuscated Fabric Loom.
- Run an actual Gradle build after each completed code/configuration change set.
- Install only a successfully checked exact runnable JAR into PrismLauncher Vanilla. Never install stale output after a failed build.
- Keep existing Minecraft instances open. Preserve Unity's separate checkout and installed artifacts.
- Keep verification clients silent. Intercept Minecraft and Neon playback in the harness while retaining sound assertions; disable optional voice chat in isolated clients. Never send test sounds to the user's speakers.
- Keep profiling and clipboard/audio interception in the separate verification mod. Report measured allocations/frame times with limits, without promising an FPS increase.
- Inspect actual module/settings pages, HUD folds and attached colors at default/minimum sizes before declaring UI work complete. Distinguish scripted GUI-scale checks from physical native-input/mixed-DPI coverage.
