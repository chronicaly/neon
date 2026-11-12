package me.xchro.neon;

import me.xchro.neon.chat.ChatMentions;
import me.xchro.neon.config.ConfigStore;
import me.xchro.neon.core.BindMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BetterChatTest {
    @Test void mentionsMatchWholeNamesIgnoringCaseAndAllowAtPrefix() {
        assertEquals(2, ChatMentions.ranges("hello @mArTiN, MARTIN!", "Martin").size());
        for (String text : new String[]{"Martina", "xMartin", "Martin_2", "2Martin", "Martin2"})
            assertTrue(ChatMentions.ranges(text, "Martin").isEmpty(), text);
        assertTrue(ChatMentions.ranges("hello", "").isEmpty());
        assertTrue(ChatMentions.ranges("hello", null).isEmpty());
    }
    @Test void matchesPreserveOffsetsAndQuoteLiteralNames() {
        var matches = ChatMentions.ranges("hi A.B and A.B", "A.B");
        assertEquals(new ChatMentions.Range(3, 6), matches.getFirst());
        assertEquals(new ChatMentions.Range(11, 14), matches.getLast());
        assertTrue(ChatMentions.ranges("AxB", "A.B").isEmpty());
    }
    @Test void normalOwnSenderPrefixesDoNotBecomeMentions() {
        for (String text : new String[]{"<Martin> hello", "Martin: hi", "[VIP] [World] martin » hi"})
            assertTrue(ChatMentions.ownPrefix(text, "Martin"), text);
        assertFalse(ChatMentions.ownPrefix("<Bob> Martin: hello", "Martin"));
        assertFalse(ChatMentions.ownPrefix("Martina: hi", "Martin"));
    }
    @Test void oldConfigsGainChatDefaultsWithoutChangingSavedAudio() {
        var old = ConfigStore.decode("{\"theme\":{\"soundVolume\":0.23}}");
        assertTrue(old.betterChat.enabled); assertTrue(old.betterChat.messagePing); assertTrue(old.betterChat.mentionPing);
        assertEquals(.4, old.betterChat.volume); assertEquals(.23, old.theme.soundVolume);
        assertEquals(120, old.betterChat.animationMillis); assertEquals(0xffffcd5a, old.betterChat.mentionColor);
    }
    @Test void chatSettingsSurviveProfilesAndInvalidValuesAreClamped() {
        var config = ConfigStore.decode("{\"betterChat\":{\"volume\":0.17,\"animationMillis\":340,\"mentionColor\":-16711936,\"messagePing\":false,\"mode\":\"HOLD\"}}");
        var saved = ConfigStore.copy(config).betterChat;
        assertEquals(.17, saved.volume); assertEquals(340, saved.animationMillis);
        assertEquals(0xff00ff00, saved.mentionColor); assertFalse(saved.messagePing); assertEquals(BindMode.HOLD, saved.mode);
        var invalid = ConfigStore.decode("{\"betterChat\":{\"volume\":5,\"animationMillis\":-3,\"mode\":null}}").betterChat;
        assertEquals(1, invalid.volume); assertEquals(50, invalid.animationMillis); assertEquals(BindMode.TOGGLE, invalid.mode);
        assertNotNull(ConfigStore.decode("{\"betterChat\":null}").betterChat);
    }
}
