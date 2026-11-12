package me.xchro.neon;

import me.xchro.neon.core.NumberDrag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NumberDragTest {
    @Test void fineMovementAccumulatesWithoutRoundingEachEvent() {
        var drag = new NumberDrag(0, 100, 1, 50, 0, 0, 100, true);
        for (int x = 1; x < 5; x++) assertEquals(50, drag.move(x, 0, 100, true));
        assertEquals(51, drag.move(5, 0, 100, true));
        assertEquals(51, drag.move(10, 0, 100, true));
    }
    @Test void changingPrecisionNeverRemapsThePointer() {
        var drag = new NumberDrag(1, 16, .5, 4, 100, 70, 150, false);
        assertEquals(5, drag.move(110, 70, 150, false));
        assertEquals(5, drag.move(110, 70, 150, true));
        assertEquals(5.5, drag.move(160, 70, 150, true));
        assertEquals(5.5, drag.move(160, 70, 150, false));
        assertEquals(6.5, drag.move(170, 70, 150, false));
    }
    @Test void excursionsPastBothEndsNeverOffsetTheReturningPointer() {
        var drag = new NumberDrag(0, 100, 1, 90, 290, 200, 100, false);
        for (int repeat = 0; repeat < 3; repeat++) {
            assertEquals(100, drag.move(1000, 200, 100, false));
            assertEquals(100, drag.move(350, 200, 100, false));
            assertEquals(75, drag.move(275, 200, 100, false));
            assertEquals(0, drag.move(-1000, 200, 100, false));
            assertEquals(0, drag.move(150, 200, 100, false));
            assertEquals(25, drag.move(225, 200, 100, false));
            assertEquals(50, drag.move(250, 200, 100, false));
        }
    }
    @Test void directFractionalPositionDoesNotInheritInitialRoundingError() {
        var drag = new NumberDrag(1, 16, .5, 4, 133, 100, 150, false);
        assertEquals(4.5, drag.value());
        assertEquals(4.5, drag.move(137, 100, 150, false));
        assertEquals(5, drag.move(140, 100, 150, false));
        assertEquals(16, drag.move(500, 100, 150, false));
        assertEquals(4.5, drag.move(133, 100, 150, false));
    }
    @Test void enteringPrecisionKeepsTheUnroundedPointerPosition() {
        var drag = new NumberDrag(0, 100, 1, 0, 12.4, 0, 100, false);
        assertEquals(12, drag.value());
        assertEquals(13, drag.move(13.4, 0, 100, true));
    }
    @Test void precisionRemainsRelativeUntilTheNextGesture() {
        var drag = new NumberDrag(1, 16, .5, 7.125, 100, 0, 150, true);
        assertEquals(8, drag.move(200, 0, 150, true));
        assertEquals(9, drag.move(210, 0, 150, false));
        var next = new NumberDrag(1, 16, .5, 9, 75, 0, 150, false);
        assertEquals(8.5, next.value());
    }
    @Test void directPositionUsesTheCurrentTrackGeometry() {
        var drag = new NumberDrag(0, 100, 1, 50, 150, 100, 100, false);
        assertEquals(50, drag.move(300, 200, 200, false));
        assertEquals(25, drag.move(250, 200, 200, false));
    }
    @Test void keyboardUsesConfiguredStepsAndPreservesFractionalStartingValues() {
        assertEquals(7.625, NumberDrag.nudge(7.125, 1, 16, .5, 1, false));
        assertEquals(12.125, NumberDrag.nudge(7.125, 1, 16, .5, 1, true));
        assertEquals(1, NumberDrag.nudge(4, 1, 16, .5, -1, true));
        assertEquals(16, NumberDrag.nudge(15, 1, 16, .5, 1, true));
        assertEquals(.55, NumberDrag.nudge(.5, .5, 3, .05, 1, false));
    }
    @Test void quantizationIsAnchoredAtTheMinimumAndIncludesBothEndpoints() {
        assertEquals(1, NumberDrag.quantize(-100, 1, 16, .5));
        assertEquals(16, NumberDrag.quantize(100, 1, 16, .5));
        assertEquals(1.3, NumberDrag.quantize(1.26, 1.1, 4, .2));
    }
}
