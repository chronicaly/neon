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
    @Test void joiningAtOffsetsKeepsMeshComplexityBounded() {
        for(int gap=-8;gap<=8;gap++)for(int dy=-3;dy<=3;dy++) {
            var boxes=List.of(box(0,0,87,24),box(87+gap,dy,313,134));
            var mesh=HudGlassGeometry.build(boxes);
            assertTrue(mesh.fill().size()<80,"bounded fill strips at gap "+gap+", offset "+dy);
            assertTrue(mesh.edge().size()<65,"bounded outside contour");
            for(var q:mesh.fill()) {
                assertTrue(Float.isFinite(q.leftTop())&&Float.isFinite(q.rightBottom()));
                assertTrue(q.rightTop()>=q.leftTop()-.001&&q.rightBottom()>=q.leftBottom()-.001);
            }
            if(gap>0&&gap<=6)assertTrue(HudGlassGeometry.shape(boxes).contains(87+gap/2.,12));
            if(gap>6)assertFalse(HudGlassGeometry.shape(boxes).contains(87+gap/2.,12));
        }
    }
    @Test void translatingClusterPreservesGeometryAndHoles() {
        var mesh=HudGlassGeometry.build(List.of(box(0,0,87,24),box(93,0,313,134)));
        var shifted=HudGlassGeometry.build(List.of(box(1600,900,87,24),box(1693,900,313,134)));
        assertEquals(mesh.fill().size(),shifted.fill().size());assertEquals(mesh.edge().size(),shifted.edge().size());
        assertEquals(mesh.bounds().getWidth(),shifted.bounds().getWidth(),.001);
        var diagonal=HudGlassGeometry.shape(List.of(box(0,0,24,24),box(28,28,24,24)));
        assertFalse(diagonal.contains(26,26),"corner-only proximity does not create an unrelated bridge");
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
