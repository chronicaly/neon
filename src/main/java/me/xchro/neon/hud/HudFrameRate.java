package me.xchro.neon.hud;

/** Render-thread frame timing, published on a 60 Hz clock rather than Minecraft's one-second counter. */
public final class HudFrameRate {
    private long previous = Long.MIN_VALUE, slot = Long.MIN_VALUE, updates;
    private double averageNanos;
    private int fps;

    public void frame(long now) {
        if (previous != Long.MIN_VALUE && now <= previous) return;
        long elapsed = previous == Long.MIN_VALUE ? 0 : now - previous;
        previous = now;
        if (elapsed > 1_000_000_000L) { averageNanos = 0; fps = 0; }
        else if (elapsed > 0) {
            double weight = 1 - Math.exp(-elapsed / 100_000_000.0);
            averageNanos = averageNanos == 0 ? elapsed : averageNanos + weight * (elapsed - averageNanos);
        }
        // Integer time buckets avoid the drift caused by resetting a 16.67 ms timer every frame.
        long currentSlot = Math.floorDiv(now, 1_000_000_000L) * 60
                + Math.floorMod(now, 1_000_000_000L) * 60 / 1_000_000_000L;
        if (currentSlot != slot) {
            slot = currentSlot; updates++;
            fps = averageNanos > 0 ? (int)Math.round(1_000_000_000.0 / averageNanos) : 0;
        }
    }
    public int fps() { return fps; }
    public long updates() { return updates; }
}
