package me.xchro.neon.ui;

import java.util.Locale;
import java.util.function.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.NeonConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    private int page, row;
    private String error = "";
    public SettingsScreen(Screen parent) { super(Component.literal("Neon")); this.parent = parent; }
    private SettingsScreen(Screen parent, int page) { this(parent); this.page = page; }
    @Override protected void init() {
        row = 40;
        var c = Neon.config;
        if (page == 0) {
            button("Camera", () -> minecraft.gui.setScreen(new SettingsScreen(this, 1)));
            button("Autoclicker", () -> minecraft.gui.setScreen(new SettingsScreen(this, 2)));
            button("HUD", () -> minecraft.gui.setScreen(new SettingsScreen(this, 3)));
            button("Key binds...", () -> minecraft.gui.setScreen(new KeyBindsScreen(this, minecraft.options)));
        } else if (page == 1) {
            slider("Freelook transition", c.freelookMillis, 0, 500, 10, " ms", v -> c.freelookMillis = (int) v);
            slider("Zoom", c.zoomFactor, 1, 16, .5, "x", v -> c.zoomFactor = v);
            slider("Zoom transition", c.zoomMillis, 0, 500, 10, " ms", v -> c.zoomMillis = (int) v);
            toggle("Zoom with scroll wheel", () -> c.zoomWheel, v -> c.zoomWheel = v);
            toggle("Zoom sensitivity", () -> c.zoomSensitivity, v -> c.zoomSensitivity = v);
        } else if (page == 2) {
            toggle("Enable autoclicker", () -> c.clickerEnabled, v -> c.clickerEnabled = v);
            toggle("Left click", () -> c.leftClick, v -> c.leftClick = v);
            toggle("Right click", () -> c.rightClick, v -> c.rightClick = v);
            slider("Left CPS", c.leftCps, 1, 20, 1, "", v -> c.leftCps = (int) v);
            slider("Right CPS", c.rightCps, 1, 20, 1, "", v -> c.rightCps = (int) v);
            button("Activation: " + (c.toggleClicker ? "Toggle" : "Hold"), () -> { c.toggleClicker = !c.toggleClicker; rebuildWidgets(); });
            toggle("Continuous mining", () -> c.continuousMining, v -> c.continuousMining = v);
        } else {
            toggle("Armor", () -> c.armor.visible, v -> c.armor.visible = v);
            slider("Armor scale", c.armor.scale, .5, 3, .25, "x", v -> c.armor.scale = v);
            toggle("Totem count", () -> c.totems.visible, v -> c.totems.visible = v);
            slider("Totem scale", c.totems.scale, .5, 3, .25, "x", v -> c.totems.scale = v);
            button("Move HUD...", () -> minecraft.gui.setScreen(new HudEditor(this)));
            button("Reset HUD", () -> { c.armor = new NeonConfig.Widget(1, .90); c.totems = new NeonConfig.Widget(1, 1); rebuildWidgets(); });
        }
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(width / 2 - 100, height - 28, 200, 20).build());
    }
    private void button(String text, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(text), b -> action.run()).bounds(width / 2 - 130, row, 260, 20).build()); row += 24;
    }
    private void toggle(String title, BooleanSupplier get, Consumer<Boolean> set) {
        button(title + ": " + (get.getAsBoolean() ? "ON" : "OFF"), () -> { set.accept(!get.getAsBoolean()); rebuildWidgets(); });
    }
    private void slider(String name, double current, double min, double max, double step, String suffix, DoubleConsumer set) {
        addRenderableWidget(new AbstractSliderButton(width / 2 - 130, row, 260, 20, Component.empty(), (current - min) / (max - min)) {
            { updateMessage(); }
            private double number() { return Math.round((min + value * (max - min)) / step) * step; }
            @Override protected void updateMessage() {
                double n = number();
                setMessage(Component.literal(name + ": " + (n == Math.rint(n) ? Integer.toString((int) n) : String.format(Locale.ROOT, "%.2f", n)) + suffix));
            }
            @Override protected void applyValue() { set.accept(number()); }
        }); row += 24;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        String subtitle = switch (page) { case 1 -> "Camera"; case 2 -> "Autoclicker"; case 3 -> "HUD"; default -> "Settings"; };
        g.centeredText(font, "Neon - " + subtitle, width / 2, 16, 0xffffffff);
        if (!error.isEmpty()) g.centeredText(font, error, width / 2, height - 42, 0xffff7777);
    }
    @Override public void onClose() {
        if (Neon.save()) minecraft.gui.setScreen(parent);
        else error = "Could not save settings. Check the game log.";
    }
    @Override public void removed() { Neon.save(); }
}
