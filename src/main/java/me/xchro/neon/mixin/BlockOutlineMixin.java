package me.xchro.neon.mixin;

import me.xchro.neon.Neon;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Style only the primary native selection outline; its shape, depth testing and target are unchanged. */
@Mixin(LevelRenderer.class)
public abstract class BlockOutlineMixin {
    @ModifyArg(method="submitBlockOutline",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/LevelRenderer;submitHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;IFZ)V",ordinal=1),index=4)
    private int neon$color(int original) { return ClientModule.BLOCK_OUTLINE.active()?Neon.config.blockOutlineColor():original; }
    @ModifyArg(method="submitBlockOutline",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/LevelRenderer;submitHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;IFZ)V",ordinal=1),index=5)
    private float neon$width(float original) { return ClientModule.BLOCK_OUTLINE.active()?(float)Neon.config.blockOutlineWidth:original; }
}
