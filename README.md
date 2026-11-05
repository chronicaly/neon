# Neon

A small client-side Fabric mod for **Minecraft 26.2 / Java 25**, with Minecraft's native interface and item rendering.

- **Smooth freelook:** hold a key to orbit the player without changing aim or movement direction. The camera eases into third person and returns to the previous view on release. Default transition: 250 ms.
- **Zoom:** hold a key for 4x magnification, with a 150 ms transition, optional scroll adjustment, and proportional sensitivity. Works during freelook.
- **Autoclicker:** separate left/right rates, hold or toggle activation, and continuous block mining. It starts inactive and stops in screens, on focus loss, death, or disconnect.
- **Armor and totems:** native equipped-item icons, durability bars, and total carried totems. Move and scale the two widgets independently.

## Install

Install Fabric Loader **0.19.5 or newer** and Fabric API for **26.2**, then put the release JAR in your instance's `minecraft/mods` folder. The first release is built and tested with Fabric API **0.161.0+26.2**. [Mod Menu](https://modrinth.com/mod/modmenu) is optional and provides a settings entry.

Use **Mods → Neon → Configure** to open settings. Assign your controls under **Options → Controls → Key Binds → Neon**. All four bindings begin unassigned to preserve existing controls. The settings shortcut works without Mod Menu once assigned.

For the autoclicker, enable it in Neon settings, choose buttons and CPS, and assign its activation key. Hold mode is the default. After opening a screen or losing focus, release the activation key before activating again. Clicking uses vanilla input and respects normal game interaction limits.

HUD settings provide visibility, scale, position editing, and reset. Totems include inventory and offhand; the selected main-hand slot is counted only once. F1 hides both widgets.

Settings are stored in `config/neon.json`. Key bindings use Minecraft's normal options. No account, server-side mod, network connection, or YouTube setup is needed.

## Build

With JDK 25:

```powershell
.\gradlew.bat build --console=plain
```

The development build checks the code and installs `build/libs/neon-0.1.0.jar` as `neon.jar` in the local PrismLauncher **Vanilla** instance. The installed file is byte-checked against the build. Existing running games remain open; new code loads on the next game launch.

Use `-PskipInstall` to build without installing, or `-PneonInstallDir=<mods-directory>` to select another destination. Linux/macOS users can run `./gradlew build -PskipInstall`.

On Windows installations affected by Java's local socket initialization error, run the included `tools/build.ps1`; it applies a process-local TCP fallback before invoking the same Gradle build.

## Verification

`build` runs the regression suite and compiles the isolated live verification harness. The harness is excluded from the release JAR. See [verification instructions](docs/verification.md) for live-test setup and results.

YouTube chat is deferred. This release contains no chat integration or OAuth code.

## License

MIT. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
