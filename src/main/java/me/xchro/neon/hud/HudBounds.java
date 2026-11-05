package me.xchro.neon.hud;

import me.xchro.neon.config.NeonConfig.Widget;

public record HudBounds(int x, int y, int width, int height) {
    public boolean contains(double mx, double my) { return mx >= x && mx < x + width && my >= y && my < y + height; }
    public static HudBounds of(Widget widget, int contentWidth, int screenWidth, int screenHeight) {
        int w = Math.min(screenWidth, (int) Math.ceil(contentWidth * widget.scale));
        int h = Math.min(screenHeight, (int) Math.ceil(16 * widget.scale));
        int marginX = Math.min(8, Math.max(0, (screenWidth - w) / 2));
        int marginY = Math.min(8, Math.max(0, (screenHeight - h) / 2));
        return new HudBounds(marginX + (int) Math.round(widget.x * Math.max(0, screenWidth - w - 2 * marginX)),
            marginY + (int) Math.round(widget.y * Math.max(0, screenHeight - h - 2 * marginY)), w, h);
    }
    public void move(Widget widget, double x, double y, int screenWidth, int screenHeight) {
        int mx = Math.min(8, Math.max(0, (screenWidth - width) / 2)), my = Math.min(8, Math.max(0, (screenHeight - height) / 2));
        widget.x = Math.clamp((x - mx) / Math.max(1, screenWidth - width - 2 * mx), 0, 1);
        widget.y = Math.clamp((y - my) / Math.max(1, screenHeight - height - 2 * my), 0, 1);
    }
}
