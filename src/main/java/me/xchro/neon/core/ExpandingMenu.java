package me.xchro.neon.core;

/** One continuous rectangle drives the shell, drawer clip and pointer geometry. */
public final class ExpandingMenu {
    public static final long DURATION = 340_000_000L;
    private final TimedValue reveal = new TimedValue(0);
    public void open(boolean open, long now) { reveal.to(open ? 1 : 0, now, DURATION); }
    public void reveal() { reveal.snap(1); }
    public double fraction(long now) { return reveal.value(now, DURATION); }
    public record Frame(double x, double width, double mainWidth, double drawerWidth, int fullDrawerWidth) {}
    public Frame frame(int x, int width, int viewportWidth, long now) {
        double available = Math.max(1, viewportWidth - 28);
        int full = (int)Math.min(300, Math.max(80, available - 280));
        double extra = full * fraction(now);
        double main = Math.max(1, Math.min(width, available - extra));
        double total = main + extra;
        double left = Math.clamp(x, 14, Math.max(14, viewportWidth - total - 14));
        return new Frame(left, total, main, extra, full);
    }
}
