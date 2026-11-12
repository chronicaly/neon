package me.xchro.neon.smoke;

import java.util.*;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.hud.*;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import net.minecraft.client.renderer.state.gui.*;
import org.lwjgl.glfw.GLFW;

/** Unity FPS port exercised through the packaged client's renderer and real screen input. */
final class FpsHudSmoke {
    private final Minecraft mc;
    private final NeonConfig original;
    private HudEditor editor;
    private SettingsScreen parent;
    private int stage, age, scenario, checks;
    private CompletableFuture<Void> reload;
    private List<String> originalPacks;
    private int originalFontWidth, originalHudWidth;
    FpsHudSmoke(Minecraft mc, int checks) {
        this.mc = mc; this.checks = checks; original = ConfigStore.copy(Neon.config); setup();
    }
    int checks() { return checks; }
    private void check(boolean ok, String name) {
        if (!ok) throw new AssertionError("FPS " + scenario + "/" + stage + ": " + name);
        checks++; System.out.println("NEON_CHECK " + name);
    }
    private void next() { stage++; age = 0; }
    private List<AbstractWidget> widgets(ContainerEventHandler parent) {
        var out = new ArrayList<AbstractWidget>();
        for (var child : parent.children()) {
            if (child instanceof AbstractWidget w) out.add(w);
            if (child instanceof ContainerEventHandler group) out.addAll(widgets(group));
        }
        return out;
    }
    private AbstractWidget named(String name) { return widgets(editor).stream().filter(w -> w.getMessage().getString().equals(name)).findFirst().orElseThrow(); }
    private MouseButtonEvent mouse(double x, double y, int button) { return new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0)); }
    private MouseButtonEvent physical(double x, double y) { return mouse(x / editor.pixelScale(), y / editor.pixelScale(), 0); }
    private void click(AbstractWidget w, int button) {
        var e = mouse(editor.guiCoordinate(w.getX() + 16), editor.guiCoordinate(w.getY() + 8), button);
        check(editor.mouseClicked(e, false), "FPS row responds: " + w.getMessage().getString()); editor.mouseReleased(e);
    }
    private void key(int key, int modifiers) { editor.keyPressed(new KeyEvent(key, 0, modifiers)); }
    private void undo() { key(GLFW.GLFW_KEY_Z, GLFW.GLFW_MOD_CONTROL); }
    private HudBounds bounds() { return FpsHud.bounds(mc.getWindow().getWidth(), mc.getWindow().getHeight()); }
    private void shot(String suffix) { Screenshot.grab(mc.gameDirectory, "neon-fps-" + scenario + "-" + suffix + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {}); }
    private void setup() {
        Neon.config.resetHud(); Neon.config.fps.x = .75; Neon.config.fps.y = .3;
        new HudPanelPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-hud-window.json"), Neon.SAVES); Neon.SAVES.flush().join();
        mc.getWindow().setWindowed(scenario < 3 ? 1280 : 640, scenario < 3 ? 800 : 480);
        mc.options.guiScale().set(scenario % 3 + 1); mc.resizeGui(); mc.setLastInputType(InputType.MOUSE);
        mc.gui.setScreen(parent = Menus.fresh(null));
        widgets(parent).stream().filter(w -> w instanceof Button && w.getMessage().getString().equals("HUD editor")).map(w -> (Button) w).findFirst().orElseThrow().onPress(null);
        editor = (HudEditor) mc.gui.screen(); stage = age = 0;
    }
    private int layers() throws Exception {
        var state = new GuiRenderState(); var g = new GuiGraphicsExtractor(mc, state, 0, 0);
        g.pose().scale(1f / editor.pixelScale()); FpsHud.draw(g, mc.getWindow().getWidth(), mc.getWindow().getHeight());
        var text = new ArrayList<GuiTextRenderState>(); state.forEachText(text::add);
        var font = GuiTextRenderState.class.getDeclaredField("font"); font.setAccessible(true);
        var shadow = GuiTextRenderState.class.getDeclaredField("dropShadow"); shadow.setAccessible(true);
        for (var run : text) {
            check(font.get(run) == mc.font, "FPS renders with Minecraft's active resource-pack font");
            check(shadow.getBoolean(run) == Neon.config.fpsShadow, "FPS uses the configured vanilla text shadow");
        }
        return text.size();
    }
    boolean run() throws Exception {
        if (++age < 10) return false;
        switch (stage) {
            case 0 -> {
                // Catch-up ticks can precede the first rendered frame after a window resize.
                if (FpsHud.value() == 0 && age < 200) return false;
                check(Neon.config.fps.visible && FpsHud.value() > 0, "production frame hook produces live FPS");
                check(editor.inspectorBounds().height() == 111, "four collapsed HUD roots retain compact rows");
                check(FpsHud.fontPixels() == 16 && bounds().height() == mc.font.lineHeight * 2 + 4, "Minecraft FPS font scales independently of GUI scale");
                shot("collapsed"); click(named("FPS"), 1); next();
            }
            case 1 -> {
                check(List.of("Smooth FPS", "Background", "Border", "Shadow").stream().allMatch(n -> named(n).active), "Unity FPS controls expand inline");
                check(named("Shadow").getBottom() <= editor.inspectorBounds().bottom(), "last FPS setting fits the HUD viewport");
                int sound = SoundAudit.neonEnables;
                click(named("Smooth FPS"), 0); check(Neon.config.fpsAnimation, "Smooth FPS applies live"); undo(); check(!Neon.config.fpsAnimation, "Undo restores FPS smoothing");
                click(named("Background"), 0); click(named("Border"), 0); click(named("Shadow"), 0);
                check(SoundAudit.neonEnables == sound+3, "FPS boolean toggles request their enable cues; Undo stays silent");
                check(layers() == 2, "FPS draws one label and one number without duplicate transition layers");
                Neon.config.fpsShadow = true; check(layers() == 2, "Minecraft handles shadows without duplicate text runs"); Neon.config.fpsShadow = false;
                var scale = widgets(editor).stream().filter(w -> w instanceof ExactSlider).findFirst().orElseThrow(); click(scale, 1);
                key(GLFW.GLFW_KEY_A, GLFW.GLFW_MOD_CONTROL);
                for (int cp : "1.125".codePoints().toArray()) editor.charTyped(new CharacterEvent(cp));
                key(GLFW.GLFW_KEY_ENTER, 0); check(Neon.config.fps.scale == 1.125 && FpsHud.fontPixels() == 18, "FPS font scale preserves exact fractional input");
                undo(); check(Neon.config.fps.scale == 1, "one Undo restores FPS scale"); next();
            }
            case 2 -> {
                shot("options"); var before = bounds();
                var start = physical(before.x() + before.width() / 2.0, before.y() + before.height() / 2.0);
                check(editor.mouseClicked(start, false), "FPS accepts direct canvas dragging");
                var end = physical(before.x() + before.width() / 2.0 - 80, before.y() + before.height() / 2.0 + 60);
                editor.mouseDragged(end, -80, 60);
                check(bounds().x() == before.x() - 80 && bounds().y() == before.y() + 60 && !editor.controlsVisible(), "FPS placement follows the pointer immediately");
                editor.mouseReleased(physical(-10, -10)); check(editor.controlsVisible(), "release outside FPS completes placement");
                undo(); check(bounds().equals(before), "one Undo restores the complete FPS placement gesture");
                var corner = physical(before.x() + before.width() - 1, before.y() + before.height() - 1);
                check(editor.mouseClicked(corner, false), "FPS invisible corner starts resizing");
                editor.mouseDragged(physical(before.x() + before.width() + 30, before.y() + before.height() + 10), 30, 10);
                check(Neon.config.fps.scale > 1, "FPS corner changes its scale");
                key(GLFW.GLFW_KEY_ESCAPE, 0); check(bounds().equals(before) && Neon.config.fps.scale == 1, "Escape cancels FPS resize");
                key(GLFW.GLFW_KEY_LEFT, 0); check(bounds().x() == before.x() - 1, "FPS keyboard nudge is one display pixel"); undo();
                click(named("FPS"), 0); check(!Neon.config.fps.visible && layers() == 0, "disabled FPS is absent from the renderer");
                check(!editor.mouseClicked(start, false), "disabled FPS ignores canvas input");
                undo(); check(Neon.config.fps.visible, "Undo restores FPS visibility");
                check(Neon.save(), "FPS settings save"); Neon.SAVES.flush().join();
                var saved = new ConfigStore(mc.gameDirectory.toPath().resolve("config/neon.json")).load();
                check(saved.fps.visible && saved.fpsBackground && saved.fpsBorder && !saved.fpsShadow && saved.fps.x == Neon.config.fps.x, "FPS placement and appearance survive reload");
                key(GLFW.GLFW_KEY_ESCAPE, 0); check(mc.gui.screen() == parent, "FPS editor returns to its Visuals menu");
                mc.gui.setScreen(null); next();
            }
            case 3 -> {
                check(FpsHud.value() > 0, "FPS remains live during gameplay"); shot("gameplay");
                if (++scenario < 6) { setup(); return false; }
                originalPacks = List.copyOf(mc.getResourcePackRepository().getSelectedIds());
                originalFontWidth = mc.font.width("FPS 0123456789"); originalHudWidth = bounds().width();
                var pack = mc.gameDirectory.toPath().resolve("resourcepacks/neon-fps-font-verification");
                Files.createDirectories(pack.resolve("assets/minecraft/font"));
                Files.writeString(pack.resolve("pack.mcmeta"), """
                    {"pack":{"description":"Neon FPS font verification only","min_format":88,"max_format":88}}
                    """);
                Files.writeString(pack.resolve("assets/minecraft/font/default.json"), """
                    {"providers":[{"type":"ttf","file":"neon:inter-medium.ttf","size":9,"oversample":2,"shift":[0,0]}]}
                    """);
                var packs = mc.getResourcePackRepository(); packs.reload();
                var selected = new ArrayList<>(originalPacks); selected.add("file/neon-fps-font-verification"); packs.setSelected(selected);
                reload = mc.reloadResourcePacks(); next();
            }
            case 4 -> {
                if (!reload.isDone() || mc.gui.overlay() != null) return false; reload.join();
                check(mc.font.width("FPS 0123456789") != originalFontWidth, "test resource pack replaces Minecraft's default font");
                check(bounds().width() != originalHudWidth && layers() == 2, "FPS rendering and bounds follow a live resource-pack change");
                shot("resource-pack");
                mc.getResourcePackRepository().setSelected(originalPacks); reload = mc.reloadResourcePacks(); next();
            }
            case 5 -> {
                if (!reload.isDone() || mc.gui.overlay() != null) return false; reload.join();
                check(mc.font.width("FPS 0123456789") == originalFontWidth && bounds().width() == originalHudWidth, "FPS font and bounds restore when the resource pack is removed");
                Neon.applyConfig(original); mc.gui.setScreen(null); mc.resizeGui(); return true;
            }
        }
        return false;
    }
}
