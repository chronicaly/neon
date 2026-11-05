package me.xchro.neon.ui;

import me.xchro.neon.Neon;
import me.xchro.neon.config.NeonConfig.ArmorAnchor;
import me.xchro.neon.hud.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class HudEditor extends Screen {
    private final Screen parent;
    private boolean dragging;
    private double offsetX, offsetY;
    public HudEditor(Screen parent) { super(Component.literal("Armor Position")); this.parent = parent; }
    @Override protected void init() {
        dragging = false;
        addRenderableWidget(Button.builder(layoutLabel(), b -> { Neon.config.armorVertical = !Neon.config.armorVertical; b.setMessage(layoutLabel()); }).bounds(width / 2 - 142, 49, 92, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset position"), b -> {
            Neon.config.armor.x = 1; Neon.config.armor.y = .90; Neon.config.armorAnchor = ArmorAnchor.CUSTOM;
        }).bounds(width / 2 - 46, 49, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(width / 2 + 54, 49, 88, 20).build());
    }
    private Component layoutLabel() { return Component.literal(Neon.config.armorVertical ? "Vertical" : "Horizontal"); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float delta) {}
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        super.extractRenderState(g, x, y, delta);
        g.centeredText(font, "Drag armor anywhere, including screen edges", width / 2, 15, 0xffffffff);
        g.centeredText(font, "Arrows: nudge / Shift: 10 px / Wheel: scale", width / 2, 30, 0xffdddddd);
        NeonHud.draw(g, true);
        HudBounds b = NeonHud.bounds(width, height, true);
        g.outline(b.x(), b.y(), b.width(), b.height(), b.contains(x, y) || dragging ? 0xffffffff : 0xff888888);
    }
    private void move(double x, double y) {
        NeonHud.bounds(width, height, true).move(Neon.config.armor, x, y, width, height);
        Neon.config.armorAnchor = ArmorAnchor.CUSTOM;
    }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        if (super.mouseClicked(e, doubled)) return true;
        HudBounds b = NeonHud.bounds(width, height, true);
        if (e.button() != 0 || !b.contains(e.x(), e.y())) return false;
        dragging = true; offsetX = e.x() - b.x(); offsetY = e.y() - b.y(); return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (!dragging) return super.mouseDragged(e, dx, dy);
        move(e.x() - offsetX, e.y() - offsetY); return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) { boolean wasDragging = dragging; dragging = false; return wasDragging || super.mouseReleased(e); }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (!NeonHud.bounds(width, height, true).contains(x, y) || dy == 0) return super.mouseScrolled(x, y, dx, dy);
        Neon.config.armor.scale = Math.clamp(Neon.config.armor.scale + Math.signum(dy) * .25, .5, 3); return true;
    }
    @Override public boolean keyPressed(KeyEvent e) {
        int step = (e.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;
        HudBounds b = NeonHud.bounds(width, height, true);
        switch (e.key()) {
            case GLFW.GLFW_KEY_LEFT -> move(b.x() - step, b.y());
            case GLFW.GLFW_KEY_RIGHT -> move(b.x() + step, b.y());
            case GLFW.GLFW_KEY_UP -> move(b.x(), b.y() - step);
            case GLFW.GLFW_KEY_DOWN -> move(b.x(), b.y() + step);
            default -> { return super.keyPressed(e); }
        }
        return true;
    }
    @Override public void onClose() { Neon.save(); minecraft.gui.setScreen(parent); }
    @Override public void removed() { Neon.save(); }
}