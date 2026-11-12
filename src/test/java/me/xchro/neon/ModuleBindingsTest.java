package me.xchro.neon;

import me.xchro.neon.config.*;
import me.xchro.neon.core.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModuleBindingsTest {
    @Test void expandingOutlineNeverPullsTheHighlightAlongAnUnchangedEdge() {
        for(long start:new long[]{1_000_000_000L,4_000_000_000L,7_000_000_000L,10_000_000_000L}) {
            var travel=new OutlineTravel();travel.update(200,100,582,370,0);
            travel.update(200,100,582,370,start);var p=travel.point();
            // Expansion, reverse expansion and a left-clamped expansion preserve tangential position.
            for(int width:new int[]{600,700,830,750,620,582}) {
                travel.update(200,100,width,370,start);var q=travel.point();
                assertEquals(p.edge(),q.edge());
                if(p.edge()%2==0)assertEquals(p.x(),q.x(),.00001);else assertEquals(p.y(),q.y(),.00001);
            }
        }
        var travel=new OutlineTravel();travel.update(200,100,582,370,0);
        travel.update(200,100,582,370,1_000_000_000L);
        travel.update(150,100,830,370,1_100_000_000L);
        assertEquals(376,travel.point().x(),.00001,"only the elapsed-time travel moves the glow");
        travel.update(150,100,582,370,1_200_000_000L);
        assertEquals(392,travel.point().x(),.00001);
    }
    @Test void cameraModesReleaseAndRearmAcrossFocusChanges() {
        for (BindMode mode : BindMode.values()) {
            var key = new Activation();
            assertFalse(key.update(true, true, mode), "held key cannot start after a reset");
            assertEquals(mode == BindMode.REVERSE_HOLD, key.update(true, false, mode));
            assertEquals(mode != BindMode.REVERSE_HOLD, key.update(true, true, mode));
            assertEquals(mode != BindMode.HOLD, key.update(true, false, mode));
            assertFalse(key.update(false, false, mode));
            assertFalse(key.update(true, true, mode));
        }
    }
    @Test void reverseHoldClickerStopsButNeverStartsFromMenuTyping() {
        var key = new ClickerActivation();
        assertTrue(key.update(true, true, false, BindMode.REVERSE_HOLD));
        assertFalse(key.update(true, true, true, BindMode.REVERSE_HOLD));
        assertFalse(key.update(true, false, false, BindMode.REVERSE_HOLD));
        assertTrue(key.update(true, true, false, BindMode.REVERSE_HOLD));
        assertFalse(key.update(true, false, true, BindMode.REVERSE_HOLD));
        assertFalse(key.update(false, true, false, BindMode.REVERSE_HOLD));
    }
    @Test void passiveBindingsPreserveSavedEnableWhileMomentaryAndIgnoreUiKeys() {
        var key = new VisibilityBinding();
        assertTrue(key.update(true, false, false, false, BindMode.HOLD).active());
        assertFalse(key.update(true, true, true, false, BindMode.HOLD).active());
        var pressed = key.update(true, true, true, true, BindMode.HOLD);
        assertTrue(pressed.enabled()); assertTrue(pressed.active());
        assertFalse(key.update(true, false, true, true, BindMode.HOLD).active());
        assertFalse(key.update(true, true, true, true, BindMode.HOLD).active());
        assertTrue(key.update(true, true, true, false, BindMode.REVERSE_HOLD).active());
        assertFalse(key.update(true, true, true, true, BindMode.REVERSE_HOLD).active());
        key.update(false, false, true, true, BindMode.TOGGLE);
        assertFalse(key.update(false, true, true, true, BindMode.TOGGLE).enabled());
        key.update(false, true, true, false, BindMode.TOGGLE);
        assertTrue(key.update(false, true, true, true, BindMode.TOGGLE).enabled());
    }
    @Test void legacyModesAndCustomSettingsSurviveRoundTrip() {
        var c = ConfigStore.decode("{\"zoomToggle\":true,\"toggleClicker\":false,\"motionBlurStrength\":73,\"freelookDistance\":7.5}");
        assertEquals(BindMode.TOGGLE,c.zoomMode()); assertEquals(BindMode.HOLD,c.clickerMode());
        c.zoomMode = c.clickerMode = c.motionBlurMode = c.totemPopsMode = c.freelookMode = BindMode.REVERSE_HOLD;
        var saved = ConfigStore.copy(c);
        assertEquals(BindMode.REVERSE_HOLD,saved.zoomMode()); assertEquals(BindMode.REVERSE_HOLD,saved.clickerMode());
        assertEquals(BindMode.REVERSE_HOLD,saved.freelookMode); assertEquals(BindMode.REVERSE_HOLD,saved.motionBlurMode);
        assertEquals(BindMode.REVERSE_HOLD,saved.totemPopsMode);assertEquals(73,saved.motionBlurStrength);assertEquals(7.5,saved.freelookDistance);
    }
    @Test void drawerReversalRetainsItsExactPositionAndEveryFrameFitsViewport() {
        for(int viewport:new int[]{484,968,1452})for(int width:new int[]{320,440,viewport-28}) {
            var motion=new ExpandingMenu(); motion.open(true,0);
            double previous=0;
            for(long time=0;time<=ExpandingMenu.DURATION;time+=1_000_000) {
                var frame=motion.frame(viewport-width-14,width,viewport,time);
                assertTrue(frame.x()>=14); assertTrue(frame.x()+frame.width()<=viewport-14+.001);
                assertTrue(frame.drawerWidth()>=previous);previous=frame.drawerWidth();
                assertEquals(frame.width(),frame.mainWidth()+frame.drawerWidth(),.000001);
            }
            long turn=120_000_000;
            var reversing=new ExpandingMenu();reversing.open(true,0);
            var before=reversing.frame(200,width,viewport,turn);reversing.open(false,turn);
            assertEquals(before,reversing.frame(200,width,viewport,turn));
            assertEquals(0,reversing.frame(200,width,viewport,turn+ExpandingMenu.DURATION).drawerWidth());
        }
    }
    @Test void widerDrawerUsesAvailableSpaceAndPreservesTheModulePane() {
        var motion = new ExpandingMenu(); motion.reveal();
        var normal = motion.frame(264, 440, 968, 0);
        assertEquals(300, normal.fullDrawerWidth());
        assertEquals(440, normal.mainWidth());
        assertEquals(740, normal.width());
        var small = motion.frame(80, 320, 484, 0);
        assertEquals(176, small.fullDrawerWidth());
        assertEquals(280, small.mainWidth());
        assertEquals(456, small.width());
        assertTrue(small.x() >= 14 && small.x() + small.width() <= 470);
    }
}
