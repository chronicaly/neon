package me.xchro.neon.config;

public final class NeonConfig {
    public int version = 2;
    public int zoomMillis = 150;
    public double zoomFactor = 4;
    public boolean zoomWheel = true, zoomSensitivity = true;
    public boolean zoomToggle = false;
    public double zoomScrollStep = .5;
    public boolean motionBlurEnabled = false, motionBlurIncludeUi = true;
    public int motionBlurStrength = 75;
    public boolean clickerEnabled = false, leftClick = true, rightClick = false, toggleClicker = false;
    public int leftIntervalTicks = 0, rightIntervalTicks = 0;
    public LeftAction leftAction = LeftAction.CLICK;
    public enum LeftAction { CLICK, HOLD }
    public boolean clickerInChat = true, clickerInBackground = true;
    public boolean clickerEntitiesOnly = false, clickerProtectTools = false;
    public int clickerMinDurability = 5;
    public Widget armor = new Widget(1, 0.90);
    // Retained for compatibility with 0.1.0: only visibility applies to the offhand count.
    public Widget totems = new Widget(1, 1);
    public boolean armorVertical = false, armorReverse = false, armorSlots = true, armorWarnings = true;
    public int armorGap = 0, armorWarningPercent = 10;
    public ArmorAnchor armorAnchor = ArmorAnchor.CUSTOM;
    public boolean editorGuides = true, editorSnap = false;
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
    public void resetCamera() {
        zoomMillis = 150; zoomFactor = 4; zoomWheel = zoomSensitivity = true;
        zoomToggle = false; zoomScrollStep = .5;
        motionBlurEnabled = false; motionBlurIncludeUi = true; motionBlurStrength = 75;
    }
    public void resetClicker() {
        clickerEnabled = rightClick = toggleClicker = clickerEntitiesOnly = clickerProtectTools = false;
        leftClick = clickerInChat = clickerInBackground = true;
        leftIntervalTicks = rightIntervalTicks = 0; leftAction = LeftAction.CLICK; clickerMinDurability = 5;
    }
    public void resetHud() { resetArmor(); totems = new Widget(1, 1); editorGuides = true; editorSnap = false; }
    public static final class Widget {
        public boolean visible = true;
        public double x, y, scale = 1;
        public Widget(double x, double y) { this.x = x; this.y = y; }
        public void sanitize() { x = finite(x, 1, 0, 1); y = finite(y, 1, 0, 1); scale = finite(scale, 1, .5, 3); }
    }
    public void sanitize() {
        version = 2;
        zoomMillis = Math.clamp(zoomMillis, 0, 500);
        zoomFactor = finite(zoomFactor, 4, 1, 16);
        zoomScrollStep = finite(zoomScrollStep, .5, .1, 4);
        motionBlurStrength = Math.clamp(motionBlurStrength, 0, 100);
        leftIntervalTicks = Math.clamp(leftIntervalTicks, 0, 60); rightIntervalTicks = Math.clamp(rightIntervalTicks, 0, 60);
        if (leftAction == null) leftAction = LeftAction.CLICK;
        clickerMinDurability = Math.clamp(clickerMinDurability, 2, 100);
        if (armor == null) armor = new Widget(1, .90);
        if (totems == null) totems = new Widget(1, 1);
        armor.sanitize(); totems.sanitize();
        armorGap = Math.clamp(armorGap, 0, 8); armorWarningPercent = Math.clamp(armorWarningPercent, 1, 30);
        if (armorAnchor == null) armorAnchor = ArmorAnchor.CUSTOM;
    }
    private static double finite(double v, double fallback, double min, double max) { return Double.isFinite(v) ? Math.clamp(v, min, max) : fallback; }
}
