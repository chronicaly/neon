package me.xchro.neon.ui;

import java.util.*;
import java.util.function.*;
import net.minecraft.client.gui.layouts.*;

final class FlowRow extends AbstractLayout {
    private final List<LayoutElement> children = new ArrayList<>();
    private final IntSupplier available;
    private final int gap;
    FlowRow(IntSupplier available, int gap) { super(0, 0, 0, 0); this.available = available; this.gap = gap; }
    <T extends LayoutElement> T add(T child) { children.add(child); return child; }
    @Override public void arrangeElements() {
        int x = 0, y = 0, rowHeight = 0; width = height = 0;
        for (LayoutElement child : children) {
            if (child instanceof Layout layout) layout.arrangeElements();
            if (x > 0 && x + child.getWidth() > available.getAsInt()) { x = 0; y += rowHeight + gap; rowHeight = 0; }
            child.setX(getX() + x); child.setY(getY() + y); x += child.getWidth() + gap;
            rowHeight = Math.max(rowHeight, child.getHeight()); width = Math.max(width, x - gap); height = y + rowHeight;
        }
    }
    @Override public void visitChildren(Consumer<LayoutElement> visitor) { children.forEach(visitor); }
    @Override public void removeChildren() { children.clear(); }
}
