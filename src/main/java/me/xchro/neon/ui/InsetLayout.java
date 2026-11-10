package me.xchro.neon.ui;

import java.util.function.Consumer;
import net.minecraft.client.gui.layouts.*;

/** Indents expanded settings beneath their module label, without another framed panel. */
final class InsetLayout extends AbstractLayout {
    private final Layout content;
    private final int inset;
    InsetLayout(Layout content, int inset) { super(0, 0, 0, 0); this.content = content; this.inset = inset; }
    @Override public void arrangeElements() {
        content.arrangeElements(); width = inset + content.getWidth(); height = content.getHeight() + 10;
        content.setX(getX() + inset); content.setY(getY() + 7);
    }
    @Override public void visitChildren(Consumer<LayoutElement> visitor) { visitor.accept(content); }
    @Override public void removeChildren() {}
}
