package me.xchro.neon.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.*;



import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.*;
import org.joml.*;
import java.nio.*;
import java.util.*;
import java.lang.Math;

/** One cached target list, one silhouette batch and one bounded shader pass per module. */
public final class BlockHighlightRenderer {
    public enum ShaderStyle { SOLID, RAINBOW, GRADIENT, BLOOM }
    public enum Mode { OUTLINE, OUTLINE_FILL, FILL, GLOW, WIREFRAME }
    public record Box(AABB bounds,int color){}
    public record Effects(ShaderStyle shader, float speed, float step, boolean smooth, float lineStrength, float fillStrength,
                          int outlineSecond, int fillSecond, float outlineOpacity, boolean innerGlow, boolean fastLines) {
        public static final Effects SOLID = new Effects(ShaderStyle.SOLID,0,.5f,true,1,1,0xffffffff,0xffffffff,1,false,false);
    }
    private record Post(float width,float height,float time) implements DynamicUniformStorage.DynamicUniform {
        public void write(ByteBuffer b){Std140Builder.intoBuffer(b).putVec4(width,height,time,0);}
    }
    private record Style(float width,float outline,float fill,float glow,boolean glowMode,boolean bloom,Effects effects,boolean throughWalls,float rainbowPhase) implements DynamicUniformStorage.DynamicUniform {
        public void write(ByteBuffer b){
            var out=Std140Builder.intoBuffer(b).putVec4(width,outline*effects.outlineOpacity,fill>0?1:0,throughWalls?1:0)
                .putVec4(1,1,1,1).putVec4(fill,glow,1,0).putVec4(1,1,1,1)
                .putVec4(glowMode?1:0,bloom?1:0,effects.innerGlow?1:0,effects.fastLines?1:0)
                .putVec4(effects.shader.ordinal(),effects.speed,effects.step,effects.smooth?1:0);
            rgba(out,effects.outlineSecond);rgba(out,effects.fillSecond);
            out.putVec4(effects.lineStrength,effects.fillStrength,rainbowPhase,0);
        }
        private static void rgba(Std140Builder out,int color){out.putVec4((color>>>16&255)/255f,(color>>>8&255)/255f,(color&255)/255f,(color>>>24)/255f);}
    }
    private static final RenderPipeline MASK=RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation(Identifier.parse("neon:pipeline/block_mask"))
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,true))
        .withColorTargetState(ColorTargetState.DEFAULT).withCull(false).build();
    private DynamicUniformStorage<Post> post;
    private DynamicUniformStorage<Style> styles;

    private final RenderType maskLayer=RenderType.create("neon_block_mask",RenderSetup.builder(MASK).setOutputTarget(new OutputTarget("neon_block_mask",()->this.target)).createRenderSetup());
    private RenderTarget target;
    private final BlockBloomRenderer.Buffers bloomBuffers=new BlockBloomRenderer.Buffers();
    private GpuBuffer screenVertices;
    private List<Box> boxes=List.of();

    private final Matrix4f projection=new Matrix4f();
    private final Vector4f point=new Vector4f();
    public void targets(List<Box> values){boxes=List.copyOf(values);}
    public int count(){return boxes.size();}
    public void clear(){boxes=List.of();}
    public void close(){clear();bloomBuffers.close();if(target!=null)target.destroyBuffers();if(post!=null)post.close();if(styles!=null)styles.close();if(screenVertices!=null)screenVertices.close();target=null;post=null;styles=null;screenVertices=null;}
    public void draw(PoseStack stack,Mode mode,float lineWidth,float fillOpacity,float glow,boolean bloom,boolean tracers){
        draw(stack,mode,lineWidth,fillOpacity,glow,bloom,tracers,Effects.SOLID);
    }
    public void draw(PoseStack stack,Mode mode,float lineWidth,float fillOpacity,float glow,boolean bloom,boolean tracers,Effects effects){
        draw(stack,mode,lineWidth,fillOpacity,glow,bloom,tracers,effects,true);
    }
    public void draw(PoseStack stack,Mode mode,float lineWidth,float fillOpacity,float glow,boolean bloom,boolean tracers,Effects effects,boolean throughWalls){
        if(boxes.isEmpty())return;
        var mc=Minecraft.getInstance();var camera=mc.gameRenderer.mainCamera();Vec3 origin=camera.position();
        if(mode==Mode.WIREFRAME)return; // Native wireframe uses the original submission path.
        camera.getViewRotationProjectionMatrix(projection);
        float minX=1,minY=1,maxX=-1,maxY=-1;boolean near=false;
        for(Box box:boxes){AABB b=box.bounds;float x0=(float)(b.minX-origin.x),x1=(float)(b.maxX-origin.x),y0=(float)(b.minY-origin.y),y1=(float)(b.maxY-origin.y),z0=(float)(b.minZ-origin.z),z1=(float)(b.maxZ-origin.z);
            // Keep every selected target submitted; the GPU clips geometry at the viewport.
            // CPU frustum rejection also cut off outlines/glow at the edge of the screen.
            for(int i=0;i<8;i++){projection.transform(point.set((i&1)==0?x0:x1,(i&2)==0?y0:y1,(i&4)==0?z0:z1,1));if(point.w<=.001f){near=true;continue;}
                minX=Math.min(minX,point.x/point.w);maxX=Math.max(maxX,point.x/point.w);minY=Math.min(minY,point.y/point.w);maxY=Math.max(maxY,point.y/point.w);}
        }
        {
            ensure(mc.getWindow().getWidth(),mc.getWindow().getHeight());
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(),new Vector4f(0),target.getDepthTexture(),0);
            VertexConsumer output=ImmediateGeometry.begin(PrimitiveTopology.QUADS,DefaultVertexFormat.POSITION_COLOR);
            for(Box box:boxes)emit(output,stack,box,origin);
            ImmediateGeometry.draw(maskLayer);
            boolean bloomEnabled=mode==Mode.GLOW||bloom||effects.shader==ShaderStyle.BLOOM;
            float radius=Math.clamp(lineWidth,.5f,8f);
            var p=post.writeUniform(new Post(target.width,target.height,(System.currentTimeMillis()%1_000_000L)/1000f));
            float rainbowPhase=0; // Neon uses the saved solid/gradient palette, never a second rainbow clock.
            var s=styles.writeUniform(new Style(radius,mode==Mode.FILL?0:1,mode==Mode.FILL||mode==Mode.OUTLINE_FILL?fillOpacity:0,glow,mode==Mode.GLOW,bloomEnabled,effects,throughWalls,rainbowPhase));
            RenderTarget bloomTarget=bloomEnabled?BlockBloomRenderer.blur(target,mc.gameRenderer.mainRenderTarget(),screenVertices,bloomBuffers,throughWalls,false,java.awt.Color.WHITE):target;
            int padding=(int)Math.ceil(radius)+(bloomEnabled?16:2),w=target.width,h=target.height;
            int left=near?0:Math.clamp((int)((minX+1)*.5f*w)-padding,0,w-1),right=near?w:Math.clamp((int)((maxX+1)*.5f*w)+padding,left+1,w);
            int top=near?0:Math.clamp((int)((1-maxY)*.5f*h)-padding,0,h-1),bottom=near?h:Math.clamp((int)((1-minY)*.5f*h)+padding,top+1,h);
            try(var pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(()->"Neon block shader highlights",mc.gameRenderer.mainRenderTarget().getColorTextureView(),Optional.empty())){
                pass.setPipeline(BlockHighlightPipeline.POST_OUTLINE);pass.setUniform("PostData",p);pass.setUniform("OutlineData",s);
                var sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
                pass.bindTexture("u_Texture",target.getColorTextureView(),sampler);pass.bindTexture("u_EntityDepth",target.getDepthTextureView(),sampler);pass.bindTexture("u_MainDepth",mc.gameRenderer.mainRenderTarget().getDepthTextureView(),sampler);
                pass.bindTexture("u_Bloom",bloomTarget.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                pass.enableScissor(left,h-bottom,right-left,bottom-top);pass.setVertexBuffer(0,screenVertices.slice());pass.draw(6,1,0,0);
            }finally{post.endFrame();styles.endFrame();}
        }

    }
    private void ensure(int width,int height){
        if(post==null){post=new DynamicUniformStorage<>("Neon block post",16,4);styles=new DynamicUniformStorage<>("Neon block style",144,4);}
        if(target==null)target=new TextureTarget("Neon block highlights",width,height,true,GpuFormat.RGBA8_UNORM);else if(target.width!=width||target.height!=height)target.resize(width,height);
        if(screenVertices==null){ByteBuffer b=ByteBuffer.allocateDirect(72).order(ByteOrder.nativeOrder());for(float v:new float[]{-1,-1,0,-1,1,0,1,1,0,-1,-1,0,1,1,0,1,-1,0})b.putFloat(v);b.flip();screenVertices=RenderSystem.getDevice().createBuffer(()->"Neon block fullscreen",GpuBuffer.USAGE_VERTEX,b);}
    }
    private static final int[] FACES={0,1,3,2,4,6,7,5,0,4,5,1,2,3,7,6,0,2,6,4,1,5,7,3};
    private static void emit(VertexConsumer out,PoseStack stack,Box box,Vec3 camera){AABB b=box.bounds;for(int i:FACES)out.addVertex(stack.last(),(float)(((i&1)==0?b.minX:b.maxX)-camera.x),(float)(((i&2)==0?b.minY:b.maxY)-camera.y),(float)(((i&4)==0?b.minZ:b.maxZ)-camera.z)).setColor(box.color);}
}
