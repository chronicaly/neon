package me.xchro.neon.config;

public final class NeonConfig {
    public int version = 2;
    public Theme theme = new Theme();
    public enum Effects { ADAPTIVE, FULL }
    public static final class Theme {
        public Effects effects = Effects.ADAPTIVE;
        public double opacity = .94, soundVolume = .4;
        public boolean alphaEnabled;
        public int background = 0xff0e131b, cards = 0xff121923, text = 0xffe3eaf1,
            muted = 0xff83909f, accent = 0xff84dce6, outline = 0xff73c8d3, secondary = 0xff9985cc;
        void sanitize() { opacity=finite(opacity,.94,.4,1);soundVolume=finite(soundVolume,.4,0,1); if (!alphaEnabled) { background |= 0xff000000; cards |= 0xff000000; text |= 0xff000000; muted |= 0xff000000; accent |= 0xff000000; outline |= 0xff000000; secondary |= 0xff000000; } }
    }
    public boolean freelookEnabled = true, zoomEnabled = true;
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
    public boolean clickerEntitiesOnly = false;
    public Widget armor = new Widget(1, 0.90);
    // Offhand decoration remains the default; floating mode is arranged in the HUD editor.
    public Widget totems = new Widget(1, 1);
    public boolean totemFloating = false;
    public boolean armorVertical = false, armorReverse = false, armorSlots = true, armorWarnings = true;
    public int armorGap = 0, armorWarningPercent = 10;
    public ArmorAnchor armorAnchor = ArmorAnchor.HOTBAR_LEFT;
    public boolean editorGuides = true, editorSnap = false;
    public double hudScale = 1;
    public int snapSensitivity = 10;
    public enum ArmorAnchor {
        CUSTOM("Custom"), TOP_LEFT("Top left"), TOP_CENTER("Top center"), TOP_RIGHT("Top right"),
        CENTER_LEFT("Center left"), CENTER_RIGHT("Center right"), BOTTOM_LEFT("Bottom left"),
        BOTTOM_RIGHT("Bottom right"), HOTBAR_LEFT("Hotbar left"), HOTBAR_RIGHT("Hotbar right");
        public final String label;
        ArmorAnchor(String label) { this.label = label; }
    }
    public void resetArmor() {
        armor = new Widget(1, .90); armorAnchor = ArmorAnchor.HOTBAR_LEFT;
        armorVertical = armorReverse = false; armorSlots = armorWarnings = true;
        armorGap = 0; armorWarningPercent = 10;
    }
    public void resetCamera() {
        freelookEnabled = zoomEnabled = true;
        zoomMillis = 150; zoomFactor = 4; zoomWheel = zoomSensitivity = true;
        zoomToggle = false; zoomScrollStep = .5;
        motionBlurEnabled = false; motionBlurIncludeUi = true; motionBlurStrength = 75;
    }
    public void resetClicker() {
        clickerEnabled = rightClick = toggleClicker = clickerEntitiesOnly = false;
        leftClick = true;
        leftIntervalTicks = rightIntervalTicks = 0; leftAction = LeftAction.CLICK;
    }
    public void resetHud() { resetArmor(); totems = new Widget(1, 1); totemFloating = false; editorGuides = true; editorSnap = false; hudScale=1;snapSensitivity=10; }
    public static final class Widget {
        public boolean visible = true;
        public double x, y, scale = 1;
        public Widget(double x, double y) { this.x = x; this.y = y; }
        public void sanitize() { x = finite(x, 1, 0, 1); y = finite(y, 1, 0, 1); scale = finite(scale, 1, .5, 3); }
    }
    public void sanitize() {
        version = 2;
        if (theme == null) theme = new Theme();
        if (theme.effects == null) theme.effects = Effects.ADAPTIVE;
        theme.sanitize();
        zoomMillis = Math.clamp(zoomMillis, 0, 500);
        zoomFactor = finite(zoomFactor, 4, 1, 16);
        zoomScrollStep = finite(zoomScrollStep, .5, .1, 4);
        motionBlurStrength = Math.clamp(motionBlurStrength, 0, 100);
        leftIntervalTicks = Math.clamp(leftIntervalTicks, 0, 60); rightIntervalTicks = Math.clamp(rightIntervalTicks, 0, 60);
        if (leftAction == null) leftAction = LeftAction.CLICK;
        if (armor == null) armor = new Widget(1, .90);
        if (totems == null) totems = new Widget(1, 1);
        armor.sanitize(); totems.sanitize();
        armorGap = Math.clamp(armorGap, 0, 8); armorWarningPercent = Math.clamp(armorWarningPercent, 1, 30);
        hudScale=finite(hudScale,1,.5,2);snapSensitivity=Math.clamp(snapSensitivity,1,24);
        if (armorAnchor == null) armorAnchor = ArmorAnchor.HOTBAR_LEFT;
    }
    private static double finite(double v, double fallback, double min, double max) { return Double.isFinite(v) ? Math.clamp(v, min, max) : fallback; }
}
