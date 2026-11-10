package me.xchro.neon.ui;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

/** Unity's single-line row rendering, adapted to Neon's native-resolution typography. */
final class HudEntry extends UiButton {
    private final Runnable select;
    private final BooleanSupplier enabled, selected;
    private int depth;
    private final me.xchro.neon.core.TimedValue hover = new me.xchro.neon.core.TimedValue(0);
    HudEntry(String name, int width, BooleanSupplier enabled, BooleanSupplier selected, Runnable toggle, Runnable select) {
        super(name, 0, 0, width, 22, Kind.CARD, toggle);
        this.enabled = enabled; this.selected = selected; this.select = select;
        hint("Left click changes the setting; right click opens its options.");
    }
    HudEntry depth(int value) { depth = value; return this; }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (event.button() == 1 && isMouseOver(event.x(), event.y())) { select.run(); return true; }
        return super.mouseClicked(event, twice);
    }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
        boolean on = enabled.getAsBoolean();
        if (depth > 0 || on || isHoveredOrFocused() || selected.getAsBoolean()) Ui.rect(g, getX(), getY(), getWidth(), getHeight(), on ? Ui.SELECTED : Ui.FIELD);
        double amount = hover.to(isHoveredOrFocused() ? 1 : 0, System.nanoTime(), 100_000_000L);
        if (amount > .01) Ui.rect(g, getX(), getY(), getWidth(), getHeight(), Ui.alpha(Ui.BLUE, (int) (amount * 9)));
        // Unity's drawRow uses four units of inset and six units per nesting level.
        int inset = 4 + depth * 6;
        Ui.text(g, Ui.fit(getMessage().getString(), getWidth() - inset - 6), getX() + inset, getY() + 6, on ? Ui.BLUE : Ui.TEXT);
        if (isFocused() && net.minecraft.client.Minecraft.getInstance().getLastInputType().isKeyboard()) Ui.stroke(g, getX(), getY(), getWidth(), getHeight(), Ui.BLUE);
    }
}
