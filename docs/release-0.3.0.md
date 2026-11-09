# Neon 0.3.0

Minecraft 26.2 / Java 25 / Fabric Loader 0.19.5+ / Fabric API for 26.2.

- **Unity motion blur:** the original frame accumulation renderer and shaders, integrated into Neon. Enable it under **Camera → Motion blur**. Strength ranges from 0-100%, with Unity's 75% default; the effect starts disabled.
- **Include UI:** blur the whole frame or keep the HUD and in-game screens sharp. Neon's settings, profiles, numeric dialogs, and HUD editor always stay clear. Works with freelook and zoom.
- **History and cleanup:** fresh frames after focus/screen/perspective changes, resizing, teleports, and long stalls. Disabling, zero strength, disconnect, and shutdown release GPU resources. Settings work with exact entry, Undo, camera resets, and profiles.
- **Native menu overview:** Camera, Autoclicker, HUD, Controls, and Profiles; keyboard navigation, narrator labels, and remembered scroll positions. The default menu key is **P**. Existing custom bindings, including Insert, are preserved.

Download **neon-0.3.0.jar** for installation. The `sources` JAR is for development. Keep one Neon JAR per instance and relaunch Minecraft to load the update.

Verified with **24 unit tests** and **178 combined live checks**, including GPU pixel readback tests for Unity's blending curve. Motion blur passed on Vulkan and OpenGL, and the final JAR passed the combined suite with **Complementary Reimagined r5.9.3** on OpenGL and Vanilla's Sodium/Iris mod set. See [verification details](https://github.com/chronicaly/neon/blob/v0.3.0/docs/verification.md).
