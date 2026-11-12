package me.xchro.neon.smoke;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.*;
import me.xchro.neon.Neon;
import me.xchro.neon.ui.SettingsScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/** Opt-in native desktop input; sound and clipboard interception remain enabled. */
public final class DesktopSmoke {
    private static final int PRIMARY=Integer.getInteger("neon.desktop.primary",GLFW.GLFW_KEY_INSERT);
    private static final int ALTERNATE=Integer.getInteger("neon.desktop.alternate",GLFW.GLFW_KEY_RIGHT_SHIFT);
    public static int mouseEvents,keyEvents;
    private static int insertPresses,alternatePresses,chatShortcuts;
    private static boolean chatPending;
    private static int lastKey,lastAction;
    private int ticks,opens,closes,focusLosses,chatChecks;
    private boolean ready,done,wasMenu,wasFocused,exactValue;
    public static boolean enabled(){return Boolean.getBoolean("neon.smoke.desktopOnly");}
    public static void key(int action,KeyEvent key){
        lastKey=key.key();lastAction=action;
        System.out.println("NEON_NATIVE_KEY key="+key.key()+" action="+action+" scan="+key.scancode()+" modifiers="+key.modifiers());
        if(action!=GLFW.GLFW_PRESS)return;
        if(key.key()==PRIMARY)insertPresses++;
        if(key.key()==ALTERNATE)alternatePresses++;
        if((key.key()==PRIMARY||key.key()==ALTERNATE)&&Minecraft.getInstance().gui.screen() instanceof ChatScreen){chatShortcuts++;chatPending=true;}
    }
    void start(){ClientTickEvents.END_CLIENT_TICK.register(mc->{
        if(done||mc.player==null||++ticks<40)return;
        Path root=mc.gameDirectory.toPath();
        try{
            if(!ready){
                ready=true;Files.deleteIfExists(root.resolve("neon-desktop-finish"));
                mc.options.pauseOnLostFocus=false;mc.options.framerateLimit().set(60);mc.options.guiScale().set(2);
                mc.getWindow().setWindowed(1280,800);mc.resizeGui();
                Neon.settings.setKey(InputConstants.Type.KEYSYM.getOrCreate(PRIMARY));
                Neon.settingsAlternate.setKey(InputConstants.Type.KEYSYM.getOrCreate(ALTERNATE));KeyMapping.resetMapping();
                Menus.reset();mc.gui.setScreen(null);wasFocused=mc.isWindowActive();
                var monitors=GLFW.glfwGetMonitors();var inventory=new com.google.gson.JsonArray();
                if(monitors!=null)for(int i=0;i<monitors.limit();i++){
                    long monitor=monitors.get(i);float[] x={0},y={0};GLFW.glfwGetMonitorContentScale(monitor,x,y);
                    var info=new JsonObject();info.addProperty("name",GLFW.glfwGetMonitorName(monitor));info.addProperty("scaleX",x[0]);info.addProperty("scaleY",y[0]);inventory.add(info);
                }
                Files.writeString(root.resolve("neon-desktop-monitors.json"),inventory.toString());
            }
            boolean menu=mc.gui.screen() instanceof SettingsScreen,focused=mc.isWindowActive();
            if(menu&&!wasMenu)opens++;if(!menu&&wasMenu)closes++;if(!focused&&wasFocused)focusLosses++;
            wasMenu=menu;wasFocused=focused;exactValue|=Neon.config.zoomFactor==4.125;
            if(chatPending){if(!(mc.gui.screen() instanceof ChatScreen))throw new AssertionError("Native shortcut escaped focused chat");chatChecks++;chatPending=false;}
            if(ticks%20==0){
                mc.getWindow().setTitle("Neon native verification — muted");
                var status=new JsonObject();status.addProperty("screen",mc.gui.screen()==null?"gameplay":mc.gui.screen().getClass().getSimpleName());status.addProperty("focused",focused);
                status.addProperty("mouseEvents",mouseEvents);status.addProperty("keyEvents",keyEvents);status.addProperty("primaryKey",PRIMARY);status.addProperty("alternateKey",ALTERNATE);status.addProperty("primaryPresses",insertPresses);status.addProperty("alternatePresses",alternatePresses);
                status.addProperty("lastKey",lastKey);status.addProperty("lastAction",lastAction);
                status.addProperty("opens",opens);status.addProperty("closes",closes);status.addProperty("focusLosses",focusLosses);status.addProperty("chatChecks",chatChecks);status.addProperty("exactValue",exactValue);status.addProperty("zoom",Neon.config.zoomFactor);
                Files.writeString(root.resolve("neon-desktop-status.json"),status.toString());
            }
            if(Files.exists(root.resolve("neon-desktop-finish"))){
                if(mouseEvents<20||keyEvents<20||insertPresses<2||alternatePresses<2||opens<2||closes<2||focusLosses<1||chatChecks<2||chatShortcuts<2||!exactValue)
                    throw new AssertionError("Native coverage incomplete; inspect neon-desktop-status.json");
                Files.writeString(root.resolve("neon-verification.txt"),"PASS native desktop input: configured menu keys "+PRIMARY+"/"+ALTERNATE+", chat priority, exact numeric entry, pointer activity and actual focus round trip\n");done=true;mc.stop();
            }
            if(ticks>24000)throw new AssertionError("Native input session timed out");
        }catch(Throwable failure){failure.printStackTrace();done=true;try{Files.writeString(root.resolve("neon-verification.txt"),"FAIL "+failure);}catch(Exception ignored){}mc.stop();}
    });}
}
