package me.xchro.neon.ui;

import java.util.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;

/** Carries transient interaction state through structural changes such as disclosure/undo. */
final class WidgetMemory {
    private final Map<String, AbstractWidget> previous = new HashMap<>();
    private final Map<String, Integer> y = new HashMap<>();
    private final UiScroll source;
    static String identity(AbstractWidget widget) {
        if (widget instanceof ExactSlider slider) return "number:" + slider.identity();
        if (widget instanceof UiButton button) return "button:" + button.identity();
        return widget.getClass().getName() + ":" + widget.getMessage().getString();
    }
    WidgetMemory(UiScroll scroll) {
        source = scroll;
        if (scroll == null) return;
        visit(scroll, new HashMap<>(), (key, widget) -> {
            previous.put(key, widget); y.put(key, widget.getY() - scroll.getY() + (int) scroll.scrollAmount());
        });
    }
    private interface Visitor { void accept(String key, AbstractWidget widget); }
    private void visit(ContainerEventHandler container, Map<String, Integer> occurrences, Visitor visitor) {
        for (var child : container.children()) {
            if (child instanceof AbstractWidget widget) {
                String base = identity(widget), key = base + ":" + occurrences.merge(base, 1, Integer::sum);
                visitor.accept(key, widget);
            }
            if (child instanceof ContainerEventHandler group) visit(group, occurrences, visitor);
        }
    }
    void restore(UiScroll scroll, boolean animate) {
        restore(scroll, animate, true);
    }
    void restore(UiScroll scroll, boolean animate, boolean drafts) {
        restore(scroll, animate, drafts, true);
    }
    void restore(UiScroll scroll, boolean animate, boolean drafts, boolean inheritScroll) {
        visit(scroll, new HashMap<>(), (key, widget) -> {
            AbstractWidget old = previous.get(key);
            if (old == null) return;
            if (widget instanceof ExactSlider slider && old instanceof ExactSlider was) slider.inherit(was);
            else if (widget instanceof UiButton button && old instanceof UiButton was) button.inherit(was);
            else if (drafts && widget instanceof UiEditBox edit && old instanceof UiEditBox was) edit.inherit(was);
            if (old.isFocused()) widget.setFocused(true);
        });
        focus(scroll);
        if (source != null && inheritScroll) scroll.inheritMotion(source);
        if (animate && !previous.isEmpty()) scroll.animateFrom(y);
    }
    private void focus(ContainerEventHandler group) {
        for (var child : group.children()) {
            if (child instanceof ContainerEventHandler container) focus(container);
            if (child.isFocused()) { group.setFocused(child); break; }
        }
    }
}
