package me.xchro.neon.mixin;

import me.xchro.neon.Neon;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.xchro.neon.render.BlockOutline;

/** Style only the primary native selection outline; its shape, depth testing and target are unchanged. */
@Mixin(LevelRenderer.class)
public abstract class BlockOutlineMixin {
    @Inject(method="render",at=@At("HEAD"))
    private void neon$begin(CallbackInfo ci){BlockOutline.begin();}
    @Inject(method="submitBlockOutline",at=@At("HEAD"),cancellable=true)
    private void neon$capture(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,
                              net.minecraft.client.renderer.state.level.LevelRenderState state,CallbackInfo ci){
        if(BlockOutline.shader()){BlockOutline.capture(state.blockOutlineRenderState);ci.cancel();}
    }
    @Inject(method="render",at=@At("RETURN"))
    private void neon$shader(CallbackInfo ci){BlockOutline.render();}
    @Inject(method="close",at=@At("HEAD"))
    private void neon$close(CallbackInfo ci){BlockOutline.close();}
    @ModifyArg(method="submitBlockOutline",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/LevelRenderer;submitHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;IFZ)V",ordinal=1),index=4)
    private int neon$color(int original) { return ClientModule.BLOCK_OUTLINE.active()?Neon.config.blockOutlineColor():original; }
    @ModifyArg(method="submitBlockOutline",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/LevelRenderer;submitHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;IFZ)V",ordinal=1),index=5)
    private float neon$width(float original) { return ClientModule.BLOCK_OUTLINE.active()?(float)Neon.config.blockOutlineWidth:original; }
}
