package me.xchro.neon.ui;

import me.xchro.neon.Neon;
import me.xchro.neon.config.NeonConfig.Widget;
import me.xchro.neon.hud.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class HudEditor extends Screen {
    private final Screen parent;
    private Widget dragging;
    private boolean armor;
    private double offsetX, offsetY;
    public HudEditor(Screen parent) { super(Component.literal("Move Neon HUD")); this.parent = parent; }
    @Override protected void init() {
        dragging = null;
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(width / 2 - 50, 42, 100, 20).build());
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float delta) {}
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        super.extractRenderState(g, x, y, delta);
        g.centeredText(font, "Drag armor and totem widgets to move them", width / 2, 18, 0xffffffff);
        NeonHud.draw(g, true);
        for (boolean isArmor : new boolean[]{true, false}) {
            if (!(isArmor ? Neon.config.armor : Neon.config.totems).visible) continue;
            HudBounds b = NeonHud.bounds(isArmor, width, height, true);
            g.outline(b.x() - 2, b.y() - 2, b.width() + 4, b.height() + 4, b.contains(x, y) ? 0xffffffff : 0xff888888);
        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        if (super.mouseClicked(e, doubled)) return true;
        if (e.button() != 0) return false;
        for (boolean isArmor : new boolean[]{false, true}) {
            Widget widget = isArmor ? Neon.config.armor : Neon.config.totems;
            HudBounds b = NeonHud.bounds(isArmor, width, height, true);
            if (widget.visible && b.contains(e.x(), e.y())) { dragging = widget; armor = isArmor; offsetX = e.x() - b.x(); offsetY = e.y() - b.y(); return true; }
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (dragging == null) return super.mouseDragged(e, dx, dy);
        NeonHud.bounds(armor, width, height, true).move(dragging, e.x() - offsetX, e.y() - offsetY, width, height); return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) { dragging = null; return super.mouseReleased(e); }
    @Override public void onClose() { Neon.save(); minecraft.gui.setScreen(parent); }
    @Override public void removed() { Neon.save(); }
}
