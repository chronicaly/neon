# Neon 0.1.2

Minecraft 26.2 / Java 25 / Fabric Loader 0.19.5+ / Fabric API for 26.2.

- Replaced CPS sliders with separate **0-60 tick intervals** for left and right clicks. The lowest value reads **Vanilla (0)** and uses vanilla weapon/use timing; positive values attempt one click every selected number of client ticks.
- Added **Left action: Click / Hold**, separate from activation-key Hold/Toggle. **Click** attacks without touching blocks. **Hold** keeps the attack button pressed for continuous mining and disables the left interval slider.
- Tick timing works in gameplay, chat, and the background. No wall-clock scheduling or catch-up click replay. Physical mouse input remains independent.
- Existing CPS configurations upgrade to vanilla timing and attack-only Click mode, retaining other settings and bindings.

Open **Mods → Neon → Configure → Autoclicker**. For combat, use **Left action: Click** and choose **Vanilla (0)** or a tick interval. For mining, choose **Hold**. To continue while typing or tabbed out, use **activation mode: Toggle**.

Download **neon-0.1.2.jar** for installation. The `sources` JAR is for development. Install only one Neon JAR per instance, then launch the game again to load the update.

Verified with 14 regression tests and 72 live checks using the Vanilla instance's Sodium/Iris mod set. See [verification details](https://github.com/chronicaly/neon/blob/v0.1.2/docs/verification.md).
