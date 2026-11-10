package me.xchro.neon.smoke;

import java.util.*;
import javax.sound.sampled.Clip;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.hud.*;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.glfw.GLFW;

/** Physical-pixel HUD geometry, offhand attachment, independent audio and odd-size GPU captures. */
final class PolishSmoke {
    private final Minecraft mc;
    private final NeonConfig original = ConfigStore.copy(Neon.config);
    private final double master;
    private final int minecraftBlur;
    private int stage, age, checks;
    private HudBounds physicalArmor;
    PolishSmoke(Minecraft mc, int checks) {
        this.mc = mc; this.checks = checks; master = mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).get();
        minecraftBlur = mc.options.menuBackgroundBlurriness().get(); mc.options.menuBackgroundBlurriness().set(0);
        Neon.config = new NeonConfig(); Neon.config.armorAnchor = NeonConfig.ArmorAnchor.CUSTOM;
        Neon.config.armor.x = .35; Neon.config.armor.y = .6;
        setup();
    }
    int checks() { return checks; }
    private void check(boolean ok, String name) { if (!ok) throw new AssertionError("Polish " + stage + ": " + name); checks++; System.out.println("NEON_CHECK " + name); }
    private List<AbstractWidget> widgets(ContainerEventHandler root) {
        var result = new ArrayList<AbstractWidget>();
        for (var child : root.children()) { if (child instanceof AbstractWidget w) result.add(w); if (child instanceof ContainerEventHandler c) result.addAll(widgets(c)); }
        return result;
    }
    private Button button(String name) { return widgets(mc.gui.screen()).stream().filter(w -> w instanceof Button && w.getMessage().getString().equals(name)).map(w -> (Button) w).findFirst().orElseThrow(); }
    private ExactSlider slider(String name) throws ReflectiveOperationException {
        var field = ExactSlider.class.getDeclaredField("name"); field.setAccessible(true);
        for (var w : widgets(mc.gui.screen())) if (w instanceof ExactSlider s && field.get(s).equals(name)) return s;
        throw new AssertionError("Missing " + name);
    }
    private HudBounds armor() { return NeonHud.pixelBounds(mc.getWindow().getWidth(), mc.getWindow().getHeight(), true); }
    private MouseButtonEvent pixel(double x, double y) { return new MouseButtonEvent(x / NeonHud.gameScale(), y / NeonHud.gameScale(), new MouseButtonInfo(0, 0)); }
    private void key(int key, int mods) { mc.gui.screen().keyPressed(new KeyEvent(key, 0, mods)); }
    private void undo() { key(GLFW.GLFW_KEY_Z, GLFW.GLFW_MOD_CONTROL); }
    private void number(String name, String value) throws ReflectiveOperationException {
        slider(name).keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0)); InlineInput.field(mc.gui.screen()).setValue(value); key(GLFW.GLFW_KEY_ENTER, 0);
    }
    private void setup() {
        mc.getWindow().setWindowed(1603, 1003); mc.options.guiScale().set(stage + 1); mc.resizeGui();
        new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"), Neon.SAVES); Neon.SAVES.flush().join();
        mc.gui.setScreen(new SettingsScreen(null)); button("Visuals").onPress(null); button("Zoom").onPress(null); age = 0;
    }
    private void shot(String name) { Screenshot.grab(mc.gameDirectory, "neon-polish-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {}); }
    private void hudItems(int expected) {
        var state = new net.minecraft.client.renderer.state.gui.GuiRenderState();
        var graphics = new net.minecraft.client.gui.GuiGraphicsExtractor(mc,state,0,0);
        mc.gui.screen().extractRenderState(graphics,0,0,0);
        int[] counts = new int[2]; state.forEachItem(item -> counts[0]++);
        state.forEachElement(element -> { if(element.getClass().getName().contains("UiEffects$Frame")) counts[1]++; },net.minecraft.client.renderer.state.gui.GuiRenderState.TraverseRange.ALL);
        check(counts[0] == expected, "editor submits only enabled HUD items: " + expected);
        check(counts[1] == 0, "HUD canvas has no selection outline or resize-handle frame");
    }
    boolean run() {
        age++;
        try {
            if (stage < 4) {
                if (age == 8) {
                    check(mc.getWindow().getGuiScale() == stage + 1, "actual GUI scale " + (stage + 1) + " selected at an odd display size");
                    var a = armor(); if (physicalArmor == null) physicalArmor = a;
                    check(a.equals(physicalArmor), "armor physical position and dimensions stay identical at GUI scale " + (stage + 1));
                    var menu = (SettingsScreen) mc.gui.screen();
                    check(menu.menuBounds().width() == 440 && menu.menuBounds().height() == 280, "menu keeps its physical default at GUI scale " + (stage + 1));
                    check(mc.options.getMenuBackgroundBlurriness() == 5 && mc.options.menuBackgroundBlurriness().get() == 0, "Neon backdrop strength is independent of the saved Minecraft blur setting");
                    check(widgets(menu).stream().noneMatch(w -> w.getMessage().getString().equals("Advanced")), "direct module settings have no nested Advanced page");
                    var t = NeonHud.totemPixelBounds(1603,1003); int s = NeonHud.gameScale();
                    check(t.x() == (mc.getWindow().getGuiScaledWidth()/2-120)*s && t.y() == (mc.getWindow().getGuiScaledHeight()-22)*s && t.width() == 20*s,
                        "totem remains on the real offhand slot at GUI scale " + (stage + 1));
                    shot("scale" + (stage + 1) + "-default");
                    var b = menu.menuBounds(); var start = pixel(b.right()*PixelScreen.UI_SCALE,b.bottom()*PixelScreen.UI_SCALE);
                    menu.mouseClicked(start,false); menu.mouseDragged(pixel((b.right()-120)*PixelScreen.UI_SCALE,(b.bottom()-70)*PixelScreen.UI_SCALE),0,0); menu.mouseReleased(start);
                }
                if (age == 16) { shot("scale" + (stage + 1) + "-minimum"); button("HUD").onPress(null); button("General").onPress(null); }
                if (age < 24) return false;
                shot("hud-scale" + (stage + 1));
                check(mc.options.getMenuBackgroundBlurriness() == 0, "HUD placement does not override Minecraft backdrop settings");
                var offhand = NeonHud.totemPixelBounds(1603,1003);
                number("HUD scale", "1.25");
                check(armor().width() == 205 && armor().height() == 55, "HUD scale edits immediately in physical pixels");
                check(NeonHud.totemPixelBounds(1603,1003).equals(offhand), "HUD scale leaves the attached offhand count alone");
                undo(); check(armor().equals(physicalArmor), "HUD scale Undo restores exact placement and size");
                number("Snap sensitivity", "18"); check(Neon.config.snapSensitivity == 18, "snap threshold accepts an exact display-pixel value");
                undo(); check(Neon.config.snapSensitivity == 10, "snap sensitivity is one Undo step");
                key(GLFW.GLFW_KEY_H,0);
                var editor = (HudEditor) mc.gui.screen(); var a = armor(); var start = pixel(a.x()+a.width()/2.,a.y()+a.height()/2.);
                editor.mouseClicked(start,false); editor.mouseReleased(start); key(GLFW.GLFW_KEY_RIGHT,0);
                check(armor().x() == a.x()+1, "arrow movement stays one display pixel at GUI scale " + (stage+1)); undo();
                Neon.config.editorSnap = true;
                var end = pixel(8+a.width()/2.,400+a.height()/2.);
                editor.mouseClicked(start,false); editor.mouseDragged(end,0,0); editor.mouseReleased(end);
                check(armor().x() == 0, "10px snap captures an 8px gap at GUI scale " + (stage+1)); undo();
                Neon.config.snapSensitivity = 4;
                editor.mouseClicked(start,false); editor.mouseDragged(end,0,0); editor.mouseReleased(end);
                check(armor().x() == 8, "4px snap leaves the same 8px gap unsnapped"); undo();
                Neon.config.snapSensitivity = 10; Neon.config.editorSnap = false;
                if (++stage < 4) setup(); else {
                    mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0); Neon.config.theme.soundVolume = .23;
                    int before = SoundAudit.neonClicks; UiSounds.moduleEnabled();
                    check(SoundAudit.neonClicks == before+1 && Math.abs(SoundAudit.lastVolume-.23)<.00001, "module sound uses Neon volume while Minecraft master is muted");
                    age = 0;
                }
                return false;
            }
            if (age < 8) return false;
            if (stage == 4) {
            var audio = UiSounds.class.getDeclaredField("clip"); audio.setAccessible(true);
            check(audio.get(null) instanceof Clip clip && clip.isOpen() && clip.getFrameLength()>0, "original Neon waveform is loaded into an independent open audio device");
            int quiet = SoundAudit.neonClicks; Neon.config.theme.soundVolume=0; UiSounds.moduleEnabled();
            check(SoundAudit.neonClicks == quiet, "Neon volume zero silences enable cues independently");
            check(SoundAudit.vanillaClicks == 0, "no Minecraft menu sound was emitted");
            Neon.config.armor.visible = Neon.config.totems.visible = false; Neon.config.totemFloating = true;
            Neon.config.editorGuides = false; mc.gui.setScreen(new HudEditor(null)); key(GLFW.GLFW_KEY_H,0); hudItems(0);
            var a = armor(); var p = pixel(a.x()+a.width()/2.,a.y()+a.height()/2.);
            check(!mc.gui.screen().mouseClicked(p,false), "disabled armor leaves no invisible drag target");
            check(!mc.gui.screen().mouseClicked(pixel(a.x(),a.y()),false), "disabled armor leaves no invisible resize target");
            double x=Neon.config.armor.x, scale=Neon.config.armor.scale;
            key(GLFW.GLFW_KEY_RIGHT,0); mc.gui.screen().mouseScrolled(p.x(),p.y(),0,1);
            check(Neon.config.armor.x==x && Neon.config.armor.scale==scale, "hidden HUD widgets ignore nudging and wheel scaling");
            Neon.config.totems.visible=true; hudItems(1); Neon.config.totems.visible=false;
            check(NeonHud.offhandCount(mc.player.getOffhandItem())==null, "disabled totem module adds no offhand total");
            key(GLFW.GLFW_KEY_H,0); button("Armor").onPress(null); key(GLFW.GLFW_KEY_H,0); hudItems(4);
            check(mc.gui.screen().mouseClicked(p,false), "re-enabled armor accepts a direct drag");
            mc.gui.screen().mouseDragged(pixel(a.x()+a.width()/2.+40,a.y()+a.height()/2.-25),0,0); hudItems(4);
            stage=5;age=0;return false;
            }
            if(stage==5){
                shot("hud-clean-drag");mc.gui.screen().mouseReleased(pixel(0,0));
                Neon.config.armor.visible=false;mc.gui.setScreen(new HudEditor(null));stage=6;age=0;return false;
            }
            shot("hud-disabled");
            mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(master); Neon.applyConfig(original); mc.gui.setScreen(null);
            check(mc.options.getMenuBackgroundBlurriness() == 0, "closing Neon restores Minecraft's original blur behavior");
            mc.options.menuBackgroundBlurriness().set(minecraftBlur);
            mc.options.guiScale().set(2); mc.getWindow().setWindowed(1280,800); mc.resizeGui(); return true;
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
}
