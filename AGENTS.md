# Neon development
- Target Minecraft 26.2, Java 25, and unobfuscated Fabric Loom.
- Run an actual Gradle build after each completed code/configuration change set.
- The build installs only a successfully checked runnable JAR into PrismLauncher Vanilla.
- Keep running Minecraft instances open. Never install a stale JAR after a failed build.
- Preserve Unity's separate checkout and installed artifacts.
- Keep the interface vanilla and the HUD limited to armor and totem widgets.
