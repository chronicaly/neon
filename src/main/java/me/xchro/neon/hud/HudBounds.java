package me.xchro.neon.hud;

import me.xchro.neon.config.NeonConfig.Widget;

public record HudBounds(int x, int y, int width, int height) {
    public boolean contains(double mx, double my) { return mx >= x && mx < x + width && my >= y && my < y + height; }
    public static double scale(Widget widget, int contentWidth, int contentHeight, int screenWidth, int screenHeight) {
        return Math.min(widget.scale, Math.min((double) screenWidth / contentWidth, (double) screenHeight / contentHeight));
    }
    public static HudBounds of(Widget widget, int contentWidth, int contentHeight, int screenWidth, int screenHeight) {
        double scale = scale(widget, contentWidth, contentHeight, screenWidth, screenHeight);
        int w = Math.min(screenWidth, (int) Math.ceil(contentWidth * scale));
        int h = Math.min(screenHeight, (int) Math.ceil(contentHeight * scale));
        return new HudBounds((int) Math.round(widget.x * (screenWidth - w)),
            (int) Math.round(widget.y * (screenHeight - h)), w, h);
    }
    public HudBounds at(int x, int y, int screenWidth, int screenHeight) {
        return new HudBounds(Math.clamp(x, 0, Math.max(0, screenWidth - width)),
            Math.clamp(y, 0, Math.max(0, screenHeight - height)), width, height);
    }
    public void move(Widget widget, double x, double y, int screenWidth, int screenHeight) {
        widget.x = Math.clamp(x / Math.max(1, screenWidth - width), 0, 1);
        widget.y = Math.clamp(y / Math.max(1, screenHeight - height), 0, 1);
    }
}
