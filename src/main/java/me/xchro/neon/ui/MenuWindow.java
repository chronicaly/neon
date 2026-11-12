package me.xchro.neon.ui;

import java.nio.file.Path;
import me.xchro.neon.Neon;
import me.xchro.neon.config.MenuPlacement;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.navigation.ScreenRectangle;

/** The same rectangle is used for extraction, input, clamping, and persistence. */
final class MenuWindow {
    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("neon-window.json");
    private final MenuPlacement placement;
    private int viewportW, viewportH, x, y, w, h, gesture = -1;
    private ScreenRectangle start;
    private double mouseX, mouseY;
    MenuWindow() {
        MenuPlacement loaded;
        try {
            String pending = Neon.SAVES.latest(file);
            loaded = pending != null ? MenuPlacement.decode(pending) : MenuPlacement.load(file);
        } catch (Exception e) { loaded = new MenuPlacement(); Neon.LOG.warn("Could not read menu placement", e); }
        placement = loaded;
    }
    int minWidth() { return Math.min(320, viewportW - 28); }
    int minHeight() { return Math.min(210, viewportH - 28); }
    void fit(int width, int height) {
        if (width == viewportW && height == viewportH) return;
        viewportW = width; viewportH = height; gesture = -1;
        w = Math.clamp(placement.customized ? (int) Math.round(width * placement.width) : Math.min(440, width / 2), minWidth(), width - 28);
        h = Math.clamp(placement.customized ? (int) Math.round(height * placement.height) : Math.min(280, height / 2), minHeight(), height - 28);
        x = Math.clamp(placement.customized ? (int) Math.round(width * placement.x) : (width - w) / 2, 14, width - w - 14);
        y = Math.clamp(placement.customized ? (int) Math.round(height * placement.y) : (height - h) / 2, 14, height - h - 14);
    }
    ScreenRectangle bounds() { return new ScreenRectangle(x, y, w, h); }
    boolean dragging() { return gesture >= 0; }
    int corner(double mx, double my) {
        for (int i = 0; i < 4; i++) if (Math.abs(mx - (x + (i % 2) * w)) <= 6 && Math.abs(my - (y + (i / 2) * h)) <= 6) return i;
        return -1;
    }
    void begin(int kind, double mx, double my) { gesture = kind; start = bounds(); mouseX = mx; mouseY = my; }
    void beginVisible(int kind,double mx,double my,int visibleX) {
        x=visibleX; begin(kind,mx,my);
    }
    void drag(double mx, double my) {
        int dx = (int) Math.round(mx - mouseX), dy = (int) Math.round(my - mouseY);
        if (gesture == 4) {
            x = Math.clamp(start.left() + dx, 14, viewportW - w - 14);
            y = Math.clamp(start.top() + dy, 14, viewportH - h - 14); return;
        }
        if (gesture < 0) return;
        boolean west = gesture % 2 == 0, north = gesture < 2;
        x = west ? Math.clamp(start.left() + dx, 14, start.right() - minWidth()) : start.left();
        y = north ? Math.clamp(start.top() + dy, 14, start.bottom() - minHeight()) : start.top();
        w = west ? start.right() - x : Math.clamp(start.width() + dx, minWidth(), viewportW - x - 14);
        h = north ? start.bottom() - y : Math.clamp(start.height() + dy, minHeight(), viewportH - y - 14);
    }
    void cancel() { if (start != null) { x = start.left(); y = start.top(); w = start.width(); h = start.height(); } gesture = -1; }
    void finish() {
        if (!dragging()) return;
        gesture = -1; placement.customized = true;
        placement.x = (double) x / viewportW; placement.y = (double) y / viewportH;
        placement.width = (double) w / viewportW; placement.height = (double) h / viewportH;
        placement.queue(file, Neon.SAVES);
    }
}
