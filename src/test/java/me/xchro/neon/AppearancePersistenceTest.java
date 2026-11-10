package me.xchro.neon;

import java.nio.file.*;
import me.xchro.neon.config.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AppearancePersistenceTest {
    @TempDir Path directory;
    @Test void independentHudAndAppearanceDefaultsMigrateAndProfilesPersistThem() throws Exception {
        NeonConfig old = ConfigStore.decode("{\"armor\":{\"x\":0.2,\"y\":0.6,\"scale\":1.4},\"armorAnchor\":\"CUSTOM\"}");
        assertEquals(1, old.hudScale); assertEquals(10, old.snapSensitivity);
        assertEquals(.94, old.theme.opacity); assertEquals(.4, old.theme.soundVolume);
        assertEquals(.2, old.armor.x); assertEquals(.6, old.armor.y); assertEquals(1.4, old.armor.scale);
        old.hudScale = 1.25; old.snapSensitivity = 7; old.theme.opacity = .8; old.theme.soundVolume = .2;
        ProfileStore profiles = new ProfileStore(directory.resolve("profiles.json")); profiles.initialize(old); profiles.put("Independent", old);
        var loaded = profiles.get("Independent");
        assertEquals(1.25, loaded.hudScale); assertEquals(7, loaded.snapSensitivity);
        assertEquals(.8, loaded.theme.opacity); assertEquals(.2, loaded.theme.soundVolume);
        old.hudScale = Double.NaN; old.snapSensitivity = 100; old.theme.opacity = -1; old.theme.soundVolume = 5; old.sanitize();
        assertEquals(1, old.hudScale); assertEquals(24, old.snapSensitivity);
        assertEquals(.4, old.theme.opacity); assertEquals(1, old.theme.soundVolume);
    }
    @Test void cameraModuleAvailabilityMigratesAndPersistsWithoutChangingBindings() {
        NeonConfig old=ConfigStore.decode("{\"zoomFactor\":7.25}");
        assertTrue(old.freelookEnabled); assertTrue(old.zoomEnabled);
        old.freelookEnabled=false; old.zoomEnabled=false;
        NeonConfig saved=ConfigStore.decode(ConfigStore.encode(old));
        assertFalse(saved.freelookEnabled); assertFalse(saved.zoomEnabled); assertEquals(7.25,saved.zoomFactor);
    }
    @Test void oldConfigurationsReceiveTheApprovedPaletteAndPreserveSettings() {
        NeonConfig old = ConfigStore.decode("{\"zoomFactor\":7.25,\"theme\":null}");
        assertEquals(7.25, old.zoomFactor);
        assertEquals(0xff0e131b, old.theme.background);
        assertEquals(0xff84dce6, old.theme.accent);
        NeonConfig partial = ConfigStore.decode("{\"theme\":{\"background\":1122867}}");
        assertEquals(0xff112233, partial.theme.background);
        assertEquals(0xff73c8d3, partial.theme.outline);
    }
    @Test void profileSnapshotsIncludeIndependentAppearanceAndExcludeWindowPlacement() throws Exception {
        NeonConfig config = new NeonConfig(); config.theme.background = 0xff163247;
        ProfileStore profiles = new ProfileStore(directory.resolve("profiles.json")); profiles.initialize(config);
        profiles.put("Custom", config); config.theme.background = 0xff334455;
        NeonConfig loaded = profiles.get("Custom"); loaded.theme.accent = 0xffaabbcc;
        assertEquals(0xff163247, profiles.get("Custom").theme.background);
        assertEquals(0xff84dce6, profiles.get("Custom").theme.accent);
        MenuPlacement placement = new MenuPlacement(); placement.customized = true; placement.x = .12; placement.width = .64;
        Path window = directory.resolve("window.json"); placement.save(window);
        String before = Files.readString(window);
        profiles.put("Custom", config); profiles.get("Custom"); profiles.delete("Custom");
        assertEquals(before, Files.readString(window));
        assertEquals(.12, MenuPlacement.load(window).x); assertEquals(.64, MenuPlacement.load(window).width);
    }
    @Test void malformedWindowPreferencesCannotProduceUnreachableGeometry() throws Exception {
        Path path = directory.resolve("window.json");
        for (String invalid : new String[]{"{bad", "null", "{\"width\":0}", "{\"height\":-2}", "{\"x\":NaN}"}) {
            Files.writeString(path, invalid);
            assertFalse(MenuPlacement.load(path).customized); assertEquals(.5, MenuPlacement.load(path).width);
        }
        Files.writeString(path, "{\"layoutVersion\":2,\"x\":-99,\"y\":50,\"width\":90,\"height\":0.01,\"customized\":true}");
        MenuPlacement sanitized = MenuPlacement.load(path);
        assertEquals(0, sanitized.x); assertEquals(1, sanitized.y);
        assertEquals(1, sanitized.width); assertEquals(.1, sanitized.height);
    }
    @Test void oversizedLegacyMenuPlacementReturnsToTheCompactDefault() throws Exception {
        Path path = directory.resolve("window.json");
        Files.writeString(path, "{\"x\":0.03,\"y\":0.1,\"width\":0.46,\"height\":0.67,\"customized\":true}");
        assertFalse(MenuPlacement.load(path).customized);
        assertEquals(4, MenuPlacement.load(path).layoutVersion);
    }
    @Test void compactPlacementGrowsFifteenPercentOnlyOnce() throws Exception {
        Path path = directory.resolve("window.json");
        Files.writeString(path, "{\"layoutVersion\":3,\"x\":0.2,\"y\":0.3,\"width\":0.4,\"height\":0.4,\"customized\":true}");
        MenuPlacement enlarged = MenuPlacement.load(path);
        assertEquals(.46, enlarged.width, .000001); assertEquals(.46, enlarged.height, .000001);
        assertEquals(.2, enlarged.x); assertEquals(.3, enlarged.y); assertTrue(enlarged.customized);
        enlarged.save(path);
        assertEquals(enlarged.width, MenuPlacement.load(path).width);
        assertEquals(enlarged.height, MenuPlacement.load(path).height);
    }
    @Test void olderCompactPlacementReceivesBothDensityIncreases() throws Exception {
        Path path = directory.resolve("window.json");
        Files.writeString(path, "{\"layoutVersion\":2,\"width\":0.4,\"height\":0.4,\"customized\":true}");
        MenuPlacement enlarged = MenuPlacement.load(path);
        assertEquals(.529, enlarged.width, .000001); assertEquals(.529, enlarged.height, .000001);
        assertEquals(4, enlarged.layoutVersion);
        enlarged.save(path); assertEquals(enlarged.width, MenuPlacement.load(path).width);
    }
}
