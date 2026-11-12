package me.xchro.neon;

import java.util.List;
import me.xchro.neon.hud.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudSnapTest {
    private final HudBounds widget = new HudBounds(0,0,100,40);
    @Test void centerGuideIdentifiesAlignmentInsteadOfOrigin() {
        var snap = HudSnap.move(widget,List.of(),1280,800,587,300,5);
        assertEquals(590,snap.x()); assertEquals(640,snap.guideX());
        assertEquals(300,snap.y()); assertEquals(-1,snap.guideY());
    }
    @Test void supportsCrossEdgeAndSixPixelSpacing() {
        var neighbor = List.of(new HudBounds(500,300,40,40));
        var edge = HudSnap.move(widget,neighbor,1280,800,401,300,2);
        assertEquals(400,edge.x()); assertEquals(500,edge.guideX());
        var before = HudSnap.move(widget,neighbor,1280,800,393,300,2);
        assertEquals(394,before.x()); assertEquals(494,before.guideX());
        var after = HudSnap.move(widget,neighbor,1280,800,547,300,2);
        assertEquals(546,after.x()); assertEquals(546,after.guideX());
        var far = HudSnap.move(widget,neighbor,1280,800,393,100,2);
        assertEquals(393,far.x()); assertEquals(-1,far.guideX());
    }
    @Test void respectsThresholdAndVisibleNeighborList() {
        assertEquals(590,HudSnap.move(widget,List.of(),1280,800,585,300,5).x());
        assertEquals(584,HudSnap.move(widget,List.of(),1280,800,584,300,5).x());
        assertEquals(401,HudSnap.move(widget,List.of(),1280,800,401,300,2).x());
    }
    @Test void hasNoForcedMarginAndNeverSnapsOutsideDisplay() {
        var edge=HudSnap.move(widget,List.of(),1280,800,-10,900,8);
        assertEquals(0,edge.x()); assertEquals(760,edge.y()); assertEquals(800,edge.guideY());
        var near=HudSnap.move(widget,List.of(),1280,800,12,100,2);
        assertEquals(12,near.x()); assertEquals(-1,near.guideX());
    }
}
