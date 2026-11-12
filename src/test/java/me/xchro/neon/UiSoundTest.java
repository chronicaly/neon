package me.xchro.neon;

import java.io.*;
import javax.sound.sampled.*;
import me.xchro.neon.config.ConfigStore;
import me.xchro.neon.core.UiSoundMailbox;
import me.xchro.neon.core.UiSoundPolicy;
import me.xchro.neon.core.UiSoundPolicy.Cue;
import me.xchro.neon.core.UiSoundPolicy.Request;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiSoundTest {
    @Test void hoverTracksLogicalTargetsAndNeverReplaysSuppressedEntries() {
        var policy = new UiSoundPolicy();
        assertEquals(.14, policy.hover("module", 0, .4).volume(), 1e-9);
        assertNull(policy.hover("module", 1_000_000_000L, .4));
        assertNotNull(policy.hover("zoom", 1_000_000_000L, .4));
        assertNull(policy.hover("blur", 1_020_000_000L, .4));
        assertNull(policy.hover("blur", 2_000_000_000L, .4));
        assertNull(policy.hover(null, 2_000_000_000L, .4));
        assertNotNull(policy.hover("blur", 2_000_000_000L, .4));
    }
    @Test void oneSemanticCueWinsAcrossNestedInputAndSuppressesHover() {
        var policy = new UiSoundPolicy(); policy.begin();
        assertNull(policy.request(Cue.BACK, 0, .4)); policy.begin();
        assertNull(policy.request(Cue.CONFIRM, 0, .4));
        assertNull(policy.request(Cue.ENABLE, 0, .4));
        assertNull(policy.end(0, .4));
        assertEquals(Cue.ENABLE, policy.end(0, .4).cue());
        assertNull(policy.hover("one", 99_000_000L, .4));
        assertNotNull(policy.hover("two", 100_000_000L, .4));
        assertEquals(Cue.DISABLE, policy.request(Cue.DISABLE, 101_000_000L, .4).cue());
    }
    @Test void zeroMutesEveryCueAndSavedVolumeNeedsNoMigration() {
        var policy = new UiSoundPolicy();
        for (var cue : Cue.values()) assertNull(policy.request(cue, 1_000_000_000L, 0));
        policy.begin(); policy.request(Cue.ENABLE, 2_000_000_000L, .4);
        assertNull(policy.end(2_000_000_000L, 0));
        assertEquals(.4, ConfigStore.decode("{}").theme.soundVolume);
        var config = ConfigStore.decode("{\"theme\":{\"soundVolume\":0.23}}");
        assertEquals(.23, ConfigStore.copy(config).theme.soundVolume);
    }
    @Test void burstInputReplacesPendingAudioAndActionsCancelHover() {
        var mailbox = new UiSoundMailbox();
        for (int i = 0; i < 10000; i++) mailbox.offer(new Request(Cue.CONFIRM, .4, i));
        mailbox.offer(new Request(Cue.HOVER, .14, 10000));
        assertEquals(9999, mailbox.poll(10000).time());
        assertEquals(Cue.HOVER, mailbox.poll(10000).cue());
        assertTrue(mailbox.empty());
        mailbox.offer(new Request(Cue.HOVER, .14, 11000));
        mailbox.offer(new Request(Cue.BACK, .4, 11000));
        assertEquals(Cue.BACK, mailbox.poll(11000).cue()); assertNull(mailbox.poll(11000));
        mailbox.offer(new Request(Cue.ENABLE, .4, 0));
        assertNull(mailbox.poll(150_000_001));
    }
    @Test void assetsAreShortQuietPcmWithSmoothEndpointsWithoutOpeningAnAudioDevice() throws Exception {
        for (var cue : Cue.values()) {
            String resource = "/assets/neon/sounds/ui/" + cue.name().toLowerCase(java.util.Locale.ROOT) + ".wav";
            try (var input = AudioSystem.getAudioInputStream(new BufferedInputStream(getClass().getResourceAsStream(resource)))) {
                var format = input.getFormat(); assertEquals(48000, format.getSampleRate());
                assertEquals(1, format.getChannels()); assertEquals(16, format.getSampleSizeInBits());
                double duration = input.getFrameLength() / format.getSampleRate();
                assertTrue(duration >= .070 && duration <= .240, resource);
                byte[] bytes = input.readAllBytes(); int previous = 0, peak = 0, jump = 0; long sum = 0; double energy = 0;
                for (int i = 0; i < bytes.length; i += 2) {
                    int sample = (short) ((bytes[i] & 255) | bytes[i + 1] << 8);
                    if (i == 0 || i == bytes.length - 2) assertEquals(0, sample, resource);
                    peak = Math.max(peak, Math.abs(sample)); jump = Math.max(jump, Math.abs(sample - previous));
                    sum += sample; energy += (double) sample * sample; previous = sample;
                }
                assertTrue(peak > 3000 && peak < 6500, resource + " quiet peak/headroom");
                assertTrue(Math.sqrt(energy / (bytes.length / 2.0)) < 3200, resource + " sustained loudness");
                assertTrue(jump < 1500, resource + " sample discontinuity");
                assertTrue(Math.abs(sum / (bytes.length / 2.0)) < 65, resource + " DC offset");
            }
        }
    }
}
