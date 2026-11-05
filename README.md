# Neon

A small client-side Fabric mod for **Minecraft 26.2 / Java 25**, with Minecraft's native interface and item rendering.

- **Freelook:** hold a key to orbit the player without changing aim or movement direction. Enters third person immediately and restores your previous view immediately on release, with vanilla camera collision.
- **Zoom:** hold a key for 4x magnification, with a 150 ms transition, optional scroll adjustment, and proportional sensitivity. Works during freelook.
- **Autoclicker:** independent left/right intervals from 0-60 ticks, with 0 selecting vanilla timing. Click mode attacks without mining; Hold mode keeps the attack button held. Toggle activation can keep running in chat and while tabbed out. Inventory and other menus, death, and disconnect stop it.
- **Armor HUD:** horizontal or vertical layouts, reversible order, native slot frames, durability bars and low-durability warnings. Scale, drag, nudge, choose placement presets, or align directly to screen edges.
- **Offhand totems:** total carried totems drawn on your existing offhand totem, without a separate HUD icon.

## Install

Install Fabric Loader **0.19.5 or newer** and Fabric API for **26.2**, then put the release JAR in your instance's `minecraft/mods` folder. The first release is built and tested with Fabric API **0.161.0+26.2**. [Mod Menu](https://modrinth.com/mod/modmenu) is optional and provides a settings entry.

Use **Mods → Neon → Configure** to open the tabbed settings menu. Assign controls directly in its **Controls** tab or under **Options → Controls → Key Binds → Neon**. All four bindings begin unassigned to preserve existing controls. Click a binding, then press a key or mouse button; Esc cancels and Backspace/Delete clears it. Conflicts are shown in red. The settings shortcut works without Mod Menu once assigned.

For the autoclicker, enable it in Neon settings, choose mouse buttons, and assign its activation key. The **activation mode** controls whether you hold the key or press it to toggle. To keep clicking while typing or tabbed out, choose **Toggle** and leave **Continue in chat** and **Run in background** on. Activate it in-game before opening chat or switching apps; typing in chat never activates or toggles it. Return to gameplay and press the activation key again to stop. Opening inventory or another menu also stops it and requires fresh activation afterward. Focus-loss pausing is temporarily suppressed only while the background toggle is running; saved Minecraft options are not changed.

**Left action** is separate from activation. **Click** (the default) repeats attacks and ignores blocks completely. **Hold** keeps the attack button down, allowing continuous vanilla mining, and disables the left interval slider. Manual mouse input remains independent, so you can still mine deliberately with your physical attack button.

Both interval sliders range from **0 to 60 ticks**. **Vanilla (0)** waits for full weapon recharge on left attacks and Minecraft's normal use delay on right clicks. **1-60** schedules one click attempt every selected number of client game ticks, without adding the vanilla timing gate. At normal speed, 20 ticks is one second and 60 ticks is three seconds. Normal item/server rules still apply. No missed clicks are replayed after a stall. Older CPS settings upgrade to vanilla timing and attack-only Click mode; other preferences are preserved. Neon does not click other applications or send chat messages.

HUD settings include visibility, horizontal/vertical layout, order, slot frames, spacing, warning threshold, scale, position presets, and reset. The position editor supports dragging, arrow-key nudging (Shift for 10 pixels), and wheel scaling over the armor. There is no forced screen margin. Hotbar presets make room for the offhand slot and attack indicator. Empty armor slots stay hidden. The visual direction is inspired by [BerdinskiyBear's Armor HUD / uku3lig's fork](https://github.com/uku3lig/armor-hud); Neon uses an independent implementation.

The totem number appears only when your offhand holds a totem. It includes inventory and offhand; the selected main-hand slot is counted only once. F1 hides the HUD normally.

Settings are stored in `config/neon.json`. Key bindings use Minecraft's normal options. No account, server-side mod, network connection, or YouTube setup is needed.

## Build

With JDK 25:

```powershell
.\gradlew.bat build --console=plain
```

The development build checks the code and installs `build/libs/neon-0.1.2.jar` as `neon.jar` in the local PrismLauncher **Vanilla** instance. The installed file is byte-checked against the build. Existing running games remain open; new code loads on the next game launch.

Use `-PskipInstall` to build without installing, or `-PneonInstallDir=<mods-directory>` to select another destination. Linux/macOS users can run `./gradlew build -PskipInstall`.

On Windows installations affected by Java's local socket initialization error, run the included `tools/build.ps1`; it applies a process-local TCP fallback before invoking the same Gradle build.

## Verification

`build` runs the regression suite and compiles the isolated live verification harness. The harness is excluded from the release JAR. See [verification instructions](docs/verification.md) for live-test setup and results.

YouTube chat is deferred. This release contains no chat integration or OAuth code.

## License

MIT. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
