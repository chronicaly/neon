package me.xchro.neon;

import me.xchro.neon.core.ClickWindow;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClickWindowTest {
    @Test void trailingSecondExpiresEachClickAtItsOwnBoundary() {
        var window=new ClickWindow();window.press(10_000_000_000L);window.press(10_200_000_000L);window.press(10_999_000_000L);
        assertEquals(3,window.count(10_999_999_999L));assertEquals(2,window.count(11_000_000_000L));
        assertEquals(1,window.count(11_200_000_000L));assertEquals(0,window.count(12_000_000_000L));
    }
    @Test void contextResetAndClockDiscontinuityDiscardHistory() {
        var window=new ClickWindow();window.press(20);window.clear();assertEquals(0,window.count(30));
        window.press(100);assertEquals(0,window.count(40));
        for(int i=0;i<1000;i++)window.press(1000+i);assertEquals(512,window.count(2000));
        assertEquals(0,window.count(2_000_000_000L));
    }
}
