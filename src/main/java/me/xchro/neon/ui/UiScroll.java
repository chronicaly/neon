package me.xchro.neon.ui;

import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Native focus and input handling with a plain, clipped content surface. */
final class UiScroll extends AbstractContainerWidget {
    private final List<AbstractWidget> children = new ArrayList<>();
    private final Map<AbstractWidget, int[]> offsets = new IdentityHashMap<>();
    private int contentHeight;
    private final Layout layout;
    private double targetScroll;
    private me.xchro.neon.core.TimedValue scrollMotion = new me.xchro.neon.core.TimedValue(0);
    private boolean moduleCards;
    private final Set<AbstractWidget> appearing = Collections.newSetFromMap(new IdentityHashMap<>());
    private long expanding;
    private record Anchor(String key, int offset) {}
    private Anchor pinned;
    private long pinnedUntil;
    void pinHeader(String name) {
        for (var child : children) if (child.getMessage().getString().equals(name) || child instanceof HudEntry entry && entry.identity().equals(name)) {
            if (child.getBottom() > getY() && child.getY() < getBottom()) {
                pinned = new Anchor(WidgetMemory.identity(child), child.getY() - getY());
                pinnedUntil = System.nanoTime() + 150_000_000L;
            }
            return;
        }
    }
    private Anchor anchor() {
        if (pinned != null && System.nanoTime() < pinnedUntil) return pinned;
        pinned = null;
        return firstVisible(this);
    }
    private Anchor firstVisible(net.minecraft.client.gui.components.events.ContainerEventHandler group) {
        for (var child : group.children()) if (child instanceof AbstractWidget widget && widget.getHeight() > 0 && widget.getBottom() > getY() && widget.getY() < getBottom()) {
            if (child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler nested) {
                Anchor result = firstVisible(nested); if (result != null) return result;
            }
            return new Anchor(WidgetMemory.identity(widget), widget.getY() - getY());
        }
        return null;
    }
    private AbstractWidget find(net.minecraft.client.gui.components.events.ContainerEventHandler group, String key) {
        for (var child : group.children()) {
            if (child instanceof AbstractWidget widget && WidgetMemory.identity(widget).equals(key)) return widget;
            if (child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler nested) {
                var result = find(nested, key); if (result != null) return result;
            }
        }
        return null;
    }
    private void restoreAnchor(Anchor anchor) {
        double before = scrollAmount();
        var widget = anchor == null ? null : find(this, anchor.key());
        double next = widget == null ? before : before + widget.getY() - getY() - anchor.offset();
        super.setScrollAmount(Math.clamp(next, 0, maxScrollAmount()));
        double delta = scrollAmount() - before;
        scrollMotion.shift(delta);
        targetScroll = Math.clamp(targetScroll + delta, 0, maxScrollAmount());
        scrollMotion.to(targetScroll, System.nanoTime(), 120_000_000L); place();
    }
    void restorePosition(double value) {
        super.setScrollAmount(value); stopMotion(); place();
    }
    void inheritMotion(UiScroll old) {
        Anchor anchor = old.anchor(); pinned = old.pinned; pinnedUntil = old.pinnedUntil;
        super.setScrollAmount(old.scrollAmount()); place();
        targetScroll = Math.clamp(old.targetScroll, 0, maxScrollAmount()); scrollMotion = old.scrollMotion;
        restoreAnchor(anchor);
        if (System.nanoTime() - old.expanding < 150_000_000L) {
            expanding = old.expanding;
            Set<String> visible = new HashSet<>(); old.appearing.forEach(w -> visible.add(WidgetMemory.identity(w)));
            for (var w : children) if (visible.contains(WidgetMemory.identity(w))) appearing.add(w);
        }
    }
    void animateFrom(Map<String, Integer> old) {
        expanding = System.nanoTime();
        Map<String, Integer> occurrences = new HashMap<>();
        for (AbstractWidget widget : children) {
            String base = WidgetMemory.identity(widget), key = base + ":" + occurrences.merge(base, 1, Integer::sum);
            if (!old.containsKey(key)) appearing.add(widget);
        }
        place();
    }
    UiScroll(int x, int y, int width, int height, Layout layout) {
        super(x, y, width, height, Component.empty());
        this.layout = layout;
        layout.setX(0); layout.setY(0); layout.arrangeElements(); contentHeight = layout.getHeight();
        layout.visitWidgets(widget -> { children.add(widget); offsets.put(widget, new int[]{widget.getX(), widget.getY()}); });
        place();
    }
    UiScroll moduleCards() { moduleCards = true; return this; }
    void stopMotion() { targetScroll = scrollAmount(); scrollMotion.snap(targetScroll); }
    void scrollTo(double target) { targetScroll = Math.clamp(target,0,maxScrollAmount()); scrollMotion.to(targetScroll,System.nanoTime(),120_000_000L); }
    @Override protected boolean isOverScrollbar(double x,double y) { return false; }
    void relayout() {
        Anchor anchor = anchor();
        appearing.clear();
        layout.setX(0); layout.setY(0); layout.arrangeElements(); contentHeight = layout.getHeight();
        layout.visitWidgets(widget -> { offsets.put(widget, new int[]{widget.getX(), widget.getY()}); });
        place(); restoreAnchor(anchor);
    }
    void reveal(int top, int bottom) {
        if (bottom > getBottom()) setScrollAmount(scrollAmount() + bottom - getBottom());
        if (top < getY()) setScrollAmount(scrollAmount() + top - getY());
    }
    private void place() {
        if (offsets == null) return;
        offsets.forEach((widget, offset) -> { widget.setX(getX() + offset[0]); widget.setY(getY() + offset[1] - (int) scrollAmount()); });
    }
    @Override public List<AbstractWidget> children() { return children; }
    @Override protected int contentHeight() { return contentHeight; }
    @Override protected double scrollRate() { return 24; }
    @Override public void setScrollAmount(double value) {
        pinned = null;
        // Explicit scrolling/focus restoration must reach controls even during an opening fold.
        if (children != null && value > maxScrollAmount()) {
            boolean changed = false;
            for (var child : children) if (child instanceof UiModuleFold fold) changed |= fold.reveal();
            if (changed) relayout();
        }
        super.setScrollAmount(value); targetScroll = scrollAmount();
        if (scrollMotion != null) scrollMotion.snap(targetScroll); place();
    }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (!isMouseOver(x, y) || maxScrollAmount() <= 0) return false;
        pinned = null;
        targetScroll = Math.clamp(targetScroll - dy * scrollRate(), 0, maxScrollAmount());
        scrollMotion.to(targetScroll, System.nanoTime(), 120_000_000L); return true;
    }
    @Override public void setX(int x) { super.setX(x); place(); }
    @Override public void setY(int y) { super.setY(y); place(); }
    @Override public void setFocused(GuiEventListener widget) {
        // Vanilla's widget container clears a descendant when reselecting the same fold.
        if (getFocused() != widget) super.setFocused(widget);
        // A mouse click must never move the gradient/slider out from under the pointer.
        // Keyboard navigation reveals the actual focused descendant of an inline fold.
        if (!net.minecraft.client.Minecraft.getInstance().getLastInputType().isKeyboard()) return;
        GuiEventListener target = widget;
        while (target instanceof net.minecraft.client.gui.components.events.ContainerEventHandler group && group.getFocused() != null) target = group.getFocused();
        if (target instanceof AbstractWidget w) {
            if (w.getY() < getY()) setScrollAmount(scrollAmount() + w.getY() - getY());
            else if (w.getBottom() > getBottom()) setScrollAmount(scrollAmount() + w.getBottom() - getBottom());
        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean twice) { return isMouseOver(e.x(), e.y()) && super.mouseClicked(e, twice); }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        double visibleScroll = scrollMotion.to(targetScroll, System.nanoTime(), 120_000_000L);
        if (visibleScroll != scrollAmount()) { super.setScrollAmount(visibleScroll); place(); }
        Ui.clip(g, getX() - (moduleCards ? 8 : 0), getY(), getRight(), getBottom());
        if (moduleCards) {
            List<UiLabel> headings = children.stream().filter(w -> w instanceof UiLabel label && label.isBoundary()).map(w -> (UiLabel) w).toList();
            for (int i = 0; i < headings.size(); i++) {
                if (!headings.get(i).isSection()) continue;
                int top = headings.get(i).getY();
                int bottom = i + 1 < headings.size() ? headings.get(i + 1).getY() - 5 : getY() + contentHeight - (int) scrollAmount();
                Ui.card(g, getX() - 8, top, getWidth() + 1, bottom - top);
            }
        }
        double progress = Math.clamp((System.nanoTime() - expanding) / 150_000_000.0, 0, 1);
        if (progress == 1) appearing.clear();
        float opacity = Ui.opacity;
        for (AbstractWidget widget : children) if (widget.getBottom() > getY() && widget.getY() < getBottom()) {
            Ui.opacity = appearing.contains(widget) ? opacity * (float) (1 - Math.pow(1 - progress, 3)) : opacity;
            widget.extractRenderState(g, mx, my, delta);
        }
        Ui.opacity = opacity;
        g.disableScissor();
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        if (getFocused() instanceof AbstractWidget widget) widget.updateNarration(output);
    }
}
