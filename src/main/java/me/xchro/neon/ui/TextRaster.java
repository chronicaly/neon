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
    private static final ThreadLocal<BufferedImage> SCRATCH = new ThreadLocal<>();
    private static final int MAX_SCRATCH_PIXELS = 1_048_576;
    record Image(int width, int height, int left, int top, int[] pixels) {}
    private TextRaster() {}
    static void clearScratch() { SCRATCH.remove(); }
    private static BufferedImage scratch(int width,int height) {
        var image=SCRATCH.get();
        if(image!=null&&image.getWidth()>=width&&image.getHeight()>=height)return image;
        int w=Math.max(width,image==null?0:image.getWidth()),h=Math.max(height,image==null?0:image.getHeight());
        // Keep at most 4 MiB per rendering thread. Unusually large runs are temporary.
        if((long)w*h>MAX_SCRATCH_PIXELS)return new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);SCRATCH.set(image);return image;
    }
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
        var high = scratch(w * SAMPLES, h * SAMPLES);
        var paint = high.createGraphics();
        paint.setComposite(AlphaComposite.Clear);
        paint.fillRect(0,0,w*SAMPLES,h*SAMPLES);
        paint.setComposite(AlphaComposite.SrcOver);
        paint.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paint.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        paint.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        paint.scale(SAMPLES, SAMPLES); paint.setColor(Color.WHITE); layout.draw(paint, -left, -top); paint.dispose();
        // TYPE_INT_ARGB is a contiguous packed-int raster. getRGB(x,y) can allocate a
        // temporary data-element array for each of the sixteen samples per pixel.
        int[] source = ((DataBufferInt) high.getRaster().getDataBuffer()).getData();
        int stride = high.getWidth();
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
