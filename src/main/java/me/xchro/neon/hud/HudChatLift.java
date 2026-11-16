package me.xchro.neon.hud;

import me.xchro.neon.core.UiMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;

/** Temporary rendering clearance for chat. Saved placement and editor hitboxes never move. */
final class HudChatLift {
    private static final UiMotion motion=new UiMotion(0);
    private static final long DURATION=220_000_000L;
    private static Object world;
    private static boolean rendering;
    private static int screenHeight,clearance;
    private static double amount;
    private static final HudBounds[] widgets=new HudBounds[24];
    private static final int[] groups=new int[24],shifts=new int[24],tops=new int[24];
    private static int count;
    static void begin(boolean preview,int height) {
        var mc=Minecraft.getInstance();
        if(world!=mc.level||preview){motion.snap(0);world=mc.level;}
        boolean chat=!preview&&mc.gui.screen() instanceof ChatScreen;
        if(chat) {
            int scale=NeonHud.gameScale(),top=mc.getWindow().getGuiScaledHeight()-14;
            for(var child:mc.gui.screen().children())if(child instanceof EditBox input){top=input.getY()-2;break;}
            // Four physical pixels separate the outside HUD contour from the input background.
            clearance=Math.max(0,height-top*scale+4);
        }
        amount=motion.to(chat?1:0,System.nanoTime(),DURATION);
        screenHeight=height;rendering=!preview;count=0;
    }
    static boolean moving(){return rendering&&amount>0;}
    static void add(HudBounds bounds){widgets[count]=bounds;groups[count]=count;count++;}
    private static int root(int i){while(groups[i]!=i)i=groups[i];return i;}
    static void arrange(){
        for(int i=0;i<count;i++)for(int j=0;j<i;j++){
            var a=widgets[i];var b=widgets[j];
            int dx=Math.max(a.x()-b.x()-b.width(),b.x()-a.x()-a.width());
            int dy=Math.max(a.y()-b.y()-b.height(),b.y()-a.y()-a.height());
            if(dx<=6&&dy<=6)groups[root(i)]=root(j);
        }
        for(int i=0;i<count;i++){shifts[i]=0;tops[i]=screenHeight;}
        for(int i=0;i<count;i++){int group=root(i);var b=widgets[i];shifts[group]=Math.max(shifts[group],b.y()+b.height()-(screenHeight-clearance));tops[group]=Math.min(tops[group],b.y());}
    }
    static HudBounds place(HudBounds bounds) {
        if(!rendering||amount==0)return bounds;
        int overlap=Math.max(0,bounds.y()+bounds.height()-(screenHeight-clearance));
        // Joined panels keep their relative layout instead of compressing into each other.
        for(int i=0;i<count;i++)if(widgets[i].equals(bounds)){int group=root(i);overlap=Math.min(shifts[group],tops[group]);break;}
        int shift=Math.min(bounds.y(),(int)Math.round(overlap*amount));
        return shift==0?bounds:new HudBounds(bounds.x(),bounds.y()-shift,bounds.width(),bounds.height());
    }
    static void end(){rendering=false;java.util.Arrays.fill(widgets,0,count,null);count=0;}
}
