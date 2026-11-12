package me.xchro.neon.core;

/** A transient label; the control's semantic name and dimensions never change. */
public record ActionFeedback(String label,long started) {
    public double amount(long now) {
        double t=Math.clamp((now-started-900_000_000L)/120_000_000.0,0,1);
        return 1-t*t*(3-2*t);
    }
}
