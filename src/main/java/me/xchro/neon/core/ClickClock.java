package me.xchro.neon.core;

public final class ClickClock {
    private long next;
    public boolean press(long now, int cps) {
        if (now < next) return false;
        long period = 1_000_000_000L / Math.clamp(cps, 1, 20);
        next = next == 0 || now - next >= period ? now + period : next + period;
        return true;
    }
    public void reset() { next = 0; }
}
