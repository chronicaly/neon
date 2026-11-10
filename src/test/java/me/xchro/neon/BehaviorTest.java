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
    @Test void exactValuesValidateRangeWithoutRoundingToSliderSteps() {
        assertEquals(4.125, NumberValue.parse("4.125", 1, 16, false).orElseThrow());
        assertEquals(53, NumberValue.parse("53", 0, 500, true).orElseThrow());
        assertTrue(NumberValue.parse("NaN", 1, 16, false).isEmpty());
        assertTrue(NumberValue.parse("Infinity", 1, 16, false).isEmpty());
        assertTrue(NumberValue.parse("1e999", 1, 16, false).isEmpty());
        assertTrue(NumberValue.parse("17", 1, 16, false).isEmpty());
        assertTrue(NumberValue.parse("2.5", 0, 60, true).isEmpty());
        assertTrue(NumberValue.parse("", 0, 60, true).isEmpty());
        assertEquals(0, NumberValue.parse("0", 0, 0, true).orElseThrow());
        assertEquals("4.125", NumberValue.format(4.125));
    }
    @Test void sectionResetsLeaveOtherSectionsAndSnapshotsUntouched() {
        NeonConfig config = new NeonConfig();
        config.zoomFactor = 7; config.zoomToggle = true; config.zoomScrollStep = 1.25;
        config.leftIntervalTicks = 43; config.clickerEntitiesOnly = true; config.armor.x = .37;
        NeonConfig snapshot = ConfigStore.copy(config);
        config.resetCamera(); assertEquals(4, config.zoomFactor); assertFalse(config.zoomToggle);
        assertEquals(43, config.leftIntervalTicks); assertEquals(.37, config.armor.x);
        assertEquals(7, snapshot.zoomFactor); assertTrue(snapshot.zoomToggle); assertEquals(1.25, snapshot.zoomScrollStep);
        config.resetClicker(); assertEquals(0, config.leftIntervalTicks); assertFalse(config.clickerEntitiesOnly);
        assertEquals(.37, config.armor.x);
        config.resetHud(); assertEquals(1, config.armor.x); assertEquals(7, snapshot.zoomFactor);
    }
    @Test void profilesPersistIndependentCopiesAndKeepVisualBaseline() throws Exception {
        Path path = directory.resolve("neon-profiles.json");
        NeonConfig baseline = new NeonConfig(); baseline.zoomFactor = 7; baseline.armor.x = .23;
        ProfileStore store = new ProfileStore(path); store.initialize(baseline);
        assertEquals(java.util.List.of("Combat", "Mining", "Building"), store.names());
        assertEquals(7, store.get("Combat").zoomFactor); assertEquals(.23, store.get("Mining").armor.x);
        assertTrue(store.get("Combat").clickerEntitiesOnly); assertTrue(store.get("Combat").toggleClicker);
        assertEquals(NeonConfig.LeftAction.HOLD, store.get("Mining").leftAction);
        assertTrue(store.get("Building").rightClick); assertFalse(store.get("Building").leftClick);
        store.put("My setup", baseline); baseline.armor.x = .99;
        NeonConfig loaded = store.get("My setup"); loaded.zoomFactor = 12;
        assertEquals(7, store.get("My setup").zoomFactor); assertEquals(.23, store.get("My setup").armor.x);
        ProfileStore reopened = new ProfileStore(path); reopened.initialize(new NeonConfig());
        assertEquals(7, reopened.get("My setup").zoomFactor);
        for (String name : reopened.names()) reopened.delete(name);
        reopened.initialize(baseline); assertTrue(reopened.names().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> reopened.put(" \n ", baseline));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }
    @Test void invalidProfilesAreBackedUpBeforeRecovery() throws Exception {
        Path path = directory.resolve("neon-profiles.json"); Files.writeString(path, "{bad");
        ProfileStore store = new ProfileStore(path); store.initialize(new NeonConfig());
        assertEquals(3, store.names().size());
        try (var files = Files.list(directory)) { assertEquals(2, files.count()); }
    }
    @Test void hudSnapsToEdgesAndCenterOnlyWithinThreshold() {
        var b = new HudBounds(40, 40, 80, 20);
        assertEquals(0, b.snapped(3, 90, 320, 240, 4).x());
        assertEquals(240, b.snapped(237, 90, 320, 240, 4).x());
        assertEquals(120, b.snapped(122, 90, 320, 240, 4).x());
        assertEquals(110, b.snapped(50, 107, 320, 240, 4).y());
        assertEquals(126, b.snapped(126, 90, 320, 240, 4).x());
        assertEquals(0, b.snapped(-100, -100, 320, 240, 4).y());
    }
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
