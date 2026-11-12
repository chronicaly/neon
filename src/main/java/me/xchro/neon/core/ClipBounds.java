package me.xchro.neon.core;

/** Integer GUI bounds which fully contain a transformed display-pixel rectangle. */
public record ClipBounds(int left, int top, int right, int bottom) {
    public static ClipBounds enclosing(float x0, float y0, float x1, float y1) {
        return new ClipBounds((int)Math.floor(Math.min(x0,x1)), (int)Math.floor(Math.min(y0,y1)),
            (int)Math.ceil(Math.max(x0,x1)), (int)Math.ceil(Math.max(y0,y1)));
    }
}
