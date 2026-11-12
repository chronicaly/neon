package me.xchro.neon.smoke;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Real pointer input after each fold settles, at default and minimum sizes. */
final class FreelookSettingsSmoke {
    private final Minecraft mc;
    private final NeonConfig original = ConfigStore.copy(Neon.config);
    private final String binding = Neon.freelook.saveString();
    private SettingsScreen menu;
    private int size, stage, age, checks, sounds;
    FreelookSettingsSmoke(Minecraft mc, int checks) { this.mc=mc; this.checks=checks; setup(); }
    int checks() { return checks; }
    private void check(boolean ok, String name) { if (!ok) throw new AssertionError(name); checks++; System.out.println("NEON_CHECK " + name); }
    private List<AbstractWidget> widgets(ContainerEventHandler parent) {
        var all=new ArrayList<AbstractWidget>();
        for(var child:parent.children()) { if(child instanceof AbstractWidget w) all.add(w); if(child instanceof ContainerEventHandler c) all.addAll(widgets(c)); }
        return all;
    }
    private UiButton button(String name) { return widgets(menu).stream().filter(w->w instanceof UiButton && w.getMessage().getString().equals(name)).map(w->(UiButton)w).findFirst().orElseThrow(); }
    private ExactSlider slider() { return widgets(menu).stream().filter(w->w instanceof ExactSlider).map(w->(ExactSlider)w).findFirst().orElseThrow(); }
    private MouseButtonEvent pointer(double x,double y) { return new MouseButtonEvent(menu.guiCoordinate(x),menu.guiCoordinate(y),new MouseButtonInfo(0,0)); }
    private void click(double x,double y) { var e=pointer(x,y); check(menu.mouseClicked(e,false),"Freelook control accepts native pointer at size " + size); menu.mouseReleased(e); }
    private void click(UiButton b) { click(b.getX()+b.getWidth()/2.0,b.getY()+b.getHeight()/2.0); }
    private int coordinate(String method) throws Exception { var m=ExactSlider.class.getDeclaredMethod(method); m.setAccessible(true); return (int)m.invoke(slider()); }
    private void undo() { menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z,0,GLFW.GLFW_MOD_CONTROL)); }
    private void setup() {
        new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"),Neon.SAVES); Neon.SAVES.flush().join();
        mc.getWindow().setWindowed(1280,800); mc.options.guiScale().set(size+1); mc.resizeGui(); mc.setLastInputType(InputType.MOUSE);
        Neon.config.freelookDistance=4; sounds=SoundAudit.neonEnables;
        mc.gui.setScreen(menu=Menus.fresh(null));
        if(size==1) { var b=menu.menuBounds(); var start=pointer(b.right()-1,b.bottom()-1); var end=pointer(b.right()-121,b.bottom()-71); menu.mouseClicked(start,false);menu.mouseDragged(end,end.x()-start.x(),end.y()-start.y());menu.mouseReleased(end); }
        click(button("Freelook")); age=stage=0;
    }
    boolean run() throws Exception {
        if(++age<8)return false; age=0;
        switch(stage++) {
            case 0 -> {
                check(slider().getMessage().getString().equals("4 blocks"),"Freelook opens Distance at its four-block default");
                check(menu.menuBounds().width()==(size==0?440:320),"Freelook control uses the requested default/minimum window");
                check(slider().getY()>button("Freelook").getBottom() && button("Zoom").getY()>=slider().getBottom(),"Distance expands inline between Freelook and Zoom");
                click(coordinate("compactTrackLeft"),coordinate("compactTrackY")); check(Neon.config.freelookDistance==1,"Distance slider reaches one block");
            }
            case 1 -> { click(coordinate("compactTrackRight"),coordinate("compactTrackY")); check(Neon.config.freelookDistance==16,"Distance slider reaches sixteen blocks"); undo(); check(Neon.config.freelookDistance==1,"Distance slider gesture undoes independently"); }
            case 2 -> { undo();check(Neon.config.freelookDistance==4,"Distance Undo restores the prior value"); }
            case 3 -> {
                click(slider().getRight()-2,coordinate("compactTrackY")); check(slider().isEditing(),"Distance exact value edits inline");
                InlineInput.field(menu).setValue("17"); check(!slider().finishInline(true),"Distance rejects out-of-range input");
                InlineInput.field(menu).setValue("7.125");menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER,0,0));check(Neon.config.freelookDistance==7.125,"Distance exact input preserves fractional blocks");
            }
            case 4 -> { click(button("Freelook")); check(widgets(menu).stream().noneMatch(w->w instanceof ExactSlider),"Freelook collapse removes setting input immediately"); }
            case 5 -> { click(button("Freelook")); }
            case 6 -> {
                check(slider().getMessage().getString().equals("7.125 blocks"),"Freelook reopening retains the exact distance");
                check(Neon.freelook.saveString().equals(binding) && SoundAudit.neonEnables==sounds,"Distance editing preserves its bind without requesting enable cues");
                if(++size<2) { setup();return false; }
                Neon.applyConfig(original);mc.gui.setScreen(null);return true;
            }
        }
        return false;
    }
}
