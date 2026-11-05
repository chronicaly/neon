# Neon 0.1.0

First release for **Minecraft 26.2**, **Java 25**, and **Fabric Loader 0.19.5+**. Requires Fabric API for 26.2; tested with 0.161.0+26.2. Mod Menu is optional.

- Smooth hold-to-freelook, including eased first/third-person transitions and collision with blocks.
- Smooth adjustable zoom, scroll adjustment, and proportional sensitivity; works with freelook.
- Left/right autoclicker with separate CPS, hold/toggle activation, continuous mining, and automatic stopping in menus or on focus loss.
- Movable, scalable armor and totem widgets using native Minecraft item icons, durability bars, and text.
- Vanilla settings pages, saved preferences, and Mod Menu integration.

Open **Mods → Neon → Configure**. Assign controls in **Options → Controls → Key Binds → Neon**; all bindings start unassigned. Enable the autoclicker in settings before using its activation key.

Download **neon-0.1.0.jar** for installation. The `sources` JAR is for development. Install only one Neon JAR per mods directory.

Validation: actual Gradle build, 10 regression tests, and 33 live checks using the packaged JAR on Vulkan with the Vanilla mod set, including Sodium and Iris. External shader packs were not part of the live test. YouTube integration is deferred.
