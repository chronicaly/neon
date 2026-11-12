package me.xchro.neon.smoke;

import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.core.BindMode;
import me.xchro.neon.module.ClientModule;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.*;
import org.lwjgl.glfw.GLFW;

final class BetterScreenshotSmoke {
    private final Minecraft mc;
    private final NeonConfig original = ConfigStore.copy(Neon.config);
    private final String binding = Neon.betterScreenshot.saveString();
    private final int soundStart = SoundAudit.cues.size();
    private volatile Component saved;
    private Path png;
    private SettingsScreen menu;
    private int stage, age, checks;
    BetterScreenshotSmoke(Minecraft mc, int checks) {
        this.mc = mc; this.checks = checks;
        Neon.config.betterScreenshot = new NeonConfig.BetterScreenshot();
        Neon.betterScreenshot.setKey(InputConstants.UNKNOWN); KeyMapping.resetMapping();
        mc.gui.hud.getChat().clearMessages(false);
    }
    int checks() { return checks; }
    private void check(boolean ok, String message) {
        if (!ok) throw new AssertionError("Screenshot " + stage + ": " + message);
        checks++; System.out.println("NEON_CHECK " + message);
    }
    private void shot(String name) throws Exception {
        saved = null; png = mc.gameDirectory.toPath().resolve("screenshots/neon-screenshot-" + name + ".png");
        Files.deleteIfExists(png);
        Screenshot.grab(mc.gameDirectory, png.getFileName().toString(), mc.gameRenderer.mainRenderTarget(), 1, result -> saved = result);
    }
    private void png(boolean copied) throws Exception {
        check(saved != null && Files.exists(png), "vanilla PNG save completed");
        boolean[] linked = {false};
        saved.visit((style, text) -> { linked[0] |= style.getClickEvent() instanceof ClickEvent.OpenFile; return Optional.empty(); }, Style.EMPTY);
        check(linked[0], "vanilla screenshot result retains its clickable file link");
        if (!copied) return;
        var actual = ImageIO.read(png.toFile()); var copy = ClipboardAudit.latest;
        check(copy.width() == actual.getWidth() && copy.height() == actual.getHeight(), "clipboard dimensions match saved PNG");
        int[] pixels = actual.getRGB(0, 0, actual.getWidth(), actual.getHeight(), null, 0, actual.getWidth());
        boolean same = true;
        for (int i = 0; i < pixels.length; i++) same &= (pixels[i] & 0xffffff) == (copy.pixels()[i] & 0xffffff);
        check(same, "every clipboard pixel matches PNG orientation and RGB channels");
    }
    @SuppressWarnings("unchecked") private long notices(String text) throws Exception {
        var chat = mc.gui.hud.getChat(); var field = chat.getClass().getDeclaredField("allMessages"); field.setAccessible(true);
        return ((List<GuiMessage>)field.get(chat)).stream().filter(m -> m.content().getString().contains(text)).count();
    }
    private List<AbstractWidget> widgets(ContainerEventHandler root) {
        var result = new ArrayList<AbstractWidget>();
        for (var child : root.children()) { if (child instanceof AbstractWidget w) result.add(w); if (child instanceof ContainerEventHandler c) result.addAll(widgets(c)); }
        return result;
    }
    private UiButton notifyButton() {
        return widgets(menu).stream().filter(w -> w instanceof UiButton b && b.hint().equals("Notify")).map(w -> (UiButton)w).findFirst().orElseThrow();
    }
    private MouseButtonEvent pointer(double x, double y, int button) {
        return new MouseButtonEvent(menu.guiCoordinate(x), menu.guiCoordinate(y), new MouseButtonInfo(button, 0));
    }
    private void setup(boolean minimum, String section) {
        new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"), Neon.SAVES); Neon.SAVES.flush().join();
        mc.getWindow().setWindowed(1280, 800); mc.options.guiScale().set(1); mc.resizeGui(); mc.setLastInputType(InputType.MOUSE);
        mc.gui.setScreen(menu = Menus.fresh(null));
        if (minimum) {
            var b = menu.menuBounds(); var from = pointer(b.right()-1, b.bottom()-1, 0); var to = pointer(b.right()-121, b.bottom()-71, 0);
            menu.mouseClicked(from, false); menu.mouseDragged(to, 0, 0); menu.mouseReleased(to);
        }
        if (section.equals("Appearance")) widgets(menu).stream().filter(w -> w instanceof Button && w.getMessage().getString().equals("Settings")).map(w -> (Button)w).findFirst().orElseThrow().onPress(null);
        Menus.configure(menu, section);
        if (minimum && section.equals("Better screenshot")) {
            var grid = (AbstractScrollArea)widgets(menu).stream().filter(w -> w.getClass().getSimpleName().equals("UiScroll")).findFirst().orElseThrow();
            grid.setScrollAmount(grid.maxScrollAmount());
        }
    }
    private void geometry() {
        var b = menu.expandedBounds(); check(b.left() >= 0 && b.right() <= menu.canvasWidth() && b.bottom() <= menu.canvasHeight(), "module/settings shell fits display");
        var detail = widgets(menu).stream().filter(w -> w.getClass().getSimpleName().equals("UiScroll")).skip(1).findFirst().orElseThrow();
        for (var w : widgets((ContainerEventHandler)detail)) check(w.getX() >= detail.getX() && w.getRight() <= detail.getRight(), "setting fits drawer: " + w.getMessage().getString());
        var tiles = widgets(menu).stream().filter(w -> w.getClass().getSimpleName().equals("UiModuleTile")).toList();
        check(tiles.stream().map(AbstractWidget::getWidth).distinct().count() == 1, "module/settings buttons retain equal widths");
        if (menu.menuBounds().width() == 440) check(tiles.stream().map(AbstractWidget::getX).distinct().count() == 2, "default grid retains two compact columns");
        for (var tile : tiles) check(tile.getHeight() == 26, "module/settings button keeps single-line compact height");
    }
    boolean run() throws Exception {
        if (++age < 20) return false;
        if (stage >= 1 && saved == null) { check(age < 200, "screenshot writer completes in time"); return false; }
        age = 0;
        switch (stage++) {
            case 0 -> shot("disabled");
            case 1 -> {
                png(false); check(ClipboardAudit.calls == 0, "disabled module leaves clipboard untouched");
                Neon.config.betterScreenshot.enabled = true; shot("enabled");
            }
            case 2 -> {
                png(true); check(ClipboardAudit.calls == 1, "one clipboard copy per screenshot");
                check(notices("Screenshot copied to clipboard") == 1, "one local success notice");
                Neon.config.betterScreenshot.notify = false; shot("quiet");
            }
            case 3 -> {
                png(true); check(ClipboardAudit.calls == 2, "Notify off still copies screenshot");
                check(notices("Screenshot copied to clipboard") == 1, "Notify off suppresses only success notice");
                ClipboardAudit.fail = true; shot("failure");
            }
            case 4 -> {
                png(true); check(notices("Could not copy screenshot") == 1, "clipboard failure reports one local error and preserves PNG");
                check(SoundAudit.cues.size() == soundStart, "screenshot notices never emit chat or UI pings"); ClipboardAudit.fail = false;
                Neon.config.betterScreenshot.mode = BindMode.HOLD;
                Neon.betterScreenshot.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F8)); KeyMapping.resetMapping();
                Neon.betterScreenshot.setDown(false); Neon.MODULE_BINDINGS.tick(mc); check(!Neon.MODULE_BINDINGS.active(ClientModule.BETTER_SCREENSHOT), "Hold bind is inactive while released");
                Neon.betterScreenshot.setDown(true); Neon.MODULE_BINDINGS.tick(mc); check(Neon.MODULE_BINDINGS.active(ClientModule.BETTER_SCREENSHOT), "Hold bind activates screenshot copying");
                Neon.config.betterScreenshot.mode = BindMode.REVERSE_HOLD; Neon.MODULE_BINDINGS.tick(mc); check(!Neon.MODULE_BINDINGS.active(ClientModule.BETTER_SCREENSHOT), "Reverse hold suppresses copying while held");
                Neon.betterScreenshot.setDown(false); Neon.MODULE_BINDINGS.tick(mc); check(Neon.MODULE_BINDINGS.active(ClientModule.BETTER_SCREENSHOT), "Reverse hold restores copying on release");
                Neon.config.betterScreenshot.mode = BindMode.TOGGLE; Neon.MODULE_BINDINGS.reset();
                Neon.betterScreenshot.setDown(false); Neon.MODULE_BINDINGS.tick(mc);
                Neon.betterScreenshot.setDown(true); Neon.MODULE_BINDINGS.tick(mc); check(!Neon.config.betterScreenshot.enabled, "Toggle bind disables module");
                Neon.betterScreenshot.setDown(false); Neon.MODULE_BINDINGS.tick(mc); Neon.betterScreenshot.setDown(true); Neon.MODULE_BINDINGS.tick(mc); check(Neon.config.betterScreenshot.enabled, "Toggle bind reenables module");
                Neon.betterScreenshot.setDown(false); Neon.betterScreenshot.setKey(InputConstants.UNKNOWN); KeyMapping.resetMapping();
                Neon.config.betterScreenshot.notify = true; setup(false, "Better screenshot");
            }
            case 5 -> { geometry(); shot("module-default"); }
            case 6 -> {
                var button = notifyButton(); var event = pointer(button.getX()+button.getWidth()/2., button.getY()+button.getHeight()/2., 0);
                check(menu.mouseClicked(event, false), "Notify switch accepts pointer"); menu.mouseReleased(event);
                check(!Neon.config.betterScreenshot.notify, "Notify changes through native setting");
                menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z, 0, GLFW.GLFW_MOD_CONTROL)); check(Neon.config.betterScreenshot.notify, "Undo restores Notify");
                Neon.config.betterScreenshot.notify = false;
                var store = new ProfileStore(mc.gameDirectory.toPath().resolve("config/screenshot-test-profiles.json"));
                store.initialize(Neon.config); store.put("Screenshot", Neon.config);
                var reloaded = new ProfileStore(mc.gameDirectory.toPath().resolve("config/screenshot-test-profiles.json")); reloaded.initialize(new NeonConfig());
                check(reloaded.get("Screenshot").betterScreenshot.enabled && !reloaded.get("Screenshot").betterScreenshot.notify, "screenshot settings persist through profile save and reload");
                var tile = widgets(menu).stream().filter(w -> w.getClass().getSimpleName().equals("UiModuleTile") && w.getMessage().getString().equals("Better screenshot")).findFirst().orElseThrow();
                var bind = widgets((ContainerEventHandler)tile).stream().filter(w -> w.getClass().getSimpleName().equals("UiBindButton")).findFirst().orElseThrow();
                var bindClick = pointer(bind.getX()+bind.getWidth()/2., bind.getY()+bind.getHeight()/2., 0);
                check(menu.mouseClicked(bindClick, false), "screenshot bracket starts key capture"); menu.mouseReleased(bindClick);
                menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_F7, 0, 0));
                check(Neon.betterScreenshot.saveString().equals("key.keyboard.f7"), "screenshot binding assigns through the module header");
                menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z, 0, GLFW.GLFW_MOD_CONTROL));
                check(Neon.betterScreenshot.isUnbound(), "Undo restores screenshot binding");
                setup(true, "Better screenshot");
            }
            case 7 -> { geometry(); shot("module-minimum"); }
            case 8 -> setup(false, "Appearance");
            case 9 -> { geometry(); shot("settings-default"); }
            case 10 -> setup(true, "Appearance");
            case 11 -> { geometry(); shot("settings-minimum"); }
            case 12 -> {
                Neon.applyConfig(original); Neon.betterScreenshot.setKey(InputConstants.getKey(binding)); KeyMapping.resetMapping(); mc.gui.setScreen(null); return true;
            }
        }
        return false;
    }
}
