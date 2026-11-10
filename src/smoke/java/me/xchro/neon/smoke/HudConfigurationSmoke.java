package me.xchro.neon.smoke;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.hud.*;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Real screen input through the unified Unity-derived HUD configuration flow. */
final class HudConfigurationSmoke {
    private final Minecraft mc;
    private final NeonConfig original;
    private int stage, age, checks;
    private SettingsScreen parent;
    private HudBounds beforeDrag;
    HudConfigurationSmoke(Minecraft mc, int checks) {
        this.mc = mc; this.checks = checks; original = ConfigStore.copy(Neon.config);
        Neon.config.resetHud(); mc.options.guiScale().set(2); mc.getWindow().setWindowed(1280, 800); mc.resizeGui();
        mc.gui.setScreen(parent = new SettingsScreen(null));
    }
    int checks() { return checks; }
    private void check(boolean ok, String name) { if (!ok) throw new AssertionError("HUD config stage " + stage + ": " + name); checks++; System.out.println("NEON_CHECK " + name); }
    private void next() { stage++; age = 0; }
    private List<AbstractWidget> widgets(ContainerEventHandler parent) {
        List<AbstractWidget> out = new ArrayList<>();
        for (var child : parent.children()) { if (child instanceof AbstractWidget w) out.add(w); if (child instanceof ContainerEventHandler c) out.addAll(widgets(c)); }
        return out;
    }
    private Button button(String name) { return widgets(mc.gui.screen()).stream().filter(w -> w instanceof Button && w.getMessage().getString().equals(name)).map(w -> (Button) w).findFirst().orElseThrow(() -> new AssertionError("Missing " + name)); }
    private ExactSlider slider(String name) {
        try {
            var field = ExactSlider.class.getDeclaredField("name"); field.setAccessible(true);
            for (var w : widgets(mc.gui.screen())) if (w instanceof ExactSlider s && field.get(s).equals(name)) return s;
            throw new AssertionError("Missing slider " + name);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private MouseButtonEvent mouse(double x, double y, int b) { return new MouseButtonEvent(x, y, new MouseButtonInfo(b, 0)); }
    private void click(AbstractWidget w, int b, boolean value) {
        var screen = (PixelScreen) mc.gui.screen();
        var e = mouse(screen.guiCoordinate(value ? w.getRight() - 8 : w.getX() + 16), screen.guiCoordinate(w.getY() + 8), b);
        check(screen.mouseClicked(e, false), "screen pointer activates " + w.getMessage().getString()); screen.mouseReleased(e);
    }
    private void click(String name, int b) { click(button(name), b, false); }
    private void key(int key, int mods) { mc.gui.screen().keyPressed(new KeyEvent(key, 0, mods)); }
    private void undo() { key(GLFW.GLFW_KEY_Z, GLFW.GLFW_MOD_CONTROL); }
    private void type(String value) {
        key(GLFW.GLFW_KEY_A, GLFW.GLFW_MOD_CONTROL);
        value.codePoints().forEach(cp -> check(mc.gui.screen().charTyped(new CharacterEvent(cp)), "inline HUD number receives native character input"));
    }
    private void shot(String name) { Screenshot.grab(mc.gameDirectory, "neon-unity-hud-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {}); }
    boolean run() {
        age++; if (age < 9) return false;
        switch (stage) {
            case 0 -> {
                click("HUD", 0); check(mc.gui.screen() instanceof HudEditor, "HUD sidebar opens the editor with no intermediary page");
                click("Armor", 1); next();
            }
            case 1 -> {
                shot("armor-options");
                check(List.of("Layout: Horizontal", "Slot frames", "Order: Helmet first", "Warnings", "Damage preview").stream().allMatch(n -> button(n).active), "all armor options live in the editor");
                check(widgets(mc.gui.screen()).stream().noneMatch(w -> Set.of("Align", "Center", "Edit HUD", "Done", "Undo").contains(w.getMessage().getString())), "no preset-position grid, launch button or footer");
                click("Slot frames", 0); check(!Neon.config.armorSlots, "armor frame option updates immediately");
                click("Layout: Horizontal", 0); check(Neon.config.armorVertical, "editor changes armor orientation"); click("Layout: Vertical", 0);
                click("Order: Helmet first", 0); check(Neon.config.armorReverse, "editor changes armor order");
                click("Warnings", 0); check(!Neon.config.armorWarnings, "editor toggles durability warnings"); click("Warnings", 0);
                click(slider("Scale"), 0, true);
                check(mc.gui.screen() instanceof HudEditor && slider("Scale").isEditing(), "HUD exact values edit in the row without a dialog");
                type("2.25"); next();
            }
            case 2 -> {
                shot("inline-number"); key(GLFW.GLFW_KEY_ENTER, 0);
                check(Neon.config.armor.scale == 2.25 && !slider("Scale").isEditing(), "inline scale commits exact native input");
                undo(); check(Neon.config.armor.scale == 1, "one Undo restores the inline scale edit");
                click(slider("Scale"), 0, true); type("9"); key(GLFW.GLFW_KEY_ENTER, 0);
                check(Neon.config.armor.scale == 1 && slider("Scale").isEditing(), "out-of-range inline number stays uncommitted");
                key(GLFW.GLFW_KEY_ESCAPE, 0); check(mc.gui.screen() instanceof HudEditor && !slider("Scale").isEditing(), "Escape cancels inline number editing without leaving the editor");
                click(slider("Spacing"), 0, true); type("3"); key(GLFW.GLFW_KEY_ENTER, 0); check(Neon.config.armorGap == 3, "editor applies exact armor spacing");
                click("Totems", 1); click("Mode: Offhand", 0); check(Neon.config.totemFloating, "totem placement mode is configured inside the editor");
                click("General", 0); next();
            }
            case 3 -> {
                shot("all-options");
                check(button("Guides").active && button("Snap").active && button("Mode: Floating").active, "both HUD modules and general controls remain available together");
                check(Neon.save(), "unified HUD settings save"); Neon.SAVES.flush().join();
                try { var saved = new ConfigStore(mc.gameDirectory.toPath().resolve("config/neon.json")).load(); check(saved.armorGap == 3 && !saved.armorSlots && saved.armorReverse && saved.totemFloating, "all editor settings survive configuration reload"); } catch (Exception e) { throw new AssertionError(e); }
                key(GLFW.GLFW_KEY_ESCAPE, 0); check(mc.gui.screen() == parent, "Escape returns to the same menu placement and page");
                click("HUD", 0); click("Totems", 1); next();
            }
            case 4 -> {
                shot("totem-options"); var editor = (HudEditor) mc.gui.screen(); beforeDrag = NeonHud.totemBounds(editor.width, editor.height);
                var start = mouse(beforeDrag.x() + 10, beforeDrag.y() + 10, 0);
                check(editor.mouseClicked(start, false), "totem canvas accepts a direct drag");
                editor.mouseDragged(mouse(editor.width / 2.0, editor.height / 2.0, 0), 0, 0);
                check(!editor.controlsVisible(), "HUD configuration column hides during placement"); next();
            }
            case 5 -> {
                shot("direct-placement"); var editor = (HudEditor) mc.gui.screen(); editor.mouseReleased(mouse(editor.width / 2.0, editor.height / 2.0, 0));
                undo(); check(NeonHud.totemBounds(editor.width, editor.height).equals(beforeDrag), "Undo restores direct totem placement");
                editor.resize(640 / editor.pixelScale(), 480 / editor.pixelScale());
                click("Armor", 1);
                var scroll = widgets(editor).stream().filter(w -> w.getClass().getSimpleName().equals("UiScroll")).map(w -> (AbstractScrollArea) w).findFirst().orElseThrow();
                scroll.setScrollAmount(scroll.maxScrollAmount()); next();
            }
            case 6 -> {
                shot("small-viewport"); var editor = (HudEditor) mc.gui.screen();
                check(editor.inspectorBounds().bottom() <= editor.canvasHeight() - 8 && button("General").getBottom() <= editor.inspectorBounds().bottom(), "complete HUD configuration stays reachable in a small viewport");
                Neon.applyConfig(original); mc.gui.setScreen(null); mc.resizeGui(); return true;
            }
        }
        return false;
    }
}
