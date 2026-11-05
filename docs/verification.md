# Neon 0.1.2 verification

Verified on 5 November 2026 with Minecraft 26.2, Java 25, Fabric Loader 0.19.5 and Fabric API 0.161.0+26.2.

## Automated regression tests

The Gradle build passed 14 JUnit tests covering zoom interpolation/reversal and FOV math, hold/toggle rearming, every custom interval from 1 to 60 ticks, vanilla-readiness timing at 0, no duplicate clicks within a tick or replay of missed intervals, configuration persistence/recovery, migration from CPS settings, exact screen-edge placement, and fitting vertical armor at large scales.

## Live verification

The packaged production JAR passed **72 live assertions** in an isolated singleplayer client on Vulkan with the Vanilla Prism instance's mod set, including Sodium 0.9.2 and Iris 1.11.4. No external shader pack was selected in the test client.

The checks cover immediate freelook entry/return, unchanged player aim while orbiting, simultaneous zoom, wheel ownership, front-view preservation, manual perspective override, camera focus/menu cancellation, nearby wall collision, and rapid release/repress. HUD checks cover inventory/offhand totals without main-hand double counting, offhand-only count decoration, vertical armor, flush edge placement, durability warnings, hotbar/offhand spacing, and configuration saving. UI checks cover a 320x240 viewport, reaching the final setting by scrolling, direct key assignment, cancellation, and unbinding.

Autoclicker checks cover scoped synthetic input, normal physical held attack input, real server-confirmed block mining, and menu stopping/rearming. With both interval sliders at 0, every automatic attack waited for full vanilla weapon recharge and use attempts were at least four game ticks apart. Custom mode was measured at exact two-tick attack and three-tick use intervals in both gameplay and background chat, including attacks before full weapon recharge. A separate run verified exact 60-tick attacks while right click independently used seven ticks.

Click mode left a targeted dirt block intact without entering mining, even at a one-tick interval, in both gameplay and background chat. Switching the same active autoclicker to Hold mined that block. UI checks exercised both slider endpoints (Vanilla / 60 ticks) and confirmed that Hold disables the left interval while Click enables it.

Active Toggle mode was verified while typing an unsent chat message, with focus lost in chat, and while tabbed out without chat. Focus-loss auto-pausing was enabled during those checks. A real server block was also mined with chat open and focus lost. Disabling chat continuation stops clicking; typing cannot activate it.

The harness simulates Minecraft/window focus changes through test-only mixins and drives feature key mappings on the client thread. It does not establish every operating system's physical input behavior or every multiplayer server's acceptance of automation. Test-only classes and mixins are excluded from both the runnable and main-source release JARs. Cooldown measurements use a tick-start counter so normal input and the chat tick-tail path share the same clock.

Camera, autoclicker and HUD settings, the HUD editor, and the in-game HUD were captured at 854x480 / GUI scale 2. The updated autoclicker controls were visually inspected, including Left action and Vanilla (0) interval labels. Prior 0.1.1 visual checks confirmed native tabs and widgets, vertical armor against the bottom/right edges, and the total count on the existing offhand totem. The interface uses native Minecraft widgets and rendering.

## Reproduce

1. Run `tools/build.ps1` on Windows, or `./gradlew build -PskipInstall` elsewhere.
2. Prepare `runtime/smoke/game/mods` with the optional mod set to verify. Do not copy Neon or Fabric API there: the production task supplies the current built Neon JAR and pinned Fabric API.
3. Prepare a disposable 26.2 world named `Neon Verification` in `runtime/smoke/game/saves`. If using a copy of world metadata, 26.2 also needs `data/minecraft/world_gen_settings.dat` alongside `level.dat`. Never point this harness at a world you use for playing: it builds a test platform and changes the test player's equipment.
4. Set `preferredGraphicsBackend:"vulkan"` in the isolated client's `options.txt` when reproducing the verified backend.
5. Run `./gradlew prodSmoke --console=plain`. On the affected Windows host, first set `JAVA_TOOL_OPTIONS` to `-Djdk.net.unixdomain.tmpdir=Z:\neon-no-unix-sockets` for the Java TCP fallback.
6. Inspect `runtime/smoke/game/neon-verification.txt`, the game log, and screenshots. The task fails if the current run does not write a PASS marker.

The development launcher encountered Minecraft's `Missing uniform Globals` rendering validation error before gameplay. Verification therefore uses Loom's production client task and the actual packaged JAR; no rendering workaround was added to Neon.

## Installation

The successful build was installed as `neon.jar` into PrismLauncher Vanilla's mods directory and checked byte-for-byte against `build/libs/neon-0.1.2.jar`. Installing a JAR does not reload code into an already-running game.
