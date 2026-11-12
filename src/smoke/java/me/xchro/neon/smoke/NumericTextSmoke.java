package me.xchro.neon.smoke;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.HudPanelPlacement;
import me.xchro.neon.ui.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;

/** Measures cold values, a repeated full drag, and idle rendering on the production JAR. */
public final class NumericTextSmoke {
    private static final com.sun.management.ThreadMXBean CPU=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
    private static NumericTextSmoke active;
    private static boolean inUi;
    private static long cpuStart,wallStart,allocStart,rasterStart,cpu,wall,allocated,rasters,rasterNanos,uploadBytes;
    private final StringBuilder csv=new StringBuilder("phase,frame,ui_cpu_us,ui_wall_us,ui_alloc_bytes,raster_calls,raster_us,upload_bytes\n");
    private final String[] phases={"cold-drag","cached-drag","idle"};
    private Minecraft mc;
    private HudEditor editor;
    private ExactSlider slider;
    private int ticks,phase,frame;
    private boolean ready,done;
    public void start() { active=this; ClientTickEvents.END_CLIENT_TICK.register(this::tick); }
    private static List<AbstractWidget> widgets(ContainerEventHandler root) {
        var list=new ArrayList<AbstractWidget>();
        for(var child:root.children()) { if(child instanceof AbstractWidget w)list.add(w);if(child instanceof ContainerEventHandler c)list.addAll(widgets(c)); }
        return list;
    }
    private void tick(Minecraft client) {
        mc=client; if(mc.player==null||mc.level==null||done)return;
        if(++ticks==40) {
            mc.options.pauseOnLostFocus=false;mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);
            mc.getWindow().setWindowed(1280,800);mc.options.guiScale().set(2);mc.resizeGui();mc.setLastInputType(InputType.MOUSE);
            new HudPanelPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-hud-window.json"),Neon.SAVES);Neon.SAVES.flush().join();
            Neon.config.resetHud();mc.gui.setScreen(editor=new HudEditor(null));
            var row=widgets(editor).stream().filter(w->w.getMessage().getString().equals("Armor")).findFirst().orElseThrow();
            var event=new MouseButtonEvent(editor.guiCoordinate(row.getX()+16),editor.guiCoordinate(row.getY()+8),new MouseButtonInfo(1,0));
            editor.mouseClicked(event,false);editor.mouseReleased(event);
        }
        if(ticks==60) {
            slider=widgets(editor).stream().filter(w->w instanceof ExactSlider).map(w->(ExactSlider)w).findFirst().orElseThrow();
            try {
                Path jar=Path.of(Neon.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar)));
                Files.writeString(mc.gameDirectory.toPath().resolve("neon-numeric-jar.txt"),jar+"\nSHA256="+hash+"\n");
            }catch(Exception e){throw new AssertionError(e);}
            ready=true;
        }
    }
    public static void uiStart() {
        if(active==null||!active.ready||active.done)return;
        inUi=true;cpuStart=CPU.getCurrentThreadCpuTime();wallStart=System.nanoTime();allocStart=CPU.getThreadAllocatedBytes(Thread.currentThread().threadId());
    }
    public static void uiEnd() {
        if(!inUi)return;cpu+=CPU.getCurrentThreadCpuTime()-cpuStart;wall+=System.nanoTime()-wallStart;
        allocated+=CPU.getThreadAllocatedBytes(Thread.currentThread().threadId())-allocStart;inUi=false;
    }
    public static void rasterStart(){if(inUi)rasterStart=System.nanoTime();}
    public static void rasterEnd(){if(inUi){rasters++;rasterNanos+=System.nanoTime()-rasterStart;}}
    public static void upload(int w,int h,int bytes){if(inUi)uploadBytes+=(long)w*h*bytes;}
    public static void frameStart() {
        if(active==null||!active.ready||active.done)return;
        active.mc.getFramerateLimitTracker().onInputReceived();
        if(active.phase<2&&active.frame%3==0) {
            var s=active.slider;var e=active.editor;
            var event=new MouseButtonEvent(e.guiCoordinate(s.getX()+s.getWidth()*(active.frame/3)/50.0),e.guiCoordinate(s.getY()+8),new MouseButtonInfo(0,0));
            if(active.frame==0)e.mouseClicked(event,false);else e.mouseDragged(event,0,0);
        }
    }
    public static void frameEnd() {
        if(active==null||!active.ready||active.done)return;
        var a=active;
        a.csv.append(String.format(Locale.ROOT,"%s,%d,%.3f,%.3f,%d,%d,%.3f,%d%n",a.phases[a.phase],a.frame,cpu/1e3,wall/1e3,allocated,rasters,rasterNanos/1e3,uploadBytes));
        cpu=wall=allocated=rasters=rasterNanos=uploadBytes=0;
        if(++a.frame==(a.phase<2?153:120)) {
            a.editor.mouseReleased(new MouseButtonEvent(0,0,new MouseButtonInfo(0,0)));a.frame=0;
            if(++a.phase==3)a.finish();
        }
    }
    private void finish() {
        done=true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("neon-numeric-text.csv"),csv);
            Files.writeString(mc.gameDirectory.toPath().resolve("neon-verification.txt"),"PASS numeric text profile: 51 cold values, 51 cached values, 120 idle frames\n");
        }catch(Exception e){throw new AssertionError(e);}
        mc.stop();
    }
}
