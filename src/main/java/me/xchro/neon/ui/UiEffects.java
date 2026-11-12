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
    private static Geometry geometry;
    private static final int[] CORNER_ALPHA = cornerAlpha();
    private static int[] cornerAlpha() {
        int[] result = new int[25];
        for (int y=0;y<=4;y++) for(int x=0;x<=4;x++) result[y*5+x]=(int)Math.round(96*Math.max(0,1-Math.hypot(x,y)/4));
        return result;
    }
    private record Geometry(int w, int h, double[] top, double[] bottom, double[] left, double[] right) {
        Geometry(int w, int h) {
            this(w, h, new double[w + 1], new double[w + 1], new double[h + 1], new double[h + 1]);
            for (int x = 0; x <= w; x++) { top[x] = x; bottom[x] = 2*w+h-x; }
            for (int y = 0; y <= h; y++) { left[y] = 2*(w+h)-y; right[y] = w+y; }
        }
    }
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
        int scale = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
        var start = g.pose().transformPosition(x, y, new Vector2f());
        var end = g.pose().transformPosition(x + w, y + h, new Vector2f());
        int left = Math.round(start.x * scale), top = Math.round(start.y * scale);
        int width = Math.round(end.x * scale) - left, height = Math.round(end.y * scale) - top;
        double phase=travel==null?(System.nanoTime()/1e9*me.xchro.neon.core.OutlineTravel.SPEED)%(2.0*(width+height)):
            travel.update(left,top,width,height,System.nanoTime());
        var pose = new Matrix3x2f().scale(1f / scale);
        var bounds = new ScreenRectangle(left-8, top-8, width+16, height+16).transformMaxBounds(pose);
        var scissor = g.scissorStack.peek();
        if (scissor != null) bounds = bounds.intersection(scissor);
        if (geometry == null || geometry.w != width || geometry.h != height) geometry = new Geometry(width, height);
        if (bounds != null) g.guiRenderState.addGuiElement(new Border(pose, left, top, width, height, phase, Ui.opacity, Ui.OUTLINE, Ui.VIOLET, geometry, scissor, bounds));
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
    /** A single ordered mesh prevents fractional-scale seams between separately sorted segments. */
    private record Border(Matrix3x2f pose, int x, int y, int w, int h, double phase, float opacity, int outline, int secondary, Geometry geometry,
                          ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline() { return RenderPipelines.GUI; }
        public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        public void buildVertices(VertexConsumer v) {
            for (int i = 0; i < w; i += 2) {
                int n = Math.min(2, w - i);
                int a = color(geometry.top[i], phase), b = color(geometry.top[i + n], phase);
                int ha=halo(a,geometry.top[i]),hb=halo(b,geometry.top[i+n]);
                if((ha>>>24)+(hb>>>24)>0){
                    quad(v,x+i,y-8,n,8,clear(a),clear(b),hb,ha);
                    quad(v,x+i,y+1,n,3,ha,hb,clear(b),clear(a));
                }
                quad(v, x + i, y, n, 1, a, b, b, a);
                a = color(geometry.bottom[i], phase); b = color(geometry.bottom[i + n], phase);
                ha=halo(a,geometry.bottom[i]);hb=halo(b,geometry.bottom[i+n]);
                if((ha>>>24)+(hb>>>24)>0){
                    quad(v,x+i,y+h,n,8,ha,hb,clear(b),clear(a));
                    quad(v,x+i,y+h-4,n,3,clear(a),clear(b),hb,ha);
                }
                quad(v, x + i, y + h - 1, n, 1, a, b, b, a);
            }
            for (int i = 0; i < h; i += 2) {
                int n = Math.min(2, h - i);
                int coreTop = Math.max(1,i), coreHeight = Math.min(h-1,i+n)-coreTop;
                int a = color(geometry.left[i], phase), b = color(geometry.left[i + n], phase);
                int ha=halo(a,geometry.left[i]),hb=halo(b,geometry.left[i+n]);
                if((ha>>>24)+(hb>>>24)>0){
                    quad(v,x-8,y+i,8,n,clear(a),ha,hb,clear(b));
                    quad(v,x+1,y+i,3,n,ha,clear(a),clear(b),hb);
                }
                if(coreHeight>0)quad(v, x, y + coreTop, 1, coreHeight, a, a, b, b);
                a = color(geometry.right[i], phase); b = color(geometry.right[i + n], phase);
                ha=halo(a,geometry.right[i]);hb=halo(b,geometry.right[i+n]);
                if((ha>>>24)+(hb>>>24)>0){
                    quad(v,x+w,y+i,8,n,ha,clear(a),clear(b),hb);
                    quad(v,x+w-4,y+i,3,n,clear(a),ha,hb,clear(b));
                }
                if(coreHeight>0)quad(v, x + w - 1, y + coreTop, 1, coreHeight, a, a, b, b);
            }
            corner(v,x,y,-1,-1,halo(color(geometry.top[0],phase),geometry.top[0]));
            corner(v,x+w,y,1,-1,halo(color(geometry.top[w],phase),geometry.top[w]));
            corner(v,x,y+h,-1,1,halo(color(geometry.bottom[0],phase),geometry.bottom[0]));
            corner(v,x+w,y+h,1,1,halo(color(geometry.bottom[w],phase),geometry.bottom[w]));
        }
        private void corner(VertexConsumer v,int cx,int cy,int sx,int sy,int color){
            if((color>>>24)==0)return;
            for(int iy=0;iy<4;iy++)for(int ix=0;ix<4;ix++){
                int left=cx+(sx<0?-2*(ix+1):2*ix),top=cy+(sy<0?-2*(iy+1):2*iy);
                int xa=sx<0?ix+1:ix,xb=sx<0?ix:ix+1,ya=sy<0?iy+1:iy,yb=sy<0?iy:iy+1;
                quad(v,left,top,2,2,cornerColor(color,xa,ya),cornerColor(color,xb,ya),cornerColor(color,xb,yb),cornerColor(color,xa,yb));
            }
        }
        private int cornerColor(int color,int x,int y){return Ui.alpha(color,(color>>>24)*CORNER_ALPHA[y*5+x]/96);}
        private void quad(VertexConsumer v, int left, int top, int width, int height, int tl, int tr, int br, int bl) {
            v.addVertexWith2DPose(pose, left, top).setColor(fade(tl));
            v.addVertexWith2DPose(pose, left, top + height).setColor(fade(bl));
            v.addVertexWith2DPose(pose, left + width, top + height).setColor(fade(br));
            v.addVertexWith2DPose(pose, left + width, top).setColor(fade(tr));
        }
        private int fade(int color){return Ui.alpha(color,Math.round((color>>>24)*opacity));}
        private int color(double base,double phase){return UiEffects.color(base,phase,2.0*(w+h),outline,secondary);}
        private int halo(int color,double position){double p=pulse(position,phase,2.0*(w+h),96);return Ui.alpha(color,(int)Math.round((color>>>24)*.64*p*p));}
        private int clear(int color){return color&0xffffff;}
    }
    private static int color(double base, double phase, double perimeter,int outline, int secondary) {
        double blend=.5+.5*Math.sin((base-phase)/perimeter*Math.PI*2);
        int neon=mix(outline,secondary,blend);
        double p=pulse(base,phase,perimeter,144);
        return mix(neon,Ui.alpha(0xffffff,neon>>>24),.75*p*p);
    }
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
        if(buttonBloom==null) {
            var pixels=new NativeImage(64,64,true);
            for(int py=0;py<64;py++)for(int px=0;px<64;px++) {
                double dx=Math.max(0,Math.max(16-px,px-47)),dy=Math.max(0,Math.max(16-py,py-47));
                pixels.setPixel(px,py,(int)Math.round(160*Math.exp(-(dx*dx+dy*dy)/64))<<24|0xffffff);
            }
            buttonBloom=new DynamicTexture(()->"Neon button hover bloom",pixels);
        }
        int radius=12;
        int[] xs={x-radius,x,x+w,x+w+radius},ys={y-radius,y,y+h,y+h+radius};
        float[] uv={0,.25f,.75f,1};
        var texture=TextureSetup.singleTexture(buttonBloom.getTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        var pose=new Matrix3x2f(g.pose());var scissor=g.scissorStack.peek();
        for(int iy=0;iy<3;iy++)for(int ix=0;ix<3;ix++) {
            if(ix==1&&iy==1)continue;
            var bounds=new ScreenRectangle(xs[ix],ys[iy],xs[ix+1]-xs[ix],ys[iy+1]-ys[iy]).transformMaxBounds(pose);
            if(scissor!=null)bounds=bounds.intersection(scissor);
            if(bounds==null)continue;
            int left=mix(Ui.BLUE,hoverViolet(),.4+.6*Math.clamp((xs[ix]-x)/(double)w,0,1));
            int right=mix(Ui.BLUE,hoverViolet(),.4+.6*Math.clamp((xs[ix+1]-x)/(double)w,0,1));
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
        if (glow != null) { glow.close(); glow = null; }
        if (buttonBloom != null) { buttonBloom.close(); buttonBloom = null; }
    }
}
