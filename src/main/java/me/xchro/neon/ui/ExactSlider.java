package me.xchro.neon.ui;

import java.util.function.*;
import me.xchro.neon.core.NumberValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

public final class ExactSlider extends AbstractSliderButton {
    private final Screen parent;
    private final SettingsSession session;
    private final String name;
    private final double min, max, step;
    private final DoubleFunction<String> format;
    private final DoubleConsumer set;
    private double number;
    public ExactSlider(Screen parent, SettingsSession session, String name, int width, double current, double min, double max, double step, DoubleFunction<String> format, DoubleConsumer set) {
        super(0, 0, width, 20, Component.empty(), (current - min) / (max - min));
        this.parent = parent; this.session = session; this.name = name; this.min = min; this.max = max;
        this.step = step; this.format = format; this.set = set; number = current; updateMessage();
    }
    private void typeValue() {
        session.end();
        Minecraft.getInstance().gui.setScreen(new NumberScreen(parent, name, number, min, max, step >= 1,
            value -> session.change(() -> set.accept(value))));
    }
    @Override public void onClick(MouseButtonEvent event, boolean doubled) {
        int textWidth = Minecraft.getInstance().font.width(getMessage());
        if (Math.abs(event.x() - (getX() + getWidth() / 2.0)) <= textWidth / 2.0 + 4) { typeValue(); return; }
        session.begin(); super.onClick(event, doubled);
    }
    @Override public void onRelease(MouseButtonEvent event) { super.onRelease(event); session.end(); }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) { typeValue(); return true; }
        session.begin(); try { return super.keyPressed(event); } finally { session.end(); }
    }
    @Override protected void updateMessage() { if (format != null) setMessage(Component.literal(format.apply(number))); }
    @Override protected MutableComponent createNarrationMessage() {
        return Component.literal(name + ": ").append(super.createNarrationMessage());
    }
    @Override protected void applyValue() {
        number = Math.clamp(Math.round((min + value * (max - min)) / step) * step, min, max);
        number = Double.parseDouble(NumberValue.format(number));
        set.accept(number); updateMessage();
    }
}
