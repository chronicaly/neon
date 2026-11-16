package me.xchro.neon.smoke;

import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.hud.*;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Pixel bounds of actual shared HUD text, including the user's coordinate example. */
final class HudAlignmentSmoke {
    private final Minecraft mc;private final NeonConfig original;private int checks,phase,age;
    private boolean pending,done;private String failure;private HudBounds coordinates,fps;
    private final HudCard sample=new HudCard(HudKind.COORDINATES){
        protected void update(Minecraft mc,boolean preview){lines=new String[]{"XYZ  16158, 68, 146772 (2019, 18346)"};}
    };
    HudAlignmentSmoke(Minecraft mc,int checks){this.mc=mc;this.checks=checks;original=ConfigStore.copy(Neon.config);}
    int checks(){return checks;}
    private void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;System.out.println("NEON_CHECK "+message);}
    private void setup(){
        mc.getWindow().setWindowed(1280,800);mc.options.guiScale().set(phase/4+1);mc.resizeGui();
        Neon.applyConfig(new NeonConfig());var c=Neon.config;c.hudScale=new double[]{.75,1,1.25,1.5}[phase%4];
        if(phase>=12){
            c.theme.opacity=1;c.theme.background=phase==12?0xffffffff:0;c.theme.outline=c.theme.secondary=phase==12?0:0xffffffff;
            mc.gui.setScreen(new PixelScreen(Component.literal("Rounded menu verification")){
                protected void renderPixels(GuiGraphicsExtractor g,int x,int y,float delta){
                    g.pose().pushMatrix();g.pose().scale(1/UI_SCALE);g.fill(0,0,1280,800,0xff000000);Ui.refreshTheme();Ui.window(g,40,40,240,140);g.pose().popMatrix();
                }
            });return;
        }
        c.hud(HudKind.COORDINATES).placement.visible=true;c.hud(HudKind.COORDINATES).shadow=false;
        c.theme.text=c.theme.accent=0xffffffff;c.fpsBackground=c.fpsBorder=true;c.fpsShadow=false;
        FpsColor.LABEL.color(c,0xffffffff);FpsColor.VALUE.color(c,0xffffffff);
        int w=mc.getWindow().getWidth(),h=mc.getWindow().getHeight();
        sample.bounds(w,h,true).move(c.hud(HudKind.COORDINATES).placement,80,80,w,h);coordinates=sample.bounds(w,h,true);
        FpsHud.bounds(w,h).move(c.fps,80,180,w,h);fps=FpsHud.bounds(w,h);
        mc.gui.setScreen(new PixelScreen(Component.literal("HUD alignment")){
            protected void renderPixels(GuiGraphicsExtractor g,int x,int y,float delta){
                g.pose().pushMatrix();g.pose().scale(1/UI_SCALE);g.fill(0,0,1280,800,0xff000000);
                sample.draw(g,1280,800,true);FpsHud.draw(g,1280,800);g.pose().popMatrix();
            }
        });
    }
    private void centered(com.mojang.blaze3d.platform.NativeImage image,HudBounds b,String name){
        int left=b.x()+b.width(),right=b.x(),top=b.y()+b.height(),bottom=b.y();
        for(int y=b.y()+2;y<b.y()+b.height()-2;y++)for(int x=b.x()+2;x<b.x()+b.width()-2;x++){
            int rgb=image.getPixel(x,y);if((rgb>>>16&255)>175&&(rgb>>>8&255)>175&&(rgb&255)>175){left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}
        }
        check(right>left&&bottom>top,name+" has visible glyph pixels at scale "+Neon.config.hudScale);
        int above=top-b.y(),below=b.y()+b.height()-1-bottom;
        check(Math.abs(above-below)<=2,name+" vertical ink is centered: "+above+" / "+below+" px");
        int before=left-b.x(),after=b.x()+b.width()-1-right;
        check(Math.abs(before-after)<=3,name+" horizontal ink is balanced: "+before+" / "+after+" px");
    }
    boolean run(){
        if(failure!=null)throw new AssertionError(failure);
        if(phase==14){Neon.applyConfig(original);mc.gui.setScreen(null);return true;}
        if(age++==0)setup();if(age<20)return false;
        if(!pending){pending=true;Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){
            image.writeToFile(mc.gameDirectory.toPath().resolve("screenshots/neon-alignment-"+phase+".png"));
            if(phase<12){centered(image,coordinates,"Coordinates");centered(image,fps,"FPS");}
            else {
                int corner=image.getPixel(40,40)&255,inside=image.getPixel(48,48)&255;
                if(phase==12){check(corner<10&&inside>=210&&inside<=220,"rounded fill cuts away the square corner and retains 84% panel opacity");var levels=new java.util.HashSet<Integer>();for(int y=40;y<47;y++)for(int x=40;x<47;x++){int c0=image.getPixel(x,y)&255;if(c0>0&&c0<inside-10)levels.add(c0);}check(levels.size()>=2,"six-pixel corner resolves multiple antialias coverage levels");}
                else {check(corner<180,"rounded outline has no opaque square corner");check((image.getPixel(50,40)&255)>80,"rounded contour joins the straight edge continuously");}
            }
            done=true;
        }catch(Throwable error){failure=error.toString();}});return false;}
        if(!done)return false;phase++;age=0;pending=done=false;return false;
    }
}
