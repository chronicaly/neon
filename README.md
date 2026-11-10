# Neon

A small client-side Fabric mod for **Minecraft 26.2 / Java 25**, with a custom Neon menu and native item rendering on the HUD.

- **Freelook:** hold a key to orbit the player without changing aim or movement direction. Enters third person immediately and restores your previous view immediately on release, with vanilla camera collision.
- **Zoom:** Hold or Toggle activation, 4x magnification by default, a 150 ms transition, adjustable wheel increments, and proportional sensitivity. Works during freelook. Menus and focus loss cancel both zoom modes.
- **Motion blur:** Unity's frame accumulation effect, with 0-100% strength and an Include UI option. Off by default; 75% strength matches Unity. Works alongside freelook and zoom.
- **Autoclicker:** independent left/right intervals from 0-60 ticks, with 0 selecting vanilla timing. Click mode attacks without mining; Hold mode keeps the attack button held. An active Toggle session keeps running behind Escape, inventory, chat, and every other menu, including while tabbed out. Only an explicit stop turns it off.
- **Armor HUD:** horizontal or vertical layouts, reversible order, native slot frames, durability bars and low-durability warnings. Defaults beside the hotbar, bottom-aligned to the left of the offhand slot. Scale, drag, or nudge directly; existing custom positions are preserved.
- **Totems:** total carried totems on your existing offhand item by default, or an independently movable and resizable widget in Floating mode.
- **Profiles and precise settings:** named setups, exact numeric entry, Undo, and per-module resets in a clean custom interface.

## Install

Install Fabric Loader **0.19.5 or newer** and Fabric API for **26.2**, then put the release JAR in your instance's `minecraft/mods` folder. The first release is built and tested with Fabric API **0.161.0+26.2**. [Mod Menu](https://modrinth.com/mod/modmenu) is optional and provides a settings entry.

Press **P** in-game to open the Neon menu, or use **Mods → Neon → Configure**. The sidebar contains **Combat**, **Visuals**, **HUD**, and **Settings**. Click a module's name or chevron to expand its settings in place; click again or press Esc to collapse. Its square toggles the module independently, and the inline bind captures a shortcut. **Settings** contains expandable **Appearance** and **Profiles** rows. **Esc** from a collapsed category closes the menu. There are no X, Undo, or Done buttons. Useful controls are exposed directly without Advanced folds. Returning from another category keeps your scroll position.

The menu follows [Neon's design language](docs/design-language.md) and approved **B — Expandable rows**: flat module rows, sharp corners, native-resolution Inter typography with 16px body text, a thin smoothly animated cyan/violet outline with localized bloom traveling with its bright highlight, and subtle glow on active controls. It uses the requested 132.25% scale and starts at 582×370 display pixels, with small sidebar icons and settings indented beneath their module. Its size is independent of Minecraft's GUI scale. Drag the header to move it or pull any corner to resize; no grips are drawn. Position and size are saved separately from profiles. Older oversized placements return to the compact default once; saved compact placements receive each requested 15% increase only once. Custom controls retain keyboard navigation, narration, and scrolling.

Assign feature controls using the compact inline bind beside each module, such as **[M5]**, or under **Options → Controls → Key Binds → Neon**. The global menu binding is managed in Minecraft's key binds and defaults to **P**; freelook, zoom, and autoclicker begin unassigned. Existing saved bindings are preserved on updates. Click a bind, then press a key or mouse button; Esc cancels and Backspace/Delete clears it. Conflicts appear in red. The menu shortcut works without Mod Menu and does not open over chat or other screens.

Vanilla also assigns **P** to Social Interactions. When both use P, Neon opens first. You can give either action another key in Minecraft's key binds; Neon never changes another action's binding for you.

Open **Visuals → Motion blur** to enable it. **Blur the UI** blends the entire frame; turn it off to keep the HUD and in-game menus sharp while the world and hand blur. Neon's settings, numeric/color entry, profiles, confirmations, and HUD editor always stay clear. Strength uses elapsed render time to keep the effect consistent across frame rates. At 0%, or while disabled, the effect releases its GPU buffers. Old frames are discarded after focus loss, screen/perspective changes, long stalls, teleports, and resizing. The module reset, Undo, and profiles include these settings.

Click any slider's displayed value, or focus it and press Enter, to type an exact number. Drag elsewhere on the slider for normal adjustment. Values are validated before applying; exact entries can fall between the slider's usual steps. Use **Ctrl+Z** to reverse recent settings changes, including complete slider drags, bindings, profile loads, colors, and HUD edits. The shared history holds up to 50 changes while the settings session remains open. **Reset** restores only the focused module. Key bindings reset through Minecraft's controls.

Open **Settings → Profiles**. **Combat**, **Mining**, and **Building** presets start with your existing camera/HUD preferences and appropriate autoclicker settings. Save named snapshots, click a profile to load it, or update/delete saved profiles. Loading leaves key bindings and window placement unchanged and preserves an active autoclicker unless the chosen profile disables it. Loading an enabled profile never starts an inactive session. **Ctrl+Z** reverses a load; changing the saved profile library uses its explicit Update/Delete controls.

For the autoclicker, enable it in Neon settings, choose mouse buttons, and assign its activation key. The **activation mode** controls whether you hold the key or press it to toggle. Choose **Toggle** for continuous operation and activate it in gameplay. Escape, Options, inventory, containers, chat, Neon settings, and focus loss all preserve the active session. While active, singleplayer continues ticking behind menus. Turn **Enabled** off in Neon or return to gameplay and press the activation key again to stop; normal Minecraft pausing then returns. Typing in a screen never activates or toggles automation, and synthetic input never clicks menu buttons or moves inventory slots. Saved Minecraft pause options are unchanged.

The Toggle state also survives death, world changes, and disconnect during the same Minecraft session. It sends no world actions while no playable player/world is available, then resumes when gameplay becomes available again, without replaying missed clicks. A new Minecraft process starts inactive. Hold activation remains release-to-stop. The old chat/background switches and automatic low-durability stop have been removed so they cannot silently switch the autoclicker off.

**Left action** is separate from activation. **Click** (the default) repeats attacks and ignores blocks completely. **Hold** keeps the attack button down, allowing continuous vanilla mining, and disables the left interval slider. Manual mouse input remains independent, so you can still mine deliberately with your physical attack button.

Both interval sliders range from **0 to 60 ticks**. **Vanilla (0)** waits for full weapon recharge on left attacks and Minecraft's normal use delay on right clicks. **1-60** schedules one click attempt every selected number of client game ticks, without adding the vanilla timing gate. At normal speed, 20 ticks is one second and 60 ticks is three seconds. Normal item/server rules still apply. No missed clicks are replayed after a stall. Older CPS settings upgrade to vanilla timing and attack-only Click mode; other preferences are preserved. Neon does not click other applications or send chat messages.

Optional **Entities only** suppresses air swings in Click mode. Tool wear does not automatically stop the session. Physical mouse input remains independent.

Click **HUD** to open the editor directly. Its Unity-derived column contains all **Armor** and **Totems** options: left click an entry to toggle visibility, or right click an entry/widget to expand its settings. Armor includes orientation, scale, order, slot frames, spacing, and durability warnings; totems include scale and offhand/floating mode. Numeric values edit inline. Drag a widget to move it, drag a corner to resize, or use the wheel to scale. There are no coordinate forms or preset positions. The column hides during placement so it cannot cover the preview. HUD elements have no selection outlines, drawn handles, or floating labels; corner resizing remains available. Disabled elements are hidden and do not receive canvas input or attract snapping. Arrow keys nudge by one display pixel (Shift for ten). General contains HUD scale (0.5–2×), Guides, Snap, and Snap sensitivity (1–24 display pixels); Alt bypasses snapping. Armor and floating widget sizes use Neon scale independently of Minecraft GUI scale. The attached totem count follows the real offhand slot.

**Damage preview** uses copies of equipped items to show nearly broken armor without changing real equipment. Press **Tab** or **H** to hide/show the column, **Ctrl+Z** to undo a gesture, or **Esc** during a gesture to cancel it. The world remains sharp and undimmed, with no bottom hint banner; chat is hidden while editing. Only armor and totems are included, with no forced screen margin. Empty armor slots stay hidden during gameplay. The armor presentation is inspired by [BerdinskiyBear's Armor HUD / uku3lig's fork](https://github.com/uku3lig/armor-hud); Neon uses an independent implementation. Unity's separate checkout and installed artifacts are preserved.

In the default **Offhand** mode, the totem total appears only when your offhand holds a totem. Drag it in the editor to place it freely, or select **Mode: Floating** in its options; this replaces the offhand total decoration. Ctrl+Z restores the entire placement change. Floating mode appears when you carry at least one totem. Both modes count inventory and offhand, with the selected main-hand slot counted only once. F1 hides the HUD normally.

**Settings → Appearance** changes Background, Outline, Accent, Text, Cards, Secondary outline, and Muted text. Opacity controls overall panel transparency (94% by default). Left or right click a color row to unfold Unity's square HSV picker, hue/opacity strips, original/current previews, and Copy/Paste. Changes apply while dragging; closing the fold or pressing Escape keeps them. Ctrl+Z restores a paste or complete drag. Copy uses #RRGGBBAA; Paste also accepts short hex, RGB(A), HSB, and named colors. Profiles include the palette, opacity, module sound volume, HUD scale, and snap sensitivity.

Neon has no scrollbars: use the wheel or keyboard focus to reach longer pages. Hover states, folds, and scrolling animate without delaying input. Only enabling a module plays a quiet original Neon sound through an independent audio line. Appearance → Module sound controls its volume independently of Minecraft sounds. Colors, navigation, other settings, and disabling modules are silent. Exact numeric edits commit with Enter or a valid outside click; Escape cancels and invalid drafts stay visibly marked.

**Appearance → Effects** defaults to **Adaptive**. Sustained frame pressure first removes menu background blur, then decorative glow, with slower recovery and at least three seconds between changes. **Full** keeps both enabled. Neon uses its own backdrop blur strength without changing the saved Minecraft preference. The dark backdrop, crisp text, animated outline and its traveling bloom, HUD items, and gameplay motion blur retain full quality in either mode.

Settings are stored atomically in `config/neon.json`, saved profiles in `config/neon-profiles.json`, and window placement in `config/neon-window.json`. An ordered background writer coalesces rapid edits for 250 ms and flushes on close, disconnect, and shutdown. Write failures are reported while unsaved changes remain in memory. Key bindings use Minecraft's normal options. No account, server-side mod, network connection, or YouTube setup is needed.

## Build

With JDK 25:

```powershell
.\gradlew.bat build --console=plain
```

The development build checks the code and installs `build/libs/neon-0.3.0.jar` as `neon.jar` in the local PrismLauncher **Vanilla** instance. The installed file is byte-checked against the build. Existing running games remain open; new code loads on the next game launch.

Use `-PskipInstall` to build without installing, or `-PneonInstallDir=<mods-directory>` to select another destination. Linux/macOS users can run `./gradlew build -PskipInstall`.

On Windows installations affected by Java's local socket initialization error, run the included `tools/build.ps1`; it applies a process-local TCP fallback before invoking the same Gradle build.

## Verification

`build` runs the regression suite and compiles the isolated live verification harness. The harness is excluded from the release JAR. See [verification instructions](docs/verification.md) for live-test setup and results.

YouTube chat is deferred. This release contains no chat integration or OAuth code.

## License

MIT. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
