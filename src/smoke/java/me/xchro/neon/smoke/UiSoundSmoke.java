package me.xchro.neon.smoke;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.core.UiSoundPolicy.Cue;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Actual UI dispatch with the production JAR; audio is intercepted before device access. */
final class UiSoundSmoke {
    private final Minecraft mc;
    private final NeonConfig original = ConfigStore.copy(Neon.config);
    private final String binding = Neon.zoom.saveString(), clipboard;
    private PixelScreen screen;
    private int stage, age, checks;
    UiSoundSmoke(Minecraft mc, int checks) {
        this.mc = mc; this.checks = checks; clipboard = mc.keyboardHandler.getClipboard();
        mc.getWindow().setWindowed(1280,800); mc.options.guiScale().set(2); mc.resizeGui(); mc.setLastInputType(InputType.MOUSE);
        new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"), Neon.SAVES); Neon.SAVES.flush().join();
        Neon.config.theme.soundVolume=.4;
        Neon.zoom.setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN); KeyMapping.resetMapping();
        expect(() -> mc.gui.setScreen(screen=Menus.fresh(null)), Cue.NAVIGATE);
        move(0,0);
    }
    int checks() { return checks; }
    private void check(boolean ok,String message) { if(!ok)throw new AssertionError("NXE stage "+stage+": "+message); checks++;System.out.println("NEON_CHECK "+message); }
    private void expect(Runnable input,Cue... expected) {
        int start=SoundAudit.cues.size(); input.run();
        var actual=SoundAudit.cues.subList(start,SoundAudit.cues.size()).stream().map(r->r.cue()).toList();
        check(actual.equals(List.of(expected)),"audio events "+actual+" match "+List.of(expected));
    }
    private List<AbstractWidget> widgets(ContainerEventHandler group) {
        var all=new ArrayList<AbstractWidget>();
        for(var child:group.children()){if(child instanceof AbstractWidget w)all.add(w);if(child instanceof ContainerEventHandler c)all.addAll(widgets(c));}return all;
    }
    private UiButton button(String name) { return widgets(screen).stream().filter(w->w instanceof UiButton && w.getMessage().getString().equals(name)).map(w->(UiButton)w).findFirst().orElseThrow(()->new AssertionError("Missing "+name)); }
    private UiButton toggle(String name) { return widgets(screen).stream().filter(w->w instanceof UiButton b && b.hint().equals("Enable "+name)).map(w->(UiButton)w).findFirst().orElseThrow(); }
    private MouseButtonEvent pointer(double x,double y,int button) { return new MouseButtonEvent(screen.guiCoordinate(x),screen.guiCoordinate(y),new MouseButtonInfo(button,0)); }
    private void click(AbstractWidget w) { click(w,0); }
    private void click(AbstractWidget w,int button) {
        for (var candidate : widgets(screen)) if (candidate.getClass().getSimpleName().equals("UiScroll")
            && candidate instanceof AbstractScrollArea scroll && widgets((ContainerEventHandler)candidate).contains(w)) {
            if (w.getBottom()>scroll.getBottom()) scroll.setScrollAmount(scroll.scrollAmount()+w.getBottom()-scroll.getBottom());
            if (w.getY()<scroll.getY()) scroll.setScrollAmount(scroll.scrollAmount()+w.getY()-scroll.getY());
        }
        var e=pointer(w.getX()+w.getWidth()/2.,w.getY()+w.getHeight()/2.,button);check(screen.mouseClicked(e,false),"control accepts pointer: "+w.getMessage().getString());screen.mouseReleased(e);
    }
    private void move(double x,double y) { screen.mouseMoved(screen.guiCoordinate(x),screen.guiCoordinate(y)); }
    private void move(AbstractWidget w) { move(w.getX()+w.getWidth()/2.,w.getY()+w.getHeight()/2.); }
    private void key(int key) { screen.keyPressed(new KeyEvent(key,0,0)); }
    private ExactSlider slider() { return widgets(screen).stream().filter(w->w instanceof ExactSlider).map(w->(ExactSlider)w).findFirst().orElseThrow(); }
    private void exact(String text) {
        var s=slider();var e=pointer(s.getRight()-2,s.getY()+s.getHeight()/2.,1);screen.mouseClicked(e,false);screen.mouseReleased(e);
        check(s.isEditing(),"numeric draft opens inline");InlineInput.field(screen).setValue(text);key(GLFW.GLFW_KEY_ENTER);
    }
    private void shot(String name) { Screenshot.grab(mc.gameDirectory,"neon-nxe-"+name+".png",mc.gameRenderer.mainRenderTarget(),1,message->{}); }
    boolean run() throws Exception {
        if(++age<15)return false;age=0;
        switch(stage++) {
            case 0 -> {
                expect(()->move(toggle("Freelook")),Cue.HOVER);
                check(Math.abs(SoundAudit.cues.getLast().volume()-.14)<1e-8,"hover gain is 35% of preserved master volume");
                expect(()->{move(button("Freelook"));move(button("[—]"));move(button("Freelook"));});
                expect(()->move(button("Zoom"))); // Immediate crossing is rate-limited, not queued.
            }
            case 1 -> {
                expect(()->move(button("Zoom"))); // Suppressed entry must never replay later.
                expect(()->move(button("Motion blur")),Cue.HOVER);
                expect(()->{double x=button("Motion blur").getX()+3,y=button("Motion blur").getY()+3;move(x,y);move(x,y);});
                expect(()->click(button("Combat")),Cue.NAVIGATE);
                expect(()->move(button("Autoclicker"))); // Action suppresses subsequent hover.
                expect(()->click(button("Combat")));
                expect(()->click(button("Visuals")),Cue.NAVIGATE);
                expect(()->click(button("Freelook")),Cue.NAVIGATE);
            }
            case 2 -> {
                expect(()->InlineInput.clickTrack(slider(),false));
                expect(()->exact("7.125"),Cue.CONFIRM);
                expect(()->exact("7.125"));
                expect(()->exact("100"));check(slider().isEditing(),"invalid exact value stays open without a cue");key(GLFW.GLFW_KEY_ESCAPE);
                expect(()->click(button("Freelook")),Cue.BACK);
                expect(()->click(toggle("Freelook")),Cue.DISABLE);
                expect(()->click(toggle("Freelook")),Cue.ENABLE);
                expect(()->{mc.resizeGui();Neon.applyConfig(ConfigStore.copy(Neon.config));});
                LiveSmoke.focused=false;expect(()->move(button("Zoom")));LiveSmoke.focused=true;expect(()->move(button("Zoom")));
            }
            case 3 -> {
                mc.setLastInputType(InputType.KEYBOARD_TAB);screen.setFocused((net.minecraft.client.gui.components.events.GuiEventListener)null);
                expect(()->key(GLFW.GLFW_KEY_TAB),Cue.HOVER);
                mc.setLastInputType(InputType.MOUSE);
                expect(()->click(button("Zoom")),Cue.NAVIGATE);
            }
            case 4 -> {
                expect(()->click(button("Hold")));
                expect(()->click(button("Toggle")),Cue.CONFIRM);
                expect(()->click(button("Toggle")));
                var key=widgets(screen).stream().filter(w->w instanceof UiButton b&&b.hint().contains("key.neon.zoom")).findFirst().orElseThrow();
                expect(()->click(key));expect(()->key(GLFW.GLFW_KEY_F8),Cue.CONFIRM);
                expect(()->click(button("Settings")),Cue.NAVIGATE);expect(()->click(button("Colors")),Cue.NAVIGATE);
            }
            case 5 -> { expect(()->click(button("Theme")),Cue.NAVIGATE); }
            case 6 -> {
                var picker=widgets(screen).stream().filter(w->w.getClass().getSimpleName().equals("ColorPicker")).findFirst().orElseThrow();
                // A blank topmost popup area must not hover a covered menu control.
                picker.setX(((SettingsScreen)screen).menuBounds().left()+110);picker.setY(((SettingsScreen)screen).menuBounds().top()+38);
                expect(()->move(picker.getX()+5,picker.getY()+8));
                var shade=widgets(screen).stream().filter(w->w.getMessage().getString().equals("Saturation and brightness")).findFirst().orElseThrow();
                var p=pointer(shade.getX()+20,shade.getY()+20,0);
                expect(()->{screen.mouseClicked(p,false);screen.mouseDragged(pointer(shade.getX()+50,shade.getY()+35,0),20,10);move(button("Paste"));screen.mouseReleased(p);});
                mc.keyboardHandler.setClipboard("invalid color");expect(()->click(button("Paste")));
                mc.keyboardHandler.setClipboard("#4488AAFF");expect(()->click(button("Paste")),Cue.CONFIRM);
                expect(()->click(button("Paste")));
                expect(()->key(GLFW.GLFW_KEY_ESCAPE),Cue.BACK);
                expect(()->click(button("Appearance")),Cue.NAVIGATE);
            }
            case 7 -> {
                shot("appearance-default");
                check(widgets(screen).stream().anyMatch(w->w instanceof ExactSlider&&w.getMessage().getString().equals("40%")),"UI sounds retains saved 40% volume");
                expect(()->exact("23"),Cue.CONFIRM);check(Math.abs(SoundAudit.cues.getLast().volume()-.23)<1e-8,"UI volume applies to confirmation immediately");
                double master=mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).get();
                mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
                expect(()->click(button("Full")),Cue.CONFIRM);check(Math.abs(SoundAudit.cues.getLast().volume()-.23)<1e-8,"UI sound stays independent of Minecraft master");
                mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(master);
                expect(()->exact("0"));
                expect(()->{click(button("Visuals"));click(toggle("Freelook"));move(button("Zoom"));});
                Neon.config.theme.soundVolume=.4;
                expect(()->click(button("HUD editor")),Cue.NAVIGATE);screen=(PixelScreen)mc.gui.screen();
            }
            case 8 -> {
                expect(()->click(button("Armor")),Cue.DISABLE);expect(()->click(button("Armor")),Cue.ENABLE);
                expect(()->click(button("Armor"),1),Cue.NAVIGATE);
            }
            case 9 -> {
                expect(()->click(button("Slot frames")),Cue.DISABLE);expect(()->click(button("Slot frames")),Cue.ENABLE);
                expect(()->click(button("Armor"),1),Cue.BACK);
                expect(()->key(GLFW.GLFW_KEY_H));expect(()->move(30,45));
                expect(()->key(GLFW.GLFW_KEY_H));
                expect(()->key(GLFW.GLFW_KEY_ESCAPE),Cue.BACK);screen=(PixelScreen)mc.gui.screen();
                expect(()->click(button("Settings")),Cue.NAVIGATE);
                // Appearance expansion is remembered across categories.
                var b=((SettingsScreen)screen).menuBounds();var p=pointer(b.right()-1,b.bottom()-1,0);var q=pointer(b.right()-121,b.bottom()-71,0);
                expect(()->{screen.mouseClicked(p,false);screen.mouseDragged(q,q.x()-p.x(),q.y()-p.y());screen.mouseReleased(q);});
            }
            case 10 -> {
                shot("appearance-minimum");expect(()->{screen.mouseScrolled(screen.guiCoordinate(500),screen.guiCoordinate(300),0,-1);mc.resizeGui();});
                var audio=UiSounds.class.getDeclaredField("CLIPS");audio.setAccessible(true);check(((Map<?,?>)audio.get(null)).isEmpty(),"silent harness never opens a Neon audio device");
                check(SoundAudit.vanillaClicks==0,"custom controls never request Minecraft click audio");
                expect(()->click(button("Profiles")),Cue.NAVIGATE);
            }
            case 11 -> {
                String profile=Neon.profiles.names().iterator().next();
                Neon.profiles.put(profile,Neon.config);
                var load=widgets(screen).stream().filter(w->w instanceof UiButton b&&b.hint().equals("Load profile "+profile)).findFirst().orElseThrow();
                expect(()->click(load));
                expect(()->click(button("Update")),Cue.NAVIGATE);screen=(PixelScreen)mc.gui.screen();
                expect(()->click(button("Yes")));screen=(PixelScreen)mc.gui.screen();
                Neon.config.zoomFactor=Neon.config.zoomFactor==4?5:4;
                expect(()->click(button("Update")),Cue.NAVIGATE);screen=(PixelScreen)mc.gui.screen();
                expect(()->click(button("Yes")),Cue.CONFIRM);screen=(PixelScreen)mc.gui.screen();
                expect(()->click(button("Save"))); // Empty profile name is rejected silently.
                expect(()->click(button("Visuals")),Cue.NAVIGATE);
                Neon.config.motionBlurEnabled=false;
                expect(()->click(button("Motion blur")),Cue.NAVIGATE);
            }
            case 12 -> {
                check(!slider().active,"disabled module setting is noninteractive");expect(()->move(slider()));
                Neon.zoom.setKey(com.mojang.blaze3d.platform.InputConstants.getKey(binding));KeyMapping.resetMapping();
                mc.keyboardHandler.setClipboard(clipboard);Neon.applyConfig(original);mc.gui.setScreen(null);return true;
            }
        }
        return false;
    }
}
