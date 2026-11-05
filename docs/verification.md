# Neon 0.1.0 verification

Verified on 5 November 2026 with Minecraft 26.2, Java 25, Fabric Loader 0.19.5 and Fabric API 0.161.0+26.2.

## Automated regression tests

The Gradle build passed 10 JUnit tests covering finite camera interpolation, uninterrupted reversal, angle wrapping, zoom math, hold/toggle rearming, click-rate accuracy at tick boundaries, no catch-up bursts, configuration persistence/recovery, and HUD placement bounds.

## Live verification

The packaged production JAR passed **33 live assertions** in an isolated singleplayer client on Vulkan with the Vanilla Prism instance's mod set, including Sodium 0.9.2 and Iris 1.11.4. No external shader pack was selected in the test client.

The checks cover smooth freelook entry/return, unchanged player aim while orbiting, simultaneous zoom, wheel ownership, front-view preservation, manual perspective override, focus/menu cancellation and fresh activation, nearby wall collision, rapid release/repress, inventory/offhand totem totals, equipped armor, scoped synthetic input, real server-confirmed block mining, mining cancellation, normal held attack input, and configuration saving.

The harness simulates focus changes through a test-only mixin and drives feature key mappings on the client thread. It does not establish every operating system's physical input behavior or every multiplayer server's acceptance of automation. Test-only classes and mixins are excluded from both the runnable and main-source release JARs.

Settings, camera settings, autoclicker settings, HUD editor, in-game HUD, and camera screenshots were captured and visually inspected at 854x480 / GUI scale 2. The interface uses native Minecraft widgets and rendering.

## Reproduce

1. Run `tools/build.ps1` on Windows, or `./gradlew build -PskipInstall` elsewhere.
2. Prepare `runtime/smoke/game/mods` with the optional mod set to verify. Do not copy Neon or Fabric API there: the production task supplies the current built Neon JAR and pinned Fabric API.
3. Prepare a disposable 26.2 world named `Neon Verification` in `runtime/smoke/game/saves`. If using a copy of world metadata, 26.2 also needs `data/minecraft/world_gen_settings.dat` alongside `level.dat`. Never point this harness at a world you use for playing: it builds a test platform and changes the test player's equipment.
4. Set `preferredGraphicsBackend:"vulkan"` in the isolated client's `options.txt` when reproducing the verified backend.
5. Run `./gradlew prodSmoke --console=plain`. On the affected Windows host, first set `JAVA_TOOL_OPTIONS` to `-Djdk.net.unixdomain.tmpdir=Z:\neon-no-unix-sockets` for the Java TCP fallback.
6. Inspect `runtime/smoke/game/neon-verification.txt`, the game log, and screenshots. The task fails if the current run does not write a PASS marker.

The development launcher encountered Minecraft's `Missing uniform Globals` rendering validation error before gameplay. Verification therefore uses Loom's production client task and the actual packaged JAR; no rendering workaround was added to Neon.

## Installation

The successful build was installed as `neon.jar` into PrismLauncher Vanilla's mods directory and checked byte-for-byte against `build/libs/neon-0.1.0.jar`. Installing a JAR does not reload code into an already-running game.
