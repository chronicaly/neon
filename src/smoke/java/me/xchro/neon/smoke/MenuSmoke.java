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
        KeyMapping.resetMapping();
        KeyMapping.click(Neon.settings.getDefaultKey());
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
    private Button button(String name) {
        return widgets().stream().filter(w -> w instanceof Button && w.getMessage().getString().equals(name))
            .map(w -> (Button) w).findFirst().orElseThrow(() -> new AssertionError("No button " + name));
    }
    private AbstractScrollArea scroll() {
        return widgets().stream().filter(w -> w instanceof AbstractScrollArea).map(w -> (AbstractScrollArea) w).findFirst().orElseThrow();
    }
    private KeyEvent key(int key) { return new KeyEvent(key, 0, 0); }
    private void shot(String name) { Screenshot.grab(mc.gameDirectory, "neon-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {}); }
    boolean run() {
        age++;
        if (age < 6) return false;
        switch (stage) {
            case 0 -> {
                check(mc.gui.screen() instanceof SettingsScreen, "P opens the menu from gameplay");
                check(mc.options.keySocialInteractions.matches(key(GLFW.GLFW_KEY_P)), "menu opens with vanilla Social Interactions still bound to P");
                check(widgets().stream().noneMatch(w -> w instanceof AbstractSliderButton), "menu opens on overview");
                check(List.of("Camera", "Autoclicker", "HUD", "Controls", "Profiles...", "Done").stream().allMatch(name -> button(name).active), "overview exposes every section and profiles");
                shot("menu-overview"); mc.gui.screen().resize(320, 240); next();
            }
            case 1 -> {
                check(widgets().stream().filter(w -> w instanceof Button).allMatch(w -> w.getX() >= 0 && w.getRight() <= 320
                    && w.getY() >= 0 && w.getBottom() <= 240), "overview actions fit minimum GUI size");
                check(scroll().maxScrollAmount() == 0, "overview needs no scrolling at minimum GUI size");
                shot("menu-overview-small");
                mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_TAB));
                check(mc.gui.screen().getFocused() != null, "overview supports keyboard focus navigation");
                button("Camera").keyPressed(key(GLFW.GLFW_KEY_ENTER));
                check(widgets().stream().anyMatch(w -> w instanceof AbstractSliderButton), "Enter opens a settings section");
                scroll().setScrollAmount(scroll().maxScrollAmount()); double position = scroll().scrollAmount();
                check(position > 0, "small Camera page scrolls");
                // Child screens return at the actual window size. Keep the same viewport for this check.
                mc.gui.screen().resize(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
                scroll().setScrollAmount(scroll().maxScrollAmount()); position = scroll().scrollAmount();
                button("Profiles...").onPress(null); button("Done").onPress(null);
                check(scroll().scrollAmount() == position, "returning from profiles preserves page and scroll");
                var slider = widgets().stream().filter(w -> w instanceof ExactSlider).map(w -> (ExactSlider) w).reduce((a, b) -> b).orElseThrow();
                slider.keyPressed(key(GLFW.GLFW_KEY_ENTER));
                check(mc.gui.screen() instanceof NumberScreen, "keyboard opens exact value entry");
                button("Cancel").onPress(null);
                check(scroll().scrollAmount() == position, "returning from number entry preserves scroll");
                button("Overview").onPress(null); button("HUD").onPress(null); button("Camera").onPress(null);
                check(scroll().scrollAmount() == position, "navigation remembers each section's scroll");
                button("Controls").onPress(null); button("P").onPress(null);
                mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_F10));
                check(Neon.settings.matches(key(GLFW.GLFW_KEY_F10)), "menu key can be rebound");
                button("F10").onPress(null); mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_ESCAPE));
                check(mc.gui.screen() instanceof SettingsScreen && Neon.settings.matches(key(GLFW.GLFW_KEY_F10)), "Escape cancels capture without closing menu");
                button("Reset page").onPress(null);
                check(Neon.settings.matches(key(GLFW.GLFW_KEY_P)), "Controls reset restores P");
                button("Undo").onPress(null);
                check(Neon.settings.matches(key(GLFW.GLFW_KEY_F10)), "Undo restores custom menu key after reset"); next();
            }
            case 2 -> {
                shot("menu-controls"); button("Done").onPress(null);
                KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F10)); next();
            }
            case 3 -> {
                check(mc.gui.screen() instanceof SettingsScreen && Neon.settings.matches(key(GLFW.GLFW_KEY_F10)), "custom binding reopens overview and is preserved");
                mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_ESCAPE));
                check(mc.gui.screen() == null, "Escape returns to gameplay");
                mc.gui.setScreen(new ChatScreen("menu key test - unsent", false));
                KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F10)); next();
            }
            case 4 -> {
                check(mc.gui.screen() instanceof ChatScreen, "menu shortcut does not replace chat");
                mc.gui.setScreen(null); next();
            }
            case 5 -> {
                check(mc.gui.screen() == null, "menu key pressed in chat does not open later");
                for (int i = 0; i < bindings.size(); i++) bindings.get(i).setKey(InputConstants.getKey(previousKeys.get(i)));
                mc.options.keySocialInteractions.setKey(InputConstants.getKey(previousSocialKey));
                KeyMapping.resetMapping(); mc.options.save(); return true;
            }
        }
        return false;
    }
}
