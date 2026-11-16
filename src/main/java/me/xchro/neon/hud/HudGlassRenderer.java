package me.xchro.neon.hud;

import java.util.*;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;

/** One non-overlapping fill and outside contour; all widgets sample one framebuffer-space light. */
public final class HudGlassRenderer {
    public record Surface(HudBounds bounds,int background,int primary,int secondary,boolean fill,boolean outline) {}
    private static final Map<List<HudGlassGeometry.Box>,HudGlassGeometry.Mesh> CACHE=new LinkedHashMap<>(32,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<List<HudGlassGeometry.Box>,HudGlassGeometry.Mesh> e){return size()>32;}
    };
    private HudGlassRenderer() {}
    private static HudGlassGeometry.Mesh mesh(List<Surface> surfaces) {
        var boxes=surfaces.stream().map(s->new HudGlassGeometry.Box(s.bounds.x(),s.bounds.y(),s.bounds.width(),s.bounds.height())).toList();
        return CACHE.computeIfAbsent(boxes,HudGlassGeometry::build);
    }
    public static void draw(GuiGraphicsExtractor g,List<Surface> surfaces,long now,boolean animated) {
        var fills=surfaces.stream().filter(Surface::fill).toList();var outlines=surfaces.stream().filter(Surface::outline).toList();
        if(fills.isEmpty()&&outlines.isEmpty())return;
        var fillMesh=mesh(fills);var edgeMesh=mesh(outlines);
        var bounds=fills.isEmpty()?edgeMesh.bounds():outlines.isEmpty()?fillMesh.bounds():fillMesh.bounds().createUnion(edgeMesh.bounds());
        int x=(int)Math.floor(bounds.getMinX())-4,y=(int)Math.floor(bounds.getMinY())-4;
        var pose=new Matrix3x2f(g.pose());var clip=g.scissorStack.peek();
        var rectangle=new ScreenRectangle(x,y,(int)Math.ceil(bounds.getMaxX())-x+4,(int)Math.ceil(bounds.getMaxY())-y+4).transformMaxBounds(pose);
        if(clip!=null)rectangle=rectangle.intersection(clip);
        if(rectangle!=null)g.guiRenderState.addGuiElement(new Glass(pose,fills,outlines,fillMesh,edgeMesh,now,animated,clip,rectangle));
    }
    private static Surface nearest(List<Surface> surfaces,float x,float y) {
        Surface nearest=surfaces.getFirst();double distance=Double.POSITIVE_INFINITY;
        for(var s:surfaces){var b=s.bounds;double dx=Math.max(b.x()-x,Math.max(0,x-b.x()-b.width())),dy=Math.max(b.y()-y,Math.max(0,y-b.y()-b.height()));double d=dx*dx+dy*dy;if(d<distance){distance=d;nearest=s;}}
        return nearest;
    }
    private static int alpha(int c,float amount){return(c&0xffffff)|Math.clamp(Math.round((c>>>24)*amount),0,255)<<24;}
    private static int mix(int a,int b,double amount){int result=0;for(int shift=0;shift<=24;shift+=8)result|=(int)Math.round(((a>>>shift)&255)*(1-amount)+((b>>>shift)&255)*amount)<<shift;return result;}
    /** Shared phase has no per-widget start time or changing-perimeter rebase. */
    public static double light(float x,float y,long now){return (x+y)/620.0-now/1e9*.13;}
    private record Glass(Matrix3x2f pose,List<Surface> fills,List<Surface> outlines,HudGlassGeometry.Mesh fillMesh,HudGlassGeometry.Mesh edgeMesh,
                         long now,boolean animated,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline(){return RenderPipelines.GUI;}
        public TextureSetup textureSetup(){return TextureSetup.noTexture();}
        public void buildVertices(VertexConsumer v) {
            for(var q:fillMesh.fill()){
                int color=background((q.leftTop()+q.leftBottom()+q.rightTop()+q.rightBottom())*.25f,(q.top()+q.bottom())*.5f);
                quad(v,q.leftTop(),q.top(),q.leftBottom(),q.bottom(),q.rightBottom(),q.bottom(),q.rightTop(),q.top(),color,color,color,color);
            }
            for(var e:fillMesh.edge()){
                int color=background((e.x0()+e.x1())*.5f,(e.y0()+e.y1())*.5f);
                ribbon(v,e,-.6f,0,alpha(color,0),alpha(color,0),color,color);
            }
            for(var e:edgeMesh.edge()) {
                var owner=nearest(outlines,(e.x0()+e.x1())*.5f,(e.y0()+e.y1())*.5f);
                int a=edge(owner,e.x0(),e.y0()),b=edge(owner,e.x1(),e.y1());
                if(animated)ribbon(v,e,-3,-.25f,alpha(a,0),alpha(b,0),alpha(a,glow(e.x0(),e.y0())),alpha(b,glow(e.x1(),e.y1())));
                ribbon(v,e,-.35f,.35f,alpha(a,0),alpha(b,0),a,b);
                ribbon(v,e,.35f,1,a,b,a,b);
                ribbon(v,e,1,1.4f,a,b,alpha(a,0),alpha(b,0));
            }
        }
        private int background(float x,float y){return nearest(fills,x,y).background;}
        private int edge(Surface s,float x,float y){double phase=animated?light(x,y,now):0;double mix=.5+.5*Math.sin(phase*Math.PI*2);return HudGlassRenderer.mix(s.primary,s.secondary,mix);}
        private float glow(float x,float y){double t=.5+.5*Math.cos(light(x,y,now)*Math.PI*2);return(float)(.28*Math.pow(t,12));}
        private void ribbon(VertexConsumer v,HudGlassGeometry.Edge e,float outer,float inner,int a,int b,int c,int d) {
            quad(v,e.x0()+e.nx0()*outer,e.y0()+e.ny0()*outer,e.x0()+e.nx0()*inner,e.y0()+e.ny0()*inner,
                e.x1()+e.nx1()*inner,e.y1()+e.ny1()*inner,e.x1()+e.nx1()*outer,e.y1()+e.ny1()*outer,a,c,d,b);
        }
        private void quad(VertexConsumer v,float x0,float y0,float x1,float y1,float x2,float y2,float x3,float y3,int a,int b,int c,int d) {
            // Area contours include holes and may wind either way; Minecraft's GUI pipeline culls back faces.
            float area=x0*y1-x1*y0+x1*y2-x2*y1+x2*y3-x3*y2+x3*y0-x0*y3;
            v.addVertexWith2DPose(pose,x0,y0).setColor(a);
            if(area>0){v.addVertexWith2DPose(pose,x3,y3).setColor(d);v.addVertexWith2DPose(pose,x2,y2).setColor(c);v.addVertexWith2DPose(pose,x1,y1).setColor(b);}
            else{v.addVertexWith2DPose(pose,x1,y1).setColor(b);v.addVertexWith2DPose(pose,x2,y2).setColor(c);v.addVertexWith2DPose(pose,x3,y3).setColor(d);}
        }
    }
}
