package me.xchro.neon.hud;

import me.xchro.neon.Neon;
import me.xchro.neon.config.FpsColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Unity's FPS label/value, stable width and optional smoothing, adapted to Neon's HUD geometry. */
public final class FpsHud {
    private static final HudFrameRate FRAME_RATE = new HudFrameRate();
    private static final HudFpsTransition TRANSITION = new HudFpsTransition();
    private FpsHud() {}
    public static void frame(long now) { FRAME_RATE.frame(now); TRANSITION.update(FRAME_RATE.fps(), now); }
    public static int value() { return Neon.config.fpsAnimation ? TRANSITION.value() : FRAME_RATE.fps(); }
    public static int fontPixels() { return Math.max(1, (int) Math.round(16 * Neon.config.hudScale * Neon.config.fps.scale)); }
    private static float textScale() { return fontPixels() / 8f; }
    private static int paddingX() { return Math.max(1, (int) Math.round(3 * Neon.config.hudScale * Neon.config.fps.scale)); }
    private static int paddingY() { return Math.max(1, (int) Math.round(2 * Neon.config.hudScale * Neon.config.fps.scale)); }
    public static HudBounds bounds(int width, int height) {
        var font = Minecraft.getInstance().font;
        // Resolve current pack metrics every frame, including proportional replacement digits.
        int digitWidth = 0;
        for (int digit = 0; digit <= 9; digit++) digitWidth = Math.max(digitWidth, font.width(Integer.toString(digit)));
        int contentWidth = (int) Math.ceil((font.width("FPS ") + Math.max(4 * digitWidth, font.width(Integer.toString(value())))) * textScale());
        int w = Math.min(width, contentWidth + 2 * paddingX()), h = Math.min(height, (int) Math.ceil(font.lineHeight * textScale()) + 2 * paddingY());
        return new HudBounds((int) Math.round(Neon.config.fps.x * (width - w)), (int) Math.round(Neon.config.fps.y * (height - h)), w, h);
    }
    public static void draw(GuiGraphicsExtractor g, int width, int height) {
        var c = Neon.config;
        if (!c.fps.visible) return;
        var b = bounds(width, height);
        if (c.fpsBackground) {
            int color = FpsColor.BACKGROUND.color(c);
            g.fill(b.x(), b.y(), b.x() + b.width(), b.y() + b.height(), color & 0xffffff | (int) Math.round((color >>> 24) * .78) << 24);
        }
        if (c.fpsBorder) {
            int color = FpsColor.BORDER.color(c), x = b.x(), y = b.y(), r = x + b.width(), bottom = y + b.height();
            g.fill(x, y, r, y + 1, color); g.fill(x, bottom - 1, r, bottom, color);
            g.fill(x, y + 1, x + 1, bottom - 1, color); g.fill(r - 1, y + 1, r, bottom - 1, color);
        }
        var font = Minecraft.getInstance().font;
        g.pose().pushMatrix();
        g.pose().translate(b.x() + paddingX(), b.y() + paddingY());
        g.pose().scale(textScale());
        g.text(font, "FPS ", 0, 0, FpsColor.LABEL.color(c), c.fpsShadow);
        g.text(font, Integer.toString(value()), font.width("FPS "), 0, FpsColor.VALUE.color(c), c.fpsShadow);
        g.pose().popMatrix();
    }
}
