package me.xchro.neon.chat;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;

/** Display-only groups. Unchanged native line objects survive accepted messages. */
public final class GroupedChat {
    private record Group(int count, List<GuiMessage.Line> lines) {}
    private Map<GuiMessage, Group> groups = new IdentityHashMap<>();

    public void clear() { groups.clear(); }

    public List<GuiMessage.Line> update(List<GuiMessage> originals, Predicate<GuiMessage> visible, Font font, int width) {
        var retained = new IdentityHashMap<GuiMessage, Group>();
        var result = new ArrayList<GuiMessage.Line>(100);
        for (int i = 0; i < originals.size() && result.size() < 100;) {
            GuiMessage latest = originals.get(i);
            int end = i + 1;
            while (end < originals.size()) {
                var older = originals.get(end);
                if (older.source() != latest.source() || !older.content().equals(latest.content())) break;
                end++;
            }
            int count = end - i;
            if (visible.test(latest)) {
                var group = groups.get(latest);
                if (group == null || group.count != count) {
                    Component content = count == 1 ? latest.content() : latest.content().copy()
                        .append(Component.literal(" [x" + count + "]").withColor(0x98abb4));
                    var display = new GuiMessage(latest.addedTime(), BetterChat.display(content, latest.source()),
                        latest.signature(), latest.source(), latest.tag());
                    var split = display.splitLines(font, width);
                    var lines = new ArrayList<GuiMessage.Line>(split.size());
                    for (int line = split.size() - 1; line >= 0; line--)
                        lines.add(new GuiMessage.Line(display, split.get(line), line == split.size() - 1));
                    group = new Group(count, List.copyOf(lines));
                }
                retained.put(latest, group);
                for (var line : group.lines) {
                    if (result.size() == 100) break;
                    result.add(line);
                }
            }
            i = end;
        }
        groups = retained;
        return result;
    }
}
