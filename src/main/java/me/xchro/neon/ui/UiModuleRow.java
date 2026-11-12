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
    private me.xchro.neon.core.UiMotion selectedMotion;
    private me.xchro.neon.core.UiMotion hoverMotion = new me.xchro.neon.core.UiMotion(0);
    void inherit(UiModuleRow old) { selectedMotion = old.selectedMotion; hoverMotion = old.hoverMotion; }
    UiModuleRow(String title, int width, Runnable action, UiButton binding, UiButton toggle, BooleanSupplier expanded) {
        super(0, 0, width, 26, Ui.label(title));
        this.binding = binding; this.toggle = toggle; this.expanded = expanded;
        selectedMotion = new me.xchro.neon.core.UiMotion(expanded.getAsBoolean() ? 1 : 0);
        if (toggle != null) toggle.identity(title + "/toggle");
        open = new UiButton(title, 0, 0, Ui.width(title) + 6, 26, UiButton.Kind.MODULE, action == null ? () -> {} : action).identity(title + "/name");
        tail = action == null ? null : new UiButton("", 0, 0, 20, 26, UiButton.Kind.DISCLOSURE, action).selected(expanded).hint((expanded.getAsBoolean() ? "Collapse " : "Expand ") + title).identity(title + "/disclosure");
        var children = new ArrayList<AbstractWidget>();
        if (toggle != null) children.add(toggle);
        children.add(open); if (binding != null) children.add(binding);
        if (tail != null) children.add(tail);
        controls = List.copyOf(children); place();
    }
    private void place() {
        if (open == null) return;
        int left = toggle == null ? 0 : 26;
        if (toggle != null) { toggle.setX(getX()); toggle.setY(getY()); }
        open.setX(getX() + left); open.setY(getY());
        if (binding != null) { binding.setX(open.getRight()); binding.setY(getY()); }
        int start = binding == null ? open.getRight() : binding.getRight();
        if (tail != null) {
            tail.setX(start); tail.setY(getY());
            tail.setWidth(Math.max(16, getRight() - tail.getX()));
        }
    }
    @Override public void setX(int x) { super.setX(x); place(); }
    @Override public void setY(int y) { super.setY(y); place(); }
    @Override public void setWidth(int width) { super.setWidth(width); place(); }
    @Override public List<AbstractWidget> children() { return controls; }
    @Override protected int contentHeight() { return getHeight(); }
    @Override protected double scrollRate() { return 0; }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        place();
        double selected = selectedMotion.to(expanded.getAsBoolean() ? 1 : 0, System.nanoTime(), 100_000_000L);
        int fill = UiEffects.mix(Ui.PANEL, Ui.BLUE, .045);
        if (selected > .001) Ui.rect(g, getX(), getY(), getWidth(), getHeight(), Ui.alpha(fill, (int)((fill >>> 24) * selected)));
        boolean keyboard = isFocused() && net.minecraft.client.Minecraft.getInstance().getLastInputType().isKeyboard();
        double hover = hoverMotion.to(isMouseOver(mx, my) || keyboard ? 1 : 0, System.nanoTime(), 100_000_000L);
        UiEffects.hoverLight(g, getX(), getY(), getWidth(), getHeight(), keyboard ? getX() + getWidth() / 2 : mx, hover);
        for (var child : controls) child.extractRenderState(g, mx, my, delta);
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        if (getFocused() instanceof AbstractWidget widget) widget.updateNarration(output);
    }
}
