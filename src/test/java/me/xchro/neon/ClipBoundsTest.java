package me.xchro.neon;

import me.xchro.neon.core.ClipBounds;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClipBoundsTest {
    @Test void translatedControlEdgesSurviveEveryGuiScale() {
        for(int scale=1;scale<=6;scale++)for(int x=0;x<1000;x+=7)for(int width:new int[]{24,184,286,440}) {
            float left=x*1.3225f/scale,right=(x+width)*1.3225f/scale;
            var clip=ClipBounds.enclosing(left,17.2f,right,49.9f);
            assertTrue(clip.left()<=left&&clip.right()>=right,"clip must contain the complete switch/value at GUI scale "+scale);
            assertTrue(left-clip.left()<1&&clip.right()-right<1,"outward rounding is limited to one native GUI unit per edge");
            assertTrue(clip.top()<=17.2f&&clip.bottom()>=49.9f);
        }
    }
    @Test void fractionalOriginsDoNotLoseASecondUnitAtTheFarEdge() {
        var clip=ClipBounds.enclosing(100.9f,20.9f,124.2f,38.2f);
        assertEquals(100,clip.left());assertEquals(125,clip.right());
        assertEquals(20,clip.top());assertEquals(39,clip.bottom());
    }
}
