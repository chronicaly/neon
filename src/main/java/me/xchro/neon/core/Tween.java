package me.xchro.neon.core;

/** Finite, frame-rate independent smoothstep; retargeting preserves the current value. */
public final class Tween {
    private double from, target;
    private long started, duration;
    public double value(long now) {
        if (duration <= 0) return target;
        double t = Math.clamp((double) (now - started) / duration, 0, 1);
        return from + (target - from) * t * t * (3 - 2 * t);
    }
    public void to(double value, long now, int millis) {
        if (value == target) return;
        from = value(now); target = value; started = now; duration = millis * 1_000_000L;
    }
    public void set(double value) { from = target = value; duration = 0; }
    public static double angle(double from, double to, double blend) {
        double delta = ((to - from) % 360 + 540) % 360 - 180;
        return from + delta * blend;
    }
    public static double zoomFov(double degrees, double factor) {
        return Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(degrees) / 2) / factor));
    }
}
