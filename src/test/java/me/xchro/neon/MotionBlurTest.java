package me.xchro.neon;

import me.xchro.neon.config.*;
import me.xchro.neon.core.FrameHistory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class MotionBlurTest {
    @TempDir Path directory;

    @Test void unityStrengthIsIndependentOfFrameRate() {
        double expected = Math.pow(.75, 6); // History surviving a tenth of a second.
        for (int fps : new int[]{30, 60, 120, 240}) {
            FrameHistory history = new FrameHistory();
            assertEquals(0, history.weight(0, 75));
            double retained = 1;
            for (int frame = 1; frame <= fps / 10; frame++) retained *= history.weight(Math.round(frame * 1e9 / fps), 75);
            assertEquals(expected, retained, 1e-6, "fps=" + fps);
        }
    }

    @Test void firstFramesStallsAndResetNeverReuseOldHistory() {
        FrameHistory history = new FrameHistory();
        assertEquals(0, history.weight(1_000_000_000, 75));
        assertTrue(history.weight(1_016_666_667, 75) > 0);
        assertEquals(0, history.weight(2_000_000_000, 75));
        assertTrue(history.weight(2_016_666_667, 75) > 0);
        history.reset(); assertEquals(0, history.weight(2_033_333_334L, 75));
        assertEquals(0, history.weight(2_033_333_334L, 75));
        assertEquals(0, history.weight(1, 75));
    }

    @Test void strengthEndpointsStayFiniteAndNeverFreezeTheImage() {
        FrameHistory history = new FrameHistory();
        history.weight(0, 0);
        assertEquals(0, history.weight(16_666_667, 0));
        assertEquals(.98, history.weight(33_333_334, 100), 1e-6);
        assertEquals(.98, history.weight(50_000_001, Integer.MAX_VALUE), 1e-6);
        assertEquals(0, history.weight(66_666_668, -1));
    }

    @Test void oldSettingsGainSafeDefaultsAndNewSettingsRoundTrip() throws Exception {
        NeonConfig old = ConfigStore.decode("{\"zoomFactor\":7}");
        assertFalse(old.motionBlurEnabled); assertEquals(75, old.motionBlurStrength); assertTrue(old.motionBlurIncludeUi);
        old.motionBlurEnabled = true; old.motionBlurStrength = 83; old.motionBlurIncludeUi = false;
        ConfigStore store = new ConfigStore(directory.resolve("neon.json")); store.save(old);
        NeonConfig loaded = store.load();
        assertTrue(loaded.motionBlurEnabled); assertEquals(83, loaded.motionBlurStrength); assertFalse(loaded.motionBlurIncludeUi);
        assertEquals(7, loaded.zoomFactor);
        loaded.motionBlurStrength = 101; loaded.sanitize(); assertEquals(100, loaded.motionBlurStrength);
        loaded.motionBlurStrength = -1; loaded.sanitize(); assertEquals(0, loaded.motionBlurStrength);
    }

    @Test void profilesAndCameraResetIncludeMotionBlur() throws Exception {
        NeonConfig config = new NeonConfig(); config.motionBlurEnabled = true; config.motionBlurStrength = 62; config.motionBlurIncludeUi = false;
        config.leftIntervalTicks = 23;
        ProfileStore profiles = new ProfileStore(directory.resolve("profiles.json")); profiles.initialize(config);
        profiles.put("Motion", config); config.resetCamera();
        assertFalse(config.motionBlurEnabled); assertEquals(75, config.motionBlurStrength); assertTrue(config.motionBlurIncludeUi);
        assertEquals(23, config.leftIntervalTicks);
        ProfileStore reopened = new ProfileStore(directory.resolve("profiles.json")); reopened.initialize(config);
        assertTrue(reopened.get("Motion").motionBlurEnabled); assertEquals(62, reopened.get("Motion").motionBlurStrength);
        assertFalse(reopened.get("Motion").motionBlurIncludeUi); assertTrue(reopened.get("Combat").motionBlurEnabled);
    }
}
