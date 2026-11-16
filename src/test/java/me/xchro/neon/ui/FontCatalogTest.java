package me.xchro.neon.ui;

import java.awt.GraphicsEnvironment;
import java.util.*;
import me.xchro.neon.config.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FontCatalogTest {
    @Test void catalogIncludesEveryLocalFamilyAndBundledDefault(){
        var names=FontCatalog.families();assertEquals("Inter",names.getFirst());
        assertEquals(names.size(),names.stream().map(n->n.toLowerCase(Locale.ROOT)).distinct().count());
        for(String family:GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT))assertTrue(FontCatalog.available(family),family);
    }
    @Test void selectedFamilySurvivesSettingsAndUnknownFontsRemainRecoverable(){
        var config=new NeonConfig();config.clientFont="Papyrus";
        assertEquals("Papyrus",ConfigStore.copy(config).clientFont);
        config.clientFont="A font from another computer";
        assertEquals(config.clientFont,ConfigStore.copy(config).clientFont);
        assertNull(FontCatalog.system(config.clientFont,false));
        config.clientFont=null;config.sanitize();assertEquals("Inter",config.clientFont);
    }
    @Test void localFontsUseRealFamilyMetricsAndRasterizeLegibly(){
        for(String name:List.of("Serif","Monospaced","Papyrus"))if(FontCatalog.available(name)){
            var font=FontCatalog.system(name,false).deriveFont(16f);
            assertEquals(FontCatalog.canonical(name),font.getFamily(Locale.ROOT));
            var raster=TextRaster.render(font,"Neon Ag 0123456789");
            assertTrue(TextRaster.advance(font,"Neon Ag 0123456789")>20);
            assertTrue(Arrays.stream(raster.pixels()).anyMatch(pixel->(pixel>>>24)>175));
        }
    }
}
