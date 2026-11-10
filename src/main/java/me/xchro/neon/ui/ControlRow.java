package me.xchro.neon.ui;

import java.util.function.*;
import net.minecraft.client.gui.layouts.*;

/** Reflows existing controls; no widget replacement when the menu is resized. */
final class ControlRow extends AbstractLayout {
    private final UiLabel label;
    private final LayoutElement control;
    private final IntSupplier available;
    ControlRow(UiLabel label, LayoutElement control, IntSupplier available) {
        super(0, 0, available.getAsInt(), 27); this.label = label; this.control = control; this.available = available;
    }
    @Override public void arrangeElements() {
        width = available.getAsInt();
        if (control instanceof Layout layout) layout.arrangeElements();
        boolean stacked = Ui.width(label.getMessage().getString()) + control.getWidth() + 10 > width;
        label.setWidth(stacked ? width : Math.max(20, width - control.getWidth() - 10)); label.setHeight(stacked ? 22 : 27);
        height = stacked ? 27 + control.getHeight() : Math.max(27, control.getHeight());
        label.setX(getX()); label.setY(getY());
        control.setX(getX() + width - control.getWidth()); control.setY(getY() + (stacked ? 22 : (height - control.getHeight()) / 2));
    }
    @Override public void visitChildren(Consumer<LayoutElement> visitor) { visitor.accept(label); visitor.accept(control); }
    @Override public void removeChildren() {}
}
