package me.xchro.neon.core;

/** Analytic critically damped UI response. Targets are normalized; camera motion is separate. */
public final class UiMotion {
    private double from, target, speed;
    private long started = Long.MIN_VALUE, duration;
    public UiMotion(double initial) { from = target = initial; }
    public double value(long now, long ignoredDuration) {
        if (started == Long.MIN_VALUE || now - started >= duration) return target;
        if (now <= started) return from;
        double t = Math.max(0, (now - started) / 1e9), omega = 10e9 / duration;
        double offset = from - target;
        return Math.clamp(target + (offset + (speed + omega * offset) * t) * Math.exp(-omega * t), 0, 1);
    }
    public double velocity(long now) {
        if (started == Long.MIN_VALUE || now - started >= duration) return 0;
        if (now <= started) return speed;
        double t = Math.max(0, (now - started) / 1e9), omega = 10e9 / duration;
        return (speed - omega * (speed + omega * (from - target)) * t) * Math.exp(-omega * t);
    }
    public double to(double next, long now, long nanos) {
        if (next != target) {
            double current = value(now, 0), currentSpeed = velocity(now);
            from = current; speed = currentSpeed; target = Math.clamp(next, 0, 1); started = now; duration = Math.max(1, nanos);
        }
        return value(now, 0);
    }
    public void snap(double next) { from = target = next; speed = 0; started = Long.MIN_VALUE; }
}
