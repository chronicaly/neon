package me.xchro.neon.smoke;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.*;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Real screen input and frame-sampled animation on the packaged production JAR. */
public final class InteractionSmoke {
    private static InteractionSmoke active;
    private final Minecraft mc;
    private final NeonConfig original;
    private SettingsScreen menu;
    private int checks, variant, age, stage, fps, anchorY, samples, intermediate, lastHeight;
    private AbstractWidget measuredFold;
    private boolean measuring, monotonic;
    private me.xchro.neon.core.TimedValue sliderMotion;
    private double sliderFrom, sliderTarget, sliderLast;
    private int sliderSamples, sliderIntermediate;
    private boolean sliderMonotonic;
    InteractionSmoke(Minecraft mc, int checks) {
        this.mc = mc; this.checks = checks; original = ConfigStore.copy(Neon.config); active = this; setup();
    }
    int checks() { return checks; }
    private void check(boolean ok, String text) {
        if (!ok) throw new AssertionError("Interaction " + variant + "/" + stage + ": " + text);
        checks++; System.out.println("NEON_CHECK " + text);
    }
    private List<AbstractWidget> widgets(ContainerEventHandler root) {
        var result = new ArrayList<AbstractWidget>();
        for (var child : root.children()) {
            if (child instanceof AbstractWidget widget) result.add(widget);
            if (child instanceof ContainerEventHandler nested) result.addAll(widgets(nested));
        }
        return result;
    }
    private Button button(String name) { return widgets(menu).stream().filter(w -> w instanceof Button && w.getMessage().getString().equals(name)).map(w -> (Button) w).findFirst().orElseThrow(); }
    private AbstractScrollArea scroll() { return widgets(menu).stream().filter(w -> w.getClass().getSimpleName().equals("UiScroll")).map(w -> (AbstractScrollArea) w).findFirst().orElseThrow(); }
    private Object field(Object instance, String name) throws Exception { var f = instance.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(instance); }
    private double track(ExactSlider slider, String name) throws Exception { var m = ExactSlider.class.getDeclaredMethod(name); m.setAccessible(true); return ((Number)m.invoke(slider)).doubleValue(); }
    private ExactSlider slider() { return widgets(menu).stream().filter(w -> w instanceof ExactSlider).map(w -> (ExactSlider) w).findFirst().orElseThrow(); }
    private MouseButtonEvent pointer(PixelScreen screen, double x, double y, int button, int mods) { return new MouseButtonEvent(screen.guiCoordinate(x), screen.guiCoordinate(y), new MouseButtonInfo(button, mods)); }
    private MouseButtonEvent pointer(double x, double y, int mods) { return pointer(menu, x, y, 0, mods); }
    private void key(int key, int mods) { menu.keyPressed(new KeyEvent(key, 0, mods)); }
    private void undo() { key(GLFW.GLFW_KEY_Z, GLFW.GLFW_MOD_CONTROL); }
    private boolean focus(ContainerEventHandler group, GuiEventListener target) {
        for (var child : group.children()) if (child == target || child instanceof ContainerEventHandler nested && focus(nested, target)) { group.setFocused(child); return true; }
        return false;
    }
    private void resizeMinimum() {
        var b = menu.menuBounds(); var start = pointer(b.right()-1,b.bottom()-1,0);
        var end = pointer(b.right()-121,b.bottom()-71,0);
        menu.mouseClicked(start,false); menu.mouseDragged(end,end.x()-start.x(),end.y()-start.y()); menu.mouseReleased(end);
    }
    private void setup() {
        fps = new int[]{30,60,144}[Math.min(2, variant / 2)];
        mc.options.enableVsync().set(false); mc.options.framerateLimit().set(fps); mc.options.pauseOnLostFocus = false;
        mc.options.guiScale().set(variant < 6 ? variant / 2 + 1 : variant == 6 ? 1 : 3);
        mc.getWindow().setWindowed(variant < 6 ? 1280 : 640, variant < 6 ? 800 : 480); mc.resizeGui(); mc.setLastInputType(InputType.MOUSE);
        Neon.config.freelookDistance = 4;
        new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"), Neon.SAVES); Neon.SAVES.flush().join();
        mc.gui.setScreen(menu = Menus.fresh(null)); if (variant % 2 == 1) resizeMinimum();
        Menus.configure(menu, "Freelook"); stage = -3; age = 0;
    }
    private void measureSlider(double from, double target) throws Exception {
        sliderMotion = (me.xchro.neon.core.TimedValue) field(slider(), "motion");
        sliderFrom = sliderLast = from; sliderTarget = target;
        sliderSamples = sliderIntermediate = 0; sliderMonotonic = true;
    }
    private void checkSlider(String action) {
        check(sliderSamples > 0 && sliderIntermediate > 0 && sliderMonotonic && Math.abs(sliderLast - sliderTarget) < 1e-8,
            action + " renders intermediate motion without overshoot and settles at " + fps + " FPS");
        System.out.println("NEON_SLIDER fps=" + fps + " minimum=" + (variant % 2 == 1) + " action=" + action + " samples=" + sliderSamples + " intermediate=" + sliderIntermediate);
        sliderMotion = null;
    }
    private void layout() throws Exception {
        var frame = (me.xchro.neon.core.ExpandingMenu.Frame) field(menu, "frame");
        check(variant < 6 ? frame.fullDrawerWidth() == 300 : frame.fullDrawerWidth() < 300,
            "settings pane uses 300 units or fits the small display");
        check(menu.expandedBounds().left() >= 13 && menu.expandedBounds().right() <= menu.canvasWidth() - 13,
            "wider combined menu stays inside the display");
        for (var widget : widgets(menu)) if (widget instanceof ExactSlider s) {
            double left = track(s, "compactTrackLeft"), right = track(s, "compactTrackRight");
            check(s.getHeight() == 44 ? right - left == s.getWidth() - 6 : right - left >= 90,
                "numeric bar has 90 inline units or spans its stacked row");
            check(left >= s.getX() && right <= s.getRight(), "drawn track stays inside the numeric control");
        }
        check(variant < 6 ? slider().getHeight() == 27 : slider().getHeight() == 44,
            "labels stay inline in the wide pane and move above the bar on a small display");
    }
    private void excursions() throws Exception {
        double left = track(slider(), "compactTrackLeft"), width = track(slider(), "compactTrackRight") - left;
        double y = track(slider(), "compactTrackY");
        int sounds = SoundAudit.cues.size();
        var previous = pointer(left + width * .2, y, 0);
        menu.mouseClicked(previous, false);
        for (int repeat = 0; repeat < 2; repeat++) {
            double[] positions = {2, 1.2, .6, -1, -.2, .3, .5};
            double[] values = {16, 16, 10, 1, 1, 5.5, 8.5};
            for (int i = 0; i < positions.length; i++) {
                var next = pointer(left + width * positions[i], y, 0);
                menu.mouseDragged(next, next.x() - previous.x(), next.y() - previous.y()); previous = next;
                check(Neon.config.freelookDistance == values[i], "leaving and returning horizontally keeps the value under the pointer");
            }
        }
        var above = pointer(left + width * .7, -30, 0);
        menu.mouseDragged(above, above.x() - previous.x(), above.y() - previous.y());
        check(Neon.config.freelookDistance == 11.5, "captured drag follows horizontal position above the slider");
        var returned = pointer(left + width * .7, y, 0);
        menu.mouseDragged(returned, 0, returned.y() - above.y());
        check(Neon.config.freelookDistance == 11.5, "vertical re-entry does not offset the value");
        menu.mouseReleased(pointer(left + width * .7, menu.canvasHeight() + 30, 0));
        menu.mouseDragged(previous, previous.x() - returned.x(), 0);
        check(Neon.config.freelookDistance == 11.5, "outside release ends capture before subsequent movement");
        check(SoundAudit.cues.size() == sounds, "ordinary dragging and excursions remain silent");
        undo(); check(Neon.config.freelookDistance == 4, "all excursions undo as a single gesture");
    }
    private void precision() throws Exception {
        int sound = SoundAudit.neonEnables;
        ExactSlider slider = slider(); double left = track(slider,"compactTrackLeft"), span = track(slider,"compactTrackRight") - left, y = track(slider,"compactTrackY");
        var start = pointer(left+span/2,y,GLFW.GLFW_MOD_SHIFT);
        menu.mouseClicked(start,false); check(Neon.config.freelookDistance == 4,"Shift press starts at the exact current value");
        var fine = pointer(left+span/2+span*2/3,y,GLFW.GLFW_MOD_SHIFT);
        menu.mouseDragged(fine,fine.x()-start.x(),0); check(Neon.config.freelookDistance == 5,"Shift drag uses one-tenth sensitivity");
        var normal = pointer(left+span/2+span*2/3+span/15,y,0);
        menu.mouseDragged(normal,normal.x()-fine.x(),0); check(Neon.config.freelookDistance == 6,"releasing Shift continues without an absolute-position jump");
        var normalized = AbstractSliderButton.class.getDeclaredField("value"); normalized.setAccessible(true);
        check(Math.abs(normalized.getDouble(slider)-5.0/15)<1e-8,"thumb and applied numeric value agree");
        double scroll = scroll().scrollAmount(); menu.mouseScrolled(start.x(),start.y(),0,-10);
        check(scroll().scrollAmount()==scroll,"wheel cannot scroll during a value drag");
        menu.mouseReleased(pointer(left+span,menu.canvasHeight()+30,0));
        check(!(boolean)field(slider,"draggingValue"),"release outside the menu finishes the drag");
        undo(); check(Neon.config.freelookDistance==4,"one Undo restores the entire precision gesture");
        slider = slider(); menu.mouseClicked(start,false); menu.mouseDragged(fine,fine.x()-start.x(),0);
        key(GLFW.GLFW_KEY_ESCAPE,0); check(Neon.config.freelookDistance==4 && mc.gui.screen()==menu,"Escape cancels only the numeric gesture");
        slider = slider(); menu.mouseClicked(start,false); menu.mouseDragged(fine,fine.x()-start.x(),0);
        LiveSmoke.focused=false; menu.tick(); LiveSmoke.focused=true;
        check(Neon.config.freelookDistance==5 && !(boolean)field(slider,"draggingValue"),"focus loss retains the applied value and ends the drag");
        menu.mouseDragged(normal,normal.x()-fine.x(),0); check(Neon.config.freelookDistance==5,"focus return cannot resume an abandoned drag");
        undo();
        slider=slider(); menu.mouseClicked(pointer(menu,slider.getRight()-4,slider.getY()+10,1,0),false);
        InlineInput.field(menu).setValue("7.125"); key(GLFW.GLFW_KEY_ENTER,0);
        check(Neon.config.freelookDistance==7.125,"exact fractional input is retained");
        mc.setLastInputType(InputType.KEYBOARD_TAB); focus(menu,slider); key(GLFW.GLFW_KEY_RIGHT,0);
        check(Neon.config.freelookDistance==7.625,"Right uses the configured half-block increment from an exact value");
        key(GLFW.GLFW_KEY_LEFT,GLFW.GLFW_MOD_SHIFT); check(Neon.config.freelookDistance==2.625,"Shift+Left uses ten increments");
        mc.setLastInputType(InputType.MOUSE);
        check(SoundAudit.neonEnables==sound,"numeric input, focus and Undo never request enable cues");
    }
    private void navigation() throws Exception {
        scroll().setScrollAmount(Math.min(80,scroll().maxScrollAmount())); double position=scroll().scrollAmount();
        button("Combat").onPress(null); button("Autoclicker").onPress(null); button("Settings").onPress(null);
        check(field(menu,"module").equals("Colors") && scroll().scrollAmount()==position,"switching category restores its section and scroll position");
        button("Theme").onPress(null); menu.onClose(); mc.gui.setScreen(menu=new SettingsScreen(null));
        check((int)field(menu,"page")==2 && field(menu,"module").equals("Colors") && scroll().scrollAmount()==position,"reopening restores the current category, section, and scroll");
        check(widgets(menu).stream().noneMatch(w->w.getClass().getSimpleName().equals("ColorPicker")),"dismissed popups do not reopen");
        button("Visuals").onPress(null); check(field(menu,"module").equals("Freelook"),"each category keeps its own expanded section");
        button("HUD editor").onPress(null); check(mc.gui.screen() instanceof HudEditor,"HUD action still opens directly");
        var editor=(HudEditor)mc.gui.screen();
        var general=widgets(editor).stream().filter(w->w.getMessage().getString().equals("General")).findFirst().orElseThrow();
        var click=pointer(editor,general.getX()+20,general.getY()+10,0,0); editor.mouseClicked(click,false); editor.mouseReleased(click);
        var scale=widgets(editor).stream().filter(w->w instanceof ExactSlider).map(w->(ExactSlider)w).findFirst().orElseThrow();
        mc.setLastInputType(InputType.KEYBOARD_TAB); focus(editor,scale); mc.setLastInputType(InputType.MOUSE);
        double old=Neon.config.hudScale;
        var start=pointer(editor,scale.getX()+scale.getWidth()/2.0,scale.getY()+10,0,GLFW.GLFW_MOD_SHIFT);
        var end=pointer(editor,scale.getX()+scale.getWidth()/2.0+scale.getWidth()/3.0,scale.getY()+10,0,GLFW.GLFW_MOD_SHIFT);
        editor.mouseClicked(start,false); editor.mouseDragged(end,end.x()-start.x(),0); editor.mouseReleased(end);
        check(Math.abs(Neon.config.hudScale-old-.05)<1e-6,"HUD numeric rows share precision dragging");
        editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z,0,GLFW.GLFW_MOD_CONTROL)); check(Neon.config.hudScale==old,"HUD precision drag is one Undo operation");
        editor.onClose(); check(mc.gui.screen()==menu && field(menu,"module").equals("Freelook"),"returning from HUD preserves the menu section");
        button("Settings").onPress(null); scroll().setScrollAmount(scroll().maxScrollAmount());
        anchorY=button("Appearance").getY(); button("Appearance").onPress(null);
    }
    public static void frameEnd() {
        if (active==null) return;
        active.mc.getFramerateLimitTracker().onInputReceived();
        if (active.sliderMotion != null) {
            double value = active.sliderMotion.value(System.nanoTime(), 120_000_000L);
            active.sliderSamples++;
            double low = Math.min(active.sliderFrom, active.sliderTarget), high = Math.max(active.sliderFrom, active.sliderTarget);
            if (value > low + 1e-8 && value < high - 1e-8) active.sliderIntermediate++;
            if (value < low - 1e-8 || value > high + 1e-8 || (value - active.sliderLast) * Math.signum(active.sliderTarget - active.sliderFrom) < -1e-8) active.sliderMonotonic = false;
            active.sliderLast = value;
        }
        if (!active.measuring) return;
        int height=active.measuredFold.getHeight();
        active.samples++; if(height>0 && height<100)active.intermediate++;
        if(height<active.lastHeight)active.monotonic=false;
        active.lastHeight=height;
    }
    boolean run() throws Exception {
        if (++age<8)return false;
        if (stage == -3 || stage == 10) {
            var frame = (me.xchro.neon.core.ExpandingMenu.Frame) field(menu, "frame");
            if (frame.drawerWidth() < frame.fullDrawerWidth() - .01) return false;
        }
        if (stage == -3) {
            layout();
            measureSlider(.2, 1);
            menu.mouseClicked(pointer(track(slider(), "compactTrackRight"), track(slider(), "compactTrackY"), 0), false);
            check(Neon.config.freelookDistance == 16, "track click applies immediately before visual easing");
            stage++; age = 0; return false;
        }
        if (stage == -2) {
            checkSlider("Track click"); measureSlider(1, 0);
            var end = pointer(track(slider(), "compactTrackLeft"), track(slider(), "compactTrackY"), 0);
            menu.mouseDragged(end, end.x() - menu.guiCoordinate(track(slider(), "compactTrackRight")), 0);
            check(Neon.config.freelookDistance == 1, "drag reversal applies the endpoint immediately");
            stage++; age = 0; return false;
        }
        if (stage == -1) {
            checkSlider("Held drag reversal");
            menu.mouseReleased(pointer(track(slider(), "compactTrackLeft"), track(slider(), "compactTrackY"), 0));
            measureSlider(0, 1.0 / 3);
            focus(menu, slider()); key(GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_MOD_SHIFT);
            check(Neon.config.freelookDistance == 6, "keyboard increments apply immediately before visual easing");
            stage++; age = 0; return false;
        }
        if(stage==0){
            checkSlider("Keyboard nudge");
            Screenshot.grab(mc.gameDirectory, "neon-wide-sliders-module-" + variant + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {});
            undo();undo();excursions();precision();
            if (Boolean.getBoolean("neon.smoke.slidersOnly")) {
                button("Settings").onPress(null); Menus.configure(menu, "Appearance"); stage = 10; age = 0; return false;
            }
            button("Settings").onPress(null);button("Colors").onPress(null);stage++;age=0;return false;
        }
        if (stage == 10) {
            layout();
            Screenshot.grab(mc.gameDirectory, "neon-wide-sliders-settings-" + variant + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {});
            if (++variant < 8) { setup(); return false; }
            active = null; mc.gui.setScreen(null); Neon.applyConfig(original); mc.options.framerateLimit().set(120); return true;
        }
        if(stage==1){navigation();stage++;age=0;return false;}
        if(stage==2){
            check(Math.abs(button("Appearance").getY()-anchorY)<=1 || scroll().scrollAmount()==0 || scroll().scrollAmount()==scroll().maxScrollAmount(),"fold keeps its clicked header anchored unless the content boundary clamps it");
            mc.gui.setScreen(menu=Menus.fresh(null));button("Zoom").onPress(null);
            measuredFold=widgets(menu).stream().filter(w->w.getMessage().getString().equals("Zoom settings")).findFirst().orElseThrow();
            samples=intermediate=lastHeight=0;monotonic=measuring=true;stage++;age=0;return false;
        }
        measuring=false;
        check(samples>0 && intermediate>0 && monotonic && measuredFold.getHeight()>=100,"fold renders intermediate, monotonic motion and reaches full height at "+fps+" FPS");
        System.out.println("NEON_MOTION fps="+fps+" minimum="+(variant%2==1)+" samples="+samples+" intermediate="+intermediate);
        Screenshot.grab(mc.gameDirectory,"neon-interaction-"+fps+"-"+(variant%2==0?"default":"minimum")+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});
        if(++variant<6){setup();return false;}
        active=null;mc.gui.setScreen(null);Neon.applyConfig(original);mc.options.framerateLimit().set(120);return true;
    }
}
