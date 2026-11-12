package me.xchro.neon.chat;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.module.ClientModule;
import me.xchro.neon.ui.UiSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Adapted from Unity BetterChatModule at 5bbd639; preserves vanilla font and signed history. */
public final class BetterChat {
    private BetterChat() {}
    private record DisplayState(boolean enabled, boolean duplicates, boolean mentions, int color) {}
    private static DisplayState previous;
    public static boolean active() { return Neon.MODULE_BINDINGS.active(ClientModule.BETTER_CHAT); }
    private static String name() {
        var player = Minecraft.getInstance().player;
        return player == null ? "" : player.getGameProfile().name();
    }
    public static boolean mention(Component message, GuiMessageSource source) {
        return !ranges(message, source).isEmpty();
    }
    private static List<ChatMentions.Range> ranges(Component message, GuiMessageSource source) {
        if (source == GuiMessageSource.SYSTEM_CLIENT) return List.of();
        String name = name(), text = message.getString();
        if (ChatMentions.ownPrefix(text, name)) return List.of();
        if (message.getContents() instanceof TranslatableContents t && t.getArgs().length > 0
            && t.getArgs()[0] instanceof Component sender && sender.getString().equalsIgnoreCase(name)) return List.of();
        return ChatMentions.ranges(text, name);
    }
    public static Component display(Component original, GuiMessageSource source) {
        if (!active() || !Neon.config.betterChat.mentions) return original;
        var ranges = ranges(original, source);
        if (ranges.isEmpty()) return original;
        boolean[] highlight = new boolean[original.getString().length()];
        for (var range : ranges) Arrays.fill(highlight, range.start(), range.end(), true);
        MutableComponent result = Component.empty(); int[] offset = {0};
        // Preserve every existing style (including click/hover events) across split name spans.
        original.visit((style, part) -> {
            int start = 0;
            while (start < part.length()) {
                boolean marked = highlight[offset[0] + start]; int end = start + 1;
                while (end < part.length() && highlight[offset[0] + end] == marked) end++;
                result.append(Component.literal(part.substring(start, end)).setStyle(marked
                    ? style.withColor(Neon.config.betterChat.mentionColor & 0xffffff) : style));
                start = end;
            }
            offset[0] += part.length(); return Optional.empty();
        }, Style.EMPTY);
        return result;
    }
    /** Called only for an accepted, newly received chat message, never for reflow/history redraw. */
    public static void received(GuiMessage message) {
        var mc = Minecraft.getInstance();
        if (!active() || mc.player == null || mc.level == null || message.source() == GuiMessageSource.SYSTEM_CLIENT) return;
        var settings = Neon.config.betterChat;
        boolean named = mention(message.content(), message.source()) && settings.mentionPing;
        if (named || settings.messagePing) UiSounds.chatPing(named, settings.volume);
    }
    public static void tick(Minecraft mc) {
        var c = Neon.config.betterChat;
        var next = new DisplayState(active(), c.duplicates, c.mentions, c.mentionColor);
        if (!next.equals(previous)) {
            previous = next;
            if (mc.gui != null && mc.gui.hud != null) mc.gui.hud.getChat().rescaleChat();
        }
    }
}
