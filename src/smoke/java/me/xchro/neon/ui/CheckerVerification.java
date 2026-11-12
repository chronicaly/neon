package me.xchro.neon.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** GPU equivalence against the preceding per-cell renderer. Verification JAR only. */
public final class CheckerVerification extends PixelScreen {
    private final Minecraft client;
    private int variant, phase, age, checks;
    private int[] reference;
    private boolean pending, captured;
    private String failure;
    public CheckerVerification(Minecraft client,int checks){
        super(Ui.label("Transparency rendering verification"));this.client=client;this.checks=checks;setup();
    }
    public int checks(){return checks;}
    private int sampleWidth(){return variant%2==0?143:13;}
    private int sampleHeight(){return variant%2==0?8:21;}
    private void setup(){
        client.options.guiScale().set(variant/4+1);client.resizeGui();client.gui.setScreen(this);
        age=phase=0;pending=captured=false;
    }
    @Override protected void pixelBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        Ui.opacity=1;Ui.rect(g,0,0,canvasWidth(),canvasHeight(),0xff15202f);
    }
    @Override protected void renderPixels(GuiGraphicsExtractor g,int mx,int my,float delta){
        Ui.opacity=variant%4<2?1:.53f;
        int w=sampleWidth(),h=sampleHeight();Ui.clip(g,34,32,31+w-2,31+h);
        if(phase==0){
            for(int y=0;y<h;y+=4)for(int x=0;x<w;x+=4)
                Ui.rect(g,31+x,31+y,Math.min(4,w-x),Math.min(4,h-y),((x+y)/4&1)==0?0xffabb6ba:0xff617077);
        }else UiEffects.checker(g,31,31,w,h);
        g.disableScissor();
    }
    public boolean run(){
        if(failure!=null)throw new AssertionError(failure);
        if(++age<6)return false;
        if(!pending){
            pending=true;int left=(int)(28*UI_SCALE),top=(int)(28*UI_SCALE);
            int width=(int)((sampleWidth()+8)*UI_SCALE),height=(int)((sampleHeight()+8)*UI_SCALE);
            Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{
                try(image){
                    int[] pixels=new int[width*height];
                    for(int y=0;y<height;y++)for(int x=0;x<width;x++)pixels[y*width+x]=image.getPixel(left+x,top+y);
                    if(phase==0)reference=pixels;
                    else{
                        if(!java.util.Arrays.equals(reference,pixels))throw new AssertionError("Checker pixels differ at variant "+variant);
                        checks++;System.out.println("NEON_CHECK batched checker matches every reference pixel at GUI scale "+(variant/4+1)+", partial cells and opacity "+(variant%4<2?1:.53));
                    }
                    captured=true;
                }catch(Throwable error){failure=error.toString();}
            });return false;
        }
        if(!captured)return false;
        if(phase++==0){age=0;pending=captured=false;return false;}
        if(++variant<12){setup();return false;}
        client.gui.setScreen(null);return true;
    }
}
