package me.xchro.neon.smoke;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.MenuPlacement;
import me.xchro.neon.core.UiMotion;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Production-client regression checks for folding geometry, clipping and interrupted motion. */
final class ModuleFoldSmoke {
    private final Minecraft mc;
    private SettingsScreen menu;
    private int size, age, checks;
    ModuleFoldSmoke(Minecraft mc, int checks) { this.mc = mc; this.checks = checks; setup(); }
    int checks() { return checks; }
    private void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++; System.out.println("NEON_CHECK " + message);
    }
    private List<AbstractWidget> widgets(ContainerEventHandler parent) {
        var result = new ArrayList<AbstractWidget>();
        for (var child : parent.children()) {
            if (child instanceof AbstractWidget widget) result.add(widget);
            if (child instanceof ContainerEventHandler group) result.addAll(widgets(group));
        }
        return result;
    }
    private UiButton button(String name) {
        return widgets(menu).stream().filter(w -> w instanceof UiButton && w.getMessage().getString().equals(name)).map(w -> (UiButton) w).findFirst().orElseThrow();
    }
    private AbstractWidget fold() {
        return widgets(menu).stream().filter(w -> w.getClass().getSimpleName().equals("UiModuleFold") && w.getMessage().getString().equals("Zoom settings")).findFirst().orElseThrow();
    }
    private AbstractScrollArea scroll() {
        return widgets(menu).stream().filter(w -> w.getClass().getSimpleName().equals("UiScroll")).map(w -> (AbstractScrollArea) w).findFirst().orElseThrow();
    }
    private UiMotion motion(AbstractWidget fold) throws Exception {
        var field = fold.getClass().getDeclaredField("opening"); field.setAccessible(true); return (UiMotion) field.get(fold);
    }
    private void layout() throws Exception {
        var method = scroll().getClass().getDeclaredMethod("relayout"); method.setAccessible(true); method.invoke(scroll());
    }
    private void sample(long millis) throws Exception {
        var started = UiMotion.class.getDeclaredField("started"); started.setAccessible(true);
        started.setLong(motion(fold()), System.nanoTime() - millis * 1_000_000L); layout();
    }
    private MouseButtonEvent pointer(double x, double y) {
        return new MouseButtonEvent(menu.guiCoordinate(x), menu.guiCoordinate(y), new MouseButtonInfo(0, 0));
    }
    private void setup() {
        new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"), Neon.SAVES); Neon.SAVES.flush().join();
        mc.getWindow().setWindowed(1280, 800); mc.options.guiScale().set(1); mc.resizeGui(); mc.setLastInputType(InputType.MOUSE);
        mc.gui.setScreen(menu = Menus.fresh(null));
        if (size == 1) {
            var b = menu.menuBounds(); var start = pointer(b.right() - 1, b.bottom() - 1); var end = pointer(b.right() - 121, b.bottom() - 71);
            menu.mouseClicked(start, false); menu.mouseDragged(end, end.x() - start.x(), end.y() - start.y()); menu.mouseReleased(end);
        }
        age = 0;
    }
    boolean run() throws Exception {
        age++;
        if (age == 8) {
            int sound = SoundAudit.neonEnables, startY = button("Motion blur").getY();
            button("Zoom").onPress(null);
            check(fold().getHeight() < 15, "module starts opening without jumping to its final height");
            sample(35); int intermediate = fold().getHeight();
            check(intermediate > 15 && intermediate < 115, "fold has a visible intermediate height");
            check(button("Motion blur").getY() == startY + intermediate, "following module tracks the animated fold height");
            var last = widgets((ContainerEventHandler) fold()).getLast();
            check(!fold().mouseClicked(new MouseButtonEvent(last.getX() + 3, last.getBottom() - 2, new MouseButtonInfo(0, 0)), false), "unrevealed settings reject pointer input");
            sample(160); int full = fold().getHeight();
            check(full > intermediate && button("Motion blur").getY() == startY + full, "opening finishes at the measured layout height");
            button("Zoom").onPress(null);
            check(((ContainerEventHandler) fold()).children().isEmpty(), "closing settings stop receiving input immediately");
            sample(35); int closing = fold().getHeight();
            check(closing > 0 && closing < full, "collapse retains clipped content and moves sibling rows smoothly");
            button("Zoom").onPress(null);
            check(Math.abs(fold().getHeight() - closing) < 12, "reversing a fold continues from its current height");
            sample(160);
            button("Zoom").onPress(null); sample(160);
            check(fold().getHeight() == 0 && button("Motion blur").getY() == startY, "collapse returns to the exact compact layout");
            button("Zoom").onPress(null); button("Zoom").onPress(null); sample(160);
            check(fold().getHeight() == 0, "two immediate clicks leave no detached opening animation");
            button("Zoom").onPress(null);
            mc.setLastInputType(InputType.KEYBOARD_TAB);
            var container = (ContainerEventHandler) fold();
            var target = container.children().getLast();
            container.setFocused(target); ((ContainerEventHandler) scroll()).setFocused(fold());
            check(fold().getHeight() == full, "keyboard focus reveals opening controls immediately");
            check(((AbstractWidget) target).getBottom() <= scroll().getBottom(), "keyboard focus scrolls the last setting into view");
            mc.setLastInputType(InputType.MOUSE); scroll().setScrollAmount(0);
            menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE, 0, 0)); sample(35);
            check(fold().getHeight() > 0 && fold().getHeight() < full, "Escape uses the same smooth collapse");
            button("Zoom").onPress(null); motion(fold()).snap(.5); layout();
            check(SoundAudit.neonEnables == sound, "fold actions never request enable cues");
        }
        if (age == 12) Screenshot.grab(mc.gameDirectory, "neon-module-fold-" + (size == 0 ? "default" : "minimum") + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {});
        if (age < 16) return false;
        if (++size < 2) { setup(); return false; }
        mc.gui.setScreen(null); return true;
    }
}
