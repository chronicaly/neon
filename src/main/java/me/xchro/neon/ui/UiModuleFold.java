package me.xchro.neon.ui;

import java.util.*;
import me.xchro.neon.core.UiMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;

/** A clipped, retargetable fold whose visible height also drives the surrounding layout. */
final class UiModuleFold extends AbstractContainerWidget {
    private static final long DURATION = 150_000_000L;
    private final Layout content;
    private final List<AbstractWidget> controls = new ArrayList<>();
    private final List<UiLabel> labels;
    private final Map<AbstractWidget, int[]> offsets = new IdentityHashMap<>();
    private final UiMotion opening;
    private final boolean open;
    private final Runnable relayout;
    private final boolean compact;
    private int fullHeight;

    UiModuleFold(String name, int width, Layout content, UiModuleFold previous, boolean open, boolean animate, Runnable relayout) {
        this(name, width, content, previous, open, animate, relayout, false);
    }

    UiModuleFold(String name, int width, Layout content, UiModuleFold previous, boolean open, boolean animate, Runnable relayout, boolean compact) {
        super(0, 0, width, 0, Ui.label(name + " settings"));
        this.content = content != null ? content : previous.content;
        this.compact = compact;
        this.open = open; this.relayout = relayout;
        opening = previous == null ? new UiMotion(animate ? 0 : open ? 1 : 0) : previous.opening;
        if (!animate && previous == null) opening.snap(open ? 1 : 0);
        opening.to(open ? 1 : 0, System.nanoTime(), DURATION);
        this.content.visitWidgets(controls::add);
        labels = controls.stream().filter(w -> w instanceof UiLabel).map(w -> (UiLabel) w).toList();
        arrange();
    }

    void arrange() {
        content.setX(0); content.setY(0); content.arrangeElements();
        fullHeight = content.getHeight() + (compact ? 0 : 10);
        for (var widget : controls) offsets.put(widget, new int[]{widget.getX(), widget.getY() + (compact ? 0 : 7)});
        tick(); place();
    }

    boolean tick() {
        int next = (int) Math.round(fullHeight * opening.value(System.nanoTime(), DURATION));
        boolean changed = next != getHeight(); setHeight(next); return changed;
    }

    boolean reveal() { if (!open) return false; opening.snap(1); return tick(); }

    private void place() {
        if (offsets == null) return;
        offsets.forEach((widget, offset) -> { widget.setX(getX() + offset[0]); widget.setY(getY() + offset[1]); });
    }

    @Override public void setX(int x) { super.setX(x); place(); }
    @Override public void setY(int y) { super.setY(y); place(); }
    // Closing controls remain visible only until the clip closes; they stop receiving input immediately.
    @Override public List<AbstractWidget> children() { return open ? controls : List.of(); }
    @Override protected int contentHeight() { return getHeight(); }
    @Override protected double scrollRate() { return 0; }
    @Override protected boolean isOverScrollbar(double x, double y) { return false; }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean twice) {
        return open && isMouseOver(e.x(), e.y()) && super.mouseClicked(e, twice);
    }
    @Override public void setFocused(GuiEventListener widget) {
        if (getFocused() != widget) super.setFocused(widget);
        if (widget != null && open && Minecraft.getInstance().getLastInputType().isKeyboard()) {
            if (reveal()) relayout.run();
        }
    }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (getHeight() <= 0) return;
        Ui.clip(g, getX(), getY(), getRight(), getBottom());
        float opacity = Ui.opacity;
        Ui.opacity *= fullHeight == 0 ? 1 : (float) getHeight() / fullHeight;
        try {
            for (int i = 0; i + 1 < labels.size(); i++) Ui.rect(g, getX(), labels.get(i).getBottom() - 1, getWidth(), 1, 0x06ffffff);
            boolean hovered = open && isMouseOver(mx, my);
            for (var widget : controls) if (widget.getBottom() > getY() && widget.getY() < getBottom())
                widget.extractRenderState(g, hovered ? mx : -1, hovered ? my : -1, delta);
        } finally { Ui.opacity = opacity; g.disableScissor(); }
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        if (open && getFocused() instanceof AbstractWidget widget) widget.updateNarration(output);
    }
}
