package me.xchro.neon.render;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.xchro.neon.Neon;
import me.xchro.neon.config.NeonConfig.BlockOutlineMode;
import me.xchro.neon.config.ThemeColor;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;

/** Unity selected-block shaders with Neon's native eligibility, voxel shape and lifecycle. */
public final class BlockOutline {
    private static BlockOutlineRenderState target;
    private static BlockOutlineRenderState previous;
    private static int previousColor;
    private static BlockHighlightRenderer renderer;
    private BlockOutline() {}
    public static boolean shader(){return ClientModule.BLOCK_OUTLINE.active()&&Neon.config.blockOutlineMode!=BlockOutlineMode.WIREFRAME;}
    public static void begin(){target=null;if(!shader())close();}
    public static void capture(BlockOutlineRenderState state){target=state;}
    public static void render(){
        if(!shader()||target==null||target.shape().isEmpty())return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        try {
            if(renderer==null)renderer=new BlockHighlightRenderer();
            var c=Neon.config;int color=c.blockOutlineColor();
            if(!target.equals(previous)||color!=previousColor){
                var pos=target.pos();var boxes=new ArrayList<BlockHighlightRenderer.Box>();
                for(var box:target.shape().toAabbs())boxes.add(new BlockHighlightRenderer.Box(box.move(pos).inflate(.002),color));
                renderer.targets(boxes);previous=target;previousColor=color;
            }
            var camera=mc.gameRenderer.mainCamera();var pose=new PoseStack();
            pose.mulPose(Axis.XP.rotationDegrees(camera.xRot()));pose.mulPose(Axis.YP.rotationDegrees(camera.yRot()+180));
            int secondary=c.theme.color(ThemeColor.SECONDARY);
            var effects=new BlockHighlightRenderer.Effects(c.blockOutlineGradient?BlockHighlightRenderer.ShaderStyle.GRADIENT:BlockHighlightRenderer.ShaderStyle.SOLID,
                .65f,.65f,true,1,1,secondary,secondary,1,false,false);
            renderer.draw(pose,BlockHighlightRenderer.Mode.valueOf(c.blockOutlineMode.name()),(float)c.blockOutlineWidth,(float)c.blockOutlineFill,.65f,c.blockOutlineGlow,false,effects,false);
        } catch(RuntimeException error){
            Neon.LOG.error("Block outline shader failed; restoring native selection",error);ClientModule.BLOCK_OUTLINE.enabled(Neon.config,false);close();Neon.save();
        }
    }
    public static void close(){target=previous=null;if(renderer!=null){renderer.close();renderer=null;}ImmediateGeometry.close();}
}
