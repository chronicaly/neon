package me.xchro.neon.ui;

import me.xchro.neon.config.NeonConfig.Widget;
import me.xchro.neon.hud.HudBounds;

/** Port of Unity's HudResize (5bbd639), using Neon's normalized widget positions. */
final class HudResize {
    private final Widget element;
    private final HudBounds initial;
    private final boolean west, north;
    private final double scale, naturalWidth, naturalHeight;
    HudResize(Widget element, HudBounds bounds, int corner, double scale) {
        this.element = element; initial = bounds; west = corner % 2 == 0; north = corner < 2; this.scale = scale;
        naturalWidth = bounds.width() / scale; naturalHeight = bounds.height() / scale;
    }
    void drag(double dx, double dy, int sw, int sh) {
        double fixedX = west ? initial.x() + initial.width() : initial.x();
        double fixedY = north ? initial.y() + initial.height() : initial.y();
        double availableWidth = west ? fixedX : sw - fixedX;
        double availableHeight = north ? fixedY : sh - fixedY;
        // Unity projects onto the original diagonal, avoiding jumps when the dominant axis changes.
        double ratio = 1 + (dx * (west ? -1 : 1) * initial.width() + dy * (north ? -1 : 1) * initial.height())
            / (initial.width() * initial.width() + initial.height() * initial.height());
        double maximum = Math.min(3, Math.min(availableWidth / naturalWidth, availableHeight / naturalHeight));
        element.scale = Math.clamp(scale * ratio, .5, Math.max(.5, maximum));
    }
    void place(HudBounds measured, int sw, int sh) {
        double fixedX = west ? initial.x() + initial.width() : initial.x();
        double fixedY = north ? initial.y() + initial.height() : initial.y();
        measured.move(element, west ? fixedX - measured.width() : fixedX, north ? fixedY - measured.height() : fixedY, sw, sh);
    }
}
