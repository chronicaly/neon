package me.xchro.neon.config;

import me.xchro.neon.core.BindMode;

public final class NeonConfig {
    public int version = 2;
    public Theme theme = new Theme();
    public BetterChat betterChat = new BetterChat();
    public BetterScreenshot betterScreenshot = new BetterScreenshot();
    public java.util.Map<me.xchro.neon.module.ClientModule, ModuleSettings> modules = new java.util.EnumMap<>(me.xchro.neon.module.ClientModule.class);
    public static final class ModuleSettings {
        public boolean enabled;
        public BindMode mode = BindMode.TOGGLE;
    }
    public ModuleSettings module(me.xchro.neon.module.ClientModule kind) { return modules.computeIfAbsent(kind,key->new ModuleSettings()); }
    public Crosshair crosshair = new Crosshair();
    public enum HudFont { INTER, MINECRAFT }
    public HudFont hudFont=HudFont.INTER;
    public String clientFont="Inter";
    public boolean sprintResume, sneakResume;
    public double fovEffectStrength, hurtCameraStrength;
    public double viewBobbingStrength=.3;
    public double fireLowering=.4;
    public double glintStrength=.6, glintSpeed=.35;
    public Integer hitColor;
    public double hitColorStrength=.3;
    public int visualTime=6000;
    public enum Weather { CLEAR, RAIN }
    public Weather weather=Weather.CLEAR;
    public double weatherIntensity=.6;
    public double fullbrightStrength=1;
    public boolean hidePumpkin=true, hidePortal=true, hideVignette=true;
    public Integer blockOutlineColor;
    public double blockOutlineWidth=2;
    public enum BlockOutlineMode { OUTLINE, OUTLINE_FILL, FILL, GLOW, WIREFRAME }
    public BlockOutlineMode blockOutlineMode=BlockOutlineMode.OUTLINE;
    public double blockOutlineFill=.18;
    public boolean blockOutlineGradient=true,blockOutlineGlow;
    public int blockOutlineColor() { return blockOutlineColor==null?theme.color(ThemeColor.ACCENT):blockOutlineColor; }
    public int hitColor() { return hitColor==null?theme.color(ThemeColor.ACCENT):hitColor; }
    public Viewmodel viewmodel=new Viewmodel();
    public static final class Viewmodel {
        public double horizontal, vertical=-.12, depth, scale=1;
        public boolean offhand=true;
        void sanitize() { horizontal=finite(horizontal,0,-1,1);vertical=finite(vertical,-.12,-1,1);depth=finite(depth,0,-1,1);scale=finite(scale,1,.5,1.5); }
    }
    public static final class Crosshair {
        public me.xchro.neon.core.CrosshairRaster.Shape shape=me.xchro.neon.core.CrosshairRaster.Shape.CROSS;
        public double length=6, gap=3, thickness=1, outlineWidth=.5;
        public boolean dot, outline=true;
        public Integer color;
        public int color(Theme theme) { return color==null?theme.color(ThemeColor.ACCENT):color; }
        void sanitize() {
            if(shape==null)shape=me.xchro.neon.core.CrosshairRaster.Shape.CROSS;
            length=finite(length,6,1,20);gap=finite(gap,3,0,20);thickness=finite(thickness,1,.5,6);outlineWidth=finite(outlineWidth,.5,.25,2);
        }
    }
    public static final class BetterScreenshot {
        public boolean enabled, notify = true;
        public BindMode mode = BindMode.TOGGLE;
        void sanitize() { if (mode == null) mode = BindMode.TOGGLE; }
    }
    public static final class BetterChat {
        public boolean enabled = true, duplicates = true, animation = true, mentions = true;
        public boolean messagePing = true, mentionPing = true;
        public boolean clientFont = true, inputFrame = true;
        public boolean messageBar = true;
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
    public java.util.Map<FpsColor, Integer> fpsColors = new java.util.EnumMap<>(FpsColor.class);
    public boolean totemFloating = false;
    public boolean armorVertical = false, armorReverse = false, armorSlots = true, armorWarnings = true;
    public int armorGap = 0, armorWarningPercent = 10;
    public ArmorAnchor armorAnchor = ArmorAnchor.HOTBAR_LEFT;
    public boolean editorGuides = true, editorSnap = false;
    public double hudScale = 1;
    public boolean hudNeonPanels = true, hudAnimatedOutline = true;
    public boolean hudSurfaces = true, hudBackgrounds = true, hudOutlines = true;
    public boolean coordinatesDimension = true, coordinatesConversion;
    public boolean compassDegrees = true;
    public boolean keystrokesMouse = true, keystrokesJump = true;
    public int potionMaxRows = 8;
    public boolean inventoryHotbar, inventorySlots = true;
    public boolean heldItemOffhand = true, heldItemPercent;
    public boolean clock24Hour = true, clockSeconds;
    public boolean worldTimeDay=true;
    public enum CounterItem {
        HELD("Held item"), PEARLS("Pearls"), ARROWS("Arrows"), ROCKETS("Rockets"), GOLDEN_APPLES("Golden apples"),
        ENCHANTED_APPLES("Enchanted apples"), EXPERIENCE("Experience bottles"), OBSIDIAN("Obsidian"), DIAMONDS("Diamonds");
        public final String label;
        CounterItem(String label) { this.label=label; }
    }
    public CounterItem counterItem=CounterItem.PEARLS;
    public boolean counterOffhand=true;
    public boolean scoreboardTitle=true, scoreboardScores=true;
    public int scoreboardRows=15;
    public boolean lightSky = true;
    public int packMaxRows = 3;
    public boolean serverAddress, serverPlayers = true;
    public java.util.Map<me.xchro.neon.hud.HudKind, HudWidget> hudWidgets = new java.util.EnumMap<>(me.xchro.neon.hud.HudKind.class);
    public static final class HudWidget {
        public Widget placement = new Widget(.02,.2);
        public boolean background = true, border = true, shadow;
        public HudWidget() { placement.visible = false; }
    }
    public HudWidget hud(me.xchro.neon.hud.HudKind kind) {
        return hudWidgets.computeIfAbsent(kind, key -> {
            var value=new HudWidget(); value.placement.x=key.x; value.placement.y=key.y; return value;
        });
    }
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
    public void resetHud() {
        hudFont=HudFont.INTER;
        resetArmor(); totems = new Widget(1, 1); fps = new Widget(1, 1); fpsColors.clear();
        fpsAnimation = fpsBackground = fpsBorder = false; fpsShadow = true; totemFloating = false;
        editorGuides = true; editorSnap = false; hudScale=1;snapSensitivity=10; hudWidgets.clear();
        hudNeonPanels=hudAnimatedOutline=coordinatesDimension=compassDegrees=true;
        hudSurfaces=hudBackgrounds=hudOutlines=true;
        keystrokesMouse=keystrokesJump=inventorySlots=heldItemOffhand=clock24Hour=lightSky=serverPlayers=true;
        coordinatesConversion=inventoryHotbar=heldItemPercent=clockSeconds=serverAddress=false;
        potionMaxRows=8;packMaxRows=3;
        worldTimeDay=true;
        counterItem=CounterItem.PEARLS;counterOffhand=true;
        scoreboardTitle=scoreboardScores=true;scoreboardRows=15;
    }
    public static final class Widget {
        public boolean visible = true;
        public double x, y, scale = 1;
        public Widget(double x, double y) { this.x = x; this.y = y; }
        public void sanitize() { x = finite(x, 1, 0, 1); y = finite(y, 1, 0, 1); scale = finite(scale, 1, .5, 3); }
    }
    public void sanitize() {
        if(hudFont==null)hudFont=HudFont.INTER;
        clientFont=clientFont==null||clientFont.isBlank()||clientFont.length()>128?"Inter":clientFont.strip();
        version = 2;
        if (theme == null) theme = new Theme();
        if (theme.effects == null) theme.effects = Effects.ADAPTIVE;
        theme.sanitize();
        if (betterChat == null) betterChat = new BetterChat();
        betterChat.sanitize();
        if (betterScreenshot == null) betterScreenshot = new BetterScreenshot();
        betterScreenshot.sanitize();
        if(modules==null)modules=new java.util.EnumMap<>(me.xchro.neon.module.ClientModule.class);
        modules.entrySet().removeIf(e->e.getKey()==null||e.getValue()==null||!e.getKey().extra());
        for(var kind:me.xchro.neon.module.ClientModule.values())if(kind.extra()) {
            var state=module(kind);if(state.mode==null)state.mode=BindMode.TOGGLE;
        }
        if(crosshair==null)crosshair=new Crosshair();crosshair.sanitize();
        fovEffectStrength=finite(fovEffectStrength,0,0,1);
        hurtCameraStrength=finite(hurtCameraStrength,0,0,1);
        viewBobbingStrength=finite(viewBobbingStrength,.3,0,1);
        fireLowering=finite(fireLowering,.4,0,.8);
        glintStrength=finite(glintStrength,.6,0,1);glintSpeed=finite(glintSpeed,.35,0,1);
        hitColorStrength=finite(hitColorStrength,.3,0,1);
        visualTime=Math.floorMod(visualTime,24000);
        if(weather==null)weather=Weather.CLEAR;weatherIntensity=finite(weatherIntensity,.6,0,1);
        fullbrightStrength=finite(fullbrightStrength,1,0,1);
        blockOutlineWidth=finite(blockOutlineWidth,2,.5,8);
        blockOutlineFill=finite(blockOutlineFill,.18,0,1);
        if(blockOutlineMode==null)blockOutlineMode=BlockOutlineMode.OUTLINE;
        if(viewmodel==null)viewmodel=new Viewmodel();viewmodel.sanitize();
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
        if (fpsColors == null) fpsColors = new java.util.EnumMap<>(FpsColor.class);
        fpsColors.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        armor.sanitize(); totems.sanitize(); fps.sanitize();
        if (hudWidgets == null) hudWidgets = new java.util.EnumMap<>(me.xchro.neon.hud.HudKind.class);
        hudWidgets.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        for(var kind:me.xchro.neon.hud.HudKind.values()) {
            var value=hud(kind);
            if(value.placement==null) { value.placement=new Widget(kind.x,kind.y); value.placement.visible=false; }
            value.placement.sanitize();
        }
        armorGap = Math.clamp(armorGap, 0, 8); armorWarningPercent = Math.clamp(armorWarningPercent, 1, 30);
        potionMaxRows = Math.clamp(potionMaxRows, 1, 12);
        packMaxRows = Math.clamp(packMaxRows, 1, 5);
        if(counterItem==null)counterItem=CounterItem.PEARLS;
        scoreboardRows=Math.clamp(scoreboardRows,1,15);
        hudScale=finite(hudScale,1,.5,2);snapSensitivity=Math.clamp(snapSensitivity,1,24);
        if (armorAnchor == null) armorAnchor = ArmorAnchor.HOTBAR_LEFT;
    }
    private static double finite(double v, double fallback, double min, double max) { return Double.isFinite(v) ? Math.clamp(v, min, max) : fallback; }
}
