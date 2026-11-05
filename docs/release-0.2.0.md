# Neon 0.2.0

Minecraft 26.2 / Java 25 / Fabric Loader 0.19.5+ / Fabric API for 26.2.

- **Exact values:** click a slider's number, or press Enter when focused, to type a precise value. Invalid/out-of-range entries cannot be applied.
- **Profiles:** Combat, Mining, and Building presets plus named snapshots you can save, load, update, and delete. Profiles preserve global bindings and never start automation on load.
- **Autoclicker safeguards:** optional entities-only attacks and tool protection with configurable remaining durability. A stopped autoclicker explains the item and reason in the action bar and requires fresh activation.
- **HUD editor:** optional guides and snapping, exact X/Y and scale entry, Undo, a hideable toolbar, and a low-durability preview that leaves actual equipment untouched.
- **Undo/reset:** shared settings history across pages, profiles, bindings, numeric entry, and HUD editing; complete slider drags undo as one change. Per-page reset leaves other sections intact.
- **Zoom:** choose Hold or Toggle, and set an exact scroll-wheel increment. Focus loss and menus still cancel zoom cleanly.

Open **Mods → Neon → Configure**. Profiles, Undo, and Reset page are in the footer. The HUD editor uses **H** to hide/show its controls and **Ctrl+Z** to undo. Existing settings and bindings are retained.

Download **neon-0.2.0.jar** for installation. The `sources` JAR is for development. Keep only one Neon JAR per instance and relaunch Minecraft to load the update.

Verified with 19 regression tests and 110 live checks using the Vanilla instance's Sodium/Iris mod set. See [verification details](https://github.com/chronicaly/neon/blob/v0.2.0/docs/verification.md).
