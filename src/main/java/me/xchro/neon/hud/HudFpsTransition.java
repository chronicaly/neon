package me.xchro.neon.hud;

/** Frame-rate-independent easing of the displayed number; the text itself stays still and opaque. */
public final class HudFpsTransition {
    private static final double RESPONSE_NANOS = 120_000_000.0;
    private long updatedAt;
    private boolean initialized;
    private double displayed;

    public void update(int fps, long now) {
        fps = Math.max(0, fps);
        if (!initialized || fps == 0 || displayed == 0 || now < updatedAt || now - updatedAt > 1_000_000_000L) {
            displayed = fps; initialized = true;
        } else {
            double weight = -Math.expm1(-(now - updatedAt) / RESPONSE_NANOS);
            displayed += (fps - displayed) * weight;
        }
        updatedAt = now;
    }

    public int value() { return (int)Math.round(displayed); }
}
