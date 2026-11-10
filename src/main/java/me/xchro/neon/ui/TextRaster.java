package me.xchro.neon.ui;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/** Shaped text coverage, resolved from 4x vector rasterization to native display pixels. */
final class TextRaster {
    static final FontRenderContext CONTEXT = new FontRenderContext(null, true, true);
    private static final int SAMPLES = 4, PAD = 2;
    record Image(int width, int height, int left, int top, int[] pixels) {}
    private TextRaster() {}
    static Font supported(Font font, String text) {
        return font.canDisplayUpTo(text) < 0 ? font : new Font(Font.DIALOG, Font.PLAIN, 1).deriveFont(font.getSize2D());
    }
    static float advance(Font font, String text) {
        return text.isEmpty() ? 0 : new TextLayout(text, supported(font, text), CONTEXT).getAdvance();
    }
    static Image render(Font font, String text) {
        var layout = new TextLayout(text, supported(font, text), CONTEXT);
        var bounds = layout.getBounds();
        int left = (int) Math.floor(bounds.getMinX()) - PAD, top = (int) Math.floor(bounds.getMinY()) - PAD;
        int w = Math.max(1, (int) Math.ceil(bounds.getMaxX()) - left + PAD);
        int h = Math.max(1, (int) Math.ceil(bounds.getMaxY()) - top + PAD);
        var high = new BufferedImage(w * SAMPLES, h * SAMPLES, BufferedImage.TYPE_INT_ARGB);
        var paint = high.createGraphics();
        paint.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paint.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        paint.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        paint.scale(SAMPLES, SAMPLES); paint.setColor(Color.WHITE); layout.draw(paint, -left, -top); paint.dispose();
        // TYPE_INT_ARGB is a contiguous packed-int raster. getRGB(x,y) can allocate a
        // temporary data-element array for each of the sixteen samples per pixel.
        int[] source = ((DataBufferInt) high.getRaster().getDataBuffer()).getData();
        int stride = w * SAMPLES;
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int coverage = 0;
            for (int sy = 0; sy < SAMPLES; sy++) for (int sx = 0; sx < SAMPLES; sx++)
                coverage += source[(y * SAMPLES + sy) * stride + x * SAMPLES + sx] >>> 24;
            int alpha = (int) Math.round(255 * Math.pow(coverage / (255.0 * SAMPLES * SAMPLES), .9));
            // Transparent texels remain white: no dark fringes at letter boundaries.
            pixels[y * w + x] = alpha << 24 | 0xffffff;
        }
        return new Image(w, h, left, top, pixels);
    }
}
