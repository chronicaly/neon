package me.xchro.neon.ui;

import java.util.*;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.state.gui.*;

/** Short-lived draw commands only: snapshots never retain widgets, input or a world framebuffer. */
public final class MenuPresentation {
    private static long epoch, holdUntil;
    private static Exit exit;
    private long generation=epoch, dimensions, changed;
    private List<GuiElementRenderState> lastFrame=List.of(), lastContent=List.of(), outgoing=List.of();
    private Capture capture;
    private ScreenRectangle clip;
    private float progress=1,backdropOpacity=1;
    static long dimensions() {
        var w=Minecraft.getInstance().getWindow();return ((long)w.getWidth()<<32)^((long)w.getHeight()<<8)^w.getGuiScale();
    }
    public static void invalidateAll() { epoch++;exit=null;holdUntil=0; }
    public static void clearExit() { exit=null; }
    static boolean retainsTextures() { return System.nanoTime()<holdUntil; }
    void invalidate() { lastFrame=lastContent=outgoing=List.of();changed=0; }
    void change() {
        if(lastContent.isEmpty())return;
        outgoing=List.copyOf(lastContent);changed=System.nanoTime();holdUntil=Math.max(holdUntil,changed+120_000_000L);
    }
    GuiGraphicsExtractor begin(GuiGraphicsExtractor destination,int mx,int my) {
        backdropOpacity=Ui.opacity;
        long size=dimensions();if(size!=dimensions||generation!=epoch){invalidate();dimensions=size;generation=epoch;}
        progress=changed==0?1:(float)Math.clamp((System.nanoTime()-changed)/120_000_000.0,0,1);
        if(progress==1)outgoing=List.of();
        if(capture==null)capture=new Capture();
        capture.begin(destination.guiRenderState);
        var graphics=new GuiGraphicsExtractor(Minecraft.getInstance(),capture,mx,my) {
            @Override public void requestCursor(com.mojang.blaze3d.platform.cursor.CursorType cursor) { destination.requestCursor(cursor); }
            @Override public void setPreeditOverlay(net.minecraft.client.gui.components.Renderable overlay) { destination.setPreeditOverlay(overlay); }
        };
        graphics.pose().set(destination.pose());return graphics;
    }
    void content(GuiGraphicsExtractor g,int x,int y,int width,int height) {
        clip=new ScreenRectangle(x,y,width,height).transformMaxBounds(g.pose());capture.content=true;
    }
    void endContent() {
        capture.replay=true;
        if(progress<1)for(var element:outgoing){var faded=fade(element,1-progress,clip);if(faded!=null)capture.addGuiElement(faded);}
        capture.content=false;capture.replay=false;
    }
    // Input runs between frames. Copy only when a transition actually needs an immutable snapshot;
    // ordinary frames reuse their recording buffers, while each extractor keeps current mouse state.
    void finish() { lastFrame=capture.frame;lastContent=capture.contents;capture.target=null; }
    record Exit(List<GuiElementRenderState> frame,Screen parent,long start,long dimensions,boolean blur,float backdropOpacity) {}
    Exit freeze(Screen parent) { return new Exit(List.copyOf(lastFrame),parent,System.nanoTime(),dimensions(),MenuEffects.blur(),backdropOpacity); }
    static void showExit(Exit snapshot) { if(!snapshot.frame.isEmpty()){exit=snapshot;holdUntil=Math.max(holdUntil,snapshot.start+80_000_000L);} }
    private static float exitOpacity() {
        if(exit==null)return 0;
        if(exit.dimensions!=dimensions()||Minecraft.getInstance().gui.screen()!=exit.parent){exit=null;return 0;}
        double t=Math.clamp((System.nanoTime()-exit.start)/80_000_000.0,0,1);
        if(t==1){exit=null;return 0;}return (float)(1-t*t*(3-2*t));
    }
    public static int exitBlur() { float alpha=exitOpacity();return alpha>0&&exit.blur?(int)Math.round(5*alpha):-1; }
    public static void drawExit(GuiRenderState state) {
        float alpha=exitOpacity();if(alpha<=0)return;
        var g=new GuiGraphicsExtractor(Minecraft.getInstance(),state,-10000,-10000);g.nextStratum();
        if(exit.blur)g.blurBeforeThisStratum();
        g.fill(0,0,g.guiWidth(),g.guiHeight(),Ui.alpha(0x060a12,Math.round(179*alpha*exit.backdropOpacity)));
        for(var element:exit.frame){var faded=fade(element,alpha,null);if(faded!=null)state.addGuiElement(faded);}
    }
    private final class Capture extends GuiRenderState {
        private GuiRenderState target;
        private final List<GuiElementRenderState> frame=new ArrayList<>(),contents=new ArrayList<>();
        private boolean content,replay;
        void begin(GuiRenderState target){this.target=target;frame.clear();contents.clear();content=replay=false;}
        @Override public void addGuiElement(GuiElementRenderState original) {
            var element=content&&!replay?fade(original,progress,clip):original;
            if(element==null)return;
            frame.add(element);if(content)contents.add(element);target.addGuiElement(element);
        }
        @Override public void addBlitToCurrentLayer(BlitRenderState element){addGuiElement(element);}
        @Override public void addGlyphToCurrentLayer(GuiElementRenderState element){addGuiElement(element);}
        @Override public void addText(GuiTextRenderState text){target.addText(text);}
        @Override public void addItem(GuiItemRenderState item){target.addItem(item);}
    }
    private static GuiElementRenderState fade(GuiElementRenderState element,float alpha,ScreenRectangle clip) {
        if(element instanceof Faded old){alpha*=old.alpha;element=old.source;var merged=intersect(clip,old.scissorArea);if(clip!=null&&old.scissorArea!=null&&merged==null)return null;clip=merged;}
        if(alpha<.003f)return null;
        ScreenRectangle scissor=intersect(element.scissorArea(),clip);
        if(element.scissorArea()!=null&&clip!=null&&scissor==null)return null;
        ScreenRectangle bounds=intersect(element.bounds(),scissor);
        if(bounds==null)return null;
        if(alpha>=1&&Objects.equals(scissor,element.scissorArea()))return element;
        return new Faded(element,alpha,scissor,bounds);
    }
    private static ScreenRectangle intersect(ScreenRectangle a,ScreenRectangle b){
        if(a==null)return b;if(b==null)return a;
        if(a.encompasses(b))return b;if(b.encompasses(a))return a;
        return a.intersection(b);
    }
    private record Faded(GuiElementRenderState source,float alpha,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline(){return source.pipeline();}
        public TextureSetup textureSetup(){return source.textureSetup();}
        public void buildVertices(VertexConsumer target){source.buildVertices(new AlphaVertices(target,alpha));}
    }
    private record AlphaVertices(VertexConsumer target,float alpha) implements VertexConsumer {
        public VertexConsumer addVertex(float x,float y,float z){target.addVertex(x,y,z);return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){target.setColor(r,g,b,Math.round(a*alpha));return this;}
        public VertexConsumer setColor(int argb){target.setColor(Ui.alpha(argb,Math.round((argb>>>24)*alpha)));return this;}
        public VertexConsumer setUv(float u,float v){target.setUv(u,v);return this;}
        public VertexConsumer setUv1(int u,int v){target.setUv1(u,v);return this;}
        public VertexConsumer setUv2(int u,int v){target.setUv2(u,v);return this;}
        public VertexConsumer setNormal(float x,float y,float z){target.setNormal(x,y,z);return this;}
        public VertexConsumer setLineWidth(float width){target.setLineWidth(width);return this;}
    }
}
