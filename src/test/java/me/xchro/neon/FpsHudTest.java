package me.xchro.neon;

import me.xchro.neon.hud.*;
import me.xchro.neon.config.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class FpsHudTest {
    @TempDir Path directory;
    @Test void clockMeasuresRenderFramesAndPublishesOnSixtyHzBuckets() {
        for (int rate : new int[]{30, 60, 144, 240}) {
            var meter = new HudFrameRate();
            // Offset from bucket boundaries so nanosecond rounding cannot straddle adjacent slots.
            for (int i = 0; i <= rate; i++) meter.frame(10_001_000_000L + Math.round(i * 1_000_000_000.0 / rate));
            assertEquals(rate, meter.fps());
            assertEquals(Math.min(rate, 60) + 1, meter.updates());
            long updates = meter.updates();
            meter.frame(11_001_000_000L); meter.frame(10_000_000_000L);
            assertEquals(updates, meter.updates()); assertEquals(rate, meter.fps());
            meter.frame(13_000_000_000L); assertEquals(0, meter.fps());
            meter.frame(13_010_000_000L); meter.frame(13_020_000_000L); assertEquals(100, meter.fps());
        }
    }
    @Test void smoothingConvergesWithoutOvershootAndHasNoFrameRateDependence() {
        long start = 10_000_000_000L;
        var one = new HudFpsTransition(); one.update(60, start); one.update(240, start + 120_000_000);
        assertTrue(one.value() > 60 && one.value() < 240);
        for (int rate : new int[]{30, 60, 144, 240}) {
            var many = new HudFpsTransition(); many.update(60, start);
            for (long offset = 1_000_000_000L / rate; offset < 120_000_000; offset += 1_000_000_000L / rate) many.update(240, start + offset);
            many.update(240, start + 120_000_000); assertEquals(one.value(), many.value());
            int last = many.value();
            for (int ms = 180; ms <= 960; ms += 60) {
                many.update(240, start + ms * 1_000_000L);
                assertTrue(many.value() >= last && many.value() <= 240); last = many.value();
            }
            assertEquals(240, many.value());
            many.update(30, start + 980_000_000); assertTrue(many.value() < 240 && many.value() > 30);
            many.update(500, start + 3_000_000_000L); assertEquals(500, many.value());
            many.update(-1, start + 3_010_000_000L); assertEquals(0, many.value());
            many.update(144, start + 3_020_000_000L); assertEquals(144, many.value());
            many.update(60, start); assertEquals(60, many.value());
        }
    }
    @Test void existingSettingsKeepTheirValuesAndReceiveUnityFpsDefaults() {
        var c = ConfigStore.decode("{\"armor\":{\"x\":0.37,\"y\":0.62,\"scale\":1.125},\"totems\":{\"x\":0.4,\"y\":0.5},\"zoomFactor\":7.125}");
        assertEquals(.37, c.armor.x); assertEquals(.62, c.armor.y); assertEquals(1.125, c.armor.scale);
        assertEquals(.4, c.totems.x); assertEquals(7.125, c.zoomFactor);
        assertTrue(c.fps.visible); assertEquals(1, c.fps.x); assertEquals(1, c.fps.y); assertEquals(1, c.fps.scale);
        assertTrue(c.fpsShadow); assertFalse(c.fpsAnimation); assertFalse(c.fpsBackground); assertFalse(c.fpsBorder);
        assertNotNull(ConfigStore.decode("{\"fps\":null}").fps);
    }
    @Test void profilesAndConfigPreserveFpsAppearanceAndFractionalPlacement() throws Exception {
        var c = new NeonConfig(); c.fps.x = .237; c.fps.y = .684; c.fps.scale = 1.125; c.fps.visible = false;
        c.fpsAnimation = c.fpsBackground = c.fpsBorder = true; c.fpsShadow = false;
        var config = new ConfigStore(directory.resolve("neon.json")); config.save(c);
        var profiles = new ProfileStore(directory.resolve("profiles.json")); profiles.initialize(c); profiles.put("FPS", c);
        var reopened = new ProfileStore(directory.resolve("profiles.json")); reopened.initialize(new NeonConfig());
        for (var saved : new NeonConfig[]{config.load(), reopened.get("FPS"), ConfigStore.copy(c)}) {
            assertEquals(.237, saved.fps.x); assertEquals(.684, saved.fps.y); assertEquals(1.125, saved.fps.scale);
            assertFalse(saved.fps.visible); assertTrue(saved.fpsAnimation); assertTrue(saved.fpsBackground); assertTrue(saved.fpsBorder); assertFalse(saved.fpsShadow);
        }
        c.resetHud(); assertTrue(c.fps.visible); assertFalse(c.fpsAnimation); assertTrue(c.fpsShadow); assertEquals(1, c.fps.x);
    }
}
