package me.xchro.neon.smoke;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Rapid input, identity, invalid drafts and GPU cache lifecycle on the packaged client. */
final class QualitySmoke {
    public static int optionsWrites;
    private final Minecraft mc;
    private final NeonConfig original;
    private int stage, age, checks;
    private double startScroll;
    private AbstractScrollArea scroller;
    private SettingsScreen menu;
    private CompletableFuture<Void> reload;
    private Object face;
    private me.xchro.neon.core.AdaptiveEffects adaptive() throws ReflectiveOperationException {
        var f=MenuEffects.class.getDeclaredField("QUALITY"); f.setAccessible(true); return (me.xchro.neon.core.AdaptiveEffects) f.get(null);
    }
    private boolean effect(String name) throws ReflectiveOperationException {
        var method=MenuEffects.class.getDeclaredMethod(name); method.setAccessible(true); return (boolean)method.invoke(null);
    }
    private void driveTier(int tier) throws ReflectiveOperationException {
        MenuEffects.frame(); var quality=adaptive(); long now=System.nanoTime();
        quality.frame(now,1e9/120,false,true);
        for(int i=0;i<165*tier;i++) quality.frame(now+=20_000_000,1e9/120,true,false);
    }
    QualitySmoke(Minecraft mc, int checks) {
        this.mc = mc; this.checks = checks; original = ConfigStore.copy(Neon.config);
        new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"), Neon.SAVES); Neon.SAVES.flush().join();
        mc.options.guiScale().set(2); mc.getWindow().setWindowed(1280,800); mc.resizeGui();
        mc.gui.setScreen(menu = new SettingsScreen(null)); press("Visuals"); press("Zoom");
    }
    int checks() { return checks; }
    private void check(boolean ok, String name) { if (!ok) throw new AssertionError("Quality stage " + stage + ": " + name); checks++; System.out.println("NEON_CHECK " + name); }
    private void next() { stage++; age=0; }
    private List<AbstractWidget> widgets(ContainerEventHandler root) {
        var list = new ArrayList<AbstractWidget>();
        for (var child : root.children()) { if (child instanceof AbstractWidget w) list.add(w); if (child instanceof ContainerEventHandler c) list.addAll(widgets(c)); }
        return list;
    }
    private List<AbstractWidget> widgets() { return widgets(mc.gui.screen()); }
    private Button button(String name) { return widgets().stream().filter(w -> w instanceof Button && w.getMessage().getString().equals(name)).map(w -> (Button)w).findFirst().orElseThrow(); }
    private void press(String name) { button(name).onPress(null); }
    private ExactSlider number() { return widgets().stream().filter(w -> w instanceof ExactSlider).map(w -> (ExactSlider)w).findFirst().orElseThrow(); }
    private MouseButtonEvent mouse(double x,double y) { return new MouseButtonEvent(menu.guiCoordinate(x),menu.guiCoordinate(y),new MouseButtonInfo(0,0)); }
    private void key(int key, int mods) { mc.gui.screen().keyPressed(new KeyEvent(key,0,mods)); }
    private void shot(String name) { Screenshot.grab(mc.gameDirectory,"neon-quality-"+name+".png",mc.gameRenderer.mainRenderTarget(),1,message -> {}); }
    boolean run() {
        age++;
        if (stage != 1 && age<5) return false;
        try {
            switch(stage) {
                case 0 -> {
                    for(String sound:List.of("enable")) {
                        var id=net.minecraft.resources.Identifier.fromNamespaceAndPath("neon","ui."+sound);
                        check(mc.getSoundManager().getSoundEvent(id)!=null,"packaged custom sound resolves: "+sound);
                    }
                    int quiet=SoundAudit.neonClicks;
                    press("Visuals"); press("Motion blur");
                    check(SoundAudit.neonClicks==quiet,"menu navigation stays silent");
                    Neon.config.motionBlurEnabled=false; press("Visuals"); press("Motion blur");
                    var blurToggle=(UiButton)widgets().stream().filter(w->w instanceof UiButton b&&b.hint().equals("Enable Motion blur")).findFirst().orElseThrow();
                    blurToggle.onPress(null); check(SoundAudit.neonClicks==quiet+1,"enabling a module plays one custom sound");
                    blurToggle.onPress(null); check(SoundAudit.neonClicks==quiet+1,"disabling a module stays silent");
                    press("Visuals"); press("Zoom");
                    scroller=widgets().stream().filter(w -> w instanceof AbstractScrollArea).map(w -> (AbstractScrollArea)w).findFirst().orElseThrow();
                    scroller.setScrollAmount(0); startScroll=scroller.scrollAmount();
                    check(menu.mouseScrolled(menu.guiCoordinate(scroller.getX()+10),menu.guiCoordinate(scroller.getY()+10),0,-1),"wheel accepted immediately");
                    check(scroller.scrollAmount()==startScroll,"wheel starts from the displayed position instead of jumping"); next();
                }
                case 1 -> {
                    check(scroller.scrollAmount()>startScroll && scroller.scrollAmount()<=Math.min(24,scroller.maxScrollAmount()),"wheel advances smoothly without overshoot");
                    scroller.setScrollAmount(0); check(scroller.scrollAmount()==0,"keyboard/focus scroll positioning remains immediate");
                    var slider=number(); var bounds=menu.menuBounds();
                    var start=mouse(bounds.left()+35,bounds.top()+17); menu.mouseClicked(start,false);
                    menu.mouseDragged(mouse(bounds.left()+55,bounds.top()+27),20,10); menu.mouseReleased(start);
                    check(number()==slider,"header drag retains the actual widget instance");
                    bounds=menu.menuBounds(); start=mouse(bounds.right(),bounds.bottom()); menu.mouseClicked(start,false);
                    menu.mouseDragged(mouse(bounds.right()-45,bounds.bottom()-30),-45,-30); menu.mouseReleased(start);
                    check(number()==slider,"corner resize reflows the existing widgets");
                    slider.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER,0,0)); InlineInput.field(menu).setValue("4.125");
                    menu.resize(menu.width,menu.height);
                    check(InlineInput.field(menu).getValue().equals("4.125"),"viewport layout preserves exact numeric draft");
                    key(GLFW.GLFW_KEY_A,GLFW.GLFW_MOD_CONTROL);
                    "4.125".codePoints().forEach(cp -> check(menu.charTyped(new CharacterEvent(cp)),"typing remains live after layout changes"));
                    key(GLFW.GLFW_KEY_ENTER,0); check(Neon.config.zoomFactor==4.125,"Enter commits the preserved numeric draft");
                    int writes=optionsWrites; key(GLFW.GLFW_KEY_Z,GLFW.GLFW_MOD_CONTROL);
                    check(optionsWrites==writes,"numeric Undo does not rewrite unchanged key bindings"); next();
                }
                case 2 -> {
                    number().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER,0,0)); InlineInput.field(menu).setValue("17"); key(GLFW.GLFW_KEY_ENTER,0);
                    check(number().isEditing(),"invalid numeric input remains editable");
                    var invalid=InlineInput.field(menu).getClass().getDeclaredField("invalid"); invalid.setAccessible(true);
                    check(invalid.getBoolean(InlineInput.field(menu)),"invalid input has explicit visible error styling"); shot("invalid-number");
                    key(GLFW.GLFW_KEY_ESCAPE,0); check(!number().isEditing() && mc.gui.screen()==menu,"Escape cancels only the numeric draft");
                    number().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER,0,0)); InlineInput.field(menu).setValue("6.25");
                    var b=menu.menuBounds(); var e=mouse(b.left()+35,b.top()+17); menu.mouseClicked(e,false); menu.mouseReleased(e);
                    check(Neon.config.zoomFactor==6.25 && !number().isEditing(),"clicking elsewhere commits valid exact input");
                    press("Settings"); press("Appearance"); press("Full");
                    check(Neon.config.theme.effects==NeonConfig.Effects.FULL,"Effects Full applies immediately");
                    press("Adaptive"); check(Neon.config.theme.effects==NeonConfig.Effects.ADAPTIVE,"Effects Adaptive applies immediately");
                    reload=mc.reloadResourcePacks(); next();
                }
                case 3 -> {
                    if(!reload.isDone() || mc.gui.overlay()!=null)return false; reload.join();
                    check(mc.gui.screen()==menu,"resource reload preserves the open menu");
                    Class<?> font=Class.forName("me.xchro.neon.ui.UiFont"); var getFace=font.getDeclaredMethod("face",int.class); getFace.setAccessible(true); face=getFace.invoke(null,11);
                    var run=face.getClass().getDeclaredMethod("run",String.class); run.setAccessible(true);
                    for(int i=0;i<220;i++) run.invoke(face,"Cache " + i + " " + "W".repeat(85));
                    var pages=face.getClass().getDeclaredField("pages"); pages.setAccessible(true);
                    check(((List<?>)pages.get(face)).size()>3,"atlas stress crosses the page budget within one extraction"); next();
                }
                case 4 -> {
                    var pages=face.getClass().getDeclaredField("pages"); pages.setAccessible(true);
                    var runs=face.getClass().getDeclaredField("runs"); runs.setAccessible(true);
                    check(((List<?>)pages.get(face)).size()<=3 && !((Map<?,?>)runs.get(face)).isEmpty(),"atlas evicts bounded pages between frames without resetting every cache");
                    shot("cache-after-reload");
                    for(int i=0;i<20;i++){ mc.gui.setScreen(null); mc.gui.setScreen(new SettingsScreen(null)); }
                    check(mc.gui.screen() instanceof SettingsScreen,"twenty repeated menu openings retain valid resources");
                    press("Settings"); press("Appearance"); driveTier(1); next();
                }
                case 5 -> {
                    check(adaptive().tier()==1 && !effect("blur") && effect("glow"),"adaptive tier one disables only the menu background blur"); shot("adaptive-no-blur");
                    driveTier(2); next();
                }
                case 6 -> {
                    check(SoundAudit.neonClicks>0 && SoundAudit.vanillaClicks==0,"Neon interactions play custom sounds without Minecraft menu clicks");
                    check(adaptive().tier()==2 && !effect("blur") && !effect("glow"),"adaptive tier two also disables decorative glow"); shot("adaptive-no-glow");
                    var q=adaptive(); long now=System.nanoTime()+10_000_000_000L;
                    for(int i=0;i<650;i++)q.frame(now+=8_340_000,1e9/120,true,false);
                    check(q.tier()==1,"adaptive recovery restores one effect tier after sustained headroom");
                    press("Full"); check(effect("blur")&&effect("glow"),"Full overrides adaptive reductions immediately");
                    q.frame(System.nanoTime(),1e9/120,false,true);
                    new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"),Neon.SAVES);
                    mc.gui.setScreen(menu=new SettingsScreen(null));
                    var b=menu.menuBounds(); var corner=mouse(b.right(),b.bottom()); menu.mouseClicked(corner,false);
                    menu.mouseDragged(mouse(b.right()-120,b.bottom()-70),-120,-70); menu.mouseReleased(corner);
                    press("Settings"); press("Appearance"); press("Background"); next();
                }
                case 7 -> {
                    scroller=widgets().stream().filter(w -> w.getClass().getSimpleName().equals("UiScroll")).map(w -> (AbstractScrollArea)w).findFirst().orElseThrow();
                    var shade=widgets().stream().filter(w -> w.getMessage().getString().equals("Saturation and brightness")).findFirst().orElseThrow();
                    mc.setLastInputType(InputType.MOUSE); double before=scroller.scrollAmount();
                    var e=mouse(shade.getX()+12,shade.getY()+12);
                    check(menu.mouseClicked(e,false),"visible gradient accepts mouse input in the minimum menu"); menu.mouseReleased(e);
                    check(scroller.scrollAmount()==before,"clicking a tall color fold does not scroll it out from under the pointer");
                    mc.setLastInputType(InputType.KEYBOARD_TAB);
                    for(int i=0;i<20 && !button("Paste").isFocused();i++)key(GLFW.GLFW_KEY_TAB,0);
                    check(button("Paste").isFocused() && button("Paste").getY()>=scroller.getY() && button("Paste").getBottom()<=scroller.getBottom(),"keyboard focus reveals the last inline picker action immediately without a scrollbar (focused="+button("Paste").isFocused()+", y="+button("Paste").getY()+".."+button("Paste").getBottom()+", viewport="+scroller.getY()+".."+scroller.getBottom()+")");
                    shot("minimum-picker-keyboard");
                    Neon.applyConfig(original); Neon.SAVES.flush().join(); mc.gui.setScreen(null); return true;
                }
            }
            return false;
        } catch(ReflectiveOperationException e) { throw new AssertionError(e); }
    }
}
