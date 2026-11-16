package me.xchro.neon.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mojang.blaze3d.vertex.PoseStack;
import me.xchro.neon.Neon;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScreenEffectRenderer.class)
public abstract class FireOverlayMixin {
    @WrapOperation(method="submitFire",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V"))
    private static void neon$lowerFire(SubmitNodeCollector collector,PoseStack pose,RenderType type,SubmitNodeCollector.CustomGeometryRenderer geometry,Operation<Void> original) {
        if(!ClientModule.LOW_FIRE.active()) { original.call(collector,pose,type,geometry);return; }
        pose.pushPose();
        try { pose.translate(0,-(float)Neon.config.fireLowering,0);original.call(collector,pose,type,geometry); }
        finally { pose.popPose(); }
    }
}
