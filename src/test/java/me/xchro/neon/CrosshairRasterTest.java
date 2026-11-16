package me.xchro.neon;

import me.xchro.neon.core.CrosshairRaster;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrosshairRasterTest {
    @Test void coverageIsCenteredAtEveryFramebufferParityAndThinWidth() {
        for(var shape:new CrosshairRaster.Shape[]{CrosshairRaster.Shape.CROSS,CrosshairRaster.Shape.DOT,CrosshairRaster.Shape.SQUARE})
            for(boolean oddX:new boolean[]{false,true})for(boolean oddY:new boolean[]{false,true})for(float width:new float[]{.5f,1,1.25f,2,3}) {
                var runs=CrosshairRaster.rasterize(new CrosshairRaster.Key(shape,6,3,width,true,.5f,oddX,oddY));
                double x=0,y=0,mass=0;
                for(var run:runs)for(int px=run.x();px<run.x()+run.length();px++) {
                    int weight=run.fill()+run.border();mass+=weight;x+=(px+.5)*weight;y+=(run.y()+.5)*weight;
                }
                assertTrue(mass>0);assertEquals(oddX?.5:0,x/mass,1e-9,shape+" horizontal center");assertEquals(oddY?.5:0,y/mass,1e-9,shape+" vertical center");
            }
    }
}
