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

/** Interaction checks for the custom interface and the live HUD canvas. */
final class DesignSmoke {
    private final Minecraft mc;
    private final NeonConfig original;
    private int stage, age, checks, essentialCount, colorPhase, colorSounds;
    private volatile int[] borderBefore, borderAfter;
    private boolean captureAfter;
    DesignSmoke(Minecraft mc, int checks) {
        this.mc = mc; this.checks = checks; original = ConfigStore.copy(Neon.config);
        mc.options.guiScale().set(2); mc.resizeGui();
        mc.getWindow().setWindowed(1280, 800); mc.resizeGui(); mc.gui.setScreen(new SettingsScreen(null));
    }
    int checks() { return checks; }
    private void check(boolean ok, String message) {
        if (!ok) throw new AssertionError("Design stage " + stage + ": " + message);
        checks++; System.out.println("NEON_CHECK " + message);
    }
    private void next() { stage++; age = 0; }
    private List<AbstractWidget> widgets(ContainerEventHandler parent) {
        List<AbstractWidget> list = new ArrayList<>();
        for (var child : parent.children()) {
            if (child instanceof AbstractWidget widget) list.add(widget);
            if (child instanceof ContainerEventHandler container) list.addAll(widgets(container));
        }
        return list;
    }
    private List<AbstractWidget> widgets() { return widgets(mc.gui.screen()); }
    private Button button(String name) { return widgets().stream().filter(w -> w instanceof Button && w.getMessage().getString().equals(name)).map(w -> (Button) w).findFirst().orElseThrow(() -> new AssertionError("No button " + name)); }
    private AbstractScrollArea scroll() { return widgets().stream().filter(w -> w instanceof AbstractScrollArea).map(w -> (AbstractScrollArea) w).findFirst().orElseThrow(); }
    private void shot(String name) { Screenshot.grab(mc.gameDirectory, "neon-design-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {}); }
    private void captureBorder(boolean after) {
        float scale = PixelScreen.UI_SCALE;
        var bounds = ((SettingsScreen) mc.gui.screen()).menuBounds();
        int w = bounds.width(), h = bounds.height(), x = Math.round(bounds.left() * scale), y = Math.round(bounds.top() * scale);
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                int[] colors = new int[2 * (w + h)]; int i = 0;
                for (int px = 0; px < w; px++) { colors[i++] = image.getPixel(x + Math.round(px * scale), y); colors[i++] = image.getPixel(x + Math.round(px * scale), y + Math.round((h - 1) * scale)); }
                for (int py = 0; py < h; py++) { colors[i++] = image.getPixel(x, y + Math.round(py * scale)); colors[i++] = image.getPixel(x + Math.round((w - 1) * scale), y + Math.round(py * scale)); }
                if (after) borderAfter = colors; else borderBefore = colors;
            }
        });
    }
    private MouseButtonEvent mouse(double x, double y) { return new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)); }
    private void key(int key) { mc.gui.screen().keyPressed(new KeyEvent(key, 0, 0)); }
    private void undo() { mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z, 0, GLFW.GLFW_MOD_CONTROL)); }
    private void closeScreen() {
        var before = mc.gui.screen(); key(GLFW.GLFW_KEY_ESCAPE);
        if (mc.gui.screen() == before) key(GLFW.GLFW_KEY_ESCAPE);
    }
    private HudBounds armor() { return NeonHud.bounds(mc.gui.screen().width, mc.gui.screen().height, true); }
    private HudBounds totem() { return NeonHud.totemBounds(mc.gui.screen().width, mc.gui.screen().height); }
    private void gesture(double x, double y, double toX, double toY) {
        var editor = mc.gui.screen();
        if (editor instanceof SettingsScreen settings) { x = settings.guiCoordinate(x); y = settings.guiCoordinate(y); toX = settings.guiCoordinate(toX); toY = settings.guiCoordinate(toY); }
        check(editor.mouseClicked(mouse(x, y), false), "canvas accepts gesture");
        editor.mouseDragged(mouse(toX, toY), toX - x, toY - y); editor.mouseReleased(mouse(toX, toY));
    }
    private void expand(String name) {
        var b = button(name); b.mouseClicked(new MouseButtonEvent(b.getX() + 5, b.getY() + 5, new MouseButtonInfo(1, 0)), false);
    }
    boolean run() {
        age++;
        if (stage == 0) {
            if (age == 8) captureBorder(false);
            if (age >= 24 && !captureAfter && borderBefore != null) { captureAfter = true; captureBorder(true); }
            if (age > 100) throw new AssertionError("Timed out reading animated menu border");
            if (borderAfter == null) return false;
        }
        if (age < 10) return false;
        switch (stage) {
            case 0 -> {
                int changed = 0; for (int i = 0; i < borderBefore.length; i++) if (borderBefore[i] != borderAfter[i]) changed++;
                check(changed > 30, "real rendered neon outline moves between frames");
                check(mc.gui.screen().width >= 600, "menu adapts to a larger window (GUI=" + mc.gui.screen().width + ", display=" + mc.getWindow().getWidth() + ", scale=" + mc.getWindow().getGuiScale() + ")");
                check(widgets().stream().filter(w -> w instanceof Button).allMatch(w -> w instanceof UiButton), "all menu actions use the custom controls");
                check(List.of("Combat", "Visuals", "HUD", "Settings").stream().map(this::button).map(AbstractWidget::getX).distinct().count() == 1, "sidebar contains categories");
                shot("cards"); button("Visuals").onPress(null); button("Zoom").onPress(null); next();
            }
            case 1 -> {
                essentialCount = widgets().size();
                check(widgets().stream().filter(w -> w instanceof ExactSlider).count() == 2, "zoom exposes magnification and transition directly");
                shot("camera");
                check(widgets().stream().noneMatch(w -> Set.of("Advanced", "Less options").contains(w.getMessage().getString())), "modules have no Advanced folds");
                scroll().setScrollAmount(scroll().maxScrollAmount()); next();
            }
            case 2 -> {
                shot("zoom-options");
                check(widgets().size() == essentialCount && button("Reset").getBottom() <= scroll().getBottom(), "all direct zoom options are reachable by scrolling");
                button("HUD").onPress(null);
                check(mc.gui.screen() instanceof HudEditor, "HUD category opens the editor directly"); next();
            }
            case 3 -> {
                shot("hud-module"); next();
            }
            case 4 -> {
                shot("editor");
                check(mc.gui.screen() instanceof HudEditor && button("Armor").active && button("Totems").active, "editor lists only armor and totems");
                Neon.config.editorSnap = false;
                expand("Armor"); key(GLFW.GLFW_KEY_H);
                HudBounds initial = armor();
                gesture(initial.x() + initial.width() / 2.0, initial.y() + initial.height() / 2.0, mc.gui.screen().width / 2.0, mc.gui.screen().height / 2.0);
                HudBounds b = armor(); double scale = Neon.config.armor.scale;
                gesture(b.x() + b.width() - 1, b.y() + b.height() - 1, b.x() + b.width() + 19, b.y() + b.height() + 9);
                check(Neon.config.armor.scale > scale && armor().x() == b.x() && armor().y() == b.y(), "bottom-right resize fixes the opposite corner");
                undo(); check(armor().equals(b) && Neon.config.armor.scale == scale, "one Undo restores a complete resize");
                var fixedPixels = NeonHud.pixelBounds(mc.getWindow().getWidth(),mc.getWindow().getHeight(),true);
                gesture(b.x(), b.y(), b.x() - 15, b.y() - 10);
                var resizedPixels = NeonHud.pixelBounds(mc.getWindow().getWidth(),mc.getWindow().getHeight(),true);
                check(resizedPixels.x()+resizedPixels.width()==fixedPixels.x()+fixedPixels.width() && resizedPixels.y()+resizedPixels.height()==fixedPixels.y()+fixedPixels.height(), "top-left resize fixes the opposite physical-pixel corner");
                undo();
                gesture(b.x() + b.width() / 2.0, b.y() + b.height() / 2.0, b.x() + b.width() / 2.0 + 36, b.y() + b.height() / 2.0 - 18);
                check(armor().x() == b.x() + 36 && armor().y() == b.y() - 18, "canvas drag moves the selected widget");
                undo(); int oldPixelX = NeonHud.pixelBounds(mc.getWindow().getWidth(),mc.getWindow().getHeight(),true).x();
                key(GLFW.GLFW_KEY_RIGHT); check(NeonHud.pixelBounds(mc.getWindow().getWidth(),mc.getWindow().getHeight(),true).x() == oldPixelX + 1, "arrow keys nudge selected widget by one display pixel");
                HudBounds nudged = armor();
                mc.gui.screen().mouseClicked(mouse(nudged.x() + nudged.width() / 2.0, nudged.y() + nudged.height() / 2.0), false);
                key(GLFW.GLFW_KEY_ESCAPE);
                check(armor().equals(nudged), "canceling an unmoved gesture preserves the previous edit");
                var start = mouse(nudged.x() + nudged.width() / 2.0, nudged.y() + nudged.height() / 2.0);
                mc.gui.screen().mouseClicked(start, false);
                mc.gui.screen().mouseDragged(mouse(start.x() + 12, start.y() + 12), 12, 12);
                key(GLFW.GLFW_KEY_ESCAPE);
                check(armor().equals(nudged), "Escape restores the start of a changed gesture");
                undo(); check(armor().equals(b), "gesture cancellation preserves earlier Undo history");
                key(GLFW.GLFW_KEY_H); next();
            }
            case 5 -> {
                int x = button("Armor").getX(), y = button("Armor").getY();
                var editor = (HudEditor) mc.gui.screen();
                gesture(editor.guiCoordinate(x + 20), editor.guiCoordinate(y - 17), editor.guiCoordinate(x + 100), editor.guiCoordinate(y - 17));
                check(button("Armor").getX() == x + 80, "inspector can be dragged away from widgets");
                expand("Totems"); key(GLFW.GLFW_KEY_H);
                HudBounds offhand = totem();
                gesture(offhand.x() + 10, offhand.y() + 10, mc.gui.screen().width / 2.0, mc.gui.screen().height / 2.0);
                check(Neon.config.totemFloating && NeonHud.offhandCount(mc.player.getOffhandItem()) == null, "floating totems replace the offhand decoration");
                HudBounds b = totem();
                gesture(b.x() + 10, b.y() + 10, b.x() - 50, b.y() + 42);
                check(totem().x() == b.x() - 60 && totem().y() == b.y() + 32, "totem widget drags independently");
                HudBounds moved = totem(); double scale = Neon.config.totems.scale;
                gesture(moved.x() + moved.width() - 1, moved.y() + moved.height() - 1, moved.x() + moved.width() + 9, moved.y() + moved.height() + 9);
                check(Neon.config.totems.scale > scale, "totem widget supports corner resizing");
                check(Neon.save(), "editor layout saves"); Neon.SAVES.flush().join();
                try {
                    NeonConfig saved = new ConfigStore(mc.gameDirectory.toPath().resolve("config/neon.json")).load();
                    check(saved.totemFloating && saved.totems.x == Neon.config.totems.x && saved.totems.scale == Neon.config.totems.scale, "floating mode, position and size persist");
                } catch (Exception e) { throw new AssertionError(e); }
                key(GLFW.GLFW_KEY_H); next();
            }
            case 6 -> {
                shot("editor-totems");
                Neon.config.totemFloating = false;
                check(!Neon.config.totemFloating && NeonHud.offhandCount(mc.player.getOffhandItem()) != null, "totems can return to the offhand");
                mc.gui.screen().resize(320 / mc.getWindow().getGuiScale(), 240 / mc.getWindow().getGuiScale());
                check(widgets().stream().filter(w -> w instanceof AbstractScrollArea).allMatch(w -> w.getX() >= 0 && w.getRight() <= 320 && w.getY() >= 0 && w.getBottom() <= 240), "editor inspector fits minimum viewport");
                Neon.applyConfig(original); mc.gui.setScreen(null); mc.getWindow().setWindowed(854, 480); next();
            }
            case 7 -> {
                check(mc.gui.screen() == null, "editor verification restores gameplay");
                mc.getWindow().setWindowed(1280, 800); mc.options.guiScale().set(1); mc.resizeGui();
                try { new me.xchro.neon.config.MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"), Neon.SAVES); Neon.SAVES.flush().join(); } catch (Exception e) { throw new AssertionError(e); }
                mc.gui.setScreen(new SettingsScreen(null)); next();
            }
            case 8 -> {
                var menu = (SettingsScreen) mc.gui.screen(); var b = menu.menuBounds();
                check(b.width() == 440 && b.height() == 280, "default menu stays compact in display pixels");
                check(widgets().stream().noneMatch(w -> w.getMessage().getString().contains("Overview")), "no overview or promotional copy");
                shot("reference-visuals");
                gesture(b.left() + 40, b.top() + 20, b.left() + 80, b.top() + 46);
                var moved = menu.menuBounds();
                check(moved.left() == b.left() + 40 && moved.top() == b.top() + 26, "brand/header moves the entire menu");
                for (int corner = 0; corner < 4; corner++) {
                    var before = menu.menuBounds(); boolean west = corner % 2 == 0, north = corner < 2;
                    int x = west ? before.left() : before.right(), y = north ? before.top() : before.bottom();
                    gesture(x, y, x + (west ? -12 : 12), y + (north ? -9 : 9));
                    var after = menu.menuBounds();
                    check(after.width() == before.width() + 12 && after.height() == before.height() + 9
                        && (west ? after.right() == before.right() : after.left() == before.left())
                        && (north ? after.bottom() == before.bottom() : after.top() == before.top()), "invisible corner " + corner + " resizes with opposite corner fixed");
                }
                var resized = menu.menuBounds();
                check(menu.mouseClicked(mouse(menu.guiCoordinate(resized.left() + 40), menu.guiCoordinate(resized.top() + 20)), false), "header begins a cancellable movement");
                menu.mouseDragged(mouse(10000, 10000), 0, 0); key(GLFW.GLFW_KEY_ESCAPE);
                check(menu.menuBounds().equals(resized), "Escape cancels menu movement");
                closeScreen(); mc.gui.setScreen(new SettingsScreen(null));
                check(((SettingsScreen) mc.gui.screen()).menuBounds().equals(resized), "menu placement persists across reopening");
                button("Settings").onPress(null);
                check(button("Appearance").active && button("Profiles").active && widgets().stream().noneMatch(w -> w.getMessage().getString().startsWith("[")), "Settings merges appearance and profiles without a bind list");
                next();
            }
            case 9 -> {
                shot("settings-cards"); button("Appearance").onPress(null); next();
            }
            case 10 -> {
                colorSounds=SoundAudit.neonClicks;
                shot("appearance"); button("Background").onPress(null); next();
            }
            case 11 -> {
                check(mc.gui.screen() instanceof SettingsScreen, "color picker expands inside Appearance without replacing the menu");
                var picker = (SettingsScreen) mc.gui.screen();
                if (colorPhase == 0) {
                shot("color-picker");
                String clipboard = mc.keyboardHandler.getClipboard();
                try {
                    mc.keyboardHandler.setClipboard("#162233CC"); button("Paste").onPress(null);
                    check(Neon.config.theme.background == 0xcc162233 && Ui.PANEL == Ui.alpha(0xcc162233,(int)Math.round(204*Neon.config.theme.opacity)), "Unity paste applies RGBA immediately including Neon opacity");
                    mc.keyboardHandler.setClipboard("invalid"); button("Paste").onPress(null);
                    check(Neon.config.theme.background == 0xcc162233, "invalid paste leaves the live color intact");
                    button("Copy").onPress(null);
                    check(mc.keyboardHandler.getClipboard().equals("#162233CC"), "Unity copy uses RGBA hex");
                } finally { mc.keyboardHandler.setClipboard(clipboard); }
                key(GLFW.GLFW_KEY_ESCAPE);
                colorPhase++; age = 0; return false;
                }
                if (colorPhase == 1) {
                check(widgets().stream().noneMatch(w -> w.getMessage().getString().equals("Color picker")), "Escape collapses the picker and keeps Appearance open");
                check(widgets().stream().noneMatch(w -> Set.of("Apply", "Cancel").contains(w.getMessage().getString())), "inline color editing has no dialog actions");
                check(Neon.save(), "appearance saves"); Neon.SAVES.flush().join();
                try { check(new ConfigStore(mc.gameDirectory.toPath().resolve("config/neon.json")).load().theme.background == 0xcc162233, "appearance and opacity survive configuration reload"); } catch (Exception e) { throw new AssertionError(e); }
                undo(); check(Neon.config.theme.background == original.theme.background, "one Undo restores color");
                button("Outline").onPress(null);
                colorPhase++; age = 0; return false;
                }
                if (colorPhase == 2) {
                var shade = widgets().stream().filter(w -> w.getMessage().getString().equals("Saturation and brightness")).findFirst().orElseThrow();
                var colorMenu = (SettingsScreen) mc.gui.screen();
                var start = mouse(colorMenu.guiCoordinate(shade.getX() + 12), colorMenu.guiCoordinate(shade.getY() + 12));
                var end = mouse(colorMenu.guiCoordinate(shade.getRight() - 12), colorMenu.guiCoordinate(shade.getBottom() - 12));
                check(colorMenu.mouseClicked(start, false), "inline gradient accepts the real screen pointer");
                colorMenu.mouseDragged(end, end.x() - start.x(), end.y() - start.y());
                check(Neon.config.theme.outline != original.theme.outline, "color changes while dragging before release");
                colorMenu.mouseReleased(end); undo();
                check(Neon.config.theme.outline == original.theme.outline, "one Undo restores the entire inline color drag");
                var row = button("Outline");
                var right = new MouseButtonEvent(picker.guiCoordinate(row.getX()+6),picker.guiCoordinate(row.getY()+6),new MouseButtonInfo(1,0));
                check(picker.mouseClicked(right,false), "right click on the entire Unity color row collapses its fold");
                colorPhase++; age = 0; return false;
                }
                check(widgets().stream().noneMatch(w -> w.getMessage().getString().equals("Color picker")), "color row closes inline after its short fold animation");
                check(SoundAudit.neonClicks==colorSounds,"opening, dragging, pasting, copying and closing colors are silent");
                var before = ((SettingsScreen) mc.gui.screen()).menuBounds();
                button("Settings").onPress(null); button("Profiles").onPress(null);
                String profile = "Design verification";
                widgets().stream().filter(w -> w instanceof EditBox).map(w -> (EditBox) w).findFirst().orElseThrow().setValue(profile);
                button("Save").onPress(null);
                Neon.config.theme.accent = 0xffbb7799; button(profile).onPress(null);
                check(Neon.config.theme.accent == original.theme.accent && ((SettingsScreen) mc.gui.screen()).menuBounds().equals(before), "profiles restore appearance without moving the menu");
                try { Neon.profiles.delete(profile); } catch (Exception e) { throw new AssertionError(e); }
                button("Visuals").onPress(null); button("Freelook").onPress(null);
                var bind = widgets().stream().filter(w -> w instanceof Button && w.getMessage().getString().startsWith("[")).map(w -> (Button) w).findFirst().orElseThrow();
                bind.onPress(null); mc.gui.screen().mouseClicked(new MouseButtonEvent(0, 0, new MouseButtonInfo(4, 0)), false);
                button("Visuals").onPress(null);
                check(button("[M5]").getWidth() < 36 && button("[M5]").getY() == button("Freelook").getY(), "mouse bind stays compact and inline with the module");
                undo();
                button("Combat").onPress(null);
                widgets().stream().filter(w -> w instanceof Button && w.getMessage().getString().startsWith("[")).map(w -> (Button) w).findFirst().orElseThrow().onPress(null);
                mc.gui.screen().mouseClicked(new MouseButtonEvent(0, 0, new MouseButtonInfo(4, 0)), false);
                mc.options.guiScale().set(2); mc.resizeGui(); next();
            }
            case 12 -> {
                check(button("[M5]").getY() == button("Autoclicker").getY() && button("[M5]").getWidth() < 36, "Autoclicker mouse bind uses one compact card row");
                shot("autoclicker-inline"); undo();
                mc.gui.screen().resize(320 / mc.getWindow().getGuiScale(), 240 / mc.getWindow().getGuiScale());
                var menu = (SettingsScreen) mc.gui.screen(); var b = menu.menuBounds();
                gesture(b.left() + 40, b.top() + 20, -1000, -1000);
                check(menu.menuBounds().left() == 14 && menu.menuBounds().top() == 14, "menu movement clamps to reachable viewport edges");
                check(button("Settings").getBottom() <= 240 && scroll().getBottom() <= 240, "categories and content stay reachable at minimum viewport");
                Neon.applyConfig(original); mc.gui.setScreen(null);
                try { new me.xchro.neon.config.MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"), Neon.SAVES); Neon.SAVES.flush().join(); } catch (Exception e) { throw new AssertionError(e); }
                mc.options.guiScale().set(2); mc.getWindow().setWindowed(854, 480); mc.resizeGui(); return true;
            }
        }
        return false;
    }
}
