package me.xchro.neon.core;

/** Retargetable, bounded ease-out. State changes never wait for the visual transition. */
public final class TimedValue {
    private double from, target;
    private long started = Long.MIN_VALUE;
    public TimedValue(double initial) { from = target = initial; }
    public double value(long now, long durationNanos) {
        if (started == Long.MIN_VALUE) return target;
        double t = Math.clamp((now - started) / (double) durationNanos, 0, 1);
        return from + (target - from) * (1 - Math.pow(1 - t, 3));
    }
    public double to(double next, long now, long durationNanos) {
        if (next != target) { from = value(now, durationNanos); target = next; started = now; }
        return value(now, durationNanos);
    }
    public void snap(double next) { from = target = next; started = Long.MIN_VALUE; }
}
