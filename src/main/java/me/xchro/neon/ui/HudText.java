package me.xchro.neon.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Native display-pixel Inter text, independent of menu opacity and Minecraft GUI scale. */
public final class HudText {
    private HudText() {}
    public static void beginFrame() { UiFont.beginHudFrame(); }
    public static void flush() { UiFont.flush(); }
    public static int width(String text, int pixels) { return UiFont.hudWidth(text, pixels); }
    public static void draw(GuiGraphicsExtractor g, String text, int x, int y, int color, int pixels, boolean shadow) {
        if (shadow) UiFont.drawHud(g, text, x + 1, y + 1, color & 0xff000000, pixels);
        UiFont.drawHud(g, text, x, y, color, pixels);
    }
}
