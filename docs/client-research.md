# Client expansion research — 16 November 2026

Neon is being expanded as a polished everyday/PvP presentation client. The user explicitly requested more HUD elements, superseding the earlier three-element limit. Preserve the existing Combat, Visuals and Settings navigation, compact drawers, direct HUD placement, saved palette and all premium interaction timings.

## Sources and decisions

| Client | Primary evidence reviewed | Direction for Neon |
| --- | --- | --- |
| Lunar | [Features](https://www.lunarclient.com/features), [Keystrokes](https://www.lunarclient.com/news/lunar-clients-keystrokes-mod-explained), [July 2025 improvements](https://www.lunarclient.com/news/lunar-client-monthly-recap-16-july-2025) | Configurable input displays, coordinates, camera controls, armor information and direct HUD placement. Preserve actual rebound controls. |
| Badlion | [Official mod catalog](https://www.badlion.net/free-minecraft-mods), [FAQ](https://www.badlion.net/faq) | Readable status widgets and visual customization; no server automation added. The main /mods and attempted support URLs were unavailable; the official catalog and FAQ were read successfully. |
| Feather | [Official client](https://feathermc.com/a/), [third-party sources](https://feathermc.com/third-party) | Consistent HUD editing and compatibility with a normal Fabric installation. No launcher, account or social-network dependency. |
| Laby | [Official client](https://laby.net/client), [HUD documentation](https://laby.net/client/mods/hud) | Useful compact information with individual placement, rather than a permanently crowded dashboard. |
| Meteor | [Public source](https://github.com/MeteorDevelopment/meteor-client), revision `474da7c33d391109a4a1feea288ea93ea696cfa9`; inspected `CompassHud.java`, `InventoryHud.java`, `PotionTimersHud.java`, and license | Study inventory indexing, effect lifetime/empty-state handling, independent HUD scale and editor previews. Independent Neon implementations; GPL code/assets are not incorporated into the MIT release. |
| LiquidBounce | [Public render-module directory](https://github.com/CCBlueX/LiquidBounce/tree/main/src/main/kotlin/net/ccbluex/liquidbounce/features/module/modules/render) | Catalog comparison only. Combat automation, ESP, X-ray and similar features are outside this expansion. |
| Sodium | [Official releases](https://github.com/CaffeineMC/sodium/releases) | Keep renderer compatibility in mind; do not claim an FPS increase from cosmetic changes. |

This is a representative survey, not a claim that every client or proprietary implementation was examined. Lunar/Badlion/Feather/Laby public product documentation informs behavior; available official public source informs engineering. No leaked proprietary source is used.

## Delivery sequence

1. Improve existing HUD consistency, preserving saved geometry, item rendering and FPS colors. Build and verify before adding modules.
2. Add features individually with settings/profile/Undo coverage and a real Gradle build after each completed change set.
3. Target keystrokes, CPS, coordinates, compass, ping, speed, potion effects, inventory, held-item durability, clock, session timer, biome, light level, resource-pack display and server information; then useful visual controls where verified APIs permit.
4. Inspect actual module/settings/editor pages at default/minimum sizes; exercise disabled widgets, input, profiles, screen changes and small displays in the silent harness.
5. Measure allocations and frame times, publish results with limits, rewrite README, run the exact-JAR release gate, install checked bytes and publish a GitHub release.

New widgets start disabled so upgrades preserve the current HUD. Armor, floating totems and optional FPS borders share a thin Neon outline and restrained traveling highlight. The editor column retains its quiet static border. Palette/alpha and FPS-specific color overrides remain authoritative.
