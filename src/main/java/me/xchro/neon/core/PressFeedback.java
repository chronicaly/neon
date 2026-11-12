package me.xchro.neon.core;

/** Visual-only press envelope. Even a quick tap has one visible, bounded response. */
public final class PressFeedback {
    private final UiMotion motion = new UiMotion(0);
    private boolean held;
    private long pressed, releaseAt = Long.MAX_VALUE;
    public void press(long now) {
        if (held) return;
        value(now); held = true; pressed = now; releaseAt = Long.MAX_VALUE;
        motion.to(1, now, 50_000_000L);
    }
    public void release(long now) {
        if (!held) return;
        held = false; releaseAt = Math.max(now, pressed + 50_000_000L);
    }
    public double value(long now) {
        if (now >= releaseAt) { motion.to(0, releaseAt, 100_000_000L); releaseAt = Long.MAX_VALUE; }
        return motion.value(now, 0);
    }
    public void clear() { held = false; releaseAt = Long.MAX_VALUE; motion.snap(0); }
}
