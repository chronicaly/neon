package me.xchro.neon.ui;

import me.xchro.neon.Neon;
import me.xchro.neon.config.ThemeColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;

/** The approved Neon palette and shared, sharp control primitives. */
public final class Ui {
    public static int TEXT = 0xffe3eaf1, MUTED = 0xff83909f, BLUE = 0xff84dce6,
        PANEL = 0xff07090e, WHITE = TEXT, FIELD = 0xff0d1119, SELECTED = 0xff0c1117,
        VIOLET = 0xff9985cc, OUTLINE = 0xff73c8d3, SIDEBAR = 0xff06070c;
    public static final int LINE = 0xff202733, RED = 0xffff6e84, INK = 0xff071218;
    private static final FontDescription FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath("neon", "ui"));
    private Ui() {}
    static float opacity = 1;
    static int faded(int color) { return alpha(color, Math.round((color >>> 24) * opacity)); }
    public static void refreshTheme() {
        var t = Neon.config.theme;
        long now = System.currentTimeMillis();
        int background = t.color(ThemeColor.BACKGROUND, now), cards = t.color(ThemeColor.CARDS, now);
        PANEL = alpha(background,(int)Math.round((background>>>24)*t.opacity)); FIELD = alpha(cards,(int)Math.round((cards>>>24)*t.opacity));
        TEXT = WHITE = t.color(ThemeColor.TEXT, now); MUTED = t.color(ThemeColor.MUTED, now);
        BLUE = t.color(ThemeColor.ACCENT, now); OUTLINE = t.color(ThemeColor.OUTLINE, now); VIOLET = t.color(ThemeColor.SECONDARY, now);
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
    public static void disclosure(GuiGraphicsExtractor g, int centerX, int textY, boolean expanded, int color) {
        String symbol = expanded ? "−" : "+";
        UiFont.draw(g, symbol, centerX - UiFont.width(symbol, 13) / 2, textY, color, 13);
    }
    public static void title(GuiGraphicsExtractor g, String value, int x, int y) { UiFont.draw(g, value, x, y, TEXT, true); }
    public static String fit(String value, int width) {
        if (width(value) <= width) return value;
        while (!value.isEmpty() && width(value + "…") > width) value = value.substring(0, value.offsetByCodePoints(value.length(), -1));
        return value + "…";
    }
    public static void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) { if (w > 0 && h > 0) g.fill(x, y, x + w, y + h, faded(color)); }
    public static void stroke(GuiGraphicsExtractor g,int x,int y,int w,int h,int color){UiEffects.stroke(g,x,y,w,h,faded(color));}
    public static void clip(GuiGraphicsExtractor g, int left, int top, int right, int bottom) {
        var start = g.pose().transformPosition(left, top, new org.joml.Vector2f());
        var end = g.pose().transformPosition(right, bottom, new org.joml.Vector2f());
        // Vanilla floors the origin AND width, cutting off up to two GUI units on the right.
        // Round the transformed endpoints outwards independently before nesting the clip.
        var bounds = me.xchro.neon.core.ClipBounds.enclosing(start.x,start.y,end.x,end.y);
        g.scissorStack.push(new net.minecraft.client.gui.navigation.ScreenRectangle(bounds.left(),bounds.top(),
            bounds.right()-bounds.left(),bounds.bottom()-bounds.top()));
    }
    public static int alpha(int color, int alpha) { return (color & 0xffffff) | Math.clamp(alpha, 0, 255) << 24; }
    /** Button tint never adds opacity to the saved Cards alpha or menu opacity. */
    static int buttonSurface(int color, double emphasis) {
        return alpha(color, (int)Math.round((FIELD >>> 24) * (.18 + .10 * Math.clamp(emphasis, 0, 1))));
    }
    public static void glow(GuiGraphicsExtractor g, int x, int y, int w, int h, int color, int radius) { if (MenuEffects.glow()) UiEffects.glow(g, x, y, w, h, faded(color), radius); }
    public static void card(GuiGraphicsExtractor g, int x, int y, int w, int h) { rect(g, x, y, w, h, FIELD); Ui.stroke(g, x, y, w, h, LINE); }
    public static void background(GuiGraphicsExtractor g, int width, int height) {
        refreshTheme(); if (MenuEffects.blur()) g.blurBeforeThisStratum(); rect(g, 0, 0, width + 1, height + 1, 0xb3060a12);
    }
    public static void window(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        rect(g, x, y, w, h, PANEL);
        UiEffects.surfaceLight(g, x + 1, y + 1, w - 2, h - 2, PANEL);
        UiEffects.outline(g, x, y, w, h);
    }
    public static void releaseFonts() { MenuPresentation.invalidateAll(); UiFont.close(); UiIcons.close(); UiEffects.close(); }
    public static void warmFonts() { UiFont.warmNative(); }
}
