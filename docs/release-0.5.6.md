# Neon 0.5.6 — search and chat efficiency

For Minecraft 26.2, Java 25 and Fabric. The release includes the exact verified runnable JAR, sources, checksums and packaged verification receipt.

- Search stays hidden until Ctrl+F in the current module/Settings category and HUD column. Ctrl+F reveals and focuses; Enter/Down reveals the first result without activating it; Escape clears then hides the empty field and restores the full list area. Clearing restores scrolling. Filters never change enabled states or saved settings.
- Chat reuses unchanged duplicate groups and clickable layouts, and batches glyphs in packed buffers. Original history, signatures, links, scrolling, source filtering and one-ping-per-accepted-message behavior remain intact.
- Expanded silent verification: incremental chat, search, native desktop paths and a configurable endurance run with repeated resource reloads and bounded-cache assertions.
- One consolidated design contract and actual-client HTML reference replace contradictory accumulated guidance. Historical snapshots remain available, and the old 0.5.5 installation note is resolved.

See [results and limits](improvements-results.md) for measured allocations/frame times, exact artifacts, test coverage and remaining physical-key/mixed-DPI gaps. Allocation reductions do not establish an FPS increase. Saved settings, palette, bindings and placements are preserved; existing Minecraft instances remain open.
