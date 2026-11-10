package me.xchro.neon.smoke;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/** Menu navigation and input checks in the disposable production client only. */
final class MenuSmoke {
    private final Minecraft mc;
    private final List<KeyMapping> bindings = List.of(Neon.settings, Neon.freelook, Neon.zoom, Neon.autoclicker);
    private final List<String> previousKeys = bindings.stream().map(KeyMapping::saveString).toList();
    private final String previousSocialKey;
    private int stage, age, checks;
    MenuSmoke(Minecraft mc, int checks) {
        this.mc = mc; this.checks = checks;
        previousSocialKey = mc.options.keySocialInteractions.saveString();
        check(Neon.settings.getDefaultKey().getValue() == GLFW.GLFW_KEY_P, "menu defaults to P");
        check(bindings.stream().skip(1).allMatch(key -> key.getDefaultKey().equals(InputConstants.UNKNOWN)), "feature bindings remain opt-in");
        Neon.settings.setKey(Neon.settings.getDefaultKey());
        mc.options.keySocialInteractions.setKey(mc.options.keySocialInteractions.getDefaultKey());
        Neon.freelook.setKey(InputConstants.UNKNOWN);
        KeyMapping.resetMapping(); KeyMapping.click(Neon.settings.getDefaultKey());
    }
    int checks() { return checks; }
    private void check(boolean condition, String name) {
        if (!condition) throw new AssertionError("Menu stage " + stage + ": " + name);
        checks++; System.out.println("NEON_CHECK " + name);
    }
    private void next() { stage++; age = 0; }
    private List<AbstractWidget> widgets(ContainerEventHandler parent) {
        List<AbstractWidget> result = new ArrayList<>();
        for (var child : parent.children()) {
            if (child instanceof AbstractWidget widget) result.add(widget);
            if (child instanceof ContainerEventHandler container) result.addAll(widgets(container));
        }
        return result;
    }
    private List<AbstractWidget> widgets() { return widgets(mc.gui.screen()); }
    private Button button(String name) { return widgets().stream().filter(w -> w instanceof Button && w.getMessage().getString().equals(name)).map(w -> (Button) w).findFirst().orElseThrow(() -> new AssertionError("No button " + name)); }
    private AbstractScrollArea scroll() { return widgets().stream().filter(w -> w instanceof AbstractScrollArea).map(w -> (AbstractScrollArea) w).findFirst().orElseThrow(); }
    private KeyEvent key(int key) { return new KeyEvent(key, 0, 0); }
    private void undo() { mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z, 0, GLFW.GLFW_MOD_CONTROL)); }
    private void closeScreen() {
        var before = mc.gui.screen(); before.keyPressed(key(GLFW.GLFW_KEY_ESCAPE));
        if (mc.gui.screen() == before) before.keyPressed(key(GLFW.GLFW_KEY_ESCAPE));
    }
    private void shot(String name) { Screenshot.grab(mc.gameDirectory, "neon-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {}); }
    boolean run() {
        age++;
        if (age < 6) return false;
        switch (stage) {
            case 0 -> {
                check(mc.gui.screen() instanceof SettingsScreen, "P opens the menu from gameplay");
                check(mc.options.keySocialInteractions.matches(key(GLFW.GLFW_KEY_P)), "menu opens with vanilla Social Interactions still bound to P");
                check(widgets().stream().noneMatch(w -> w instanceof AbstractSliderButton), "menu opens on compact module cards");
                check(List.of("Combat", "Visuals", "HUD", "Settings", "Freelook", "Zoom", "Motion blur").stream().allMatch(name -> button(name).active), "categories contain module cards");
                check(widgets().stream().noneMatch(w -> Set.of("Undo", "Done", "×").contains(w.getMessage().getString())), "menu has no close or footer buttons");
                check(widgets().stream().noneMatch(w -> w.getMessage().getString().contains("Overview")), "Overview is removed");
                shot("menu-cards"); mc.gui.screen().resize(320 / mc.getWindow().getGuiScale(), 240 / mc.getWindow().getGuiScale()); next();
            }
            case 1 -> {
                check(widgets().stream().filter(w -> w instanceof Button).allMatch(w -> w.getX() >= 0 && w.getRight() <= 320 && w.getY() >= 0 && w.getBottom() <= 240), "card actions fit minimum GUI size");
                check(scroll().maxScrollAmount() == 0, "three module cards fit without scrolling at minimum size");
                shot("menu-cards-small"); mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_TAB));
                check(mc.gui.screen().getFocused() != null, "menu supports keyboard focus navigation");
                button("Zoom").keyPressed(key(GLFW.GLFW_KEY_ENTER));
                check(widgets().stream().anyMatch(w -> w instanceof AbstractSliderButton), "Enter opens focused module settings");
                scroll().setScrollAmount(scroll().maxScrollAmount());
                check(scroll().scrollAmount() > 0, "small module page scrolls");
                mc.gui.screen().resize(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
                scroll().setScrollAmount(scroll().maxScrollAmount()); double position = scroll().scrollAmount();
                button("Settings").onPress(null); button("Profiles").onPress(null); button("Visuals").onPress(null); button("Zoom").onPress(null);
                check(scroll().scrollAmount() == position, "returning from profiles preserves module scroll");
                var slider = widgets().stream().filter(w -> w instanceof ExactSlider).map(w -> (ExactSlider) w).reduce((a, b) -> b).orElseThrow();
                slider.keyPressed(key(GLFW.GLFW_KEY_ENTER)); check(slider.isEditing() && mc.gui.screen() instanceof SettingsScreen, "keyboard edits exact values inline");
                slider.keyPressed(key(GLFW.GLFW_KEY_ESCAPE)); check(scroll().scrollAmount() == position, "canceling inline number entry preserves scroll");
                button("HUD").onPress(null); mc.gui.screen().onClose(); button("Visuals").onPress(null); button("Zoom").onPress(null);
                check(scroll().scrollAmount() == position, "navigation remembers module scroll");
                button("Visuals").onPress(null); button("Freelook").onPress(null); button("[—]").onPress(null); mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_F9));
                check(Neon.freelook.matches(key(GLFW.GLFW_KEY_F9)), "inline module bind captures keys");
                button("[F9]").onPress(null); mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_ESCAPE));
                check(mc.gui.screen() instanceof SettingsScreen && Neon.freelook.matches(key(GLFW.GLFW_KEY_F9)), "Escape cancels capture without closing menu");
                undo(); check(Neon.freelook.isUnbound(), "Undo restores previous module binding");
                // The global menu key is managed by Minecraft's key bindings, outside Appearance/Profiles.
                Neon.settings.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F10)); KeyMapping.resetMapping(); mc.options.save(); next();
            }
            case 2 -> { closeScreen(); KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F10)); next(); }
            case 3 -> {
                check(mc.gui.screen() instanceof SettingsScreen && Neon.settings.matches(key(GLFW.GLFW_KEY_F10)), "custom global binding reopens menu and is preserved");
                mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_ESCAPE)); check(mc.gui.screen() == null, "Escape returns to gameplay");
                mc.gui.setScreen(new ChatScreen("menu key test - unsent", false)); KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F10)); next();
            }
            case 4 -> { check(mc.gui.screen() instanceof ChatScreen, "menu shortcut does not replace chat"); mc.gui.setScreen(null); next(); }
            case 5 -> {
                check(mc.gui.screen() == null, "menu key pressed in chat does not open later");
                for (int i = 0; i < bindings.size(); i++) bindings.get(i).setKey(InputConstants.getKey(previousKeys.get(i)));
                mc.options.keySocialInteractions.setKey(InputConstants.getKey(previousSocialKey)); KeyMapping.resetMapping(); mc.options.save(); return true;
            }
        }
        return false;
    }
}
