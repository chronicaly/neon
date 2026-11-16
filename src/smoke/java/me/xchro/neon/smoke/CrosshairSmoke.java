package me.xchro.neon.smoke;

import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Actual framebuffer centroid, with an isolated black patch behind the native crosshair extraction. */
public final class CrosshairSmoke {
    private static boolean active;
    private final Minecraft mc;
    private final NeonConfig original;
    private final int width,height,gui;
    private int index,age,checks;
    private boolean pending,done;
    private String failure;
    CrosshairSmoke(Minecraft mc,int checks) {
        this.mc=mc;this.checks=checks;original=ConfigStore.copy(Neon.config);
        width=mc.getWindow().getWidth();height=mc.getWindow().getHeight();gui=mc.options.guiScale().get();
    }
    public static void underlay(GuiGraphicsExtractor g) {
        if(!active)return;var window=Minecraft.getInstance().getWindow();
        g.pose().pushMatrix();g.pose().scale(1f/window.getGuiScale());
        int x=window.getWidth()/2,y=window.getHeight()/2;g.fill(x-40,y-40,x+40,y+40,0xff000000);g.pose().popMatrix();g.nextStratum();
    }
    int checks(){return checks;}
    boolean run() {
        if(failure!=null)throw new AssertionError(failure);
        if(index==32) {
            active=false;Neon.applyConfig(original);mc.getWindow().setWindowed(width,height);mc.options.guiScale().set(gui);mc.resizeGui();return true;
        }
        if(age++==0) {
            Neon.applyConfig(new NeonConfig());ClientModule.CROSSHAIR.enabled(Neon.config,true);
            Neon.config.crosshair.thickness=index<16?1:2;Neon.config.crosshair.color=0xff18e5bc;
            mc.gui.setScreen(null);mc.options.setCameraType(CameraType.FIRST_PERSON);
            int requestedScale=index/4%4+1;
            mc.getWindow().setWindowed(1280+(index%4&1),(requestedScale==4?960:800)+(index%4>>1));mc.options.guiScale().set(requestedScale);mc.resizeGui();active=true;
        }
        if(age<10)return false;
        if(!pending) {
            pending=true;Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{
                try(image) {
                    int w=image.getWidth(),h=image.getHeight();double x=0,y=0,mass=0;
                    for(int py=h/2-30;py<h/2+30;py++)for(int px=w/2-30;px<w/2+30;px++) {
                        int weight=image.getPixel(px,py)>>>8&255;mass+=weight;x+=(px+.5)*weight;y+=(py+.5)*weight;
                    }
                    if(mass<8||Math.abs(x/mass-w/2.0)>.05||Math.abs(y/mass-h/2.0)>.05)
                        throw new AssertionError("Crosshair centroid "+x/mass+","+y/mass+" vs "+w/2.0+","+h/2.0+" GUI "+mc.getWindow().getGuiScale());
                    checks++;System.out.println("NEON_CHECK centered crosshair "+w+"x"+h+" GUI "+mc.getWindow().getGuiScale()+" thickness "+Neon.config.crosshair.thickness);
                    if(index==0||index==15)image.writeToFile(mc.gameDirectory.toPath().resolve("screenshots/neon-crosshair-centered-"+index+".png"));
                    done=true;
                }catch(Throwable error){failure=error.toString();}
            });return false;
        }
        if(!done)return false;index++;age=0;pending=done=false;return false;
    }
}
