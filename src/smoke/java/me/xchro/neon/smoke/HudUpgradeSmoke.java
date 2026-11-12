package me.xchro.neon.smoke;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.core.UiMotion;
import me.xchro.neon.hud.*;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Runs against the packaged client: animated geometry and native screen input. */
final class HudUpgradeSmoke {
    private final Minecraft mc;
    private final NeonConfig original;
    private HudEditor editor;
    private int age, scenario, checks;
    HudUpgradeSmoke(Minecraft mc, int checks) {
        this.mc = mc; this.checks = checks; original = ConfigStore.copy(Neon.config); setup();
    }
    int checks() { return checks; }
    private void check(boolean ok, String name) {
        if (!ok) throw new AssertionError("HUD upgrade " + scenario + ": " + name);
        checks++; System.out.println("NEON_CHECK " + name);
    }
    private List<AbstractWidget> widgets(ContainerEventHandler parent) {
        var result = new ArrayList<AbstractWidget>();
        for (var child : parent.children()) {
            if (child instanceof AbstractWidget widget) result.add(widget);
            if (child instanceof ContainerEventHandler group) result.addAll(widgets(group));
        }
        return result;
    }
    private AbstractWidget named(String name) {
        return widgets(editor).stream().filter(w -> w.getMessage().getString().equals(name)).findFirst().orElseThrow();
    }
    private AbstractWidget fold() { return named("Armor settings"); }
    private UiMotion motion() throws Exception {
        var f = fold().getClass().getDeclaredField("opening"); f.setAccessible(true); return (UiMotion) f.get(fold());
    }
    private void sample(long millis) throws Exception {
        var f = UiMotion.class.getDeclaredField("started"); f.setAccessible(true); f.setLong(motion(), System.nanoTime() - millis * 1_000_000L);
        var tick = fold().getClass().getDeclaredMethod("tick"); tick.setAccessible(true); tick.invoke(fold());
        var layout = HudEditor.class.getDeclaredMethod("layoutColumn"); layout.setAccessible(true); layout.invoke(editor);
    }
    private void toggle() {
        click(named("Armor"),1);
    }
    private void click(AbstractWidget row,int button) {
        var event = new MouseButtonEvent(editor.guiCoordinate(row.getX()+16),editor.guiCoordinate(row.getY()+8),new MouseButtonInfo(button,0));
        check(editor.mouseClicked(event,false),"pointer activates "+row.getMessage().getString()); editor.mouseReleased(event);
    }
    private void key(int key,int modifiers) { editor.keyPressed(new KeyEvent(key,0,modifiers)); }
    private void setup() {
        new HudPanelPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-hud-window.json"),Neon.SAVES); Neon.SAVES.flush().join();
        Neon.config.resetHud(); mc.getWindow().setWindowed(scenario == 3 ? 640 : 1280, scenario == 3 ? 360 : 800);
        mc.options.guiScale().set(scenario == 3 ? 2 : scenario + 1); mc.resizeGui(); mc.setLastInputType(InputType.MOUSE);
        mc.gui.setScreen(editor = new HudEditor(null)); age = 0;
    }
    private HudBounds armor() { return NeonHud.pixelBounds(mc.getWindow().getWidth(),mc.getWindow().getHeight(),true); }
    private MouseButtonEvent dragArmor(double x, double y, int modifiers) {
        var b=armor(); int scale=editor.pixelScale();
        var start=new MouseButtonEvent((b.x()+b.width()/2.0)/scale,(b.y()+b.height()/2.0)/scale,new MouseButtonInfo(0,modifiers));
        var end=new MouseButtonEvent((x+b.width()/2.0)/scale,(y+b.height()/2.0)/scale,new MouseButtonInfo(0,modifiers));
        check(editor.mouseClicked(start,false),"canvas starts armor drag"); editor.mouseDragged(end,end.x()-start.x(),end.y()-start.y()); return end;
    }
    private double guide(String name) throws Exception { var field=HudEditor.class.getDeclaredField(name);field.setAccessible(true);return field.getDouble(editor); }
    boolean run() throws Exception {
        age++;
        if (age == 8) {
            int sound = SoundAudit.neonEnables, baseline = named("Totems").getY(), collapsed = editor.inspectorBounds().height();
            toggle(); check(fold().getHeight() < 20, "HUD fold starts without jumping");
            sample(35); int middle = fold().getHeight();
            check(middle > 20 && middle < 176, "HUD fold has intermediate height");
            check(named("Totems").getY() == baseline + middle, "following HUD roots move with the fold");
            var last = widgets((ContainerEventHandler)fold()).getLast();
            check(!fold().mouseClicked(new MouseButtonEvent(last.getX()+2,last.getBottom()-2,new MouseButtonInfo(0,0)),false), "unrevealed HUD controls reject clicks");
            sample(160); int full = fold().getHeight(); check(full == 176, "expanded armor retains eight compact 22-unit rows");
            toggle(); check(((ContainerEventHandler)fold()).children().isEmpty(), "closing HUD controls immediately stop receiving input");
            sample(35); int closing = fold().getHeight(); check(closing > 0 && closing < full, "HUD collapse interpolates height");
            toggle(); check(Math.abs(fold().getHeight()-closing)<12, "HUD fold reversal preserves current height"); sample(160);
            toggle(); sample(160); check(fold().getHeight()==0 && named("Totems").getY()==baseline && editor.inspectorBounds().height()==collapsed, "collapse restores exact original panel geometry");
            toggle(); toggle(); sample(160); check(fold().getHeight()==0, "immediate double fold leaves no detached animation");
            toggle(); mc.setLastInputType(InputType.KEYBOARD_TAB);
            var group = (ContainerEventHandler)fold(); group.setFocused(group.children().getLast());
            var scroll = widgets(editor).stream().filter(w->w instanceof AbstractScrollArea && w.getClass().getSimpleName().equals("UiScroll")).findFirst().orElseThrow();
            ((ContainerEventHandler)scroll).setFocused(fold());
            check(fold().getHeight()==full, "keyboard focus reveals the HUD fold immediately");
            check(((AbstractWidget)group.children().getLast()).getBottom()<=scroll.getBottom(), "keyboard focus reveals the last HUD row");
            mc.setLastInputType(InputType.MOUSE); ((AbstractScrollArea)scroll).setScrollAmount(0);
            check(SoundAudit.neonEnables==sound, "HUD expansion never requests enable cues");
        }
        if (age==12) Screenshot.grab(mc.gameDirectory,"neon-hud-upgrade-"+scenario+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});
        if (age==13) {
            editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_H,0,0));
            Neon.config.totems.visible=false; Neon.config.editorSnap=true; Neon.config.snapSensitivity=8;
            double center=(mc.getWindow().getWidth()-armor().width())/2.0;
            var end=dragArmor(center-3,mc.getWindow().getHeight()/3.0,0);
            check(armor().x()==Math.round(center),"canvas centers armor using physical-pixel snapping");
            check(guide("guideX")==mc.getWindow().getWidth()/2.0,"canvas guide marks the aligned screen center");
            editor.mouseReleased(end);
            end=dragArmor(center-3,armor().y(),GLFW.GLFW_MOD_ALT);
            check(armor().x()==Math.round(center-3) && guide("guideX")==-1,"Alt bypasses snapping and its guide"); editor.mouseReleased(end);
            Neon.config.totemFloating=true; Neon.config.totems.x=.78; Neon.config.totems.y=.55;
            var neighbor=NeonHud.totemPixelBounds(mc.getWindow().getWidth(),mc.getWindow().getHeight());
            double target=neighbor.x()-armor().width()+2;
            end=dragArmor(target,neighbor.y(),0);
            check(armor().x()==Math.round(target),"disabled totems do not attract canvas snapping"); editor.mouseReleased(end);
            Neon.config.totems.visible=true;
            end=dragArmor(target,neighbor.y(),0);
            check(armor().x()+armor().width()==neighbor.x() && guide("guideX")==neighbor.x(),"enabled totems support cross-edge alignment and the correct guide");
        }
        if(age==15) Screenshot.grab(mc.gameDirectory,"neon-hud-snap-"+scenario+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});
        if(age==18) {
            editor.mouseReleased(new MouseButtonEvent(0,0,new MouseButtonInfo(0,0)));
            Neon.config.resetHud(); mc.gui.setScreen(editor=new HudEditor(null)); click(named("Totems"),1);
        }
        if(age==25) {
            check(widgets(editor).stream().noneMatch(w->w instanceof ExactSlider),"attached totem has no editable scale");
            var attached=NeonHud.totemPixelBounds(mc.getWindow().getWidth(),mc.getWindow().getHeight());
            editor.mouseScrolled((attached.x()+attached.width()/2.0)/editor.pixelScale(),(attached.y()+attached.height()/2.0)/editor.pixelScale(),0,1);
            check(!Neon.config.totemFloating,"wheel scaling never detaches the real offhand count");
            click(named("Mode: Offhand"),0);
            check(Neon.config.totemFloating && named("Mode: Floating")!=null,"explicit mode change updates the visible label immediately");
            var scale=widgets(editor).stream().filter(w->w instanceof ExactSlider).map(w->(ExactSlider)w).findFirst().orElseThrow();
            click(scale,1); key(GLFW.GLFW_KEY_A,GLFW.GLFW_MOD_CONTROL);
            "1.75".codePoints().forEach(cp->editor.charTyped(new CharacterEvent(cp))); key(GLFW.GLFW_KEY_ENTER,0);
            check(Neon.config.totems.scale==1.75,"floating totem retains inline exact scale editing inside an animated fold");
            click(named("Mode: Floating"),0);
            check(!Neon.config.totemFloating && widgets(editor).stream().noneMatch(w->w instanceof ExactSlider),"reattaching removes the scale row immediately");
            int factor=editor.pixelScale();
            var start=new MouseButtonEvent((attached.x()+attached.width()/2.0)/factor,(attached.y()+attached.height()/2.0)/factor,new MouseButtonInfo(0,0));
            var end=new MouseButtonEvent(mc.getWindow().getWidth()*.7/factor,mc.getWindow().getHeight()*.5/factor,new MouseButtonInfo(0,GLFW.GLFW_MOD_ALT));
            check(editor.mouseClicked(start,false),"attached totem accepts direct dragging"); editor.mouseDragged(end,end.x()-start.x(),end.y()-start.y());
            check(Neon.config.totemFloating && !editor.controlsVisible(),"direct drag detaches totem and hides controls"); editor.mouseReleased(end);
            check(named("Mode: Floating")!=null,"drag completion refreshes the placement mode");
            key(GLFW.GLFW_KEY_Z,GLFW.GLFW_MOD_CONTROL);
            check(!Neon.config.totemFloating && named("Mode: Offhand")!=null && attached.equals(NeonHud.totemPixelBounds(mc.getWindow().getWidth(),mc.getWindow().getHeight())),"one Undo restores the full offhand attachment and its row state");
        }
        if(age==28) Screenshot.grab(mc.gameDirectory,"neon-hud-totem-"+scenario+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});
        if(age==30) {
            var bounds=editor.inspectorBounds();
            var start=new MouseButtonEvent(editor.guiCoordinate(bounds.left()+20),editor.guiCoordinate(bounds.top()+10),new MouseButtonInfo(0,0));
            var end=new MouseButtonEvent(editor.guiCoordinate(editor.canvasWidth()-bounds.width()-24+20),editor.guiCoordinate(editor.canvasHeight()-bounds.height()-24+10),new MouseButtonInfo(0,0));
            check(editor.mouseClicked(start,false),"HUD header starts panel movement"); editor.mouseDragged(end,end.x()-start.x(),end.y()-start.y()); editor.mouseReleased(end);
            var placed=editor.inspectorBounds();
            // Reopening before the background writer flushes must see the newest position.
            mc.gui.setScreen(editor=new HudEditor(null));
            check(editor.inspectorBounds().left()==placed.left() && editor.inspectorBounds().top()==placed.top(),"HUD column immediately reopens at its saved position");
            Neon.SAVES.flush().join();
            var saved=HudPanelPlacement.load(mc.gameDirectory.toPath().resolve("config/neon-hud-window.json")).fit(editor.canvasWidth(),editor.canvasHeight(),placed.width(),111);
            check(saved.x()==placed.left() && saved.y()==placed.top(),"HUD panel placement survives disk reload");
            Neon.applyConfig(new NeonConfig()); mc.gui.setScreen(editor=new HudEditor(null));
            check(editor.inspectorBounds().left()==placed.left() && editor.inspectorBounds().top()==placed.top(),"loading a module configuration does not move the HUD column");
            bounds=editor.inspectorBounds(); start=new MouseButtonEvent(editor.guiCoordinate(bounds.left()+20),editor.guiCoordinate(bounds.top()+10),new MouseButtonInfo(0,0));
            end=new MouseButtonEvent(start.x()-20,start.y()-20,new MouseButtonInfo(0,0));
            editor.mouseClicked(start,false); editor.mouseDragged(end,-20,-20); key(GLFW.GLFW_KEY_ESCAPE,0);
            mc.gui.setScreen(editor=new HudEditor(null));
            check(editor.inspectorBounds().left()==placed.left() && editor.inspectorBounds().top()==placed.top(),"Escape cancels panel movement without overwriting saved position");
            mc.getWindow().setWindowed(640,360); mc.resizeGui();
        }
        if(age==34) {
            var b=editor.inspectorBounds(); check(b.left()>=8 && b.top()>=8 && b.right()<=editor.canvasWidth()-8 && b.bottom()<=editor.canvasHeight()-8,"saved HUD column clamps inside a smaller display");
            Screenshot.grab(mc.gameDirectory,"neon-hud-placement-"+scenario+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});
        }
        if (age<36) return false;
        if (++scenario<4) { setup(); return false; }
        Neon.applyConfig(original); mc.gui.setScreen(null); return true;
    }
}
