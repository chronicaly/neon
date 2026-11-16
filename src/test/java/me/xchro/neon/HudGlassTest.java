package me.xchro.neon;

import java.util.List;
import me.xchro.neon.hud.*;
import me.xchro.neon.config.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudGlassTest {
    private static HudGlassGeometry.Box box(int x,int y,int w,int h){return new HudGlassGeometry.Box(x,y,w,h);}
    @Test void nearbyPanelsJoinButDistantPanelsRemainSeparate() {
        var joined=HudGlassGeometry.shape(List.of(box(20,20,120,24),box(20,50,80,24)));
        assertTrue(joined.contains(50,47));assertFalse(joined.contains(130,60));
        assertEquals(20,joined.getBounds2D().getMinX(),.02);assertEquals(140,joined.getBounds2D().getMaxX(),.02);
        var separate=HudGlassGeometry.shape(List.of(box(20,20,120,24),box(20,66,80,24)));
        assertFalse(separate.contains(50,54));
        var isolated=HudGlassGeometry.shape(List.of(box(20,20,120,24)));
        assertFalse(isolated.contains(50,47));
    }
    @Test void overlappingFillIsOneUnionAndHasNoInteriorStroke() {
        var mesh=HudGlassGeometry.build(List.of(box(10,10,100,40),box(60,10,100,40)));
        double area=mesh.fill().stream().mapToDouble(q->((q.rightTop()-q.leftTop())+(q.rightBottom()-q.leftBottom()))*.5*(q.bottom()-q.top())).sum();
        assertTrue(area>5900&&area<6001,"150 by 40 union, never 200 by 40 alpha accumulation");
        assertTrue(mesh.edge().stream().noneMatch(e->e.x0()>65&&e.x0()<105&&e.y0()>12&&e.y0()<48));
    }
    @Test void joiningDoesNotFillTheHoleInARing() {
        var area=HudGlassGeometry.shape(List.of(box(10,10,140,20),box(10,110,140,20),box(10,30,20,80),box(130,30,20,80)));
        assertFalse(area.contains(80,70));assertTrue(area.contains(20,70));
        assertFalse(HudGlassGeometry.build(List.of()).fill().iterator().hasNext());
    }
    @Test void masterSwitchesPreserveLocalPreferencesAndSurviveCopies() {
        var c=new NeonConfig();c.hudSurfaces=false;c.hudBackgrounds=false;c.hudOutlines=false;
        c.fpsBackground=c.fpsBorder=true;c.hud(HudKind.COORDINATES).background=false;
        var copy=ConfigStore.copy(c);assertFalse(copy.hudSurfaces);assertFalse(copy.hudBackgrounds);assertFalse(copy.hudOutlines);
        assertTrue(copy.fpsBorder);assertTrue(copy.fpsBackground);assertFalse(copy.hud(HudKind.COORDINATES).background);
        copy.resetHud();assertTrue(copy.hudSurfaces&&copy.hudBackgrounds&&copy.hudOutlines);
        var legacy=ConfigStore.decode("{}");assertTrue(legacy.hudSurfaces&&legacy.hudBackgrounds&&legacy.hudOutlines);
    }
    @Test void coordinatesUseUnityFlooringCommasAndInlineConversion() {
        assertEquals("-1, 64, -9 (-1, -2)",HudNumbers.coordinates(-.2,64.9,-8.1,.125));
        assertEquals("12, 64, -1 (99, -1)",HudNumbers.coordinates(12.4,64,-.1,8));
        assertEquals("12, 64, 90",HudNumbers.coordinates(12.9,64,90,1));
    }
}
