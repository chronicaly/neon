package me.xchro.neon.ui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

/** Interpolated color geometry and a filtered Gaussian, never stacked rectangle halos. */
final class UiEffects {
    private static DynamicTexture glow;
    private static DynamicTexture buttonBloom;
    private UiEffects() {}
    /** One render command for the transparency grid, with the same cell edges and colors. */
    static void checker(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        var pose = new Matrix3x2f(g.pose());
        var bounds = new ScreenRectangle(x, y, w, h).transformMaxBounds(pose);
        var scissor = g.scissorStack.peek();
        if (scissor != null) bounds = bounds.intersection(scissor);
        if (bounds != null) g.guiRenderState.addGuiElement(new Checker(pose,x,y,w,h,
            Ui.faded(0xffabb6ba),Ui.faded(0xff617077),scissor,bounds));
    }
    private record Checker(Matrix3x2f pose,int x,int y,int w,int h,int light,int dark,
                           ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline() { return RenderPipelines.GUI; }
        public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        public void buildVertices(VertexConsumer v) {
            for (int py=0;py<h;py+=4) for (int px=0;px<w;px+=4) {
                int color=((px+py)/4&1)==0?light:dark;
                int left=x+px,top=y+py,right=x+Math.min(w,px+4),bottom=y+Math.min(h,py+4);
                v.addVertexWith2DPose(pose,left,top).setColor(color);
                v.addVertexWith2DPose(pose,left,bottom).setColor(color);
                v.addVertexWith2DPose(pose,right,bottom).setColor(color);
                v.addVertexWith2DPose(pose,right,top).setColor(color);
            }
        }
    }
    /** Low-contrast light inside the surface; the existing perimeter mesh stays untouched. */
    static void surfaceLight(GuiGraphicsExtractor g, int x, int y, int w, int h, int surface) {
        double phase = (System.nanoTime() % 16_000_000_000L) / 16_000_000_000.0 * Math.PI * 2;
        double breath = .5 + .5 * Math.sin(phase);
        int cyan = light(Ui.OUTLINE, surface, .025 + .018 * breath);
        int violet = light(Ui.VIOLET, surface, .025 + .018 * (1 - breath));
        float depth = Math.min(76, h * .36f), span = w * .78f;
        gradient(g, x, y, span, depth, cyan, cyan & 0xffffff, cyan & 0xffffff, cyan & 0xffffff);
        gradient(g, x + w - span, y + h - depth, span, depth,
            violet & 0xffffff, violet & 0xffffff, violet, violet & 0xffffff);
    }
    private static int light(int color, int surface, double strength) {
        return Ui.alpha(color, (int)Math.round((color >>> 24) * (surface >>> 24) / 255.0 * strength));
    }
    /** A soft, bounded pointer light, also centered on keyboard-focused controls. */
    static void hoverLight(GuiGraphicsExtractor g, int x, int y, int w, int h, int pointerX, double amount) {
        if (amount <= .001 || !MenuEffects.glow()) return;
        float center = Math.clamp(pointerX, x, x + w);
        float left = Math.max(x, center - 65), right = Math.min(x + w, center + 65);
        int bright = light(Ui.BLUE, Ui.PANEL, .07 * amount), clear = bright & 0xffffff;
        gradient(g, left, y, center - left, h, clear, bright, bright, clear);
        gradient(g, center, y, right - center, h, bright, clear, clear, bright);
    }
    static int mix(int a, int b, double amount) {
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) result |= (int) Math.round(((a >>> shift) & 255) * (1 - amount) + ((b >>> shift) & 255) * amount) << shift;
        return result;
    }
    static void gradient(GuiGraphicsExtractor g, float x, float y, float w, float h, int tl, int tr, int br, int bl) {
        if (w <= 0 || h <= 0) return;
        var pose = new Matrix3x2f(g.pose());
        var bounds = new ScreenRectangle((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(w + 1), (int) Math.ceil(h + 1)).transformMaxBounds(pose);
        var scissor = g.scissorStack.peek();
        if (scissor != null) bounds = bounds.intersection(scissor);
        if (bounds != null) g.guiRenderState.addGuiElement(new Quad(pose, x, y, x + w, y + h, Ui.faded(tl), Ui.faded(tr), Ui.faded(br), Ui.faded(bl), scissor, bounds));
    }
    private record Quad(Matrix3x2f pose, float x0, float y0, float x1, float y1, int tl, int tr, int br, int bl,
                        ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline() { return RenderPipelines.GUI; }
        public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        public void buildVertices(VertexConsumer v) {
            v.addVertexWith2DPose(pose, x0, y0).setColor(tl);
            v.addVertexWith2DPose(pose, x0, y1).setColor(bl);
            v.addVertexWith2DPose(pose, x1, y1).setColor(br);
            v.addVertexWith2DPose(pose, x1, y0).setColor(tr);
        }
    }
    static void outline(GuiGraphicsExtractor g, float x, float y, float w, float h) {
        outline(g,x,y,w,h,null);
    }
    static void outline(GuiGraphicsExtractor g,float x,float y,float w,float h,me.xchro.neon.core.OutlineTravel travel) {
        outline(g,x,y,w,h,travel,Ui.OUTLINE,Ui.VIOLET,Ui.opacity);
    }
    static void outline(GuiGraphicsExtractor g,float x,float y,float w,float h,me.xchro.neon.core.OutlineTravel travel,int primary,int secondary,float opacity) {
        RoundedUi.outline(g,x,y,w,h,travel,primary,secondary,opacity);
    }
    static void stroke(GuiGraphicsExtractor g,int x,int y,int w,int h,int color){
        stroke(g,x,y,w,h,color,color);
    }
    static void stroke(GuiGraphicsExtractor g,int x,int y,int w,int h,int leftColor,int rightColor){
        if(w<=0||h<=0)return;
        int scale=Math.max(1,Minecraft.getInstance().getWindow().getGuiScale());
        var a=g.pose().transformPosition(x,y,new Vector2f());var b=g.pose().transformPosition(x+w,y+h,new Vector2f());
        int left=Math.round(a.x*scale),top=Math.round(a.y*scale),width=Math.round(b.x*scale)-left,height=Math.round(b.y*scale)-top;
        var pose=new Matrix3x2f().scale(1f/scale);var clip=g.scissorStack.peek();
        var bounds=new ScreenRectangle(left,top,width,height).transformMaxBounds(pose);if(clip!=null)bounds=bounds.intersection(clip);
        if(bounds!=null)g.guiRenderState.addGuiElement(new Frame(pose,left,top,width,height,leftColor,rightColor,clip,bounds));
    }
    private record Frame(Matrix3x2f pose,int x,int y,int w,int h,int left,int right,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline(){return RenderPipelines.GUI;}public TextureSetup textureSetup(){return TextureSetup.noTexture();}
        public void buildVertices(VertexConsumer v){rect(v,x,y,w,1,left,right);rect(v,x,y+h-1,w,1,left,right);rect(v,x,y+1,1,h-2,left,left);rect(v,x+w-1,y+1,1,h-2,right,right);}
        private void rect(VertexConsumer v,int x,int y,int w,int h,int a,int b){if(w<=0||h<=0)return;v.addVertexWith2DPose(pose,x,y).setColor(a);v.addVertexWith2DPose(pose,x,y+h).setColor(a);v.addVertexWith2DPose(pose,x+w,y+h).setColor(b);v.addVertexWith2DPose(pose,x+w,y).setColor(b);}
    }
    private static int color(double base, double phase, double perimeter,int outline, int secondary) {
        double blend=.5+.5*Math.sin((base-phase)/perimeter*Math.PI*2);
        int neon=mix(outline,secondary,blend);
        double p=pulse(base,phase,perimeter,144);
        return mix(neon,Ui.alpha(0xffffff,neon>>>24),.75*p*p);
    }
    static int outlineColor(double base,double phase,double perimeter,int primary,int secondary){return color(base,phase,perimeter,primary,secondary);}
    static double outlinePulse(double position,double phase,double perimeter,double radius){return pulse(position,phase,perimeter,radius);}
    private static double pulse(double position,double phase,double perimeter,double radius){
        double distance=((position-phase)%perimeter+perimeter)%perimeter;
        return Math.max(0,1-Math.min(distance,perimeter-distance)/radius);
    }
    static void glow(GuiGraphicsExtractor g, int x, int y, int w, int h, int color, int radius) {
        if (glow == null) {
            var pixels = new NativeImage(64, 64, true);
            for (int py = 0; py < 64; py++) for (int px = 0; px < 64; px++) {
                double dx = Math.max(0, Math.max(16 - px, px - 47)), dy = Math.max(0, Math.max(16 - py, py - 47));
                int alpha = (int) Math.round(36 * Math.exp(-(dx * dx + dy * dy) / 50));
                pixels.setPixel(px, py, alpha << 24 | 0xffffff);
            }
            glow = new DynamicTexture(() -> "Neon active control glow", pixels);
        }
        int[] xs = {x - radius, x, x + w, x + w + radius}, ys = {y - radius, y, y + h, y + h + radius};
        float[] uv = {0, .25f, .75f, 1};
        var texture = TextureSetup.singleTexture(glow.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        for (int iy = 0; iy < 3; iy++) for (int ix = 0; ix < 3; ix++) {
            if (ix == 1 && iy == 1) continue;
            g.guiRenderState.addGuiElement(new BlitRenderState(RenderPipelines.GUI_TEXTURED, texture, new Matrix3x2f(g.pose()),
                xs[ix], ys[iy], xs[ix + 1], ys[iy + 1], uv[ix], uv[ix + 1], uv[iy], uv[iy + 1], color, g.scissorStack.peek()));
        }
    }
    static int hoverViolet() { return mix(Ui.VIOLET,Ui.alpha(0xad63ff,Ui.VIOLET>>>24),.65); }
    /** A broad violet Gaussian with a cool cyan shoulder, reserved for hovered grid buttons. */
    static void buttonGlow(GuiGraphicsExtractor g,int x,int y,int w,int h,double amount) {
        if(amount<=.001 || !MenuEffects.glow())return;
        var r=RoundedUi.rect(g,x,y,w,h);if(r==null||r.w()<6||r.h()<6)return;
        if(buttonBloom==null) {
            // The nine-slice corners include the three pixels INSIDE the box that its rounded
            // surface cuts away. A square center cutout leaves dark pinholes at those corners.
            var pixels=new NativeImage(76,76,true);
            for(int py=0;py<76;py++)for(int px=0;px<76;px++) {
                double dx=Math.abs(px+.5-38)-19,dy=Math.abs(py+.5-38)-19;
                double distance=Math.hypot(Math.max(dx,0),Math.max(dy,0))+Math.min(Math.max(dx,dy),0)-3;
                double outside=Math.max(0,distance),coverage=Math.clamp(distance+.5,0,1);
                pixels.setPixel(px,py,(int)Math.round(160*Math.exp(-outside*outside/64)*coverage)<<24|0xffffff);
            }
            buttonBloom=new DynamicTexture(()->"Neon button hover bloom",pixels);
        }
        // Use exactly the surface's snapped physical bounds, independent of Minecraft GUI scale.
        int[] xs={r.x()-16,r.x()+3,r.x()+r.w()-3,r.x()+r.w()+16},ys={r.y()-16,r.y()+3,r.y()+r.h()-3,r.y()+r.h()+16};
        float[] uv={0,.25f,.75f,1};
        var texture=TextureSetup.singleTexture(buttonBloom.getTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        var pose=r.pose();var scissor=r.clip();
        for(int iy=0;iy<3;iy++)for(int ix=0;ix<3;ix++) {
            if(ix==1&&iy==1)continue;
            var bounds=new ScreenRectangle(xs[ix],ys[iy],xs[ix+1]-xs[ix],ys[iy+1]-ys[iy]).transformMaxBounds(pose);
            if(scissor!=null)bounds=bounds.intersection(scissor);
            if(bounds==null)continue;
            int left=mix(Ui.BLUE,hoverViolet(),.4+.6*Math.clamp((xs[ix]-r.x())/(double)r.w(),0,1));
            int right=mix(Ui.BLUE,hoverViolet(),.4+.6*Math.clamp((xs[ix+1]-r.x())/(double)r.w(),0,1));
            left=Ui.faded(Ui.alpha(left,(int)Math.round((left>>>24)*amount)));
            right=Ui.faded(Ui.alpha(right,(int)Math.round((right>>>24)*amount)));
            g.guiRenderState.addGuiElement(new Bloom(pose,xs[ix],ys[iy],xs[ix+1],ys[iy+1],uv[ix],uv[iy],uv[ix+1],uv[iy+1],left,right,texture,scissor,bounds));
        }
    }
    private record Bloom(Matrix3x2f pose,float x0,float y0,float x1,float y1,float u0,float v0,float u1,float v1,
                         int left,int right,TextureSetup textureSetup,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline(){return RenderPipelines.GUI_TEXTURED;}
        public void buildVertices(VertexConsumer v){
            v.addVertexWith2DPose(pose,x0,y0).setUv(u0,v0).setColor(left);
            v.addVertexWith2DPose(pose,x0,y1).setUv(u0,v1).setColor(left);
            v.addVertexWith2DPose(pose,x1,y1).setUv(u1,v1).setColor(right);
            v.addVertexWith2DPose(pose,x1,y0).setUv(u1,v0).setColor(right);
        }
    }
    static void close() {
        RoundedUi.clear();
        if (glow != null) { glow.close(); glow = null; }
        if (buttonBloom != null) { buttonBloom.close(); buttonBloom = null; }
    }
}
