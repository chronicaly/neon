package me.xchro.neon.config;

public final class NeonConfig {
    public int version = 1;
    public int freelookMillis = 250, zoomMillis = 150;
    public double zoomFactor = 4;
    public boolean zoomWheel = true, zoomSensitivity = true;
    public boolean clickerEnabled = false, leftClick = true, rightClick = false, toggleClicker = false, continuousMining = true;
    public int leftCps = 8, rightCps = 8;
    public Widget armor = new Widget(1, 0.90), totems = new Widget(1, 1);
    public static final class Widget {
        public boolean visible = true;
        public double x, y, scale = 1;
        public Widget(double x, double y) { this.x = x; this.y = y; }
        public void sanitize() { x = finite(x, 1, 0, 1); y = finite(y, 1, 0, 1); scale = finite(scale, 1, .5, 3); }
    }
    public void sanitize() {
        freelookMillis = Math.clamp(freelookMillis, 0, 500); zoomMillis = Math.clamp(zoomMillis, 0, 500);
        zoomFactor = finite(zoomFactor, 4, 1, 16);
        leftCps = Math.clamp(leftCps, 1, 20); rightCps = Math.clamp(rightCps, 1, 20);
        if (armor == null) armor = new Widget(1, .90);
        if (totems == null) totems = new Widget(1, 1);
        armor.sanitize(); totems.sanitize();
    }
    private static double finite(double v, double fallback, double min, double max) { return Double.isFinite(v) ? Math.clamp(v, min, max) : fallback; }
}
