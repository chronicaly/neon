package me.xchro.neon;

import java.nio.file.Path;
import me.xchro.neon.config.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ThemeTest {
    @TempDir Path directory;

    @Test void stockSurfacesUpgradeOnceWithoutChangingOutlineAlphaOrCustomColors() throws Exception {
        String legacy = "{\"theme\":{\"alphaEnabled\":true,\"background\":1208881947,\"cards\":-15591133,\"outline\":-9189165,\"secondary\":-6715956,\"opacity\":0.7},\"motionBlurStrength\":37}";
        var c = ConfigStore.decode(legacy);
        assertEquals(0x4807090e, c.theme.background);
        assertEquals(0xff0d1119, c.theme.cards);
        assertEquals(0xff73c8d3, c.theme.outline);
        assertEquals(0xff9985cc, c.theme.secondary);
        assertEquals(.7, c.theme.opacity); assertEquals(37, c.motionBlurStrength);
        // An intentional edit back to an old RGB value must survive subsequent loads.
        c.theme.background = 0x480e131b;
        assertEquals(c.theme.background, ConfigStore.copy(c).theme.background);
        var path = directory.resolve("profiles.json");
        java.nio.file.Files.writeString(path, "{\"profiles\":{\"Legacy\":" + legacy + "}}");
        var profiles = new ProfileStore(path); profiles.initialize(new NeonConfig());
        assertEquals(0x4807090e, profiles.get("Legacy").theme.background);
        assertEquals(0xff73c8d3, profiles.get("Legacy").theme.outline);
    }

    @Test void linkedSurfacesStayDarkAcrossBrightThemeColors() {
        var t = new NeonConfig.Theme();
        for (int primary : new int[]{0xffffffff, 0xffff0000, 0xff00ff00, 0xff0000ff, 0xff84dce6}) {
            t.themeColor(primary);
            for (ThemeColor role : new ThemeColor[]{ThemeColor.BACKGROUND, ThemeColor.CARDS}) {
                int color = t.color(role);
                for (int shift : new int[]{0, 8, 16}) assertTrue((color >>> shift & 255) <= 27);
            }
        }
    }

    @Test void migrationPreservesEverySavedColorAndAlpha() {
        var c = ConfigStore.decode("{\"theme\":{\"alphaEnabled\":true,\"background\":305419896,\"accent\":-1122868,\"opacity\":0.7,\"soundVolume\":0.2}}");
        for (ThemeColor role : ThemeColor.values()) assertEquals(role.local(c.theme), c.theme.color(role));
        assertEquals(0x12345678, c.theme.background);
        assertTrue(c.theme.syncedColors.isEmpty());
        assertEquals(.7, c.theme.opacity); assertEquals(.2, c.theme.soundVolume);
    }

    @Test void themeCoordinatesSurfacesAndKeepsIndependentAlphaAndBehavior() {
        var t = new NeonConfig.Theme(); t.alphaEnabled = true;
        t.background = 0x48112233; t.text = 0xcc445566;
        t.soundVolume = .23; t.opacity = .67; t.effects = NeonConfig.Effects.FULL;
        t.themeColor(0xffed80c2);
        assertEquals(0xffed80c2, t.color(ThemeColor.ACCENT));
        assertEquals(0xffed80c2, t.color(ThemeColor.OUTLINE));
        assertEquals(0x48, t.color(ThemeColor.BACKGROUND) >>> 24);
        assertEquals(0xcc, t.color(ThemeColor.TEXT) >>> 24);
        assertTrue((t.color(ThemeColor.BACKGROUND) & 255) < 80);
        assertTrue((t.color(ThemeColor.TEXT) & 255) > 230);
        assertEquals(.23, t.soundVolume); assertEquals(.67, t.opacity); assertEquals(NeonConfig.Effects.FULL, t.effects);
        t.color(ThemeColor.ACCENT, 0x66336699, true);
        assertFalse(t.syncedColors.contains(ThemeColor.ACCENT));
        assertEquals(0x66336699, t.color(ThemeColor.ACCENT));
        t.sync(ThemeColor.ACCENT, true);
        assertEquals(0x66ed80c2, t.color(ThemeColor.ACCENT));
        t.color(ThemeColor.ACCENT, 0x33ed80c2, false);
        assertTrue(t.syncedColors.contains(ThemeColor.ACCENT));
        t.sync(ThemeColor.ACCENT, false);
        assertEquals(0x33ed80c2, t.color(ThemeColor.ACCENT));
    }

    @Test void rainbowAnimatesLinkedColorsWithoutChangingSavedConfig() {
        var c = new NeonConfig(); var t = c.theme;
        t.rainbow(true); t.color(ThemeColor.TEXT, 0xffabcdef, true);
        String before = ConfigStore.encode(c);
        assertNotEquals(t.color(ThemeColor.ACCENT, 0L), t.color(ThemeColor.ACCENT, 6000L));
        assertEquals(t.color(ThemeColor.TEXT, 0L), t.color(ThemeColor.TEXT, 6000L));
        assertEquals(before, ConfigStore.encode(c));
        t.themeColor(0xff66aaff);
        assertFalse(t.rainbow); assertEquals(7, t.syncedColors.size());
    }

    @Test void themeLinksOverridesAndRainbowSurviveSettingsAndProfiles() throws Exception {
        var c = new NeonConfig(); c.theme.themeColor(0xffeeaa77); c.theme.rainbow(true);
        c.theme.alphaEnabled = true; c.theme.color(ThemeColor.TEXT, 0xccddeeff, true);
        c.theme.rainbowSeconds = 7; c.theme.rainbowSaturation = .42;
        var store = new ConfigStore(directory.resolve("neon.json")); store.save(c);
        var profiles = new ProfileStore(directory.resolve("profiles.json")); profiles.initialize(c); profiles.put("Theme", c);
        var reloaded = new ProfileStore(directory.resolve("profiles.json")); reloaded.initialize(new NeonConfig());
        for (var saved : new NeonConfig[]{store.load(), reloaded.get("Theme")}) {
            assertTrue(saved.theme.rainbow); assertEquals(7, saved.theme.rainbowSeconds);
            assertEquals(.42, saved.theme.rainbowSaturation); assertEquals(0xffeeaa77, saved.theme.themeColor);
            assertEquals(0xccddeeff, saved.theme.color(ThemeColor.TEXT));
            assertFalse(saved.theme.syncedColors.contains(ThemeColor.TEXT)); assertEquals(6, saved.theme.syncedColors.size());
        }
    }
}
