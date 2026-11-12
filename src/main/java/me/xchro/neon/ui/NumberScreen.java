package me.xchro.neon.ui;

import java.util.function.DoubleConsumer;
import me.xchro.neon.core.NumberValue;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public final class NumberScreen extends PixelScreen {
    private final Screen parent;
    private final double current, min, max;
    private final boolean integer;
    private final DoubleConsumer apply;
    private EditBox input;
    private Button done;
    private int x, y, w;
    public NumberScreen(Screen parent, String name, double current, double min, double max, boolean integer, DoubleConsumer apply) {
        super(Ui.label(name)); this.parent = parent; this.current = current; this.min = min; this.max = max; this.integer = integer; this.apply = apply;
    }
    @Override protected void init() {
        w = Math.min(280, canvasWidth() - 32); x = (canvasWidth() - w) / 2; y = (canvasHeight() - 160) / 2;
        input = addRenderableWidget(new UiEditBox(x + 24, y + 68, w - 48, title.getString()));
        input.setMaxLength(24); input.setValue(NumberValue.format(current));
        int bw = (w - 40) / 2;
        addRenderableWidget(new UiButton("Cancel", x + 16, y + 120, bw, 26, this::onClose));
        done = addRenderableWidget(new UiButton("Apply", x + 24 + bw, y + 120, bw, 26, UiButton.Kind.PRIMARY, this::commit));
        input.setResponder(value -> done.active = NumberValue.parse(value, min, max, integer).isPresent());
        setInitialFocus(input); input.moveCursorToEnd(false); input.setHighlightPos(0);
    }
    private void commit() { NumberValue.parse(input.getValue(), min, max, integer).ifPresent(value -> { apply.accept(value); minecraft.gui.setScreen(parent); if (value != current) UiSounds.cue(me.xchro.neon.core.UiSoundPolicy.Cue.CONFIRM); }); }
    @Override protected boolean pixelKeyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) { commit(); return true; }
        return super.pixelKeyPressed(event);
    }
    @Override protected void pixelBackground(GuiGraphicsExtractor g, int mx, int my, float delta) { Ui.background(g, canvasWidth(), canvasHeight()); }
    @Override protected void renderPixels(GuiGraphicsExtractor g, int mx, int my, float delta) {
 Ui.window(g, x, y, w, 160);
        Ui.title(g, title.getString(), x + 18, y + 18);
        Ui.text(g, (integer ? "Whole number · " : "Range · ") + NumberValue.format(min) + " – " + NumberValue.format(max), x + 18, y + 43, Ui.MUTED);
        renderWidgets(g, mx, my, delta);
        if (!done.active) Ui.text(g, "Enter a value within this range", x + 18, y + 98, Ui.RED);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); UiSounds.cue(me.xchro.neon.core.UiSoundPolicy.Cue.BACK); }
}
