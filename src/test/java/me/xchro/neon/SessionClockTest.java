package me.xchro.neon;

import me.xchro.neon.core.SessionClock;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionClockTest {
    @Test void startsOnConnectionAndClearsOnDisconnect() {
        var clock=new SessionClock();assertEquals(0,clock.seconds(100));clock.connect(10_000_000_000L);
        assertEquals(65,clock.seconds(75_000_000_000L));clock.disconnect();assertEquals(0,clock.seconds(80_000_000_000L));
        clock.connect(90_000_000_000L);assertEquals(1,clock.seconds(91_000_000_000L));
        assertEquals(0,clock.seconds(10_000_000_000L));
    }
    @Test void durationsDoNotWrapAtTwentyFourHours() {
        assertEquals("0:00:00",SessionClock.format(-1));assertEquals("0:01:05",SessionClock.format(65));
        assertEquals("27:01:01",SessionClock.format(27*3600+61));
    }
}
