package me.xchro.neon.config;

import me.xchro.neon.core.BindMode;

public final class NeonConfig {
    public int version = 2;
    public Theme theme = new Theme();
    public BetterChat betterChat = new BetterChat();
    public static final class BetterChat {
        public boolean enabled = true, duplicates = true, animation = true, mentions = true;
        public boolean messagePing = true, mentionPing = true;
        public int animationMillis = 120, mentionColor = 0xffffcd5a;
        public double volume = .4;
        public BindMode mode = BindMode.TOGGLE;
        void sanitize() {
            animationMillis = Math.clamp(animationMillis, 50, 600);
            volume = finite(volume, .4, 0, 1);
            if (mode == null) mode = BindMode.TOGGLE;
        }
    }
    public enum Effects { ADAPTIVE, FULL }
    public static final class Theme {
        public int paletteVersion = 1;
        public Effects effects = Effects.ADAPTIVE;
        public double opacity = .94, soundVolume = .4;
        public boolean alphaEnabled;
        public int themeColor = 0xff84dce6;
        // Empty for legacy configs: introducing themes must not recolor a saved palette.
        public java.util.Set<ThemeColor> syncedColors = java.util.EnumSet.noneOf(ThemeColor.class);
        public boolean rainbow;
        public double rainbowSeconds = 12, rainbowSaturation = .59, rainbowBrightness = .59;
        public int background = 0xff07090e, cards = 0xff0d1119, text = 0xffe3eaf1,
            muted = 0xff83909f, accent = 0xff84dce6, outline = 0xff73c8d3, secondary = 0xff9985cc;
        public int color(ThemeColor role) { return color(role, System.currentTimeMillis()); }
        public int color(ThemeColor role, long millis) {
            return syncedColors.contains(role) ? role.resolve(primary(millis), role.local(this)) : role.local(this);
        }
        private int primary(long millis) {
            return rainbow ? java.awt.Color.HSBtoRGB((float)((millis % (rainbowSeconds * 1000)) / (rainbowSeconds * 1000)),
                (float)rainbowSaturation, (float)rainbowBrightness) : themeColor;
        }
        public void themeColor(int color) {
            if ((themeColor & 0xffffff) != (color & 0xffffff)) {
                rainbow = false;
                syncedColors = java.util.EnumSet.allOf(ThemeColor.class);
            }
            themeColor = color;
        }
        public void rainbow(boolean enabled) {
            rainbow = enabled;
            if (enabled) syncedColors = java.util.EnumSet.allOf(ThemeColor.class);
        }
        public void color(ThemeColor role, int color, boolean rgbChanged) {
            // Alpha-only edits retain the link, just as in Unity's picker.
            if (rgbChanged) syncedColors.remove(role);
            role.local(this, color);
        }
        public void sync(ThemeColor role, boolean enabled) {
            if (enabled) syncedColors.add(role);
            else { int current = color(role); syncedColors.remove(role); role.local(this, current); }
        }
        void upgradeSurfaces() {
            // Only replace the former stock RGB values; custom colors and alpha stay intact.
            if ((background & 0xffffff) == 0x0e131b) background = (background & 0xff000000) | 0x07090e;
            if ((cards & 0xffffff) == 0x121923) cards = (cards & 0xff000000) | 0x0d1119;
            paletteVersion = 1;
        }
        void sanitize() {
            opacity=finite(opacity,.94,.4,1);soundVolume=finite(soundVolume,.4,0,1);
            rainbowSeconds=finite(rainbowSeconds,12,1,30);
            rainbowSaturation=finite(rainbowSaturation,.59,0,1);rainbowBrightness=finite(rainbowBrightness,.59,.01,1);
            if (syncedColors == null) syncedColors = java.util.EnumSet.noneOf(ThemeColor.class);
            else syncedColors.remove(null);
            if (!alphaEnabled) { themeColor |= 0xff000000; background |= 0xff000000; cards |= 0xff000000; text |= 0xff000000; muted |= 0xff000000; accent |= 0xff000000; outline |= 0xff000000; secondary |= 0xff000000; }
        }
    }
    public boolean freelookEnabled = true, zoomEnabled = true;
    public BindMode freelookMode = BindMode.HOLD, motionBlurMode = BindMode.TOGGLE, totemPopsMode = BindMode.TOGGLE;
    // Missing fields retain the old activation choices, including legacy profile snapshots.
    public BindMode zoomMode, clickerMode;
    public BindMode zoomMode() { return zoomMode != null ? zoomMode : zoomToggle ? BindMode.TOGGLE : BindMode.HOLD; }
    public BindMode clickerMode() { return clickerMode != null ? clickerMode : toggleClicker ? BindMode.TOGGLE : BindMode.HOLD; }
    public double freelookDistance = 4;
    public int zoomMillis = 150;
    public double zoomFactor = 4;
    public boolean zoomWheel = true, zoomSensitivity = true;
    public boolean zoomToggle = false;
    public double zoomScrollStep = .5;
    public boolean motionBlurEnabled = false, motionBlurIncludeUi = true;
    public int motionBlurStrength = 10;
    public boolean totemPopsEnabled = true;
    public int totemPopsRedAt = 10;
    public boolean clickerEnabled = false, leftClick = true, rightClick = false, toggleClicker = false;
    public int leftIntervalTicks = 0, rightIntervalTicks = 0;
    public LeftAction leftAction = LeftAction.CLICK;
    public enum LeftAction { CLICK, HOLD }
    public boolean clickerEntitiesOnly = false;
    public Widget armor = new Widget(1, 0.90);
    // Offhand decoration remains the default; floating mode is arranged in the HUD editor.
    public Widget totems = new Widget(1, 1);
    public Widget fps = new Widget(1, 1);
    public boolean fpsAnimation = false, fpsBackground = false, fpsBorder = false, fpsShadow = true;
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
        freelookMode = BindMode.HOLD; zoomMode = null; motionBlurMode = BindMode.TOGGLE;
        freelookDistance = 4;
        zoomMillis = 150; zoomFactor = 4; zoomWheel = zoomSensitivity = true;
        zoomToggle = false; zoomScrollStep = .5;
        motionBlurEnabled = false; motionBlurIncludeUi = true; motionBlurStrength = 10;
    }
    public void resetClicker() {
        clickerMode = null;
        clickerEnabled = rightClick = toggleClicker = clickerEntitiesOnly = false;
        leftClick = true;
        leftIntervalTicks = rightIntervalTicks = 0; leftAction = LeftAction.CLICK;
    }
    public void resetHud() { resetArmor(); totems = new Widget(1, 1); fps = new Widget(1, 1); fpsAnimation = fpsBackground = fpsBorder = false; fpsShadow = true; totemFloating = false; editorGuides = true; editorSnap = false; hudScale=1;snapSensitivity=10; }
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
        if (betterChat == null) betterChat = new BetterChat();
        betterChat.sanitize();
        if (freelookMode == null) freelookMode = BindMode.HOLD;
        if (motionBlurMode == null) motionBlurMode = BindMode.TOGGLE;
        if (totemPopsMode == null) totemPopsMode = BindMode.TOGGLE;
        freelookDistance = finite(freelookDistance, 4, 1, 16);
        zoomMillis = Math.clamp(zoomMillis, 0, 500);
        zoomFactor = finite(zoomFactor, 4, 1, 16);
        zoomScrollStep = finite(zoomScrollStep, .5, .1, 4);
        motionBlurStrength = Math.clamp(motionBlurStrength, 0, 100);
        totemPopsRedAt = Math.clamp(totemPopsRedAt, 2, 20);
        leftIntervalTicks = Math.clamp(leftIntervalTicks, 0, 60); rightIntervalTicks = Math.clamp(rightIntervalTicks, 0, 60);
        if (leftAction == null) leftAction = LeftAction.CLICK;
        if (armor == null) armor = new Widget(1, .90);
        if (totems == null) totems = new Widget(1, 1);
        if (fps == null) fps = new Widget(1, 1);
        armor.sanitize(); totems.sanitize(); fps.sanitize();
        armorGap = Math.clamp(armorGap, 0, 8); armorWarningPercent = Math.clamp(armorWarningPercent, 1, 30);
        hudScale=finite(hudScale,1,.5,2);snapSensitivity=Math.clamp(snapSensitivity,1,24);
        if (armorAnchor == null) armorAnchor = ArmorAnchor.HOTBAR_LEFT;
    }
    private static double finite(double v, double fallback, double min, double max) { return Double.isFinite(v) ? Math.clamp(v, min, max) : fallback; }
}
