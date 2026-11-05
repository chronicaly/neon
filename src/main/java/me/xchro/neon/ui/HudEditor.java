package me.xchro.neon.ui;

import java.util.*;
import java.util.function.DoubleConsumer;
import me.xchro.neon.Neon;
import me.xchro.neon.config.NeonConfig.ArmorAnchor;
import me.xchro.neon.core.NumberValue;
import me.xchro.neon.hud.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class HudEditor extends Screen {
    private final Screen parent;
    private final SettingsSession session;
    private final List<Runnable> refreshers = new ArrayList<>();
    private boolean dragging, damagePreview, toolbar = true;
    private double offsetX, offsetY;
    public HudEditor(Screen parent) {
        super(Component.literal("Armor Position")); this.parent = parent;
        session = parent instanceof SettingsScreen settings ? settings.session() : new SettingsSession();
    }
    public boolean damagePreview() { return damagePreview; }
    @Override protected void init() {
        if (dragging) session.end(); dragging = false; refreshers.clear();
        if (!toolbar) {
            addRenderableWidget(Button.builder(Component.literal("Show controls (H)"), b -> { toolbar = true; rebuildWidgets(); }).bounds(width / 2 - 65, 12, 130, 20).build());
            return;
        }
        int panel = Math.min(312, width - 16), w = (panel - 12) / 4, left = (width - panel) / 2;
        Button layout = action("", left, 44, w, () -> Neon.config.armorVertical = !Neon.config.armorVertical, "Switch between horizontal and vertical armor.");
        refreshers.add(() -> layout.setMessage(Component.literal(Neon.config.armorVertical ? "Vertical" : "Horizontal")));
        Button guides = action("", left + w + 4, 44, w, () -> Neon.config.editorGuides = !Neon.config.editorGuides, "Show the screen center and edge guides while editing.");
        refreshers.add(() -> guides.setMessage(Component.literal("Guides: " + (Neon.config.editorGuides ? "ON" : "OFF"))));
        Button snap = action("", left + 2 * (w + 4), 44, w, () -> Neon.config.editorSnap = !Neon.config.editorSnap, "Snap dragging to screen edges or center within 4 pixels. Exact values and arrow nudges stay precise.");
        refreshers.add(() -> snap.setMessage(Component.literal("Snap: " + (Neon.config.editorSnap ? "ON" : "OFF"))));
        action("Done", left + 3 * (w + 4), 44, w, this::onClose, "Save and return to settings.");
        Button x = action("", left, 68, w, () -> {
            HudBounds b = bounds(); number("Armor X", b.x(), 0, width - b.width(), true, value -> move(value, bounds().y(), false));
        }, "Enter the exact X coordinate, measured from the left screen edge in GUI pixels.");
        Button y = action("", left + w + 4, 68, w, () -> {
            HudBounds b = bounds(); number("Armor Y", b.y(), 0, height - b.height(), true, value -> move(bounds().x(), value, false));
        }, "Enter the exact Y coordinate, measured from the top screen edge in GUI pixels.");
        Button scale = action("", left + 2 * (w + 4), 68, w, () -> number("Armor scale", Neon.config.armor.scale, .5, 3, false, value -> Neon.config.armor.scale = value), "Type an exact scale, or use the wheel while hovering over the armor.");
        refreshers.add(() -> { x.setMessage(Component.literal("X: " + bounds().x())); y.setMessage(Component.literal("Y: " + bounds().y())); scale.setMessage(Component.literal("Scale: " + NumberValue.format(Neon.config.armor.scale))); });
        action("Reset pos.", left + 3 * (w + 4), 68, w, () -> {
            Neon.config.armor.x = 1; Neon.config.armor.y = .90; Neon.config.armorAnchor = ArmorAnchor.CUSTOM;
        }, "Reset only the armor position. Undo restores its previous position.");
        Button preview = action("", left, 92, w * 2 + 4, () -> damagePreview = !damagePreview, "Preview nearly broken armor using display copies. Your actual equipment is never changed.");
        refreshers.add(() -> preview.setMessage(Component.literal("Damage preview: " + (damagePreview ? "ON" : "OFF"))));
        Button undo = addRenderableWidget(Button.builder(Component.literal("Undo"), b -> { session.undo(); refresh(); }).bounds(left + 2 * (w + 4), 92, w, 20).build());
        refreshers.add(() -> undo.active = session.canUndo());
        action("Hide (H)", left + 3 * (w + 4), 92, w, () -> { toolbar = false; rebuildWidgets(); }, "Hide the toolbar to place armor anywhere. Press H to bring it back.");
        refresh();
    }
    private HudBounds bounds() { return NeonHud.bounds(width, height, true); }
    private Button action(String label, int x, int y, int width, Runnable action, String tooltip) {
        Button button = addRenderableWidget(Button.builder(Component.literal(label), b -> { session.change(action); refresh(); }).bounds(x, y, width, 20).build());
        button.setTooltip(Tooltip.create(Component.literal(tooltip))); return button;
    }
    private void number(String name, double value, double min, double max, boolean integer, DoubleConsumer set) {
        minecraft.gui.setScreen(new NumberScreen(this, name, value, min, max, integer, next -> session.change(() -> set.accept(next))));
    }
    private void refresh() { refreshers.forEach(Runnable::run); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float delta) {}
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        if (Neon.config.editorGuides) {
            g.fill(width / 2, 0, width / 2 + 1, height, 0x55ffffff);
            g.fill(0, height / 2, width, height / 2 + 1, 0x55ffffff);
            g.outline(0, 0, width, height, 0x5599ccff);
        }
        NeonHud.draw(g, true);
        HudBounds b = bounds();
        g.outline(b.x(), b.y(), b.width(), b.height(), b.contains(x, y) || dragging ? 0xffffffff : 0xff888888);
        refresh(); super.extractRenderState(g, x, y, delta);
        if (toolbar) {
            g.centeredText(font, "Drag armor or enter exact coordinates", width / 2, 12, 0xffffffff);
            g.centeredText(font, "Arrows: nudge / Shift: 10 px / Ctrl+Z: undo", width / 2, 27, 0xffdddddd);
        }
    }
    private void move(double x, double y, boolean snap) {
        HudBounds b = bounds();
        if (snap) { HudBounds snapped = b.snapped(x, y, width, height, 4); x = snapped.x(); y = snapped.y(); }
        b.move(Neon.config.armor, x, y, width, height); Neon.config.armorAnchor = ArmorAnchor.CUSTOM;
    }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        if (super.mouseClicked(e, doubled)) return true;
        HudBounds b = bounds();
        if (e.button() != 0 || !b.contains(e.x(), e.y())) return false;
        session.begin(); dragging = true; offsetX = e.x() - b.x(); offsetY = e.y() - b.y(); return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (!dragging) return super.mouseDragged(e, dx, dy);
        move(e.x() - offsetX, e.y() - offsetY, Neon.config.editorSnap); return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) {
        boolean wasDragging = dragging; dragging = false; session.end(); refresh(); return wasDragging || super.mouseReleased(e);
    }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (children().stream().anyMatch(child -> child.isMouseOver(x, y)) || !bounds().contains(x, y) || dy == 0) return super.mouseScrolled(x, y, dx, dy);
        session.change(() -> Neon.config.armor.scale = Math.clamp(Neon.config.armor.scale + Math.signum(dy) * .25, .5, 3)); refresh(); return true;
    }
    @Override public boolean keyPressed(KeyEvent e) {
        if (e.key() == GLFW.GLFW_KEY_Z && (e.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) { session.undo(); refresh(); return true; }
        if (e.key() == GLFW.GLFW_KEY_H) { toolbar = !toolbar; rebuildWidgets(); return true; }
        int step = (e.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;
        HudBounds b = bounds();
        switch (e.key()) {
            case GLFW.GLFW_KEY_LEFT -> session.change(() -> move(b.x() - step, b.y(), false));
            case GLFW.GLFW_KEY_RIGHT -> session.change(() -> move(b.x() + step, b.y(), false));
            case GLFW.GLFW_KEY_UP -> session.change(() -> move(b.x(), b.y() - step, false));
            case GLFW.GLFW_KEY_DOWN -> session.change(() -> move(b.x(), b.y() + step, false));
            default -> { return super.keyPressed(e); }
        }
        refresh(); return true;
    }
    @Override public void onClose() { session.end(); Neon.save(); minecraft.gui.setScreen(parent); }
    @Override public void removed() { session.end(); dragging = false; Neon.save(); }
}