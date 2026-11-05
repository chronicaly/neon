package me.xchro.neon.core;

/** Client-tick scheduler. Zero delegates timing to vanilla; never replays missed clicks. */
public final class ClickClock {
    private long next = Long.MIN_VALUE;
    public boolean press(long tick, int interval, boolean vanillaReady) {
        if (tick < next || interval == 0 && !vanillaReady) return false;
        next = tick + Math.clamp(interval, 1, 60);
        return true;
    }
    public void reset() { next = Long.MIN_VALUE; }
}
