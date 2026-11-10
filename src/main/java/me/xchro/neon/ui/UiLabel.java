package me.xchro.neon.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.StringWidget;

final class UiLabel extends StringWidget {
    private final int color;
    private final boolean themed;
    private boolean section, boundary;
    UiLabel(String text, int width, int height, int color) { super(width, height, Ui.label(text), Minecraft.getInstance().font); this.color = color; themed = color == Ui.TEXT; }
    UiLabel section() { section = boundary = true; return this; }
    UiLabel endCards() { boundary = true; return this; }
    boolean isSection() { return section; }
    boolean isBoundary() { return boundary; }
    @Override public void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int x = getX(), y = getY() + (getHeight() - 10) / 2;
        if (section) { Ui.rect(g, x, y + 3, 3, 3, Ui.BLUE); x += 10; }
        Ui.text(g, Ui.fit(getMessage().getString(), getWidth() - (section ? 10 : 0)), x, y, themed ? Ui.TEXT : color);
    }
}
