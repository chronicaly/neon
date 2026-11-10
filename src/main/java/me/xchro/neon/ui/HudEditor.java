package me.xchro.neon.ui;

import java.util.*;
import java.util.function.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.NeonConfig;
import me.xchro.neon.config.NeonConfig.ArmorAnchor;
import me.xchro.neon.hud.*;
import me.xchro.neon.core.NumberValue;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Unity HudEditorScreen's root/group/bool/range row model, adapted to Neon's two widgets and renderer. */
public final class HudEditor extends PixelScreen {
    private enum Element { ARMOR, TOTEMS }
    private enum Gesture { NONE, MOVE, RESIZE, PANEL }
    private final Screen parent;
    private final SettingsSession session;
    private final List<Runnable> refreshers = new ArrayList<>();
    private final List<AbstractWidget> inspector = new ArrayList<>();
    private final Set<String> expanded = new HashSet<>();
    private record Row(String key, String label, int depth, Object value, Runnable left, Runnable right) {}
    private record Range(double value, double min, double max, double step, DoubleConsumer setter) {}
    private UiScroll column;
    private Element selected = Element.ARMOR;
    private Gesture gesture = Gesture.NONE;
    private boolean toolbar = true, damagePreview;
    private int px = 12, py = 12, pw = 188, ph, panelOriginX, panelOriginY;
    private double startX, startY, startScale, panelGrabX, panelGrabY;
    private HudBounds origin;
    private HudResize resizeGesture;
    private int guideX = -1, guideY = -1;
    private String error = "";
    private boolean animateRows;
    public HudEditor(Screen parent) {
        super(Ui.label("HUD editor")); this.parent = parent;
        session = parent instanceof SettingsScreen settings ? settings.session() : new SettingsSession();
    }
    public boolean damagePreview() { return damagePreview; }
    public boolean controlsVisible() { return toolbar && gesture != Gesture.MOVE && gesture != Gesture.RESIZE; }
    public ScreenRectangle inspectorBounds() { return new ScreenRectangle(px, py, pw, ph); }
    private NeonConfig.Widget widget() { return selected == Element.ARMOR ? Neon.config.armor : Neon.config.totems; }
    private boolean visible(Element element) { return (element == Element.ARMOR ? Neon.config.armor : Neon.config.totems).visible; }
    private int hudWidth() { return Math.min(width * pixelScale(), minecraft.getWindow().getWidth()); }
    private int hudHeight() { return Math.min(height * pixelScale(), minecraft.getWindow().getHeight()); }
    private HudBounds bounds(Element element) { return element == Element.ARMOR ? NeonHud.pixelBounds(hudWidth(), hudHeight(), true) : NeonHud.totemPixelBounds(hudWidth(), hudHeight()); }
    private double hudCoordinate(double canvas) { return canvas * UI_SCALE; }
    private double controlCoordinate(double pixel) { return pixel / UI_SCALE; }
    private double detachedScale() { return Math.clamp(pixelScale() / NeonHud.density(), .5, 3); }
    private HudBounds bounds() { return bounds(selected); }
    // Ported from Unity HudEditorScreen: a root toggles on left click and folds on right click.
    private void fold(String key) { if (!expanded.remove(key)) expanded.add(key); animateRows = true; rebuildWidgets(); }
    private void root(List<Row> out, Element element, String name) {
        out.add(new Row(element.name(), name, 0, element == Element.ARMOR ? Neon.config.armor.visible : Neon.config.totems.visible,
            () -> { selected = element; session.change(() -> widget().visible = !widget().visible); if(widget().visible)UiSounds.moduleEnabled(); rebuildWidgets(); },
            () -> { selected = element; fold(element.name()); }));
    }
    private void group(List<Row> out, String key, String name, int depth) { out.add(new Row(key, name, depth, null, () -> fold(key), () -> fold(key))); }
    private void bool(List<Row> out, String key, String name, int depth, boolean value, Runnable action) {
        out.add(new Row(key, name, depth, value, () -> { session.change(action); rebuildWidgets(); }, null));
    }
    private void range(List<Row> out, String key, String name, int depth, double value, double min, double max, double step, DoubleConsumer setter) {
        out.add(new Row(key, name, depth, new Range(value, min, max, step, setter), null, null));
    }
    private void choice(List<Row> out, String key, String name, String value, Runnable cycle) {
        out.add(new Row(key, name + ": " + value, 1, null, () -> { session.change(cycle); rebuildWidgets(); }, null));
    }
    private List<Row> makeRows() {
        List<Row> out = new ArrayList<>(); var c = Neon.config;
        for (Element e : Element.values()) {
            root(out, e, e == Element.ARMOR ? "Armor" : "Totems");
            if (!expanded.contains(e.name())) continue;
            var widget = e == Element.ARMOR ? c.armor : c.totems;
            range(out, e + ":Scale", "Scale", 1, e == Element.TOTEMS && !c.totemFloating ? detachedScale() : widget.scale, .5, 3, .05,
                v -> { selected = e; resizeAtTopLeft(v); });
            if (e == Element.ARMOR) {
                choice(out, "armor:layout", "Layout", c.armorVertical ? "Vertical" : "Horizontal", () -> c.armorVertical = !c.armorVertical);
                bool(out, "armor:frames", "Slot frames", 1, c.armorSlots, () -> c.armorSlots = !c.armorSlots);
                choice(out, "armor:order", "Order", c.armorReverse ? "Boots first" : "Helmet first", () -> c.armorReverse = !c.armorReverse);
                range(out, "armor:spacing", "Spacing", 1, c.armorGap, 0, 8, 1, v -> c.armorGap = (int) v);
                bool(out, "armor:warnings", "Warnings", 1, c.armorWarnings, () -> c.armorWarnings = !c.armorWarnings);
                if (c.armorWarnings) range(out, "armor:threshold", "Warn below", 1, c.armorWarningPercent, 1, 30, 1, v -> c.armorWarningPercent = (int) v);
                bool(out, "armor:preview", "Damage preview", 1, damagePreview, () -> damagePreview = !damagePreview);
            } else {
                choice(out, "totems:mode", "Mode", c.totemFloating ? "Floating" : "Offhand", () -> {
                    selected = Element.TOTEMS; if (c.totemFloating) c.totemFloating = false; else detachTotem();
                });
            }
        }
        group(out, "general", "General", 0);
        if (expanded.contains("general")) {
            range(out, "general:scale", "HUD scale", 1, c.hudScale, .5, 2, .05, v -> c.hudScale = v);
            bool(out, "general:guides", "Guides", 1, c.editorGuides, () -> c.editorGuides = !c.editorGuides);
            bool(out, "general:snap", "Snap", 1, c.editorSnap, () -> c.editorSnap = !c.editorSnap);
            range(out, "general:sensitivity", "Snap sensitivity", 1, c.snapSensitivity, 1, 24, 1, v -> c.snapSensitivity = (int) v);
        }
        return out;
    }
    @Override protected void init() {
        WidgetMemory memory = new WidgetMemory(column);
        double position = column == null ? 0 : column.scrollAmount(); column = null;
        session.end(); gesture = Gesture.NONE; resizeGesture = null; refreshers.clear(); inspector.clear(); guideX = guideY = -1;
        if (!toolbar) return;
        pw = Math.min(188, canvasWidth() - 16);
        px = Math.clamp(px, 8, canvasWidth() - pw - 8);
        py = Math.clamp(py, 8, Math.max(8, canvasHeight() - 89));
        LinearLayout rows = LinearLayout.vertical().spacing(0);
        for (Row row : makeRows()) {
            if (row.value instanceof Range r) {
                rows.addChild(new ExactSlider(this, session, row.label, pw - 8, r.value, r.min, r.max, r.step,
                    n -> NumberValue.format(n) + (row.label.endsWith("scale") || row.label.equals("Scale") ? "x" : row.label.equals("Warn below") ? "%" : row.label.equals("Snap sensitivity") ? "px" : ""), r.setter).inlineRow().identity(row.key));
            } else {
                rows.addChild(new HudEntry(row.label, pw - 8, () -> Boolean.TRUE.equals(row.value),
                    () -> expanded.contains(row.key), row.left == null ? () -> {} : row.left, row.right == null ? () -> {} : row.right).depth(row.depth).identity(row.key));
            }
        }
        rows.arrangeElements();
        ph = Math.min(23 + rows.getHeight(), canvasHeight() - py - 8);
        column = addInspector(new UiScroll(px + 1, py + 22, pw - 2, Math.max(22, ph - 23), rows));
        column.setScrollAmount(position);
        memory.restore(column, animateRows); animateRows = false;
        if (column.getFocused() != null) setFocused(column);
        refresh();
    }
    private <T extends AbstractWidget> T addInspector(T widget) { inspector.add(widget); return addRenderableWidget(widget); }
    private void select(Element next) {
        if (selected == next) return;
        selected = next;
    }
    private void detachTotem() {
        if (selected != Element.TOTEMS || Neon.config.totemFloating) return;
        HudBounds before = bounds(); Neon.config.totemFloating = true; widget().scale = detachedScale();
        before.move(widget(), before.x(), before.y(), hudWidth(), hudHeight());
    }
    private void resizeAtTopLeft(double scale) { detachTotem(); HudBounds before = bounds(); widget().scale = scale; move(before.x(), before.y(), false); }
    private void refresh() { refreshers.forEach(Runnable::run); }
    private ExactSlider editingSlider() {
        if (column == null) return null;
        return column.children().stream().filter(w -> w instanceof ExactSlider slider && slider.isEditing()).map(w -> (ExactSlider) w).findFirst().orElse(null);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void pixelBackground(GuiGraphicsExtractor g, int x, int y, float delta) {}
    @Override protected void renderPixels(GuiGraphicsExtractor g, int mx, int my, float delta) {
        // Gameplay and editor share physical-pixel geometry; attached totems follow the real offhand slot.
        g.pose().pushMatrix(); g.pose().scale(1 / UI_SCALE);
        NeonHud.drawPixels(g, true, hudWidth(), hudHeight());
        if (Neon.config.totems.visible && !Neon.config.totemFloating && (minecraft.player == null || !minecraft.player.getOffhandItem().is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING))) NeonHud.drawTotemPixels(g, true, hudWidth(), hudHeight());
        g.pose().popMatrix();
        boolean moving = gesture == Gesture.MOVE || gesture == Gesture.RESIZE;
        if (moving && Neon.config.editorGuides) {
            Ui.rect(g, canvasWidth() / 2, 0, 1, canvasHeight(), 0x30ffffff);
            Ui.rect(g, 0, canvasHeight() / 2, canvasWidth(), 1, 0x30ffffff);
            if (guideX >= 0) Ui.rect(g, (int) Math.round(controlCoordinate(guideX)), 0, 1, canvasHeight(), Ui.BLUE);
            if (guideY >= 0) Ui.rect(g, 0, (int) Math.round(controlCoordinate(guideY)), canvasWidth(), 1, Ui.BLUE);
        }
        if (controlsVisible()) {
            Ui.rect(g, px, py, pw, ph, Ui.PANEL); Ui.stroke(g, px, py, pw, ph, Ui.LINE); Ui.text(g, "HUD", px + 7, py + 6, Ui.TEXT);
            Ui.rect(g, px + 1, py + 21, pw - 2, 1, Ui.LINE);
            refresh(); renderWidgets(g, mx, my, delta);
        }
        if (!error.isEmpty()) Ui.small(g, error, 12, 12, Ui.RED);
    }
    private HudBounds handle(HudBounds b, int corner) {
        int size = (int) Math.ceil(hudCoordinate(7));
        int x = (corner % 2 == 0 ? b.x() : b.x() + b.width()) - size / 2;
        int y = (corner < 2 ? b.y() : b.y() + b.height()) - size / 2;
        return new HudBounds(Math.clamp(x, 0, hudWidth() - size), Math.clamp(y, 0, hudHeight() - size), size, size);
    }
    private void move(double x, double y, boolean snap) {
        detachTotem();
        HudBounds b = bounds(); guideX = guideY = -1;
        if (snap && Neon.config.editorSnap) {
            Element neighbor = selected == Element.ARMOR ? Element.TOTEMS : Element.ARMOR;
            HudBounds other = visible(neighbor) ? bounds(neighbor) : null;
            double nx = snapAxis(x, b.width(), hudWidth(), other == null ? 0 : other.x(), other == null ? -1 : other.width());
            double ny = snapAxis(y, b.height(), hudHeight(), other == null ? 0 : other.y(), other == null ? -1 : other.height());
            if (nx != x) guideX = (int) nx;
            if (ny != y) guideY = (int) ny;
            x = nx; y = ny;
        }
        b.move(widget(), x, y, hudWidth(), hudHeight());
        if (selected == Element.ARMOR) Neon.config.armorAnchor = ArmorAnchor.CUSTOM;
    }
    private double snapAxis(double value, int size, int extent, int other, int otherSize) {
        double result = value, nearest = Neon.config.snapSensitivity;
        double[] anchors = otherSize < 0 ? new double[]{0, (extent-size)/2.0, extent-size}
            : new double[]{0, (extent-size)/2.0, extent-size, other, other+(otherSize-size)/2.0, other+otherSize-size};
        for (double anchor : anchors) {
            double distance = Math.abs(value - anchor);
            if (distance < nearest) { nearest = distance; result = anchor; }
        }
        return result;
    }
    @Override protected boolean pixelMouseClicked(MouseButtonEvent e, boolean twice) {
        ExactSlider editing = editingSlider();
        if (editing != null && !editing.isMouseOver(e.x(), e.y()) && !editing.finishInline(true)) return true;
        if (toolbar && e.button() == 0 && e.x() >= px && e.x() <= px + pw && e.y() >= py && e.y() < py + 22) {
            gesture = Gesture.PANEL; panelOriginX = px; panelOriginY = py; panelGrabX = e.x() - px; panelGrabY = e.y() - py; return true;
        }
        if (super.pixelMouseClicked(e, twice)) return true;
        if (toolbar && e.x() >= px && e.x() <= px + pw && e.y() >= py && e.y() <= py + ph) return true;
        e = new MouseButtonEvent(hudCoordinate(e.x()), hudCoordinate(e.y()), e.buttonInfo());
        if (e.button() == 1) {
            for (Element element : Element.values()) if (visible(element) && bounds(element).contains(e.x(), e.y())) {
                selected = element; expanded.add(element.name()); toolbar = true; rebuildWidgets(); return true;
            }
            return false;
        }
        if (e.button() != 0) return false;
        setFocused((GuiEventListener) null);
        // Selection takes precedence when widgets overlap; the inspector can select either one.
        for (Element element : new Element[]{selected, selected == Element.ARMOR ? Element.TOTEMS : Element.ARMOR}) {
            if (!visible(element)) continue;
            HudBounds b = bounds(element);
            for (int i=0;i<4;i++) if(handle(b,i).contains(e.x(),e.y())) {
                select(element);start(e,Gesture.RESIZE);resizeGesture=new HudResize(widget(),origin,i,startScale);return true;
            }
            if (b.contains(e.x(), e.y())) { select(element); start(e, Gesture.MOVE); return true; }
        }
        return false;
    }
    private void start(MouseButtonEvent e, Gesture type) {
        session.begin(); gesture = type; origin = bounds(); startX = e.x(); startY = e.y();
        startScale = selected == Element.TOTEMS && !Neon.config.totemFloating ? detachedScale() : widget().scale;
    }
    @Override protected boolean pixelMouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (gesture == Gesture.PANEL) {
            int nx = Math.clamp((int) (e.x() - panelGrabX), 8, canvasWidth() - pw - 8);
            int ny = Math.clamp((int) (e.y() - panelGrabY), 8, Math.max(8, canvasHeight() - ph - 8));
            int ox = nx - px, oy = ny - py; px = nx; py = ny;
            for (AbstractWidget widget : inspector) { widget.setX(widget.getX() + ox); widget.setY(widget.getY() + oy); } return true;
        }
        if (gesture != Gesture.MOVE && gesture != Gesture.RESIZE) return super.pixelMouseDragged(e, dx, dy);
        e = new MouseButtonEvent(hudCoordinate(e.x()), hudCoordinate(e.y()), e.buttonInfo());
        if (gesture == Gesture.MOVE) { move(origin.x() + e.x() - startX, origin.y() + e.y() - startY, (e.modifiers() & GLFW.GLFW_MOD_ALT) == 0); refresh(); return true; }
        if (gesture == Gesture.RESIZE) {
            detachTotem();
            resizeGesture.drag(e.x() - startX, e.y() - startY, hudWidth(), hudHeight());
            if (selected == Element.ARMOR) Neon.config.armorAnchor = ArmorAnchor.CUSTOM;
            resizeGesture.place(bounds(), hudWidth(), hudHeight());
            refresh(); return true;
        }
        return false;
    }
    @Override protected boolean pixelMouseReleased(MouseButtonEvent e) {
        boolean handled = gesture != Gesture.NONE; gesture = Gesture.NONE; guideX = guideY = -1; session.end();
        boolean control = super.pixelMouseReleased(e); if (handled) rebuildWidgets(); return handled || control;
    }
    @Override protected boolean pixelMouseScrolled(double x, double y, double dx, double dy) {
        if (toolbar && x >= px && x <= px + pw && y >= py && y <= py + ph) return super.pixelMouseScrolled(x, y, dx, dy);
        if (visible(selected) && bounds().contains(hudCoordinate(x), hudCoordinate(y)) && dy != 0) {
            double current = selected == Element.TOTEMS && !Neon.config.totemFloating ? detachedScale() : widget().scale;
            session.change(() -> resizeAtTopLeft(Math.clamp(current + Math.signum(dy) * .05, .5, 3))); rebuildWidgets(); return true;
        }
        return super.pixelMouseScrolled(x, y, dx, dy);
    }
    @Override public boolean keyPressed(KeyEvent e) {
        ExactSlider editing = editingSlider();
        if (editing != null) return editing.keyPressed(e);
        if (e.key() == GLFW.GLFW_KEY_Z && (e.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) { session.undo(); rebuildWidgets(); return true; }
        if (e.key() == GLFW.GLFW_KEY_H || e.key() == GLFW.GLFW_KEY_TAB) { toolbar = !toolbar; rebuildWidgets(); return true; }
        if (e.key() == GLFW.GLFW_KEY_ESCAPE && gesture != Gesture.NONE) {
            if (gesture == Gesture.PANEL) { px = panelOriginX; py = panelOriginY; }
            else session.cancel();
            rebuildWidgets(); return true;
        }
        if (getFocused() != null || !visible(selected)) return super.keyPressed(e);
        int step = (e.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1; HudBounds b = bounds();
        switch (e.key()) {
            case GLFW.GLFW_KEY_LEFT -> session.change(() -> move(b.x() - step, b.y(), false));
            case GLFW.GLFW_KEY_RIGHT -> session.change(() -> move(b.x() + step, b.y(), false));
            case GLFW.GLFW_KEY_UP -> session.change(() -> move(b.x(), b.y() - step, false));
            case GLFW.GLFW_KEY_DOWN -> session.change(() -> move(b.x(), b.y() + step, false));
            default -> { return super.keyPressed(e); }
        }
        refresh(); return true;
    }
    @Override public void onClose() { session.end(); if (Neon.save()) minecraft.gui.setScreen(parent); else error = "Could not save settings. Check the game log."; }
    @Override public void removed() { session.end(); gesture = Gesture.NONE; Neon.flushSaves(); }
}
