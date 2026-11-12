package me.xchro.neon.core;

/** Direct pointer positioning until precision is used; thereafter an unrounded relative gesture. */
public final class NumberDrag {
    private final double min, max, step;
    private double raw, pointer;
    private boolean relative;
    public NumberDrag(double min, double max, double step, double current, double pointer,
                      double left, double width, boolean fine) {
        this.min = min; this.max = max; this.step = step;
        relative = fine;
        raw = fine ? Math.clamp(current, min, max) : position(pointer, left, width);
        this.pointer = pointer;
    }
    private double position(double x, double left, double width) {
        return min + Math.clamp((x - left) / Math.max(1, width), 0, 1) * (max - min);
    }
    public double value() { return quantize(raw, min, max, step); }
    public double move(double next, double left, double width, boolean fine) {
        relative |= fine;
        raw = relative
            ? Math.clamp(raw + (next - pointer) * (max - min) / Math.max(1, width) * (fine ? .1 : 1), min, max)
            : position(next, left, width);
        pointer = next;
        return value();
    }
    public static double quantize(double value, double min, double max, double step) {
        if (value <= min) return min;
        if (value >= max) return max;
        return clean(Math.clamp(min + Math.round((value - min) / step) * step, min, max));
    }
    public static double nudge(double value, double min, double max, double step, int direction, boolean coarse) {
        return clean(Math.clamp(value + direction * step * (coarse ? 10 : 1), min, max));
    }
    private static double clean(double value) { return Double.parseDouble(NumberValue.format(value)); }
}
