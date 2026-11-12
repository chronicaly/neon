package me.xchro.neon;

import me.xchro.neon.core.PressFeedback;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PressFeedbackTest {
    @Test void quickTapIsVisibleThenSettles() {
        var press = new PressFeedback(); press.press(0); press.release(1);
        assertTrue(press.value(25_000_000) > 0);
        assertEquals(1, press.value(50_000_000));
        assertTrue(press.value(100_000_000) > 0);
        assertEquals(0, press.value(150_000_000));
    }
    @Test void holdAndOutsideReleaseHaveFiniteTimings() {
        var press = new PressFeedback(); press.press(0);
        assertEquals(1, press.value(500_000_000)); press.release(500_000_000);
        assertEquals(0, press.value(600_000_000));
    }
    @Test void interruptedPressAndFocusLossDoNotStick() {
        var press = new PressFeedback(); press.press(0); press.release(60_000_000);
        double before = press.value(90_000_000); press.press(90_000_000);
        assertEquals(before, press.value(90_000_000)); press.clear();
        assertEquals(0, press.value(91_000_000));
    }
}
