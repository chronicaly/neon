package me.xchro.neon;

import me.xchro.neon.config.*;
import me.xchro.neon.hud.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudWidgetsTest {
    @Test void worldClockWrapsAtMidnightAndHandlesNegativeTimes() {
        assertEquals("06:00",HudNumbers.worldTime(0));assertEquals("12:00",HudNumbers.worldTime(6000));
        assertEquals("00:00",HudNumbers.worldTime(18000));assertEquals("06:00",HudNumbers.worldTime(24000));
        assertEquals("05:59",HudNumbers.worldTime(-1));assertEquals("18:00",HudNumbers.worldTime(-12000));
    }
    @Test void resetRestoresEveryNewWidgetOptionAndDisablesAddedWidgets() {
        var config=new NeonConfig();
        for(var kind:HudKind.values()) { config.hud(kind).placement.visible=true;config.hud(kind).placement.scale=2; }
        config.compassDegrees=config.keystrokesMouse=config.keystrokesJump=config.inventorySlots=config.heldItemOffhand=config.clock24Hour=config.lightSky=config.serverPlayers=false;
        config.coordinatesConversion=config.inventoryHotbar=config.heldItemPercent=config.clockSeconds=config.serverAddress=true;
        config.potionMaxRows=2;config.packMaxRows=1;config.worldTimeDay=false;config.counterItem=NeonConfig.CounterItem.DIAMONDS;config.counterOffhand=false;
        config.scoreboardRows=3;config.scoreboardTitle=config.scoreboardScores=false;config.resetHud();
        var expected=new NeonConfig();expected.sanitize();config.sanitize();
        assertEquals(ConfigStore.encode(expected),ConfigStore.encode(config));
    }
    @Test void speedUsesHorizontalDistanceAndActualTickRateButRejectsTeleports() {
        assertEquals(5,HudNumbers.horizontalSpeed(.15,.2,20),1e-9);
        assertEquals(2.5,HudNumbers.horizontalSpeed(.15,.2,10),1e-9);
        assertEquals(0,HudNumbers.horizontalSpeed(20,0,20));assertEquals(0,HudNumbers.horizontalSpeed(Double.NaN,0,20));
    }
    @Test void compassWrapsAcrossNorthAndMatchesMinecraftAxes() {
        assertEquals("S",HudNumbers.heading(0));assertEquals("W",HudNumbers.heading(90));
        assertEquals("N",HudNumbers.heading(180));assertEquals("E",HudNumbers.heading(-90));
        assertEquals("N",HudNumbers.heading(-540));assertEquals("SW",HudNumbers.heading(45));
        assertEquals(2,HudNumbers.angleDifference(1,359));assertEquals(-2,HudNumbers.angleDifference(359,1));
    }
    @Test void upgradesAreOptInAndPreserveExistingPlacementAndPalette() {
        var c=ConfigStore.decode("{\"armor\":{\"x\":0.32,\"y\":0.71,\"scale\":1.25},\"theme\":{\"paletteVersion\":1,\"alphaEnabled\":true,\"accent\":305419896}}");
        assertEquals(.32,c.armor.x); assertEquals(1.25,c.armor.scale); assertEquals(305419896,c.theme.accent);
        for(var kind:HudKind.values()) assertFalse(c.hud(kind).placement.visible);
        var card=c.hud(HudKind.COORDINATES); card.placement.visible=true; card.placement.x=.37; card.placement.scale=1.125; card.border=false;
        c.coordinatesConversion=true;
        var copied=ConfigStore.copy(c);
        assertTrue(copied.hud(HudKind.COORDINATES).placement.visible);
        assertEquals(.37,copied.hud(HudKind.COORDINATES).placement.x);
        assertEquals(1.125,copied.hud(HudKind.COORDINATES).placement.scale);
        assertFalse(copied.hud(HudKind.COORDINATES).border); assertTrue(copied.coordinatesConversion);
    }
    @Test void missingAndInvalidPlacementsRecoverDisabledWithinScreen() {
        for(String json:new String[]{"{\"hudWidgets\":null}","{\"hudWidgets\":{\"COORDINATES\":null}}","{\"hudWidgets\":{\"COORDINATES\":{\"placement\":null}}}"}) {
            var card=ConfigStore.decode(json).hud(HudKind.COORDINATES);
            assertFalse(card.placement.visible); assertTrue(card.placement.x>=0 && card.placement.x<=1);
        }
    }
    @Test void negativeCoordinatesAndNetherConversionsFloorInsteadOfTruncating() {
        assertEquals(-1,HudNumbers.coordinate(-.01,1)); assertEquals(-1,HudNumbers.coordinate(-.01,.125));
        assertEquals(16,HudNumbers.coordinate(128,.125)); assertEquals(-17,HudNumbers.coordinate(-128.1,.125));
        assertEquals(-9,HudNumbers.coordinate(-1.01,8)); assertEquals(30_000_000,HudNumbers.coordinate(30_000_000,1));
    }
}
