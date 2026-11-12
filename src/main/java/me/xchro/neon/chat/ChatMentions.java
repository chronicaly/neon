package me.xchro.neon.chat;

import java.util.*;
import java.util.regex.Pattern;

/** Unity's whole-username, case-insensitive matcher, independent of the renderer. */
public final class ChatMentions {
    private ChatMentions() {}
    public record Range(int start, int end) {}
    public static List<Range> ranges(String text, String name) {
        if (name == null || name.isBlank() || text == null) return List.of();
        var matcher = Pattern.compile("(?i)(?<![A-Za-z0-9_])" + Pattern.quote(name) + "(?![A-Za-z0-9_])").matcher(text);
        var ranges = new ArrayList<Range>();
        while (matcher.find()) ranges.add(new Range(matcher.start(), matcher.end()));
        return ranges;
    }
    public static boolean ownPrefix(String text, String name) {
        if (name == null || name.isBlank()) return false;
        return Pattern.compile("(?i)^(?:\\[[^]\\r\\n]+]\\s*)*(?:<" + Pattern.quote(name)
            + ">|" + Pattern.quote(name) + "\\s*[:»>])").matcher(text).find();
    }
}
