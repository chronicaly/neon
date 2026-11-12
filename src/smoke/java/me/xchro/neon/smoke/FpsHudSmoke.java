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
    private final String clipboard;
    private HudEditor editor;
    private SettingsScreen parent;
    private int stage, age, scenario, checks;
    private CompletableFuture<Void> reload;
    private List<String> originalPacks;
    private int originalFontWidth, originalHudWidth;
    FpsHudSmoke(Minecraft mc, int checks) {
        this.mc = mc; this.checks = checks; original = ConfigStore.copy(Neon.config); clipboard=mc.keyboardHandler.getClipboard(); setup();
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
    private void paste(String value) { mc.keyboardHandler.setClipboard(value); click(named("Paste"),0); }
    private void colors() throws Exception {
        var state=new GuiRenderState();var g=new GuiGraphicsExtractor(mc,state,0,0);
        FpsHud.draw(g,mc.getWindow().getWidth(),mc.getWindow().getHeight());
        var text=new ArrayList<GuiTextRenderState>();state.forEachText(text::add);
        var color=GuiTextRenderState.class.getDeclaredField("color");color.setAccessible(true);
        check(text.size()==2 && color.getInt(text.get(0))==FpsColor.LABEL.color(Neon.config) && color.getInt(text.get(1))==FpsColor.VALUE.color(Neon.config),"renderer uses independent FPS label and value colors");
        var rectangles=new ArrayList<ColoredRectangleRenderState>();
        state.forEachElement(element->{if(element instanceof ColoredRectangleRenderState r)rectangles.add(r);},GuiRenderState.TraverseRange.ALL);
        int background=FpsColor.BACKGROUND.color(Neon.config);
        int paint=background&0xffffff|(int)Math.round((background>>>24)*.78)<<24;
        check(rectangles.stream().anyMatch(r->r.col1()==paint),"FPS background uses its own RGBA color");
        check(rectangles.stream().filter(r->r.col1()==FpsColor.BORDER.color(Neon.config)).count()==4,"FPS border uses its own RGBA color on every edge");
    }
    private void fitsColor() {
        var shell=editor.inspectorBounds();var picker=named("Color picker");
        check(shell.left()>=8 && shell.top()>=8 && shell.right()<=editor.canvasWidth()-8 && shell.bottom()<=editor.canvasHeight()-8,"HUD color drawer stays inside the display");
        check(picker.getX()>shell.left() && picker.getRight()<shell.right() && named("Paste").getBottom()<shell.bottom(),"compact color actions fit inside the quiet shared HUD border");
    }
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
                undo(); check(Neon.config.fps.scale == 1, "one Undo restores FPS scale"); stage=6;age=0;
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
                Neon.applyConfig(original); mc.keyboardHandler.setClipboard(clipboard); mc.gui.setScreen(null); mc.resizeGui(); return true;
            }
            case 6 -> { Neon.config.fps.y=.9;check(Arrays.stream(FpsColor.values()).allMatch(role->named(role.label).active),"four FPS color rows live directly in the FPS fold");click(named("Label color"),0);next(); }
            case 7 -> {
                fitsColor();var before=ConfigStore.copy(Neon.config);paste("#FF8844FF");
                check(FpsColor.LABEL.color(Neon.config)==0xffff8844 && FpsColor.VALUE.synced(Neon.config),"label edit leaves the number following the theme");
                var edited=ConfigStore.copy(Neon.config);edited.fpsColors.clear();before.fpsColors.clear();
                check(ConfigStore.encode(edited).equals(ConfigStore.encode(before)),"FPS color editing never mutates the menu palette, alpha or other settings");
                undo();check(FpsColor.LABEL.synced(Neon.config),"Undo restores the original theme link");paste("#FF8844FF");
                check(FpsColor.LABEL.color(Neon.config)==0xffff8844,"pasting immediately after Undo uses the restored picker state");
                click(named("Value color"),1);paste("#44DDFFFF");
                click(named("Background color"),0);paste("#142535A0");
                click(named("Border color"),0);paste("#9966FFFF");colors();
                check(FpsColor.LABEL.color(Neon.config)==0xffff8844 && FpsColor.VALUE.color(Neon.config)==0xff44ddff && FpsColor.BACKGROUND.color(Neon.config)==0xa0142535 && FpsColor.BORDER.color(Neon.config)==0xff9966ff,"all four selected FPS colors remain independent");
                click(named("Copy"),0);check(mc.keyboardHandler.getClipboard().equals("#9966FFFF"),"HUD color Copy retains RGBA order");
                var hex=(EditBox)named("Hex color");click(hex,0);hex.setValue("#ABCDEF80");key(GLFW.GLFW_KEY_ENTER,0);
                check(FpsColor.BORDER.color(Neon.config)==0x80abcdef,"HUD color hex edits apply live");undo();
                var shade=named("Saturation and brightness");int sounds=SoundAudit.cues.size();int previous=FpsColor.BORDER.color(Neon.config);
                var start=mouse(editor.guiCoordinate(shade.getX()+5),editor.guiCoordinate(shade.getY()+5),0);
                var end=mouse(editor.guiCoordinate(shade.getRight()-5),editor.guiCoordinate(shade.getBottom()-5),0);
                editor.mouseClicked(start,false);editor.mouseDragged(end,end.x()-start.x(),end.y()-start.y());editor.mouseReleased(end);
                check(FpsColor.BORDER.color(Neon.config)!=previous && SoundAudit.cues.size()==sounds,"continuous HUD color dragging changes the color silently");
                undo();check(FpsColor.BORDER.color(Neon.config)==previous,"one Undo restores a complete HUD color drag");
                click(named("Sync"),0);check(FpsColor.BORDER.synced(Neon.config),"Sync restores theme following for only the selected FPS color");undo();colors();
                Neon.save();Neon.SAVES.flush().join();var saved=new ConfigStore(mc.gameDirectory.toPath().resolve("config/neon.json")).load();
                check(saved.fpsColors.equals(Neon.config.fpsColors),"all four independent FPS colors survive a real save and reload");
                next();
            }
            case 8 -> {
                fitsColor();shot("colors");
                click(named("Hex color"),0);var panel=editor.inspectorBounds();
                var start=mouse(editor.guiCoordinate(panel.left()+24),editor.guiCoordinate(panel.top()+8),0);
                var end=mouse(editor.guiCoordinate(panel.left()+48),editor.guiCoordinate(panel.top()+18),0);
                editor.mouseClicked(start,false);editor.mouseDragged(end,end.x()-start.x(),end.y()-start.y());
                check(editor.inspectorBounds().left()==panel.left()+24,"HUD header dragging takes priority over focused color hex input");
                key(GLFW.GLFW_KEY_ESCAPE,0);editor.mouseReleased(end);
                check(editor.inspectorBounds().left()==panel.left() && named("Color picker")!=null,"Escape cancels a HUD panel move before dismissing its color drawer");
                key(GLFW.GLFW_KEY_ESCAPE,0);
                check(mc.gui.screen()==editor && widgets(editor).stream().noneMatch(w->w.getMessage().getString().equals("Color picker")),"Escape closes only the color drawer and retains its edits");next();
            }
            case 9 -> { check(editor.inspectorBounds().width()==188,"closing restores the compact HUD column");Neon.config.fps.y=.3;stage=2;age=0; }
        }
        return false;
    }
}
