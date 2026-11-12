package me.xchro.neon.ui;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

/** Unity's single-line row rendering, adapted to Neon's native-resolution typography. */
final class HudEntry extends UiButton {
    private final Runnable select;
    private final BooleanSupplier enabled, selected;
    private int depth;
    HudEntry(String name, int width, BooleanSupplier enabled, BooleanSupplier selected, Runnable toggle, Runnable select) {
        super(name, 0, 0, width, 22, Kind.CARD, toggle);
        this.enabled = enabled; this.selected = selected; this.select = select;
        hint(select == null ? "Left click changes the setting." : "Left click changes the setting; right click opens its options.");
    }
    HudEntry depth(int value) { depth = value; return this; }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (select != null && event.button() == 1 && isMouseOver(event.x(), event.y())) { select.run(); return true; }
        return super.mouseClicked(event, twice);
    }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
        boolean on = enabled.getAsBoolean();
        boolean open = selected.getAsBoolean();
        if (depth > 0 || open) Ui.rect(g, getX(), getY(), getWidth(), getHeight(), HudEditor.surface(Ui.PANEL, 100));
        double enabledAmount = selectionFraction(on);
        if (enabledAmount > .001) Ui.rect(g, getX(), getY(), getWidth(), getHeight(), HudEditor.surface(Ui.BLUE, (int)(22 * enabledAmount)));
        boolean keyboardFocus = isFocused() && net.minecraft.client.Minecraft.getInstance().getLastInputType().isKeyboard();
        double amount = hoverFraction();
        if (amount > .01) Ui.rect(g, getX(), getY(), getWidth(), getHeight(), HudEditor.surface(Ui.BLUE, (int) (amount * 16)));
        // Unity's drawRow uses four units of inset and six units per nesting level.
        int inset = 4 + depth * 6;
        Ui.text(g, Ui.fit(getMessage().getString(), getWidth() - inset - (select == null ? 6 : 23)), getX() + inset, getY() + 6, UiEffects.mix(Ui.TEXT, Ui.BLUE, enabledAmount));
        if (select != null) Ui.disclosure(g, getRight() - 12, getY() + 4, open, HudEditor.surface(Ui.MUTED, 190));
        if (keyboardFocus) Ui.stroke(g, getX(), getY(), getWidth(), getHeight(), Ui.BLUE);
    }
}
