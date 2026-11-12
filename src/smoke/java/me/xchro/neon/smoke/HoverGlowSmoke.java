package me.xchro.neon.smoke;

import java.awt.image.BufferedImage;
import java.util.*;
import javax.imageio.ImageIO;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;

/** Checks the actual Gaussian pixels outside hovered buttons and their fade back to idle. */
public final class HoverGlowSmoke {
    private static HoverGlowSmoke active;
    private final Minecraft mc;
    private final NeonConfig original=ConfigStore.copy(Neon.config);
    private SettingsScreen menu;
    private int variant,stage,age,checks;
    private boolean hovering,pending,done;
    private int[] idle;
    private String failure;
    HoverGlowSmoke(Minecraft mc,int checks){this.mc=mc;this.checks=checks;active=this;setup();}
    int checks(){return checks;}
    private List<AbstractWidget> widgets(ContainerEventHandler root){var list=new ArrayList<AbstractWidget>();for(var child:root.children()){if(child instanceof AbstractWidget w)list.add(w);if(child instanceof ContainerEventHandler c)list.addAll(widgets(c));}return list;}
    private AbstractWidget target(){String name=variant<2?"Freelook":"Colors";return widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiModuleTile")&&w.getMessage().getString().equals(name)).findFirst().orElseThrow();}
    private void check(boolean ok,String text){if(!ok)throw new AssertionError("Hover "+variant+"/"+stage+": "+text);checks++;System.out.println("NEON_CHECK "+text);}
    public static int[] pointer(){if(active==null)return null;if(!active.hovering)return new int[]{-10000,-10000};var tile=active.target();return new int[]{tile.getX()+tile.getWidth()/2,tile.getY()+tile.getHeight()/2};}
    private MouseButtonEvent mouse(double x,double y){return new MouseButtonEvent(menu.guiCoordinate(x),menu.guiCoordinate(y),new MouseButtonInfo(0,0));}
    private void setup(){
        Neon.config=new NeonConfig();Neon.config.theme.effects=NeonConfig.Effects.FULL;
        Neon.config.theme.opacity=1; // Stable background for framebuffer comparisons, not a saved default.
        new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"),Neon.SAVES);Neon.SAVES.flush().join();
        mc.getWindow().setWindowed(1280,800);mc.options.guiScale().set(new int[]{1,3,2,1}[variant]);mc.resizeGui();mc.setLastInputType(InputType.MOUSE);
        mc.gui.setScreen(menu=Menus.fresh(null));
        if(variant%2==1){var b=menu.menuBounds();var from=mouse(b.right()-1,b.bottom()-1);var to=mouse(b.right()-121,b.bottom()-71);menu.mouseClicked(from,false);menu.mouseDragged(to,to.x()-from.x(),to.y()-from.y());menu.mouseReleased(to);}
        if(variant>=2)widgets(menu).stream().filter(w->w instanceof Button&&w.getMessage().getString().equals("Settings")).map(w->(Button)w).findFirst().orElseThrow().onPress(null);
        Menus.configure(menu,variant<2?"Zoom":"Appearance");
        stage=age=0;hovering=pending=done=false;
    }
    private void capture(){
        pending=true;var tile=target();float scale=PixelScreen.UI_SCALE;
        int x=Math.round(tile.getX()*scale),y=Math.round(tile.getY()*scale),right=Math.round(tile.getRight()*scale),bottom=Math.round(tile.getBottom()*scale);
        int[][] samples={{x-3,(y+bottom)/2},{right+2,(y+bottom)/2},{(x+right)/2,y-5},{(x+right)/2,bottom+1}};
        var bounds=menu.expandedBounds();String name="neon-hover-"+(variant<2?"modules":"settings")+"-"+(variant%2==0?"default":"minimum")+"-"+new String[]{"idle","hover","leave"}[stage]+".png";
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{
            try(image){
                int[] colors=new int[samples.length];for(int i=0;i<colors.length;i++)colors[i]=image.getPixel(samples[i][0],samples[i][1]);
                if(stage==0)idle=colors;
                else if(stage==1){
                    for(int i=0;i<colors.length;i++)check(gain(colors[i],idle[i])>12,"soft bloom remains visible outside button edge "+i+" (gain "+gain(colors[i],idle[i])+")");
                    check(channel(colors[0],8)>channel(colors[1],8)+8,"hover bloom transitions from cyan to violet across the button");
                }else for(int i=0;i<colors.length;i++)check(Math.abs(gain(colors[i],idle[i]))<=4,"hover bloom fades completely at edge "+i);
                int left=Math.round(bounds.left()*scale),top=Math.round(bounds.top()*scale),w=Math.round(bounds.right()*scale)-left,h=Math.round(bounds.bottom()*scale)-top;
                var crop=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
                for(int cy=0;cy<h;cy++)for(int cx=0;cx<w;cx++)crop.setRGB(cx,cy,image.getPixel(left+cx,top+cy)|0xff000000);
                ImageIO.write(crop,"png",mc.gameDirectory.toPath().resolve("screenshots").resolve(name).toFile());
                done=true;
            }catch(Throwable error){failure=error.toString();}
        });
    }
    private int channel(int color,int shift){return color>>>shift&255;}
    private int gain(int color,int baseline){return Math.max(channel(color,16)-channel(baseline,16),Math.max(channel(color,8)-channel(baseline,8),channel(color,0)-channel(baseline,0)));}
    boolean run(){
        if(failure!=null)throw new AssertionError(failure);
        if(++age<14)return false;
        if(!pending){capture();return false;}if(!done)return false;
        if(stage<2){hovering=stage==0;stage++;age=0;pending=done=false;return false;}
        var tiles=widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiModuleTile")).toList();
        check(tiles.stream().map(AbstractWidget::getWidth).distinct().count()==1,"hover preserves equal button widths");
        check(tiles.stream().allMatch(w->w.getHeight()==26),"hover preserves compact button height");
        if(variant%2==0)check(tiles.stream().map(AbstractWidget::getX).distinct().count()==2,"default grid retains two columns with bloom padding");
        if(++variant<4){setup();return false;}
        active=null;Neon.applyConfig(original);mc.gui.setScreen(null);return true;
    }
}
