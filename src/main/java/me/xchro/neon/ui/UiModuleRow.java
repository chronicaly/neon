package me.xchro.neon.ui;

import java.util.*;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarrationElementOutput;

/** A flat module row: toggle, name, inline bind, and disclosure are separate targets. */
final class UiModuleRow extends AbstractContainerWidget {
    private final UiButton open, binding, toggle, tail;
    private final List<AbstractWidget> controls;
    private final BooleanSupplier expanded;
    UiModuleRow(String title, int width, Runnable action, UiButton binding, UiButton toggle, BooleanSupplier expanded) {
        super(0, 0, width, 26, Ui.label(title));
        this.binding = binding; this.toggle = toggle; this.expanded = expanded;
        open = new UiButton(title, 0, 0, Ui.width(title) + 6, 26, UiButton.Kind.MODULE, action);
        tail = new UiButton("", 0, 0, 20, 26, UiButton.Kind.DISCLOSURE, action).selected(expanded).hint("Expand " + title);
        var children = new ArrayList<AbstractWidget>();
        if (toggle != null) children.add(toggle);
        children.add(open); if (binding != null) children.add(binding); children.add(tail);
        controls = List.copyOf(children); place();
    }
    private void place() {
        if (open == null) return;
        int left = toggle == null ? 0 : 26;
        if (toggle != null) { toggle.setX(getX()); toggle.setY(getY()); }
        open.setX(getX() + left); open.setY(getY());
        if (binding != null) { binding.setX(open.getRight()); binding.setY(getY()); }
        int start = binding == null ? open.getRight() : binding.getRight();
        tail.setX(start); tail.setY(getY()); tail.setWidth(Math.max(16, getRight() - start));
    }
    @Override public void setX(int x) { super.setX(x); place(); }
    @Override public void setY(int y) { super.setY(y); place(); }
    @Override public void setWidth(int width) { super.setWidth(width); place(); }
    @Override public List<AbstractWidget> children() { return controls; }
    @Override protected int contentHeight() { return getHeight(); }
    @Override protected double scrollRate() { return 0; }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        place();
        if (expanded.getAsBoolean()) Ui.rect(g, getX(), getY(), getWidth(), getHeight(), UiEffects.mix(Ui.PANEL, Ui.BLUE, .10));
        else Ui.rect(g, getX(), getBottom() - 1, getWidth(), 1, Ui.alpha(Ui.MUTED, 42));
        for (var child : controls) child.extractRenderState(g, mx, my, delta);
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        if (getFocused() instanceof AbstractWidget widget) widget.updateNarration(output);
    }
}
