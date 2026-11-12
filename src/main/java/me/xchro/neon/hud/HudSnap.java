package me.xchro.neon.hud;

import java.util.List;

/** Alignment in physical display pixels; a guide is the aligned edge/center, not the widget origin. */
public final class HudSnap {
    private static final int GAP = 6;
    public record Result(double x, double y, double guideX, double guideY) {}
    private record Axis(double position, double guide) {}
    private HudSnap() {}

    public static Result move(HudBounds moving, List<HudBounds> visibleOthers, int width, int height,
                              double x, double y, int sensitivity) {
        x = Math.clamp(x, 0, Math.max(0, width - moving.width()));
        y = Math.clamp(y, 0, Math.max(0, height - moving.height()));
        Axis horizontal = axis(x, y, moving.width(), moving.height(), width, visibleOthers, sensitivity, true);
        Axis vertical = axis(y, x, moving.height(), moving.width(), height, visibleOthers, sensitivity, false);
        return new Result(horizontal.position, vertical.position, horizontal.guide, vertical.guide);
    }

    private static Axis axis(double position, double perpendicular, int size, int perpendicularSize, int extent,
                             List<HudBounds> others, int sensitivity, boolean horizontal) {
        var candidates = new java.util.ArrayList<Axis>();
        for (double target : new double[]{0, extent / 2.0, extent})
            align(candidates, target, size);
        for (HudBounds other : others) {
            int start = horizontal ? other.x() : other.y(), length = horizontal ? other.width() : other.height();
            for (double target : new double[]{start, start + length / 2.0, start + length}) align(candidates, target, size);
            int cross = horizontal ? other.y() : other.x(), crossSize = horizontal ? other.height() : other.width();
            if (perpendicular <= cross + crossSize + GAP && perpendicular + perpendicularSize >= cross - GAP) {
                candidates.add(new Axis(start - GAP - size, start - GAP));
                candidates.add(new Axis(start + length + GAP, start + length + GAP));
            }
        }
        Axis result = new Axis(position, -1);
        double nearest = Math.max(0, sensitivity) + .0001;
        for (Axis candidate : candidates) {
            if (candidate.position < 0 || candidate.position > extent - size) continue;
            double distance = Math.abs(candidate.position - position);
            if (distance < nearest) { nearest = distance; result = candidate; }
        }
        return result;
    }
    private static void align(List<Axis> candidates, double target, int size) {
        for (double anchor : new double[]{0, size / 2.0, size}) candidates.add(new Axis(target - anchor, target));
    }
}
