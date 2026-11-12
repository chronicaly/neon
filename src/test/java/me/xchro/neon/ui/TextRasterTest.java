package me.xchro.neon.ui;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextRasterTest {
    private Font font() throws Exception {
        try (var in = getClass().getResourceAsStream("/assets/neon/font/inter-medium.ttf")) {
            return Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(in)).deriveFont(16f)
                .deriveFont(Map.of(TextAttribute.KERNING, TextAttribute.KERNING_ON, TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON));
        }
    }
    @Test void curvedLettersKeepContinuousCoverageAndWhiteTransparentPadding() throws Exception {
        var image = TextRaster.render(font(), "Autoclicker [M5]");
        Set<Integer> edges = new HashSet<>(); int solid = 0;
        for (int pixel : image.pixels()) {
            assertEquals(0xffffff, pixel & 0xffffff);
            int alpha = pixel >>> 24;
            if (alpha > 0 && alpha < 255) edges.add(alpha);
            if (alpha == 255) solid++;
        }
        assertTrue(edges.size() > 60, "Letters need many intermediate edge coverage levels");
        assertTrue(solid > 20, "Letter interiors must remain crisp");
        for (int x = 0; x < image.width(); x++) {
            assertEquals(0, image.pixels()[x] >>> 24);
            assertEquals(0, image.pixels()[(image.height() - 1) * image.width() + x] >>> 24);
        }
    }
    @Test void shapedTextPreservesKerningAccentsAndDescenders() throws Exception {
        Font font = font();
        assertTrue(TextRaster.advance(font, "AV") < TextRaster.advance(font, "A") + TextRaster.advance(font, "V"));
        var image = TextRaster.render(font, "Ágý Čž");
        assertTrue(image.top() < -10); assertTrue(image.top() + image.height() > 2);
        for (int y = 0; y < image.height(); y++) {
            assertEquals(0, image.pixels()[y * image.width()] >>> 24);
            assertEquals(0, image.pixels()[(y + 1) * image.width() - 1] >>> 24);
        }
    }
    @Test void scratchReusePreservesEveryPixelAcrossChangingRunsAndReloads() throws Exception {
        Font base=font();
        String[] runs={"Autoclicker [M5]", "#FFFFFFFF", "#00000000", "#73C8D3FF", "i", "Ágý Čž AV", "", "A much wider label [Reverse hold]"};
        for(int pass=0;pass<3;pass++)for(int size:new int[]{15,16,19,32})for(String text:runs){
            if(text.isEmpty()){TextRaster.clearScratch();continue;}
            Font face=base.deriveFont((float)size);
            var expected=freshRaster(face,text);var actual=TextRaster.render(face,text);
            assertEquals(expected.width(),actual.width());assertEquals(expected.height(),actual.height());
            assertEquals(expected.left(),actual.left());assertEquals(expected.top(),actual.top());
            assertArrayEquals(expected.pixels(),actual.pixels(),"Changed text coverage: "+text+" at "+size);
            int[] retained=actual.pixels().clone();TextRaster.render(face,"#11223344");
            assertArrayEquals(retained,actual.pixels(),"Previously returned raster must remain immutable");
        }
        TextRaster.clearScratch();
    }
    /** Fresh-image reference path from the preceding renderer, independent of scratch reuse. */
    private TextRaster.Image freshRaster(Font font,String text){
        var layout=new java.awt.font.TextLayout(text,TextRaster.supported(font,text),TextRaster.CONTEXT);
        var bounds=layout.getBounds();int left=(int)Math.floor(bounds.getMinX())-2,top=(int)Math.floor(bounds.getMinY())-2;
        int w=Math.max(1,(int)Math.ceil(bounds.getMaxX())-left+2),h=Math.max(1,(int)Math.ceil(bounds.getMaxY())-top+2);
        var image=new java.awt.image.BufferedImage(w*4,h*4,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS,java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.scale(4,4);g.setColor(java.awt.Color.WHITE);layout.draw(g,-left,-top);g.dispose();
        int[] pixels=new int[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int coverage=0;for(int sy=0;sy<4;sy++)for(int sx=0;sx<4;sx++)coverage+=image.getRGB(x*4+sx,y*4+sy)>>>24;
            pixels[y*w+x]=(int)Math.round(255*Math.pow(coverage/(255.0*16),.9))<<24|0xffffff;
        }
        return new TextRaster.Image(w,h,left,top,pixels);
    }
}
