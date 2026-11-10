The expandable-row interface is documented first below. Earlier performance measurements are in [quality-results.md](quality-results.md); historical sections describe their respective builds.

# Neon verification

## Independent scale, direct controls and traveling bloom — 10 November 2026

The latest correction keeps the compact B layout and concentrates bloom around the moving colored outline highlight. There is no full-window halo. Control borders are aligned to native display pixels; module toggles animate for 100 ms, while clicks apply immediately. Sidebar selection uses a quiet gray fill and line icons. Module controls appear directly without Advanced folds. Appearance includes panel opacity, independent module-sound volume, and Effects directly.

Armor and floating HUD widgets share physical-pixel geometry between gameplay and the editor. General exposes HUD scale and snap sensitivity; nudging and snapping use display pixels. The user explicitly chose to keep the totem count attached to the actual offhand slot, so global HUD scaling leaves it there. Canvas selection outlines, drawn handles, and floating labels are removed; invisible-corner resizing remains available. Only enabled widgets render or receive canvas input, and hidden widgets do not attract snapping. The column has a quiet static border. Native menu text, controls, backdrop blur strength, and module-enable audio are independent of Minecraft's GUI scale, blur preference, and sound volume. Minecraft preferences remain unchanged; the HUD editor stays unobstructed and sharp.

The font stress probe exposed per-sample temporary arrays in BufferedImage.getRGB. Reading the same packed ARGB raster directly removes that allocation path. A comparison against the preceding checked JAR verified **336 text runs**, including every texel, dimension and glyph bearing, are identical (`runtime/neon-raster-equivalence.log`).

The checked build passed **48 unit tests** and **6,320 packaged-client checks**. Verification includes all module/settings pages and seven color folds at default/minimum sizes and GUI scales 1–3, additional odd-resolution captures at GUI scales 1–4, actual HUD placement/resizing/Undo, exact scale/snap editing, draft preservation, animation input, resource reload, adaptive effects, profiles, save behavior, camera/autoclicker timing, and GPU motion blur. The physical armor bounds are identical at GUI scales 1–4; snapping an 8px gap succeeds with a 10px threshold and remains unsnapped with a 4px threshold. An actual independent audio line loads the original waveform and enables the cue with Minecraft master volume zero; Neon volume zero silences it. Every other menu interaction remains silent.

Actual captures: [menu with traveling bloom](images/neon-polish-ingame.png), [minimum size](images/neon-polish-minimum.png), and [HUD General at Minecraft GUI scale 4](images/neon-polish-hud.png). Additional actual captures show [outline-free dragging](images/neon-hud-clean-drag.png) and [disabled HUD elements](images/neon-hud-disabled.png). Extraction checks verify zero submitted items when both widgets are off, one with floating totems enabled, four with armor enabled, and no canvas outline frames during placement. The current menu remains 582×370 display pixels by default and 423×278 at minimum. Native captures were visually inspected as well as checked for text coverage, complete labels, border continuity, hitboxes, and reachable last rows. Historical `-advanced` capture filenames now identify the scrolled-bottom state; no Advanced control is present.

Evidence: `runtime/neon-polish-build.log`, `runtime/neon-polish-final.log`, and `runtime/neon-polish-final-pass.txt`. The frozen checked JAR is archived as `runtime/neon-polish-checked.jar`, SHA-256 `bd6f55a02ae591239b77ad3b4f26c9c0e6885765121caeaa999652dc0a1a2cbf`. Earlier performance work below refers to its own historical build; this pass compares against the previously installed B build `0c38f7b5c63d9682c199d6b3adcf8ea6193528da29548a4eadbaa60adcc5923f`.

The final three-run performance comparison found no repeated frame-time regression above 5%. HUD placement uses 11.6% fewer allocated bytes and 47.6% less measured scope time; the font stress probe uses 20.5% less scope time. Traveling bloom adds approximately 0.060 ms/frame in the idle menu. Full data and limits are recorded in [the performance report](neon-polish-performance.md).

Installation completed through the successful Gradle build (`runtime/neon-polish-installed.log`). The built, frozen production-test, archived checked, and installed PrismLauncher Vanilla `mods/neon.jar` all match SHA-256 `bd6f55a02ae591239b77ad3b4f26c9c0e6885765121caeaa999652dc0a1a2cbf`. Existing Minecraft instances remain open; the update loads on the next launch. Unity's separate checkout and installed artifacts were preserved. The final package contains the native fonts and original PCM cue and excludes verification classes.

## Approved option B: compact expandable rows — 9 November 2026

The card grid and separate module pages are replaced by flat vertical rows. A small square enables each module; the name/chevron expands settings beneath it, and the adjacent bind chip captures independently. Category icons, a subdued violet selection, cyan expanded rows, fine dividers, and square slider handles follow the approved B render. Appearance and Profiles also expand inside Settings. The default remains **582×370 display pixels**, with a **423×278 minimum**, independent of Minecraft GUI scale. Header movement, invisible corner resizing, keyboard navigation, smooth wheel scrolling without scrollbars, inline colors/numbers, Undo, and the direct HUD editor remain available.

Freelook and Zoom now have real persisted enable switches, defaulting to enabled for compatibility with existing bindings. Verification confirms disabled bindings do nothing and disabling an active module releases its camera/zoom state. Only enabling a module plays the original quiet sound. The concept's example shortcut labels are not fabricated in the app; Motion blur still has no bind. Existing key assignments, palette, profiles, and placements are preserved.

The final Gradle build passed **47 unit tests**. The full packaged-client suite passed **6,021 live checks**, including 156 page captures across default/minimum sizes and GUI scales 1–3, native row/toggle/bind hit testing, rapid expansion, numeric drafts, profile operations, color folds, HUD placement, resource reload, camera, autoclicker, and GPU motion blur. A late resize check also verifies an already-open color picker fits after the game window shrinks and retains its live color. All module/Settings pages were visually inspected at default/minimum sizes; native-resolution captures and text coverage checks confirm complete labels and reachable final actions. The earlier run passed 6,013 checks; its follow-up passed 117 focused checks before the final resize fix was included in the full release run.

Actual packaged-client captures: [default](images/neon-expandable-rows-ingame.png), [minimum at GUI scale 3](images/neon-expandable-rows-minimum.png). Compare with the [approved concept](images/neon-expandable-rows-reference.png). The six `runtime/smoke/game/screenshots/neon-rows-window-scale*-*.png` captures show the same row state at all tested scales. Full page captures use `neon-quality-page-*`.

Logs: `runtime/neon-rows-resize-build.log`, `runtime/neon-rows-release.log`, and `runtime/neon-rows-installed.log`. The runnable, frozen production-test, and installed PrismLauncher Vanilla JARs match SHA-256 **0c38f7b5c63d9682c199d6b3adcf8ea6193528da29548a4eadbaa60adcc5923f**. Running user Minecraft instances were left alone; the new code loads on the next launch. Unity's separate checkout and artifacts were untouched. Reproduce the focused row/camera/window-resize checks with `prodSmoke -ProwsOnly`; use `prodSmoke` for the full suite.

## Inline controls and unified Unity-derived HUD editor — 9 November 2026

This supersedes the earlier HUD navigation and color-dialog descriptions below. The HUD sidebar now opens the editor directly. Armor and Totems expose all their configuration in expandable rows using Unity's row model; numeric input remains inside its row. The editor ports Unity's diagonal corner-resize calculation and slider motion. Coordinate forms, preset-position grids, the intermediate HUD cards, and the Edit HUD launch action are removed. Directly dragging an offhand totem enables free placement, and one undo restores the gesture and mode together. The column hides during placement.

Appearance colors expand beneath their setting row and apply live. Both HSV gestures and hex input participate in undo; Escape collapses the picker. Its height adapts so the setting name and complete picker remain visible in the smallest resizable menu. Settings rows reserve a scrollbar/glow gutter, key names such as `[DEL]` fit completely, narrow choices reflow, and mouse interaction no longer leaves large slider focus boxes. The back glyph uses the native-resolution font renderer. The approved animated outline and 132.25% interface scale are retained.

Final verification: **37 unit tests** and **1,210 packaged-client live checks** passed. The page audit inspected 38 rendered default/minimum-size views covering all remaining module pages, advanced options, profiles, and all seven inline colors. It checks full control/label widths, scrollbar clearance, complete picker visibility, and reachability of the last row. GPU readback confirmed the final scrolled Reset labels rendered (165 non-background text pixels in each audited case). Additional native-input checks cover direct HUD entry, every armor/totem option, inline numeric editing/validation/cancellation, persistence, dragging, resizing, undo, minimum viewports, and returning to the same parent menu. The full run also covers camera, autoclicker, motion blur, resource reload, and shutdown. GUI-scale comparisons retained zero sampled text/card differences and zero outline gaps.

Logs: `runtime/neon-release-build.log`, `runtime/neon-release-live.log`. Actual in-game review captures include `runtime/final-autoclicker.png`, `runtime/final-zoom-advanced.png`, `runtime/final-inline-color-narrow.png`, `runtime/neon-final-inline-colors.png`, and `runtime/neon-final-hud-editor.png`; complete per-page captures are under `runtime/smoke/game/screenshots/neon-page-*` and `neon-unity-hud-*`.

The runnable `build/libs/neon-0.3.0.jar` and installed PrismLauncher Vanilla `mods/neon.jar` match SHA-256 **478646c2d7e0b4a145cf99a5ddf35fcf48d4df532e4397bf255b1310687d1150**. The checked JAR is installed for the next launch. Existing gameplay instances remained open. Unity's checkout was read as the source for the requested port; its files and installed artifacts were not changed. Source attribution is recorded in `NOTICE`, and the latest UI contract is in `AGENTS.md` and `docs/design-language.md`.

## Native-resolution typography and second scale increase — 9 November 2026

The user requested another 15% increase and clearer typography. The interface scale is now **132.25%**, giving an approximately **582×370 display-pixel** default menu. Inter body text is **16px**, secondary text **15px**, and headings **19px**. Saved placements migrate through the requested increases once; gameplay armor/totem size is unchanged.

The font renderer now shapes complete text runs with kerning, rasterizes at 4× resolution, area-resolves edge coverage, and composites the result at one texel per display pixel. Pixel-aligned origins avoid a second resampling step, transparent texels retain white RGB, and texture uploads are batched at frame end. Editable fields use the same renderer with measured cursor/selection positions and preserve vanilla keyboard editing and IME composition state. Compact Cancel/Apply labels fit again. The animated outline uses one ordered mesh aligned to display pixels, removing gaps that were visible between segments at fractional scales.

The final build passed **37 unit tests** and the packaged production client passed **283 live checks**. New checks cover smooth alpha coverage, kerning, accents/descenders, sequential placement migrations, native pointer placement in text fields, select-all and replacement. GPU readback found **192 intermediate text shades**, **zero border gaps** at GUI scales 1/2/3, and **zero changed pixels** in the sampled text/card region across those scales. The full run also passed profiles, exact-value entry, menu movement/resizing, HUD editing, camera, autoclicker, motion blur, resource reload, and shutdown checks. Logs: `runtime/neon-type-input-build.log` and `runtime/neon-type-final-live.log`.

Visually inspected final captures are `runtime/smoke/game/screenshots/neon-typography-132-menu.png` and `neon-typography-132-input.png`. Final runnable/installed SHA-256: `9670a26ab25d88cd38813cfbb490fb6466e5e5cb0e641909ce83512c86e84ca2`. Both JARs match; the checked release is installed in PrismLauncher Vanilla and loads on the next launch. Existing gameplay clients and Unity's checkout/artifacts were not closed or modified.

## Independent UI scale and Unity-style HUD editor — 9 November 2026

This update supersedes the dimensions and controls in the earlier compact-design section. The user requested smaller module/category boxes and a clearer HUD editor, then a 15% increase from that corrected density. The final default menu is **506×322 display pixels**. Minecraft's GUI scale no longer enlarges the controls. X, Undo, and Done buttons and their footer space are removed; Escape goes back/closes and Ctrl+Z retains the shared history. Header dragging, invisible resize corners, inline binds, Appearance, and Profiles remain functional. Legacy oversized placements reset once; existing compact placements grow by 15% once without repeatedly expanding on load.

The HUD editor follows Unity's compact draggable column, approximately **216×102 display pixels** when collapsed. Armor/Totems settings expand on right click, including right click on the widget itself. The inspector disappears during canvas move/resize gestures, Tab/H hides it manually, and no toolbar or bottom hint obstructs placement. The world stays sharp and undimmed, chat is suppressed, and guides appear only during gestures. Armor/totem rendering and canvas positions retain Minecraft's gameplay coordinates and scale. Unity's separate checkout and installed artifacts are unchanged.

Inter uses fractional glyph advances and newly rasterized glyphs at twice the final physical size for the 115% scale. Actual GPU screenshots at GUI scales **1, 2, and 3** show the same menu rectangle; the sampled text/card region has **zero changed pixels** across all three scales. Native input tests cover clicking, clipping/hover, wheel scrolling, header movement, HUD dragging, Undo, and hiding/restoring the column at these scales. Visual review includes `neon-density-scale-2.png`, `neon-density-hud-collapsed.png`, `neon-density-hud-expanded.png`, and `neon-density-hud-dragging.png` in `runtime/smoke/game/screenshots`.

The final Gradle build passed **34 unit tests**, and the packaged client passed **121 live menu/HUD checks** after the 15% scale adjustment. Logs: `runtime/neon-scale115-build-final.log` and `runtime/neon-scale115-verified-final.log`. Before that final scale adjustment, the complete client suite passed **271 live checks**, including camera, autoclicker, profiles, HUD, and GPU motion blur (`runtime/neon-clean-controls-verified.log`). The final UI verification was rerun against the actual packaged JAR; unrelated gameplay checks were not repeated for the presentation-only adjustment.

Final runnable and installed JAR SHA-256: `b06cd46969192c25202219502307410964857dd0e7a627ea64c303000f52b05e`. The files match. The checked release is installed in PrismLauncher Vanilla; existing gameplay instances were kept open, and the changes load on the next launch. The latest geometry and interaction contract is saved in `docs/design-language.md` and required by `AGENTS.md`.

## Compact design language — 9 November 2026

The approved visual contract is now preserved in `docs/design-language.md` and `docs/neon-design-reference.html`, and required by `AGENTS.md`. The final Gradle build passed **32 JUnit tests** and installed the checked runnable JAR into PrismLauncher Vanilla. The packaged production client passed **238 live checks** on OpenGL with the existing Sodium/Iris mod set and no external shader pack. Logs: `runtime/neon-compact-build-final.log` and `runtime/neon-compact-verified.log`.

The menu opens on category module cards, with no Overview or decorative subtitles. Cards contain one row with compact inline binds, including `Autoclicker [M5]`. Clicking a module opens its settings. Settings contains Appearance and Profiles without a keybind list; the global menu key remains P by default and existing saved bindings are preserved. Appearance includes an HSV/hex picker, validation, Apply/Cancel, shared Undo, and profile persistence.

Real input checks cover header/brand dragging, all four invisible resize corners with the opposite corner fixed, Escape cancellation, viewport clamping, placement persistence across reopening, minimum viewport usability, category navigation, keyboard focus, exact-value entry, module-specific scroll restoration, bind capture/cancellation/unbinding, and profile save/load/update/delete. Profile loading restores appearance without changing window placement or bindings. HUD editing remains limited to armor and totems. Existing camera, autoclicker, and GPU motion-blur checks passed in the same final run.

The approved HTML was rendered at 1280×800 and compared with Minecraft at the same logical dimensions. The menu rectangle is 640×400, with an 88-pixel sidebar, 43-pixel header, 33-pixel cards, 6-pixel card gaps, and 15-pixel content inset. Screenshot pixel samples match exactly for the panel (`#0e131b`), sidebar (`#0c1016`), cards (`#121923`), and structural border (`#27313c`). Inter is rasterized at the physical GUI scale; browser and Java glyph antialiasing are separate rasterizers. The thin cyan/violet outline uses continuously interpolated colors, and GPU readback confirms animation. Active-control glow uses a linearly filtered Gaussian texture instead of stepped rectangle halos.

Actual screenshots inspected include `neon-design-reference-visuals.png`, `neon-design-cards.png`, `neon-design-autoclicker-inline.png`, `neon-design-camera.png`, `neon-design-appearance.png`, `neon-design-color-picker.png`, and `neon-profiles.png` in `runtime/smoke/game/screenshots`. Rendering was checked at GUI scales 1 and 2; controls were exercised down to a 320×240 logical viewport. Small viewports use readable minimum dimensions instead of forcing the quarter-area default. A transient Windows file-replacement lock observed during repeated saves is handled with bounded retries; final verification logged no Neon save failures.

Final runnable/installed JAR SHA-256: `bd70208f8ab61333a6d7a2de5560ce4263a52f30b3a2e6f625ba658dc367766a`. Both files match; the release contains Inter fonts/license and excludes test-only classes. The isolated verification client exited normally. Existing gameplay clients and Unity's separate checkout/artifacts were left intact. New code loads on the next Minecraft launch. Use `prodSmoke -PdesignOnly` for the shorter menu/HUD verification path.

## Persistent autoclicker update — 9 November 2026

The build passed **29 unit tests**, including five new activation/migration tests. The dedicated production client passed **49 live assertions** with the Vanilla mod set and Complementary Reimagined on OpenGL. The combined regression harness also passed **207 live checks** against the same development build.

The dedicated run verifies continued attack/use timing behind Escape, Options, inventory, a container, chat, Neon settings, and a screen containing a button and editable text. It checks that screens stay open, inventory/carried stacks stay unchanged, no synthetic UI input or key queues leak, and both the client and integrated server keep ticking. Custom intervals remain exactly two/three ticks; interval zero still waits for full attack recharge and the vanilla use delay, including in a background menu.

Explicit disabling immediately stops both buttons and restores normal singleplayer pausing. Enabling the setting alone or typing an activation key in a menu cannot start a session; typed keys are not replayed when the menu closes. Manual gameplay toggling still stops it. A nearly worn shovel breaks a real dirt block behind Escape without an automatic durability stop. Disconnect retains the Toggle state, emits no world actions without a player/world, and rejoining resumes without catch-up bursts. Minecraft's saved pause-on-focus-loss option remains unchanged.

Reproduce with `prodSmoke -PclickerOnly -PsmokeDir=runtime/clicker/game --console=plain`, after preparing that disposable directory as described below. The log is `runtime/clicker/persistent-final.log`, and its marker is `runtime/clicker/game/neon-verification.txt`. The separate directory avoids interference with menu-design verification. The previous screen-cancellation, tool-protection, and profile-load cancellation assertions were updated to match the user's manual-stop requirement; historical results below describe earlier behavior.

Verified on 5 and 9 November 2026 with Minecraft 26.2, Java 25, Fabric Loader 0.19.5 and Fabric API 0.161.0+26.2. The latest 0.3.0 results are recorded below; earlier sections retain the historical checks.

## Sharp Neon interface and live HUD editor — 9 November 2026

The final Gradle build passed **29 JUnit tests** and installed the checked runnable JAR into PrismLauncher Vanilla. The packaged production client then passed **211 live checks** on OpenGL with the existing Sodium/Iris mod set and no external shader pack. Logs: `runtime/neon-sharp-build.log` and `runtime/neon-sharp-smoke-final.log`.

The sidebar now contains Combat, Visuals, HUD, and Settings categories, with module cards inside them. The custom interface has sharp rectangular corners, dark surfaces, antialiased Inter text rasterized at the window's pixel scale, smooth hover/switch transitions, and glowing cyan/violet perimeter traces. GPU screenshot readback verifies that the rendered outline changes between frames. Visual inspection covered the overview, module cards, Advanced settings, profiles, invalid exact-value entry, and both HUD inspector selections at 1280×800 and 854×480 / GUI scale 2. The bundled font license and both font weights were checked in the release JAR.

Live checks cover P opening the menu while vanilla Social Interactions retains P, custom bindings and capture cancellation, keyboard navigation, page-specific scroll restoration, category navigation fitting the minimum 320×240 viewport without overlapping Profiles, and Advanced expanding/collapsing back to the minimal page. The HUD category exposes only armor/totem visibility and editor access before Advanced is expanded.

The live HUD editor verifies selection of only Armor and Totems, movable inspector controls, drag positioning, fixed-opposite-corner resizing, one-step gesture Undo, exact coordinates, nudging, screen alignment, snapping, wheel scaling, and damage preview without changing equipment. Tests cover canceling both changed and unmoved gestures without consuming earlier Undo history. Floating totems move and resize independently, replace the offhand total decoration, persist mode/position/scale, and can return to the default Offhand mode. The inspector fits the minimum viewport and gameplay resumes after editing.

The final JAR SHA-256 is `ee715ed0ae947b5a61006b38b25bdf2a99b483d7403431f8d19c9cd49a2410f2`. The installed `neon.jar` matches it byte-for-byte; test-only classes remain excluded. Verification used the isolated client and disposable world. No gameplay client or Unity checkout/artifact was closed or replaced by this work. Code changes load on a subsequent Minecraft launch.

## Motion blur and combined 0.3.0 verification — 9 November 2026

The Gradle build passed **24 JUnit tests**. Five new tests cover Unity's elapsed-time strength curve at 30/60/120/240 FPS, first frames, history resets and long stalls, finite strength endpoints, old-config defaults, persistence, clamping, profiles, and Camera-page resets.

The production harness ran with Vanilla's current mod set, including Sodium 0.9.2, Iris 1.11.4, ImmediatelyFast 1.16.5, and ShulkerBoxTooltip 5.4.3:

| Backend | External shader pack | Result |
| --- | --- | --- |
| Vulkan | None | 178 combined live checks passed |
| OpenGL | None | 45 motion-blur live checks passed |
| OpenGL | Complementary Reimagined r5.9.3, copied with the user's shader options | 178 combined live checks passed on the final runnable JAR |

Motion-blur checks read actual GPU output for asymmetric color patterns, proving texture orientation and the temporal blend at both render boundaries. They verify fresh history after Include UI changes, closing settings, regaining focus, perspective changes, resizing, re-enabling, and resource reload. Tests confirm disabled/zero-strength/disconnected states free textures, vertices, and uniform storage; normal rendering works together with freelook and zoom.

The native controls were exercised for enable/disable, dependent control availability, exact strength entry, Include UI, and Undo. Settings, number entry, profiles, and the HUD editor were verified to exclude the effect. Motion-blur settings and moving gameplay screenshots were visually inspected. The combined runs also cover the earlier camera, autoclicker, armor/totem, profile, and menu checks.

Logs are retained locally as `runtime/smoke/neon-030-full.log`, `neon-030-opengl.log`, and `neon-030-shaders.log`. To run only the motion-blur portion, add `-PmotionOnly` to `prodSmoke`. Choose `opengl` or `vulkan` in the isolated client's `preferredGraphicsBackend` setting; external shader packs are supplied only to that isolated client. The harness is never run against the user's gameplay world.

The final runnable JAR's SHA-256 is `db1d6d271a89ade9aa61ce436a78e7b48a67d0a6a958463e98bd4d670e25de18`. It contains both shaders and the attribution notice, excludes all test-only classes/mixins, and matches Vanilla's installed `neon.jar` byte-for-byte. Existing gameplay clients were kept open; the new code requires a subsequent launch.

## Automated regression tests

The Gradle build passed 19 JUnit tests covering zoom interpolation/reversal and FOV math, hold/toggle rearming, every custom interval from 1 to 60 ticks, vanilla-readiness timing at 0, no duplicate clicks within a tick or replay of missed intervals, configuration persistence/recovery, migration from CPS settings, exact screen-edge placement, and fitting vertical armor at large scales. New tests cover precise numeric validation, independent section resets, profile copy/persistence/recovery, preserving the initial visual baseline in presets, and snapping thresholds.

## Live verification

The packaged production JAR passed **110 live assertions** in an isolated singleplayer client on Vulkan with the Vanilla Prism instance's mod set, including Sodium 0.9.2 and Iris 1.11.4. No external shader pack was selected in the test client.

The checks cover immediate freelook entry/return, unchanged player aim while orbiting, simultaneous zoom, wheel ownership, front-view preservation, manual perspective override, camera focus/menu cancellation, nearby wall collision, and rapid release/repress. HUD checks cover inventory/offhand totals without main-hand double counting, offhand-only count decoration, vertical armor, flush edge placement, durability warnings, hotbar/offhand spacing, and configuration saving. UI checks cover a 320x240 viewport, reaching the final setting by scrolling, direct key assignment, cancellation, and unbinding.

Autoclicker checks cover scoped synthetic input, normal physical held attack input, real server-confirmed block mining, and menu stopping/rearming. With both interval sliders at 0, every automatic attack waited for full vanilla weapon recharge and use attempts were at least four game ticks apart. Custom mode was measured at exact two-tick attack and three-tick use intervals in both gameplay and background chat, including attacks before full weapon recharge. A separate run verified exact 60-tick attacks while right click independently used seven ticks.

Click mode left a targeted dirt block intact without entering mining, even at a one-tick interval, in both gameplay and background chat. Switching the same active autoclicker to Hold mined that block. UI checks exercised both slider endpoints (Vanilla / 60 ticks) and confirmed that Hold disables the left interval while Click enables it.

Active Toggle mode was verified while typing an unsent chat message, with focus lost in chat, and while tabbed out without chat. Focus-loss auto-pausing was enabled during those checks. A real server block was also mined with chat open and focus lost. Disabling chat continuation stops clicking; typing cannot activate it.

The 0.2.0 checks cover exact decimal entry between slider steps, rejection of invalid values, undoing numeric changes and a complete slider drag, immediate Undo-button availability, isolated Camera resets, and restoring key bindings through Undo. Profiles were saved, loaded, updated, and deleted through their native screens; loading preserved bindings, was undoable, and stopped active automation without restarting it.

HUD checks verified exact X/Y placement at the top-left edge, independent coordinate undo, snapping to an edge and screen center, undoing a complete drag, wheel-scale undo, and low-durability preview without modifying actual equipment. Zoom Toggle persisted after key release, toggled off on a second press, used a configured 1.25x wheel step, and canceled/rearmed correctly on focus changes.

Entities-only mode suppressed air swings and attacked a real targeted armor stand in the isolated world. Tool protection stopped a shovel with five durability remaining before it could mine a dirt block or take further wear. Its action-bar message named the item and remaining durability. Replacing the tool while holding the activation key did not restart automation; a fresh activation did.

The harness simulates Minecraft/window focus changes through test-only mixins and drives feature key mappings on the client thread. It does not establish every operating system's physical input behavior or every multiplayer server's acceptance of automation. Test-only classes and mixins are excluded from both the runnable and main-source release JARs. Cooldown measurements use a tick-start counter so normal input and the chat tick-tail path share the same clock.

Camera, autoclicker and HUD settings, the HUD editor, and the in-game HUD were captured at 854x480 / GUI scale 2. Visual inspection covered the profile library, invalid exact-value entry, guides and low-durability preview, Zoom Toggle, and the tool-protection action-bar notice. Prior checks confirmed native tabs and widgets, vertical armor against the bottom/right edges, and the total count on the existing offhand totem. The interface uses native Minecraft widgets and rendering.

## Reproduce

1. Run `tools/build.ps1` on Windows, or `./gradlew build -PskipInstall` elsewhere.
2. Prepare `runtime/smoke/game/mods` with the optional mod set to verify. Do not copy Neon or Fabric API there: the production task supplies the current built Neon JAR and pinned Fabric API.
3. Prepare a disposable 26.2 world named `Neon Verification` in `runtime/smoke/game/saves`. If using a copy of world metadata, 26.2 also needs `data/minecraft/world_gen_settings.dat` alongside `level.dat`. Never point this harness at a world you use for playing: it builds a test platform and changes the test player's equipment.
4. Set `preferredGraphicsBackend:"vulkan"` in the isolated client's `options.txt` when reproducing the verified backend.
5. Run `./gradlew prodSmoke --console=plain`. On the affected Windows host, first set `JAVA_TOOL_OPTIONS` to `-Djdk.net.unixdomain.tmpdir=Z:\neon-no-unix-sockets` for the Java TCP fallback.
6. Inspect `runtime/smoke/game/neon-verification.txt`, the game log, and screenshots. The task fails if the current run does not write a PASS marker.

The development launcher encountered Minecraft's `Missing uniform Globals` rendering validation error before gameplay. Verification therefore uses Loom's production client task and the actual packaged JAR; no rendering workaround was added to Neon.

## Installation

The successful build was installed as `neon.jar` into PrismLauncher Vanilla's mods directory and checked byte-for-byte against `build/libs/neon-0.3.0.jar`. Installing a JAR does not reload code into an already-running game.

## Menu update — 9 November 2026

The current Gradle build passed all 24 JUnit tests. The packaged JAR then passed **133 live assertions** in the isolated production client with the existing Sodium/Iris mod set, using OpenGL with no external shader pack. This includes the previous 110 checks and 23 menu checks.

Menu checks cover the P default with vanilla Social Interactions still bound to P, opening the overview from gameplay, access to every section and Profiles, keyboard focus and Enter navigation, and the overview fitting a 320×240 GUI viewport without scrolling. They also cover returning from Profiles and exact-value entry without losing scroll position, preserving each section's scroll position, rebinding the menu, capture cancellation, resetting to P, undoing that reset, reopening with a custom key, Escape returning to gameplay, and preventing a shortcut pressed in chat from opening the menu later. Feature bindings remain unassigned by default.

The overview and Controls screenshots were visually inspected at 854×480 / GUI scale 2. The menu uses native Minecraft widgets, background rendering, and tooltips; setting controls include their labels in narrator output. Armor and offhand totem rendering remain the only Neon HUD widgets. The installed `neon.jar` matched the checked build byte-for-byte, and the existing multiplayer client was left running.
