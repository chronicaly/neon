package me.xchro.neon.ui;

import java.util.*;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.xchro.neon.hud.HudGlassGeometry;
import me.xchro.neon.core.OutlineTravel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.*;
import java.lang.Math;

/** Shared native-pixel corners. Fill and outline use the same antialiased contour. */
final class RoundedUi {
    static void clear(){CACHE.clear();}
    private static final Map<Long,HudGlassGeometry.Mesh> CACHE=new LinkedHashMap<>(64,.75f,true){
        protected boolean removeEldestEntry(Map.Entry<Long,HudGlassGeometry.Mesh> entry){return size()>96;}
    };
    private record Rect(int x,int y,int w,int h,int scale,Matrix3x2f pose,ScreenRectangle clip,ScreenRectangle bounds){}
    private static Rect rect(GuiGraphicsExtractor g,float x,float y,float w,float h){
        int scale=Math.max(1,Minecraft.getInstance().getWindow().getGuiScale());
        var a=g.pose().transformPosition(x,y,new Vector2f());var b=g.pose().transformPosition(x+w,y+h,new Vector2f());
        int left=Math.round(a.x*scale),top=Math.round(a.y*scale),width=Math.round(b.x*scale)-left,height=Math.round(b.y*scale)-top;
        if(width<=0||height<=0)return null;
        var pose=new Matrix3x2f().scale(1f/scale);var clip=g.scissorStack.peek();
        var bounds=new ScreenRectangle(left-9,top-9,width+18,height+18).transformMaxBounds(pose);
        if(clip!=null)bounds=bounds.intersection(clip);
        return bounds==null?null:new Rect(left,top,width,height,scale,pose,clip,bounds);
    }
    private static HudGlassGeometry.Mesh mesh(Rect r,int radius){
        long key=((long)r.w<<32)|((long)r.h<<8)|radius;
        return CACHE.computeIfAbsent(key,k->HudGlassGeometry.build(List.of(new HudGlassGeometry.Box(0,0,r.w,r.h)),radius));
    }
    static void fill(GuiGraphicsExtractor g,float x,float y,float w,float h,int color,int radius){gradient(g,x,y,w,h,color,color,color,color,radius);}
    static void gradient(GuiGraphicsExtractor g,float x,float y,float w,float h,int tl,int tr,int br,int bl,int radius){
        var r=rect(g,x,y,w,h);if(r!=null)g.guiRenderState.addGuiElement(new Fill(r,mesh(r,radius),Ui.faded(tl),Ui.faded(tr),Ui.faded(br),Ui.faded(bl),-1,0));
    }
    static void panel(GuiGraphicsExtractor g,float x,float y,float w,float h,int sidebar){
        var r=rect(g,x,y,w,h);if(r==null)return;
        float split=sidebar<0?-1:sidebar*Math.abs(g.pose().m00())*r.scale;
        int panel=Ui.faded(Ui.panelSurface(Ui.PANEL)),side=Ui.faded(Ui.panelSurface(Ui.SIDEBAR));
        g.guiRenderState.addGuiElement(new Fill(r,mesh(r,6),panel,panel,panel,panel,split,side));
    }
    static void stroke(GuiGraphicsExtractor g,int x,int y,int w,int h,int left,int right){
        stroke(g,x,y,w,h,left,right,3);
    }
    static void stroke(GuiGraphicsExtractor g,int x,int y,int w,int h,int left,int right,int radius){
        var r=rect(g,x,y,w,h);if(r!=null)g.guiRenderState.addGuiElement(new Stroke(r,mesh(r,radius),radius,left,right,0,1,false));
    }
    static void outline(GuiGraphicsExtractor g,float x,float y,float w,float h,OutlineTravel travel,int primary,int secondary,float opacity){
        var r=rect(g,x,y,w,h);if(r==null)return;
        double phase=travel==null?(System.nanoTime()/1e9*OutlineTravel.SPEED)%(2.0*(r.w+r.h)):travel.update(r.x,r.y,r.w,r.h,System.nanoTime());
        g.guiRenderState.addGuiElement(new Stroke(r,mesh(r,6),6,primary,secondary,phase,opacity,true));
    }
    private interface State extends GuiElementRenderState {
        Rect rect();
        default RenderPipeline pipeline(){return RenderPipelines.GUI;}
        default TextureSetup textureSetup(){return TextureSetup.noTexture();}
        default ScreenRectangle scissorArea(){return rect().clip;}
        default ScreenRectangle bounds(){return rect().bounds;}
        default void quad(VertexConsumer v,float x0,float y0,float x1,float y1,float x2,float y2,float x3,float y3,int a,int b,int c,int d){
            var r=rect();float area=x0*y1-x1*y0+x1*y2-x2*y1+x2*y3-x3*y2+x3*y0-x0*y3;
            v.addVertexWith2DPose(r.pose,r.x+x0,r.y+y0).setColor(a);
            if(area>0){v.addVertexWith2DPose(r.pose,r.x+x3,r.y+y3).setColor(d);v.addVertexWith2DPose(r.pose,r.x+x2,r.y+y2).setColor(c);v.addVertexWith2DPose(r.pose,r.x+x1,r.y+y1).setColor(b);}
            else{v.addVertexWith2DPose(r.pose,r.x+x1,r.y+y1).setColor(b);v.addVertexWith2DPose(r.pose,r.x+x2,r.y+y2).setColor(c);v.addVertexWith2DPose(r.pose,r.x+x3,r.y+y3).setColor(d);}
        }
    }
    private record Fill(Rect rect,HudGlassGeometry.Mesh mesh,int tl,int tr,int br,int bl,float split,int side) implements State {
        private int color(float x,float y,boolean sidebar){return sidebar?side:UiEffects.mix(UiEffects.mix(tl,tr,x/rect.w),UiEffects.mix(bl,br,x/rect.w),y/rect.h);}
        private void fill(VertexConsumer v,float lt,float rt,float top,float lb,float rb,float bottom,boolean sidebar){
            quad(v,lt,top,lb,bottom,rb,bottom,rt,top,color(lt,top,sidebar),color(lb,bottom,sidebar),color(rb,bottom,sidebar),color(rt,top,sidebar));
        }
        public void buildVertices(VertexConsumer v){
            for(var q:mesh.fill()){
                if(split>q.leftTop()&&split<q.rightTop()&&split>q.leftBottom()&&split<q.rightBottom()){
                    fill(v,q.leftTop(),split,q.top(),q.leftBottom(),split,q.bottom(),true);
                    fill(v,split,q.rightTop(),q.top(),split,q.rightBottom(),q.bottom(),false);
                }else fill(v,q.leftTop(),q.rightTop(),q.top(),q.leftBottom(),q.rightBottom(),q.bottom(),split>0&&(q.leftTop()+q.rightTop())*.5<split);
            }
            for(var e:mesh.edge()){
                boolean sidebar=split>0&&(e.x0()+e.x1())*.5<split;int a=color(e.x0(),e.y0(),sidebar),b=color(e.x1(),e.y1(),sidebar);
                quad(v,e.x0()-.5f*e.nx0(),e.y0()-.5f*e.ny0(),e.x0(),e.y0(),e.x1(),e.y1(),e.x1()-.5f*e.nx1(),e.y1()-.5f*e.ny1(),a&0xffffff,a,b,b&0xffffff);
            }
        }
    }
    private record Stroke(Rect rect,HudGlassGeometry.Mesh mesh,int radius,int primary,int secondary,double phase,float opacity,boolean animated) implements State {
        private double position(float x,float y){
            int w=rect.w,h=rect.h,r=radius;double angle;
            if(x<r&&y<r){angle=Math.atan2(y-r,x-r);return 2.0*(w+h)-r+(angle+Math.PI)*4*r/Math.PI;}
            if(x>w-r&&y<r){angle=Math.atan2(y-r,x-w+r);return w-r+(angle+Math.PI/2)*4*r/Math.PI;}
            if(x>w-r&&y>h-r){angle=Math.atan2(y-h+r,x-w+r);return w+h-r+angle*4*r/Math.PI;}
            if(x<r&&y>h-r){angle=Math.atan2(y-h+r,x-r);return 2*w+h-r+(angle-Math.PI/2)*4*r/Math.PI;}
            if(y<r)return x;if(x>w-r)return w+y;if(y>h-r)return 2*w+h-x;return 2.0*(w+h)-y;
        }
        private int color(float x,float y){int c=animated?UiEffects.outlineColor(position(x,y),phase,2.0*(rect.w+rect.h),primary,secondary):UiEffects.mix(primary,secondary,x/rect.w);return Ui.alpha(c,Math.round((c>>>24)*opacity));}
        private int halo(int color,float x,float y){double p=UiEffects.outlinePulse(position(x,y),phase,2.0*(rect.w+rect.h),96);return Ui.alpha(color,(int)Math.round((color>>>24)*.64*p*p));}
        private void ribbon(VertexConsumer v,float x0,float y0,float x1,float y1,float nx0,float ny0,float nx1,float ny1,float outer,float inner,int a,int b,int c,int d){
            quad(v,x0+nx0*outer,y0+ny0*outer,x0+nx0*inner,y0+ny0*inner,x1+nx1*inner,y1+ny1*inner,x1+nx1*outer,y1+ny1*outer,a,c,d,b);
        }
        public void buildVertices(VertexConsumer v){for(var e:mesh.edge()){
            int count=Math.max(1,(int)Math.ceil(Math.hypot(e.x1()-e.x0(),e.y1()-e.y0())/3));
            for(int i=0;i<count;i++){
                float t0=(float)i/count,t1=(float)(i+1)/count;
                float x0=e.x0()+(e.x1()-e.x0())*t0,y0=e.y0()+(e.y1()-e.y0())*t0,x1=e.x0()+(e.x1()-e.x0())*t1,y1=e.y0()+(e.y1()-e.y0())*t1;
                float nx0=e.nx0()+(e.nx1()-e.nx0())*t0,ny0=e.ny0()+(e.ny1()-e.ny0())*t0,nx1=e.nx0()+(e.nx1()-e.nx0())*t1,ny1=e.ny0()+(e.ny1()-e.ny0())*t1;
                int a=color(x0,y0),b=color(x1,y1);
                if(animated){int ha=halo(a,x0,y0),hb=halo(b,x1,y1);if((ha>>>24)+(hb>>>24)>0){
                    ribbon(v,x0,y0,x1,y1,nx0,ny0,nx1,ny1,-8,0,a&0xffffff,b&0xffffff,ha,hb);
                    ribbon(v,x0,y0,x1,y1,nx0,ny0,nx1,ny1,0,3,ha,hb,a&0xffffff,b&0xffffff);
                }}
                ribbon(v,x0,y0,x1,y1,nx0,ny0,nx1,ny1,-.3f,.3f,a&0xffffff,b&0xffffff,a,b);
                ribbon(v,x0,y0,x1,y1,nx0,ny0,nx1,ny1,.3f,.85f,a,b,a,b);
                ribbon(v,x0,y0,x1,y1,nx0,ny0,nx1,ny1,.85f,1.3f,a,b,a&0xffffff,b&0xffffff);
            }
        }}
    }
}
