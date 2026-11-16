package me.xchro.neon.hud;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.ThemeColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared HUD surface collection, with one animation clock and global/local visibility gates. */
public final class HudChrome {
    private static final List<HudGlassRenderer.Surface> surfaces=new ArrayList<>(24);
    private static boolean collecting,content;
    private static long frameTime;
    public static void frame(long now){frameTime=now;}
    public static boolean backgrounds(){return Neon.config.hudSurfaces&&Neon.config.hudBackgrounds;}
    public static boolean outlines(){return Neon.config.hudSurfaces&&Neon.config.hudOutlines;}
    public static void begin(){surfaces.clear();collecting=true;content=false;}
    public static void flush(GuiGraphicsExtractor g){HudGlassRenderer.draw(g,List.copyOf(surfaces),frameTime,Neon.config.hudAnimatedOutline);collecting=false;content=true;}
    public static void end(){collecting=content=false;surfaces.clear();}
    public void draw(GuiGraphicsExtractor g,HudBounds b){panel(g,b,true,true);}
    public void panel(GuiGraphicsExtractor g,HudBounds b,boolean background,boolean border){
        var t=Neon.config.theme;panel(g,b,t.color(ThemeColor.BACKGROUND),t.color(ThemeColor.OUTLINE),t.color(ThemeColor.SECONDARY),background,border);
    }
    public void border(GuiGraphicsExtractor g,HudBounds b,int primary,int secondary){panel(g,b,0,primary,secondary,false,true);}
    public void panel(GuiGraphicsExtractor g,HudBounds b,int background,int primary,int secondary,boolean fill,boolean outline){
        if(content||b.width()<=0||b.height()<=0)return;
        fill &= backgrounds();outline &= outlines();
        if(!fill&&!outline)return;
        int plate=(background&0xffffff)|Math.round((background>>>24)*.74f)<<24;
        var surface=new HudGlassRenderer.Surface(b,plate,primary,secondary,fill,outline);
        if(collecting)surfaces.add(surface);
        else HudGlassRenderer.draw(g,List.of(surface),frameTime==0?System.nanoTime():frameTime,Neon.config.hudAnimatedOutline);
    }
}
