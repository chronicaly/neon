package me.xchro.neon.smoke;

import java.util.*;
import java.nio.file.Files;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.hud.*;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.*;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.lwjgl.glfw.GLFW;

/** Display-pixel parity and transformed native input on the real packaged client. */
final class DensitySmoke {
    private final Minecraft mc;
    private final NeonConfig original;
    private int stage, age, checks;
    private ScreenRectangle baseline;
    private final int[][] captures = new int[3][];
    private final int[] borderGaps = new int[3];
    private boolean requested;
    private HudBounds hudBefore;
    private double dragX, dragY;
    DensitySmoke(Minecraft mc, int checks) {
        this.mc = mc; this.checks = checks; original = ConfigStore.copy(Neon.config);
        clearPlacement(); mc.options.guiScale().set(1); mc.getWindow().setWindowed(1280, 800); mc.resizeGui();
        mc.player.setXRot(12); mc.player.setYRot(70); mc.gui.setScreen(Menus.fresh(null));
    }
    int checks() { return checks; }
    private void clearPlacement() { try { new me.xchro.neon.config.MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"), Neon.SAVES); Neon.SAVES.flush().join(); } catch (Exception e) { throw new AssertionError(e); } }
    private void check(boolean ok, String message) { if (!ok) throw new AssertionError("Density stage " + stage + ": " + message); checks++; System.out.println("NEON_CHECK " + message); }
    private void next() { stage++; age = 0; requested = false; }
    private List<AbstractWidget> widgets(ContainerEventHandler parent) {
        List<AbstractWidget> result = new ArrayList<>();
        for (var child : parent.children()) { if (child instanceof AbstractWidget w) result.add(w); if (child instanceof ContainerEventHandler c) result.addAll(widgets(c)); }
        return result;
    }
    private Button button(String name) { return widgets(mc.gui.screen()).stream().filter(w -> w instanceof Button && w.getMessage().getString().equals(name)).map(w -> (Button) w).findFirst().orElseThrow(() -> new AssertionError("No button " + name)); }
    private MouseButtonEvent mouse(double x, double y, int button) { return new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0)); }
    private void click(String name, int mouseButton) {
        var b = button(name); var screen = (PixelScreen) mc.gui.screen();
        var e = mouse(screen.guiCoordinate(b.getX() + b.getWidth() / 2.0), screen.guiCoordinate(b.getY() + b.getHeight() / 2.0), mouseButton);
        check(screen.mouseClicked(e, false), "native pointer hits " + name + " at GUI scale " + screen.pixelScale());
        screen.mouseReleased(e);
    }
    private void shot(String name) { Screenshot.grab(mc.gameDirectory, "neon-density-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {}); }
    private void capture(int index) {
        var b = ((SettingsScreen) mc.gui.screen()).menuBounds();
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                int w = Math.round(230 * PixelScreen.UI_SCALE), h = Math.round(96 * PixelScreen.UI_SCALE);
                int left = Math.round((b.left() + 90) * PixelScreen.UI_SCALE), top = Math.round((b.top() + 8) * PixelScreen.UI_SCALE);
                int[] pixels = new int[w * h]; int p = 0;
                for (int y = top; y < top + h; y++) for (int x = left; x < left + w; x++) pixels[p++] = image.getPixel(x, y);
                int x0 = Math.round(b.left() * PixelScreen.UI_SCALE), y0 = Math.round(b.top() * PixelScreen.UI_SCALE);
                int x1 = Math.round(b.right() * PixelScreen.UI_SCALE) - 1, y1 = Math.round(b.bottom() * PixelScreen.UI_SCALE) - 1;
                for (int x = x0; x <= x1; x++) for (int y : new int[]{y0, y1})
                    if ((image.getPixel(x, y) >>> 16 & 255) < 30) borderGaps[index]++;
                for (int y = y0 + 1; y < y1; y++) for (int x : new int[]{x0, x1})
                    if ((image.getPixel(x, y) >>> 16 & 255) < 30) borderGaps[index]++;
                captures[index] = pixels;
            }
        });
    }
    boolean run() {
        age++; if (age < 12) return false;
        if (stage < 3) {
            var menu = (SettingsScreen) mc.gui.screen(); var b = menu.menuBounds();
            if (!requested) { requested = true; capture(stage); return false; }
            if (captures[stage] == null) return false;
            check(menu.pixelScale() == stage + 1, "verification uses actual Minecraft GUI scale " + (stage + 1));
            check(b.width() == 440 && b.height() == 280 && button("Freelook").getHeight() == 26 && button("Combat").getHeight() == 23, "window, cards and categories retain compact physical dimensions");
            check(Math.round(b.width() * PixelScreen.UI_SCALE) == 582 && Math.round(b.height() * PixelScreen.UI_SCALE) == 370, "menu renders another fifteen percent larger at 582 by 370 display pixels");
            check(borderGaps[stage] == 0, "animated outline remains continuous with no fractional-scale gaps (" + borderGaps[stage] + ")");
            if (stage == 0) {
                Set<Integer> edgeShades = new HashSet<>();
                for (int pixel : captures[0]) {
                    int r = pixel >>> 16 & 255, g = pixel >>> 8 & 255, blue = pixel & 255;
                    if (r > 22 && r < 224 && g - r >= 4 && g - r <= 9 && blue - g >= 6 && blue - g <= 12) edgeShades.add(r);
                }
                check(edgeShades.size() > 60, "GPU text retains smooth antialiasing (" + edgeShades.size() + " intermediate shades)");
            }
            if (stage == 0) baseline = b;
            else {
                check(b.equals(baseline), "menu occupies the identical display rectangle after GUI scale change");
                int different = 0;
                for (int i = 0; i < captures[0].length; i++) if (captures[0][i] != captures[stage][i]) different++;
                check(different < captures[0].length / 50, "rendered text and card pixels match across GUI scales (" + different + " changed pixels)");
            }
            shot("scale-" + (stage + 1));
            if (stage < 2) { mc.options.guiScale().set(stage + 2); mc.resizeGui(); }
            next(); return false;
        }
        switch (stage) {
            case 3 -> {
                click("Combat", 0); click("Autoclicker", 0);
                check(button("Hold").active, "module settings open through transformed screen input");
                click("Visuals", 0); click("Zoom", 0);
                var menu = (SettingsScreen) mc.gui.screen();
                var bounds = menu.menuBounds();
                var corner = mouse(menu.guiCoordinate(bounds.right()), menu.guiCoordinate(bounds.bottom()), 0);
                menu.mouseClicked(corner, false);
                var smaller = mouse(menu.guiCoordinate(bounds.right()-120), menu.guiCoordinate(bounds.bottom()-70), 0);
                menu.mouseDragged(smaller, smaller.x()-corner.x(), smaller.y()-corner.y()); menu.mouseReleased(smaller);
                var scroll = widgets(menu).stream().filter(w -> w instanceof AbstractScrollArea).map(w -> (AbstractScrollArea) w).findFirst().orElseThrow();
                check(menu.mouseScrolled(menu.guiCoordinate(scroll.getX() + 30), menu.guiCoordinate(scroll.getY() + 40), 0, -8), "wheel scrolling accepts transformed menu coordinates");
                var visible = widgets(menu).stream().filter(w -> w instanceof Button && w.getY() >= scroll.getY() && w.getBottom() <= scroll.getBottom()).map(w -> (Button) w).findFirst().orElseThrow();
                var g = new GuiGraphicsExtractor(mc, new GuiRenderState(), 0, 0); g.pose().scale(PixelScreen.UI_SCALE / menu.pixelScale());
                g.enableScissor(visible.getX(), visible.getY(), visible.getRight(), visible.getBottom());
                visible.extractRenderState(g, visible.getX() + 2, visible.getY() + 2, 0);
                check(visible.isHovered(), "native hover clipping follows the pixel canvas");
                visible.extractRenderState(g, visible.getRight() + 4, visible.getY() + 2, 0);
                check(!visible.isHovered(), "clipped controls cannot hover outside their bounds"); g.disableScissor();
                var b = menu.menuBounds();
                var start = mouse(menu.guiCoordinate(b.left() + 30), menu.guiCoordinate(b.top() + 15), 0);
                menu.mouseClicked(start, false);
                var end = mouse(start.x() + menu.guiCoordinate(30), start.y() + menu.guiCoordinate(18), 0);
                menu.mouseDragged(end, end.x() - start.x(), end.y() - start.y()); menu.mouseReleased(end);
                check(menu.menuBounds().left() == b.left() + 30 && menu.menuBounds().top() == b.top() + 18, "header drag follows the scaled pointer at GUI scale 3");
                mc.options.guiScale().set(2); mc.resizeGui(); mc.gui.setScreen(new HudEditor(Menus.fresh(null))); next();
            }
            case 4 -> {
                var editor = (HudEditor) mc.gui.screen();
                check(editor.inspectorBounds().width() == 188 && editor.inspectorBounds().height() == 89, "HUD editor starts as Unity's compact collapsed column");
                check(widgets(editor).stream().noneMatch(w -> w.getWidth() > 188), "HUD editor has no full-width toolbar");
                check(widgets(editor).stream().noneMatch(w -> Set.of("Undo", "Done", "×").contains(w.getMessage().getString())), "HUD editor has no close or footer buttons");
                shot("hud-simple");
                int count = widgets(editor).size(); click("Armor", 1);
                check(widgets(editor).size() > count && button("Layout: " + (Neon.config.armorVertical ? "Vertical" : "Horizontal")).active && button("Slot frames").active && widgets(editor).stream().anyMatch(w -> w instanceof ExactSlider), "Unity-style expansion exposes armor configuration inside the editor");
                check(widgets(editor).stream().noneMatch(w -> Set.of("Align", "Center", "Reset widget").contains(w.getMessage().getString()) || w.getMessage().getString().startsWith("X: ") || w.getMessage().getString().startsWith("Y: ")), "HUD settings have no position presets or coordinate forms");
                next();
            }
            case 5 -> {
                var editor = (HudEditor) mc.gui.screen();
                shot("hud-expanded");
                hudBefore = NeonHud.bounds(editor.width, editor.height, true);
                var start = mouse(hudBefore.x() + hudBefore.width() / 2.0, hudBefore.y() + hudBefore.height() / 2.0, 0);
                check(editor.mouseClicked(start, false), "HUD canvas accepts actual gameplay coordinates");
                dragX = editor.width / 2.0; dragY = editor.height / 2.0;
                editor.mouseDragged(mouse(dragX, dragY, 0), 0, 0);
                check(!editor.controlsVisible(), "entire inspector hides during a widget drag"); next();
            }
            case 6 -> {
                shot("hud-dragging"); var editor = (HudEditor) mc.gui.screen();
                var moved = NeonHud.bounds(editor.width, editor.height, true);
                check(Math.abs(moved.x() + moved.width() / 2.0 - dragX) <= 1 && Math.abs(moved.y() + moved.height() / 2.0 - dragY) <= 1, "HUD preview follows the cursor in gameplay space");
                editor.mouseReleased(mouse(dragX, dragY, 0)); check(editor.controlsVisible(), "inspector returns after releasing the widget");
                editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z, 0, GLFW.GLFW_MOD_CONTROL));
                check(NeonHud.bounds(editor.width, editor.height, true).equals(hudBefore), "one Undo restores the entire canvas move");
                editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_TAB, 0, 0)); check(!editor.controlsVisible() && editor.children().isEmpty(), "Tab hides all editor controls without an overlay hint");
                editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_TAB, 0, 0));
                mc.options.guiScale().set(3); mc.resizeGui(); next();
            }
            case 7 -> {
                var editor = (HudEditor) mc.gui.screen();
                check(editor.inspectorBounds().width() == 188 && button("Armor").getHeight() == 22, "HUD editor controls keep their size at GUI scale 3");
                shot("hud-scale-3");
                Neon.applyConfig(original); mc.gui.setScreen(null); clearPlacement();
                mc.options.guiScale().set(2); mc.getWindow().setWindowed(854, 480); mc.resizeGui(); return true;
            }
        }
        return false;
    }
}
