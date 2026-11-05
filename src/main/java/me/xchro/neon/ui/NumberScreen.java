package me.xchro.neon.ui;

import java.util.function.DoubleConsumer;
import me.xchro.neon.core.NumberValue;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Native text entry for precise values, validated before changing any setting. */
public final class NumberScreen extends Screen {
    private final Screen parent;
    private final double current, min, max;
    private final boolean integer;
    private final DoubleConsumer apply;
    private EditBox input;
    private Button done;
    public NumberScreen(Screen parent, String name, double current, double min, double max, boolean integer, DoubleConsumer apply) {
        super(Component.literal(name)); this.parent = parent; this.current = current; this.min = min; this.max = max; this.integer = integer; this.apply = apply;
    }
    @Override protected void init() {
        int y = height / 2 - 10;
        input = addRenderableWidget(new EditBox(font, width / 2 - 100, y, 200, 20, title));
        input.setMaxLength(24); input.setValue(NumberValue.format(current));
        done = addRenderableWidget(Button.builder(Component.literal("Apply"), b -> commit()).bounds(width / 2 - 102, y + 46, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(width / 2 + 2, y + 46, 100, 20).build());
        input.setResponder(value -> done.active = NumberValue.parse(value, min, max, integer).isPresent());
        setInitialFocus(input); input.moveCursorToEnd(false); input.setHighlightPos(0);
    }
    private void commit() {
        NumberValue.parse(input.getValue(), min, max, integer).ifPresent(value -> { apply.accept(value); onClose(); });
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) { commit(); return true; }
        return super.keyPressed(event);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        super.extractRenderState(g, x, y, delta);
        g.centeredText(font, title, width / 2, height / 2 - 48, 0xffffffff);
        g.centeredText(font, (integer ? "Whole number: " : "Range: ") + NumberValue.format(min) + " - " + NumberValue.format(max), width / 2, height / 2 - 30, 0xffaaaaaa);
        if (!done.active) g.centeredText(font, "Enter a valid value within the range", width / 2, height / 2 + 18, 0xffff7777);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
