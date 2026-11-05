# Neon 0.1.1

Minecraft 26.2 / Java 25 / Fabric Loader 0.19.5+ / Fabric API for 26.2.

- Removed freelook transition animation. Entry, release, and perspective restoration are immediate; smooth zoom remains.
- Rebuilt settings with native tabs, scrolling, tooltips, and direct key assignment with conflict highlighting.
- Reworked armor HUD with horizontal/vertical layouts, reversible order, resource-pack-aware vanilla slot frames, low-durability warnings, placement presets, and no forced screen padding. Drag, nudge with arrows, or resize with the wheel in the position editor.
- Moved the inventory + offhand totem total onto the existing offhand totem. No separate totem icon.
- Automatic attacks wait for full vanilla weapon cooldown; right click respects vanilla's use delay. CPS settings are upper limits. Continuous mining still uses vanilla breaking behavior.
- Toggle activation can continue while typing in chat and while tabbed out. Separate settings control each. Typing cannot activate the autoclicker; inventory/other menus, death, and disconnect stop it.

Open **Mods → Neon → Configure**. Bind controls in the **Controls** tab. For chat/background clicking, enable the autoclicker, select **Toggle**, and activate it in gameplay first. All bindings start unassigned; existing bindings and settings are preserved.

Download **neon-0.1.1.jar** for installation. The `sources` JAR is for development. Install only one Neon JAR per instance, then launch the game again to load the update.

Verified with 11 regression tests and 58 live checks using the Vanilla instance's Sodium/Iris mod set. See [verification details](https://github.com/chronicaly/neon/blob/v0.1.1/docs/verification.md). Armor HUD presentation is inspired by [uku3lig/armor-hud](https://github.com/uku3lig/armor-hud), implemented independently.
