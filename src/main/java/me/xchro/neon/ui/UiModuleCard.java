package me.xchro.neon.ui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarrationElementOutput;

/** One rectangular card with two independently focusable targets on the same row. */
final class UiModuleCard extends AbstractContainerWidget {
    private final UiButton open, binding;
    private final List<AbstractWidget> controls;
    private final java.util.function.Supplier<String> status;
    UiModuleCard(String title, int width, Runnable action, UiButton binding, java.util.function.Supplier<String> status) {
        super(0, 0, width, 26, Ui.label(title));
        this.binding = binding; this.status = status;
        int reserved = binding != null ? binding.getWidth() + 6 : status != null ? 30 : 0;
        open = new UiButton(title, 0, 0, binding == null ? width : width - reserved, 26, UiButton.Kind.CARD, action);
        controls = binding == null ? List.of(open) : List.of(open, binding);
        place();
    }
    private void place() {
        if (open == null) return;
        open.setX(getX()); open.setY(getY());
        if (binding != null) { binding.setX(getRight() - binding.getWidth() - 6); binding.setY(getY()); }
    }
    @Override public void setX(int x) { super.setX(x); place(); }
    @Override public void setY(int y) { super.setY(y); place(); }
    @Override public void setWidth(int w) { super.setWidth(w); if (open != null) open.setWidth(binding == null ? w : w - binding.getWidth() - 6); place(); }
    @Override public List<AbstractWidget> children() { return controls; }
    @Override protected int contentHeight() { return getHeight(); }
    @Override protected double scrollRate() { return 0; }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Ui.card(g, getX(), getY(), getWidth(), getHeight());
        for (var child : children()) child.extractRenderState(g, mx, my, delta);
        if (status != null) {
            String value = status.get();
            Ui.small(g, value, getRight() - 8 - UiFont.width(value, 10), getY() + 8, value.equals("On") ? Ui.BLUE : Ui.MUTED);
        }
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        if (getFocused() instanceof AbstractWidget widget) widget.updateNarration(output);
    }
}
