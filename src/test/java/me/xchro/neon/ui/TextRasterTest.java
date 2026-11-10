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
}
