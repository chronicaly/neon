package me.xchro.neon.ui;

import me.xchro.neon.Neon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;

/** The approved Neon palette and shared, sharp control primitives. */
public final class Ui {
    public static int TEXT = 0xffe3eaf1, MUTED = 0xff83909f, BLUE = 0xff84dce6,
        PANEL = 0xff0e131b, WHITE = TEXT, FIELD = 0xff121923, SELECTED = 0xff131b23,
        VIOLET = 0xff9985cc, OUTLINE = 0xff73c8d3, SIDEBAR = 0xff0c1016;
    public static final int LINE = 0xff27313c, RED = 0xffff6e84, INK = 0xff071218;
    private static final FontDescription FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath("neon", "ui"));
    private Ui() {}
    static float opacity = 1;
    static int faded(int color) { return alpha(color, Math.round((color >>> 24) * opacity)); }
    public static void refreshTheme() {
        var t = Neon.config.theme;
        PANEL = alpha(t.background,(int)Math.round((t.background>>>24)*t.opacity)); FIELD = alpha(t.cards,(int)Math.round((t.cards>>>24)*t.opacity)); TEXT = WHITE = t.text; MUTED = t.muted;
        BLUE = t.accent; OUTLINE = t.outline; VIOLET = t.secondary;
        SELECTED = UiEffects.mix(PANEL, BLUE, .04); SIDEBAR = UiEffects.mix(PANEL, alpha(0,PANEL>>>24), .17);
    }
    public static MutableComponent label(String text) { return Component.literal(text).withStyle(s -> s.withFont(FONT).withoutShadow()); }
    public static MutableComponent label(Component text) { return text.copy().withStyle(s -> s.withFont(FONT).withoutShadow()); }
    public static int width(String text) { return UiFont.width(text); }
    public static void text(GuiGraphicsExtractor g, String value, int x, int y, int color) { UiFont.draw(g, value, x, y, color, false); }
    public static void text(GuiGraphicsExtractor g, Component value, int x, int y, int color) {
        text(g, value.getString(), x, y, value.getStyle().getColor() == null ? color : 0xff000000 | value.getStyle().getColor().getValue());
    }
    public static void small(GuiGraphicsExtractor g, String value, int x, int y, int color) { UiFont.draw(g, value, x, y, color, 10); }
    public static void centered(GuiGraphicsExtractor g, String value, int x, int y, int color) { text(g, value, x - width(value) / 2, y, color); }
    public static void title(GuiGraphicsExtractor g, String value, int x, int y) { UiFont.draw(g, value, x, y, TEXT, true); }
    public static String fit(String value, int width) {
        if (width(value) <= width) return value;
        while (!value.isEmpty() && width(value + "…") > width) value = value.substring(0, value.offsetByCodePoints(value.length(), -1));
        return value + "…";
    }
    public static void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) { if (w > 0 && h > 0) g.fill(x, y, x + w, y + h, faded(color)); }
    public static void stroke(GuiGraphicsExtractor g,int x,int y,int w,int h,int color){UiEffects.stroke(g,x,y,w,h,faded(color));}
    public static int alpha(int color, int alpha) { return (color & 0xffffff) | Math.clamp(alpha, 0, 255) << 24; }
    public static void glow(GuiGraphicsExtractor g, int x, int y, int w, int h, int color, int radius) { if (MenuEffects.glow()) UiEffects.glow(g, x, y, w, h, faded(color), radius); }
    public static void card(GuiGraphicsExtractor g, int x, int y, int w, int h) { rect(g, x, y, w, h, FIELD); Ui.stroke(g, x, y, w, h, LINE); }
    public static void background(GuiGraphicsExtractor g, int width, int height) {
        refreshTheme(); if (MenuEffects.blur()) g.blurBeforeThisStratum(); rect(g, 0, 0, width + 1, height + 1, 0xb3060a12);
    }
    public static void window(GuiGraphicsExtractor g, int x, int y, int w, int h) { rect(g, x, y, w, h, PANEL); UiEffects.outline(g, x, y, w, h); }
    public static void releaseFonts() { UiFont.close(); UiIcons.close(); UiEffects.close(); }
    public static void warmFonts() { UiFont.warmNative(); }
}
