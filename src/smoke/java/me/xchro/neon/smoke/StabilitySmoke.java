package me.xchro.neon.smoke;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import me.xchro.neon.Neon;
import me.xchro.neon.config.ConfigStore;
import me.xchro.neon.ui.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** A timed, rendered soak. All inputs, clipboard and sounds stay in the verification client. */
final class StabilitySmoke {
    private Minecraft mc;
    private SettingsScreen menu;
    private int ticks,age,stage,cycles,checks,reloads;
    private boolean ready,done;
    private long started;
    private CompletableFuture<Void> reload;
    private void check(boolean value,String name){if(!value)throw new AssertionError("Stability "+cycles+"/"+stage+": "+name);checks++;}
    private List<AbstractWidget> widgets(ContainerEventHandler root){var result=new ArrayList<AbstractWidget>();for(var child:root.children()){if(child instanceof AbstractWidget w)result.add(w);if(child instanceof ContainerEventHandler group)result.addAll(widgets(group));}return result;}
    private AbstractWidget widget(String name){return widgets(menu).stream().filter(w->w.getMessage().getString().equals(name)).findFirst().orElseThrow();}
    private void press(String name){widgets(menu).stream().filter(w->w instanceof Button&&w.getMessage().getString().equals(name)).map(w->(Button)w).findFirst().orElseThrow().onPress(null);}
    private void click(AbstractWidget w){var e=new MouseButtonEvent(menu.guiCoordinate(w.getX()+5),menu.guiCoordinate(w.getY()+7),new MouseButtonInfo(0,0));menu.mouseClicked(e,false);menu.mouseReleased(e);}
    private static Object field(Object object,String name)throws Exception{Class<?> c=object instanceof Class<?> type?type:object.getClass();var f=c.getDeclaredField(name);f.setAccessible(true);return f.get(object instanceof Class<?>?null:object);}
    private void cacheBounds()throws Exception{
        var font=Class.forName("me.xchro.neon.ui.UiFont");
        for(String mapName:List.of("FACES","HUD_FACES"))for(Object face:((Map<?,?>)field(font,mapName)).values()){
            check(((Map<?,?>)field(face,"advances")).size()<=512,"advance cache remains bounded");
            check(((List<?>)field(face,"pages")).size()<=3,"atlas cache settles within its page budget");
        }
        var scratch=(java.awt.image.BufferedImage)((ThreadLocal<?>)field(Class.forName("me.xchro.neon.ui.TextRaster"),"SCRATCH")).get();
        check(scratch==null||(long)scratch.getWidth()*scratch.getHeight()<=1_048_576,"text scratch stays within 4 MiB");
    }
    void start(){ClientTickEvents.END_CLIENT_TICK.register(client->{
        mc=client;if(done||mc.player==null||++ticks<40)return;
        Path root=mc.gameDirectory.toPath();
        try{
            if(!ready){ready=true;started=System.nanoTime();mc.options.pauseOnLostFocus=false;mc.options.framerateLimit().set(60);Files.writeString(root.resolve("neon-stability.csv"),"cycles,elapsed_seconds,heap_used_bytes,checks,reloads\n");}
            if(reload!=null){if(!reload.isDone()||mc.gui.overlay()!=null)return;reload.join();reload=null;reloads++;}
            if(++age<6)return;age=0;
            switch(stage++){
                case 0->{
                    if(cycles%25==0){boolean small=(cycles/25)%2==1;mc.getWindow().setWindowed(small?640:1280,small?480:800);mc.options.guiScale().set(cycles/25%3+1);mc.resizeGui();}
                    mc.gui.setScreen(menu=Menus.fresh(null));press("Settings");press("Colors");
                    widgets(menu).stream().filter(w->w instanceof AbstractScrollArea).map(w->(AbstractScrollArea)w).reduce((a,b)->b).orElseThrow().setScrollAmount(150);
                }
                case 1->click(widget("Accent"));
                case 2->{
                    String before=ConfigStore.encode(Neon.config);mc.keyboardHandler.setClipboard(String.format("#%06X80",0x123400+cycles));press("Paste");
                    check(Neon.config.theme.accent==(0x80123400+cycles),"color changes during repeated rendered sessions");
                    menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z,0,GLFW.GLFW_MOD_CONTROL));
                    check(ConfigStore.encode(Neon.config).equals(before),"one Undo restores the entire palette");
                    menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE,0,0));
                }
                case 3->{press("HUD editor");check(mc.gui.screen() instanceof HudEditor,"direct HUD editor remains reachable");mc.gui.screen().onClose();check(mc.gui.screen()==menu,"HUD returns to the same menu");}
                case 4->{menu.onClose();check(mc.gui.screen()==null,"closing releases gameplay immediately");}
                case 5->{
                    cacheBounds();cycles++;stage=0;
                    if(cycles%25==0){Neon.SAVES.flush().join();System.gc();}
                    if(cycles%50==0){
                        long heap=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();double elapsed=(System.nanoTime()-started)/1e9;
                        Files.writeString(root.resolve("neon-stability.csv"),cycles+","+elapsed+","+heap+","+checks+","+reloads+"\n",StandardOpenOption.APPEND);
                        System.out.println("NEON_STABILITY cycles="+cycles+" checks="+checks+" elapsed="+elapsed+" heap="+heap);
                        if(cycles<200)reload=mc.reloadResourcePacks();
                    }
                    if(cycles==200){check(reloads==3,"three full resource reloads finish");Files.writeString(root.resolve("neon-verification.txt"),"PASS "+checks+" stability checks, 200 rendered menu/color/HUD cycles, 3 resource reloads, "+((System.nanoTime()-started)/1e9)+" seconds\n");done=true;mc.stop();}
                }
            }
        }catch(Throwable failure){failure.printStackTrace();done=true;try{Files.writeString(root.resolve("neon-verification.txt"),"FAIL "+failure);}catch(Exception ignored){}mc.stop();}
    });}
}
