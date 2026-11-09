package me.xchro.neon.core;

/** Unity's 60 Hz strength curve, evaluated using actual rendered-frame time. */
public final class FrameHistory {
    private long lastFrame;
    private boolean ready;

    public float weight(long now, int strength) {
        long elapsed = now - lastFrame;
        boolean usable = ready && elapsed > 0 && elapsed <= 250_000_000L;
        lastFrame = now;
        ready = true;
        if (!usable) return 0;
        double base = Math.clamp(strength, 0, 98) / 100.0;
        return (float) Math.pow(base, elapsed * 60.0 / 1_000_000_000.0);
    }

    public void reset() { ready = false; }
}
