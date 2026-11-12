package me.xchro.neon.core;

/** One continuous rectangle drives the shell, drawer clip and pointer geometry. */
public final class ExpandingMenu {
    public static final long DURATION = 340_000_000L;
    public static final int COLOR_WIDTH = 168;
    private final UiMotion reveal = new UiMotion(0);
    private final UiMotion colorReveal = new UiMotion(0);
    public void open(boolean open, long now) { reveal.to(open ? 1 : 0, now, DURATION); }
    public void color(boolean open, long now) { colorReveal.to(open ? 1 : 0, now, DURATION); }
    public void reveal() { reveal.snap(1); }
    public void revealColor() { reveal(); colorReveal.snap(1); }
    public void hideColor() { colorReveal.snap(0); }
    public double fraction(long now) { return reveal.value(now, DURATION); }
    public record Frame(double x, double width, double mainWidth, double drawerWidth, int fullDrawerWidth,
                        double colorWidth, int fullColorWidth, boolean replacesDetails) {}
    public Frame frame(int x, int width, int viewportWidth, long now) {
        double available = Math.max(1, viewportWidth - 28);
        int full = (int)Math.min(300, Math.max(80, available - 280));
        boolean replace = available < 280 + full + COLOR_WIDTH;
        int fullColor = Math.min(COLOR_WIDTH, (int)Math.max(80, available - 280));
        double color = colorReveal.value(now, DURATION);
        double drawer = full * fraction(now) * (replace ? 1 - color : 1);
        double colorWidth = fullColor * color;
        double extra = drawer + colorWidth;
        double main = Math.max(1, Math.min(width, available - extra));
        double total = main + extra;
        double left = Math.clamp(x, 14, Math.max(14, viewportWidth - total - 14));
        return new Frame(left, total, main, drawer, full, colorWidth, fullColor, replace);
    }
}
