package me.xchro.neon.ui;

import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.events.ContainerEventHandler;

/** A local view filter, never part of settings, profiles or Undo. */
final class UiSearch extends UiEditBox {
    UiSearch(String name, String value, Consumer<String> changed) {
        super(0,0,100,name);
        compact();setHeight(18);setMaxLength(64);setHint(Ui.label(name));
        setValue(value);setResponder(changed);
    }
    static boolean matches(String label, String query) {
        String haystack=label.toLowerCase(Locale.ROOT).replace('_',' ');
        for(String word:query.strip().toLowerCase(Locale.ROOT).split("\\s+"))
            if(!haystack.contains(word))return false;
        return true;
    }
    static boolean focusFirst(UiScroll results) {
        for(var child:results.children()) {
            if(!child.active||!child.visible||child instanceof UiLabel)continue;
            results.setFocused(child);
            if(child instanceof ContainerEventHandler group&&!group.children().isEmpty())group.setFocused(group.children().getFirst());
            results.reveal(child.getY(),child.getBottom());return true;
        }
        return false;
    }
}
