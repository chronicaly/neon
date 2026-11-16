package me.xchro.neon;

import java.util.List;
import me.xchro.neon.hud.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudMagnetTest {
    final HudBounds other=new HudBounds(500,300,100,60);
    @Test void catchesBeforeOverlapAndReleasesAfterDeliberatePush(){
        var drag=new HudSnap.Drag();var moving=new HudBounds(300,300,100,40);
        var a=drag.move(moving,List.of(other),1280,800,390,300,10,false);
        assertEquals(394,a.x());assertEquals(494,a.guideX());
        moving=new HudBounds(394,300,100,40);
        assertEquals(394,drag.move(moving,List.of(other),1280,800,410,300,10,false).x());
        assertEquals(425,drag.move(moving,List.of(other),1280,800,425,300,10,false).x());
        assertEquals(498,drag.move(new HudBounds(425,300,100,40),List.of(other),1280,800,498,300,10,true).x());
    }
    @Test void fastCrossingStillCatchesOnceAndRequiresMorePointerTravel(){
        var drag=new HudSnap.Drag();var moving=new HudBounds(300,300,100,40);
        assertEquals(394,drag.move(moving,List.of(other),1280,800,550,300,10,false).x());
        moving=new HudBounds(394,300,100,40);
        assertEquals(394,drag.move(moving,List.of(other),1280,800,550,300,10,false).x());
        assertEquals(580,drag.move(moving,List.of(other),1280,800,580,300,10,false).x());
    }
    @Test void catchesAllApproachDirections(){
        assertEquals(606,new HudSnap.Drag().move(new HudBounds(700,300,100,40),List.of(other),1280,800,602,300,10,false).x());
        assertEquals(254,new HudSnap.Drag().move(new HudBounds(500,100,100,40),List.of(other),1280,800,500,250,10,false).y());
        assertEquals(366,new HudSnap.Drag().move(new HudBounds(500,500,100,40),List.of(other),1280,800,500,360,10,false).y());
    }
    @Test void existingOverlapStaysEditableAndHiddenWidgetsReleaseTheMagnet(){
        var drag=new HudSnap.Drag();
        assertEquals(525,drag.move(new HudBounds(520,310,100,40),List.of(other),1280,800,525,310,10,false).x());
        drag.move(new HudBounds(300,300,100,40),List.of(other),1280,800,390,300,10,false);
        assertEquals(410,drag.move(new HudBounds(394,300,100,40),List.of(),1280,800,410,300,10,false).x());
        drag.clear();assertEquals(525,drag.move(new HudBounds(520,310,100,40),List.of(other),1280,800,525,310,10,false).x());
    }
    @Test void clusterContactAlsoAlignsNeighborEdgesWithoutGeneralAlignment(){
        var result=new HudSnap.Drag().move(new HudBounds(300,303,100,40),List.of(other),1280,800,390,303,10,false);
        assertEquals(394,result.x());assertEquals(300,result.y());
    }
}
