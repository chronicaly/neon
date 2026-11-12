package me.xchro.neon;

import me.xchro.neon.core.UiMotion;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiMotionTest {
    @Test void reversalPreservesPositionAndVelocity() {
        var m=new UiMotion(0);m.to(1,0,340_000_000L);
        long now=70_000_000;double position=m.value(now,0),speed=m.velocity(now);
        m.to(0,now,340_000_000L);
        assertEquals(position,m.value(now,0),1e-12);assertEquals(speed,m.velocity(now),1e-12);
        assertTrue(m.value(now+1_000,0)>position);
        assertEquals(0,m.value(now+340_000_000L,0));
    }
    @Test void randomReversalsStayBoundedAndFrameRateIndependent() {
        var a=new UiMotion(0);var b=new UiMotion(0);var random=new java.util.Random(17);
        for(long now=0;now<3_000_000_000L;now+=7_000_000){
            if(random.nextInt(4)==0){double target=random.nextBoolean()?1:0;a.to(target,now,220_000_000);b.to(target,now,220_000_000);}
            for(int frame=0;frame<7;frame++)a.value(now+frame*1_000_000,0);
            double value=b.value(now+6_000_000,0);assertTrue(value>=0&&value<=1);assertEquals(value,a.value(now+6_000_000,0),1e-12);
        }
    }
    @Test void exactFocusRevealAndSettlingDoNotLeaveMotion() {
        var m=new UiMotion(0);m.to(1,0,150_000_000);m.snap(1);
        assertEquals(1,m.value(10_000_000,0));assertEquals(0,m.velocity(10_000_000));
        m.to(0,20_000_000,150_000_000);assertEquals(0,m.value(170_000_000,0));
    }
}
