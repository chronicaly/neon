package me.xchro.neon.smoke;

/** Intercept only the OS clipboard boundary; screenshots still use the real GPU/PNG pipeline. */
public final class ClipboardAudit {
    public record Capture(int width, int height, int[] pixels) {}
    public static volatile Capture latest;
    public static volatile int calls;
    public static volatile boolean fail;
    public static void copy(int width, int height, int[] pixels) {
        latest = new Capture(width, height, pixels.clone()); calls++;
        if (fail) throw new IllegalStateException("Verification clipboard busy");
    }
}
