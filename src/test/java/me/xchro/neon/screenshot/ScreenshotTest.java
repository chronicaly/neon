package me.xchro.neon.screenshot;

import com.sun.jna.Memory;
import me.xchro.neon.config.ConfigStore;
import me.xchro.neon.core.BindMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScreenshotTest {
    @Test void windowsDibKeepsDimensionsChannelsAndBottomUpRowOrder() {
        int[] pixels = {0xffff0000, 0xff00ff00, 0xff0000ff, 0xff123456, 0xffabcdef, 0xff010203};
        try (var memory = new Memory(40 + pixels.length * 4L)) {
            WindowsImageClipboard.writeDib(memory, 3, 2, pixels);
            assertEquals(40, memory.getInt(0)); assertEquals(3, memory.getInt(4)); assertEquals(2, memory.getInt(8));
            assertEquals(1, memory.getShort(12)); assertEquals(32, memory.getShort(14));
            assertEquals(0, memory.getInt(16)); assertEquals(24, memory.getInt(20));
            assertArrayEquals(new int[]{pixels[3], pixels[4], pixels[5], pixels[0], pixels[1], pixels[2]}, memory.getIntArray(40, 6));
            assertArrayEquals(new byte[]{0x56, 0x34, 0x12, (byte)0xff}, memory.getByteArray(40, 4));
        }
    }
    @Test void oldAndMalformedConfigsKeepUnityDefaultsWithoutEnablingClipboardWrites() {
        for (String json : new String[]{"{}", "{\"betterScreenshot\":null}", "{\"betterScreenshot\":{\"mode\":null}}"}) {
            var config = ConfigStore.decode(json).betterScreenshot;
            assertFalse(config.enabled); assertTrue(config.notify); assertEquals(BindMode.TOGGLE, config.mode);
        }
    }
    @Test void settingsAndProfileCopiesKeepScreenshotChoicesAndExistingTheme() {
        var original = ConfigStore.decode("{\"betterScreenshot\":{\"enabled\":true,\"notify\":false,\"mode\":\"REVERSE_HOLD\"},\"theme\":{\"opacity\":0.63,\"soundVolume\":0.17}}");
        var copy = ConfigStore.copy(original);
        assertTrue(copy.betterScreenshot.enabled); assertFalse(copy.betterScreenshot.notify);
        assertEquals(BindMode.REVERSE_HOLD, copy.betterScreenshot.mode);
        assertEquals(.63, copy.theme.opacity); assertEquals(.17, copy.theme.soundVolume);
        copy.betterScreenshot.enabled = false; assertTrue(original.betterScreenshot.enabled);
    }
}
