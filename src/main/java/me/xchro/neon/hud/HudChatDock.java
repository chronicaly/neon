package me.xchro.neon.hud;

import me.xchro.neon.Neon;
import me.xchro.neon.core.UiMotion;
import me.xchro.neon.ui.HudEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;

/** Bottom-left chat/coordinates share a collision-free stack and one render-frame displacement. */
public final class HudChatDock {
    private static final UiMotion motion=new UiMotion(0);
    private static Object world,screen;
    private static long frame,computed=-1;
    private static int offset,clearance;
    private HudChatDock(){}
    static void frame(long now){frame=now;}
    public static boolean active(){
        var chat=Neon.config.hud(HudKind.CHAT).placement;var coordinates=Neon.config.hud(HudKind.COORDINATES).placement;
        return chat.visible&&coordinates.visible&&chat.x<=.1&&coordinates.x<=.1&&chat.y>=.8&&coordinates.y>=.8;
    }
    /** Keep a six-pixel gap, shortening the upper panel if the display cannot fit both. */
    public static HudBounds stack(HudBounds chat,HudBounds coordinates){
        int bottom=Math.max(0,Math.min(chat.y()+chat.height(),coordinates.y()-6));
        int height=Math.min(chat.height(),bottom);
        return new HudBounds(chat.x(),bottom-height,chat.width(),height);
    }
    static HudBounds coordinates(int width,int height,boolean preview){return HudWidgets.get(HudKind.COORDINATES).bounds(width,height,preview);}
    static int offset(){
        var mc=Minecraft.getInstance();var current=mc.gui.screen();
        if(world!=mc.level||!active()||current instanceof HudEditor){motion.snap(0);world=mc.level;computed=-1;return offset=0;}
        if(computed==frame&&screen==current)return offset;
        computed=frame;screen=current;
        int height=mc.getWindow().getHeight();
        if(current instanceof ChatScreen){
            int top=mc.getWindow().getGuiScaledHeight()-14;
            for(var child:current.children())if(child instanceof EditBox input){top=input.getY()-2;break;}
            var b=coordinates(mc.getWindow().getWidth(),height,false);
            clearance=Math.max(0,b.y()+b.height()-top*NeonHud.gameScale()+4);
        }
        offset=(int)Math.round(clearance*motion.to(current instanceof ChatScreen?1:0,System.nanoTime(),220_000_000L));return offset;
    }
    static HudBounds place(HudBounds bounds){
        int shift=Math.min(offset(),bounds.y()+bounds.height()),top=Math.max(0,bounds.y()-shift);
        return new HudBounds(bounds.x(),top,bounds.width(),Math.max(0,bounds.y()+bounds.height()-shift-top));
    }
}
