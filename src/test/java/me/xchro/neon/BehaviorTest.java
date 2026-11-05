package me.xchro.neon;

import me.xchro.neon.core.*;
import me.xchro.neon.config.*;
import me.xchro.neon.hud.HudBounds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class BehaviorTest {
    @TempDir Path directory;
    @Test void animationIsFiniteAndFrameRateIndependent() {
        Tween motion = new Tween(); motion.to(1, 0, 250);
        assertEquals(0, motion.value(0)); assertEquals(.5, motion.value(125_000_000), 1e-8);
        assertEquals(1, motion.value(250_000_000)); assertEquals(1, motion.value(1_000_000_000));
        motion.to(0, 1_000_000_000, 0); assertEquals(0, motion.value(1_000_000_000));
    }
    @Test void reversalStartsAtCurrentPose() {
        Tween motion = new Tween(); motion.to(1, 0, 250);
        double before = motion.value(100_000_000);
        motion.to(0, 100_000_000, 250);
        assertEquals(before, motion.value(100_000_000));
        assertTrue(motion.value(150_000_000) < before);
        assertEquals(0, motion.value(350_000_000));
    }
    @Test void zoomPreservesVanillaWhenReleased() {
        assertEquals(90, Tween.zoomFov(90, 1), 1e-8);
        assertEquals(28.0724869, Tween.zoomFov(90, 4), 1e-6);
    }
    @Test void actionsRequireFreshPressAfterFocusLoss() {
        Activation action = new Activation();
        assertFalse(action.update(true, true, false));
        assertFalse(action.update(true, false, false));
        assertTrue(action.update(true, true, false));
        assertFalse(action.update(false, true, false));
        assertFalse(action.update(true, true, false));
        action.update(true, false, false); assertTrue(action.update(true, true, false));
    }
    @Test void toggleCannotResumeAfterMenuOrWorldChange() {
        Activation action = new Activation(); action.update(true, false, true);
        assertTrue(action.update(true, true, true)); assertTrue(action.update(true, false, true));
        assertFalse(action.update(true, true, true));
        action.update(false, false, true); assertFalse(action.update(true, true, true));
    }
    @Test void everyCustomIntervalUsesExactTicksWithoutWaitingForVanilla() {
        for (int interval = 1; interval <= 60; interval++) {
            ClickClock clock = new ClickClock();
            for (long tick = 0; tick < 600; tick++) {
                assertEquals(tick % interval == 0, clock.press(tick, interval, false), "interval=" + interval + ", tick=" + tick);
                assertFalse(clock.press(tick, interval, true), "Never click twice in one tick");
            }
        }
    }
    @Test void zeroUsesVanillaReadinessAndNeverClicksTwiceInOneTick() {
        ClickClock clock = new ClickClock();
        assertFalse(clock.press(0, 0, false)); assertTrue(clock.press(0, 0, true));
        assertFalse(clock.press(0, 0, true)); assertFalse(clock.press(1, 0, false));
        assertTrue(clock.press(2, 0, true)); assertTrue(clock.press(3, 0, true));
    }
    @Test void missedTicksNeverCreateCatchUpBurstsAndResetStartsFresh() {
        ClickClock clock = new ClickClock();
        assertTrue(clock.press(0, 60, true)); assertFalse(clock.press(59, 60, true));
        assertTrue(clock.press(1000, 60, true)); assertFalse(clock.press(1000, 60, true));
        assertFalse(clock.press(1001, 60, true)); assertTrue(clock.press(1060, 60, true));
        clock.reset(); assertTrue(clock.press(1061, 60, true));
    }
    @Test void configRoundTripAndBounds() throws Exception {
        var store = new ConfigStore(directory.resolve("neon.json"));
        NeonConfig config = new NeonConfig(); config.zoomFactor = 8; config.leftIntervalTicks = 100; config.rightIntervalTicks = -1;
        config.armor.scale = Double.NaN; config.totems.x = 2; store.save(config);
        NeonConfig loaded = store.load();
        assertEquals(8, loaded.zoomFactor); assertEquals(60, loaded.leftIntervalTicks); assertEquals(0, loaded.rightIntervalTicks);
        assertEquals(1, loaded.armor.scale); assertEquals(1, loaded.totems.x);
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }
    @Test void malformedConfigIsPreserved() throws Exception {
        Path path = directory.resolve("neon.json"); Files.writeString(path, "{bad");
        assertEquals(150, new ConfigStore(path).load().zoomMillis);
        try (var files = Files.list(directory)) { assertEquals(2, files.count()); }
    }
    @Test void hudClampsOnResizeAndDrag() {
        var widget = new NeonConfig.Widget(1, 1); widget.scale = 3;
        for (int width : new int[]{50, 320, 640, 1920}) {
            HudBounds bounds = HudBounds.of(widget, 82, 22, width, 240);
            assertTrue(bounds.x() >= 0 && bounds.x() + bounds.width() <= width);
            assertTrue(bounds.y() + bounds.height() <= 240);
            bounds.move(widget, -100, 1000, width, 240);
            assertEquals(0, widget.x); assertEquals(1, widget.y);
        }
    }
    @Test void armorCanTouchEveryEdgeAndFitVerticalAtLargeScale() {
        var widget = new NeonConfig.Widget(0, 0); widget.scale = 3;
        var topLeft = HudBounds.of(widget, 22, 82, 320, 240);
        assertEquals(0, topLeft.x()); assertEquals(0, topLeft.y());
        assertTrue(topLeft.height() <= 240);
        topLeft.move(widget, 1000, 1000, 320, 240);
        var bottomRight = HudBounds.of(widget, 22, 82, 320, 240);
        assertEquals(320, bottomRight.x() + bottomRight.width());
        assertEquals(240, bottomRight.y() + bottomRight.height());
        assertTrue(82 * HudBounds.scale(widget, 22, 82, 320, 240) <= 240);
    }
    @Test void oldConfigRetainsPreferencesAndDropsFreelookAnimation() throws Exception {
        Path path = directory.resolve("neon.json");
        Files.writeString(path, "{\"freelookMillis\":500,\"zoomFactor\":8,\"totems\":{\"visible\":false,\"x\":1,\"y\":1},\"armor\":{\"x\":0.3,\"y\":0.6,\"scale\":2}}");
        ConfigStore store = new ConfigStore(path); NeonConfig config = store.load();
        assertEquals(8, config.zoomFactor); assertFalse(config.totems.visible);
        assertEquals(.3, config.armor.x); assertEquals(2, config.armor.scale);
        assertFalse(config.armorVertical); assertTrue(config.armorSlots);
        config.armorVertical = true; config.armorAnchor = NeonConfig.ArmorAnchor.HOTBAR_LEFT;
        store.save(config);
        assertFalse(Files.readString(path).contains("freelookMillis"));
        assertTrue(store.load().armorVertical);
        assertEquals(NeonConfig.ArmorAnchor.HOTBAR_LEFT, store.load().armorAnchor);
    }
    @Test void cpsConfigUpgradesToVanillaAttackOnlyWithoutChangingOtherPreferences() throws Exception {
        Path path = directory.resolve("neon.json");
        Files.writeString(path, "{\"version\":1,\"leftCps\":1,\"rightCps\":20,\"continuousMining\":true,\"toggleClicker\":true,\"clickerEnabled\":true}");
        ConfigStore store = new ConfigStore(path); NeonConfig config = store.load();
        assertEquals(2, config.version); assertEquals(0, config.leftIntervalTicks); assertEquals(0, config.rightIntervalTicks);
        assertEquals(NeonConfig.LeftAction.CLICK, config.leftAction);
        assertTrue(config.toggleClicker); assertTrue(config.clickerEnabled);
        config.leftAction = NeonConfig.LeftAction.HOLD; config.leftIntervalTicks = 60; store.save(config);
        assertEquals(NeonConfig.LeftAction.HOLD, store.load().leftAction); assertEquals(60, store.load().leftIntervalTicks);
        assertFalse(Files.readString(path).contains("Cps")); assertFalse(Files.readString(path).contains("continuousMining"));
    }
}
