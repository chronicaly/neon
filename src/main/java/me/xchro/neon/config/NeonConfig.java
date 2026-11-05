package me.xchro.neon.config;

public final class NeonConfig {
    public int version = 1;
    public int zoomMillis = 150;
    public double zoomFactor = 4;
    public boolean zoomWheel = true, zoomSensitivity = true;
    public boolean clickerEnabled = false, leftClick = true, rightClick = false, toggleClicker = false, continuousMining = true;
    public int leftCps = 8, rightCps = 8;
    public boolean clickerInChat = true, clickerInBackground = true;
    public Widget armor = new Widget(1, 0.90);
    // Retained for compatibility with 0.1.0: only visibility applies to the offhand count.
    public Widget totems = new Widget(1, 1);
    public boolean armorVertical = false, armorReverse = false, armorSlots = true, armorWarnings = true;
    public int armorGap = 0, armorWarningPercent = 10;
    public ArmorAnchor armorAnchor = ArmorAnchor.CUSTOM;
    public enum ArmorAnchor {
        CUSTOM("Custom"), TOP_LEFT("Top left"), TOP_CENTER("Top center"), TOP_RIGHT("Top right"),
        CENTER_LEFT("Center left"), CENTER_RIGHT("Center right"), BOTTOM_LEFT("Bottom left"),
        BOTTOM_RIGHT("Bottom right"), HOTBAR_LEFT("Hotbar left"), HOTBAR_RIGHT("Hotbar right");
        public final String label;
        ArmorAnchor(String label) { this.label = label; }
    }
    public void resetArmor() {
        armor = new Widget(1, .90); armorAnchor = ArmorAnchor.CUSTOM;
        armorVertical = armorReverse = false; armorSlots = armorWarnings = true;
        armorGap = 0; armorWarningPercent = 10;
    }
    public static final class Widget {
        public boolean visible = true;
        public double x, y, scale = 1;
        public Widget(double x, double y) { this.x = x; this.y = y; }
        public void sanitize() { x = finite(x, 1, 0, 1); y = finite(y, 1, 0, 1); scale = finite(scale, 1, .5, 3); }
    }
    public void sanitize() {
        zoomMillis = Math.clamp(zoomMillis, 0, 500);
        zoomFactor = finite(zoomFactor, 4, 1, 16);
        leftCps = Math.clamp(leftCps, 1, 20); rightCps = Math.clamp(rightCps, 1, 20);
        if (armor == null) armor = new Widget(1, .90);
        if (totems == null) totems = new Widget(1, 1);
        armor.sanitize(); totems.sanitize();
        armorGap = Math.clamp(armorGap, 0, 8); armorWarningPercent = Math.clamp(armorWarningPercent, 1, 30);
        if (armorAnchor == null) armorAnchor = ArmorAnchor.CUSTOM;
    }
    private static double finite(double v, double fallback, double min, double max) { return Double.isFinite(v) ? Math.clamp(v, min, max) : fallback; }
}
