package me.xchro.neon.hud;

/** Additive widget identities are stable in configs and profiles. */
public enum HudKind {
    COORDINATES("Coordinates", .02, .20),
    COMPASS("Compass", .5, .02),
    PING("Ping", 1, .92),
    SPEED("Speed", 1, .84),
    CPS("CPS", .02, .12),
    KEYSTROKES("Keystrokes", .02, .45),
    POTIONS("Potion effects", 1, .30),
    INVENTORY("Inventory", 1, .65),
    HELD_ITEM("Held item", .02, .75),
    CLOCK("Clock", 1, .02),
    SESSION("Session", 1, .10),
    BIOME("Biome", .02, .30),
    LIGHT("Light level", .02, .37),
    PACKS("Resource packs", 1, .18),
    SERVER("Server", .02, .02),
    MOVEMENT("Movement", .5, .85),
    WORLD_TIME("World time", .5, .18),
    ITEM_COUNTER("Item counter", .5, .76),
    SCOREBOARD("Scoreboard", 1, .45),
    MEMORY("Memory", .5, .28);
    public final String label;
    public final double x, y;
    HudKind(String label, double x, double y) { this.label=label; this.x=x; this.y=y; }
}
