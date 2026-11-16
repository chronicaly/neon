# Neon 0.5.0 — Client expansion

For **Minecraft 26.2**, **Java 25** and **Fabric**. Install the runnable `neon-0.5.0.jar` alongside Fabric API for 26.2; replace older Neon JARs.

## Fifteen new modules

Crosshair, Toggle sprint, Toggle sneak, Dynamic FOV, Hurt camera, View bobbing, Low fire, Viewmodel, Enchant glint, Hit color, Time changer, Weather, Fullbright, Clean view and Block outline.

All use the existing Neon module grid, right-hand drawers, optional bracketed binds, Toggle/Hold/Reverse hold, profiles and Undo. Cosmetic changes preserve the underlying world and native gameplay state. Fullbright respects darkness/blindness; Weather changes visual precipitation and sky rendering, not ambient audio.

## Twenty new HUD elements

Coordinates, Compass, Ping, Speed, CPS, Keystrokes, Potion effects, Inventory, Held item, Clock, Session, Biome, Light level, Resource packs, Server, Movement, World time, Item counter, Scoreboard and Memory join Armor, Totems and FPS.

Each is independently placed in the direct HUD editor. Native data, resource-pack text and item rendering are retained. CPS excludes generated/menu clicks. The optional scoreboard preserves team styling, hidden entries and custom number formats; disabling it restores the native sidebar. Actual World time remains independent of the cosmetic Time changer.

## Consistent Neon presentation

The follow-up adapts Unity's crosshair coverage for exact odd/even framebuffer centering, fractional stroke widths and Cross/T/Dot/Square shapes. HUD information and FPS use native-resolution Inter by default, with a Minecraft-font option that snaps bitmap glyphs to whole display pixels. Font measurement and shadow ordering follow Unity's fixes; scoreboard colors, bold/italic, underlining and number formats remain. Panels fit their content, and keystrokes remain readable at rest.

Armor and floating totems share a sharp theme-linked plate. New widgets and the optional FPS border use the continuous animated outline with restrained traveling bloom. General offers static outlines and the original armor-slot presentation. FPS keeps its four independent colors and per-color theme Sync. The expanded HUD column stays compact and remembers its scroll through placement and hide/show.

Existing palette, alpha, module preferences, keys and custom HUD placement are preserved. New modules and widgets start disabled. The menu still supports the saved primary binding plus Right Shift, immediate gameplay return, precise numeric edits, fixed input targets and durable Copied/Saved feedback.

## Engineering notes

Features were added individually after polishing the existing HUD, with a Gradle build and muted packaged-client checks after each change set. See [research](client-research.md), [verification and measured performance](expansion-results.md), and the [updated README](../README.md). No proprietary client source or GPL implementation is bundled. Test profiling and audio/clipboard interception are excluded from the runnable JAR.

The final artifact passed 134 unit tests and 17,905 packaged-client checks, including 32 crosshair framebuffer-centering configurations at actual GUI scales 1–4. This exact runnable JAR was installed into PrismLauncher's Vanilla instance. The [verification receipt](expansion-receipt.json) records its SHA-256 identity.
